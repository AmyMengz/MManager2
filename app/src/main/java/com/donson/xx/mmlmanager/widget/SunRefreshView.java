package com.donson.xx.mmlmanager.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Animatable;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.donson.xx.mmlmanager.R;
import com.donson.xx.mmlmanager.util.Utils;

/**
 * Created by Administrator on 2017/4/27.
 */

public class SunRefreshView extends BaseRefreshView implements Animatable {
    private static final float SCALE_START_PERCENT = .5f;
    private static final int ANIMATION_DURATION = 1000;

    private static final float SKY_RATIO = .65f;//天空比
    private static final float SKY_INITIAL_SCALE = 1.05f;

    private final static float TOWN_RATIO = 0.22f;
    private static final float TOWN_INITIAL_SCALE = 1.20f;
    private static final float TOWN_FINAL_SCALE = 1.30f;

    private static final float SUN_FINAL_SCALE = 0.75f;
    private static final float SUN_INITIAL_ROTATE_GROWTH = 1.2f;
    private static final float SUN_FINAL_ROTATE_GROWTH = 1.5f;
    private static final Interpolator LINERA_INTERPOLATOR = new LinearInterpolator();//控制android动画的执行速率
    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Animation mAnimation;
    private int mTop;
    private int mScreenWidth;

    private int mSkyHeight;
    private float mSkyTopOffset;
    private float mSkyMoveOffset;

    private int mTownHeight;
    private float mTownInitialTopOffset;
    private float mTownFinalTopOffset;
    private float mTownMoveOffset;

    private int mSunSize = 100;
    private float mSunLeftOffset;
    private float mSunTopOffset;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mSky;
    private Bitmap mSun;
    private Bitmap mTown;

    private boolean isRefreshing = false;

    public SunRefreshView(Context context, final PullToRefreshView patent) {
        super(context, patent);
        mParent = patent;
        mMatrix = new Matrix();
        setupAnimations();
        mParent.post(new Runnable() {
            @Override
            public void run() {
                initiateDimes(patent.getWidth());
            }


        });

    }

    private void initiateDimes(int viewWidth) {
        if (viewWidth <= 0 || viewWidth == mScreenWidth) return;
        mScreenWidth = viewWidth;
        mSkyHeight = (int) (SKY_RATIO * mScreenWidth);
        mSkyTopOffset = (mSkyHeight * 0.38f);
        mSkyMoveOffset = Utils.convertDpToPixel(getContext(), 15);

        mTownHeight = (int) (TOWN_RATIO * mScreenWidth);
        mTownInitialTopOffset = (mParent.getTotalDragDistance() - mTownHeight * TOWN_INITIAL_SCALE);
        mTownFinalTopOffset = (mParent.getTotalDragDistance() - mTownHeight * TOWN_FINAL_SCALE);
        mTownMoveOffset = Utils.convertDpToPixel(getContext(), 10);

        mSunLeftOffset = 0.3f * (float) mScreenWidth;
        mSunTopOffset = (mParent.getTotalDragDistance() * 0.1f);

        mTop = -mParent.getTotalDragDistance();
        createBitmaps();
    }

