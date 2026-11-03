const { pool } = require('../db/database');
const fs = require('fs/promises');
const path = require('path');
const { UPLOAD_DIR } = require('./reportService');
const { readPhotoMetadata, PHOTO_TIME_ZONE } = require('./photoMetadata');

// Older backend versions saved original files without extracting EXIF. Process them once,
// keeping existing non-null metadata. Missing files are retried on the next startup.
async function backfillPhotoMetadata() {
    let cursor = null;
    let processed = 0;
    while (true) {
        const page = await pool.query(`SELECT id,file_name FROM report_images
            WHERE metadata_processed=FALSE AND ($1::uuid IS NULL OR id>$1::uuid) ORDER BY id LIMIT 100`,[cursor]);
        if (!page.rows.length) break;
        for (const image of page.rows) {
            cursor = image.id;
            const file = path.join(UPLOAD_DIR,image.file_name);
            try { await fs.access(file); } catch (_) { continue; }
            const meta = await readPhotoMetadata(file);
            await pool.query(`UPDATE report_images SET
                taken_at=COALESCE(taken_at,($2::text || $3::text)::timestamptz,$2::timestamp AT TIME ZONE $4::text),
                latitude=COALESCE(latitude,$5),longitude=COALESCE(longitude,$6),altitude=COALESCE(altitude,$7),
                camera_make=COALESCE(camera_make,$8),camera_model=COALESCE(camera_model,$9),
                exif=COALESCE(exif,$10::jsonb),metadata_processed=TRUE WHERE id=$1`,
                [image.id,meta.takenAtLocal,meta.takenAtOffset,PHOTO_TIME_ZONE,meta.latitude,meta.longitude,
                    meta.altitude,meta.cameraMake,meta.cameraModel,meta.exif ? JSON.stringify(meta.exif) : null]);
            processed++;
        }
    }
    return processed;
}
module.exports = { backfillPhotoMetadata };
