package com.eujor.seasonalarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 90;
    private LinearLayout alarmsBox;
    private LinearLayout clocksBox;
    private LinearLayout permissionsBox;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { renderClocks(); handler.postDelayed(this, 1000); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (scheduler.canScheduleExact()) scheduler.rescheduleAll();
        renderPermissions();
        renderAlarms();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) renderPermissions();
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

        root.addView(section("REQUIRED ACCESS"));
        permissionsBox = new LinearLayout(this);
        permissionsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(permissionsBox);

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
        renderPermissions();
    }

    private void renderPermissions() {
        if (permissionsBox == null) return;
        permissionsBox.removeAllViews();

        if (Build.VERSION.SDK_INT >= 33) {
            boolean notifications = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (notifications) {
                permissionsBox.addView(text("✓ Notifications allowed", 14, false));
            } else {
                Button b = new Button(this);
                b.setText("ALLOW NOTIFICATIONS");
                b.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS));
                permissionsBox.addView(b);
            }
        } else {
            permissionsBox.addView(text("✓ Notifications available", 14, false));
        }

        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (scheduler.canScheduleExact()) {
            permissionsBox.addView(text("✓ Exact alarms allowed", 14, false));
        } else {
            Button b = new Button(this);
            b.setText("ALLOW EXACT ALARMS");
            b.setOnClickListener(v -> {
                Intent i = scheduler.exactAlarmSettingsIntent();
                if (i != null) startActivity(i);
            });
            permissionsBox.addView(b);
        }

        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            boolean fullScreen = nm != null && nm.canUseFullScreenIntent();
            if (fullScreen) {
                permissionsBox.addView(text("✓ Full-screen alarm access allowed", 14, false));
            } else {
                Button b = new Button(this);
                b.setText("ALLOW FULL-SCREEN ALARMS");
                b.setOnClickListener(v -> {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                });
                permissionsBox.addView(b);
            }
        } else {
            permissionsBox.addView(text("✓ Full-screen alarm access available", 14, false));
        }

        permissionsBox.addView(text("✓ Vibration permission included (no runtime prompt required)", 14, false));
        permissionsBox.addView(text("✓ Alarm tones use the Android system alarm-tone picker", 14, false));
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }
}
