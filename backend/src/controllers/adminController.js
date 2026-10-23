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

async function updateUser(req, res) {
    try {
        const { userId } = req.params;
        const user = await authService.updateUser(userId, req.body);
        return res.json(user);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

async function deleteUser(req, res) {
    try {
        const { userId } = req.params;
        const result = await authService.deleteUser(userId, req.user?.id);
        return res.json(result);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

async function createStore(req, res) {
    try {
        const store = await authService.createStore(req.body);
        return res.status(201).json(store);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

async function updateStore(req, res) {
    try {
        const { storeId } = req.params;
        const store = await authService.updateStore(storeId, req.body);
        return res.json(store);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

async function deleteStore(req, res) {
    try {
        const { storeId } = req.params;
        const result = await authService.deleteStore(storeId);
        return res.json(result);
    } catch (err) {
        return res.status(400).json({ error: err.message });
    }
}

module.exports = {
    getAllUsers,
    createUser,
    updateUser,
    deleteUser,
    resetPassword,
    createStore,
    updateStore,
    deleteStore
};
