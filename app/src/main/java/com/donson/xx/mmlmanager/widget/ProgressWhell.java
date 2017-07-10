package com.donson.xx.mmlmanager.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import com.donson.xx.mmlmanager.R;
import com.donson.xx.mmlmanager.util.Logger;

/**
 * Created by Administrator on 2017/4/24.
 */

public class ProgressWhell extends View{
    private int barWidth = 4;//bar的宽度，圆弧线宽
    private int rimWidth = 4;//边缘圆弧线宽
    private int circleRadius =28;//圆弧半径  在onMeasure中与控件宽度对比
    private boolean fillRadius = false;
    private float spinSpeed = 230.0f;//旋转速度

    private long lastTimeAnimated = 0;//最后一次转时间  从开机到现在的毫秒数
    private boolean isSpinning = false;
    private float mProgress = 0.0f;
    private float mTargetProgress = 0.0f;
    private boolean shouldAnimate;
    private RectF circleBounds = new RectF();//circle 边界

    private Paint rimPaint = new Paint();
    private Paint barPaint = new Paint();

    private int barColor = 0xAA000000;
    private int rimColor = 0x00FFFFFF;


    public ProgressWhell(Context context) {
        this(context,null);
    }

    public ProgressWhell(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ProgressWhell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
        
    }
    public void setProgress(float progress){
        mProgress = progress;
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressWheel);
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        barWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,barWidth,metrics);//转变为标准尺寸
        rimWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,rimWidth,metrics);
        circleRadius =(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, circleRadius, metrics);
        circleRadius = (int) a.getDimension(R.styleable.ProgressWheel_matProg_circleRadius,circleRadius);
        fillRadius = a.getBoolean(R.styleable.ProgressWheel_matProg_fillRadius,false);
        barWidth = (int)a.getDimension(R.styleable.ProgressWheel_matProg_barWidth,barWidth);
        rimWidth = (int)a.getDimension(R.styleable.ProgressWheel_matProg_rimWidth,rimWidth);
        float baseSpinSpeed = a.getFloat(R.styleable.ProgressWheel_matProg_spinSpeed,spinSpeed/360.0f);
        spinSpeed = baseSpinSpeed * 360;

        barColor = a.getColor(R.styleable.ProgressWheel_matProg_barColor, barColor);

        rimColor = a.getColor(R.styleable.ProgressWheel_matProg_rimColor, rimColor);
       if( a.getBoolean(R.styleable.ProgressWheel_matProg_progressIndeterminate, false)){
           spin();
       }
        a.recycle();
        setAnimationEnabled();
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setAnimationEnabled() {
        int currentApiVersion = Build.VERSION.SDK_INT;
        float  animationValue;
        if(currentApiVersion >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            animationValue = Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1);
        }else {
            animationValue = Settings.System.getFloat(getContext().getContentResolver(),Settings.System.ANIMATOR_DURATION_SCALE,1);
        }
        shouldAnimate = animationValue != 0;
    }

    public boolean isSpinning() {
        return isSpinning;
    }

    /**
     *
     * puts the view on spin mode
     */
    private void spin() {
        lastTimeAnimated = SystemClock.uptimeMillis();
        isSpinning = true;
        invalidate();
    }

    /**
     * turn off spin mode
     */
    public void stopSpinning() {
        isSpinning = false;
        mProgress = 0.0f;
        mTargetProgress = 0.0f;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Logger.i("onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int viewWidth = circleRadius + this.getPaddingLeft() + this.getPaddingRight();
        int viewHeight = circleRadius + this.getPaddingTop() + this.getPaddingBottom();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;
        if(widthMode == MeasureSpec.EXACTLY){
            width = widthSize;
        }else if(widthMode == MeasureSpec.AT_MOST){//wrap_content
            width = Math.min(viewWidth,widthSize);
        }else {
            width = viewWidth;
        }
        if (heightMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(viewHeight, heightSize);
        } else {
            //Be whatever you want
            height = viewHeight;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Logger.i("onSizeChanged"+w+"___"+h+"___"+oldw+":"+oldw);
        super.onSizeChanged(w, h, oldw, oldh);

        setupBounds(w, h);
        setupPaints();
        invalidate();
    }

    private void setupBounds(int layout_width, int layout_height) {
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        if (!fillRadius) {
            // Width should equal to Height, find the min value to setup the circle
            int minValue = Math.min(layout_width - paddingLeft - paddingRight,
                    layout_height - paddingBottom - paddingTop);

            int circleDiameter = Math.min(minValue, circleRadius * 2 - barWidth * 2);

            // Calc the Offset if needed for centering the wheel in the available space
            int xOffset = (layout_width - paddingLeft - paddingRight - circleDiameter) / 2 + paddingLeft;
            int yOffset = (layout_height - paddingTop - paddingBottom - circleDiameter) / 2 + paddingTop;

            circleBounds =
                    new RectF(xOffset + barWidth, yOffset + barWidth, xOffset + circleDiameter - barWidth,
                            yOffset + circleDiameter - barWidth);



        } else {
            circleBounds = new RectF(paddingLeft + barWidth, paddingTop + barWidth,
                    layout_width - paddingRight - barWidth, layout_height - paddingBottom - barWidth);
        }
    }
    private void setupPaints() {
        barPaint.setColor(barColor);
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setStrokeWidth(barWidth);

        rimPaint.setColor(rimColor);
        rimPaint.setAntiAlias(true);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(rimWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Logger.i("onDraw");
        super.onDraw(canvas);
        canvas.drawArc(circleBounds,360,360,false,rimPaint);
        boolean mustInvalidate = false;

        if (!shouldAnimate) {
            return;
        }

        if (isSpinning) {
            //Draw the spinning bar
            mustInvalidate = true;
            long deltaTime = (SystemClock.uptimeMillis()-lastTimeAnimated);
            float deltaNormalized = deltaTime*spinSpeed/1000.0f;
//            long deltaTime = (SystemClock.uptimeMillis() - lastTimeAnimated);
//            float deltaNormalized = deltaTime * spinSpeed / 1000.0f;
            Logger.i("deltaNormalized::"+deltaNormalized+"  mProgress:"+mProgress+"  isInEditMode():"+isInEditMode()+"  deltaTime:"+deltaTime+"   "+SystemClock.uptimeMillis());

//            updateBarLength(deltaTime);

            mProgress += deltaNormalized;
            if (mProgress > 360) {
                mProgress -= 360f;

                // A full turn has been completed
                // we run the callback with -1 in case we want to
                // do something, like changing the color
//                runCallback(-1.0f);
            }
            lastTimeAnimated = SystemClock.uptimeMillis();

            float from = mProgress - 90;
            float length =80;//= barLength + barExtraLength;

            if (isInEditMode()) {
                from = 0;
                length = 135;
            }

            canvas.drawArc(circleBounds, from, length, false, barPaint);
        } else {
            float oldProgress = mProgress;

            if (mProgress != mTargetProgress) {
                //We smoothly increase the progress bar
                mustInvalidate = true;

                float deltaTime = (float) (SystemClock.uptimeMillis() - lastTimeAnimated) / 1000;
                float deltaNormalized = deltaTime * spinSpeed;

                mProgress = Math.min(mProgress + deltaNormalized, mTargetProgress);
                lastTimeAnimated = SystemClock.uptimeMillis();
            }

            if (oldProgress != mProgress) {
//                runCallback();
            }

            float offset = 0.0f;
            float progress = mProgress;
//            if (!linearProgress) {
//                float factor = 2.0f;
//                offset = (float) (1.0f - Math.pow(1.0f - mProgress / 360.0f, 2.0f * factor)) * 360.0f;
//                progress = (float) (1.0f - Math.pow(1.0f - mProgress / 360.0f, factor)) * 360.0f;
//            }

            if (isInEditMode()) {
                progress = 360;
            }

            canvas.drawArc(circleBounds, offset - 90, progress, false, barPaint);
        }

        if (mustInvalidate) {
            invalidate();
        }

    }
}
