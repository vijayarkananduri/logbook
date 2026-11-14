# Logbook for Android

Logbook is a private, local-first mind space. Catch a thought, review its suggested shelves, find it again, and move an explicit next step into Things in motion. The app uses a warm paper-and-ink identity with pumpkin, amber, husky grey, rust, mango and lime accents; Urbanist body text; and Caveat display headings.

## Build and download

The **Build debug APK** GitHub Actions workflow builds `app-debug.apk` on pushes to `main` or via **Actions → Build debug APK → Run workflow**. It publishes the latest APK as an asset on the private `logbook-debug-latest` GitHub release. The repository and asset remain private. This is an unsigned debug build, not a production release.

## Privacy and offline behavior

- The Android manifest intentionally has **no `INTERNET` permission**. There are no accounts, sync, analytics, ads, network AI or remote speech fallback.
- Memories and tasks use SQLCipher. The database passphrase is wrapped with an Android Keystore AES-GCM key. Loss of that key makes the encrypted database unreadable; uninstalling removes local records. Automatic cloud/device backup is disabled. JSON export is the user-controlled portability path.
- Voice is foreground-only and uses Android's on-device recognizer only when Android 12+ reports an installed on-device model. Otherwise, Logbook explains the limitation and keeps text entry available. Review the transcript before saving.
- Reminder alarms are local. Android 13+ notification permission is requested only when a dated task needs an alert. Completed, deleted and cleared tasks have their pending alarms cancelled; unfinished alarms are restored after reboot.
- JSON files contain personal writing. Choose a destination you trust; a document picker may offer cloud-backed providers even though Logbook itself does not upload the file.

## Product behavior

- The home screen surfaces a capture action, today's thought count, open-task count and recent memories.
- Search ranks matches across thought text, categories and locally extracted attributes. Category suggestions, name/link/date extraction, deadline parsing and the local guide are deterministic rules, not generative AI.
- A captured thought is reviewed before it is saved. Possible actions can be optionally added to Tasks in the same database transaction as the thought.
- The interface supports warm light/dark themes and six accent choices. Liquid-glass sheen and lift are optional; reduced-effects mode uses flatter surfaces without blur or shadows.
- JSON import/export, profile name, onboarding replay and clear-all controls are available in Settings.

## Validation notes

CI verifies that the debug APK compiles and is published. A successful build does not prove device/runtime behavior. Validate on physical devices, especially on-device speech model availability, Android notification permissions, reminder delivery across reboot, SQLCipher key lifecycle, import/export rollback, TalkBack, large text and reduced-effects performance.
