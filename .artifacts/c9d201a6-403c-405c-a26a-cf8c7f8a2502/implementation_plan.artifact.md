# Migrate Backend from Spring Boot (Java) to Node.js (Express)

This plan outlines the migration of the backend service from Spring Boot (Kotlin/Java) to Node.js using Express, JavaScript/TypeScript (or clean JavaScript), and PostgreSQL (`pg`), fully preserving the existing database schema, authentication token/session mechanism, and API endpoints consumed by the Android app.

## User Review Required

> [!IMPORTANT]
> **Tech Stack Choice**: We will use Node.js with **Express**, **pg** (PostgreSQL client), **bcryptjs** for password hashing, and **cors**. This provides a lightweight, fast, and robust backend identical in API contracts and database structure.

> [!IMPORTANT]
> **Database & Docker**: The PostgreSQL database schema (`V1__create_users_and_sessions_tables.sql`) and `docker-compose.yml` will remain fully compatible. The Dockerfile for the backend will be updated to a Node.js Alpine base image.

## Open Questions

- None. The API contracts (`AuthApi.kt`) and database schema are fully defined and tested.

## Proposed Changes

### Backend Replacement (`backend/`)

We will replace the Gradle/Spring Boot files in the `backend/` directory with a Node.js Express application structure:

#### [NEW] [package.json](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/package.json)
- Express dependencies, `pg`, `bcryptjs`, `cors`, `dotenv`.

#### [NEW] [Dockerfile](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/Dockerfile)
- Node.js alpine base image, dependency installation, and start script (`npm start`).

#### [NEW] [src/index.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/index.js)
- Express server setup, middleware (`express.json`, `cors`), health check endpoint (`/actuator/health`), and database connection pool.

#### [NEW] [src/db/database.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/db/database.js)
- PostgreSQL connection pool configuration and database initialization (running migration SQL / seeding admin user).

#### [NEW] [src/services/authService.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/services/authService.js)
- Authentication logic, password verification (bcrypt), token generation (secure random tokens stored in `user_sessions` table matching Spring Boot session tokens).

#### [NEW] [src/controllers/authController.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/controllers/authController.js)
- Handlers for login, getCurrentUser (`/me`), and logout.

#### [NEW] [src/controllers/adminController.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/controllers/adminController.js)
- Handlers for getting all users, creating a user, and resetting user passwords (ADMIN role check).

#### [NEW] [src/middleware/authMiddleware.js](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/backend/src/middleware/authMiddleware.js)
- Bearer token validation middleware checking active, unexpired sessions in `user_sessions`.

#### [MODIFY] [docker-compose.yml](file:///C:/Users/besta/AndroidStudioProjects/NP_Nilson/docker-compose.yml)
- Update backend build context and health check (`wget` or `curl` to `/actuator/health`).

## Verification Plan

### Automated Tests
- Build and test Node.js backend container using `docker compose up --build`.
- Verify health check (`http://localhost:8080/actuator/health`).

### Manual Verification
- Deploy and run Android app against the Node.js backend.
- Test login with admin credentials (`admin@admin.com` / `admin`), fetch current user (`/me`), and perform admin user management operations.
