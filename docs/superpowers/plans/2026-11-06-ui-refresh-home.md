# Newt Home UI refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Newt Home meets the UI refresh bar on newt-ui 2.0.0: the launcher (mockup C), Commands help (5), and the press-and-hold menu (6). Folders and All apps get 2dp borders. The letter grid stays as it is.

**Spec:** `newt-android-lib/docs/superpowers/specs/2026-11-04-ui-refresh-design.md`, Home rows; mockups `round1-screens.html` C and `round2-menus.html` 5 and 6.

## Global Constraints

- No text under 16sp. Never color alone. Words on every action.
- **▶ Try** only fills the command box; it never runs a command (mis-tap safety).
- Both looks (terminal and clean) keep working; the terminal look stays the default.
- Merge on green CI.

## Review Focus

1. **Search in Commands with no match:** say "No command matches", and keep the look and buttons switches reachable. (Task 2 test.)
2. **Try with a command that needs more input** ("call mom"): it's still just placed at the prompt, nothing runs. (Task 2 test via onPick only.)
3. **The hold menu for a website or shortcut:** Remove is red and sits apart from the grid, never as a neighboring tile. (Task 3 test.)
4. **No upcoming event:** no countdown card, and nothing says "in 0 min". (Task 4 test of `countdown()`.)
5. **An event already started:** "now" rather than a negative countdown. (Task 4 test.)

### Task 1: newt-ui 2.0.0 and the 16sp floor (done: f6c0f62)

### Task 2: Commands help as a cheat sheet
- `CommandHelp(example, does, topic: HelpTopic)`, where `enum HelpTopic(title) { TIME("Time"), PEOPLE("People"), PHONE("Phone"), APPS("Apps & look") }`.
- `HelpSheet` has a "Find a command" field and topic cards of command chips. Tapping a chip shows its one line plus **▶ Try** (calls `onPick(example)`).
- The look and buttons choices become `NewtSwitchRow`s: "Clean look" and "Screen buttons".
- Tests:
  - The topic headings show.
  - A chip shows its line, and Try calls onPick once with the example.
  - Search "wif" leaves only the wifi chip.
  - Search "zz" says "No command matches".
  - The look switch calls onSwitchLook.

### Task 3: Press-and-hold menu as jump-to chips plus a 2-column grid
- Shortcut chips in both looks.
- `NewtActionGrid`: Favorite/Unfavorite, To folder, Move up, Move down, Search words, Hide/Show.
- Remove (website or shortcut) is a red `NewtPrimaryButton(Danger)` under the grid.
- The last row is App info | Cancel.
- Tests:
  - The tiles are buttons, and "To folder" opens the folder page.
  - Remove is not inside the grid (it's a separate node below "Hide").
  - Cancel dismisses.

### Task 4: Launcher (mockup C)
- `fun countdown(startMillis: Long, nowMillis: Long): String?` gives "in 45 min", "in 2 h", "in 1 h 15 min", "now" (started, not over), or null (no event).
- In the clean look, the next event is a card: "NEXT · IN 45 MIN", then "15:00 Dentist", then "+2 more today". Weather sits on the date line. Favorites are 2-column tiles, with a red "2 missed" pill for Phone/Messages when there are any.
- In the terminal look, the agenda line gains "· in 45 min".
- Tests:
  - `countdown` cases.
  - The clean-look card text exists.
  - The favorites tiles are 2 columns (the second tile's left edge is ≥ half the width).
  - The missed pill reads "2 missed".

### Task 5: Folders / All apps 2dp borders, and the PR
- Home's own `neonOutline` default goes to 2dp.
- Run the full suite and lint, push, open the PR, merge when green.