    private void createBitmaps() {
        mSky = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.sky);
        mSky = Bitmap.createScaledBitmap(mSky, mScreenWidth, mSkyHeight, true);
        mTown = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.buildings);
        mTown = Bitmap.createScaledBitmap(mTown, mScreenWidth, (int) (mScreenWidth * TOWN_RATIO), true);
        mSun = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.sun);
        mSun = Bitmap.createScaledBitmap(mSun, mSunSize, mSunSize, true);
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);//无限
        mAnimation.setRepeatMode(Animation.RESTART);
        mAnimation.setInterpolator(LINERA_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    /**
     * 旋转
     *
     * @param rotate
     */
    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);

    }

    public void setPercent(float percent) {
        mPercent = percent;
    }


    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mScreenWidth <= 0) return;
        final int saveCount = canvas.save();
        canvas.translate(0, mTop);
        canvas.clipRect(0, -mTop, mScreenWidth, mParent.getTotalDragDistance());
        drawSky(canvas);
        drawSun(canvas);
        drawTown(canvas);
        canvas.restoreToCount(saveCount);

    }

    private void drawTown(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();
        float dragPercent = Math.min(1f, Math.abs(mPercent));
        float townScale;
        float townTopOffset;
        float townMoveOffset;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            townScale = TOWN_INITIAL_SCALE + (TOWN_FINAL_SCALE - TOWN_INITIAL_SCALE) * scalePercent;
            townTopOffset = mTownInitialTopOffset - (mTownFinalTopOffset - TOWN_INITIAL_SCALE) * scalePercent;
            townMoveOffset = mTownMoveOffset * (1.0f - scalePercent);
        } else {
            float scalePercent = dragPercent / SCALE_START_PERCENT;
            townScale = TOWN_INITIAL_SCALE;
            townTopOffset = mTownInitialTopOffset;
            townMoveOffset = mTownMoveOffset * scalePercent;
        }
        float offsetX = -(mScreenWidth * townScale - mScreenWidth) / 2.0f;
        float offsetY = (1.0f - dragPercent) * mParent.getTotalDragDistance()
                + townTopOffset
                - mTownHeight * (townScale - 1.0f) / 2
                + townMoveOffset;
        matrix.postScale(townScale,townScale);
        matrix.postTranslate(offsetX,offsetY);
        canvas.drawBitmap(mTown,matrix,null);
    }

    private void drawSky(Canvas canvas) {
        Matrix matrix = mMatrix;
        mMatrix.reset();
        float dragPercent = Math.min(1f,Math.abs(mPercent));
        float skyScale;
        float scalePErcentDelta = dragPercent - SCALE_START_PERCENT;
        if(scalePErcentDelta>0){
            float scalePercent = scalePErcentDelta/(1.0f - SCALE_START_PERCENT);
            skyScale = SKY_INITIAL_SCALE - (SKY_INITIAL_SCALE -1.0f)*scalePercent;
        }else {
            skyScale = SKY_INITIAL_SCALE;
        }
        float offsetX = -(mScreenWidth*skyScale-mScreenWidth)/2.0f;
        float offserY = (1.0f -dragPercent)*mParent.getTotalDragDistance()
                - mSkyTopOffset
                -mSkyHeight*(skyScale-1.0f)/2
                +mSkyMoveOffset*dragPercent;
        matrix.postScale(skyScale,skyScale);
        matrix.postTranslate(offsetX,offserY);
        canvas.drawBitmap(mSky,mMatrix,null);

    }
    private void drawSun(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        float sunRadius = (float) mSunSize / 2.0f;
        float sunRotateGrowth = SUN_INITIAL_ROTATE_GROWTH;

        float offsetX = mSunLeftOffset;
        float offsetY = mSunTopOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f - dragPercent) // Move the sun up
                - mTop; // Depending on Canvas position

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            float sunScale = 1.0f - (1.0f - SUN_FINAL_SCALE) * scalePercent;
            sunRotateGrowth += (SUN_FINAL_ROTATE_GROWTH - SUN_INITIAL_ROTATE_GROWTH) * scalePercent;

            matrix.preTranslate(offsetX + (sunRadius - sunRadius * sunScale), offsetY * (2.0f - sunScale));
            matrix.preScale(sunScale, sunScale);

            offsetX += sunRadius;
            offsetY = offsetY * (2.0f - sunScale) + sunRadius * sunScale;
        } else {
            matrix.postTranslate(offsetX, offsetY);

            offsetX += sunRadius;
            offsetY += sunRadius;
        }

        matrix.postRotate(
                (isRefreshing ? -360 : 360) * mRotate * (isRefreshing ? 1 : sunRotateGrowth),
                offsetX,
                offsetY);

        canvas.drawBitmap(mSun, matrix, null);

    }
    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, mSkyHeight+top);
    }


    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);

    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    @Override
    public boolean isRunning() {
        return false;
    }



}
