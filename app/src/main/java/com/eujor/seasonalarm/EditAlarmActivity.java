package com.eujor.seasonalarm;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EditAlarmActivity extends Activity {
    private static final int PICK_TONE = 77;
    private Alarm alarm;
    private List<City> cities;
    private EditText label;
    private Spinner citySpinner, repeatSpinner;
    private Button timeBtn, toneBtn, onceDateBtn;
    private Switch vibrate;
    private final CheckBox[] dayChecks = new CheckBox[7];
    private EditText datesText, snoozeText;
    private TextView preview;
    private LinearLayout daysBox, datesBox, onceBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        long id = getIntent().getLongExtra("id", -1);
        alarm = id == -1 ? new Alarm() : new AlarmRepository(this).find(id);
        if (alarm == null) alarm = new Alarm();

        if (id == -1) {
            String cityName = getIntent().getStringExtra("cityName");
            String zoneId = getIntent().getStringExtra("zoneId");
            if (cityName != null && zoneId != null) {
                alarm.cityName = cityName;
                alarm.zoneId = zoneId;
            }
        }
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 30));
        root.setBackgroundColor(Ui.BG);
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 16));
        hero.setBackground(Ui.rounded(Ui.BRAND_DARK, 22, Ui.BRAND_DARK, 0, this));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, 0, 0, Ui.dp(this, 14));
        hero.setLayoutParams(hp);
        hero.addView(Ui.text(this, getIntent().hasExtra("id") ? "Edit alarm" : "Create alarm", 25, Color.WHITE, true));
        TextView hs = Ui.text(this, "Set the time where the people are. SeasonAlarm handles Nairobi conversion.", 12, 0xffb9d5d1, false);
        hs.setPadding(0, Ui.dp(this, 4), 0, 0);
        hero.addView(hs);
        root.addView(hero);

        root.addView(Ui.sectionTitle(this, "ALARM"));
        LinearLayout alarmCard = Ui.card(this);
        alarmCard.addView(fieldLabel("Label"));
        label = new EditText(this);
        label.setSingleLine(true);
        label.setImeOptions(EditorInfo.IME_ACTION_DONE);
        label.setHint("e.g. London morning call");
        label.setText(alarm.label);
        alarmCard.addView(label);

        alarmCard.addView(fieldLabel("Target city / timezone"));
        cities = TimeZones.cities();
        citySpinner = new Spinner(this);
        citySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities));
        int cityIndex = 0;
        for (int i = 0; i < cities.size(); i++) {
            if (cities.get(i).zoneId.equals(alarm.zoneId) && cities.get(i).name.equals(alarm.cityName)) {
                cityIndex = i;
                break;
            }
        }
        citySpinner.setSelection(cityIndex);
        citySpinner.setOnItemSelectedListener(new SimpleItemSelected(this::updatePreview));
        alarmCard.addView(citySpinner);

        alarmCard.addView(fieldLabel("Time in selected city"));
        timeBtn = Ui.primaryButton(this, "");
        timeBtn.setTextSize(24);
        updateTimeButton();
        timeBtn.setOnClickListener(v -> new TimePickerDialog(this, (view, h, m) -> {
            alarm.hour = h;
            alarm.minute = m;
            updateTimeButton();
            updatePreview();
        }, alarm.hour, alarm.minute, true).show());
        alarmCard.addView(timeBtn);
        root.addView(alarmCard);

        preview = Ui.text(this, "", 14, Ui.INK, false);
        preview.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        preview.setBackground(Ui.rounded(Ui.BRAND_SOFT, 18, 0xffc9e6e1, 1, this));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.setMargins(0, 0, 0, Ui.dp(this, 14));
        preview.setLayoutParams(pp);
        root.addView(preview);

        root.addView(Ui.sectionTitle(this, "REPEAT"));
        LinearLayout repeatCard = Ui.card(this);
        repeatSpinner = new Spinner(this);
        String[] modes = {"Weekdays (Mon–Fri)", "Every day", "Selected weekdays", "Specific calendar dates", "One time"};
        repeatSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        repeatSpinner.setSelection(modeIndex(alarm.repeatMode));
        repeatSpinner.setOnItemSelectedListener(new SimpleItemSelected(this::refreshRepeatVisibility));
        repeatCard.addView(repeatSpinner);

        daysBox = new LinearLayout(this);
        daysBox.setOrientation(LinearLayout.VERTICAL);
        daysBox.setPadding(0, Ui.dp(this, 8), 0, 0);
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(dayNames[i]);
            cb.setChecked(alarm.weekdays.contains(i + 1));
            dayChecks[i] = cb;
            daysBox.addView(cb);
        }
        repeatCard.addView(daysBox);

        datesBox = new LinearLayout(this);
        datesBox.setOrientation(LinearLayout.VERTICAL);
        datesBox.setPadding(0, Ui.dp(this, 8), 0, 0);
        datesBox.addView(fieldLabel("Dates in target city's calendar"));
        datesText = new EditText(this);
        datesText.setHint("2026-11-03, 2026-11-10");
        datesText.setText(String.join(",", alarm.dates));
        datesBox.addView(datesText);
        repeatCard.addView(datesBox);

        onceBox = new LinearLayout(this);
        onceBox.setOrientation(LinearLayout.VERTICAL);
        onceBox.setPadding(0, Ui.dp(this, 8), 0, 0);
        onceDateBtn = Ui.secondaryButton(this, alarm.onceDate.isEmpty() ? "Choose date" : alarm.onceDate);
        onceDateBtn.setOnClickListener(v -> chooseDate());
        onceBox.addView(onceDateBtn);
        repeatCard.addView(onceBox);
        root.addView(repeatCard);

        root.addView(Ui.sectionTitle(this, "SOUND & HAPTICS"));
        LinearLayout soundCard = Ui.card(this);
        LinearLayout vibRow = new LinearLayout(this);
        vibRow.setOrientation(LinearLayout.HORIZONTAL);
        vibRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout vibText = new LinearLayout(this);
        vibText.setOrientation(LinearLayout.VERTICAL);
        vibText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        vibText.addView(Ui.text(this, "Vibration", 15, Ui.INK, true));
        vibText.addView(Ui.text(this, "Vibrate while the alarm is ringing", 12, Ui.MUTED, false));
        vibRow.addView(vibText);
        vibrate = new Switch(this);
        vibrate.setChecked(alarm.vibrate);
        vibRow.addView(vibrate);
        soundCard.addView(vibRow);
        soundCard.addView(Ui.divider(this));

        TextView toneLabel = fieldLabel("Alarm tone");
        toneLabel.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 5));
        soundCard.addView(toneLabel);
        toneBtn = Ui.secondaryButton(this, toneButtonText());
        toneBtn.setOnClickListener(v -> pickTone());
        soundCard.addView(toneBtn);

        soundCard.addView(fieldLabel("Snooze minutes"));
        snoozeText = new EditText(this);
        snoozeText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        snoozeText.setText(String.valueOf(alarm.snoozeMinutes));
        soundCard.addView(snoozeText);
        root.addView(soundCard);

        Button save = Ui.primaryButton(this, "Save alarm");
        save.setOnClickListener(v -> save());
        root.addView(save);
        Button cancel = Ui.secondaryButton(this, "Cancel");
        cancel.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 48));
        cp.setMargins(0, Ui.dp(this, 2), 0, 0);
        cancel.setLayoutParams(cp);
        root.addView(cancel);

        refreshRepeatVisibility();
        updatePreview();
        setContentView(scroll);
    }

    private void save() {
        City c = cities.get(citySpinner.getSelectedItemPosition());
        alarm.cityName = c.name;
        alarm.zoneId = c.zoneId;
        alarm.label = label.getText().toString().trim().isEmpty() ? "Alarm" : label.getText().toString().trim();
        alarm.vibrate = vibrate.isChecked();
        try {
            alarm.snoozeMinutes = Math.max(1, Integer.parseInt(snoozeText.getText().toString().trim()));
        } catch (Exception e) {
            alarm.snoozeMinutes = 5;
        }
        alarm.repeatMode = modeKey(repeatSpinner.getSelectedItemPosition());
        alarm.weekdays.clear();
        for (int i = 0; i < 7; i++) if (dayChecks[i].isChecked()) alarm.weekdays.add(i + 1);
        alarm.dates.clear();
        for (String x : datesText.getText().toString().split(",")) {
            x = x.trim();
            if (!x.isEmpty()) {
                try { LocalDate.parse(x); alarm.dates.add(x); }
                catch (Exception ignored) {}
            }
        }
        if ("SELECTED_DAYS".equals(alarm.repeatMode) && alarm.weekdays.isEmpty()) {
            Toast.makeText(this, "Choose at least one weekday", Toast.LENGTH_LONG).show();
            return;
        }
        if ("DATES".equals(alarm.repeatMode) && alarm.dates.isEmpty()) {
            Toast.makeText(this, "Enter at least one valid date", Toast.LENGTH_LONG).show();
            return;
        }
        if ("ONCE".equals(alarm.repeatMode) && alarm.onceDate.isEmpty()) {
            Toast.makeText(this, "Choose a date", Toast.LENGTH_LONG).show();
            return;
        }

        alarm.enabled = true;
        new AlarmRepository(this).save(alarm);
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (!scheduler.canScheduleExact()) {
            Intent i = scheduler.exactAlarmSettingsIntent();
            if (i != null) startActivity(i);
            Toast.makeText(this, "Saved. Allow exact alarms, then return to the app.", Toast.LENGTH_LONG).show();
        } else {
            scheduler.scheduleNext(alarm);
        }
        finish();
    }

    private void updatePreview() {
        if (preview == null || cities == null || citySpinner == null) return;
        City c = cities.get(citySpinner.getSelectedItemPosition());
        ZoneId z = TimeZones.resolveZone(c.zoneId);
        ZonedDateTime nowThere = ZonedDateTime.now(z);
        ZonedDateTime sample = LocalDateTime.of(nowThere.toLocalDate(), LocalTime.of(alarm.hour, alarm.minute)).atZone(z);
        ZonedDateTime eat = sample.withZoneSameInstant(ZoneId.of(TimeZones.HOME_ZONE));
        String line = "NOW THERE  " + nowThere.format(DateTimeFormatter.ofPattern("HH:mm:ss z")) +
                "\n" + TimeZones.seasonText(z, Instant.now()) +
                "\n\nYOUR ALARM  " + String.format("%02d:%02d", alarm.hour, alarm.minute) + " in " + c.name +
                "\nNAIROBI  " + eat.format(DateTimeFormatter.ofPattern("HH:mm")) + " EAT on that target-city date";
        preview.setText(line);
    }

    private String toneButtonText() {
        if (AlarmRingingService.SILENT_TONE.equals(alarm.toneUri)) return "Silent / no tone";
        if (alarm.toneUri == null || alarm.toneUri.isEmpty()) return "Default alarm tone";
        try {
            Ringtone r = RingtoneManager.getRingtone(this, Uri.parse(alarm.toneUri));
            String title = r == null ? null : r.getTitle(this);
            return title == null || title.isEmpty() ? "Custom alarm tone" : title;
        } catch (Exception e) {
            return "Custom alarm tone";
        }
    }

    private void updateTimeButton() {
        if (timeBtn != null) timeBtn.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
    }

    private TextView fieldLabel(String s) {
        TextView v = Ui.text(this, s, 12, Ui.MUTED, true);
        v.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        return v;
    }

    private int modeIndex(String k) {
        return switch (k) {
            case "DAILY" -> 1;
            case "SELECTED_DAYS" -> 2;
            case "DATES" -> 3;
            case "ONCE" -> 4;
            default -> 0;
        };
    }

    private String modeKey(int i) {
        return switch (i) {
            case 1 -> "DAILY";
            case 2 -> "SELECTED_DAYS";
            case 3 -> "DATES";
            case 4 -> "ONCE";
            default -> "WEEKDAYS";
        };
    }

    private void refreshRepeatVisibility() {
        if (daysBox == null || repeatSpinner == null) return;
        int i = repeatSpinner.getSelectedItemPosition();
        daysBox.setVisibility(i == 2 ? View.VISIBLE : View.GONE);
        datesBox.setVisibility(i == 3 ? View.VISIBLE : View.GONE);
        onceBox.setVisibility(i == 4 ? View.VISIBLE : View.GONE);
    }

    private void chooseDate() {
        LocalDate d;
        try { d = alarm.onceDate.isEmpty() ? LocalDate.now() : LocalDate.parse(alarm.onceDate); }
        catch (Exception e) { d = LocalDate.now(); }
        new DatePickerDialog(this, (v, y, m, day) -> {
            alarm.onceDate = String.format("%04d-%02d-%02d", y, m + 1, day);
            onceDateBtn.setText(alarm.onceDate);
        }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
    }

    private void pickTone() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        Uri existing = (alarm.toneUri == null || alarm.toneUri.isEmpty() || AlarmRingingService.SILENT_TONE.equals(alarm.toneUri))
                ? Settings.System.DEFAULT_ALARM_ALERT_URI : Uri.parse(alarm.toneUri);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        startActivityForResult(i, PICK_TONE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TONE && resultCode == RESULT_OK && data != null) {
            Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            alarm.toneUri = uri == null ? AlarmRingingService.SILENT_TONE : uri.toString();
            toneBtn.setText(toneButtonText());
        }
    }

    private static class SimpleItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable r;
        SimpleItemSelected(Runnable r) { this.r = r; }
        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { r.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
    }
}
