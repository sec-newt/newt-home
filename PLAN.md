# Newt Home Plan

Clickable mock-up: https://claude.ai/artifact/LYmb4u8upwZb4NaTQ2oYBA · Snapshot: Oct 9, 2026

## Overview

A private home screen (launcher) for GrapheneOS that looks like a terminal but works by tapping. Everyday apps are one tap away, folders are two taps away, and typing is a shortcut, never a requirement. This is the second Newt app, after Newt Phone, and follows the shared [Newt house style](https://github.com/sec-newt/newt-phone/blob/main/NEWT_STYLE.md).

- **Phone:** GrapheneOS. No Google services needed, with or without sandboxed Google Play.
- **No internet permission.** A launcher never needs one, and the build fails if it's ever added.
- **Distribution:** GitHub releases through Obtainium, the same as Newt Phone.
- **Stack:** Kotlin + Jetpack Compose. Every push builds on GitHub Actions; no local Android setup.

What I liked in other launchers, and what to avoid:

| Launcher | Take | Leave |
| --- | --- | --- |
| Dragon Launcher (current) | Fast launching without hunting through a grid | Setup screens full of unlabeled icons; unclear what a setting changes |
| KISS | Search first; results ranked by what I actually use | Plain look |
| Niagara | Category folders (AI, Blind tools, Games, Productivity) | Not open source |
| Terminal launchers (T-UI) | The fun of a prompt | Having to type commands for everything |

## Look: cyberpunk hacker

The feel is a hacker's terminal in a cyberpunk city: neon on true black. That's why the Newt colors are Tokyo Night. It's a terminal color scheme with neon blue, purple and cyan, so the launcher can lean into it fully.

- **Terminal session layout.** Lines start with `newt@home:~$`, text is monospace (JetBrains Mono), and the prompt sits at the bottom like a shell.
- **Newt colors and contrast rules** from the house style: Tokyo Night on `#050608`, every text color at least 7:1, accents blue `#85ABF8`, purple `#BB9AF7`, cyan `#7DCFFF`, green `#9ECE6A`, red `#FF7A93`. Tokyo Night Day in light mode.
- **Neon in the right places.** The blue-to-purple gradient is for outlines, the prompt box and pressed items. Never behind text you have to read.
- **Icons stay.** Each app's icon is drawn as a one-color line icon tinted in Newt colors. Android 13+ apps ship a monochrome icon for this; apps without one get a letter tile.
- **Readable first.** Big clock, rows at least 48dp tall, normal capitals in names, and no fake terminal clutter (no file sizes, permissions or made-up output).

## Home screen

Everything fits one screen. Nothing on it needs the keyboard.

```
(▪ = the app's icon)

newt@home:~$ date
13:22  fri 09 oct              ← tap time: Clock · tap date: Calendar

~$ ls favs          ~$ tree
▪ Phone             ▸ ▪ AI/
  2 missed          ▾ ▪ Blind tools/
▪ Messages            ├ Be My Eyes
▪ Signal              ├ Magnifier
▪ Camera              └ Speech Services
▪ Vanadium          ▸ ▪ Games/
                    ▸ ▪ Productivity/

~$ type an app…                ← the prompt
```

- **Clock and date at the top.** Tapping the time opens the clock app; tapping the date opens the calendar app. Calendar events can come later.
- **Two columns:** favorites on the left (`ls favs`), folders as a tree on the right (`tree`).
- **Folders open in place**, one at a time, so the screen never scrolls. The folder I opened last stays open next time.
- **Missed calls** show as "2 missed" in red under Phone, a word and not just a dot (see Newt family, below).
- **At large text sizes** the two columns stack into one. Every layout is checked at 200% text.

## The prompt (search)

- **Type part of a name** and the columns are replaced by `find …` results, each with its icon. Capitals never matter.
- **Loose matching:** `mgnf` finds Magnifier. It also matches folder names, my own search words and Android's app type (game, music, maps).
- **Each result says why it matched**, for example "Your keyword: bank" or "In Blind tools".
- **Results are ranked by use.** The apps I open most come first.
- **Opens the only match:** when exactly one app has the typed letters together in its name (`sig` → Signal), it opens by itself after about ¾ second. Typing another letter cancels it. Loose matches (letters scattered through a name), keyword and folder matches never open anything by themselves. This can be switched off.
- **Enter opens the top result.**
- **One command:** `ls` lists every app. Plain names always work, so there's nothing to learn.

## All apps (when I don't remember the name)

- **A–Z list** with recent apps at the top and a "New" label on recent installs.
- **Letter strip** down the side: sliding a thumb shows a big letter.
- **Letter grid,** for no scrolling: tap **S** and see only the S apps.
- Folders and search words also work as memory aids.

## Press-and-hold menu

Every option is a word with one plain line under it saying what it changes. No unlabeled icon buttons.

- Add to / Remove from favorites
- Move to folder
- Rename ("Only changes the name here")
- Add search words
- Hide from lists ("Search still finds it")
- App info, or Website details

**Arrange home** uses Up and Down buttons, so dragging is never needed. New folders and new favorites start there too.

## Websites as apps

- **Catch the browser's "Add to home screen."** Android hands these to the default launcher, so they show up in the app list, folders and search like any app, with a "Website" label. This works with Vanadium and most other browsers.
- **"Add a website" in the launcher:** for sites where that doesn't work, enter a name and address and choose the browser. These open as a normal browser tab.
- The launcher has no internet, so it can't fetch a site's logo. It makes a letter tile in Newt colors, and I can pick a picture instead.

## Features

| Feature | What it does | Android piece | Effort |
| --- | --- | --- | --- |
| Home screen | Clock, date, favorites, folder tree, prompt | Home app role | Medium |
| App list | Every launchable app, kept up to date as apps are installed and removed | `LauncherApps` | Low |
| Prompt search | Loose matching, ranking by use, opening the only match | App database | Medium |
| Folders | Create, rename, reorder; remember the last one open | App database | Low |
| Press-and-hold menu | Favorites, folders, rename, search words, hide | App database | Low |
| Monochrome icons | Line icons tinted in Newt colors, letter tile fallback | `AdaptiveIconDrawable` monochrome layer | Low |
| Websites | Accept "Add to home screen"; add a site by hand | Pinned shortcuts | Medium |
| App shortcuts | Long-press shortcuts, such as favorite contacts from Newt Phone | `LauncherApps` shortcuts (default launcher only) | Medium |
| Work profile / Private Space | Show those apps in their own section | `LauncherApps` profiles | Medium |

**Stretch ideas:** today's calendar events under the date; a Dragon-style gesture compass that shows labeled targets while you drag (never the only way to reach an app); more prompt words like `call mom`.

## Newt family

- **Newt Phone favorites** can appear as shortcuts on Phone's press-and-hold menu.
- **Missed-call count:** rather than giving the launcher call-log access, Newt Phone could share just the count with other Newt apps signed with the same key. Nothing else could read it.
- **Shared code:** copy the Newt theme and fonts from Newt Phone for now. Once both apps are stable, move them into a shared Newt library.
- **Icon:** the same newt with a new badge for the launcher (a prompt `>_`?).

## Privacy

- No internet permission, enforced by the permission allowlist check in CI.
- No `QUERY_ALL_PACKAGES`. The app list comes from `LauncherApps` plus a narrow "launchable apps" query.
- Usage counts, folders, search words and settings stay in the app's private storage and are excluded from backups.
- No notification access unless a later feature truly needs it, then off by default and explained.
- No Google Play Services, Firebase, analytics or ads.

## Roadmap

1. **Phase 1: A usable home screen.** Clock and date, favorites, folder tree, prompt search with opening the only match, the all-apps list, the press-and-hold menu and Arrange home.
   - Done when: a week as my only launcher, with my 5–10 daily apps one tap away and nothing I need hidden more than two taps or one search away.
2. **Phase 2: Websites and shortcuts.** Accept "Add to home screen", Add a website, app shortcuts, and the Newt Phone missed-call count.
3. **Phase 3: Extras.** Work profile and Private Space, calendar events under the date, the gesture compass.

- [x] Create the GitHub repo
- [x] Clickable mock-up
- [x] Project skeleton and APK build on GitHub Actions (copy Newt Phone's setup: CI checks, signed releases, Obtainium)
- [ ] Phase 1 build
  - [x] Home screen, favorites, prompt search, opening the only match, All apps
  - [x] Folders as a tree in a second column, remembered open folder, search by folder name
  - [ ] Hide apps, search words, letter strip and letter grid in All apps

## GrapheneOS notes

- **Making it the home app:** Settings → Apps → Default apps → Home app. Keep the stock launcher installed as a fallback.
- **Restricted settings:** if Android blocks anything for an app installed from a file, use Settings → Apps → Newt Home → ⋮ → Allow restricted settings, the same as Newt Phone.
- **Network permission:** not requested, so there's nothing to turn off.
- **Prior art to study:** KISS (search and ranking), Olauncher and its forks (minimal text home), Dragon Launcher (gestures), Lawnchair (shortcuts, Private Space).

## Open questions

- **Private Space:** what does GrapheneOS allow a third-party launcher to show? Needs a test on the phone.
- **Auto-open timing:** is ¾ second right, or should it be faster or slower?
- **Right column:** folders only, or also a short "recent" list?
- **Gestures:** does the compass earn a place, or does the prompt cover it?
