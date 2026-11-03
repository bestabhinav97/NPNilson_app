const { pool } = require('../db/database');
const { v4: uuid, validate: isUuid } = require('uuid');
const TYPES = ['TEXT', 'MULTILINE', 'NUMBER', 'DATE', 'SELECT', 'CHECKBOX'];
const ICONS = ['DAMAGED_GOODS','SAFETY','MAINTENANCE','OTHER'];
function invalid(message) { const err = new Error(message); err.status = 400; throw err; }
const text = (value, max, label) => {
    if (typeof value !== 'string' || !value.trim() || value.trim().length > max) invalid(`${label}: 1–${max} tecken krävs`);
    return value.trim();
};
function validateCategory(data) {
    if (!data || typeof data !== 'object' || Array.isArray(data)) invalid('Ogiltig kategori');
    const name = text(data.name, 100, 'Kategorinamn');
    if (typeof (data.description ?? '') !== 'string' || (data.description || '').length > 500) invalid('Beskrivningen får vara högst 500 tecken');
    if (!ICONS.includes(data.icon ?? 'OTHER')) invalid('Ogiltig ikon');
    if (!Number.isInteger(data.sortOrder ?? 0) || Math.abs(data.sortOrder ?? 0) > 10000) invalid('Ogiltig sorteringsordning');
    if (typeof (data.isActive ?? true) !== 'boolean') invalid('Ogiltigt kategoristatus');
    if (!Array.isArray(data.fields) || data.fields.length > 30) invalid('Högst 30 fält per kategori');
    const ids = new Set();
    const fields = data.fields.map((field, index) => {
        if (!field || typeof field !== 'object' || Array.isArray(field)) invalid('Ogiltigt formulärfält');
        const id = field.id || uuid();
        if (!isUuid(id) || ids.has(id)) invalid('Ogiltigt eller dubblerat fält-ID');
        ids.add(id);
        if (!TYPES.includes(field.type)) invalid('Ogiltig fälttyp');
        if (typeof (field.required ?? false) !== 'boolean') invalid('Ogiltigt obligatoriskt fält');
        const label = text(field.label, 100, 'Fältnamn');
        let options = field.type === 'SELECT' ? field.options : [];
        if (!Array.isArray(options) || options.length > 50 ||
            (field.type === 'SELECT' && (!options.length || new Set(options).size !== options.length)) ||
            options.some(option => typeof option !== 'string' || !option.trim() || option.length > 100)) invalid('Ange unika val, högst 50');
        options = options.map(option => option.trim());
        if (new Set(options).size !== options.length) invalid('Alternativen måste vara unika');
        const validation = field.validation || {};
        if (typeof validation !== 'object' || Array.isArray(validation) ||
            Object.keys(validation).some(key => !['min','max','maxLength'].includes(key))) invalid('Ogiltiga valideringsregler');
        for (const key of ['min', 'max']) {
            if (validation[key] != null && (field.type !== 'NUMBER' || !Number.isFinite(validation[key]))) invalid('Talgränser kräver ett nummerfält');
        }
        if (validation.min != null && validation.max != null && validation.min > validation.max) invalid('Min måste vara mindre än max');
        if (validation.maxLength != null && (!['TEXT','MULTILINE'].includes(field.type) || !Number.isInteger(validation.maxLength) || validation.maxLength < 1 || validation.maxLength > 4000)) invalid('Textgränsen måste vara 1–4000');
        return { id, label, type: field.type, required: field.required ?? false, options, validation, sortOrder: index };
    });
    return { name, description: (data.description || '').trim(), icon: data.icon || 'OTHER',
        sortOrder: data.sortOrder ?? 0, isActive: data.isActive ?? true, fields };
}
async function listCategories(includeArchived = false, db = pool) {
    const categories = await db.query(`SELECT * FROM report_categories ${includeArchived ? '' : 'WHERE is_active=TRUE'} ORDER BY sort_order,name,id`);
    const fields = await db.query('SELECT * FROM category_fields WHERE is_active=TRUE ORDER BY sort_order,id');
    return categories.rows.map(row => ({ id: row.id, code: row.code, name: row.name, description: row.description,
        icon: row.icon, sortOrder: row.sort_order, isActive: row.is_active,
        fields: fields.rows.filter(f => f.category_id === row.id).map(f => ({ id: f.id, label: f.label, type: f.type,
            required: f.required, options: f.options, validation: f.validation, sortOrder: f.sort_order })) }));
}
async function saveCategory(categoryId, data) {
    const value = validateCategory(data);
    if (categoryId && !isUuid(categoryId)) invalid('Ogiltig kategori');
    const id = categoryId || uuid();
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        if (categoryId) {
            const existing = await client.query('SELECT id FROM report_categories WHERE id=$1 FOR UPDATE', [id]);
            if (!existing.rows.length) { const err = new Error('Kategorin finns inte'); err.status = 404; throw err; }
            await client.query('UPDATE report_categories SET name=$2,description=$3,icon=$4,sort_order=$5,is_active=$6,updated_at=NOW() WHERE id=$1',
                [id,value.name,value.description,value.icon,value.sortOrder,value.isActive]);
        } else {
            await client.query('INSERT INTO report_categories(id,code,name,description,icon,sort_order,is_active) VALUES($1,$2,$3,$4,$5,$6,$7)',
                [id,`CUSTOM_${id}`,value.name,value.description,value.icon,value.sortOrder,value.isActive]);
        }
        for (const f of value.fields) {
            const existing = await client.query('SELECT category_id FROM category_fields WHERE id=$1', [f.id]);
            if (existing.rows.length && existing.rows[0].category_id !== id) invalid('Fältet tillhör en annan kategori');
            await client.query(`INSERT INTO category_fields(id,category_id,label,type,required,options,validation,sort_order)
                VALUES($1,$2,$3,$4,$5,$6,$7,$8) ON CONFLICT(id) DO UPDATE SET
                label=$3,type=$4,required=$5,options=$6,validation=$7,sort_order=$8,is_active=TRUE`,
                [f.id,id,f.label,f.type,f.required,JSON.stringify(f.options),JSON.stringify(f.validation),f.sortOrder]);
        }
        await client.query('UPDATE category_fields SET is_active=FALSE WHERE category_id=$1 AND NOT(id=ANY($2::uuid[]))', [id,value.fields.map(f => f.id)]);
        const result = (await listCategories(true, client)).find(c => c.id === id);
        await client.query('COMMIT');
        return result;
    } catch (err) { await client.query('ROLLBACK'); throw err; }
    finally { client.release(); }
}
function validateValues(fields, values) {
    if (!values || typeof values !== 'object' || Array.isArray(values)) invalid('Ogiltiga fältvärden');
    if (Object.keys(values).some(id => !fields.some(f => f.id === id))) invalid('Formuläret har ändrats. Öppna kategorin igen.');
    return fields.flatMap(field => {
        let value = values[field.id];
        if (typeof value === 'string') value = value.trim();
        const absent = value == null || value === '';
        if (absent) {
            if (field.required) invalid(`${field.label} krävs`);
            return [{ definition: field, value: null }];
        }
        const rules = field.validation || {};
        if (['TEXT','MULTILINE'].includes(field.type) && (typeof value !== 'string' || value.length > (rules.maxLength || 4000))) invalid(`${field.label}: ogiltig text eller för lång`);
        if (field.type === 'NUMBER' && (typeof value !== 'number' || !Number.isFinite(value) ||
            (rules.min != null && value < rules.min) || (rules.max != null && value > rules.max))) invalid(`${field.label}: ogiltigt tal`);
        if (field.type === 'CHECKBOX' && typeof value !== 'boolean') invalid(`${field.label}: välj ja eller nej`);
        if (field.type === 'SELECT' && !field.options.includes(value)) invalid(`${field.label}: välj ett giltigt alternativ`);
        if (field.type === 'DATE' && (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value) ||
            !Number.isFinite(Date.parse(value)) || new Date(value).toISOString().slice(0,10) !== value)) invalid(`${field.label}: använd ett giltigt datum (ÅÅÅÅ-MM-DD)`);
        return [{ definition: field, value }];
    });
}
module.exports = { listCategories, saveCategory, validateCategory, validateValues };
