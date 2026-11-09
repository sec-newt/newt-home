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
- **Today's events (optional).** The first time, a line under the date asks whether to
  show today's events. Say yes and Android asks for calendar access; then a line like
  `next: 3:00 Dentist · 2 more today` sits under the date. Tap it or the date for the
  day's list, and tap an event to open it in your calendar app. **Stop showing events**
  in that list turns it off; press and hold the date to be asked again.
- **Color that means something.** The battery shows as a little bar, green when it's
  fine, amber at 30% and below, red at 15% and below, with a bolt while charging; the
  percent is always written next to it. Each event has a dot in its calendar's own color,
  on the home screen and in the day's list.
- **Two looks.** The terminal look (the `~$` prompt, scanlines and the ASCII newt) or the
  clean look: plain cards and words, like Newt Phone, with every feature the same. Type
  `look clean` or `look terminal` at the prompt, or use the button at the end of `help`.
  Newt Home remembers your choice.
- **Sky colors.** In both looks, the outlines and headings follow the time of day (amber at
  dawn, cyan by day, purple and orange at dusk, the usual blue and purple at night), and the
  weather wins when Newt Weather is sharing: steel blue for rain, icy for snow, amber and red
  for a storm or warning. Only those accents change; text colors never do, and every sky color
  keeps 7:1 contrast (a test checks each one). `sky off` keeps the usual colors.
- **The newt by the clock (clean look).** The newt from the app icon sits beside the time, in
  the sky's colors, with a small badge and a word for what's going on: `storm warning`,
  `battery low` (it droops), `charging`, `rain` or `rain soon`, `snow`, `Lord's day` (Sunday
  morning), `sleeping` (late at night, eyes closed) or `sunny`. With nothing to say it's just
  the newt. It blinks now and then unless animations are off. Tap it for the phone's status.
  `pet off` hides it.
- **Small tiles (clean look).** Favorites show the icon beside the name, so eight fit with
  room to spare; the date and weather share a line. `tiles big` brings back the tall tiles.
- **Every one of these is a switch.** At the end of `help`, under **Screen**: Sky colors,
  Newt by the clock, Big tiles and Math answers, next to Clean look and Screen buttons.
- **Swipe down to close.** Every card that slides up (the calendar, commands, app
  menus and the rest) closes with a swipe down, as well as with back or tapping outside.
  It follows your finger, so a short drag springs back.
- **Three screens.** Home has the time, date and favorites (`~$ ls favs`). Swipe right
  for folders (`~$ tree`), or swipe up past the end of the list for all apps (`~$ ls`).
  The **← Folders** and **All apps ↑** buttons above the prompt do the same, so
  swiping is never required. If you'd rather swipe, `buttons off` hides them (or use the
  button at the end of `help`); `buttons on` brings them back. Back and Home return to
  the home screen.
- **Favorites.** Press and hold any app and choose **Add to favorites**. In the same
  menu, **Move up** and **Move down** change their order.
- **Folders.** Tap a folder to open it in place; the one you opened last stays open.
  Press and hold an app and choose **Move to folder** (or make a new one there).
  Press and hold a folder to rename, move or delete it; deleting a folder never
  removes its apps. It starts with AI, Blind tools, Games and Productivity.
- **All apps.** Tap **All apps ↑**, swipe up, or type `ls` at the prompt. Swipe down
  from the top of the list (or use back) to go home again. The **letter
  strip** down the right side jumps to a letter: touch it, or slide your thumb along it,
  and the letter shows big in the middle of the screen while the list follows. Or tap
  **Letters** (or any letter heading) for the **letter grid**: big buttons for each
  letter. Tap **S** and only the S apps are listed, with nothing to scroll.
- **Hide apps.** Press and hold an app and choose **Hide from lists**. It leaves All apps
  and folders, but search still finds it, and it's tucked under **▸ hidden apps** at the
  end of All apps. **Show in lists** puts it back.
- **Search words.** Press and hold an app and choose **Add search words**, like `bank` or
  `email`, so typing one finds it even when you can't remember its name. Results say
  "your word: bank" so you know why it showed up.
- **The prompt.** It's on both home and folders. Type part of an app's name, or a folder's name; capitals don't matter.
  While you type, the clock, weather, events and buttons step aside so the results have
  the room the keyboard leaves; clear the prompt and they come back. When only one
  app matches (`sig` → Signal), it opens by itself after a moment. Keep typing to
  cancel. **Enter** opens the top result.
