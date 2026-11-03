const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { randomUUID } = require('node:crypto');

// A small EXIF JPEG fixture: camera, capture time and GPS in a TIFF APP1 segment.
function photoFixture() {
    const tiff = Buffer.alloc(512);
    tiff.write('II'); tiff.writeUInt16LE(42,2); tiff.writeUInt32LE(8,4);
    let data = 8 + 2 + 4*12 + 4;
    function entry(at,tag,type,count,value) {
        tiff.writeUInt16LE(tag,at); tiff.writeUInt16LE(type,at+2); tiff.writeUInt32LE(count,at+4);
        if (Buffer.isBuffer(value)) {
            if (value.length <= 4) value.copy(tiff,at+8);
            else { tiff.writeUInt32LE(data,at+8); value.copy(tiff,data); data += value.length; }
        } else tiff.writeUInt32LE(value,at+8);
    }
    tiff.writeUInt16LE(4,8);
    entry(10,0x010f,2,10,Buffer.from('TestCamera\0'));
    entry(22,0x0110,2,8,Buffer.from('Model42\0'));
    entry(34,0x0132,2,20,Buffer.from('2026:10:26 12:30:00\0'));
    const gps = data; data += 2+4*12+4;
    entry(46,0x8825,4,1,gps);
    tiff.writeUInt16LE(4,gps);
    function coordinate(degrees,minutes) {
        const b=Buffer.alloc(24); [degrees,minutes,0].forEach((n,i)=>{ b.writeUInt32LE(n,i*8); b.writeUInt32LE(1,i*8+4); }); return b;
    }
    entry(gps+2,1,2,2,Buffer.from('N\0'));
    entry(gps+14,2,5,3,coordinate(59,20));
    entry(gps+26,3,2,2,Buffer.from('E\0'));
    entry(gps+38,4,5,3,coordinate(18,3));
    const payload=Buffer.concat([Buffer.from('Exif\0\0'),tiff.subarray(0,data)]);
    const header=Buffer.from([0xff,0xd8,0xff,0xe1,0,0]); header.writeUInt16BE(payload.length+2,4);
    return Buffer.concat([header,payload,Buffer.from([0xff,0xd9])]);
}

