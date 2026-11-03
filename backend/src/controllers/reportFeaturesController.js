const { pool } = require('../db/database');
const { validate: isUuid } = require('uuid');
const categories = require('../services/categoryService');
const reports = require('../services/reportService');

function endpoint(handler) {
    return async (req, res) => {
        try { await handler(req,res); }
        catch (err) {
            if (!err.status || err.status >= 500) console.error('Report API:',err);
            res.status(err.status || 500).json({ error: err.status ? err.message : 'Serverfel. Försök igen.' });
        }
    };
}
async function authorizeReport(req) {
    const id = req.params.reportId;
    if (isUuid(id)) {
        const result = await pool.query('SELECT user_id FROM reports WHERE id=$1', [id]);
        if (result.rows.length && (req.user.role === 'ADMIN' || result.rows[0].user_id === req.user.id)) return;
    }
    const err = new Error('Rapporten finns inte'); err.status = 404; throw err;
}
const listCategories = endpoint(async (req,res) => res.json(await categories.listCategories(req.user.role === 'ADMIN' && req.query.includeArchived === 'true')));
const saveCategory = endpoint(async (req,res) => res.status(req.params.categoryId ? 200 : 201).json(await categories.saveCategory(req.params.categoryId, req.body)));
const getReport = endpoint(async (req,res) => {
    await authorizeReport(req);
    res.json(await reports.getReportById(req.params.reportId));
});
const getMessages = endpoint(async (req,res) => {
    await authorizeReport(req);
    const before = req.query.before;
    const after = req.query.after;
    if ((before && !isUuid(before)) || (after && !isUuid(after)) || (before && after)) { const err=new Error('Ogiltig meddelandemarkör'); err.status=400; throw err; }
    const result = await pool.query(`SELECT * FROM (
        SELECT id, report_id AS "reportId", sender_id AS "senderId", sender_name AS "senderName",
            sender_role AS "senderRole", body, created_at AS "createdAt"
        FROM report_messages WHERE report_id=$1
            ${before ? 'AND (created_at,id)<(SELECT created_at,id FROM report_messages WHERE id=$2 AND report_id=$1)' : ''}
            ${after ? 'AND (created_at,id)>(SELECT created_at,id FROM report_messages WHERE id=$2 AND report_id=$1)' : ''}
        ORDER BY created_at ${after ? 'ASC' : 'DESC'},id ${after ? 'ASC' : 'DESC'} LIMIT 50
    ) page ORDER BY "createdAt",id`, (before || after) ? [req.params.reportId,before || after] : [req.params.reportId]);
    res.json(result.rows);
});
const sendMessage = endpoint(async (req,res) => {
    await authorizeReport(req);
    const { id, body } = req.body || {};
    if (!isUuid(id || '') || typeof body !== 'string' || !body.trim() || body.trim().length > 4000) {
        const err = new Error('Skriv ett meddelande på 1–4000 tecken'); err.status=400; throw err;
    }
    const columns = 'id,report_id AS "reportId",sender_id AS "senderId",sender_name AS "senderName",sender_role AS "senderRole",body,created_at AS "createdAt"';
    const result = await pool.query(`INSERT INTO report_messages(id,report_id,sender_id,sender_name,sender_role,body)
        VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT(id) DO NOTHING RETURNING ${columns}`,
        [id,req.params.reportId,req.user.id,`${req.user.firstname} ${req.user.lastname}`,req.user.role,body.trim()]);
    if (result.rows.length) return res.status(201).json(result.rows[0]);
    const existing = await pool.query(`SELECT ${columns} FROM report_messages WHERE id=$1 AND report_id=$2 AND sender_id=$3 AND body=$4`,
        [id,req.params.reportId,req.user.id,body.trim()]);
    if (!existing.rows.length) { const err=new Error('Meddelande-ID används redan'); err.status=409; throw err; }
    res.json(existing.rows[0]);
});
module.exports={listCategories,saveCategory,getReport,getMessages,sendMessage};
