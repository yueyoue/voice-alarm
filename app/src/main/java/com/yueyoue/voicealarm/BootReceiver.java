package com.yueyoue.voicealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/**
 * 开机 / 应用更新 / 时间变更后重新排定所有已开启的闹钟。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        List<Alarm> alarms = AlarmStore.get(context).all();
        for (Alarm a : alarms) {
            if (a.enabled) {
                AlarmScheduler.schedule(context, a);
            }
        }
    }
}