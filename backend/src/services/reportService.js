const { pool } = require('../db/database');
const { v4: uuidv4 } = require('uuid');
const fs = require('fs');
const path = require('path');
const { PHOTO_TIME_ZONE, readPhotoMetadata } = require('./photoMetadata');

const UPLOAD_DIR = process.env.UPLOAD_DIR || path.join(__dirname, '..', '..', 'uploads');
const categoryService = require('./categoryService');
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
const MAX_TITLE_LENGTH = 150;

fs.mkdirSync(UPLOAD_DIR, { recursive: true });

function formatReportResponse(row, images) {
    return {
        id: row.id,
        category: row.category,
        categoryName: row.category_name || row.category,
        categoryIcon: row.category_icon || 'OTHER',
        fields: row.fields || [],
        title: row.title,
        description: row.description,
        status: row.status,
        createdAt: row.created_at,
        storeId: row.store_id ? parseInt(row.store_id, 10) : null,
        storeName: row.store_name || null,
        author: {
            id: row.user_id,
            firstname: row.firstname,
            lastname: row.lastname
        },
        images: images.map(img => ({
            id: img.id,
            url: `/api/reports/${row.id}/images/${img.id}`,
            contentType: img.content_type,
            sizeBytes: img.size_bytes,
            metadata: {
                takenAt: img.taken_at,
                latitude: img.latitude,
                longitude: img.longitude,
                altitude: img.altitude,
                cameraMake: img.camera_make,
                cameraModel: img.camera_model
            }
        }))
    };
}

async function removeFiles(files) {
    await Promise.all((files || []).map(file => fs.promises.unlink(file.path).catch(() => {})));
}

async function createReport(user, data, files) {
    const { category, title, description = '' } = data;

    try {
        if (typeof category !== 'string') throw new Error('Välj en kategori');
        if (typeof title !== 'string' || !title.trim()) {
            throw new Error('Title is required');
        }
        if (title.trim().length > MAX_TITLE_LENGTH) {
            throw new Error(`Title must be at most ${MAX_TITLE_LENGTH} characters`);
        }
        if (typeof description !== 'string') throw new Error('Beskrivningen måste vara text');
    } catch (err) {
        await removeFiles(files);
        throw err;
    }

    const reportId = uuidv4();
    const storeId = user.store ? user.store.storeId : null;
    const metadata = await Promise.all((files || []).map(file => readPhotoMetadata(file.path)));
    let client;

    try {
        client = await pool.connect();
        await client.query('BEGIN');
        const categoryRow = await client.query('SELECT id FROM report_categories WHERE code=$1 AND is_active=TRUE FOR SHARE', [category]);
        if (!categoryRow.rows.length) throw new Error('Kategorin är inte tillgänglig. Uppdatera formuläret.');
        const selectedCategory = (await categoryService.listCategories(false, client)).find(c => c.code === category);
        let values;
        try { values = JSON.parse(data.fieldValues || '{}'); } catch (_) { throw new Error('Ogiltiga fältvärden'); }
        const fields = categoryService.validateValues(selectedCategory.fields, values);
        await client.query(
            'INSERT INTO reports (id, user_id, store_id, category, title, description,category_id,category_name,category_icon) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)',
            [reportId, user.id, storeId, category, title.trim(), description.trim(),selectedCategory.id,selectedCategory.name,selectedCategory.icon]
        );
        for (const field of fields) {
            await client.query('INSERT INTO report_field_values(report_id,field_id,definition,value) VALUES($1,$2,$3,$4)',
                [reportId,field.definition.id,JSON.stringify(field.definition),JSON.stringify(field.value)]);
        }

        for (const [index, file] of (files || []).entries()) {
            const meta = metadata[index];
            // taken_at uses the photo's own offset, or PHOTO_TIME_ZONE when the camera didn't record one
            await client.query(
                `INSERT INTO report_images
                    (id, report_id, file_name, content_type, size_bytes,
                     taken_at, latitude, longitude, altitude, camera_make, camera_model, exif, metadata_processed)
                 VALUES ($1, $2, $3, $4, $5,
                     COALESCE(($6::text || $7::text)::timestamptz, $6::timestamp AT TIME ZONE $8::text),
                     $9, $10, $11, $12, $13, $14, TRUE)`,
                [
                    path.parse(file.filename).name, reportId, file.filename, file.mimetype, file.size,
                    meta.takenAtLocal, meta.takenAtOffset, PHOTO_TIME_ZONE,
                    meta.latitude, meta.longitude, meta.altitude, meta.cameraMake, meta.cameraModel,
                    meta.exif ? JSON.stringify(meta.exif) : null
                ]
            );
        }

        await client.query('COMMIT');
    } catch (err) {
        if (client) await client.query('ROLLBACK').catch(() => {});
        await removeFiles(files);
        throw err;
    } finally {
        if (client) client.release();
    }

    return getReportById(reportId);
}

