# Krono for Android

**A minimalist, deadline-focused to-do app for Android.** Built with Kotlin and Jetpack Compose, Krono pairs a refined dark interface with quick task entry, deadline status, local persistence, and native Android reminders.

<p align="center">
  <a href="https://github.com/r-samarth/kronoAPK/releases/latest">
    <strong>📥 Download the latest Android APK</strong>
  </a>
</p>

> The APK is generated and published automatically after the Android project is merged to `main` and the GitHub Actions build succeeds. If no release is available yet, check the [Actions tab](https://github.com/r-samarth/kronoAPK/actions) for the latest build status.

## Features

- **Quick task capture** — add tasks from the main screen.
- **Flexible deadlines** — choose `+1h`, `+3h`, `Tonight`, or `Tomorrow`, or choose a custom date and time.
- **At-a-glance status** — upcoming, due within three hours, overdue, and completed states are visually distinct.
- **Active tasks and Logbook** — switch between pending and completed tasks.
- **Native deadline reminders** — a notification 30 minutes before a deadline and another at the deadline.
- **Month-end summary** — at 11:59 PM on the last calendar day of each month, get a notification with completed and overdue counts plus a completion-progress percentage; completed tasks are then cleared.
- **Monthly notification history** — open the clock-history button in the app to view saved month-by-month summaries, completion percentages, notification text, and last-updated times.
- **Creator watermark** — tap “Built by Sam / Open source” to open the Krono GitHub repository.
- **Krono app branding** — app logo shown in the app header.
- **Offline by design** — no account, internet connection, or cloud setup is required.
- **Private on-device task storage** — tasks and monthly summary history are stored locally using Android app storage. No Supabase connection is added to Android.
- **Dark, high-contrast interface** — the same Carbon, Graphite, Vermilion, Amber, Crimson, and Emerald design language as Krono for Apple platforms.
- **Haptic feedback** — tactile feedback for supported interactions.

## Download and install

### Install on your Android phone

1. Open the [latest Krono release](https://github.com/r-samarth/kronoAPK/releases/latest).
2. Download **Krono-Android.apk** from the release assets.
3. Open the downloaded APK.
4. If Android asks, allow your browser or file manager to install unknown apps for this installation. Wording varies by Android version and manufacturer.
5. Return to the installer and tap **Install**.
6. Launch Krono and allow notifications so deadline reminders can appear.

Only install APKs from a source you trust. You can turn off the browser/file manager's **Install unknown apps** permission after installation.

## Get started

Krono does not require sign-up or configuration:

1. Open the app.
2. Enter a task in the composer at the bottom of the screen.
3. Select a deadline preset, or use the calendar button to pick a custom date and time.
4. Tap the **+** button to save the task.
5. Tap the checkbox when you complete it.
6. Use **Active** and **Logbook** to switch between pending and completed work.
7. Tap the **×** beside a task to delete it.

New installs include two sample tasks to help you explore the interface. You can complete or delete them like any other task.

## Notifications

For each active task with a future deadline, Krono schedules:

- **Approaching Deadline (30m)** — 30 minutes before the deadline, when enough time remains.
- **Deadline Reached** — at the task's deadline.

Notifications require permission. On some Android versions, exact reminders may also depend on the device's **Alarms & reminders** or **Exact alarms** setting. If exact-alarm access is unavailable, Krono falls back to an inexact alarm, which may be delayed by Android.

Completing or deleting a task cancels its future deadline alarms.

## Month-end cleanup and summary

Krono schedules a local notification for **11:59 PM on the last day of each calendar month**.

The notification is titled with the month and summarizes the locally saved task list, including a progress percentage, for example:

> Completed 8 tasks this month. Still 3 overdue tasks pending. Progress: 73%.

The counts above are an example. Krono calculates the actual number of tasks marked completed during that month and the number of active tasks whose deadlines have passed. Progress is calculated from completed tasks divided by completed plus pending-overdue tasks. After the summary is prepared, completed tasks from that month and older months are removed from local task storage.

### Monthly notification history

Tap the **clock-history icon** in the app header to open the Monthly Archive. Krono stores one locally saved summary per month, ordered newest first, including completed count, pending-overdue count, progress percentage, the notification title/body, and the last-updated time. The current month's entry is updated as task data changes and the delivered month-end notification is saved to the same entry.

Android does not let Krono import notifications that were already dismissed before the history feature was installed. New history entries accumulate from this version onward. Uninstalling Krono or clearing app data removes the local archive too.

Allow notifications for Krono. Krono restores deadline and month-end alarms after a device restart, app update, or local date/time-zone change. Android may delay alarms if exact-alarm access is unavailable. Force-stopping the app can prevent scheduled alarms until you open Krono again.

## Privacy and storage

The Android app described here saves tasks on the device in Android's private app storage. It does **not** connect to Supabase and does **not** sync its task list with Krono for macOS or iPhone.

- No account is required.
- Task data is local to this Android installation.
- Clearing Krono's app storage or uninstalling it may remove saved tasks.
- Back up any task information you cannot afford to lose.

## Compatibility

**Current app version:** 1.2.0 (version code 3), including monthly notification history, progress percentages, app-header branding, and completed-task cleanup.

- **Minimum Android version:** Android 8.0 (API 26).
- **Build JDK:** Java 17.
- **Android SDK:** API 34.
- **Build system:** Gradle 8.7, Kotlin 2.0.0, Android Gradle Plugin 8.5.2.

## Build from source

### Requirements

Install [Android Studio](https://developer.android.com/studio), a JDK 17, and Android SDK Platform 34.

### Open in Android Studio

1. Clone this repository:

   ```bash
   git clone https://github.com/r-samarth/kronoAPK.git
   ```

2. In Android Studio, choose **Open** and select the repository's **android** directory.
3. Allow Gradle sync to finish. Use Gradle **8.7** if Android Studio asks you to select or configure a Gradle installation.
4. Select the **app** run configuration and a connected Android phone or emulator.
5. Press **Run**.

### Build an APK from Terminal

Install Gradle 8.7 and ensure your Android SDK is available, then run:

```bash
cd android
gradle assembleDebug
```

The debug APK will be written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

To build the release variant locally:

```bash
gradle assembleRelease
```

The release APK is written to `android/app/build/outputs/apk/release/app-release.apk`.

### Automated builds and releases

GitHub Actions builds the release APK when Android source or its workflow changes reach `main). Successful main-branch builds are published to [GitHub Releases](https://github.com/r-samarth/kronoAPK/releases) as **Krono-Android.apk**. Pull requests also run a build check.

The current direct-download build uses Gradle's debug signing configuration for convenient sideloading. Before publishing to a wider audience or distributing regular upgrades, configure a persistent private release keystore in GitHub Actions; do not commit private signing keys or passwords to the repository.

## Troubleshooting

**I cannot find the APK.**  
The first release may still be building. Check [GitHub Actions](https://github.com/r-samarth/kronoAPK/actions), then visit [Releases](https://github.com/r-samarth/kronoAPK/releases).

**I don't receive notifications.**  
Allow notifications for Krono in Android Settings. If your device offers exact-alarm access, enable it for more punctual reminders. Some manufacturers also apply battery restrictions to background alarms.

**My tasks disappeared.**  
Tasks are stored locally. Clearing app storage or uninstalling Krono can remove them; cloud recovery is not available in this Android version.

**Can Android tasks sync with my Mac or iPhone?**  
Not currently. The Android app is local-only and uses separate on-device storage.

**Android says the APK cannot be installed.**  
Confirm that the APK finished downloading and is from the official [Krono Releases](https://github.com/r-samarth/kronoAPK/releases) page. If Android reports a signature conflict with an older test build, back up any important tasks before uninstalling the old build.

## Project structure

```text
kronoAPK/
├── .github/workflows/
│   └── android-release.yml         # Build and publish APK
└── android/
    ├── settings.gradle.kts         # Gradle project setup
    ├── build.gradle.kts             # Root Android/Kotlin plugins
    ├── gradle.properties
    └── app/
        ├── build.gradle.kts         # Android app and dependencies
        └── src/main/
            ├── AndroidManifest.xml
            └── java/dev/samarthr/krono/
                ├── MainActivity.kt  # App entry point and permissions
                ├── KronoEngine.kt  # Task model, local storage and alarms
                └── KronoViews.kt   # Jetpack Compose interface
```

## Feedback and contributions

Found a bug or have an idea? [Open an issue](https://github.com/r-samarth/kronoAPK/issues) with your Android version, device model, steps to reproduce, and any relevant error message. Please don't post private task data.

---

Built by [Samarth](https://github.com/r-samarth).
