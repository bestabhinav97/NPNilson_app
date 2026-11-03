// Additive, repeatable migrations. Existing reports retain their original category and field labels.
async function migrateReportFeatures(client) {
    await client.query('BEGIN');
    try {
        await client.query(`
            ALTER TABLE report_images ADD COLUMN IF NOT EXISTS metadata_processed BOOLEAN NOT NULL DEFAULT FALSE;
            CREATE TABLE IF NOT EXISTS report_categories (
                id UUID PRIMARY KEY, code VARCHAR(80) UNIQUE NOT NULL,
                name VARCHAR(100) NOT NULL, description VARCHAR(500) NOT NULL DEFAULT '',
                icon VARCHAR(30) NOT NULL DEFAULT 'OTHER', sort_order INTEGER NOT NULL DEFAULT 0,
                is_active BOOLEAN NOT NULL DEFAULT TRUE, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            CREATE TABLE IF NOT EXISTS category_fields (
                id UUID PRIMARY KEY, category_id UUID NOT NULL REFERENCES report_categories(id),
                label VARCHAR(100) NOT NULL, type VARCHAR(20) NOT NULL
                    CHECK (type IN ('TEXT','MULTILINE','NUMBER','DATE','SELECT','CHECKBOX')),
                required BOOLEAN NOT NULL DEFAULT FALSE, options JSONB NOT NULL DEFAULT '[]',
                validation JSONB NOT NULL DEFAULT '{}', sort_order INTEGER NOT NULL DEFAULT 0,
                is_active BOOLEAN NOT NULL DEFAULT TRUE
            );
            CREATE INDEX IF NOT EXISTS idx_category_fields_category ON category_fields(category_id);
            INSERT INTO report_categories (id,code,name,icon,sort_order) VALUES
                ('00000000-0000-4000-8000-000000000001','DAMAGED_GOODS','Skadat gods','DAMAGED_GOODS',0),
                ('00000000-0000-4000-8000-000000000002','SAFETY','Säkerhet','SAFETY',1),
                ('00000000-0000-4000-8000-000000000003','MAINTENANCE','Underhåll','MAINTENANCE',2),
                ('00000000-0000-4000-8000-000000000004','OTHER','Övrigt','OTHER',3)
            ON CONFLICT (code) DO NOTHING;
            ALTER TABLE reports DROP CONSTRAINT IF EXISTS reports_category_check;
            ALTER TABLE reports ALTER COLUMN category TYPE VARCHAR(80);
            ALTER TABLE reports ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES report_categories(id);
            ALTER TABLE reports ADD COLUMN IF NOT EXISTS category_name VARCHAR(100);
            ALTER TABLE reports ADD COLUMN IF NOT EXISTS category_icon VARCHAR(30);
            UPDATE reports r SET category_id=c.id, category_name=c.name, category_icon=c.icon
                FROM report_categories c WHERE r.category=c.code AND r.category_id IS NULL;
            CREATE TABLE IF NOT EXISTS report_field_values (
                report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
                field_id UUID NOT NULL REFERENCES category_fields(id),
                definition JSONB NOT NULL, value JSONB NOT NULL,
                PRIMARY KEY(report_id,field_id)
            );
            CREATE TABLE IF NOT EXISTS report_messages (
                id UUID PRIMARY KEY, report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
                sender_id UUID REFERENCES users(id) ON DELETE SET NULL,
                sender_name VARCHAR(205) NOT NULL, sender_role VARCHAR(20) NOT NULL,
                body TEXT NOT NULL CHECK(length(body) BETWEEN 1 AND 4000),
                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            CREATE INDEX IF NOT EXISTS idx_report_messages_report ON report_messages(report_id,created_at,id);
        `);
        await client.query('COMMIT');
    } catch (err) {
        await client.query('ROLLBACK');
        throw err;
    }
}
module.exports = { migrateReportFeatures };
