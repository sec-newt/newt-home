# Security

## Reporting a problem

Please report security problems **privately**: on this repository, open the
**Security** tab and choose **Report a vulnerability**. Please don't open a
public issue for them.

This is a one-person hobby project, so replies may take a few days.

## How the app protects your data

- **No internet access.** The permissions are "view network connections"
  (Wi-Fi or mobile data, for the status line), which can't open connections, and
  read-only calendar access, asked for only if you choose to show today's events
  under the date. With no internet, events can't leave the phone. "Set an alarm"
  lets the `timer` and `alarm` commands hand a new timer or alarm to the clock
  app; it can't read or change existing ones. "Missed call count" reads only
  the number of unseen missed calls from Newt Phone; Newt Phone grants it only
  to apps signed with the Newt key, and no call history is shared. "Access
  hidden profiles" lets the default home app list Private space apps in their
  own section; nothing shows while Private space is locked. A
  check in every build fails if any permission outside
  `config/allowed-permissions.txt` is added.
- **It sees only what a home screen needs:** apps that can be opened from a
  launcher, not every installed app.
- **Data stays private.** Favorites and usage counts live in the app's private
  storage and are excluded from backups.

## How releases are protected

- Releases are built and signed by GitHub Actions from the `main` branch only,
  with a signing key stored as a GitHub secret. Pull requests from forks never
  see that key.
- Every change passes tests, Android lint, the permission check and a scan for
  leaked passwords or keys before release.
- Build tools are pinned to exact versions, and Dependabot proposes updates,
  which go through the same checks.
