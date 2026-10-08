const authService = require('../services/authService');

async function getAllUsers(req, res) {
    try {
        const users = await authService.getAllUsers();
        return res.json(users);
    } catch (err) {
        return res.status(500).json({ error: err.message });
    }
}

async function createUser(req, res) {
    try {
        const user = await authService.createUser(req.body);
        return res.status(201).json(user);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

async function resetPassword(req, res) {
    try {
        const { userId } = req.params;
        const { newPassword } = req.body;
        const user = await authService.resetPassword(userId, newPassword);
        return res.json(user);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

module.exports = {
    getAllUsers,
    createUser,
    resetPassword
};
