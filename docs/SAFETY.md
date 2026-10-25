# Safety and privacy

## Before installation

1. Confirm ADB authorization and record the serial number.
2. Run `backup.sh` and store its output privately.
3. Run `dry-run.sh` using the correct Fire OS profile.
4. Read `restore.sh` and keep the computer on the same network until restoration is tested.

## What installation changes

- Installs the Minimal Home APK.
- Enables its package for Android user 0.
- Records it as the preferred Home activity, even when Fire OS ignores that preference.
- Enables the narrow accessibility redirect on Fire OS 7, or grants Usage Access and overlay
  access on Fire OS 6.
- Starts Minimal Home.

It does not uninstall, disable, or clear data from Amazon packages.

## What restoration changes

- Revokes Minimal Home's app operations.
- Removes only Minimal Home's accessibility component while preserving unrelated services.
- Restores Amazon's Home activity preference.
- Stops and disables Minimal Home without uninstalling it.

## Backup privacy

Generated backups may contain the device serial, private LAN address, installed applications,
system properties, settings, and screenshots. `backups/` is ignored by Git, but users remain
responsible for storing or deleting those files safely.

Never commit ADB private keys, Amazon credentials, account tokens, router credentials, or raw
device backups to a public repository.
