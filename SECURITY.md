# Security

## Reporting a problem

Please report security problems **privately**: on this repository, open the
**Security** tab and choose **Report a vulnerability**. Please don't open a
public issue for them.

This is a one-person hobby project, so replies may take a few days.

## How the app protects your data

- **No permissions and no internet access.** A check in every build fails if
  any permission outside `config/allowed-permissions.txt` is added.
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
