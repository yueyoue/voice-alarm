package com.yueyoue.voicealarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * 设置：语音提醒总开关、新闹钟默认字号、权限检查、使用提示。
 */
public class SettingsActivity extends Activity {

    private static final float[] FONT_SCALES = {1.0f, 1.3f, 1.7f, 2.2f};

    private TextView[] fontOptions = new TextView[4];
    private TextView tvVoiceStatus;
    private TextView tvExactStatus;
    private TextView tvFullScreenStatus;
    private TextView tvNotifStatus;

    private TextToSpeech tts;
    private boolean ttsReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final AlarmStore store = AlarmStore.get(this);

        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        Switch swVoice = findViewById(R.id.swVoice);
        swVoice.setChecked(store.isVoiceEnabled());
        updateVoiceStatus(store.isVoiceEnabled());
        swVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                store.setVoiceEnabled(isChecked);
                updateVoiceStatus(isChecked);
            }
        });

        fontOptions[0] = findViewById(R.id.sFont0);
        fontOptions[1] = findViewById(R.id.sFont1);
        fontOptions[2] = findViewById(R.id.sFont2);
        fontOptions[3] = findViewById(R.id.sFont3);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            fontOptions[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.setDefaultFontScale(FONT_SCALES[index]);
                    refreshFontOptions(store.getDefaultFontScale());
                }
            });
        }
        refreshFontOptions(store.getDefaultFontScale());

        findViewById(R.id.btnTestVoice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak("语音提醒已开启，您的闹钟将在设定时间响起，并朗读提醒内容。");
            }
        });

        tvExactStatus = findViewById(R.id.tvExactStatus);
        tvFullScreenStatus = findViewById(R.id.tvFullScreenStatus);
        tvNotifStatus = findViewById(R.id.tvNotifStatus);

        findViewById(R.id.rowExact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExactAlarmSettings();
            }
        });
        findViewById(R.id.rowFullScreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFullScreenIntentSettings();
            }
        });
        findViewById(R.id.rowNotif).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationSettings();
            }
        });
        findViewById(R.id.rowBattery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatterySettings();
            }
        });

        initTts();
        refreshPermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionStatus();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Exception ignored) {
                // ignore
            }
            tts = null;
        }
        super.onDestroy();
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && tts != null) {
                        int r = tts.setLanguage(Locale.CHINA);
                        ttsReady = r != TextToSpeech.LANG_MISSING_DATA
                                && r != TextToSpeech.LANG_NOT_SUPPORTED;
                    } else {
                        ttsReady = false;
                    }
                }
            });
        } catch (Exception e) {
            ttsReady = false;
        }
    }

    private void speak(String text) {
        if (!ttsReady || tts == null) {
            Toast.makeText(this, "语音引擎未就绪，请检查系统的「文字转语音」中文语音包",
                    Toast.LENGTH_LONG).show();
            return;
        }
        tts.setSpeechRate(0.9f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "settings_demo");
    }

    private void updateVoiceStatus(boolean enabled) {
        tvVoiceStatus.setText(enabled
                ? "已开启：响铃时朗读提醒文字"
                : "已关闭：只响铃，不朗读文字");
    }

    private void refreshFontOptions(float current) {
        int selected = 0;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < FONT_SCALES.length; i++) {
            float diff = Math.abs(FONT_SCALES[i] - current);
            if (diff < bestDiff) {
                bestDiff = diff;
                selected = i;
            }
        }
        for (int i = 0; i < 4; i++) {
            fontOptions[i].setBackgroundResource(
                    i == selected ? R.drawable.bg_option_selected : R.drawable.bg_option);
        }
    }

    private void refreshPermissionStatus() {
        boolean exact = AlarmScheduler.canScheduleExact(this);
        tvExactStatus.setText(exact ? "已允许" : "去开启 ›");
        tvExactStatus.setTextColor(getColor(exact ? R.color.accentLight : R.color.textDim));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            boolean ok = nm != null && nm.canUseFullScreenIntent();
            tvFullScreenStatus.setText(ok ? "已允许" : "去开启 ›");
            tvFullScreenStatus.setTextColor(getColor(ok ? R.color.accentLight : R.color.textDim));
        } else {
            tvFullScreenStatus.setText("无需设置");
            tvFullScreenStatus.setTextColor(getColor(R.color.textDim));
        }

        boolean notif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        tvNotifStatus.setText(notif ? "已允许" : "去开启 ›");
        tvNotifStatus.setTextColor(getColor(notif ? R.color.accentLight : R.color.textDim));
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
                return;
            } catch (Exception ignored) {
                // 落到应用详情
            }
        }
        openAppDetails();
    }

    private void openFullScreenIntentSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startActivity(new Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT",
                        Uri.parse("package:" + getPackageName())));
                return;
            } catch (Exception ignored) {
                // 落到应用详情
            }
        }
        openAppDetails();
    }

    private void openNotificationSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private void openBatterySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "请在手机设置中允许本应用自启动与后台运行", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppDetails() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            Toast.makeText(this, "请到系统设置中手动开启权限", Toast.LENGTH_LONG).show();
        }
    }
}