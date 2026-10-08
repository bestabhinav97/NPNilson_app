const { pool } = require('../db/database');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const crypto = require('crypto');

function formatUserResponse(row) {
    if (!row) return null;
    return {
        id: row.id,
        firstname: row.firstname,
        lastname: row.lastname,
        email: row.email,
        role: row.role,
        store: row.store_id ? {
            storeId: parseInt(row.store_id, 10),
            storeName: row.store_name,
            address: row.address
        } : null
    };
}

async function login(email, password) {
    if (!email || !password) {
        throw new Error('Email and password are required');
    }

    const result = await pool.query(
        `SELECT u.id, u.firstname, u.lastname, u.email, u.password_hash, u.role, u.store_id,
                s.store_name, s.address
         FROM users u
         LEFT JOIN stores s ON u.store_id = s.store_id
         WHERE LOWER(u.email) = LOWER($1)`,
        [email.trim()]
    );

    if (result.rows.length === 0) {
        throw new Error('Invalid email or password');
    }

    const user = result.rows[0];
    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
        throw new Error('Invalid email or password');
    }

    // Generate token
    const token = crypto.randomBytes(32).toString('hex');
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 hours

    await pool.query(
        'INSERT INTO user_sessions (token, user_id, expires_at, revoked) VALUES ($1, $2, $3, false)',
        [token, user.id, expiresAt]
    );

    return {
        token,
        user: formatUserResponse(user)
    };
}

async function getUserByToken(token) {
    if (!token) return null;

    const result = await pool.query(
        `SELECT u.id, u.firstname, u.lastname, u.email, u.role, u.store_id,
                s.store_name, s.address
         FROM user_sessions s
         JOIN users u ON s.user_id = u.id
         LEFT JOIN stores s ON u.store_id = s.store_id
         WHERE s.token = $1 AND s.revoked = false AND s.expires_at > CURRENT_TIMESTAMP`,
        [token]
    );

    if (result.rows.length === 0) return null;
    return formatUserResponse(result.rows[0]);
}

async function logout(token) {
    if (!token) return;
    await pool.query('UPDATE user_sessions SET revoked = true WHERE token = $1', [token]);
}

async function getAllUsers() {
    const result = await pool.query(
        `SELECT u.id, u.firstname, u.lastname, u.email, u.role, u.store_id,
                s.store_name, s.address
         FROM users u
         LEFT JOIN stores s ON u.store_id = s.store_id
         ORDER BY u.created_at DESC`
    );
    return result.rows.map(formatUserResponse);
}

async function createUser(data) {
    const { firstname, lastname, email, password, role = 'USER', storeId = null, store_id = null } = data;
    const targetStoreId = storeId || store_id || (role === 'USER' ? 1 : null);

    if (!firstname || !lastname || !email || !password) {
        throw new Error('All user fields are required');
    }

    const existing = await pool.query('SELECT id FROM users WHERE LOWER(email) = LOWER($1)', [email.trim()]);
    if (existing.rows.length > 0) {
        throw new Error('User with this email already exists');
    }

    const userId = uuidv4();
    const passwordHash = await bcrypt.hash(password, 10);

    await pool.query(
        'INSERT INTO users (id, firstname, lastname, email, password_hash, role, store_id) VALUES ($1, $2, $3, $4, $5, $6, $7)',
        [userId, firstname, lastname, email.trim(), passwordHash, role, targetStoreId]
    );

    const userResult = await pool.query(
        `SELECT u.id, u.firstname, u.lastname, u.email, u.role, u.store_id,
                s.store_name, s.address
         FROM users u
         LEFT JOIN stores s ON u.store_id = s.store_id
         WHERE u.id = $1`,
        [userId]
    );

    return formatUserResponse(userResult.rows[0]);
}

async function resetPassword(userId, newPassword) {
    if (!newPassword) {
        throw new Error('New password is required');
    }

    const passwordHash = await bcrypt.hash(newPassword, 10);
    await pool.query(
        'UPDATE users SET password_hash = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
        [passwordHash, userId]
    );

    const result = await pool.query(
        `SELECT u.id, u.firstname, u.lastname, u.email, u.role, u.store_id,
                s.store_name, s.address
         FROM users u
         LEFT JOIN stores s ON u.store_id = s.store_id
         WHERE u.id = $1`,
        [userId]
    );

    if (result.rows.length === 0) {
        throw new Error('User not found');
    }

    return formatUserResponse(result.rows[0]);
}

async function getAllStores() {
    const result = await pool.query('SELECT store_id as "storeId", store_name as "storeName", address FROM stores ORDER BY store_id ASC');
    return result.rows;
}

module.exports = {
    login,
    getUserByToken,
    logout,
    getAllUsers,
    createUser,
    resetPassword,
    getAllStores
};
