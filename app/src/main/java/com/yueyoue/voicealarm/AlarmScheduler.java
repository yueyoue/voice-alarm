package com.yueyoue.voicealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * 使用 AlarmManager.setAlarmClock 精确排定闹钟（系统会将其视为闹钟应用，可在待机时唤醒）。
 */
public class AlarmScheduler {

    public static final String ACTION_ALARM = "com.yueyoue.voicealarm.ACTION_ALARM";

    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms();
        }
        return true;
    }

    private static PendingIntent alarmIntent(Context context, int id) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM);
        intent.putExtra(AlarmService.EXTRA_ALARM_ID, id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, id, intent, flags);
    }

    private static int immutableFlag() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    /** 计算 after 之后的下一次触发时间（毫秒）。 */
    public static long nextTriggerAt(Alarm alarm, long after) {
        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(after);
        for (int offset = 0; offset <= 8; offset++) {
            Calendar candidate = (Calendar) base.clone();
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, alarm.hour);
            candidate.set(Calendar.MINUTE, alarm.minute);
            candidate.set(Calendar.SECOND, 0);
            candidate.set(Calendar.MILLISECOND, 0);
            long t = candidate.getTimeInMillis();
            if (t <= after) {
                continue;
            }
            if (!alarm.repeats()) {
                return t;
            }
            if (alarm.days[dayIndex(candidate)]) {
                return t;
            }
        }
        return after + 24L * 60L * 60L * 1000L;
    }

    /** Calendar.DAY_OF_WEEK -> 0=周一 ... 6=周日 */
    public static int dayIndex(Calendar c) {
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    public static void schedule(Context context, Alarm alarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        long triggerAt = nextTriggerAt(alarm, System.currentTimeMillis() + 1000L);
        PendingIntent pi = alarmIntent(context, alarm.id);
        PendingIntent showPi = PendingIntent.getActivity(context, alarm.id,
                new Intent(context, MainActivity.class), immutableFlag());
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showPi), pi);
        } catch (SecurityException e) {
            e.printStackTrace();
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } catch (Exception ignored) {
                // 无精确闹钟权限时退化为不精确闹钟
                try {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } catch (Exception ignored2) {
                    ignored2.printStackTrace();
                }
            }
        }
    }

    public static void cancel(Context context, int id) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(context, id);
        if (am != null) {
            am.cancel(pi);
        }
        pi.cancel();
    }
}