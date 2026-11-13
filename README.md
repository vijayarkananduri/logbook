# Logbook for Android

A private, local-first Android thought log with a warm paper interface, handwritten-feeling headings, review-before-save note capture, local search, todos, a deterministic text helper, light/dark themes, JSON import/export, and optional on-device speech input.

## Build

The **Build debug APK** GitHub Actions workflow builds `app-debug.apk` on pushes to `main` or via **Actions → Build debug APK → Run workflow**. Download it from the workflow run’s artifacts.

## Privacy and offline behavior

- The Android manifest intentionally has **no `INTERNET` permission**. There are no accounts, sync, analytics, ads, network AI, or remote speech fallback.
- Memory and todo records use SQLCipher with a database key wrapped by an Android Keystore AES-GCM key. Keystore key loss makes the encrypted records unreadable; uninstalling the app removes local records. Automatic cloud/device backup is disabled. Use the in-app JSON export to make a user-controlled copy.
- Voice is foreground-only and requested after tapping Mic. Only Android’s on-device recognizer API is used, when that API reports availability. It requires a locally available language model; otherwise Logbook explains that voice is unavailable and text entry remains usable. Speech availability has not yet been verified against a device matrix.
- Notifications are local Android reminders. Android 13+ notification permission is not yet requested contextually by this initial project; permission and reminder delivery should be tested on devices before relying on it.
- JSON exports contain private writing. Pick a local destination; Android document providers may also present cloud locations. The app does not upload the file.

## Design

Warm paper/ink foundation with amber, pumpkin orange, husky grey, rust, mango/blush and lime accents; 18/12/8dp-like rounded hierarchy; responsive system text sizing; a low-resource frosted-glass preference (flat translucent surfaces instead of blur); and offline font role intent of Urbanist for body and Caveat for display headings. Font assets can be added under OFL after their first build verification.

## Status and scope

This is an Android project plus CI build workflow, not a signed release. CI generates an installable debug APK; no private signing key is stored in GitHub. Validate the full source-handoff acceptance matrix on real devices, especially import rollback, notification permissions/restart behavior, TalkBack, large fonts, database key lifecycle, and the installed offline speech model. No release or offline speech claim should be inferred from a CI build alone.
