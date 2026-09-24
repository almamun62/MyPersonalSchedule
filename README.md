# 🎓 University Course Schedule & Planner (Android App)

A modern, offline-first Android university timetable and academic planner built with **Kotlin** and **Jetpack Compose (Material 3)**.

---

## ✨ Key Features

- **📅 Advanced Timetable Grid**: Supports 1-12+ periods per day, multi-week schedules, single/double week rotation (单双周), and weekend make-up days (调休).
- **📥 Multi-Format Import**: Import your course schedule instantly from **CSV, Excel (XLSX), PDF**, or by pasting free text from Chinese university portals (Tsinghua, SWPU, etc.).
- **⏰ Dynamic Break Times & Period Timings**: Automatically synchronizes break durations and period start/end times based on your imported schedule.
- **🔔 Smart Reminders & DND**: 15-minute advance class notifications and automatic Do Not Disturb (DND) mode during lectures.
- **🌙 Sleep & Wake Alarm Sync**: Automatically adjusts your morning alarm based on tomorrow's first class.
- **🔒 Study Focus Mode**: Lock phone distractions and manage whitelisted apps during study hours.
- **🔄 In-App Update Checker**: Checks GitHub Releases for new updates directly from the Settings menu.
- **🌐 Bilingual Support**: Full toggle between English and Chinese (`中文`).
- **🛡️ 100% Offline-First**: All your schedule data is stored securely on-device using **Room Database**.

---

## 🛠️ Tech Stack

- **UI**: Jetpack Compose, Material Design 3 (M3)
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Local Persistence**: Room Database (SQLite)
- **Networking**: OkHttp for GitHub release checks & updates
- **CI/CD**: GitHub Actions for automated Debug APK builds

---

## 🚀 GitHub Actions & Releases

This project includes a pre-configured GitHub Actions workflow (`.github/workflows/build.yml`) that automatically compiles your Android app into a debug APK on every push.

### Managing Releases & Betas:
1. Go to your GitHub repository -> **Releases** -> **Draft a new release**.
2. Tag your release (e.g., `v1.0.0` or `v1.1.0-beta.1` for pre-releases).
3. Attach your built APK and publish.
4. Users can tap **"Check for Updates"** in the app's **Settings & Tools** menu to check for new releases and download them instantly!

---

## 📥 Installation

1. Download the latest APK from the [GitHub Releases](../../releases) page.
2. Install on your Android device (Android 8.0 / API 26 or higher).
3. Import your schedule or load the pre-configured sample semester schedule!
