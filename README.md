# Fire TV Minimal Launcher

> A small, reversible Fire TV home screen that replaces everyday trips through Amazon's
> advertising-heavy interface with a fast app grid and rotating nature wallpapers.

**Status:** Working reference implementation
**Stack:** Java, Android SDK, ADB, Bash, Fire OS 6 and 7
**Project:** [100 Automations](https://umid.dev/100-automations.html)

## The problem

The stock Fire TV home screen is busy and slow to navigate when the goal is simply to open an
app. Replacing it is not as simple as selecting another Android launcher: Fire OS protects the
Amazon Home activity, and Fire OS 6 and Fire OS 7 handle Home-button redirects differently.

## The practical improvement

Minimal Home provides a television-first, five-column app grid with remote-friendly focus
states, a clock, and three bundled nature wallpapers. It has no Internet permission and does
not collect or transmit data.

The repository turns the customization into a repeatable workflow:

- back up the original Fire TV state;
- preview compatibility without changing the device;
- build the launcher from source;
- install the appropriate Fire OS 6 or Fire OS 7 Home-button strategy; and
- restore stock Amazon Home without uninstalling anything.

## What the public edition includes

- Alphabetical shortcuts for Amazon Appstore, Apple TV, Disney+, Downloader, Hulu, Max,
  Netflix, Paramount+, Peacock, Prime Video, Settings, Tubi, and YouTube.
- Local nature wallpapers created for this project.
- A Fire OS 7 accessibility-based Home redirect.
- A Fire OS 6 Usage Access monitor and temporary transition mask.
- Serial-number verification before every device-changing script.
- Backup, dry-run, install, build, and restore scripts.

This public edition deliberately does **not** disable Amazon packages. Debloat lists are highly
dependent on the model, Fire OS build, region, and features the owner uses.

## How it works

```text
Remote Home button
       |
       v
Amazon's protected Home activity
       |
       +-- Fire OS 7: narrowly scoped accessibility event
       |
       `-- Fire OS 6: local foreground-transition monitor
       |
       v
Minimal Home
       |
       v
App grid + clock + rotating local wallpaper
```

The redirect reacts only to `com.amazon.tv.launcher/.ui.HomeActivity_vNext`. The Fire OS 6
monitor reads recent foreground-transition events locally, stores no history, and sends no
data. The app does not request Internet access.

## Tested device profiles

| Device | Fire OS | Home strategy | Notes |
| --- | --- | --- | --- |
| Fire TV Stick Lite (`AFTSS`) | Fire OS 7 / Android 9 | Accessibility redirect | Amazon Home tile intentionally omitted |
| Fire TV Stick 4K (`AFTMM`) | Fire OS 6 / Android 7.1.2 | Usage Access monitor | Uses a temporary local wallpaper mask |

Other models may behave differently. Start with the dry run and keep a tested ADB recovery
path.

## Requirements

- A computer with `adb`
- A Fire TV on the same network
- ADB debugging enabled and the computer authorized on the television
- Android SDK 35 and a compatible JDK to build from source

## Quick start

1. Build the debug APK:

   ```sh
   ./scripts/build.sh
   ```

2. Record the Fire TV serial and create a private backup. The backup directory is ignored by
   Git:

   ```sh
   adb -s FIRE_TV_IP:5555 shell getprop ro.serialno
   ./scripts/backup.sh FIRE_TV_IP:5555 EXPECTED_SERIAL
   ```

3. Run the read-only compatibility preview:

   ```sh
   ./scripts/dry-run.sh FIRE_TV_IP:5555 EXPECTED_SERIAL fire-os-7
   ```

4. Install the matching profile:

   ```sh
   ./scripts/install.sh FIRE_TV_IP:5555 EXPECTED_SERIAL fire-os-7
   ```

   Use `fire-os-6` instead when appropriate.

The IP and serial are mandatory. There are no personal defaults in the public scripts.

## Restore Amazon Home

```sh
./scripts/restore.sh FIRE_TV_IP:5555 EXPECTED_SERIAL
```

The restore script revokes Minimal Home's special access, removes only its accessibility
service from the enabled list, restores Amazon Home, and disables Minimal Home. The APK remains
installed so the process stays reversible.

## Customize the launcher

- App shortcuts are defined in
  [`MainActivity.java`](launcher/app/src/main/java/dev/umid/firetvlauncher/MainActivity.java).
- Wallpapers live under
  [`drawable-nodpi`](launcher/app/src/main/res/drawable-nodpi/).
- Wallpaper provenance is documented in [Included visual assets](docs/ASSETS.md).
- The visible launcher name is in
  [`strings.xml`](launcher/app/src/main/res/values/strings.xml).

See [Customization](docs/CUSTOMIZATION.md) for package discovery and editing examples.

## Repository layout

```text
FireTv-MinimalLauncher/
├── launcher/           # Complete Android project
├── scripts/            # Build, backup, preview, install, and restore
├── docs/               # Fire OS profiles, customization, and safety notes
├── LICENSE
└── README.md
```

## Safety rules

- Never use a cleanup list copied from another model without reviewing every package.
- Keep ADB authorized until the restore path has been tested.
- Supply the device serial as well as its IP; DHCP addresses can change.
- Run `dry-run.sh` before `install.sh`.
- Do not publish generated backups. They may contain serials, settings, app inventories, and
  screenshots.
- Fire OS updates can change protected activities and redirect behavior.

Read [Safety and privacy](docs/SAFETY.md) before applying the project to a new device.

## Why there is no Amazon Home tile

On the tested Fire OS 7 device, intentionally leaving Amazon Home in the foreground can prevent
the next Home press from producing the accessibility event used by the redirect. Settings and
Amazon Appstore remain available as normal tiles. Use `restore.sh` whenever the full stock Home
experience is needed.

## License

Released under the [MIT License](LICENSE).

## Disclaimer

This independent project is not affiliated with or endorsed by Amazon, Apple, or any listed
streaming service. Fire TV and Fire OS behavior can vary by model, region, and software update.
Review the source and maintain a recovery path before making changes.
