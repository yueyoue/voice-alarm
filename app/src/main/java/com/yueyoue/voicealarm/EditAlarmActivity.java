package com.yueyoue.voicealarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

/**
 * 新建 / 编辑闹钟：时间、提醒文字、文字大小、语音播报、重复星期。
 */
public class EditAlarmActivity extends Activity {

    private static final float[] FONT_SCALES = {1.0f, 1.3f, 1.7f, 2.2f};

    private int alarmId;
    private Alarm alarm;

    private int hour;
    private int minute;
    private final boolean[] days = new boolean[7];
    private float fontScale = 1.0f;
    private boolean voice = true;

    private TextView tvTime;
    private EditText etText;
    private Switch swVoice;
    private TextView[] fontOptions = new TextView[4];
    private TextView[] dayViews = new TextView[7];
    private TextView btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        tvTime = findViewById(R.id.tvTime);
        etText = findViewById(R.id.etText);
        swVoice = findViewById(R.id.swVoice);
        btnDelete = findViewById(R.id.btnDelete);
        TextView tvTitle = findViewById(R.id.tvTitle);

        alarmId = getIntent().getIntExtra(AlarmService.EXTRA_ALARM_ID, 0);
        AlarmStore store = AlarmStore.get(this);
        if (alarmId > 0) {
            alarm = store.find(alarmId);
        }

        if (alarm != null) {
            hour = alarm.hour;
            minute = alarm.minute;
            System.arraycopy(alarm.days, 0, days, 0, 7);
            fontScale = alarm.fontScale;
            voice = alarm.voice;
            tvTitle.setText("编辑闹钟");
            etText.setText(alarm.text);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            alarm = new Alarm();
            alarmId = 0;
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, 5);
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
            fontScale = store.getDefaultFontScale();
            voice = store.isVoiceEnabled();
            tvTitle.setText("添加闹钟");
            btnDelete.setVisibility(View.GONE);
        }

        fontOptions[0] = findViewById(R.id.optFont0);
        fontOptions[1] = findViewById(R.id.optFont1);
        fontOptions[2] = findViewById(R.id.optFont2);
        fontOptions[3] = findViewById(R.id.optFont3);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            fontOptions[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectFont(index);
                }
            });
        }

        dayViews[0] = findViewById(R.id.day0);
        dayViews[1] = findViewById(R.id.day1);
        dayViews[2] = findViewById(R.id.day2);
        dayViews[3] = findViewById(R.id.day3);
        dayViews[4] = findViewById(R.id.day4);
        dayViews[5] = findViewById(R.id.day5);
        dayViews[6] = findViewById(R.id.day6);
        for (int i = 0; i < 7; i++) {
            final int index = i;
            dayViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    days[index] = !days[index];
                    refreshDays();
                }
            });
        }

        updateTimeText();
        refreshDays();

        int selected = 0;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < FONT_SCALES.length; i++) {
            float diff = Math.abs(FONT_SCALES[i] - fontScale);
            if (diff < bestDiff) {
                bestDiff = diff;
                selected = i;
            }
        }
        selectFont(selected);

        swVoice.setChecked(voice);
        swVoice.setOnCheckedChangeListener(null);

        tvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime();
            }
        });

        swVoice.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                voice = isChecked;
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });
    }

    private void pickTime() {
        TimePickerDialog dialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minuteOfDay) {
                        hour = hourOfDay;
                        minute = minuteOfDay;
                        updateTimeText();
                    }
                }, hour, minute, DateFormat.is24HourFormat(this));
        dialog.show();
    }

    private void updateTimeText() {
        tvTime.setText(String.format(Locale.CHINA, "%02d:%02d", hour, minute));
    }

    private void selectFont(int index) {
        fontScale = FONT_SCALES[index];
        for (int i = 0; i < 4; i++) {
            fontOptions[i].setBackgroundResource(
                    i == index ? R.drawable.bg_option_selected : R.drawable.bg_option);
        }
        etText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f * fontScale);
    }

    private void refreshDays() {
        for (int i = 0; i < 7; i++) {
            dayViews[i].setBackgroundResource(
                    days[i] ? R.drawable.bg_option_selected : R.drawable.bg_option);
        }
    }

    private void save() {
        String text = etText.getText().toString().trim();
        alarm.hour = hour;
        alarm.minute = minute;
        alarm.text = text;
        System.arraycopy(days, 0, alarm.days, 0, 7);
        alarm.fontScale = fontScale;
        alarm.voice = voice;

        AlarmStore store = AlarmStore.get(this);
        if (alarmId <= 0) {
            alarm.enabled = true;
            alarm.id = store.nextId();
        }
        store.upsert(alarm);

        if (alarm.enabled) {
            AlarmScheduler.schedule(this, alarm);
        } else {
            AlarmScheduler.cancel(this, alarm.id);
        }

        String tip = alarm.enabled
                ? "已保存，将在 " + alarm.timeText() + " 提醒"
                : "已保存（该闹钟当前是关闭状态）";
        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除闹钟")
                .setMessage("确定删除这个闹钟吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlarmScheduler.cancel(EditAlarmActivity.this, alarm.id);
                        AlarmStore.get(EditAlarmActivity.this).delete(alarm.id);
                        finish();
                    }
                })
                .show();
    }
}