- **Commands.** Type `help` for the list. Each shows what it will do before you press
  **Enter** (or tap it), and none runs by itself:
  - `timer 10` (minutes), `timer 90s`, `timer 1h 30m`: starts a timer in your clock app.
  - `alarm 7:30`, `alarm 6pm`, `alarm 1845`: sets an alarm in your clock app.
  - `flashlight` or `torch` (or `flashlight on` / `off`). While it's on, the status line
    shows `[light on]`.
  - `call mom` opens Newt Phone's Contacts with "mom" searched; `call 555 0100` opens
    the dialpad with the number in. You still press call. `call` alone opens the phone app.
  - `wifi`, `bluetooth` and `files` open those.
  - `ask what's a good buttermilk substitute` hands the question to the Claude app (or
    lets you pick another app if Claude isn't installed). `ask` on its own opens Claude.
    Newt Home itself sends nothing anywhere.
  - `sys` (or `neofetch`) shows this phone's system info: Android version and build,
    model, uptime, app count, storage, memory and battery. Tapping the status line at
    the top does the same.
- **Math and conversions.** Type a sum or a conversion at the prompt and the answer shows
  as you type, worked out on the phone. **Enter** (or a tap) copies the number.
  - Math: `2 + 3 * 4`, `15% of 80`, `80 + 15%` (adds 15%), `(2 + 3) x 4`, `2^10`.
  - Conversions: `5 km to mi`, `1 cup to ml`, `350 f to c`, `1 GB to GiB`, `gallon to cups`.
    Length, weight, cooking (teaspoons to gallons), temperature, speed, time, data and area.
  - Just a number and a unit converts the usual way: `350 f` shows °C, `5 km` shows miles,
    `1 cup` shows milliliters.
  - Cooking amounts also give the fraction: `80 ml to cups` says "about 1/3".
  - It doesn't turn cups into grams; that depends on what's in the cup.
  - A plain number, or an app like 1Password, gets no answer. `answers off` stops the answers.
- **Speak instead of typing.** The microphone at the right of the empty prompt opens your
  phone's voice input app (FUTO Voice Input works, offline) and puts what you said at the
  prompt, as if typed: an app name opens like typing it, and a command shows what it will
  do so you can press Enter. Newt Home never uses the microphone itself.
- **Home and Back** clear the prompt and return to favorites.
- **The gadget look:** faint scanlines and a dim ASCII newt behind everything, the
  prompt typing out `date` when you come home (skipped when animations are off),
  a neon glow on whatever you press, and a color for each folder.
- **Terminal status line.** `[bat 54%] [wifi ●]` replaces Android's status bar on the
  home screen. Swipe down from the top edge to bring Android's bar and your
  notifications back.

- **Websites as apps.** In your browser (Vanadium or most others), choose **Add to home
  screen**. Newt Home asks first, then lists the site in All apps and search with a
  letter tile and "Vanadium shortcut" under its name. Press and hold it to add it to
  favorites or a folder, or **Remove shortcut**. This needs Newt Home to be your default
  home app.

- **Add a website by hand.** For sites where the browser's option doesn't work, tap
  **+ Add a website** at the end of All apps, type its address (like `weather.gov`) and
  an optional name. It opens in your browser and shows "website · weather.gov" under its
  name.

- **App shortcuts.** Press and hold an app and its own shortcuts come first, under
  **Jump to**: things like Signal's "New message" or a recent chat. Tap one to go straight
  there. (Android shares these only with the default home app.)

- **Work profile and Private space.** Their apps get their own sections at the end of All
  apps (`~/work/`, `~/private-space/`), each with **Lock** / **Unlock**. Work apps also
  show up in search, marked "work". Private space apps only ever show in their own
  section, and only while it's unlocked; unlocking asks for your PIN or fingerprint.
- **Missed calls.** With Newt Phone installed, "2 missed" shows in red under Phone.
  Newt Phone shares only that number, and only with apps signed with the Newt key, so
  Newt Home never sees your call history.
- **Unread messages.** With Newt Messages installed, "3 unread" shows under Messages, the
  same way. Only the number is shared, and only with Newt-signed apps.

## Privacy

- **No internet access, and four small permissions.** The app can't send anything
  anywhere. "View network connections" lets the status line show whether you're on
  Wi-Fi or mobile data. "Calendar" (read only) is asked for only if you choose to
  show today's events, and those events stay on the phone. "Set an alarm" lets the
  `timer` and `alarm` commands hand a new one to your clock app; it can't see or
  change the alarms you have. "Missed call count" reads just the number of
  missed calls from Newt Phone, which only Newt-signed apps can get. The flashlight
  needs no permission. The build fails if any
  other permission is ever added (see `config/allowed-permissions.txt`).
- It only sees apps that can be opened from a home screen, not everything installed.
- Favorites, folders, hidden apps, search words and how often you open each app stay on
  the phone, in the app's private storage, and are excluded from backups.
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
| Logic tests | Mistakes in search, opening the only match, favorites, and the calculator's math and conversions |
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
