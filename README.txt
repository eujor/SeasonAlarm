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
