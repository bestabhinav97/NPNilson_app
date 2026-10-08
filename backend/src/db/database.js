const { Pool } = require('pg');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

const dbUser = process.env.SPRING_DATASOURCE_USERNAME || process.env.POSTGRES_USER || 'np_db_user';
const dbPassword = process.env.SPRING_DATASOURCE_PASSWORD || process.env.POSTGRES_PASSWORD || 'np_db_password';
const dbHost = process.env.POSTGRES_HOST || 'postgres';
const dbPort = process.env.POSTGRES_PORT || 5432;
const dbName = process.env.POSTGRES_DB || 'np_nilson';

const pool = new Pool({
    host: dbHost,
    port: Number(dbPort),
    database: dbName,
    user: dbUser,
    password: dbPassword,
});

async function initDatabase() {
    const client = await pool.connect();
    try {
        await client.query(`
            CREATE TABLE IF NOT EXISTS users (
                id UUID PRIMARY KEY,
                firstname VARCHAR(100) NOT NULL,
                lastname VARCHAR(100) NOT NULL,
                email VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        `);

        await client.query(`
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
        `);

        await client.query(`
            CREATE TABLE IF NOT EXISTS user_sessions (
                token VARCHAR(128) PRIMARY KEY,
                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                revoked BOOLEAN NOT NULL DEFAULT FALSE
            );
        `);

        await client.query(`
            CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
        `);

        await client.query(`
            CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);
        `);

        // Seed admin users
        const adminEmail1 = process.env.SEED_ADMIN_EMAIL || 'admin@npnilsson.se';
        const adminEmail2 = 'admin@admin.com';
        const adminPassword = process.env.SEED_ADMIN_PASSWORD || 'AdminSecurePassword123!';
        const adminPasswordAlt = 'admin';
        const hashed1 = await bcrypt.hash(adminPassword, 10);
        const hashed2 = await bcrypt.hash(adminPasswordAlt, 10);

        const check1 = await client.query('SELECT id FROM users WHERE LOWER(email) = LOWER($1)', [adminEmail1]);
        if (check1.rows.length === 0) {
            await client.query(
                'INSERT INTO users (id, firstname, lastname, email, password_hash, role) VALUES ($1, $2, $3, $4, $5, $6)',
                [uuidv4(), 'NP', 'Admin', adminEmail1, hashed1, 'ADMIN']
            );
            console.log(`Seeded admin user: ${adminEmail1}`);
        }

        const check2 = await client.query('SELECT id FROM users WHERE LOWER(email) = LOWER($1)', [adminEmail2]);
        if (check2.rows.length === 0) {
            await client.query(
                'INSERT INTO users (id, firstname, lastname, email, password_hash, role) VALUES ($1, $2, $3, $4, $5, $6)',
                [uuidv4(), 'NP', 'Admin', adminEmail2, hashed2, 'ADMIN']
            );
            console.log(`Seeded admin user: ${adminEmail2}`);
        }

        console.log('Database initialized and seeded successfully.');
    } catch (err) {
        console.error('Database initialization error:', err);
        throw err;
    } finally {
        client.release();
    }
}

module.exports = {
    pool,
    initDatabase,
};
