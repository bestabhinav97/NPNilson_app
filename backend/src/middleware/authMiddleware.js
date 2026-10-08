const authService = require('../services/authService');

async function requireAuth(req, res, next) {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({ error: 'Missing or invalid Authorization header' });
        }

        const token = authHeader.substring(7);
        const user = await authService.getUserByToken(token);

        if (!user) {
            return res.status(401).json({ error: 'Unauthorized or expired session' });
        }

        req.user = user;
        req.token = token;
        next();
    } catch (err) {
        console.error('Auth middleware error:', err);
        return res.status(401).json({ error: 'Authentication failed' });
    }
}

async function requireAdmin(req, res, next) {
    await requireAuth(req, res, async () => {
        if (!req.user || req.user.role !== 'ADMIN') {
            return res.status(403).json({ error: 'Access denied: Admin role required' });
        }
        next();
    });
}

module.exports = {
    requireAuth,
    requireAdmin
};