test('report features with isolated PostgreSQL schema', async t => {
    const uploadDir = await fs.mkdtemp(path.join(os.tmpdir(),'np-features-'));
    process.env.UPLOAD_DIR = uploadDir;
    const { pool,initDatabase } = require('../src/db/database');
    const schema = `test_features_${randomUUID().replaceAll('-','')}`;
    const originalConnect = pool.connect.bind(pool);
    const originalQuery = pool.query.bind(pool);
    let client, server;
    t.after(async () => {
        if (server) await new Promise(resolve=>server.close(resolve));
        pool.connect=originalConnect; pool.query=originalQuery;
        try {
            if (client) {
                await client.query('ROLLBACK');
                await client.query('SET search_path TO public');
                await client.query(`DROP SCHEMA IF EXISTS ${schema} CASCADE`);
            }
        } finally {
            if (client) client.release();
            await pool.end(); await fs.rm(uploadDir,{recursive:true,force:true});
        }
    });
    client=await originalConnect();
    await client.query(`CREATE SCHEMA ${schema}`);
    await client.query(`SET search_path TO ${schema}`);
    pool.query=client.query.bind(client);
    pool.connect=async()=>({query:client.query.bind(client),release(){}});
    await initDatabase();
    const auth=require('../src/services/authService');
    const categories=require('../src/services/categoryService');
    const admin=await auth.login('admin@admin.com','admin');
    const employee=await auth.login('test@test.com','test');
    await auth.createUser({firstname:'Other',lastname:'Employee',email:'other@example.test',password:'testpassword',role:'USER'});
    const other=await auth.login('other@example.test','testpassword');
    const {app}=require('../src/index');
    server=await new Promise(resolve=>{ const s=app.listen(0,'127.0.0.1',()=>resolve(s)); });
    const base=`http://127.0.0.1:${server.address().port}`;
    const request=(route,user=admin,options={})=>fetch(base+route,{...options,headers:{...options.headers,Authorization:`Bearer ${user.token}`}});
    const json=(method,body)=>({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
    async function expectJson(response,status) {
        assert.equal(response.status,status,await response.clone().text()); return response.json();
    }
    let category,report;
    const fields=[
        {id:randomUUID(),label:'Location',type:'TEXT',required:true,validation:{maxLength:100}},
        {id:randomUUID(),label:'Count',type:'NUMBER',required:true,validation:{min:0,max:10}},
        {id:randomUUID(),label:'Day',type:'DATE',required:true},
        {id:randomUUID(),label:'Priority',type:'SELECT',required:true,options:['Normal','Urgent']},
        {id:randomUUID(),label:'Stopped',type:'CHECKBOX',required:true},
        {id:randomUUID(),label:'Details',type:'MULTILINE',required:false}
    ];
    const values=Object.fromEntries(fields.map((f,i)=>[f.id,['Warehouse',3,'2026-10-26','Urgent',false,'Long text'][i]]));
    function form(data=values,photo=false) {
        const f=new FormData(); f.set('category',category.code); f.set('title','Custom report'); f.set('description','Description');
        f.set('fieldValues',JSON.stringify(data));
        if(photo) f.append('images',new Blob([photoFixture()],{type:'image/jpeg'}),'camera.jpg');
        return {method:'POST',body:f};
    }
    await t.test('migrations repeat and preserve legacy reports',async()=>{
        const id=randomUUID();
        await pool.query("INSERT INTO reports(id,user_id,category,title) VALUES($1,$2,'SAFETY','Legacy')",[id,employee.user.id]);
        await require('../src/db/reportFeatures').migrateReportFeatures(client);
        await require('../src/db/reportFeatures').migrateReportFeatures(client);
        const legacy=await expectJson(await request(`/api/reports/${id}`,employee),200);
        assert.equal(legacy.categoryName,'Säkerhet'); assert.deepEqual(legacy.fields,[]);
        assert.equal((await categories.listCategories()).length,4);
    });
    await t.test('only admins manage categories',async()=>{
        await expectJson(await request('/api/admin/categories',employee,json('POST',{name:'No',fields:[]})),403);
        category=await expectJson(await request('/api/admin/categories',admin,json('POST',{name:'Custom',description:'Test',fields})),201);
        assert.equal(category.fields.length,6);
        assert.equal((await expectJson(await request('/api/categories',employee),200)).length,5);
    });
    await t.test('all field types validated on server',async()=>{
        for(const [field,value] of [[fields[0],' '],[fields[1],11],[fields[1],'3'],[fields[2],'2026-02-30'],[fields[3],'BAD'],[fields[4],'false']]) {
            await expectJson(await request('/api/reports',employee,form({...values,[field.id]:value})),400);
        }
        await expectJson(await request('/api/reports',employee,form({...values,[randomUUID()]:'Injected'})),400);
        report=await expectJson(await request('/api/reports',employee,form(values,true)),201);
        assert.equal(report.fields.length,6); assert.equal(report.categoryName,'Custom');
        assert.equal(report.fields.find(f=>f.definition.id===fields[4].id).value,false);
    });
    await t.test('EXIF time, camera and GPS survive storage and response',async()=>{
        const meta=report.images[0].metadata;
        assert.equal(meta.cameraMake,'TestCamera'); assert.equal(meta.cameraModel,'Model42');
        assert.equal(meta.takenAt,'2026-10-26T11:30:00.000Z');
        assert.ok(Math.abs(meta.latitude-(59+20/60))<0.000001);
        assert.ok(Math.abs(meta.longitude-18.05)<0.000001);
        const image=await request(report.images[0].url,employee);
        assert.equal(image.status,200); assert.deepEqual(Buffer.from(await image.arrayBuffer()),photoFixture());
        assert.equal((await request(report.images[0].url,other)).status,404);
    });
    await t.test('optional fields retain their original definitions when unanswered',async()=>{
        const missing={...values}; delete missing[fields[5].id];
        const created=await expectJson(await request('/api/reports',employee,form(missing)),201);
        const optional=created.fields.find(f=>f.definition.id===fields[5].id);
        assert.equal(optional.definition.label,'Details'); assert.equal(optional.value,null);
        await pool.query('DELETE FROM reports WHERE id=$1',[created.id]);
    });
    await t.test('existing uploads are backfilled once without replacing stored values',async()=>{
        const id=randomUUID(); const name=`${id}.jpg`;
        await fs.writeFile(path.join(uploadDir,name),photoFixture());
        await pool.query("INSERT INTO report_images(id,report_id,file_name,content_type,size_bytes,camera_make) VALUES($1,$2,$3,'image/jpeg',$4,'Original')",[id,report.id,name,photoFixture().length]);
        const {backfillPhotoMetadata}=require('../src/services/backfillPhotoMetadata');
        assert.equal(await backfillPhotoMetadata(),1);
        assert.equal(await backfillPhotoMetadata(),0);
        const row=(await pool.query('SELECT * FROM report_images WHERE id=$1',[id])).rows[0];
        assert.equal(row.camera_make,'Original'); assert.equal(row.camera_model,'Model42'); assert.ok(row.latitude);
        assert.equal(row.metadata_processed,true);
        await pool.query('DELETE FROM report_images WHERE id=$1',[id]);
    });
    await t.test('lists and dashboard reflect real reports and status changes',async()=>{
        assert.equal((await expectJson(await request('/api/reports',employee),200)).length,2);
        assert.equal((await expectJson(await request('/api/reports',other),200)).length,0);
        let dashboard=await expectJson(await request('/api/admin/dashboard'),200);
        assert.equal(dashboard.openReports,2); assert.equal(dashboard.storeCount,3);
        await expectJson(await request(`/api/reports/${report.id}/status`,admin,json('PATCH',{status:'RESOLVED'})),200);
        dashboard=await expectJson(await request('/api/admin/dashboard'),200);
        assert.equal(dashboard.openReports,1); assert.equal(dashboard.resolvedReports,1);
    });
    await t.test('reporter and admin exchange messages with idempotent retries',async()=>{
        const route=`/api/reports/${report.id}/messages`;
        const message={id:randomUUID(),body:'Please investigate'};
        const first=await expectJson(await request(route,employee,json('POST',message)),201);
        const retry=await expectJson(await request(route,employee,json('POST',message)),200);
        assert.equal(first.id,retry.id);
        await expectJson(await request(route,admin,json('POST',{id:randomUUID(),body:'We are reviewing this'})),201);
        assert.equal((await expectJson(await request(route,employee),200)).length,2);
        await expectJson(await request(route,other),404);
        await expectJson(await request(route,other,json('POST',{id:randomUUID(),body:'Unauthorized'})),404);
        await expectJson(await request(`/api/reports/${report.id}`,other),404);
        await expectJson(await request(route,employee,json('POST',{id:message.id,body:'Different'})),409);
        for(const body of [' ','x'.repeat(4001)]) await expectJson(await request(route,employee,json('POST',{id:randomUUID(),body})),400);
    });
    await t.test('message pagination has no duplicates or gaps',async()=>{
        for(let i=0;i<55;i++) await pool.query("INSERT INTO report_messages(id,report_id,sender_id,sender_name,sender_role,body) VALUES($1,$2,$3,'Test','USER',$4)",[randomUUID(),report.id,employee.user.id,`Message ${i}`]);
        const route=`/api/reports/${report.id}/messages`;
        const page=await expectJson(await request(route,employee),200);
        assert.equal(page.length,50);
        const older=await expectJson(await request(`${route}?before=${page[0].id}`,employee),200);
        assert.equal(older.length,7); assert.equal(new Set([...page,...older].map(m=>m.id)).size,57);
        const catchup=await expectJson(await request(`${route}?after=${older[older.length-1].id}`,employee),200);
        assert.deepEqual(catchup.map(m=>m.id),page.map(m=>m.id));
        assert.equal((await expectJson(await request(`${route}?after=${page[page.length-1].id}`,employee),200)).length,0);
    });
    await t.test('editing and archiving preserves old report definitions',async()=>{
        const changed={...category,name:'Renamed',isActive:false,fields:category.fields.slice(1).map(f=>({...f,label:'New label'}))};
        await expectJson(await request(`/api/admin/categories/${category.id}`,admin,json('PUT',changed)),200);
        const old=await expectJson(await request(`/api/reports/${report.id}`,employee),200);
        assert.equal(old.categoryName,'Custom'); assert.equal(old.fields[0].definition.label,'Location');
        assert.equal((await expectJson(await request('/api/categories',employee),200)).some(c=>c.id===category.id),false);
        assert.equal((await expectJson(await request('/api/categories?includeArchived=true',employee),200)).some(c=>c.id===category.id),false);
        assert.equal((await expectJson(await request('/api/categories?includeArchived=true',admin),200)).find(c=>c.id===category.id).isActive,false);
        await expectJson(await request('/api/reports',employee,form()),400);
        assert.equal((await expectJson(await request(`/api/reports/${report.id}/messages`,employee),200)).length,50);
    });
    await t.test('cannot steal a field from another category',async()=>{
        await expectJson(await request('/api/admin/categories',admin,json('POST',{name:'Invalid',fields:[fields[0]]})),400);
        assert.equal((await categories.listCategories(true)).length,5);
    });
    await t.test('invalid category configuration rejected',async()=>{
        for(const field of [ {...fields[3],options:[]}, {...fields[1],validation:{min:5,max:1}}, {...fields[0],validation:{maxLength:0}} ]) {
            await expectJson(await request('/api/admin/categories',admin,json('POST',{name:'Bad',fields:[{...field,id:randomUUID()}]})),400);
        }
    });
    await t.test('expired sessions rejected by new endpoints',async()=>{
        await auth.logout(other.token);
        assert.equal((await request('/api/categories',other)).status,401);
        assert.equal((await request(`/api/reports/${report.id}/messages`,other)).status,401);
    });
});
