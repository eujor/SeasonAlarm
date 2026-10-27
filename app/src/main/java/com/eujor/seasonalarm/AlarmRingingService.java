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
        nm.createNotificationChannel(ch);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAlarm(); stopSelf(); return START_NOT_STICKY;
        }
        if (intent != null && ACTION_SNOOZE.equals(intent.getAction())) {
            Alarm a = new AlarmRepository(this).find(alarmId);
            if (a != null) new AlarmScheduler(this).snooze(a, a.snoozeMinutes);
            stopAlarm(); stopSelf(); return START_NOT_STICKY;
        }

        alarmId = intent == null ? -1 : intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1);
        Alarm alarm = new AlarmRepository(this).find(alarmId);
        if (alarm == null) { stopSelf(); return START_NOT_STICKY; }

        Intent full = new Intent(this, RingActivity.class).putExtra(AlarmScheduler.EXTRA_ID, alarmId);
        PendingIntent fullPi = PendingIntent.getActivity(this, 1001, full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopI = new Intent(this, AlarmRingingService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 1002, stopI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent snoozeI = new Intent(this, AlarmRingingService.class).setAction(ACTION_SNOOZE);
        PendingIntent snoozePi = PendingIntent.getService(this, 1003, snoozeI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.label)
                .setContentText(alarm.cityName + " • " + String.format("%02d:%02d", alarm.hour, alarm.minute))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(fullPi, true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_lock_idle_alarm, "Snooze", snoozePi).build())
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", stopPi).build())
                .build();
        startForeground(42, n);
        play(alarm);
        return START_NOT_STICKY;
    }

    private void play(Alarm alarm) {
        stopAlarm();
        try {
            Uri uri = alarm.toneUri == null || alarm.toneUri.isEmpty()
                    ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(alarm.toneUri);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception ignored) {}

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

    private void stopAlarm() {
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; }
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
    }

    @Override public void onDestroy() { stopAlarm(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
