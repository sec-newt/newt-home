# Newt Home

A launcher for GrapheneOS that looks like a terminal but works by tapping. The owner
has low vision; readability beats everything else. What it does is in
[README.md](README.md), the plan in [PLAN.md](PLAN.md), the shared rules in the
[Newt house style](https://github.com/sec-newt/newt-phone/blob/main/NEWT_STYLE.md).

## Stack

Kotlin, Jetpack Compose, Material 3. The theme and shared pieces come from `newt-ui`, and
the screenshot and accessibility test helpers (`newtScreenshot()`) from `newt-test`. Both
live in [newt-android-lib](https://github.com/sec-newt/newt-android-lib). Code is in
`app/src/main/java/io/github/secnewt/home/`, tests in `app/src/test/`.

## Checks: run them and show the output

CI runs the same checks on every PR (see `.github/workflows/build.yml`).

```sh
./gradlew recordRoborazziDebug   # logic, screenshot and accessibility tests
./gradlew lintDebug              # lint, including Android security checks
./gradlew assembleDebug && scripts/check-permissions.sh app/build/outputs/apk/debug/app-debug.apk
```

Paste the result. Don't say "tests pass" without the output.

## Rules that are easy to break

- **No internet permission, ever.** New permissions go in `config/allowed-permissions.txt`
  with a plain sentence saying why; the build fails otherwise.
- **7:1 contrast** for every text color, **no text under 16sp**, buttons at least 56dp
  and rows at least 48dp tall, words next to icons. Must work at 200% text size and with TalkBack, and no two
  things on a screen read out the same.
- **Every feature can be switched off**, and a command (`sky off`) and a switch at the end of
  `help` do the same thing. Typing is a shortcut, never required.
- **Animations stop when Android's animations are off.**
- **README.md is the user's manual.** Update "What it does so far" when behavior changes,
  in plain words.

## Before opening a PR

1. Run the checks above.
2. Run `/code-review` and `/security-review` and fix what they find, or say why not.
3. Fill in the PR template (Testing, Risk, How to revert).

Merge only after the owner says "merge it". Merges to `main` publish a signed release.
