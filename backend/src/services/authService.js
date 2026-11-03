const { pool } = require('../db/database');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const crypto = require('crypto');

// ---------------------------------------------------------
// Helpers
// ---------------------------------------------------------

function formatUserResponse(row) {
    if (!row) {
        return null;
    }

    return {
        id: row.id,
        firstname: row.firstname,
        lastname: row.lastname,
        email: row.email,
        role: row.role,
        isActive: row.is_active !== false,
        store: row.store_id
            ? {
                storeId: parseInt(row.store_id, 10),
                storeName: row.store_name,
                address: row.address
            }
            : null
    };
}

function normalizeRole(role) {
    const normalized = String(role || '').trim().toUpperCase();

    if (!['ADMIN', 'USER'].includes(normalized)) {
        throw new Error('Invalid role');
    }

    return normalized;
}

async function ensureStoreExists(storeId) {
    if (storeId === null || storeId === undefined) {
        return;
    }

    const result = await pool.query(
        `
            SELECT store_id
            FROM stores
            WHERE store_id = $1
        `,
        [storeId]
    );

    if (result.rows.length === 0) {
        throw new Error('Store not found');
    }
}

async function getUserById(userId) {
    const result = await pool.query(
        `
            SELECT
                u.id,
                u.firstname,
                u.lastname,
                u.email,
                u.role,
                u.is_active,
                u.store_id,
                s.store_name,
                s.address
            FROM users u
            LEFT JOIN stores s
                ON u.store_id = s.store_id
            WHERE u.id = $1::uuid
        `,
        [userId]
    );

    if (result.rows.length === 0) {
        return null;
    }

    return result.rows[0];
}

async function countActiveAdmins() {
    const result = await pool.query(
        `
            SELECT COUNT(*)::int AS count
            FROM users
            WHERE role = 'ADMIN'
              AND is_active = TRUE
        `
    );

    return Number(result.rows[0].count);
}

// Audit history should never make the main admin action fail.
async function recordAdminActivity(
    actorUserId,
    action,
    targetType,
    targetId = null,
    details = null
) {
    try {
        await pool.query(
            `
                INSERT INTO admin_activity (
                    actor_user_id,
                    action,
                    target_type,
                    target_id,
                    details
                )
                VALUES (
                    $1::uuid,
                    $2,
                    $3,
                    $4,
                    $5
                )
            `,
            [
                actorUserId || null,
                action,
                targetType,
                targetId != null ? String(targetId) : null,
                details || null
            ]
        );
    } catch (error) {
        console.error(
            'Failed to record admin activity:',
            error
        );
    }
}

// ---------------------------------------------------------
// Authentication
// ---------------------------------------------------------

async function login(email, password) {
    if (!email || !password) {
        throw new Error(
            'Email and password are required'
        );
    }

    const result = await pool.query(
        `
            SELECT
                u.id,
                u.firstname,
                u.lastname,
                u.email,
                u.password_hash,
                u.role,
                u.is_active,
                u.store_id,
                s.store_name,
                s.address
            FROM users u
            LEFT JOIN stores s
                ON u.store_id = s.store_id
            WHERE LOWER(u.email) = LOWER($1)
        `,
        [email.trim()]
    );

    if (result.rows.length === 0) {
        throw new Error(
            'Invalid email or password'
        );
    }

    const user = result.rows[0];

    const passwordMatch =
        await bcrypt.compare(
            password,
            user.password_hash
        );

    if (!passwordMatch) {
        throw new Error(
            'Invalid email or password'
        );
    }

    if (user.is_active === false) {
        throw new Error(
            'This account has been deactivated'
        );
    }

    const token =
        crypto
            .randomBytes(32)
            .toString('hex');

    const expirationHours =
        Number(
            process.env.SESSION_EXPIRATION_HOURS ||
            24
        );

    const expiresAt =
        new Date(
            Date.now() +
            expirationHours *
            60 *
            60 *
            1000
        );

    await pool.query(
        `
            INSERT INTO user_sessions (
                token,
                user_id,
                expires_at,
                revoked
            )
            VALUES (
                $1,
                $2,
                $3,
                FALSE
            )
        `,
        [
            token,
            user.id,
            expiresAt
        ]
    );

    return {
        token,
        user: formatUserResponse(user)
    };
}

