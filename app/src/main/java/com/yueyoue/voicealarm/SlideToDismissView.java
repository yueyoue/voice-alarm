package com.yueyoue.voicealarm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 向右滑动才能解除的滑块控件（避免误触关闭闹铃）。
 */
public class SlideToDismissView extends View {

    public interface OnDismissListener {
        void onDismissed();
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final float density;
    private final float pad;

    private float thumbX = 0f;
    private float downX = 0f;
    private float startThumbX = 0f;
    private float maxTravel = 0f;
    private boolean tracking = false;
    private boolean busy = false;
    private OnDismissListener listener;

    public SlideToDismissView(Context context) {
        this(context, null);
    }

    public SlideToDismissView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideToDismissView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        pad = 6f * density;

        trackPaint.setColor(Color.parseColor("#1FFFFFFF"));
        trackPaint.setStyle(Paint.Style.FILL);

        fillPaint.setColor(Color.parseColor("#553D82F0"));
        fillPaint.setStyle(Paint.Style.FILL);

        thumbPaint.setColor(Color.parseColor("#2F6FEB"));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(17f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        arrowPaint.setColor(Color.WHITE);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(3f * density);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);

        setClickable(true);
    }

    public void setOnDismissListener(OnDismissListener l) {
        this.listener = l;
    }

    private float thumbRadius() {
        return Math.max(0f, (getHeight() - 2f * pad) / 2f);
    }

    private void recompute() {
        float r = thumbRadius();
        maxTravel = Math.max(0f, getWidth() - 2f * pad - 2f * r);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recompute();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        recompute();
        float w = getWidth();
        float h = getHeight();
        float r = thumbRadius();
        if (r <= 0f || w <= 0f) {
            return;
        }

        // 轨道
        rect.set(0f, pad, w, h - pad);
        canvas.drawRoundRect(rect, r, r, trackPaint);

        // 已滑动进度
        float progressRight = pad + thumbX + 2f * r;
        rect.set(pad, pad, Math.min(progressRight, w - pad), h - pad);
        canvas.drawRoundRect(rect, r, r, fillPaint);

        // 提示文字（随滑动淡出）
        float progress = maxTravel > 0f ? thumbX / maxTravel : 0f;
        int alpha = (int) (255f * (1f - progress));
        textPaint.setAlpha(Math.max(0, alpha));
        float textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText("向右滑动解除闹钟", w / 2f + r * 0.5f, textY, textPaint);

        // 滑块
        float cx = pad + thumbX + r;
        float cy = h / 2f;
        canvas.drawCircle(cx, cy, r, thumbPaint);

        // 滑块内箭头
        float a = r * 0.34f;
        canvas.drawLine(cx - a, cy - a, cx + a * 0.7f, cy, arrowPaint);
        canvas.drawLine(cx - a, cy + a, cx + a * 0.7f, cy, arrowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || busy) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                startThumbX = thumbX;
                tracking = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking) {
                    setThumb(startThumbX + (event.getX() - downX));
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (tracking) {
                    tracking = false;
                    if (maxTravel > 0f && thumbX / maxTravel >= 0.8f) {
                        completeDismiss();
                    } else {
                        animateTo(0f, 200);
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void setThumb(float value) {
        thumbX = Math.max(0f, Math.min(maxTravel, value));
        invalidate();
    }

    private void animateTo(float target, long duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(thumbX, target);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setThumb((Float) animation.getAnimatedValue());
            }
        });
        animator.start();
    }

    private void completeDismiss() {
        busy = true;
        ValueAnimator animator = ValueAnimator.ofFloat(thumbX, maxTravel);
        animator.setDuration(180);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setThumb((Float) animation.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (listener != null) {
                    listener.onDismissed();
                }
            }
        });
        animator.start();
    }
}