async function getReportById(reportId) {
    const result = await pool.query(
        `SELECT r.*, u.firstname, u.lastname, s.store_name,
         COALESCE((SELECT jsonb_agg(jsonb_build_object('definition',v.definition,'value',v.value) ORDER BY (v.definition->>'sortOrder')::int,v.field_id)
             FROM report_field_values v WHERE v.report_id=r.id),'[]') AS fields
         FROM reports r
         JOIN users u ON r.user_id = u.id
         LEFT JOIN stores s ON r.store_id = s.store_id
         WHERE r.id = $1`,
        [reportId]
    );
    if (result.rows.length === 0) return null;

    const images = await pool.query(
        'SELECT * FROM report_images WHERE report_id = $1 ORDER BY created_at ASC',
        [reportId]
    );
    return formatReportResponse(result.rows[0], images.rows);
}

async function getReportsForUser(user) {
    const isAdmin = user.role === 'ADMIN';
    const result = await pool.query(
        `SELECT r.*, u.firstname, u.lastname, s.store_name,
         COALESCE((SELECT jsonb_agg(jsonb_build_object('definition',v.definition,'value',v.value) ORDER BY (v.definition->>'sortOrder')::int,v.field_id)
             FROM report_field_values v WHERE v.report_id=r.id),'[]') AS fields
         FROM reports r
         JOIN users u ON r.user_id = u.id
         LEFT JOIN stores s ON r.store_id = s.store_id
         ${isAdmin ? '' : 'WHERE r.user_id = $1'}
         ORDER BY r.created_at DESC`,
        isAdmin ? [] : [user.id]
    );
    if (result.rows.length === 0) return [];

    const images = await pool.query(
        'SELECT * FROM report_images WHERE report_id = ANY($1::uuid[]) ORDER BY created_at ASC',
        [result.rows.map(r => r.id)]
    );
    return result.rows.map(row =>
        formatReportResponse(row, images.rows.filter(img => img.report_id === row.id))
    );
}

// Returns the updated report, or null if it doesn't exist
async function updateReportStatus(reportId, status) {
    if (!STATUSES.includes(status)) {
        throw new Error(`Status must be one of: ${STATUSES.join(', ')}`);
    }

    const result = await pool.query('UPDATE reports SET status = $1 WHERE id = $2', [status, reportId]);
    if (result.rowCount === 0) return null;

    return getReportById(reportId);
}

// Returns the image file info if the user is allowed to see it (owner or admin)
async function getReportImage(user, reportId, imageId) {
    const result = await pool.query(
        `SELECT i.file_name, i.content_type, r.user_id
         FROM report_images i
         JOIN reports r ON i.report_id = r.id
         WHERE i.id = $1 AND r.id = $2`,
        [imageId, reportId]
    );
    if (result.rows.length === 0) return null;

    const row = result.rows[0];
    if (user.role !== 'ADMIN' && row.user_id !== user.id) return null;

    return {
        path: path.join(UPLOAD_DIR, row.file_name),
        contentType: row.content_type
    };
}

module.exports = {
    UPLOAD_DIR,
    createReport,
    getReportsForUser,
    updateReportStatus,
    getReportImage,
    getReportById
};
