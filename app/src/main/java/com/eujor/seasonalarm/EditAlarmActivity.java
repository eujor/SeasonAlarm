package com.eujor.seasonalarm;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
import java.util.ArrayList;
import java.util.List;

public class EditAlarmActivity extends Activity {
    private static final int PICK_TONE = 77;
    private Alarm alarm;
    private List<City> cities;
    private EditText label;
    private Spinner citySpinner, repeatSpinner;
    private Button timeBtn, toneBtn, onceDateBtn;
    private Switch vibrate;
    private CheckBox[] dayChecks = new CheckBox[7];
    private EditText datesText, snoozeText;
    private TextView preview;
    private LinearLayout daysBox, datesBox, onceBox;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        long id = getIntent().getLongExtra("id", -1);
        alarm = id == -1 ? new Alarm() : new AlarmRepository(this).find(id);
        if (alarm == null) alarm = new Alarm();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,40);
        scroll.addView(root);

        TextView heading = new TextView(this); heading.setText("ALARM EDITOR"); heading.setTextSize(24); root.addView(heading);

        root.addView(labelText("Label"));
        label = new EditText(this); label.setText(alarm.label); root.addView(label);

        root.addView(labelText("Target city / timezone"));
        cities = TimeZones.cities();
        citySpinner = new Spinner(this);
        citySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cities));
        int cityIndex = 0;
        for (int i=0;i<cities.size();i++) if (cities.get(i).zoneId.equals(alarm.zoneId) && cities.get(i).name.equals(alarm.cityName)) { cityIndex=i; break; }
        citySpinner.setSelection(cityIndex);
        citySpinner.setOnItemSelectedListener(new SimpleItemSelected(() -> updatePreview()));
        root.addView(citySpinner);

        root.addView(labelText("Time in selected city"));
        timeBtn = new Button(this); updateTimeButton();
        timeBtn.setOnClickListener(v -> new TimePickerDialog(this, (view,h,m)->{ alarm.hour=h; alarm.minute=m; updateTimeButton(); updatePreview(); }, alarm.hour, alarm.minute, true).show());
        root.addView(timeBtn);

        root.addView(labelText("Repeat"));
        repeatSpinner = new Spinner(this);
        String[] modes = {"Weekdays (Mon–Fri)", "Every day", "Selected weekdays", "Specific calendar dates", "One time"};
        repeatSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        repeatSpinner.setSelection(modeIndex(alarm.repeatMode));
        repeatSpinner.setOnItemSelectedListener(new SimpleItemSelected(this::refreshRepeatVisibility));
        root.addView(repeatSpinner);

        daysBox = new LinearLayout(this); daysBox.setOrientation(LinearLayout.VERTICAL);
        String[] dayNames = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
        for (int i=0;i<7;i++) {
            CheckBox cb = new CheckBox(this); cb.setText(dayNames[i]); cb.setChecked(alarm.weekdays.contains(i+1)); dayChecks[i]=cb; daysBox.addView(cb);
        }
        root.addView(daysBox);

        datesBox = new LinearLayout(this); datesBox.setOrientation(LinearLayout.VERTICAL);
        datesBox.addView(labelText("Dates in target city's calendar (comma separated YYYY-MM-DD)"));
        datesText = new EditText(this); datesText.setText(String.join(",", alarm.dates)); datesBox.addView(datesText);
        root.addView(datesBox);

        onceBox = new LinearLayout(this); onceBox.setOrientation(LinearLayout.VERTICAL);
        onceDateBtn = new Button(this); onceDateBtn.setText(alarm.onceDate.isEmpty() ? "CHOOSE DATE" : alarm.onceDate);
        onceDateBtn.setOnClickListener(v -> chooseDate()); onceBox.addView(onceDateBtn); root.addView(onceBox);

        vibrate = new Switch(this); vibrate.setText("Vibration"); vibrate.setChecked(alarm.vibrate); root.addView(vibrate);

        toneBtn = new Button(this); toneBtn.setText("SELECT ALARM TONE");
        toneBtn.setOnClickListener(v -> pickTone()); root.addView(toneBtn);

        root.addView(labelText("Snooze minutes"));
        snoozeText = new EditText(this); snoozeText.setInputType(2); snoozeText.setText(String.valueOf(alarm.snoozeMinutes)); root.addView(snoozeText);

        preview = new TextView(this); preview.setTextSize(16); preview.setPadding(0,18,0,18); root.addView(preview);

        Button save = new Button(this); save.setText("SAVE ALARM"); save.setOnClickListener(v -> save()); root.addView(save);
        Button cancel = new Button(this); cancel.setText("CANCEL"); cancel.setOnClickListener(v -> finish()); root.addView(cancel);

        refreshRepeatVisibility(); updatePreview(); setContentView(scroll);
    }

    private void save() {
        City c = cities.get(citySpinner.getSelectedItemPosition());
        alarm.cityName = c.name; alarm.zoneId = c.zoneId;
        alarm.label = label.getText().toString().trim().isEmpty() ? "Alarm" : label.getText().toString().trim();
        alarm.vibrate = vibrate.isChecked();
        try { alarm.snoozeMinutes = Math.max(1, Integer.parseInt(snoozeText.getText().toString().trim())); } catch (Exception e) { alarm.snoozeMinutes = 5; }
        alarm.repeatMode = modeKey(repeatSpinner.getSelectedItemPosition());
        alarm.weekdays.clear(); for (int i=0;i<7;i++) if (dayChecks[i].isChecked()) alarm.weekdays.add(i+1);
        alarm.dates.clear();
        for (String x : datesText.getText().toString().split(",")) {
            x=x.trim(); if (!x.isEmpty()) { try { LocalDate.parse(x); alarm.dates.add(x); } catch (Exception ignored) {} }
        }
        if ("SELECTED_DAYS".equals(alarm.repeatMode) && alarm.weekdays.isEmpty()) { Toast.makeText(this,"Choose at least one weekday",Toast.LENGTH_LONG).show(); return; }
        if ("DATES".equals(alarm.repeatMode) && alarm.dates.isEmpty()) { Toast.makeText(this,"Enter at least one valid date",Toast.LENGTH_LONG).show(); return; }
        if ("ONCE".equals(alarm.repeatMode) && alarm.onceDate.isEmpty()) { Toast.makeText(this,"Choose a date",Toast.LENGTH_LONG).show(); return; }

        alarm.enabled = true;
        new AlarmRepository(this).save(alarm);
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (!scheduler.canScheduleExact()) {
            Intent i = scheduler.exactAlarmSettingsIntent(); if (i != null) startActivity(i);
            Toast.makeText(this,"Saved. Allow exact alarms, then return to the app.",Toast.LENGTH_LONG).show();
        } else scheduler.scheduleNext(alarm);
        finish();
    }

    private void updatePreview() {
        if (preview == null || cities == null) return;
        City c = cities.get(citySpinner.getSelectedItemPosition());
        ZoneId z = TimeZones.resolveZone(c.zoneId);
        ZonedDateTime nowThere = ZonedDateTime.now(z);
        ZonedDateTime sample = LocalDateTime.of(nowThere.toLocalDate(), LocalTime.of(alarm.hour, alarm.minute)).atZone(z);
        ZonedDateTime eat = sample.withZoneSameInstant(ZoneId.of(TimeZones.HOME_ZONE));
        String line = "Now in " + c.name + ": " + nowThere.format(DateTimeFormatter.ofPattern("HH:mm:ss z")) +
                "\n" + TimeZones.seasonText(z, Instant.now()) +
                "\nSelected " + String.format("%02d:%02d", alarm.hour, alarm.minute) + " there = " + eat.format(DateTimeFormatter.ofPattern("HH:mm")) + " EAT on that date.";
        preview.setText(line);
    }

    private void updateTimeButton() { if (timeBtn != null) timeBtn.setText(String.format("%02d:%02d", alarm.hour, alarm.minute)); }
    private TextView labelText(String s) { TextView v = new TextView(this); v.setText(s); v.setTextSize(14); v.setPadding(0,12,0,4); return v; }
    private int modeIndex(String k) { return switch(k){ case "DAILY"->1; case "SELECTED_DAYS"->2; case "DATES"->3; case "ONCE"->4; default->0; }; }
    private String modeKey(int i) { return switch(i){ case 1->"DAILY"; case 2->"SELECTED_DAYS"; case 3->"DATES"; case 4->"ONCE"; default->"WEEKDAYS"; }; }
    private void refreshRepeatVisibility() { if (daysBox==null) return; int i=repeatSpinner.getSelectedItemPosition(); daysBox.setVisibility(i==2?View.VISIBLE:View.GONE); datesBox.setVisibility(i==3?View.VISIBLE:View.GONE); onceBox.setVisibility(i==4?View.VISIBLE:View.GONE); }

    private void chooseDate() {
        LocalDate d; try { d = alarm.onceDate.isEmpty()?LocalDate.now():LocalDate.parse(alarm.onceDate); } catch(Exception e){ d=LocalDate.now(); }
        new DatePickerDialog(this,(v,y,m,day)->{ alarm.onceDate=String.format("%04d-%02d-%02d",y,m+1,day); onceDateBtn.setText(alarm.onceDate); },d.getYear(),d.getMonthValue()-1,d.getDayOfMonth()).show();
    }

    private void pickTone() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        Uri existing = alarm.toneUri.isEmpty()? Settings.System.DEFAULT_ALARM_ALERT_URI:Uri.parse(alarm.toneUri);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        startActivityForResult(i, PICK_TONE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode==PICK_TONE && resultCode==RESULT_OK && data!=null) {
            Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            alarm.toneUri = uri==null?"":uri.toString();
            toneBtn.setText(uri==null?"SILENT / NO TONE":"TONE SELECTED");
        }
    }

    private static class SimpleItemSelected implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable r; SimpleItemSelected(Runnable r){this.r=r;}
        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id){r.run();}
        @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
    }
}
