# NP Nilsson Android App & Backend

Welcome to the **NP Nilsson** project! This repository contains an Android application (built with Jetpack Compose) and a Node.js Express backend connected to a PostgreSQL database.

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
   - Builds and starts the Node.js Express backend (`np_backend`) on port `8080`, initializes database tables, and seeds an admin account.

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

## 🔑 Default Admin Credentials

Log into the app using the seeded admin account:
* **Email:** `admin@admin.com`
* **Password:** `admin`

---

## 🛠️ Tech Stack
* **Frontend:** Android (Kotlin, Jetpack Compose, Retrofit, Material 3)
* **Backend:** Node.js, Express, PostgreSQL (`pg`), `bcryptjs`
* **Infrastructure:** Docker & Docker Compose
