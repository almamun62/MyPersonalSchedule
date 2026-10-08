# 🎓 University Course Schedule & Planner (Android App)

A 100% offline-first Android university timetable, course schedule, and study planner built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**.

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/example/course-schedule-android.git
cd course-schedule-android
```

### 2. Open in Android Studio
1. Launch **Android Studio** (Ladybug, Koala, or Iguana recommended).
2. Select **File > Open...** and choose the cloned repository root folder.
3. Ensure JDK version is set to **JDK 21** (`Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`).

### 3. Build & Run
- Connect an Android device (Android 8.0 / API 26+) or launch an Android emulator.
- Click **Run 'app'** (`Shift + F10`) or assemble the APK via terminal:
```bash
./gradlew assembleDebug
```

---

## 🛡️ Real Runtime Permissions

This application runs **100% offline**. It declares **NO `android.permission.INTERNET`** and makes zero network calls at runtime.

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Delivers heads-up reminders before upcoming classes and focus session completions. |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Triggers alarms at the exact minute classes start and ends. |
| `RECEIVE_BOOT_COMPLETED` | Automatically reschedules class reminders and Do-Not-Disturb alarms after device reboot. |
| `ACCESS_NOTIFICATION_POLICY` | *(Optional)* Automatically enables Do Not Disturb (DND) during lectures when granted. |
| `VIBRATE` | Vibrates on class alert alarms. |
| `WAKE_LOCK` | Maintains accurate study countdowns when screen turns off. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps study focus mode ongoing and shields notifications. |
| `PACKAGE_USAGE_STATS` | *(Optional)* Monitors allowed study tools during Focus Mode with graceful standalone fallback if denied. |
| `REORDER_TASKS` | Assists screen pinning in kiosk focus mode. |

*Removed Permissions: `CAMERA`, `RECORD_AUDIO`, `READ_CALENDAR`, `WRITE_CALENDAR`, `REQUEST_INSTALL_PACKAGES`.*

---

## 📥 Course Import System

The course import screen provides two clear paths:

### 1. File Upload (CSV, XLSX, PDF)
- **CSV / TSV**: Imports comma- or tab-delimited timetables. A downloadable CSV template can be exported directly to device storage.
- **Excel (.xlsx / .xls)**: Parses binary OOXML workbooks and 2D matrix timetable grids completely offline.
- **PDF Documents**: Uses [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) for offline text extraction without any network dependencies. Extracted text is fed into the timetable matrix parser.
  - *Note on Scanned PDFs*: Scanned or image-only PDFs contain no selectable text layer. In this case, the app notifies the user to use Manual Add (no heavy cloud OCR).
- **Editable Preview List**: All imported classes are shown in an editable preview list before saving. Rows with missing names, invalid periods, or conflicts (via `CourseConflictDetector`) are highlighted. Users can edit rows or toggle Replace/Merge.

### 2. Manual Course Add
- Simplified primary entry: **Name**, **Day of Week**, **Start-End Period**, **Room**, and **Weeks rotation (All / Odd / Even / Range)**.
- Remembers last-used room and semester automatically.
- Secondary metadata (Course code, Teacher, Credits, Badge color, Notes, Retake) are neatly organized under **More options**.

---

## ⏰ Reminders & Alarms

- **`ClassReminderReceiver`**: Fires 15 minutes before class with lecture room, time, and quick Snooze/Dismiss actions.
- **`NotificationActionReceiver`**: Handles interactive notification clicks (Snooze 5m, Dismiss).
- **`SleepAlarmReceiver`**: Evening reminder checking tomorrow's schedule and notifying students of their earliest morning class.
- Uses `AlarmManager.setExactAndAllowWhileIdle` (with `canScheduleExactAlarms` check and settings deep-link).
- Automatically reschedules all alarms on `BOOT_COMPLETED` and after any course modification.
- Stores generic task and exam reminders locally in Room Database.

---

## 🎯 Study Focus Mode

- **Process Death Resilient**: Stores end timestamp (`targetEndTimeMillis`) in persistent storage rather than an ephemeral decrementing counter.
- **Screen-Off Operation**: Foreground service runs reliably with screen off.
- **Graceful Fallback**: If `PACKAGE_USAGE_STATS` is not granted, focus mode gracefully falls back to standalone kiosk mode with built-in tools (Calculator, Drawing Notes, Course Materials) without errors.

---

## ✍️ Note Canvas

- Freehand vector drawing and text memos linked to any course.
- **Autosave**: Canvas strokes and text autosave to Room database periodically and on dismiss.
- **Undo / Redo**: Full undo and redo history for drawn strokes.
- **Eraser Tool**: Touch-based stroke eraser.

---

## 📦 Releases

This application does not contain an in-app updater or auto-download mechanism.

New versions are published exclusively on the **GitHub Releases** page:
1. Navigate to the GitHub repository **Releases** tab.
2. Download the latest `app-release.apk` (or `app-debug.apk`) directly to your Android device.
3. Open the APK from your browser download history or device File Manager to install or update manually.

---

## 💡 Suggested Commit Message

```text
feat(offline): 100% offline overhaul, exact alarm reminders, and course import stream parser

- Remove INTERNET and unused sensitive permissions (CAMERA, RECORD_AUDIO, CALENDAR, REQUEST_INSTALL_PACKAGES)
- Delete AppUpdateManager, AppUpdateDialog, and in-app updater code
- Implement ClassReminderReceiver, NotificationActionReceiver, and SleepAlarmReceiver with exact alarms
- Reschedule alarms on BOOT_COMPLETED and course edits with canScheduleExactAlarms verification
- Add Room migrations array (v1 through v8) and generic task/exam reminder storage
- Streamline Course Import to two paths: File Upload (CSV, XLSX, PDF via PdfBox-Android) and simplified Manual Add
- Add downloadable CSV template generator and editable pre-import conflict preview
- Implement end-timestamp persistence for Study Focus Mode surviving process death and screen-off
- Add Room autosave, undo/redo, eraser, and course linking to Note Canvas
- Overhaul README with build instructions, actual permissions, and manual GitHub Releases guide
```
