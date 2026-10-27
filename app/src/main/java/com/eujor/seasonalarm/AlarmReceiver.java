package com.eujor.seasonalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1);
        boolean snooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false);
        Alarm alarm = new AlarmRepository(context).find(id);
        if (alarm == null) return;

        Intent service = new Intent(context, AlarmRingingService.class)
                .putExtra(AlarmScheduler.EXTRA_ID, id);
        context.startForegroundService(service);

        if (!snooze) {
            if ("ONCE".equals(alarm.repeatMode)) {
                alarm.enabled = false;
                new AlarmRepository(context).save(alarm);
                new AlarmScheduler(context).refreshUpcomingNotification();
            } else {
                new AlarmScheduler(context).scheduleNext(alarm);
            }
        }
    }
}
