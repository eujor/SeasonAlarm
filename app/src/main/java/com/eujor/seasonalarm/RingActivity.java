package com.eujor.seasonalarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RingActivity extends Activity {
    private long id;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        id = getIntent().getLongExtra(AlarmScheduler.EXTRA_ID,-1);
        Alarm a = new AlarmRepository(this).find(id);
        if (a == null) { finish(); return; }

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(30,80,30,50); root.setGravity(android.view.Gravity.CENTER);
        TextView label = new TextView(this); label.setText(a.label); label.setTextSize(28); label.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(label);
        TextView time = new TextView(this); time.setText(String.format("%02d:%02d",a.hour,a.minute)); time.setTextSize(52); time.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(time);
        TextView city = new TextView(this); city.setText(a.cityName); city.setTextSize(20); root.addView(city);

        Button snooze = new Button(this); snooze.setText("SNOOZE " + a.snoozeMinutes + " MIN"); snooze.setOnClickListener(v->{ action(AlarmRingingService.ACTION_SNOOZE); finish(); }); root.addView(snooze);
        Button dismiss = new Button(this); dismiss.setText("DISMISS"); dismiss.setOnClickListener(v->{ action(AlarmRingingService.ACTION_STOP); finish(); }); root.addView(dismiss);
        setContentView(root);
    }
    private void action(String a) { startService(new Intent(this,AlarmRingingService.class).setAction(a)); }
}
