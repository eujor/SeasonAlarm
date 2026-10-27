package com.eujor.seasonalarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class AlarmRepository {
    private static final String PREF = "season_alarm_store";
    private static final String KEY = "alarms";
    private final SharedPreferences prefs;

    public AlarmRepository(Context c) { prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    public List<Alarm> all() {
        ArrayList<Alarm> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i=0;i<arr.length();i++) out.add(Alarm.fromJson(arr.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public Alarm find(long id) {
        for (Alarm a : all()) if (a.id == id) return a;
        return null;
    }

    public void save(Alarm alarm) {
        List<Alarm> list = all();
        boolean found = false;
        for (int i=0;i<list.size();i++) if (list.get(i).id == alarm.id) { list.set(i, alarm); found=true; break; }
        if (!found) list.add(alarm);
        write(list);
    }

    public void delete(long id) {
        List<Alarm> list = all();
        list.removeIf(a -> a.id == id);
        write(list);
    }

    private void write(List<Alarm> list) {
        JSONArray arr = new JSONArray();
        for (Alarm a : list) arr.put(a.toJson());
        prefs.edit().putString(KEY, arr.toString()).apply();
    }
}