async function getUserByToken(token) {
    if (!token) {
        return null;
    }

    const result = await pool.query(
        `
            SELECT
                u.id,
                u.firstname,
                u.lastname,
                u.email,
                u.role,
                u.is_active,
                u.store_id,
                s.store_name,
                s.address
            FROM user_sessions us
            JOIN users u
                ON us.user_id = u.id
            LEFT JOIN stores s
                ON u.store_id = s.store_id
            WHERE
                us.token = $1
                AND us.revoked = FALSE
                AND us.expires_at > CURRENT_TIMESTAMP
                AND u.is_active = TRUE
        `,
        [token]
    );

    if (result.rows.length === 0) {
        return null;
    }

    return formatUserResponse(
        result.rows[0]
    );
}

async function logout(token) {
    if (!token) {
        return;
    }

    await pool.query(
        `
            UPDATE user_sessions
            SET revoked = TRUE
            WHERE token = $1
        `,
        [token]
    );
}

// ---------------------------------------------------------
// Admin dashboard
// ---------------------------------------------------------

async function getAdminDashboard() {
    const countsResult =
        await pool.query(
            `
                SELECT
                    (
                        SELECT COUNT(*)::int
                        FROM users
                    ) AS total_users,

                    (
                        SELECT COUNT(*)::int
                        FROM users
                        WHERE is_active = TRUE
                    ) AS active_users,

                    (
                        SELECT COUNT(*)::int
                        FROM users
                        WHERE role = 'ADMIN'
                          AND is_active = TRUE
                    ) AS admin_count,

                    (
                        SELECT COUNT(*)::int
                        FROM stores
                    ) AS store_count,

                    (
                        SELECT COUNT(*)::int
                        FROM reports
                        WHERE status = 'OPEN'
                    ) AS open_reports,

                    (
                        SELECT COUNT(*)::int
                        FROM reports
                        WHERE status = 'IN_PROGRESS'
                    ) AS in_progress_reports,

                    (
                        SELECT COUNT(*)::int
                        FROM reports
                        WHERE status = 'RESOLVED'
                    ) AS resolved_reports
            `
        );

    const storeStatsResult =
        await pool.query(
            `
                SELECT
                    s.store_id,
                    s.store_name,
                    s.address,

                    (
                        SELECT COUNT(*)::int
                        FROM users u
                        WHERE u.store_id = s.store_id
                    ) AS user_count,

                    (
                        SELECT COUNT(*)::int
                        FROM reports r
                        WHERE r.store_id = s.store_id
                          AND r.status = 'OPEN'
                    ) AS open_reports

                FROM stores s
                ORDER BY s.store_name ASC
            `
        );

    const activityResult =
        await pool.query(
            `
                SELECT
                    a.id,
                    a.action,
                    a.target_type,
                    a.target_id,
                    a.details,
                    a.created_at,

                    COALESCE(
                        NULLIF(
                            TRIM(
                                CONCAT(
                                    u.firstname,
                                    ' ',
                                    u.lastname
                                )
                            ),
                            ''
                        ),
                        'System'
                    ) AS actor_name

                FROM admin_activity a

                LEFT JOIN users u
                    ON a.actor_user_id = u.id

                ORDER BY a.created_at DESC

                LIMIT 30
            `
        );

    const counts =
        countsResult.rows[0];

    return {
        totalUsers:
            Number(counts.total_users),

        activeUsers:
            Number(counts.active_users),

        adminCount:
            Number(counts.admin_count),

        storeCount:
            Number(counts.store_count),

        openReports:
            Number(counts.open_reports),

        inProgressReports:
            Number(counts.in_progress_reports),

        resolvedReports:
            Number(counts.resolved_reports),

        stores:
            storeStatsResult.rows.map(
                row => ({
                    storeId:
                        Number(row.store_id),

                    storeName:
                        row.store_name,

                    address:
                        row.address,

                    userCount:
                        Number(row.user_count),

                    openReports:
                        Number(row.open_reports)
                })
            ),

        recentActivity:
            activityResult.rows.map(
                row => ({
                    id:
                        Number(row.id),

                    action:
                        row.action,

                    targetType:
                        row.target_type,

                    targetId:
                        row.target_id,

                    details:
                        row.details,

                    createdAt:
                        row.created_at,

                    actorName:
                        row.actor_name
                })
            )
    };
}

