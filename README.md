# Logbook

<p align="center"><img src="docs/logo.svg" width="96" alt="Logbook mark"></p>

<p align="center"><strong>A little room for your thoughts.</strong><br>Private, local-first capture for Android.</p>

Logbook is a quiet place to catch thoughts before they disappear, find them again when you need them, and move explicit next steps into a small, dependable task list. It is designed for people who want less filing and more remembering.

- **Capture first.** Write a thought, link, feeling, idea, or detail without deciding where it belongs.
- **Review before saving.** Logbook suggests shelves such as Ideas, Projects, Feelings, Links, Code, Life, People, and References. You stay in charge.
- **Search everything.** Universal search reaches thoughts, categories, extracted names, links, dates, and tasks from one place.
- **Keep tasks explicit.** Turn a thought into a next step only when you mean to.
- **Choose one reminder moment.** If a task has no time, Logbook asks when to remind you instead of guessing. Android 13+ notification permission is requested only for a dated reminder.
- **Use gestures in Search.** Swipe left to delete. Swipe right to archive. Archive thoughts older than 30 days with one tap.
- **Keep it yours.** No account, network permission, cloud sync, ads, analytics, or remote AI. Local reminders are restored after reboot.

## Product pages

- [Product landing page](https://vijayarkananduri.github.io/logbook/)
- [Detailed guide](https://vijayarkananduri.github.io/logbook/guide.html)

The two pages are linked to each other and are published from [`docs/`](docs/). Enable them in **Settings → Pages → GitHub Actions**.

## Build

The **Build debug APK** workflow builds `app-debug.apk` on pushes to `main` or through **Actions → Build debug APK → Run workflow**. It publishes the latest unsigned debug build as an asset on the private `logbook-debug-latest` GitHub release.

```bash
./gradlew assembleDebug
```

## Privacy and offline behavior

Logbook stores memories and tasks in an encrypted SQLCipher database. The database passphrase is wrapped by an Android Keystore AES-GCM key. Automatic cloud/device backup is disabled; JSON export is the user-controlled portability path. Voice is foreground-only and uses Android's on-device recognizer when a local model is available. Review the transcript before saving.

Reminder alarms are local. Completed, deleted, and cleared tasks have their pending alarms cancelled; unfinished alarms are restored after reboot. JSON files contain personal writing, so choose an export destination you trust.

## Current implementation status

The redesign is implemented in Compose with the warm paper-and-ink identity, compact three-step intro, universal Search, four-destination navigation, archive/delete gestures, task reminder prompt, compact Settings, and an external Help link to the detailed guide. Runtime validation still belongs on a physical Android device, especially notification delivery, reboot recovery, speech-model availability, TalkBack, large text, and reduced-effects performance.
