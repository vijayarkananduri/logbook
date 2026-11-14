# Logbook for Android

Logbook is a private place to catch thoughts before they disappear. Write an idea, save a link, capture a feeling, remember a person, or turn a clear next step into a task. It is designed to feel like a calm paper notebook with useful structure underneath.

## What it does

- **Capture:** write or speak a thought using Android’s on-device speech recognition when available.
- **Review:** edit the thought, keep or remove suggested labels, and optionally create a task.
- **Find:** search words, links, people, dates, shelves, and recurring topics.
- **Connect:** browse hubs for people, projects, and topics that keep appearing.
- **Friendly nudges:** optional shuffled morning and evening check-ins. Morning messages include the real number of open tasks when useful; evening messages ask how the day went. They are not task reminders, and both times can be changed in Settings.
- **Private export:** export or import a JSON copy when you choose.
- **Warm interface:** opaque paper surfaces, gentle depth, sticky-note detail views, light/dark themes, and a simple splash built around the Logbook mark.

## How it works

Logbook is local-first. Thoughts and tasks are stored in an encrypted SQLCipher database on the device. The app has no account, sync, ads, analytics, network AI, or remote speech fallback. Search, labels, date detection, task suggestions, related-thought matching, hubs, and nudge wording all run locally through deterministic rules.

When a note looks like an action—such as “call Maya Friday” or “remember to send the file”—Logbook can suggest a task and a local alarm. Nothing is scheduled until you choose to save it. Completed, deleted, and cleared tasks cancel their alarms, and unfinished alarms are restored after reboot.

On first onboarding, Logbook asks for **notifications** so local task alarms and friendly nudges can reach you, and **microphone** access so you can dictate a thought. Both are optional: typing and the rest of the app still work if either permission is declined.

## Build and download

Pushes to `main` run the **Build debug APK** GitHub Actions workflow. A successful run publishes the unsigned testing APK to the private `logbook-debug-latest` release. It is for development and testing, not a production release.

```sh
gradle --no-daemon :app:assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy notes

The manifest intentionally has no `INTERNET` permission. Automatic cloud/device backup is disabled. JSON exports contain personal writing, so choose a destination you trust. Losing the Android Keystore key or uninstalling the app can make the encrypted local database unavailable.
