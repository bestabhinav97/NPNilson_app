# Task Checklist: Migrate Backend to Node.js

- [ ] Initialize Node.js project (`package.json`, dependencies) `[1]`
- [ ] Create database connection and migration runner (`src/db/database.js`) `[2]`
- [ ] Implement authentication service and token session management (`src/services/authService.js`) `[3]`
- [ ] Implement middleware for session authentication and admin authorization (`src/middleware/authMiddleware.js`) `[4]`
- [ ] Implement Auth and Admin controllers/routes (`src/controllers/`) `[5]`
- [ ] Create Express application entry point (`src/index.js`) `[6]`
- [ ] Update Dockerfile and Docker Compose configuration (`backend/Dockerfile`, `docker-compose.yml`) `[7]`
- [ ] Verify backend compilation, health check, and Android app integration `[8]`
