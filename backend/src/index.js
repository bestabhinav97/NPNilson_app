const express = require('express');
const cors = require('cors');
require('dotenv').config();

const { initDatabase } = require('./db/database');
const authController = require('./controllers/authController');
const adminController = require('./controllers/adminController');
const authService = require('./services/authService');
const { requireAuth, requireAdmin } = require('./middleware/authMiddleware');

const app = express();
const PORT = process.env.SERVER_PORT || process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

// Health check endpoint (matches Spring Boot Actuator health)
app.get('/actuator/health', (req, res) => {
    res.json({ status: 'UP' });
});

app.get('/', (req, res) => {
    res.json({ message: 'NP Nilsson Node.js Backend is running' });
});

// Auth Routes
app.post('/api/auth/login', authController.login);
app.get('/api/auth/me', requireAuth, authController.getCurrentUser);
app.post('/api/auth/logout', requireAuth, authController.logout);

// Stores Route
app.get('/api/stores', requireAuth, async (req, res) => {
    try {
        const stores = await authService.getAllStores();
        res.json(stores);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin Routes
app.get('/api/admin/users', requireAdmin, adminController.getAllUsers);
app.post('/api/admin/users', requireAdmin, adminController.createUser);
app.post('/api/admin/users/:userId/reset-password', requireAdmin, adminController.resetPassword);

// Global Error Handler
app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({ error: err.message || 'Internal server error' });
});

async function startServer() {
    await initDatabase();
    app.listen(PORT, '0.0.0.0', () => {
        console.log(`Backend server running on port ${PORT}`);
    });
}

startServer().catch(err => {
    console.error('Failed to start server:', err);
    process.exit(1);
});
