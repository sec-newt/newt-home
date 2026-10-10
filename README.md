# Newt Home

A private home screen (launcher) for Android, built for GrapheneOS. It looks like
a terminal (neon Tokyo Night colors, monospace text, a `~$` prompt) but works by
tapping, with large text and icons for people with low vision.

> **A personal project, in early development.** Keep your phone's stock launcher
> installed so you can switch back at any time.

The plan and mock-up are in [PLAN.md](PLAN.md). This is the second Newt app,
after [Newt Phone](https://github.com/sec-newt/newt-phone), and follows the
same [house style](https://github.com/sec-newt/newt-phone/blob/main/NEWT_STYLE.md).

## What it does so far

- **Time and date at the top.** Tap the time to open the clock app, or the date to
  open the calendar app (GrapheneOS has none built in; Etar on F-Droid works).
- **Two columns:** favorites on the left (`~$ ls favs`), folders as a tree on the
  right (`~$ tree`). With very large text they stack instead.
- **Favorites.** Press and hold any app and choose **Add to favorites**. In the same
  menu, **Move up** and **Move down** change their order.
- **Folders.** Tap a folder to open it in place; the one you opened last stays open.
  Press and hold an app and choose **Move to folder** (or make a new one there).
  Press and hold a folder to rename, move or delete it; deleting a folder never
  removes its apps. It starts with AI, Blind tools, Games and Productivity.
- **All apps.** Tap **› All apps**, or type `ls` at the prompt.
- **The prompt.** Type part of an app's name, or a folder's name; capitals don't matter. When only one
  app matches (`sig` → Signal), it opens by itself after a moment. Keep typing to
  cancel. **Enter** opens the top result.
- **Home and Back** clear the prompt and return to favorites.
- **The gadget look:** faint scanlines and a dim ASCII newt behind everything, the
  prompt typing out `date` when you come home (skipped when animations are off),
  a neon glow on whatever you press, and a color for each folder.
- **Terminal status line.** `[bat 54%] [wifi ●]` replaces Android's status bar on the
  home screen. Swipe down from the top edge to bring Android's bar and your
  notifications back.

Websites as apps and app shortcuts are next (see the plan).

## Privacy

- **No internet access, and one small permission.** The app can't send anything
  anywhere. Its only permission, "view network connections", lets the status line
  show whether you're on Wi-Fi or mobile data. The build fails if any other
  permission is ever added (see `config/allowed-permissions.txt`).
- It only sees apps that can be opened from a home screen, not everything installed.
- Favorites, folders and how often you open each app stay on the phone, in the app's private
  storage, and are excluded from backups.
- No Google Play Services, Firebase, analytics or ads.

## Install

With [Obtainium](https://github.com/ImranR98/Obtainium):

1. In Obtainium, tap **Add app** and paste this repository's link.
2. Install **Newt Home**.
3. Make it your home screen: **Settings → Apps → Default apps → Home app → Newt Home**.

To go back, choose your old launcher in the same place. Requirements: Android 13
or newer.

## Automatic checks

Every pull request and every push to `main` runs these on GitHub:

| Check | What it catches |
| --- | --- |
| Logic tests | Mistakes in search, opening the only match, and favorites |
| Screenshots | The home screen in light, dark and at 200% text (download the `screenshots` artifact to look) |
| Accessibility | Low contrast, small touch targets, missing screen-reader labels |
| Lint | Common Android bugs and security problems |
| Permission audit | Any permission not listed in `config/allowed-permissions.txt` |
| Secret scan | Passwords or keys accidentally committed, across the whole history |

Test builds from pull requests are named **Newt Home (test)** and install
alongside the real app. They're under the **Actions** tab, in the run's
**Artifacts** section, as a zip containing the APK.

## Release signing

Every change merged into `main` that passes all checks is published as a signed
release. Releases use **the same signing key as Newt Phone**, so Newt apps can
later trust each other (for example, to show Newt Phone's missed calls). Add the
same two secrets to this repository (**Settings → Secrets and variables → Actions**):

| Secret | Contents |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | The keystore file (`release.p12`), base64-encoded |
| `SIGNING_PASSWORD` | Its password (the key alias is `dialer`) |

Without these secrets the build still runs and skips publishing a release.

## License

Newt Home is free software under the [GNU General Public License v3.0](LICENSE).
Bundled fonts are under the SIL Open Font License; their license files are in
`app/src/main/assets/licenses`.