// ---------------------------------------------------------
// Users
// ---------------------------------------------------------

async function getAllUsers() {
    const result = await pool.query(
        `
            SELECT
                u.id,
                u.firstname,
                u.lastname,
                u.email,
                u.role,
                u.is_active,
                u.store_id,
                s.store_name,
                s.address

            FROM users u

            LEFT JOIN stores s
                ON u.store_id = s.store_id

            ORDER BY
                u.is_active DESC,
                u.created_at DESC
        `
    );

    return result.rows.map(
        formatUserResponse
    );
}

async function createUser(
    data,
    actorUserId = null
) {
    const {
        firstname,
        lastname,
        email,
        password,
        role = 'USER',
        storeId,
        store_id
    } = data;

    if (
        !firstname ||
        !lastname ||
        !email ||
        !password
    ) {
        throw new Error(
            'All user fields are required'
        );
    }

    const normalizedRole =
        normalizeRole(role);

    const targetStoreId =
        storeId !== undefined
            ? storeId
            : store_id !== undefined
                ? store_id
                : null;

    if (password.length < 8) {
        throw new Error(
            'Password must be at least 8 characters'
        );
    }

    await ensureStoreExists(
        targetStoreId
    );

    const existing =
        await pool.query(
            `
                SELECT id
                FROM users
                WHERE LOWER(email) = LOWER($1)
            `,
            [email.trim()]
        );

    if (
        existing.rows.length > 0
    ) {
        throw new Error(
            'User with this email already exists'
        );
    }

    const userId =
        uuidv4();

    const passwordHash =
        await bcrypt.hash(
            password,
            10
        );

    await pool.query(
        `
            INSERT INTO users (
                id,
                firstname,
                lastname,
                email,
                password_hash,
                role,
                store_id,
                is_active
            )
            VALUES (
                $1,
                $2,
                $3,
                $4,
                $5,
                $6,
                $7,
                TRUE
            )
        `,
        [
            userId,
            firstname.trim(),
            lastname.trim(),
            email.trim(),
            passwordHash,
            normalizedRole,
            targetStoreId
        ]
    );

    const createdUser =
        await getUserById(
            userId
        );

    await recordAdminActivity(
        actorUserId,
        'USER_CREATED',
        'USER',
        userId,
        `${firstname.trim()} ${lastname.trim()} (${email.trim()})`
    );

    return formatUserResponse(
        createdUser
    );
}

async function updateUser(
    userId,
    data,
    actorUserId = null
) {
    const {
        firstname,
        lastname,
        email,
        role,
        storeId,
        store_id
    } = data;

    if (
        !firstname ||
        !lastname ||
        !email ||
        !role
    ) {
        throw new Error(
            'First name, last name, email, and role are required'
        );
    }

    const normalizedRole =
        normalizeRole(role);

    const existingUser =
        await getUserById(
            userId
        );

    if (!existingUser) {
        throw new Error(
            'User not found'
        );
    }

    const targetStoreId =
        storeId !== undefined
            ? storeId
            : store_id !== undefined
                ? store_id
                : null;

    await ensureStoreExists(
        targetStoreId
    );

    if (
        existingUser.role === 'ADMIN' &&
        existingUser.is_active === true &&
        normalizedRole !== 'ADMIN'
    ) {
        const activeAdmins =
            await countActiveAdmins();

        if (activeAdmins <= 1) {
            throw new Error(
                'You cannot demote the final active administrator'
            );
        }
    }

    const emailConflict =
        await pool.query(
            `
                SELECT id
                FROM users
                WHERE
                    LOWER(email) = LOWER($1)
                    AND id != $2::uuid
            `,
            [
                email.trim(),
                userId
            ]
        );

    if (
        emailConflict.rows.length > 0
    ) {
        throw new Error(
            'Another user with this email already exists'
        );
    }

    await pool.query(
        `
            UPDATE users

            SET
                firstname = $1,
                lastname = $2,
                email = $3,
                role = $4,
                store_id = $5,
                updated_at = CURRENT_TIMESTAMP

            WHERE id = $6::uuid
        `,
        [
            firstname.trim(),
            lastname.trim(),
            email.trim(),
            normalizedRole,
            targetStoreId,
            userId
        ]
    );

    const updatedUser =
        await getUserById(
            userId
        );

    await recordAdminActivity(
        actorUserId,
        'USER_UPDATED',
        'USER',
        userId,
        `${firstname.trim()} ${lastname.trim()} (${email.trim()})`
    );

    return formatUserResponse(
        updatedUser
    );
}

