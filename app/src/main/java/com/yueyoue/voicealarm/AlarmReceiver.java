package com.yueyoue.voicealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 闹钟到点广播：启动响铃服务并使一次闹钟自动关闭、重复闹钟自动排下一次。
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        int id = intent.getIntExtra(AlarmService.EXTRA_ALARM_ID, -1);
        AlarmStore store = AlarmStore.get(context);
        Alarm alarm = store.find(id);
        if (alarm == null) {
            return;
        }

        if (alarm.repeats()) {
            AlarmScheduler.schedule(context, alarm);
        } else {
            alarm.enabled = false;
            store.upsert(alarm);
        }

        Intent service = new Intent(context, AlarmService.class);
        service.setAction(AlarmService.ACTION_RING);
        service.putExtra(AlarmService.EXTRA_ALARM_ID, id);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 尽力直接拉起全屏界面（部分系统会拦截，通知中的全屏 Intent 作为兜底）
        try {
            Intent ring = new Intent(context, RingActivity.class);
            ring.putExtra(AlarmService.EXTRA_ALARM_ID, id);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(ring);
        } catch (Exception ignored) {
            // 忽略：依赖通知全屏 Intent
        }
    }
}