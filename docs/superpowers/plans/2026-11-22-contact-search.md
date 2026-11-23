# People in Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Typing part of a name at the prompt shows matching contacts under a `people`
heading; tapping one shows Call, Text and Open.

**Architecture:** Pure logic (grouping rows into people, matching, phone-type words) lives
in `apps/People.kt` and is unit-tested. `apps/ContactRepository.kt` reads the phone's own
contacts (personal profile only) into memory, like `CalendarRepository`. `MainActivity` owns
the permission ask, refresh and the three intents; `HomeRoute` searches; `HomeScreen` draws.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Robolectric + Roborazzi (`newtScreenshot`).

**Spec:** `docs/superpowers/specs/2026-11-22-contact-search-design.md`

## Global Constraints

- No internet permission. The only new permission is `android.permission.READ_CONTACTS`.
- 7:1 contrast for every text color; no text under 16sp; buttons ≥ 56dp, rows ≥ 48dp; words, not icon-only.
- Works at 200% text and with TalkBack; no two things on a screen read out the same.
- The feature is **off by default**; `people on` / `people off` and a "People in search" switch at the end of `help`.
- Nothing calls by itself: Call uses `ACTION_DIAL`, never `ACTION_CALL`.
- A contact never opens by itself, and a contact match stops an app's auto-open.
- Names and numbers live in memory only: never written to prefs, files or logs.
- README "What it does so far" updated in plain words.

## Review Focus

1. **The same number saved twice, formatted differently** (`(555) 010-0100` and `5550100100`): one number, not two. Test in Task 2.
2. **A contact with no name** (number only): left out, not shown as a blank row. Test in Task 2.
3. **Accents and capitals** (`jose` finds "José Ruiz"): same as apps. Test in Task 2.
4. **Access taken away in Settings while away:** on return the people list is empty, and nothing crashes. `refreshPeople()` runs in `onResume` and checks the permission first (Task 3).
5. **A big address book with a loose query** (`sa` matching hundreds): at most 5 people shown, best matches first. Test in Task 2.

---

### Task 1: The `people` switch, off by default

**Files:**
- Modify: `app/src/main/java/io/github/secnewt/home/apps/Commands.kt` (`HomeSwitch`, `HELP`, `describe`)
- Modify: `app/src/main/java/io/github/secnewt/home/apps/HomeStore.kt:79-80`
- Modify: `app/src/main/java/io/github/secnewt/home/ui/HomeRoute.kt:117-118`
- Test: `app/src/test/java/io/github/secnewt/home/apps/CommandsTest.kt`

**Interfaces:**
- Produces: `HomeSwitch.PEOPLE`; `HomeSwitch.defaultOn: Boolean` (true for all but `BIG_TILES` and `PEOPLE`).

- [ ] **Step 1: Write the failing tests** in `CommandsTest`:

```kotlin
@Test
fun `people switch`() {
    assertEquals(Command.Switch(HomeSwitch.PEOPLE, true), ready("people on"))
    assertEquals(Command.Switch(HomeSwitch.PEOPLE, false), ready("People Off"))
    assertFalse(HomeSwitch.PEOPLE.defaultOn)
    assertFalse(HomeSwitch.BIG_TILES.defaultOn)
    assertTrue(HomeSwitch.SKY.defaultOn)
    assertEquals("show people from your contacts when you type", Commands.describe(Command.Switch(HomeSwitch.PEOPLE, true)) { _, _ -> "" })
    assertEquals("stop showing people when you type", Commands.describe(Command.Switch(HomeSwitch.PEOPLE, false)) { _, _ -> "" })
}
```

- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*CommandsTest*'`. Expected: FAIL, `PEOPLE` unresolved.

- [ ] **Step 3: Implement.** Add `defaultOn: Boolean = true` as the last `HomeSwitch` constructor parameter; `BIG_TILES` passes `false`. Add
  `PEOPLE("people", "People in search", "Shows people from your contacts as you type, to call, text or open", "on", "off", defaultOn = false)`.
  Replace `what != HomeSwitch.BIG_TILES` with `what.defaultOn` in `HomeStore.switch` and `HomeRoute.isOn` (and fix their comments). Add the `describe` texts from the test, plus `null -> "show or hide people when you type"`. Add `CommandHelp("people on", "Shows people from your contacts as you type. people off hides them", HelpTopic.PEOPLE)`.

- [ ] **Step 4: Run** the same command. Expected: PASS.

- [ ] **Step 5: Commit** `git commit -m "people on/off switch, off by default"`

