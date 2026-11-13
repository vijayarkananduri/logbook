# Product and UI direction

Logbook should retain its own identity as a **private mind space**, not imitate another product. Its website presents a local, offline experience for capturing thoughts, browsing categories, searching memories, managing tasks, and exporting or restoring JSON data. The Android app keeps those same jobs close to hand, with review-before-save and no account or cloud AI.

## Design decisions

- Keep the Logbook palette: warm paper and readable ink, with pumpkin, amber, husky grey, rust, mango and lime as restrained accents in both themes.
- Keep the requested type pairing: Urbanist for readable interface text and Caveat for a few expressive headings.
- Borrow craft, not product identity, from Vijayarka's own Khata project: warm materials, carefully spaced content, softly raised cards, a fine reflective edge and a calm dark treatment. Khata's DewSurface source explicitly uses safe gradients and rim highlights instead of backdrop render graphs.
- Make the home screen useful before the user searches: a clear capture action, today's activity, open-task count and recent thoughts.
- Keep a five-destination navigation for Home, Search, Tasks, Guide and Settings. Preserve 48dp-class interactive targets and visible labels.
- Use light layered sheen and soft elevation when effects are enabled. The reduced-effects option uses a flatter, tinted surface with no shadow or blur. This is intentional: Compose's documented `Modifier.blur` blurs the layer's own content rather than the background behind it, and an app-wide blur would add GPU work without creating true backdrop glass.
- Categorization, date/name extraction, search and the helper remain deterministic and on-device. Voice is offered only through Android's on-device recognizer when available; text entry remains the dependable fallback.

## References

- Logbook product and privacy language: https://thelogbook.netlify.app/
- Vijayarka's Khata README, including its visual principles: https://github.com/vijayarkananduri/khata/blob/main/README.md
- Khata's DewSurface implementation: https://github.com/vijayarkananduri/khata/blob/main/app/src/main/java/com/khata/ui/theme/DewSurface.kt
- Android Material 3 Compose theming and color roles: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose accessibility defaults and minimum touch targets: https://developer.android.com/develop/ui/compose/accessibility/api-defaults
- Compose blur behavior and platform requirements: https://developer.android.com/develop/ui/compose/graphics/blur/overview
