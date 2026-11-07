# Sky colors, the newt by the clock, and small tiles

Date: 2026-11-07. Goal: a prettier home screen that never gives up contrast, and favorites
that fit without scrolling.

## Small tiles (clean look)
- The UI refresh's tiles put the icon above the name: about 150dp each, so five favorites
  filled half the screen. Small tiles put the icon beside the name (64dp minimum), with
  "2 missed" in red under the name. Two to a row; one to a row at big text (as before).
- The date and the weather share a line (a FlowRow, so they wrap at big text).
- `tiles big` / Big tiles switch keeps the old tiles.

## Sky colors (both looks)
- Only `primary` and `secondary` change (the gradient outlines, headings, the newt). Text
  colors never change.
- Time of day: dawn 5–7, day 8–16, dusk 17–19, night otherwise (night = the usual colors).
  Weather wins: warning or thunderstorm → storm, rain, snow; cloudy/fog → overcast by day only.
- Every sky color is at least 7:1 on `background` and `surfaceContainerLow`, in dark and
  light; a faint glow behind the clock (14% at its brightest) keeps the clock and date at 7:1.
  `SkyTest` checks both.
- `sky off` / Sky colors switch.

## The newt by the clock (clean look)
- The app icon's newt (same paths as `ic_launcher_monochrome.xml`), filled with the sky
  gradient, beside the clock. No new art: poses are small edits (closed eyes; drooping legs).
- A badge (drawn icons, no new dependency) and a caption that says it in words, so it's never
  the only signal. Priority: storm warning → battery low → charging → rain → rain soon → snow
  → Lord's day (Sunday 6–12) → sleeping (22–6) → sunny → plain (no badge or caption).
- Blinks every 4–9 seconds unless animations are off. Tap: the phone's status (the `sys` card).
- `pet off` / Newt by the clock switch. The word isn't `newt` because every Newt app's name
  starts with it.

## Commands
`sky on|off`, `pet on|off`, `tiles big|small`. The word alone asks for on or off, so Enter
still opens an app that starts with it.
