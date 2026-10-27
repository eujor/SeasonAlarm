package com.eujor.seasonalarm;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Alarm {
    public long id = System.currentTimeMillis();
    public String label = "Alarm";
    public String cityName = "Device local time";
    public String zoneId = TimeZones.HOME_ZONE;
    public int hour = 8;
    public int minute = 30;
    public String repeatMode = "WEEKDAYS";
    public List<Integer> weekdays = new ArrayList<>(); // 1=Mon ... 7=Sun
    public List<String> dates = new ArrayList<>(); // YYYY-MM-DD in target city
    public String onceDate = "";
    public boolean enabled = true;
    public boolean vibrate = true;
    public String toneUri = "";
    public int snoozeMinutes = 5;
    public int autoStopMinutes = 15;

    public Alarm() {
        weekdays.add(1); weekdays.add(2); weekdays.add(3); weekdays.add(4); weekdays.add(5);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id); o.put("label", label); o.put("cityName", cityName); o.put("zoneId", zoneId);
            o.put("hour", hour); o.put("minute", minute); o.put("repeatMode", repeatMode);
            o.put("enabled", enabled); o.put("vibrate", vibrate); o.put("toneUri", toneUri); o.put("snoozeMinutes", snoozeMinutes); o.put("autoStopMinutes", autoStopMinutes);
            o.put("onceDate", onceDate);
            JSONArray w = new JSONArray(); for (Integer d : weekdays) w.put(d); o.put("weekdays", w);
            JSONArray ds = new JSONArray(); for (String d : dates) ds.put(d); o.put("dates", ds);
        } catch (Exception ignored) {}
        return o;
    }

    public static Alarm fromJson(JSONObject o) {
        Alarm a = new Alarm();
        try {
            a.id = o.getLong("id"); a.label = o.optString("label", "Alarm");
            a.cityName = o.optString("cityName", "Device local time"); a.zoneId = o.optString("zoneId", TimeZones.HOME_ZONE);
            a.hour = o.optInt("hour", 8); a.minute = o.optInt("minute", 30); a.repeatMode = o.optString("repeatMode", "WEEKDAYS");
            a.enabled = o.optBoolean("enabled", true); a.vibrate = o.optBoolean("vibrate", true);
            a.toneUri = o.optString("toneUri", ""); a.snoozeMinutes = o.optInt("snoozeMinutes", 5); a.autoStopMinutes = o.optInt("autoStopMinutes", 15);
            a.onceDate = o.optString("onceDate", "");
            a.weekdays.clear(); JSONArray w = o.optJSONArray("weekdays"); if (w != null) for (int i=0;i<w.length();i++) a.weekdays.add(w.optInt(i));
            a.dates.clear(); JSONArray ds = o.optJSONArray("dates"); if (ds != null) for (int i=0;i<ds.length();i++) a.dates.add(ds.optString(i));
        } catch (Exception ignored) {}
        return a;
    }
}
