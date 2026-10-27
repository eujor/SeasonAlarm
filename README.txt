SeasonAlarm v2.0
================

A timezone-aware Android alarm app for Android 10+.

V2 UI:
- Smart World Clocks / My Alarms tabs
- Region filters for major cities across the Americas, Europe, Africa, Asia and Oceania
- Live city clocks, current DST/standard-time status and offset vs Nairobi/EAT
- Smart alarm cards with next city time -> Nairobi/EAT conversion
- Polished alarm editor for city, repeat, tone, vibration and snooze
- Polished full-screen ringing UI
- SeasonAlarm launcher icon

Alarm functionality retained:
- Target-city local-time alarms
- Nairobi/EAT home reference
- Local device-time alarms
- DST-aware scheduling
- Weekdays / daily / selected weekdays / dates / one-time
- Exact alarms
- Tone picker + silent option
- Vibration toggle
- Snooze and dismiss
- Reboot/timezone-change recovery
- Android 10+ (API 29)

Cloud build:
GitHub -> Actions -> Build SeasonAlarm APK -> Run workflow.
Artifact: SeasonAlarm-v2-debug-apk

SeasonAlarm releases
--------------------
App Info -> Check other versions opens:
https://github.com/eujor/SeasonAlarm/releases

Pushing a version tag such as v2.2.0 triggers Publish SeasonAlarm Release and attaches the APK to GitHub Releases.