async function setUserActive(
    userId,
    active,
    requesterId = null
) {
    if (
        typeof active !== 'boolean'
    ) {
        throw new Error(
            'Active must be true or false'
        );
    }

    const user =
        await getUserById(
            userId
        );

    if (!user) {
        throw new Error(
            'User not found'
        );
    }

    if (
        active === false &&
        requesterId &&
        String(userId) ===
        String(requesterId)
    ) {
        throw new Error(
            'You cannot deactivate your own account'
        );
    }

    if (
        active === false &&
        user.role === 'ADMIN' &&
        user.is_active === true
    ) {
        const activeAdmins =
            await countActiveAdmins();

        if (activeAdmins <= 1) {
            throw new Error(
                'You cannot deactivate the final active administrator'
            );
        }
    }

    await pool.query(
        `
            UPDATE users

            SET
                is_active = $1,
                updated_at = CURRENT_TIMESTAMP

            WHERE id = $2::uuid
        `,
        [
            active,
            userId
        ]
    );

    if (!active) {
        await pool.query(
            `
                UPDATE user_sessions
                SET revoked = TRUE
                WHERE user_id = $1::uuid
            `,
            [userId]
        );
    }

    const updatedUser =
        await getUserById(
            userId
        );

    await recordAdminActivity(
        requesterId,
        active
            ? 'USER_ACTIVATED'
            : 'USER_DEACTIVATED',
        'USER',
        userId,
        `${user.firstname} ${user.lastname} (${user.email})`
    );

    return formatUserResponse(
        updatedUser
    );
}

async function resetPassword(
    userId,
    newPassword,
    actorUserId = null
) {
    if (!newPassword) {
        throw new Error(
            'New password is required'
        );
    }

    if (newPassword.length < 8) {
        throw new Error(
            'New password must be at least 8 characters'
        );
    }

    const user =
        await getUserById(
            userId
        );

    if (!user) {
        throw new Error(
            'User not found'
        );
    }

    const passwordHash =
        await bcrypt.hash(
            newPassword,
            10
        );

    await pool.query(
        `
            UPDATE users

            SET
                password_hash = $1,
                updated_at = CURRENT_TIMESTAMP

            WHERE id = $2::uuid
        `,
        [
            passwordHash,
            userId
        ]
    );

    // Require the user to sign in again with the new password.
    await pool.query(
        `
            UPDATE user_sessions
            SET revoked = TRUE
            WHERE user_id = $1::uuid
        `,
        [userId]
    );

    const updatedUser =
        await getUserById(
            userId
        );

    await recordAdminActivity(
        actorUserId,
        'USER_PASSWORD_RESET',
        'USER',
        userId,
        `${user.firstname} ${user.lastname} (${user.email})`
    );

    return formatUserResponse(
        updatedUser
    );
}

async function deleteUser(
    userId,
    requesterId
) {
    if (
        requesterId &&
        String(userId) ===
        String(requesterId)
    ) {
        throw new Error(
            'You cannot delete your own account'
        );
    }

    const user =
        await getUserById(
            userId
        );

    if (!user) {
        throw new Error(
            'User not found'
        );
    }

    if (
        user.role === 'ADMIN' &&
        user.is_active === true
    ) {
        const activeAdmins =
            await countActiveAdmins();

        if (activeAdmins <= 1) {
            throw new Error(
                'You cannot delete the final active administrator'
            );
        }
    }

    const reportCheck =
        await pool.query(
            `
                SELECT COUNT(*)::int AS count
                FROM reports
                WHERE user_id = $1::uuid
            `,
            [userId]
        );

    const reportCount =
        Number(
            reportCheck.rows[0].count
        );

    if (reportCount > 0) {
        throw new Error(
            'This user has report history and cannot be deleted. Deactivate the account instead.'
        );
    }

    await pool.query(
        `
            DELETE FROM users
            WHERE id = $1::uuid
        `,
        [userId]
    );

    await recordAdminActivity(
        requesterId,
        'USER_DELETED',
        'USER',
        userId,
        `${user.firstname} ${user.lastname} (${user.email})`
    );

    return {
        success: true,
        message: 'User deleted successfully'
    };
}

