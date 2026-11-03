const authService = require('../services/authService');

// ---------------------------------------------------------
// Dashboard
// ---------------------------------------------------------

async function getDashboard(req, res) {
    try {
        const dashboard =
            await authService.getAdminDashboard();

        return res.json(dashboard);
    } catch (err) {
        return res
            .status(500)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Users
// ---------------------------------------------------------

async function getAllUsers(req, res) {
    try {
        const users =
            await authService.getAllUsers();

        return res.json(users);
    } catch (err) {
        return res
            .status(500)
            .json({
                error: err.message
            });
    }
}

async function createUser(req, res) {
    try {
        const user =
            await authService.createUser(
                req.body,
                req.user?.id
            );

        return res
            .status(201)
            .json(user);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function updateUser(req, res) {
    try {
        const { userId } =
            req.params;

        const user =
            await authService.updateUser(
                userId,
                req.body,
                req.user?.id
            );

        return res.json(user);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function setUserActive(req, res) {
    try {
        const { userId } =
            req.params;

        const { active } =
            req.body;

        if (typeof active !== 'boolean') {
            return res
                .status(400)
                .json({
                    error: 'active must be true or false'
                });
        }

        const user =
            await authService.setUserActive(
                userId,
                active,
                req.user?.id
            );

        return res.json(user);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function deleteUser(req, res) {
    try {
        const { userId } =
            req.params;

        const result =
            await authService.deleteUser(
                userId,
                req.user?.id
            );

        return res.json(result);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function resetPassword(req, res) {
    try {
        const { userId } =
            req.params;

        const { newPassword } =
            req.body;

        const user =
            await authService.resetPassword(
                userId,
                newPassword,
                req.user?.id
            );

        return res.json(user);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Stores
// ---------------------------------------------------------

async function createStore(req, res) {
    try {
        const store =
            await authService.createStore(
                req.body,
                req.user?.id
            );

        return res
            .status(201)
            .json(store);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function updateStore(req, res) {
    try {
        const { storeId } =
            req.params;

        const store =
            await authService.updateStore(
                storeId,
                req.body,
                req.user?.id
            );

        return res.json(store);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

async function deleteStore(req, res) {
    try {
        const { storeId } =
            req.params;

        const result =
            await authService.deleteStore(
                storeId,
                req.user?.id
            );

        return res.json(result);
    } catch (err) {
        return res
            .status(400)
            .json({
                error: err.message
            });
    }
}

// ---------------------------------------------------------
// Exports
// ---------------------------------------------------------

module.exports = {
    getDashboard,

    getAllUsers,
    createUser,
    updateUser,
    setUserActive,
    deleteUser,
    resetPassword,

    createStore,
    updateStore,
    deleteStore
};