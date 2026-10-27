package com.eujor.seasonalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.provider.Settings;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlarmScheduler {
    public static final String EXTRA_ID = "alarm_id";
    public static final String EXTRA_SNOOZE = "is_snooze";
    private static final String STATUS_CHANNEL = "scheduled_alarm_status";
    private static final int STATUS_NOTIFICATION_ID = 43;
    private final Context context;
    private final AlarmManager am;

    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public boolean canScheduleExact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return am.canScheduleExactAlarms();
        return true;
    }

    public Intent exactAlarmSettingsIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.getPackageName()));
        }
        return null;
    }

    public void cancel(long id) {
        Intent i = new Intent(context, AlarmReceiver.class).putExtra(EXTRA_ID, id);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode(id, false), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    public void scheduleNext(Alarm alarm) {
        cancel(alarm.id);
        if (!alarm.enabled || !canScheduleExact()) { refreshUpcomingNotification(); return; }
        ZonedDateTime next = nextOccurrence(alarm, Instant.now());
        if (next == null) {
            alarm.enabled = false;
            new AlarmRepository(context).save(alarm);
            refreshUpcomingNotification();
            return;
        }
        Intent fire = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ID, alarm.id)
                .putExtra(EXTRA_SNOOZE, false);
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode(alarm.id, false), fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(context, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(context, requestCode(alarm.id, true), show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), showPi);
        am.setAlarmClock(info, operation);
        refreshUpcomingNotification();
    }

    public void snooze(Alarm alarm, int minutes) {
        long when = System.currentTimeMillis() + Math.max(1, minutes) * 60_000L;
        Intent fire = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ID, alarm.id)
                .putExtra(EXTRA_SNOOZE, true);
        PendingIntent operation = PendingIntent.getBroadcast(context, requestCode(alarm.id, true), fire,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(context, MainActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(context, requestCode(alarm.id, false), show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), operation);
        refreshUpcomingNotification();
    }

    public void rescheduleAll() {
        for (Alarm a : new AlarmRepository(context).all()) if (a.enabled) scheduleNext(a);
        refreshUpcomingNotification();
    }

    public void refreshUpcomingNotification() {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                STATUS_CHANNEL, "Scheduled alarm status", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows that SeasonAlarm has an upcoming alarm scheduled");
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);

        Alarm best = null;
        ZonedDateTime bestTime = null;
        Instant now = Instant.now();
        for (Alarm a : new AlarmRepository(context).all()) {
            if (!a.enabled) continue;
            ZonedDateTime next = nextOccurrence(a, now);
            if (next != null && (bestTime == null || next.toInstant().isBefore(bestTime.toInstant()))) {
                best = a;
                bestTime = next;
            }
        }

        if (best == null || bestTime == null) {
            nm.cancel(STATUS_NOTIFICATION_ID);
            return;
        }

        PendingIntent open = PendingIntent.getActivity(
                context, STATUS_NOTIFICATION_ID,
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        ZonedDateTime local = bestTime.withZoneSameInstant(TimeZones.resolveZone(TimeZones.HOME_ZONE));
        String when = local.format(DateTimeFormatter.ofPattern("EEE dd MMM • HH:mm"));
        Notification n = new Notification.Builder(context, STATUS_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Alarm set • " + when)
                .setContentText(best.label + " • " + best.cityName)
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .build();
        nm.notify(STATUS_NOTIFICATION_ID, n);
    }

    private int requestCode(long id, boolean alt) {
        long x = id ^ (id >>> 32);
        int base = (int)(x & 0x7fffffff);
        return alt ? base ^ 0x3f3f3f3f : base;
    }

    public static ZonedDateTime nextOccurrence(Alarm a, Instant after) {
        ZoneId zone = TimeZones.resolveZone(a.zoneId);
        ZonedDateTime now = after.atZone(zone);
        LocalTime time = LocalTime.of(a.hour, a.minute);

        if ("ONCE".equals(a.repeatMode)) {
            LocalDate d;
            try { d = LocalDate.parse(a.onceDate); }
            catch (Exception e) { d = now.toLocalDate(); }
            ZonedDateTime z = LocalDateTime.of(d, time).atZone(zone);
            if (z.toInstant().isAfter(after)) return z;
            return null;
        }

        if ("DATES".equals(a.repeatMode)) {
            List<String> copy = new ArrayList<>(a.dates);
            Collections.sort(copy);
            for (String s : copy) {
                try {
                    LocalDate d = LocalDate.parse(s);
                    ZonedDateTime z = LocalDateTime.of(d, time).atZone(zone);
                    if (z.toInstant().isAfter(after)) return z;
                } catch (Exception ignored) {}
            }
            return null;
        }

        LocalDate start = now.toLocalDate();
        for (int i=0;i<370;i++) {
            LocalDate d = start.plusDays(i);
            int dow = d.getDayOfWeek().getValue();
            boolean match = switch (a.repeatMode) {
                case "DAILY" -> true;
                case "WEEKDAYS" -> dow >= DayOfWeek.MONDAY.getValue() && dow <= DayOfWeek.FRIDAY.getValue();
                case "SELECTED_DAYS" -> a.weekdays.contains(dow);
                default -> false;
            };
            if (!match) continue;
            ZonedDateTime z = LocalDateTime.of(d, time).atZone(zone);
            if (z.toInstant().isAfter(after)) return z;
        }
        return null;
    }

    public static String nextSummary(Alarm a) {
        ZonedDateTime target = nextOccurrence(a, Instant.now());
        if (target == null) return "No future occurrence";
        ZonedDateTime home = target.withZoneSameInstant(TimeZones.resolveZone(TimeZones.HOME_ZONE));
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE dd MMM, HH:mm");
        return target.format(f) + " " + shortZone(target) + "  →  " + home.format(f) + " local";
    }

    private static String shortZone(ZonedDateTime z) {
        return z.format(DateTimeFormatter.ofPattern("z"));
    }
}