---

### Task 2: People logic

**Files:**
- Create: `app/src/main/java/io/github/secnewt/home/apps/People.kt`
- Modify: `app/src/main/java/io/github/secnewt/home/apps/AppSearch.kt` (`onlyMatch`)
- Test: `app/src/test/java/io/github/secnewt/home/apps/PeopleTest.kt`, `AppSearchTest.kt`

**Interfaces:**
- Produces (all in `People.kt`, package `io.github.secnewt.home.apps`, no Android imports):
  - `data class PhoneNumber(val number: String, val type: String)`. `type` is a word like `"mobile"`, or `""` when there isn't one.
  - `@Immutable data class Person(val id: Long, val lookupKey: String, val name: String, val numbers: List<PhoneNumber>)`
  - `data class ContactRow(val id: Long, val lookupKey: String, val name: String?, val number: String?, val type: Int, val label: String?)`, one row from the phone's contacts.
  - `sealed interface PersonAction { data class Call(val number: String); data class Text(val number: String); data class Open(val person: Person) }`, all `: PersonAction`.
  - `object People { const val MAX = 5; fun fromRows(rows: List<ContactRow>): List<Person>; fun typeWord(type: Int, label: String?): String; fun search(people: List<Person>, query: String): List<Person> }`
  - `AppSearch.onlyMatch(results, query, name = null, othersMatch: Boolean = false)`. When `othersMatch` is true it returns null.

- [ ] **Step 1: Write the failing tests** in `PeopleTest` (made-up names, 555 numbers only):

```kotlin
private fun row(id: Long, name: String?, number: String?, type: Int = 2, label: String? = null) =
    ContactRow(id, "key$id", name, number, type, label)

@Test fun `rows for one contact become one person`() {
    val people = People.fromRows(listOf(row(1, "Sarah Lee", "555-0100", 2), row(1, "Sarah Lee", "555-0101", 1)))
    assertEquals(1, people.size)
    assertEquals(listOf(PhoneNumber("555-0100", "mobile"), PhoneNumber("555-0101", "home")), people[0].numbers)
}
@Test fun `the same number twice counts once`() {
    val p = People.fromRows(listOf(row(1, "Sam Ortiz", "(555) 010-0100"), row(1, "Sam Ortiz", "5550100100")))
    assertEquals(1, p[0].numbers.size)
}
@Test fun `no name or no number is left out`() {
    assertTrue(People.fromRows(listOf(row(1, null, "555-0100"), row(2, "  ", "555-0101"), row(3, "Ana Diaz", null))).isEmpty())
}
@Test fun `type words`() {
    assertEquals("mobile", People.typeWord(2, null))
    assertEquals("home", People.typeWord(1, null))
    assertEquals("work", People.typeWord(3, null))
    assertEquals("gym", People.typeWord(0, "Gym"))   // TYPE_CUSTOM uses its label, lower case
    assertEquals("", People.typeWord(7, null))       // TYPE_OTHER → no word
}
@Test fun `search matches loosely, ignores accents, best first`() {
    val people = People.fromRows(listOf(row(1, "José Ruiz", "555-0100"), row(2, "Sarah Lee", "555-0101"), row(3, "Rosa Hart", "555-0102")))
    assertEquals(listOf("José Ruiz"), People.search(people, "jose").map { it.name })
    assertEquals("Sarah Lee", People.search(people, "srh").first().name)
    assertEquals(listOf("Sarah Lee", "Rosa Hart"), People.search(people, "sa").map { it.name }) // start before inside
}
@Test fun `at most five`() {
    val many = People.fromRows((1L..40L).map { row(it, "Sam $it", "555-01${it.toString().padStart(2, '0')}") })
    assertEquals(5, People.search(many, "sam").size)
}
@Test fun `blank query finds nobody`() = assertTrue(People.search(People.fromRows(listOf(row(1, "Sam", "555-0100"))), " ").isEmpty())
```
In `AppSearchTest`, add: when exactly one app matches `"sig"`, `onlyMatch(results, "sig", othersMatch = true)` is null, and `othersMatch = false` still returns it.

- [ ] **Step 2: Run** `./gradlew testDebugUnitTest --tests '*PeopleTest*' --tests '*AppSearchTest*'`. Expected: FAIL, unresolved.

