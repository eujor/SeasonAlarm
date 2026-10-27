package com.eujor.seasonalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 90;
    private static final int TAB_CLOCKS = 0;
    private static final int TAB_ALARMS = 1;

    private LinearLayout contentBox;
    private LinearLayout clocksBox;
    private LinearLayout alarmsBox;
    private Button clocksTab;
    private Button alarmsTab;
    private TextView headerClock;
    private int activeTab = TAB_CLOCKS;
    private String activeRegion = "All";
    private int clockTicks = 0;
    private String clockQuery = "";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateHeaderClock();
            clockTicks++;
            if (activeTab == TAB_CLOCKS && clockTicks % 30 == 0) renderClocks();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.configure(this);
        buildUi();
        requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (scheduler.canScheduleExact()) scheduler.rescheduleAll();
        scheduler.refreshUpcomingNotification();
        showTab(activeTab);
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS && activeTab == TAB_ALARMS) showTab(TAB_ALARMS);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        root.setBackgroundColor(Ui.BG);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18));
        hero.setBackground(Ui.rounded(Ui.BRAND_DARK, 24, Ui.BRAND_DARK, 0, this));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 0, 0, Ui.dp(this, 12));
        hero.setLayoutParams(hp);

        TextView brand = Ui.text(this, "SeasonAlarm", 28, Color.WHITE, true);
        hero.addView(brand);
        TextView sub = Ui.text(this, "World-time alarms that follow each city's clock", 13, 0xffb9d5d1, false);
        sub.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 12));
        hero.addView(sub);

        headerClock = Ui.text(this, "", 17, Color.WHITE, true);
        hero.addView(headerClock);
        TextView home = Ui.text(this, "DETECTED DEVICE REGION • " + TimeZones.deviceRegionLabel(), 11, 0xff9fc7c1, true);
        home.setLetterSpacing(0.08f);
        home.setPadding(0, Ui.dp(this, 4), 0, 0);
        hero.addView(home);
        Switch dark = new Switch(this);
        dark.setText("Dark mode");
        dark.setTextColor(Color.WHITE);
        dark.setChecked(Ui.isDark(this));
        dark.setOnCheckedChangeListener((b1, checked) -> {
            getSharedPreferences("ui", MODE_PRIVATE).edit().putBoolean("dark", checked).apply();
            recreate();
        });
        hero.addView(dark);
        root.addView(hero);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        tabs.setBackground(Ui.rounded(Ui.CARD, 17, Ui.LINE, 1, this));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, 0, 0, Ui.dp(this, 12));
        tabs.setLayoutParams(tp);

        clocksTab = Ui.tabButton(this, "World Clocks", true);
        alarmsTab = Ui.tabButton(this, "My Alarms", false);
        clocksTab.setOnClickListener(v -> showTab(TAB_CLOCKS));
        alarmsTab.setOnClickListener(v -> showTab(TAB_ALARMS));
        tabs.addView(clocksTab);
        tabs.addView(alarmsTab);
        root.addView(tabs);

        contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        contentBox.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(contentBox);

        setContentView(root);
        updateHeaderClock();
        showTab(TAB_CLOCKS);
    }

    private void showTab(int tab) {
        activeTab = tab;
        Ui.styleTab(this, clocksTab, tab == TAB_CLOCKS);
        Ui.styleTab(this, alarmsTab, tab == TAB_ALARMS);
        contentBox.removeAllViews();
        if (tab == TAB_CLOCKS) buildClocksTab(); else buildAlarmsTab();
    }

    private void buildClocksTab() {
        LinearLayout intro = Ui.card(this);
        TextView title = Ui.text(this, "Regional clocks", 20, Ui.INK, true);
        intro.addView(title);
        TextView copy = Ui.text(this,
                "Choose an alarm in the city's local time. SeasonAlarm converts it to your detected device timezone automatically when DST or standard time changes.",
                13, Ui.MUTED, false);
        copy.setPadding(0, Ui.dp(this, 5), 0, 0);
        intro.addView(copy);
        contentBox.addView(intro);

        EditText search = new EditText(this);
        search.setHint("Search city or IANA timezone");
        search.setSingleLine(true);
        Ui.styleInput(this, search);
        search.setText(clockQuery);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence q, int st, int before, int count) { clockQuery = q.toString(); renderClocks(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        contentBox.addView(search, new LinearLayout.LayoutParams(-1, Ui.dp(this, 50)));

        contentBox.addView(Ui.sectionTitle(this, "REGION"));
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, 0, Ui.dp(this, 6), Ui.dp(this, 8));
        for (String region : TimeZones.regions()) {
            Button chip = Ui.chip(this, region, region.equals(activeRegion));
            chip.setOnClickListener(v -> {
                activeRegion = region;
                buildClocksTabRefresh();
            });
            chips.addView(chip);
        }
        hsv.addView(chips);
        contentBox.addView(hsv);

        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        frame.setBackground(Ui.rounded(Ui.CARD, 18, Ui.LINE, 1, this));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
        fp.setMargins(0, 0, 0, Ui.dp(this, 2));
        frame.setLayoutParams(fp);

        ScrollView resultsScroll = new ScrollView(this);
        resultsScroll.setFillViewport(true);
        resultsScroll.setVerticalScrollBarEnabled(true);
        clocksBox = new LinearLayout(this);
        clocksBox.setOrientation(LinearLayout.VERTICAL);
        clocksBox.setPadding(0, 0, 0, Ui.dp(this, 2));
        resultsScroll.addView(clocksBox, new ScrollView.LayoutParams(-1, -2));
        frame.addView(resultsScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        contentBox.addView(frame);
        renderClocks();
    }

    private void buildClocksTabRefresh() {
        showTab(TAB_CLOCKS);
    }

    private void renderClocks() {
        if (clocksBox == null) return;
        clocksBox.removeAllViews();
        List<City> cities = clockQuery.trim().isEmpty() ? TimeZones.cities() : TimeZones.searchableCities();
        for (City city : cities) {
            if (!clockQuery.trim().isEmpty() && !city.name.toLowerCase().contains(clockQuery.trim().toLowerCase()) && !city.zoneId.toLowerCase().contains(clockQuery.trim().toLowerCase())) continue;
            boolean homeMatch = "Home".equals(activeRegion) && (TimeZones.LOCAL_ZONE.equals(city.zoneId) || TimeZones.HOME_ZONE.equals(city.zoneId));
            if (!"All".equals(activeRegion) && !homeMatch && !activeRegion.equals(city.region)) continue;
            clocksBox.addView(clockCard(city));
        }
    }

    private View clockCard(City city) {
        ZoneId zone = TimeZones.resolveZone(city.zoneId);
        ZonedDateTime now = ZonedDateTime.now(zone);

        LinearLayout card = Ui.card(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        names.setLayoutParams(np);
        TextView name = Ui.text(this, city.name, 17, Ui.INK, true);
        names.addView(name);
        TextView region = Ui.text(this, city.region + "  •  " + now.format(DateTimeFormatter.ofPattern("z")), 11, Ui.MUTED, true);
        region.setPadding(0, Ui.dp(this, 2), 0, 0);
        names.addView(region);
        top.addView(names);

        TextView season = Ui.badge(this,
                zone.getRules().getTransitionRules().isEmpty() ? "FIXED" : (zone.getRules().isDaylightSavings(Instant.now()) ? "DST" : "STANDARD"),
                Ui.BRAND, Ui.BRAND_SOFT);
        top.addView(season);
        card.addView(top);

        TextView time = Ui.text(this, now.format(DateTimeFormatter.ofPattern("HH:mm")), 32, Ui.INK, true);
        time.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(time);
        TextView date = Ui.text(this, now.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")), 13, Ui.MUTED, false);
        card.addView(date);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, Ui.dp(this, 12), 0, 0);
        TextView offset = Ui.text(this, TimeZones.offsetFromHome(city.zoneId), 12, Ui.BRAND, true);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, -2, 1f);
        offset.setLayoutParams(op);
        bottom.addView(offset);
        TextView seasonText = Ui.text(this, TimeZones.seasonText(zone, now.toInstant()), 11, Ui.MUTED, false);
        seasonText.setGravity(Gravity.END);
        bottom.addView(seasonText);
        card.addView(bottom);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, EditAlarmActivity.class);
            i.putExtra("cityName", city.name);
            i.putExtra("zoneId", city.zoneId);
            startActivity(i);
        });
        return card;
    }

    private void buildAlarmsTab() {
        contentBox.addView(readinessCard());

        Button add = Ui.primaryButton(this, "+  Create alarm");
        add.setOnClickListener(v -> startActivity(new Intent(this, EditAlarmActivity.class)));
        contentBox.addView(add);

        contentBox.addView(Ui.sectionTitle(this, "SCHEDULED ALARMS"));

        LinearLayout alarmFrame = new LinearLayout(this);
        alarmFrame.setOrientation(LinearLayout.VERTICAL);
        alarmFrame.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        alarmFrame.setBackground(Ui.rounded(Ui.CARD, 18, Ui.LINE, 1, this));
        LinearLayout.LayoutParams afp = new LinearLayout.LayoutParams(-1, 0, 1f);
        afp.setMargins(0, 0, 0, Ui.dp(this, 10));
        alarmFrame.setLayoutParams(afp);

        ScrollView alarmsScroll = new ScrollView(this);
        alarmsScroll.setFillViewport(true);
        alarmsScroll.setVerticalScrollBarEnabled(true);
        alarmsBox = new LinearLayout(this);
        alarmsBox.setOrientation(LinearLayout.VERTICAL);
        alarmsBox.setPadding(0, 0, 0, Ui.dp(this, 2));
        alarmsScroll.addView(alarmsBox, new ScrollView.LayoutParams(-1, -2));
        alarmFrame.addView(alarmsScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        contentBox.addView(alarmFrame);
        renderAlarms();

        LinearLayout about = Ui.card(this);
        about.addView(Ui.text(this, "App info", 18, Ui.INK, true));
        about.addView(Ui.text(this, "SeasonAlarm v3.1.0\nDeveloper: 𖤢ꚶꚠ𖣠𖦪\n© " + java.time.Year.now().getValue() + " 𖤢ꚶꚠ𖣠𖦪", 13, Ui.MUTED, false));
        Button repo = Ui.secondaryButton(this, "Check other versions");
        repo.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eujor/SeasonAlarm/releases"))));
        about.addView(repo);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.setMargins(0, 0, 0, 0);
        about.setLayoutParams(ap);
        contentBox.addView(about);
    }

    private View readinessCard() {
        LinearLayout card = Ui.card(this);
        AlarmScheduler scheduler = new AlarmScheduler(this);
        boolean notifications = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean exact = scheduler.canScheduleExact();
        boolean fullScreen = true;
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            fullScreen = nm != null && nm.canUseFullScreenIntent();
        }
        boolean ready = notifications && exact && fullScreen;

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, ready ? "Alarm system ready" : "Alarm access needs attention", 17, Ui.INK, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1f);
        t.setLayoutParams(tlp);
        heading.addView(t);
        heading.addView(Ui.badge(this, ready ? "READY" : "CHECK", ready ? Ui.SUCCESS : Ui.DANGER,
                ready ? 0xffe5f5ec : 0xffffe9e9));
        card.addView(heading);

        TextView detail = Ui.text(this,
                "Exact timing • notifications • full-screen alarm • tone • vibration",
                12, Ui.MUTED, false);
        detail.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 10));
        card.addView(detail);

        if (!notifications) {
            Button b = Ui.secondaryButton(this, "Allow notifications");
            b.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS));
            card.addView(b);
        }
        if (!exact) {
            Button b = Ui.secondaryButton(this, "Allow exact alarms");
            b.setOnClickListener(v -> {
                Intent i = scheduler.exactAlarmSettingsIntent();
                if (i != null) startActivity(i);
            });
            card.addView(b);
        }
        if (!fullScreen && Build.VERSION.SDK_INT >= 34) {
            Button b = Ui.secondaryButton(this, "Allow full-screen alarms");
            b.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + getPackageName()))));
            card.addView(b);
        }
        return card;
    }

    private void renderAlarms() {
        if (alarmsBox == null) return;
        alarmsBox.removeAllViews();
        List<Alarm> alarms = new AlarmRepository(this).all();
        if (alarms.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            TextView icon = Ui.text(this, "⏰", 38, Ui.BRAND, false);
            icon.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.addView(icon);
            TextView title = Ui.text(this, "No alarms yet", 18, Ui.INK, true);
            title.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.addView(title);
            TextView sub = Ui.text(this, "Create one using a local city time or your device time.", 13, Ui.MUTED, false);
            sub.setGravity(Gravity.CENTER_HORIZONTAL);
            sub.setPadding(0, Ui.dp(this, 5), 0, 0);
            empty.addView(sub);
            alarmsBox.addView(empty);
            return;
        }

        for (Alarm a : alarms) alarmsBox.addView(alarmCard(a));
    }

    private View alarmCard(Alarm a) {
        LinearLayout card = Ui.card(this);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        titleBox.addView(Ui.text(this, a.label, 18, Ui.INK, true));
        TextView city = Ui.text(this, a.cityName, 12, Ui.MUTED, false);
        city.setPadding(0, Ui.dp(this, 2), 0, 0);
        titleBox.addView(city);
        top.addView(titleBox);

        Switch enabled = new Switch(this);
        enabled.setChecked(a.enabled);
        enabled.setContentDescription("Enable " + a.label);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            a.enabled = checked;
            new AlarmRepository(this).save(a);
            AlarmScheduler s = new AlarmScheduler(this);
            if (checked) s.scheduleNext(a); else { s.cancel(a.id); s.refreshUpcomingNotification(); }
            renderAlarms();
        });
        top.addView(enabled);
        card.addView(top);

        TextView time = Ui.text(this, String.format("%02d:%02d", a.hour, a.minute), 34, a.enabled ? Ui.INK : 0xff91a1a5, true);
        time.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(time);
        TextView local = Ui.text(this, "Target city local time", 11, Ui.MUTED, true);
        card.addView(local);

        TextView next = Ui.text(this, "Next  •  " + AlarmScheduler.nextSummary(a), 12, Ui.BRAND, true);
        next.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        card.addView(next);
        card.addView(Ui.divider(this));

        String tone = AlarmRingingService.SILENT_TONE.equals(a.toneUri) ? "Silent" : (a.toneUri == null || a.toneUri.isEmpty() ? "Default tone" : "Custom tone");
        TextView info = Ui.text(this, repeatLabel(a) + "   •   " + tone + "   •   " + (a.vibrate ? "Vibrate" : "No vibration") + "   •   Snooze " + a.snoozeMinutes + "m",
                12, Ui.MUTED, false);
        info.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        card.addView(info);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button edit = Ui.secondaryButton(this, "Edit");
        edit.setLayoutParams(new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        edit.setOnClickListener(v -> startActivity(new Intent(this, EditAlarmActivity.class).putExtra("id", a.id)));
        actions.addView(edit);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 8), 1));
        actions.addView(spacer);

        Button del = Ui.secondaryButton(this, "Delete");
        del.setTextColor(Ui.DANGER);
        del.setLayoutParams(new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete alarm?")
                .setMessage(a.label + " will be removed.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    new AlarmScheduler(this).cancel(a.id);
                    new AlarmRepository(this).delete(a.id);
                    new AlarmScheduler(this).refreshUpcomingNotification();
                    renderAlarms();
                }).show());
        actions.addView(del);
        card.addView(actions);
        return card;
    }

    private String repeatLabel(Alarm a) {
        return switch (a.repeatMode) {
            case "DAILY" -> "Every day";
            case "WEEKDAYS" -> "Weekdays";
            case "SELECTED_DAYS" -> "Selected weekdays";
            case "DATES" -> "Calendar dates";
            case "ONCE" -> "One time";
            default -> a.repeatMode;
        };
    }

    private void updateHeaderClock() {
        if (headerClock == null) return;
        ZonedDateTime now = ZonedDateTime.now(TimeZones.resolveZone(TimeZones.HOME_ZONE));
        headerClock.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss  •  EEE, dd MMM")));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }
}
