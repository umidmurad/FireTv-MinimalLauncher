# Fire OS profiles

Amazon protects its launcher on the tested Fire OS builds. Android's normal Home preference can
be recorded without becoming the activity that actually opens when the remote Home button is
pressed.

## Fire OS 7

The `fire-os-7` installer profile enables `HomeRedirectService`. It listens only for window-state
changes from Amazon's exact Home activity and launches Minimal Home. A local accessibility
overlay covers the stock page during Fire OS's delayed app switch.

The service cannot inspect window content and the app has no Internet permission.

## Fire OS 6

The tested Fire OS 6 device removed third-party accessibility registrations. The `fire-os-6`
profile therefore grants Minimal Home Usage Access and Draw Over Other Apps.

`HomeMonitorService` reads recent foreground-transition events, reacts only to Amazon Home, and
stores nothing. The overlay permission is used for a non-interactive local wallpaper mask while
Fire OS completes the delayed launch. `BootReceiver` restarts the monitor after reboot only when
Usage Access is already granted.

## Unsupported or changed builds

If Amazon changes the Home activity name, both strategies may stop working safely: Amazon Home
will remain visible and Minimal Home will still be launchable as an ordinary app. Do not broaden
the monitored package or accessibility event scope without understanding the privacy impact.
