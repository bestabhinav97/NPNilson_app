const authService = require('../services/authService');

async function login(req, res) {
    try {
        const { email, password } = req.body;
        const result = await authService.login(email, password);
        return res.json(result);
    } catch (err) {
        return res.status(401).json({ error: err.message || 'Invalid email or password' });
    }
}

async function getCurrentUser(req, res) {
    try {
        return res.json(req.user);
    } catch (err) {
        return res.status(500).json({ error: err.message });
    }
}

async function logout(req, res) {
    try {
        await authService.logout(req.token);
        return res.json({ message: 'Logged out successfully' });
    } catch (err) {
        return res.status(500).json({ error: err.message });
    }
}

module.exports = {
    login,
    getCurrentUser,
    logout
};
