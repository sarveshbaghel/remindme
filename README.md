# Taskly — Smart Reminder & Task Management Android Application

Taskly is a modern, minimal, and functional Android application built with Kotlin and Jetpack Compose. It empowers users to organize daily tasks, configure smart multiple reminders, schedule recurring tasks, navigate a monthly calendar agenda, and track productivity analytics completely offline with zero tracking or account requirements.

---

## 📱 Features Overview

### 1. Home Dashboard
- **Dynamic Header**: Time-of-day greeting ("Good Morning", "Good Afternoon", "Good Evening") and current formatted date.
- **Productivity Progress**: Daily completion percentage progress bar with real-time counters (Total Today, Completed, Pending, Overdue).
- **Categorized Sections**:
  - **Overdue Tasks**: Prominently highlighted with warning indicators and collapsible accordion.
  - **Today's Tasks**: Grouped with due time, priority dots, category badges, and multiple-reminder indicators.
  - **Upcoming Tasks**: Quick preview of upcoming days.
  - **Completed Today**: Completed items with strikethrough styling and one-tap reopen capability.
- **Task Detail Bottom Sheet**: Quick actions to complete, postpone (+1 day, +3 days), edit, duplicate, archive, or delete.

### 2. Task Creation & Management
- **Add / Edit Task Screen**:
  - Title and multiline notes/description.
  - Due date picker (Material 3 DatePicker).
  - Due time picker (Material 3 TimePicker).
  - Priority selector (Low, Medium, High, Urgent).
  - Default categories (Personal, Work, College, Projects, Health, Shopping, Other) + custom category creation on the fly.
  - Smart Multiple Reminders configuration (Notification vs. Alarm style).
  - Recurrence rules (Daily, Weekdays, Weekly, Monthly, Custom).

### 3. Smart Multiple Reminder System
- Assign **multiple independent reminders** to a single task (e.g. 10m before, 1h before, at due time).
- Supported Alert Types:
  - **Standard Notification**: Informational alert with "Complete" and "Snooze (10m)" action buttons.
  - **Alarm-Style Notification**: High-importance heads-up alert with custom vibration, sound, and full-screen intent support.
- **Smart Cancellation**: Marking a task as completed automatically cancels all remaining alarms from `AlarmManager`.
- **Snooze Engine**: Snoozing reschedules the alarm by the configured duration without creating duplicate alerts.
- **Boot & Timezone Restoration**: `BootReceiver` restores and reschedules all active reminders upon device reboot, app update, or clock/timezone changes.

### 4. Recurring Tasks Engine
- Supports recurring task patterns: Daily, Weekdays (Mon–Fri), Weekly, Monthly, Yearly, and Custom intervals.
- When a recurring task is completed, Taskly automatically generates the next occurrence with the calculated future due date and replicates its reminder offsets, preserving full completion history.

### 5. Tasks Screen (Filters, Sort & Search)
- **Filters**: All, Today, Upcoming, Overdue, Completed, Archived.
- **Category Filter Chips**: Filter tasks by any category with matching icon and theme tint.
- **Real-Time Search**: Substring search across task titles and descriptions.
- **Sorting Options**: Due Date, Priority (Urgent first), Creation Date (Newest first), and Alphabetical.

### 6. Calendar Agenda
- **Monthly Calendar Grid**: Navigate across months with day-of-week headers and today shortcut.
- **Visual Dot Indicators**: Each date displays up to 3 colored dots reflecting the priority of scheduled tasks.
- **Selected Day Agenda**: Inspect all tasks scheduled for the selected day with one-tap completion, or tap "+ Add Task" to pre-fill that date.

### 7. Insights & Productivity Analytics
- Calculated exclusively from actual local Room database entries (no dummy data):
  - Weekly and Monthly completion rates.
  - Total tasks created, completed, and postponed.
  - Current productivity streak (consecutive days with completed tasks).
  - Most productive day of the week.
  - Interactive Canvas-based Bar Chart showing completed tasks per day of the week.
  - Category breakdown with visual percentage distribution bars.

### 8. Settings & Privacy
- **Appearance**: Toggle between System Default, Light Mode, and Dark Mode.
- **Notification Preferences**: Global notification toggle, vibration toggle, default snooze duration (5m, 10m, 15m, 30m).
- **Task Defaults**: Default priority selection.
- **Data Management**:
  - **JSON Export**: Export full database (tasks, categories, reminders) to a formatted JSON string.
  - **JSON Import**: Restore database from backup with structure and version validation.
  - **Clear Completed Tasks**: Remove completed task history with confirmation dialog.
  - **Delete All Data**: Factory reset with safety confirmation dialog.
- **100% Offline & Private**: No analytics SDKs, no external network requests, no login required.

---

## 🏗 Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9.22 |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Architecture** | MVVM with Unidirectional Data Flow (StateFlow / Flow) |
| **Database** | Room 2.6.1 (SQLite with foreign keys & indices) |
| **Preferences** | AndroidX DataStore Preferences |
| **Reminders** | AlarmManager (`setExactAndAllowWhileIdle`) |
| **Notifications** | AndroidX Core NotificationCompat & NotificationManager |
| **Dependency Injection** | Application-level Container Pattern (`AppContainer`) |
| **Build System** | Gradle 8.5 with Kotlin DSL |

---

## 🚀 How to Build & Run

### Prerequisites
- JDK 17 (or Android Studio bundled JBR 17+)
- Android SDK API 34 installed

### 1. Build Debug APK
Run the following command from the project root:
```bash
./gradlew assembleDebug
```
The compiled debug APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Run Automated Unit Tests
Run the unit test suite:
```bash
./gradlew testDebugUnitTest
```
All unit tests verify:
- Recurrence date arithmetic (Daily, Weekdays, Weekly, Monthly, Custom)
- Date and time formatting helpers
- Task completion, status toggling, and reminder replication logic
- JSON backup export and import serialization

### 3. Build Release APK
To assemble a minified release APK with ProGuard optimization:
```bash
./gradlew assembleRelease
```
*Note: For Play Store distribution, configure your keystore in `app/build.gradle.kts` under `signingConfigs`.*

