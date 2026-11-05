# Pocket Planner

A reminders, notes and calendar app for Android, with a home-screen widget.

- Foggy-glass look, dark by default, with themes, fonts and your own background photo
- Type reminders in plain words: "call mom tomorrow at 7pm", "gym every monday at 6am", "midsems on 20 nov"
- One simple time per reminder, repeats, optional priority and countdowns
- Real notifications at the time you set, plus an optional morning summary
- Mood check-in, weekly report and 6 months of history
- Widget in 2×2, 4×1, 4×2 and 4×4 sizes: tick tasks off, tap + to add
- Pop sound and confetti when you finish things
- On Nothing phones: the Glyph lights show your progress while the app is open

## Install on your phone

1. Open the **Releases** page of this repository on your phone (right side of the repo page, or `/releases`).
2. Under the newest release, tap **PocketPlanner.apk** to download it.
3. Open the downloaded file. If Android asks, allow your browser to install apps, then tap **Install**.
4. If Google Play Protect warns that it doesn't know the app, tap **More details → Install anyway**.

Updates install the same way, over the old version. Your reminders are kept.

## Add the widget

Long-press your home screen → **Widgets** → **Pocket Planner** → drag it out. Resize it to the size you like.

## Glyph lights for reminders (Nothing phones)

Phone Settings → **Glyph Interface** → **Notifications** → pick a light pattern for Pocket Planner.

## How it's built

Every push to `main` builds a signed APK with GitHub Actions and publishes it as a release.
The Nothing Glyph SDK is downloaded during the build from
[Nothing's official Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit).
To build locally, put `glyph-matrix-sdk-2.0.aar` from that repo into `app/libs/`.

Fonts: Outfit, Bricolage Grotesque, Nunito, Caveat and Doto (SIL Open Font License),
Special Elite (Apache 2.0). Licenses are in `fonts-licenses/`.
