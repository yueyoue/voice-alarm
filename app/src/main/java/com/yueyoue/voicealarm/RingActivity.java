package com.yueyoue.voicealarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 闹钟全屏响铃界面：大字显示提醒内容，必须向右滑动才能解除。
 */
public class RingActivity extends Activity {

    private SlideToDismissView slide;
    private int alarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.activity_ring);

        alarmId = getIntent().getIntExtra(AlarmService.EXTRA_ALARM_ID, -1);
        Alarm alarm = AlarmStore.get(this).find(alarmId);

        TextView tvTime = findViewById(R.id.tvRingTime);
        TextView tvText = findViewById(R.id.tvRingText);
        TextView tvVoice = findViewById(R.id.tvRingVoice);

        String text = alarm == null || alarm.text == null ? "" : alarm.text.trim();
        if (alarm != null) {
            tvTime.setText(alarm.timeText());
        } else {
            tvTime.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
        }

        if (text.isEmpty()) {
            tvText.setText("闹钟时间到了");
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f);
        } else {
            tvText.setText(text);
            float scale = alarm == null ? 1.0f : alarm.fontScale;
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f * scale);
        }

        boolean voiceOn = alarm != null && alarm.voice && AlarmStore.get(this).isVoiceEnabled();
        tvVoice.setVisibility(voiceOn && !text.isEmpty() ? View.VISIBLE : View.GONE);

        slide = findViewById(R.id.slide);
        slide.setOnDismissListener(new SlideToDismissView.OnDismissListener() {
            @Override
            public void onDismissed() {
                dismissAlarm();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // 必须滑动解除，返回键不关闭闹铃
    }

    private void dismissAlarm() {
        Intent stop = new Intent(this, AlarmService.class);
        stop.setAction(AlarmService.ACTION_STOP);
        try {
            startService(stop);
        } catch (Exception ignored) {
            // ignore
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(AlarmService.NOTIFICATION_ID);
        }
        finish();
    }
}