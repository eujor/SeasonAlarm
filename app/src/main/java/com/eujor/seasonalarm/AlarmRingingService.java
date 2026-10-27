package com.eujor.seasonalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;

public class AlarmRingingService extends Service {
    public static final String ACTION_STOP = "com.eujor.seasonalarm.STOP";
    public static final String ACTION_SNOOZE = "com.eujor.seasonalarm.SNOOZE";
    public static final String SILENT_TONE = "SILENT";
    private static final String CHANNEL = "alarm_ringing";
    private MediaPlayer player;
    private Vibrator vibrator;
    private long alarmId = -1;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Active Season Alarm notifications");
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        // Sound/vibration are controlled by each alarm, not by the notification channel.
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlarm(); stopSelf(); return START_NOT_STICKY;
        }
        if (intent != null && ACTION_SNOOZE.equals(intent.getAction())) {
            long requestedId = intent.getLongExtra(AlarmScheduler.EXTRA_ID, alarmId);
            Alarm a = new AlarmRepository(this).find(requestedId);
            if (a != null) new AlarmScheduler(this).snooze(a, a.snoozeMinutes);
            stopAlarm(); stopSelf(); return START_NOT_STICKY;
        }

        alarmId = intent == null ? -1 : intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1);
        Alarm alarm = new AlarmRepository(this).find(alarmId);
        if (alarm == null) { stopSelf(); return START_NOT_STICKY; }

        Intent full = new Intent(this, RingActivity.class).putExtra(AlarmScheduler.EXTRA_ID, alarmId);
        PendingIntent fullPi = PendingIntent.getActivity(this, req(alarmId, 1), full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopI = new Intent(this, AlarmRingingService.class)
                .setAction(ACTION_STOP)
                .putExtra(AlarmScheduler.EXTRA_ID, alarmId);
        PendingIntent stopPi = PendingIntent.getService(this, req(alarmId, 2), stopI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent snoozeI = new Intent(this, AlarmRingingService.class)
                .setAction(ACTION_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ID, alarmId);
        PendingIntent snoozePi = PendingIntent.getService(this, req(alarmId, 3), snoozeI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.label)
                .setContentText(alarm.cityName + " • " + String.format("%02d:%02d", alarm.hour, alarm.minute))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullPi)
                .setFullScreenIntent(fullPi, true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi).build())
                .build();
        startForeground(42, n);
        play(alarm);
        new android.os.Handler(getMainLooper()).postDelayed(() -> { stopAlarm(); stopSelf(); }, Math.max(1, alarm.autoStopMinutes) * 60_000L);
        return START_NOT_STICKY;
    }

    private void play(Alarm alarm) {
        stopAlarm();

        if (!SILENT_TONE.equals(alarm.toneUri)) {
            Uri uri = alarm.toneUri == null || alarm.toneUri.isEmpty()
                    ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(alarm.toneUri);
            if (!startPlayer(uri) && !Settings.System.DEFAULT_ALARM_ALERT_URI.equals(uri)) {
                startPlayer(Settings.System.DEFAULT_ALARM_ALERT_URI);
            }
        }

        if (alarm.vibrate) {
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,700,350,700,350}, 1));
            }
        }
    }

    private boolean startPlayer(Uri uri) {
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
            return true;
        } catch (Exception e) {
            if (player != null) {
                try { player.release(); } catch (Exception ignored) {}
                player = null;
            }
            return false;
        }
    }

    private int req(long id, int salt) {
        long x = id ^ (id >>> 32);
        return ((int)(x & 0x7fffffff)) ^ (salt * 0x13579b);
    }

    private void stopAlarm() {
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; }
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
    }

    @Override public void onDestroy() { stopAlarm(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
