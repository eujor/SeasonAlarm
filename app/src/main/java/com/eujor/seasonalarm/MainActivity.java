package com.eujor.seasonalarm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {
    private LinearLayout alarmsBox;
    private LinearLayout clocksBox;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { renderClocks(); handler.postDelayed(this, 1000); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestNotificationPermission();
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (scheduler.canScheduleExact()) scheduler.rescheduleAll();
        renderAlarms();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 40);
        scroll.addView(root);

        TextView title = text("SEASON ALARM", 26, true);
        root.addView(title);
        root.addView(text("Home timezone: Nairobi, Africa (EAT)", 14, false));

        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (!scheduler.canScheduleExact()) {
            Button exact = new Button(this);
            exact.setText("ALLOW EXACT ALARMS");
            exact.setOnClickListener(v -> {
                Intent i = scheduler.exactAlarmSettingsIntent();
                if (i != null) startActivity(i);
            });
            root.addView(exact);
        }

        Button add = new Button(this);
        add.setText("+ NEW ALARM");
        add.setOnClickListener(v -> startActivity(new Intent(this, EditAlarmActivity.class)));
        root.addView(add);

        root.addView(section("WORLD CLOCKS"));
        clocksBox = new LinearLayout(this);
        clocksBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(clocksBox);

        root.addView(section("ALARMS"));
        alarmsBox = new LinearLayout(this);
        alarmsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(alarmsBox);

        setContentView(scroll);
    }

    private void renderClocks() {
        if (clocksBox == null) return;
        clocksBox.removeAllViews();
        String[][] clocks = {
                {"Nairobi", "Africa/Nairobi"}, {"London", "Europe/London"}, {"New York", "America/New_York"},
                {"Dallas / Texas", "America/Chicago"}, {"Seattle", "America/Los_Angeles"},
                {"Beijing", "Asia/Shanghai"}, {"Tokyo", "Asia/Tokyo"}, {"Singapore", "Asia/Singapore"},
                {"Sydney", "Australia/Sydney"}
        };
        for (String[] c : clocks) {
            TextView v = text(TimeZones.currentClockLine(c[0], c[1]), 15, false);
            v.setPadding(0, 10, 0, 10);
            clocksBox.addView(v);
        }
    }

    private void renderAlarms() {
        if (alarmsBox == null) return;
        alarmsBox.removeAllViews();
        List<Alarm> alarms = new AlarmRepository(this).all();
        if (alarms.isEmpty()) {
            alarmsBox.addView(text("No alarms yet.", 15, false));
            return;
        }
        for (Alarm a : alarms) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(0xffeef3f2);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cp);

            card.addView(text(a.label + "  •  " + String.format("%02d:%02d", a.hour, a.minute), 20, true));
            card.addView(text(a.cityName + "\n" + repeatLabel(a) + "\n" + AlarmScheduler.nextSummary(a), 14, false));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Switch enabled = new Switch(this);
            enabled.setText("Enabled");
            enabled.setChecked(a.enabled);
            enabled.setOnCheckedChangeListener((b, checked) -> {
                a.enabled = checked;
                new AlarmRepository(this).save(a);
                AlarmScheduler s = new AlarmScheduler(this);
                if (checked) s.scheduleNext(a); else s.cancel(a.id);
                renderAlarms();
            });
            row.addView(enabled);

            Button edit = new Button(this); edit.setText("EDIT");
            edit.setOnClickListener(v -> startActivity(new Intent(this, EditAlarmActivity.class).putExtra("id", a.id)));
            row.addView(edit);
            Button del = new Button(this); del.setText("DELETE");
            del.setOnClickListener(v -> {
                new AlarmScheduler(this).cancel(a.id);
                new AlarmRepository(this).delete(a.id);
                renderAlarms();
            });
            row.addView(del);
            card.addView(row);
            alarmsBox.addView(card);
        }
    }

    private String repeatLabel(Alarm a) {
        return switch (a.repeatMode) {
            case "DAILY" -> "Every day";
            case "WEEKDAYS" -> "Weekdays (Mon–Fri)";
            case "SELECTED_DAYS" -> "Selected weekdays " + a.weekdays;
            case "DATES" -> "Specific dates " + a.dates;
            case "ONCE" -> "Once on " + a.onceDate;
            default -> a.repeatMode;
        };
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(0xff132226);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private TextView section(String s) {
        TextView v = text(s, 17, true);
        v.setPadding(0, 28, 0, 8);
        return v;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
        }
    }
}
