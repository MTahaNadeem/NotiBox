# 📬 NotiBox — Offline Notification Logger & Unsent Message Tracker

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Database](https://img.shields.io/badge/Database-Room%20(SQLite)-4285F4?logo=sqlite&logoColor=white)](#)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Internet)-00C853)](#)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**NotiBox** is a privacy-first, 100% offline Android utility built with modern Android architecture (Jetpack Compose, Kotlin Coroutines, StateFlow, and Room Database). It intercepts, structures, stores, and analyzes all incoming push notifications directly from the Android status bar framework—recovering deleted/unsent messages, tracking distraction metrics, and allowing complete local data export without root.

---

## ✨ Key Features

### 🛡️ 100% Offline & Zero Network Footprint
* Built strictly without `android.permission.INTERNET`.
* Your sensitive chats, OTPs, verification codes, and alerts never leave your physical device.
* All data is stored securely in an isolated, sandboxed local SQLite/Room database.

### 👻 "Unsent" Message Tracker (Ghost Alerts)
* **WhatsApp "Delete for Everyone" Detection:** Automatically flags revoked messages when a replacement *"This message was deleted"* notification arrives, preserving the original message text permanently with a warning badge.
* **Instagram & Messaging Retraction Protection:** Captures fast-expiring or manually unsent DMs the instant they arrive in the status bar buffer before they vanish from your chats.

### 🔍 Deep Payload Inspection & Unclipped Text
* Extracts full unclipped messages from `Notification.EXTRA_BIG_TEXT`, `Notification.EXTRA_TEXT_LINES`, and `NotificationCompat.MessagingStyle` bundles.
* Dedicated **Full Detail Sheet** displaying unclipped bodies, package names, raw timestamps, and system notification IDs.
* Read messages completely without triggering "Seen" status or blue tick read receipts.

### 📅 Advanced Search, App Filters & Date Picker
* **Real-Time Keyword Search:** Debounced instant query search across message text, titles, and sender names.
* **Dynamic App Chips:** Automatically populates filter chips for each active app (WhatsApp, Instagram, Messages, etc.).
* **Date Range Picker:** Filter notifications by single dates or custom intervals to view history from any specific day.

### 🗑️ Multi-Select, Batch Delete & Safety Safeguards
* **No Accidental Bulk Wipes:** Accidental "Clear All" wipes are disabled by design.
* **Multi-Select Mode:** Long-press any card to select multiple notifications and delete them simultaneously.
* **Double Confirmation:** Every delete action (single or multi-item) requires explicit confirmation via Material 3 alert dialogs.

### 📊 Notification Analytics & Distraction Insights
* **Spam & Volume Breakdown:** Visual bar charts ranking the most active spam apps over 24h, 7 days, or all time.
* **Peak Distraction Hours:** 24-hour histogram revealing when you receive the highest concentration of notifications.

### 💾 Data Portability & Export (No PC Required)
* **SQLite Database Export:** Flushes Room's Write-Ahead Log (`wal_checkpoint`) and exports the raw `.db` file for viewing in any mobile SQLite viewer.
* **CSV Spreadsheet Export:** Streams notification logs directly into `.csv` files for Google Sheets or Excel.
* **Storage Budget Management:** Configurable auto-cleanup rules and storage caps up to **5 GB**.

---

## 🛠️ Tech Stack & Architecture

* **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
* **UI Framework:** Jetpack Compose with Material Design 3 (Dynamic Light & AMOLED Dark modes)
* **Local Persistence:** Room Database (SQLite) with Write-Ahead Logging (WAL) and Coroutines `Flow`
* **System Integration:** Android `NotificationListenerService`, Storage Access Framework (SAF)
* **Background Processing:** AndroidX WorkManager & Kotlin Coroutines

---

## 🚀 Setup & Installation

### Option 1: Download from GitHub Actions (Pre-compiled APK)
1. Navigate to the **Actions** tab in this repository.
2. Select the latest successful workflow run from the `main` branch.
3. Scroll down to **Artifacts** and download `NotiBox-APK.zip`.
4. Unzip and install `app-debug.apk` on your Android device (Android 8.0+ / API 26+).

### Option 2: Build from Source

#### Prerequisites
* JDK 17
* Android SDK (API 34)

#### Build Command
```bash
# Clone repository
git clone [https://github.com/your-username/NotiBox.git](https://github.com/your-username/NotiBox.git)
cd NotiBox

# Build Debug APK
./gradlew assembleDebug