- [ ] **Step 3: Implement.** `fromRows`: group by `id` in first-seen order; name trimmed, skip blank; numbers deduped by their digits (keep `+`); skip people with no numbers. `typeWord` maps the `ContactsContract.CommonDataKinds.Phone.TYPE_*` ints as literals (1 home, 2 mobile, 3 work, 0 custom → `label.lowercase()`, everything else `""`), so tests need no Android. `search` = `AppSearch.search(people, query, name = { it.name }).map { it.item }.take(MAX)`. Add `othersMatch` to `onlyMatch` and update its KDoc ("a person matching too stops it").

- [ ] **Step 4: Run** the same command. Expected: PASS.

- [ ] **Step 5: Commit** `git commit -m "People: group, match and name phone types"`

---

### Task 3: Reading contacts, the permission, and the three actions

**Files:**
- Create: `app/src/main/java/io/github/secnewt/home/apps/ContactRepository.kt`
- Modify: `app/src/main/AndroidManifest.xml` (beside `READ_CALENDAR`, line 14)
- Modify: `config/allowed-permissions.txt`
- Modify: `app/src/main/java/io/github/secnewt/home/MainActivity.kt`

**Interfaces:**
- Consumes: `ContactRow`, `People.fromRows`, `Person`, `PersonAction`, `HomeSwitch.PEOPLE` (Tasks 1–2).
- Produces: `class ContactRepository(context: Context) { fun people(): List<Person> }`; in `MainActivity` a state `people: List<Person>`, passed to `HomeRoute(people = …, onPerson = ::actOn)`. Task 4 adds those two parameters.

- [ ] **Step 1: `ContactRepository.people()`** queries `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` for `CONTACT_ID, LOOKUP_KEY, DISPLAY_NAME_PRIMARY, NUMBER, TYPE, LABEL`, sorted by `DISPLAY_NAME_PRIMARY`. It builds `ContactRow`s and returns `People.fromRows(rows)`. On `SecurityException`/`RuntimeException` it returns `emptyList()`. This URI is the personal profile only, so work and Private space contacts are left out with no extra code. Say so in the KDoc.

- [ ] **Step 2: Permission.** Add `<uses-permission android:name="android.permission.READ_CONTACTS" />` to the manifest. Add to `allowed-permissions.txt`, in the file's style:
```
#
# People in search: names and phone numbers of your contacts, so typing part of a
# name shows them to call, text or open. Asked for only after you turn people on, and
# with no internet they can't leave the phone.
android.permission.READ_CONTACTS
```

- [ ] **Step 3: MainActivity wiring**, copying the calendar pattern (`askCalendar`, `refreshAgenda`, `calendarChanges`):
  - `askContacts` (`RequestPermission`). Denied: set `PEOPLE` back off (state + `store.setSwitch`) and `say("Contact access is off. Turn it on under Settings → Apps → Newt Home → Permissions.")`. Then `refreshPeople()`.
  - In the `Command.Switch` branch: after saving, if `what == PEOPLE && on && !canReadContacts()` call `askContacts.launch(READ_CONTACTS)`. Always `refreshPeople()` after any PEOPLE change.
  - `refreshPeople()`: if the switch is off or there's no permission, set `people = emptyList()` and stop watching. Otherwise register a `ContentObserver` on `ContactsContract.Contacts.CONTENT_URI` (once), then read `ContactRepository.people()` on `Dispatchers.IO` and set `people`. Call it in `onCreate` setup and `onResume`. Unregister where `stopWatchingCalendar()` is called.
  - `actOn(action: PersonAction)`:
    - `Call` → `Intent(ACTION_DIAL, Uri.fromParts("tel", number, null))`, else `say("No phone app found")`
    - `Text` → `Intent(ACTION_SENDTO, Uri.fromParts("smsto", number, null))`, else `say("No messages app found")`
    - `Open` → `Intent(ACTION_VIEW, Contacts.getLookupUri(person.id, person.lookupKey))`, else `say("No contacts app found")`
    - All go through the existing `tryStart`.

- [ ] **Step 4: Verify**: `./gradlew assembleDebug && scripts/check-permissions.sh app/build/outputs/apk/debug/app-debug.apk`. Expected: build succeeds and the script passes with `READ_CONTACTS` allowed. (Task 4 adds the `HomeRoute` parameters. If this task runs first, add them as unused parameters with defaults so it compiles.)

- [ ] **Step 5: Commit** `git commit -m "Read contacts when people is on; call, text and open"`

---

### Task 4: People in the results, docs