// ---------------------------------------------------------
// Stores
// ---------------------------------------------------------

async function getAllStores() {
    const result =
        await pool.query(
            `
                SELECT
                    store_id AS "storeId",
                    store_name AS "storeName",
                    address

                FROM stores

                ORDER BY
                    store_name ASC
            `
        );

    return result.rows;
}

async function createStore(
    data,
    actorUserId = null
) {
    const {
        storeName,
        address
    } = data;

    if (
        !storeName ||
        !address
    ) {
        throw new Error(
            'Store name and address are required'
        );
    }

    const result =
        await pool.query(
            `
                INSERT INTO stores (
                    store_name,
                    address
                )
                VALUES (
                    $1,
                    $2
                )
                RETURNING
                    store_id AS "storeId",
                    store_name AS "storeName",
                    address
            `,
            [
                storeName.trim(),
                address.trim()
            ]
        );

    const store =
        result.rows[0];

    await recordAdminActivity(
        actorUserId,
        'STORE_CREATED',
        'STORE',
        store.storeId,
        `${store.storeName} - ${store.address}`
    );

    return store;
}

async function updateStore(
    storeId,
    data,
    actorUserId = null
) {
    const {
        storeName,
        address
    } = data;

    if (
        !storeName ||
        !address
    ) {
        throw new Error(
            'Store name and address are required'
        );
    }

    const existing =
        await pool.query(
            `
                SELECT
                    store_id,
                    store_name,
                    address
                FROM stores
                WHERE store_id = $1
            `,
            [storeId]
        );

    if (
        existing.rows.length === 0
    ) {
        throw new Error(
            'Store not found'
        );
    }

    const result =
        await pool.query(
            `
                UPDATE stores

                SET
                    store_name = $1,
                    address = $2

                WHERE store_id = $3

                RETURNING
                    store_id AS "storeId",
                    store_name AS "storeName",
                    address
            `,
            [
                storeName.trim(),
                address.trim(),
                storeId
            ]
        );

    const updatedStore =
        result.rows[0];

    await recordAdminActivity(
        actorUserId,
        'STORE_UPDATED',
        'STORE',
        storeId,
        `${updatedStore.storeName} - ${updatedStore.address}`
    );

    return updatedStore;
}

async function deleteStore(
    storeId,
    actorUserId = null
) {
    const storeCheck =
        await pool.query(
            `
                SELECT
                    store_id,
                    store_name,
                    address
                FROM stores
                WHERE store_id = $1
            `,
            [storeId]
        );

    if (
        storeCheck.rows.length === 0
    ) {
        throw new Error(
            'Store not found'
        );
    }

    const store =
        storeCheck.rows[0];

    const relationCheck =
        await pool.query(
            `
                SELECT
                    (
                        SELECT COUNT(*)::int
                        FROM users
                        WHERE store_id = $1
                    ) AS user_count,

                    (
                        SELECT COUNT(*)::int
                        FROM reports
                        WHERE store_id = $1
                    ) AS report_count
            `,
            [storeId]
        );

    const userCount =
        Number(
            relationCheck.rows[0].user_count
        );

    const reportCount =
        Number(
            relationCheck.rows[0].report_count
        );

    if (
        userCount > 0
    ) {
        throw new Error(
            'This store still has assigned users. Reassign them before deleting the store.'
        );
    }

    if (
        reportCount > 0
    ) {
        throw new Error(
            'This store has report history and cannot be deleted.'
        );
    }

    await pool.query(
        `
            DELETE FROM stores
            WHERE store_id = $1
        `,
        [storeId]
    );

    await recordAdminActivity(
        actorUserId,
        'STORE_DELETED',
        'STORE',
        storeId,
        `${store.store_name} - ${store.address}`
    );

    return {
        success: true,
        message: 'Store deleted successfully'
    };
}

// ---------------------------------------------------------
// Exports
// ---------------------------------------------------------

module.exports = {
    login,
    getUserByToken,
    logout,

    getAdminDashboard,
    recordAdminActivity,

    getAllUsers,
    createUser,
    updateUser,
    setUserActive,
    deleteUser,
    resetPassword,

    getAllStores,
    createStore,
    updateStore,
    deleteStore
};