package com.yueyoue.voicealarm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * 单条闹钟数据。
 * days 索引约定：0=周一 ... 6=周日，全部为 false 表示只响一次。
 */
public class Alarm {

    public int id;
    public int hour = 7;
    public int minute = 0;
    public String text = "";
    public boolean enabled = true;
    public boolean[] days = new boolean[7];
    public float fontScale = 1.0f;
    public boolean voice = true;

    public boolean repeats() {
        for (boolean d : days) {
            if (d) {
                return true;
            }
        }
        return false;
    }

    public String dayNames() {
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                sb.append(names[i]);
            }
        }
        return sb.toString();
    }

    public String daysLabel() {
        if (!repeats()) {
            return "仅一次";
        }
        boolean all = true;
        for (boolean d : days) {
            if (!d) {
                all = false;
                break;
            }
        }
        if (all) {
            return "每天";
        }
        return "每周" + dayNames();
    }

    public String timeText() {
        return String.format(Locale.CHINA, "%02d:%02d", hour, minute);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("text", text == null ? "" : text);
            o.put("enabled", enabled);
            o.put("fontScale", (double) fontScale);
            o.put("voice", voice);
            JSONArray arr = new JSONArray();
            for (int i = 0; i < 7; i++) {
                arr.put(days[i]);
            }
            o.put("days", arr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return o;
    }

    public static Alarm fromJson(JSONObject o) {
        Alarm a = new Alarm();
        a.id = o.optInt("id", 0);
        a.hour = o.optInt("hour", 7);
        a.minute = o.optInt("minute", 0);
        a.text = o.optString("text", "");
        a.enabled = o.optBoolean("enabled", true);
        a.fontScale = (float) o.optDouble("fontScale", 1.0d);
        a.voice = o.optBoolean("voice", true);
        JSONArray arr = o.optJSONArray("days");
        if (arr != null) {
            for (int i = 0; i < 7 && i < arr.length(); i++) {
                a.days[i] = arr.optBoolean(i, false);
            }
        }
        return a;
    }
}