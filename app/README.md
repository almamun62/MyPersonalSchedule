# 📚 MySchedule — University Course & Timetable Manager

> *"Your academic life, perfectly organized and always at your fingertips."*

---

## 💡 The Story & Motivation Behind MySchedule

As students, navigating complex university schedules, changing classrooms, impending assignment deadlines, and exam timetables can often feel overwhelming. Too many timetable apps are bogged down by unnecessary clutter, bloated AI features, or excessive battery drain. 

**MySchedule** was built with a simple core philosophy: **simplicity, reliability, and respect for your device's battery.** Because a timetable app is primarily used as a clean, static reference throughout your day, MySchedule is engineered from the ground up to be lightweight and ultra-efficient. It sits quietly in the background with **smart battery savings**, consuming virtually zero power when idle, yet stepping up instantly when you need your schedule, reminders, or focus sessions.

---

## ✨ Key Features

- **📅 Streamlined Horizontal Calendar Strip**: Switch between days instantly with a single tap using a clean, uncluttered calendar strip at the top of your timetable.
- **⚡ Ultra-Efficient & Battery-Friendly**: Designed primarily for static viewing, ensuring near-zero background power consumption.
- **🔔 Reliable Dual-Layer Alarms & Reminders**: Combines WorkManager and exact `AlarmManager` broadcasts to ensure class reminders never miss a beat.
- **📊 System Calendar Integration**: Export individual courses or your entire semester schedule to Google Calendar, Outlook, or Apple Calendar via `.ics` and `ACTION_INSERT`.
- **📌 Live Status Notification & Widget**: Glanceable home screen widgets and status bar notifications showing your ongoing and next upcoming classes.
- **🔒 Study Focus Lock**: Pomodoro study timer with fullscreen lock to keep you productive and distraction-free.
- **🌐 Bilingual Support**: Fully localized in English and Simplified Chinese (中文).

---

## 📥 Installation

1. Go to the [GitHub Releases](https://github.com/your-username/myschedule/releases) page.
2. Download the latest `app-debug.apk` (or release APK).
3. Install the APK on your Android device (Android 8.0 / API 26 or higher).
4. Launch the app and import your schedule or load the pre-configured sample semester schedule!

---

## 📱 Manufacturer Troubleshooting & Permissions

Some Android manufacturers (especially Chinese OEMs like Xiaomi, Huawei, Vivo, Oppo, and OnePlus) employ aggressive battery management systems that may restrict background alarms and notifications. 

To ensure your class reminders and notifications work 100% reliably on your device, please follow these quick steps:

### 🔴 Xiaomi / Redmi (MIUI / HyperOS)
1. Go to **Settings** -> **Apps** -> **MySchedule**.
2. Enable **Autostart** (自启动).
3. Under **Battery Saver**, select **No restrictions** (无限制).

### 🟠 Huawei / Honor (HarmonyOS / EMUI)
1. Go to **Settings** -> **Battery** -> **App launch** -> **MySchedule**.
2. Switch off **Manage automatically** and manually enable **Auto-launch**, **Secondary launch**, and **Run in background**.

### 🟢 Vivo (OriginOS / Funtouch OS)
1. Go to **Settings** -> **Apps** -> **MySchedule** -> **Battery**.
2. Select **High background power consumption** (允许后台高耗电).
3. In system manager, allow **App Autostart**.

### 🔵 Oppo / Realme (ColorOS)
2. Go to **Settings** -> **Battery** -> **More battery settings** -> **Optimize battery use** -> **MySchedule** -> Set to **Don't optimize**.
3. Allow app autostart in permission manager.

---

## 🛠️ Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose & Material Design 3
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Local Database**: Room (Offline-first, secure local persistence)
- **Integration**: CalendarContract, FileProvider, AppWidgetProvider
