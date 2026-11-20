package com.yueyoue.voicealarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 闹钟列表：多条闹钟的展示、启停、进入编辑。
 */
public class MainActivity extends Activity {

    private static final int REQ_NOTIFICATION = 10;

    private LinearLayout container;
    private TextView tvNext;
    private TextView tvPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        tvNext = findViewById(R.id.tvNext);
        tvPermission = findViewById(R.id.tvPermission);

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditAlarmActivity.class));
            }
        });

        tvPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPermissionSettings();
            }
        });

        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionBanner();
    }

    private void openPermissionSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(this)) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent i = new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
                return;
            }
        } catch (Exception ignored) {
            // 落到应用详情页
        }
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "请到系统设置中手动开启权限", Toast.LENGTH_LONG).show();
        }
    }

    private void render() {
        AlarmStore store = AlarmStore.get(this);
        final List<Alarm> alarms = store.all();
        container.removeAllViews();

        if (alarms.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("还没有闹钟\n点下面的「+ 添加闹钟」添加一条提醒");
            empty.setTextColor(getColor(R.color.textDim));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(0, 60, 0, 60);
            container.addView(empty);
            updateNextSummary();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (final Alarm alarm : alarms) {
            View row = inflater.inflate(R.layout.item_alarm, container, false);
            final TextView tvTime = row.findViewById(R.id.tvTime);
            final TextView tvText = row.findViewById(R.id.tvText);
            final TextView tvDays = row.findViewById(R.id.tvDays);
            Switch sw = row.findViewById(R.id.swEnabled);

            tvTime.setText(alarm.timeText());
            tvTime.setTextColor(getColor(alarm.enabled ? R.color.text : R.color.textOff));

            String text = alarm.text == null ? "" : alarm.text.trim();
            if (text.isEmpty()) {
                tvText.setText("（未填写提醒内容）");
                tvText.setTextColor(getColor(R.color.textDim));
                tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
            } else {
                tvText.setText(text);
                tvText.setTextColor(getColor(alarm.enabled ? R.color.text : R.color.textOff));
                tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f * alarm.fontScale);
            }

            String label = alarm.daysLabel() + (alarm.voice ? " · 语音播报" : "");
            tvDays.setText(label);
            tvDays.setTextColor(getColor(alarm.enabled ? R.color.textDim : R.color.textOff));

            sw.setChecked(alarm.enabled);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    alarm.enabled = isChecked;
                    AlarmStore.get(MainActivity.this).upsert(alarm);
                    if (isChecked) {
                        AlarmScheduler.schedule(MainActivity.this, alarm);
                    } else {
                        AlarmScheduler.cancel(MainActivity.this, alarm.id);
                    }
                    tvTime.setTextColor(getColor(isChecked ? R.color.text : R.color.textOff));
                    tvText.setTextColor(getColor(isChecked ? R.color.text : R.color.textOff));
                    tvDays.setTextColor(getColor(isChecked ? R.color.textDim : R.color.textOff));
                    updateNextSummary();
                }
            });

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(MainActivity.this, EditAlarmActivity.class);
                    i.putExtra(AlarmService.EXTRA_ALARM_ID, alarm.id);
                    startActivity(i);
                }
            });

            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    confirmDelete(alarm);
                    return true;
                }
            });

            container.addView(row);
        }
        updateNextSummary();
    }

    private void confirmDelete(final Alarm alarm) {
        new AlertDialog.Builder(this)
                .setTitle("删除闹钟")
                .setMessage("确定删除 " + alarm.timeText() + " 的闹钟吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlarmScheduler.cancel(MainActivity.this, alarm.id);
                        AlarmStore.get(MainActivity.this).delete(alarm.id);
                        render();
                    }
                })
                .show();
    }

    private void updateNextSummary() {
        AlarmStore store = AlarmStore.get(this);
        long now = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        Alarm bestAlarm = null;
        for (Alarm a : store.all()) {
            if (!a.enabled) {
                continue;
            }
            long t = AlarmScheduler.nextTriggerAt(a, now + 1000L);
            if (t < best) {
                best = t;
                bestAlarm = a;
            }
        }
        if (bestAlarm == null) {
            tvNext.setText("还没有开启的闹钟");
        } else {
            String when = dayLabel(best);
            String time = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(best));
            tvNext.setText("下一次：" + when + " " + time + " · " + bestAlarm.daysLabel());
        }
        updatePermissionBanner();
    }

    private String dayLabel(long time) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        String target = f.format(new Date(time));
        String today = f.format(new Date());
        String tomorrow = f.format(new Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L));
        if (target.equals(today)) {
            return "今天";
        }
        if (target.equals(tomorrow)) {
            return "明天";
        }
        return new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(time));
    }

    private void updatePermissionBanner() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !AlarmScheduler.canScheduleExact(this)) {
            missing.add("精确闹钟");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && !nm.canUseFullScreenIntent()) {
                missing.add("全屏通知");
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add("通知");
        }
        if (missing.isEmpty()) {
            tvPermission.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb = new StringBuilder("点击此处开启：");
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append(missing.get(i));
        }
        sb.append(" 权限，否则闹钟可能不响或无法全屏弹出");
        tvPermission.setText(sb.toString());
        tvPermission.setVisibility(View.VISIBLE);
    }
}