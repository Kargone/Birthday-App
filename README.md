# Birthday App

An Android app, written in Java, for keeping track of family and friends' birthdays. It reminds you on the day and gets a text message ready to send.

## Features

- **Birthday list:** grouped by month starting with the current one, with each person's date, the weekday of their next birthday, and their current age. Today's birthdays are bold.
- **This Month:** show only the current month's birthdays.
- **Stats:** the number of people, the youngest and oldest, and any birthdays shared by two ("doubles") or three ("triples") people.
- **Reminders:** a daily background check posts a notification for each birthday that day.
- **Birthday texts:** save a phone number and custom message per person. The app opens your messaging app with the text filled in; you press send.
- **Light and dark themes:** follows the system setting.

## Install

Requires Android 10 (API 29) or newer.

1. Download the APK from the [Releases page](https://github.com/Kargone/Birthday-App/releases).
2. Open it on your phone and allow installs from that source if asked.

## Usage

| To... | Do this |
| --- | --- |
| Add a birthday | Type a name and a date (`MM/DD/YYYY`, slashes are added for you), then press Done on the keyboard. |
| Edit a phone number or message | Tap the person, make changes, and tap **Save**. |
| Text someone | Tap the person, then **Send now**, or tap their birthday notification. If no number is saved, you'll be asked for one. |
| Delete someone | Tap the person, then **Delete**. |
| Turn on reminders | Tap **Enable Notifications**. Once they're allowed, a test notification appears. |

The default message is `Happy birthday, <name>! 🎉`. Clearing a custom message restores it.

Open the app at least once after installing so it can schedule the daily check. The first check is set for just after the next midnight, but Android decides exactly when background work runs, so reminders may arrive later.

## Privacy

The app has no accounts or server and doesn't request internet access. Data is stored on the device in `SharedPreferences`. Android's standard backup is enabled, so it may be included in your device or cloud backups.

The app doesn't request SMS or contacts permissions.

## Building from source

You need JDK 17 or newer and the Android SDK with API 36. Opening the project in a recent version of Android Studio that supports Android Gradle Plugin 9.3 is the easiest way to set up the SDK. From the command line, set `ANDROID_HOME` or `sdk.dir` in `local.properties`.

```sh
git clone https://github.com/Kargone/Birthday-App.git
cd Birthday-App
```

| Task | Windows | macOS / Linux |
| --- | --- | --- |
| Build a debug APK | `.\gradlew.bat assembleDebug` | `sh ./gradlew assembleDebug` |
| Install on a connected device | `.\gradlew.bat installDebug` | `sh ./gradlew installDebug` |
| Run unit tests | `.\gradlew.bat testDebugUnitTest` | `sh ./gradlew testDebugUnitTest` |

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Release builds are unsigned by default; supply your own keystore to sign them. The test suite currently contains only the Android Studio template tests.

## Project structure

The code lives in `app/src/main/java/com/karson/birthday_app/`:

| File | Purpose |
| --- | --- |
| `MainActivity.java` | Main screen: adding, editing, sorting, stats, and scheduling the daily check |
| `BirthdayAdapter.java` | `RecyclerView` adapter for the month headers and people |
| `BirthdayWorker.java` | WorkManager job that looks for today's birthdays |
| `BirthdayTexts.java` | Saved phone numbers and messages, notifications, and opening the messaging app |

Layouts, themes, and icons are in `app/src/main/res/`.

## Known issues

- Reminders can occasionally be missed, because each daily run is timed from when the previous one finished.
- February 29 birthdays only get a reminder in leap years.
- **Send now** uses the saved number and message, not unsaved edits, so tap **Save** first.
- Adding a name that already exists overwrites that person's birthday.
- Stats don't list birthdays shared by four or more people.
