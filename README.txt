SEASON ALARM v1.0
=================

Android 10+ (API 29).

Core behavior
-------------
- You set an alarm in the TARGET CITY'S LOCAL TIME, not EAT.
- The app converts each next occurrence to the correct instant and Android rings locally.
- Nairobi/Africa (EAT) is the permanent home/reference timezone.
- DST/standard-time changes are handled by Android's IANA timezone database.
- Local device-time alarms are also supported.
- Repeat: weekdays, daily, selected weekdays, specific target-city calendar dates, or one-time.
- Selectable alarm tone, vibration, snooze, dismiss.
- Restores alarms after reboot/timezone/time changes.
- World clocks show Nairobi, London, New York, Texas, Seattle, Beijing, Tokyo, Singapore and Sydney.
- No GPS and no Internet connection are required for alarm calculations.

Cloud APK build
---------------
1. Upload the CONTENTS of this folder to a GitHub repository root.
2. Open GitHub > Actions > Build Season Alarm APK.
3. Run workflow.
4. Download artifact: SeasonAlarm-debug-apk.
5. Inside is app-debug.apk.

Exact alarms
------------
On Android 12+ the app may ask you to allow exact alarms. Allow it or alarms cannot be guaranteed at the requested exact minute.

VERSION 1.1 RELIABILITY CHECKLIST
---------------------------------
On first launch, Season Alarm shows a REQUIRED ACCESS section.
Grant every item shown there:
- Notifications (Android 13+)
- Exact alarms / Alarms & reminders (Android 12+)
- Full-screen alarms (Android 14+ when required by the device)

Vibration permission is a normal manifest permission and does not show a runtime popup.
Tone selection uses Android's system alarm-tone picker and needs no storage permission.
Each alarm can use the default tone, a selected system alarm tone, or Silent / No Tone.
Each alarm independently supports vibration on/off and a configurable snooze duration.
The ringing screen and the alarm notification both provide Snooze and Dismiss controls.

The app uses AudioAttributes.USAGE_ALARM, so the phone's Alarm volume controls loudness.
