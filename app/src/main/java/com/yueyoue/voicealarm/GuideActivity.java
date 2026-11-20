package com.yueyoue.voicealarm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 使用教程：手机后台权限配置（重点小米 / 红米）、常见问题与本应用用法。
 * 内容以卡片形式动态渲染，便于后续增删条目。
 */
public class GuideActivity extends Activity {

    private static final String[][] SECTIONS = {
            {
                    "先看这里：为什么闹钟可能不响",
                    "安卓系统为了省电，会在锁屏后「冻结」或「清理」后台应用。闹钟一旦被冻结，到点就不会响，或者响得不准。\n\n"
                            + "解决办法是把下面三件事一起做完：\n"
                            + "① 允许自启动\n② 省电策略设为「无限制」\n③ 在多任务界面给应用加锁\n\n"
                            + "只做其中一项，多数机型仍然会被清理。"
            },
            {
                    "第一步：允许自启动",
                    "路径：设置 → 应用设置 → 授权管理 → 自启动管理 → 找到「语音闹钟」→ 打开开关。\n\n"
                            + "系统默认会「根据使用情况智能优化不常用应用的自启动权限」，新安装的应用常被自动关掉，所以必须手动打开。\n\n"
                            + "同一页面下方还有「关联启动管理」，建议点右上角「全部禁止」，既省电又不影响闹钟。"
            },
            {
                    "第二步：省电策略改成「无限制」（最关键）",
                    "路径：设置 → 省电与电池 → 右上角齿轮图标 → 应用省电策略 → 找到「语音闹钟」→ 选择「无限制」。\n\n"
                            + "快捷方式：长按桌面上的「语音闹钟」图标 → 应用信息 → 省电策略 → 无限制。\n\n"
                            + "默认的「智能限制后台」会在锁屏后限制唤醒和联网，闹钟就会延迟或不响。这一步没做，只开自启动也没用。"
            },
            {
                    "第三步：后台任务加锁",
                    "从屏幕底部上滑进入多任务界面 → 长按「语音闹钟」的卡片 → 点一下小锁图标。\n\n"
                            + "锁定后即使点「清理后台」，本应用也不会被清掉，还能避免反复冷启动，反而更省电。"
            },
            {
                    "第四步：打开通知与全屏提醒",
                    "路径：设置 → 应用设置 → 应用管理 → 语音闹钟 → 通知管理 → 全部打开，并允许「悬浮通知 / 横幅」。\n\n"
                            + "Android 13 及以上需要「通知」权限；Android 14 及以上还需要「全屏通知」权限。"
                            + "这两项可以直接点首页顶部的黄色提示条，或在「设置 → 权限检查」里开启。\n\n"
                            + "没有全屏通知权限时，闹钟只会出现在通知栏，不会自动弹出大字界面。"
            },
            {
                    "第五步：精确闹钟与电池优化",
                    "精确闹钟：点首页黄色提示条，或 设置 → 应用 → 特殊权限 → 闹钟和提醒 → 允许。\n\n"
                            + "电池优化：设置 → 应用设置 → 特殊权限 → 电池优化 → 找到「语音闹钟」→ 选择「不优化」。"
            },
            {
                    "不同系统版本的菜单对照",
                    "HyperOS 2 / 3：自启动 = 设置 → 应用设置 → 授权管理 → 自启动管理；省电 = 设置 → 省电与电池 → 齿轮 → 应用省电策略\n\n"
                            + "MIUI 12 – 14：自启动同上；省电 = 手机管家 → 省电与电池 → 齿轮 → 应用智能省电\n\n"
                            + "老版 MIUI：安全中心 → 应用管理 → 权限 → 自启动管理；省电 = 安全中心 → 省电模式 → 应用智能省电\n\n"
                            + "找不到时，直接在设置顶部的搜索框输入「自启动」或「省电策略」，一般能直达对应页面。"
            },
            {
                    "其他品牌手机",
                    "华为 / 荣耀：设置 → 应用 → 应用启动管理 → 关闭「自动管理」→ 允许自启动、关联启动、后台活动。\n\n"
                            + "OPPO / 一加 / realme：设置 → 电池 → 应用耗电管理 → 允许后台运行；设置 → 应用管理 → 自启动管理。\n\n"
                            + "VIVO / iQOO：设置 → 电池 → 后台耗电管理 → 允许后台高耗电；i管家 → 应用管理 → 自启动。\n\n"
                            + "三星：设置 → 电池 → 后台使用限制 → 把本应用加入「永不休眠的应用」。"
            },
            {
                    "常见问题",
                    "· 只开自启动还是被清：省电策略多半仍是「智能限制」，改成「无限制」。\n"
                            + "· 开了省电模式：省电模式会统一限制后台，期间要重新确认策略。\n"
                            + "· 开发者选项里的「后台进程限制」：如果设过「最多 4 个进程」之类，请改回「标准限制」。\n"
                            + "· 不要频繁一键清理后台：反复清理再打开会造成冷启动，更耗电，也更容易杀掉闹钟。\n"
                            + "· 语音没有声音：系统设置 → 语言与输入 → 文字转语音（TTS）→ 安装或选择中文语音引擎。"
            },
            {
                    "本应用怎么用",
                    "① 首页点「+ 添加闹钟」→ 点大号时间选时间 → 输入提醒内容 → 选文字大小 → 需要朗读就打开「语音朗读」→ 选重复的星期 → 保存。\n"
                            + "② 每条闹钟右侧开关可单独启用 / 停用；点闹钟可以修改；长按闹钟可以删除。\n"
                            + "③ 闹钟响起是满屏大字，必须按住底部滑块向右拖到尽头才会停止，点按和返回键都关不掉，避免误触。\n"
                            + "④ 设置页可以开关「语音提醒」、设置新闹钟默认字号、试听语音、检查权限。"
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        final LinearLayout container = findViewById(R.id.guideContainer);
        for (String[] section : SECTIONS) {
            addSection(container, section[0], section[1]);
        }

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btnOpenAutostart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAutostartSettings();
            }
        });

        findViewById(R.id.btnOpenAppInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppInfo();
            }
        });
    }

    private void addSection(LinearLayout container, String title, String body) {
        float density = getResources().getDisplayMetrics().density;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int padding = (int) (16 * density);
        card.setPadding(padding, padding, padding, padding);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.text));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextColor(getColor(R.color.textBody));
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        tvBody.setLineSpacing(6f * density, 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = (int) (8 * density);
        tvBody.setLayoutParams(bodyLp);
        card.addView(tvBody);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = (int) (12 * density);
        container.addView(card, cardLp);
    }

    /** 尝试直达各品牌的自启动管理页面，失败则回退到应用详情页。 */
    private void openAutostartSettings() {
        String[][] targets = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
        };
        for (String[] target : targets) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(target[0], target[1]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (getPackageManager().resolveActivity(intent, 0) != null) {
                    startActivity(intent);
                    return;
                }
            } catch (Exception ignored) {
                // 继续尝试下一个
            }
        }
        Toast.makeText(this, "没有找到自启动设置页面，请按上面的步骤手动查找", Toast.LENGTH_LONG).show();
        openAppInfo();
    }

    private void openAppInfo() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            Toast.makeText(this, "请到手机设置中手动开启权限", Toast.LENGTH_LONG).show();
        }
    }
}