**Files:**
- Modify: `app/src/main/java/io/github/secnewt/home/ui/HomeRoute.kt` (parameters, search, `opening`)
- Modify: `app/src/main/java/io/github/secnewt/home/ui/HomeScreen.kt` (`HomeView.Results`, `resultItems`, new `PersonRow`)
- Modify: `README.md`, `PLAN.md`
- Test: `app/src/test/java/io/github/secnewt/home/ui/HomeScreenTest.kt`

**Interfaces:**
- Consumes: `Person`, `PersonAction`, `People.search`, `AppSearch.onlyMatch(…, othersMatch)` (Task 2).
- Produces: `HomeRoute(people: List<Person> = emptyList(), onPerson: (PersonAction) -> Unit = {})`; `HomeView.Results(…, people: List<Person> = emptyList())`; `HomeScreen(…, onPerson: (PersonAction) -> Unit = {})`.

- [ ] **Step 1: Write the failing tests** in `HomeScreenTest`, with made-up people (`Sarah Lee` with mobile `555-0100` and home `555-0101`; `Sam Ortiz` with one number). Extend the `results()` helper to take `people`:
  - `searchWithPeople()`: screenshot `search_people` (dark), query `"sa"`. Asserts `"People"` is displayed.
  - `personOpened()`: screenshot `search_person_open` (light). Click `"Sam Ortiz"`, then assert nodes with content descriptions `"Call Sam Ortiz"`, `"Text Sam Ortiz"`, `"Open Sam Ortiz"` exist.
  - `personNumberPicker()`: screenshot `search_person_numbers` (dark). Click `"Sarah Lee"`, then `"Call Sarah Lee"`. Assert `"mobile · 555-0100"` and `"home · 555-0101"` are displayed. Clicking `"mobile · 555-0100"` records `PersonAction.Call("555-0100")`.
  - `personOpenedLargeText()`: screenshot `search_person_open_200_percent`, `fontScale = 2f`, Sam opened.
  - `tappingCallWithOneNumberDials()`: Sam opened, click `"Call Sam Ortiz"`, records `PersonAction.Call(<Sam's number>)`.

  `render()` already runs the accessibility checks, which cover contrast, size and duplicate labels.

- [ ] **Step 2: Run** `./gradlew recordRoborazziDebug --tests '*HomeScreenTest*'`. Expected: FAIL to compile (no `people`).

- [ ] **Step 3: Implement.**
  - `HomeRoute`: `val found = remember(query, people) { if (query.isBlank() || isList || command != null) emptyList() else People.search(people, query) }`. Pass `othersMatch = found.isNotEmpty()` to `onlyMatch`. Put `found` in `HomeView.Results`. Thread `onPerson` through to `HomeScreen` (calling `reset()` after an action, like `open`).
  - `resultItems`: after the app rows, if `people` isn't empty, add a `PromptLine`-style heading titled `"People"` and one `PersonRow` per person (key `"person:${id}"`). The `# no app matches` hint shows only when apps *and* people are both empty.
  - `PersonRow` (same look as `AppRow`: height ≥ 48dp terminal / 60dp clean, `rowFont()`, 18/21sp). Tapping toggles open (`rememberSaveable` per row). The row's semantics read `"${name}, contact"`, with `onClickLabel` `"Show call, text and open"`. When open, a `FlowRow` of three buttons appears under the name, each ≥ 56dp tall, with text `Call`/`Text`/`Open` and content descriptions `"Call $name"` etc. Use the existing `NavButton` if it fits. With two or more numbers, Call/Text replace the buttons with one row per number. Each reads `"$type · $number"` (just the number when `type` is empty) and sends that action.
- [ ] **Step 4: Run** `./gradlew recordRoborazziDebug`. Expected: PASS. Look at the four new screenshots: nothing clipped at 200%, buttons under the name.

- [ ] **Step 5: Docs.** In README "What it does so far", next to Search words, add in plain words:
  "**People.** Type `people on` (or use the switch at the end of `help`) and typing part of a name also shows people from your contacts. Tap a name for **Call** (the number goes in the dialpad; you press call), **Text** or **Open**. Newt Home asks for contact access when you turn it on. With no internet, your contacts never leave the phone. A person never opens by itself."
  Add `people on` / `off` to the commands list. Add a ticked line to PLAN.md Phase 2.

- [ ] **Step 6: Full checks**, output pasted:
```sh
./gradlew recordRoborazziDebug
./gradlew lintDebug
./gradlew assembleDebug && scripts/check-permissions.sh app/build/outputs/apk/debug/app-debug.apk
```
Expected: all three succeed.

- [ ] **Step 7: Commit** `git commit -m "People in search results, with call, text and open"`
