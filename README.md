# NP Nilsson Android App & Backend

Welcome to the **NP Nilsson** project! This repository contains an Android application (built with Jetpack Compose) and a Node.js Express backend connected to a PostgreSQL database.

---

## ✨ Features

### 📋 Report Management
* **Create Reports**: Submit incident reports across multiple categories (*Damaged Goods*, *Safety*, *Maintenance*, *Other*).
* **Camera & Media Capture**: Capture photos using the in-app camera or pick images from the device gallery.
* **EXIF Metadata Preservation**: Automatically extracts and stores EXIF metadata (camera model, GPS location, timestamp) with uploaded photos.
* **Status Workflow**: Filter, track, and update report statuses (*Open*, *In Progress*, *Resolved*).

### 🛡️ Admin Management Module
* **User Management**: Search, filter, create, edit, or delete user accounts and assign user roles (`USER` / `ADMIN`).
* **Password Reset**: Directly reset user passwords from the admin panel.
* **Store Branch Management**: Create and manage store branches and assign staff members to specific locations.
* **Centralized Dashboard**: Dedicated Admin navigation tab for system-wide oversight.

---

## 🚀 Getting Started (Step-by-Step from Git Clone)

Follow these steps to clone and set up the project on your machine:

### Step 1: Clone the Repository
```bash
git clone <repository-url>
cd NP_Nilson
```

### Step 2: Set Up and Run the Backend (Docker Compose)
The easiest way to run the PostgreSQL database, pgAdmin, and the Node.js backend is via Docker Compose.

1. Make sure **Docker Desktop** is open and running on your PC.
2. Run the following command in the project root directory:
   ```bash
   docker compose up --build -d
   ```
3. This automatically:
   - Starts PostgreSQL (`np_postgres`) on port `5432`.
   - Starts pgAdmin (`np_pgadmin`) on port `5050`.
   - Builds and starts the Node.js Express backend (`np_backend`) on port `8080`, initializes database tables, and seeds default accounts and stores.

To verify the backend is running, open your browser and visit:
👉 **[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)** (should return `{"status":"UP"}`).

---

### Step 3: Run the Android App

1. Open **Android Studio**.
2. Select **Open** and choose the cloned `NP_Nilson` folder.
3. Allow Gradle to sync successfully.
4. Select your target device (an Android Emulator or a physical phone connected via USB).
5. Click **Run (`Shift + F10`)** to build and install the app.

#### Connecting the App to the Backend:

* **If using an Android Emulator:**
  The app defaults to `http://10.0.2.2:8080/`, which automatically connects to your PC's localhost.

* **If using a Physical Phone connected via USB:**
  1. Ensure **USB Debugging** is enabled on your phone.
  2. Run ADB port forwarding in your terminal so your phone can reach your PC's port `8080`:
     ```bash
     adb reverse tcp:8080 tcp:8080
     ```
  3. Open the app on your phone. On the **Login screen**, tap the **Settings icon (gear icon)** in the top right corner.
  4. Set the Server Base URL to:
     ```text
     http://127.0.0.1:8080/
     ```
  5. Tap **Save**.

---

## 🔑 Default Seeded Credentials

Log into the app using the pre-seeded accounts:

* **Admin Account:**
  * **Email:** `admin@admin.com`
  * **Password:** `admin`

* **Test Employee Account:**
  * **Email:** `test@test.com`
  * **Password:** `test`

---

## 🛠️ Tech Stack
* **Frontend:** Android (Kotlin, Jetpack Compose, Retrofit, Material 3)
* **Backend:** Node.js, Express, PostgreSQL (`pg`), `bcryptjs`, Multer, Exifr
* **Infrastructure:** Docker & Docker Compose

## Configurable reports and conversations

Rebuild the backend after updating this branch so the running API and database match the app:

```powershell
docker compose up --build -d backend
```

Startup applies repeatable migrations without deleting reports. The four original categories are seeded into `report_categories`; admins can add, edit, reorder, or archive categories under **Administration → Kategorier**. Category fields support text, multiline text, numbers, dates, dropdowns, and checkboxes, including required fields and numeric/text limits. Removing a used field archives its definition. Existing reports retain snapshots of category labels, field definitions, and answers.

Report details show photo capture time, camera, and GPS when present, with fullscreen viewing and a map action. Existing uploaded files are inspected once for recoverable EXIF metadata; metadata absent from the file cannot be reconstructed. Missing files are retried at the next startup.

Each report has a text conversation accessible only to its submitter and admins. The app refreshes while the report is visible, supports older-message pagination, and reuses a message UUID when retrying a failed send to avoid duplicate messages. Attachments, read receipts, and push notifications are not included.

### Verification

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
docker compose exec backend npm test
docker compose exec backend npm audit --omit=dev
```

Backend integration tests use a temporary PostgreSQL schema and upload directory and remove both afterward. Tests require schema creation permission in the configured database; the Compose database user has it. Coverage includes migration preservation, dynamic-field validation, archived definitions, photo metadata and backfill, dashboard counts, message authorization, idempotent sends, and pagination.
