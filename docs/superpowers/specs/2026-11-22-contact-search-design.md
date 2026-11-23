# People in search

Date: 2026-11-22. Goal: type part of someone's name at the prompt, see them in the results,
and call, text or open them right from the home screen, without opening another app first.

## Approaches
- **A. Newt Home reads contacts itself (picked).** Asks for contact access when the feature
  is switched on, like today's events and the calendar. One repo, works with any phone or
  messages app. The launcher holds read access to the address book; it has no internet
  permission, so nothing can leave the phone.
- B. Newt Phone shares matching contacts through a Newt-signed provider, like the missed-call
  count. Two repos, works only with Newt Phone, and the privacy gain is small (it would still
  hand names and numbers to any Newt-signed app). Not chosen.

## What you see
```
find sar
  [S] Sargasso                 ← apps first, as today
people
  Sarah Lee                    ← tap to show the buttons
    [ Call ]  [ Text ]  [ Open ]
```
- Contacts show under a `people` heading, after the app results, up to 5, with the same
  loose matching as apps (`srh` finds Sarah). Capitals never matter.
- Tapping a name shows three buttons below it (each at least 56dp, words not icons).
  The buttons sit under the name, not beside it, so they still fit at 200% text.
  - **Call**: the number goes into the dialpad and you press call. Nothing calls by itself.
  - **Text**: a new message to that number in your messages app.
  - **Open**: their contact card in your contacts app.
  - More than one number: Call and Text ask which one, showing the number and its type
    ("mobile", "home").
- Only contacts with a phone number show.
- TalkBack: the name reads "Sarah Lee, contact"; buttons read "Call Sarah Lee", "Text Sarah
  Lee", "Open Sarah Lee", so nothing reads the same.

## Rules
- **Off until you switch it on.** `people on` / `people off`, and a "People in search"
  switch at the end of `help`. Android asks for contact access the first time you switch it
  on. If you say no, a line explains how to turn it on in Settings, as calendar does now.
  The word is `people`, not `contacts`: a command word stops apps starting the same way
  from opening by themselves, and a Contacts app is common. If you say no to access, the
  switch goes back off.
- **A contact never opens by itself.** If a contact matches, the "only one app matches, opens
  by itself" timer doesn't run, so typing `sam` can't launch an app when you meant Sam.
- Names and numbers are read into memory only, never saved, and reread when contacts change.
  Work-profile and Private space contacts are left out.
- New permission `android.permission.READ_CONTACTS` in `config/allowed-permissions.txt`,
  with its reason.

## Builder's calls
- Where the people code lives (likely `apps/ContactRepository.kt` beside
  `CalendarRepository.kt`) and how matching reuses `AppSearch`.
- The `call` command stays as it is.
- Tests: matching and the "no auto-open when a contact matches" rule as logic tests;
  screenshots of results with people, the opened buttons, and the number picker, at normal
  and 200% text.
- README "What it does so far" gets a plain-words entry.
