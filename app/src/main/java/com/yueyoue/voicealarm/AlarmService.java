package com.yueyoue.voicealarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * 响铃前台服务：循环播放闹铃、震动，并按设定朗读提醒内容。
 */
public class AlarmService extends Service {

    public static final String ACTION_RING = "com.yueyoue.voicealarm.START_RING";
    public static final String ACTION_STOP = "com.yueyoue.voicealarm.STOP_RING";
    public static final String EXTRA_ALARM_ID = "alarm_id";
    public static final String CHANNEL_ID = "alarm_ring_channel";
    public static final int NOTIFICATION_ID = 2001;

    private static final String TAG = "AlarmService";
    private static final long VOICE_REPEAT_MS = 45000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MediaPlayer player;
    private Vibrator vibrator;
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean ringing;
    private int alarmId = -1;
    private String speakText = "";
    private int savedAlarmVolume = -1;

    private final Runnable voiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (!ringing) {
                return;
            }
            speakNow();
            handler.postDelayed(this, VOICE_REPEAT_MS);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        initTts();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopRinging(true);
            return START_NOT_STICKY;
        }
        if (ACTION_RING.equals(action)) {
            alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
            AlarmStore store = AlarmStore.get(this);
            Alarm alarm = store.find(alarmId);
            String text = alarm == null || alarm.text == null ? "" : alarm.text.trim();
            boolean voiceOn = alarm != null && alarm.voice && store.isVoiceEnabled();
            speakText = voiceOn ? text : "";

            startForeground(NOTIFICATION_ID, buildNotification(alarm));
            startRinging();

            handler.removeCallbacks(voiceRunnable);
            if (!speakText.isEmpty()) {
                handler.postDelayed(voiceRunnable, 1000L);
            }
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRinging(false);
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
            Log.w(TAG, "TTS 初始化失败", e);
            ttsReady = false;
        }
    }

    private void speakNow() {
        if (!ttsReady || tts == null || speakText.isEmpty() || !ringing) {
            return;
        }
        try {
            tts.setSpeechRate(0.9f);
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "alarm_speech");
        } catch (Exception e) {
            Log.w(TAG, "语音播报失败", e);
        }
    }

    private void startRinging() {
        if (ringing) {
            return;
        }
        ringing = true;
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            try {
                savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
                am.setStreamVolume(AudioManager.STREAM_ALARM,
                        am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
            } catch (Exception ignored) {
                savedAlarmVolume = -1;
            }
        }
        startPlayer();
        startVibration();
    }

    private void startPlayer() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (uri == null) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            Log.w(TAG, "闹铃播放失败", e);
            releasePlayer();
        }
    }

    private void startVibration() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            long[] pattern = {0, 800, 500, 800, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "震动失败", e);
        }
    }

    private void stopRinging(boolean stopService) {
        ringing = false;
        handler.removeCallbacks(voiceRunnable);
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {
                // ignore
            }
        }
        releasePlayer();
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
                // ignore
            }
            vibrator = null;
        }
        restoreVolume();
        if (stopService) {
            try {
                stopForeground(true);
            } catch (Exception ignored) {
                // ignore
            }
            stopSelf();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
            } catch (Exception ignored) {
                // ignore
            }
            try {
                player.release();
            } catch (Exception ignored) {
                // ignore
            }
            player = null;
        }
    }

    private void restoreVolume() {
        if (savedAlarmVolume < 0) {
            return;
        }
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            try {
                am.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0);
            } catch (Exception ignored) {
                // ignore
            }
        }
        savedAlarmVolume = -1;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "闹钟响铃",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("闹钟到点时的全屏提醒");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setBypassDnd(true);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(Alarm alarm) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        Intent ring = new Intent(this, RingActivity.class);
        ring.putExtra(EXTRA_ALARM_ID, alarmId);
        ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent ringPi = PendingIntent.getActivity(this, alarmId, ring, flags);

        Intent stop = new Intent(this, AlarmService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, NOTIFICATION_ID + 1, stop, flags);

        String title = alarm == null ? "闹钟" : ("闹钟 " + alarm.timeText());
        String content = speakText.isEmpty() ? "时间到了，点开后向右滑动解除" : speakText;

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(false)
                .setContentIntent(ringPi)
                .setFullScreenIntent(ringPi, true);

        builder.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_notification),
                "打开闹钟", ringPi).build());
        builder.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, R.drawable.ic_notification),
                "停止响铃", stopPi).build());
        return builder.build();
    }
}