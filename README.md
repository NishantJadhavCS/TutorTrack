# TutorTrack

An Android app for private tutors to track how far each student has got through their syllabus, and whether they're on pace to finish before the semester starts.

For every student you list their subjects and lessons, then mark each lesson as **taught** and later as **learned**. TutorTrack compares that against the time left and tells you, per subject, if you're on track or falling behind, and how many lessons a day you need to keep up.

## Features

- **Students, subjects and lessons**: add students with their standard, then subjects (drag to reorder, star, rename) and lessons in bulk. Lessons can have titles and be marked **optional**.
- **Taught & learned dates**: each lesson records the day it was taught and the day the student had learned it.
- **Pace tracking**: set a semester start (an exact date, or just month and year) and the app works out the teaching days left (Monday–Saturday) and gives each subject a status:
  - **Done**: every lesson taught
  - **On track**: within 5 percentage points of where you should be
  - **Slightly behind**: within 15 points
  - **Behind**: more than 15 points short

  It also gives the pace needed in plain words, e.g. *"about 1 lesson every 3 days"*. Optional lessons don't count towards progress.
- **Lesson calendar**: a month view of every taught/learned mark, for one student or for all students on the home screen, with filters by student and by taught/learned.
- **Backup & restore**: export all students, subjects, lessons and your profile to a JSON file, and import it again on any device.
- **Profile & themes**: your name for the home-screen greeting, plus light, dark or system theme.

Everything is stored on the device. There are no accounts and nothing is sent over the network.

## Tech stack

| Area | Library |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| Database | Room (with KSP) |
| Settings | DataStore Preferences |
| Backup format | kotlinx.serialization (JSON) |
| Drag-to-reorder | [Reorderable](https://github.com/Calvin-LL/Reorderable) |

Min SDK 26 (Android 8.0), target SDK 37.

## Project structure

```
app/src/main/java/com/example/tutionprogess/
├── data/       Room entities, DAOs, repository, DataStore prefs, backup codec
├── domain/     Progress and calendar logic (pure Kotlin, unit-tested)
└── ui/
    ├── home/       Overview of all students + combined calendar
    ├── student/    A student's subjects, pace and calendar
    ├── subject/    Lesson list with taught/learned dates
    ├── calendar/   Shared month calendar component
    ├── profile/    Tutor profile
    ├── settings/   Theme, students, export/import
    ├── components/ Shared UI pieces
    └── theme/      Colours and typography (Manrope)
```

## Getting started

### Requirements

- A recent [Android Studio](https://developer.android.com/studio) with Android SDK 37 installed
- JDK 25 for the Gradle daemon (Gradle downloads it automatically via the Foojay toolchain resolver if it's missing)

### Build and run

Open the project folder in Android Studio and run the **app** configuration, or from the command line:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install on a connected device or emulator
```

On Windows use `gradlew.bat` instead of `./gradlew`.

### Tests

The progress, calendar and backup logic is covered by JVM unit tests:

```bash
./gradlew testDebugUnitTest
```

## Using the app

1. Open **Settings** and add a student. Set when you started tracking them and, if you know it, when their semester starts.
2. Open the student and add their subjects.
3. Open a subject and tap **Add lessons** to create them all in one go.
4. As you go, mark lessons taught and learned. The home screen and calendar update to show who's on track.
5. Every so often, use **Settings → Export data** to keep a backup.
