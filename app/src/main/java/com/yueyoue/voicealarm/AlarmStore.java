package com.yueyoue.voicealarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 闹钟与设置的本地存储（SharedPreferences + JSON）。
 */
public class AlarmStore {

    private static final String PREF_NAME = "voice_alarm_store";
    private static final String KEY_ALARMS = "alarms";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_DEFAULT_FONT = "default_font_scale";

    private static AlarmStore instance;

    private final SharedPreferences sp;
    private final List<Alarm> alarms = new ArrayList<>();

    private AlarmStore(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized AlarmStore get(Context context) {
        if (instance == null) {
            instance = new AlarmStore(context.getApplicationContext());
        }
        return instance;
    }

    private void load() {
        alarms.clear();
        String raw = sp.getString(KEY_ALARMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    alarms.add(Alarm.fromJson(o));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void persist() {
        JSONArray arr = new JSONArray();
        for (Alarm a : alarms) {
            arr.put(a.toJson());
        }
        sp.edit().putString(KEY_ALARMS, arr.toString()).apply();
    }

    /** 返回按时间排序的副本。 */
    public List<Alarm> all() {
        List<Alarm> list = new ArrayList<>(alarms);
        Collections.sort(list, new Comparator<Alarm>() {
            @Override
            public int compare(Alarm a, Alarm b) {
                int x = a.hour * 60 + a.minute;
                int y = b.hour * 60 + b.minute;
                return Integer.compare(x, y);
            }
        });
        return list;
    }

    public Alarm find(int id) {
        for (Alarm a : alarms) {
            if (a.id == id) {
                return a;
            }
        }
        return null;
    }

    public int nextId() {
        int max = 0;
        for (Alarm a : alarms) {
            if (a.id > max) {
                max = a.id;
            }
        }
        return max + 1;
    }

    public void upsert(Alarm alarm) {
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == alarm.id) {
                alarms.set(i, alarm);
                persist();
                return;
            }
        }
        if (alarm.id <= 0) {
            alarm.id = nextId();
        }
        alarms.add(alarm);
        persist();
    }

    public void delete(int id) {
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == id) {
                alarms.remove(i);
                persist();
                return;
            }
        }
    }

    public boolean isVoiceEnabled() {
        return sp.getBoolean(KEY_VOICE_ENABLED, true);
    }

    public void setVoiceEnabled(boolean enabled) {
        sp.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
    }

    public float getDefaultFontScale() {
        return sp.getFloat(KEY_DEFAULT_FONT, 1.0f);
    }

    public void setDefaultFontScale(float scale) {
        sp.edit().putFloat(KEY_DEFAULT_FONT, scale).apply();
    }
}