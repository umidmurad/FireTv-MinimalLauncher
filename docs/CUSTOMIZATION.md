# Customization

## Change the app grid

The launcher entries are declared in `MainActivity.java` and displayed in their source order.
Keep them alphabetized for predictable remote navigation.

```java
AppEntry.app("Netflix", "com.netflix.ninja"),
AppEntry.settings("Settings", "com.amazon.tv.settings.v2"),
```

Use `appstore(...)` for Amazon Appstore and `settings(...)` for Fire TV Settings. Use `app(...)`
for ordinary Leanback-compatible applications.

To find an installed package:

```sh
adb -s FIRE_TV_IP:5555 shell pm list packages | sort
```

If an app has no launchable television activity, its tile will report that it is unavailable.
Missing apps appear dimmed rather than crashing the launcher.

## Change the launcher name

Edit `launcher/app/src/main/res/values/strings.xml`. Keep the package name unchanged unless you
also update the installer, restore script, Java source path, manifest namespace, and application
ID.

## Replace the wallpapers

Replace the three PNG files in `launcher/app/src/main/res/drawable-nodpi/` while keeping their
resource names. Use 16:9 images sized for a television. Avoid large source files that waste the
limited storage and memory available on older Fire TV hardware.

## Change rotation timing or columns

`WALLPAPER_INTERVAL_MS` and `GRID_COLUMNS` are near the top of `MainActivity.java`. The tested
defaults are 90 seconds and five columns.

## Build again

```sh
./scripts/build.sh
```

The debug APK is written to `launcher/app/build/outputs/apk/debug/app-debug.apk`.
