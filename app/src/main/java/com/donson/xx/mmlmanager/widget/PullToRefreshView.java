package com.donson.xx.mmlmanager.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;

import com.donson.xx.mmlmanager.util.Logger;
import com.donson.xx.mmlmanager.util.Utils;

/**
 * Created by Administrator on 2017/4/25.
 */

public class PullToRefreshView extends ViewGroup {
    private static final int DRAG_MAX_DISTANCE = 120;
    private static final float DRAG_RATE = .5f;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;

    public static final int STYLE_SUN = 0;
    public static final int MAX_OFFSET_ANIMATION_DURATION = 700;

    private static final int INVALID_POINTER = -1;


    private View mTarget;
    ImageView mRefreshView;
    private Interpolator mDecelerateInterpolator;
    private int mTouchSlop;
    private int mTotalDragDistance;
    private BaseRefreshView mBaseRefreshView;
    private float mCurrentDragPercent;
    private int mCurrentOffsetTop;

    private int mTargetPaddingTop;
    private int mTargetPaddingBottom;
    private int mTargetPaddingRight;
    private int mTargetPaddingLeft;

    private boolean mRefreshing;
    private int mActivePointerId;
    private boolean mIsBeingDragged;
    private float mInitialMotionY; //action down 时 y相对于View的位置
    private int mFrom;
    private float mFromDragPercent;
    private boolean mNotify;
    private OnRefreshListener mListener;

    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    public void setRefreshing(boolean refreshing) {
        if(mRefreshing != refreshing){
            setRefreshing(refreshing,false);
        }
    }

    public interface OnRefreshListener {
        public void onRefresh();
    }


    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mRefreshView = new ImageView(context);
//        mRefreshView.setImageResource(R.drawable.buildings);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTotalDragDistance = Utils.convertDpToPixel(context, DRAG_MAX_DISTANCE);
        mBaseRefreshView = new SunRefreshView(getContext(), this);
        mRefreshView.setImageDrawable(mBaseRefreshView);
        addView(mRefreshView);
        setWillNotDraw(false);
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ensureTarget();
        if (mTarget == null) {
            return;
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingRight() - getPaddingLeft(), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
        mTarget.measure(widthMeasureSpec, heightMeasureSpec);
        mRefreshView.measure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * ensure target
     */
    private void ensureTarget() {
        if (mTarget != null) return;
        if (getChildCount() > 0) {

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                Logger.i("getChildCount():" + getChildCount() + "  child:" + child);
                if (child != mRefreshView) {
                    if (child != mRefreshView) {
                        mTarget = child;
                        mTargetPaddingBottom = mTarget.getPaddingBottom();
                        mTargetPaddingLeft = mTarget.getPaddingLeft();
                        mTargetPaddingRight = mTarget.getPaddingRight();
                        mTargetPaddingTop = mTarget.getPaddingTop();
                    }
                }
            }
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTop(0, true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);//点击的point相对于view的y方向距离

                if (initialMotionY == -1) {
                    return false;
                }
                mInitialMotionY = initialMotionY;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialMotionY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                Logger.i("ACTION_UP11111111111：mActivePointerId:" + mActivePointerId);

                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        Logger.i("：mIsBeingDragged: action" + action + "  " + mIsBeingDragged+"  mActivePointerId:"+mActivePointerId);
        return mIsBeingDragged;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        if (!mIsBeingDragged) {
            return super.onTouchEvent(ev);
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
//                Logger.i("ACTION_MOVE：pointerIndex:"+pointerIndex+"  mActivePointerId:"+mActivePointerId+"  mTotalDragDistance  :"+mTotalDragDistance);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;//手指滑动的差值
                final float scrollTop = yDiff * DRAG_RATE;//view要移动的距离
                mCurrentDragPercent = scrollTop / mTotalDragDistance;//View移动的距离和总距离的比例
                if (mCurrentDragPercent < 0) {
                    return false;
                }
                Logger.i("ACTION_MOVE：mCurrentDragPercent:" + mCurrentDragPercent+"  yDiff:"+yDiff+" mCurrentOffsetTop: "+mCurrentOffsetTop);
                float boundedDragPercent = Math.min(1f, Math.abs(mCurrentDragPercent));//最大percent
                float extraOS = Math.abs(scrollTop) - mTotalDragDistance;
                float slingshotDist = mTotalDragDistance;

                float tensionSlingshotPercent = Math.max(0,
                        Math.min(extraOS, slingshotDist * 2) / slingshotDist);

                float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                        (tensionSlingshotPercent / 4), 2)) * 2f;
                float extraMove = (slingshotDist) * tensionPercent / 2;
                int targetY = (int) ((slingshotDist * boundedDragPercent) + extraMove);
//                Logger.i("ACTION_MOVE：mInitialMotionY："+mInitialMotionY+" scrollTop:" + scrollTop + " extraOS:" + extraOS + "  tensionSlingshotPercent:" + tensionSlingshotPercent + "  tensionPercent:" + tensionPercent + " extraMove " + extraMove
//                        + " targetY:" + targetY);
                mBaseRefreshView.setPercent(mCurrentDragPercent, true);
                setTargetOffsetTop(targetY - mCurrentOffsetTop, true);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN:
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overScrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsBeingDragged = false;
                Logger.i("ACTION_UP：mActivePointerId:" + mActivePointerId+"  pointerIndex:"+pointerIndex+" y: "+y+" overScrollTop:"+overScrollTop+" mInitialMotionY:"+mInitialMotionY);

                if (overScrollTop > mTotalDragDistance) {
                    setRefreshing(true, true);
                } else {
                    mRefreshing = false;
                    animateOffsetToStartPosition();
                }

                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    /**
     * 刷新
     * @param refreshing
     * @param notify
     */
    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                mBaseRefreshView.setPercent(1f, true);
                animateOffsetToCorrectPosition();
            } else {
                animateOffsetToStartPosition();
            }
        }
    }

    /**
     * 动画回到初始状态
     */
    private void animateOffsetToStartPosition() {
        mFrom = mCurrentOffsetTop;
        mFromDragPercent = mCurrentDragPercent;
        long animationDuration = Math.abs((long) (MAX_OFFSET_ANIMATION_DURATION * mFromDragPercent));
        Logger.i("mFrom：mFrom:" + mFrom+"  mCurrentDragPercent:"+mCurrentDragPercent
                +" mFromDragPercent: "+mFromDragPercent+" animationDuration:"+animationDuration);
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(animationDuration);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mAnimateToStartPosition.setAnimationListener(mToStartListener);//监听Animation过程
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mAnimateToStartPosition);
    }

    /**
     * 刷新 显示最多
     */
    private void animateOffsetToCorrectPosition() {
        mFrom = mCurrentOffsetTop;
        mFromDragPercent = mCurrentDragPercent;

        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(MAX_OFFSET_ANIMATION_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        mRefreshView.clearAnimation();
        mRefreshView.startAnimation(mAnimateToCorrectPosition);

        if (mRefreshing) {
            mBaseRefreshView.start();
            if (mNotify) {
                if (mListener != null) {
                    mListener.onRefresh();
                }
            }
        } else {
            mBaseRefreshView.stop();
            animateOffsetToStartPosition();
        }
        mCurrentOffsetTop = mTarget.getTop();
        mTarget.setPadding(mTargetPaddingLeft, mTargetPaddingTop, mTargetPaddingRight, mTotalDragDistance);
    }

    /**
     * 回到初始状态的动画
     */
    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    /**
     *
     * 回到初始状态的动画
     * @param interpolatedTime
     */
    private void moveToStart(float interpolatedTime) {
        int targetTop = mFrom - (int) (mFrom * interpolatedTime);

        float targetPercent = mFromDragPercent * (1.0f - interpolatedTime);
        int offset = targetTop - mTarget.getTop();
        Logger.i("moveToStart---interpolatedTime:"+interpolatedTime+"  targetTop:"+targetTop+" targetPercent:"+targetPercent+" offset:"+offset+" mTarget.getTop():"+mTarget.getTop());
        mCurrentDragPercent = targetPercent;
        mBaseRefreshView.setPercent(mCurrentDragPercent, true);
        mTarget.setPadding(mTargetPaddingLeft, mTargetPaddingTop, mTargetPaddingRight, mTargetPaddingBottom + targetTop);
        setTargetOffsetTop(offset, false);
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop;
            int endTarget = mTotalDragDistance;
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mTarget.getTop();

            mCurrentDragPercent = mFromDragPercent - (mFromDragPercent - 1.0f) * interpolatedTime;
            mBaseRefreshView.setPercent(mCurrentDragPercent, false);

            Logger.i("targetTop:"+targetTop+" mFrom:"+mFrom+" offset:"+offset+"　mCurrentDragPercent:"+mCurrentDragPercent+" mFromDragPercent: "+mFromDragPercent);
            setTargetOffsetTop(offset, false /* requires update */);
        }
    };


    private Animation.AnimationListener mToStartListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mBaseRefreshView.stop();
            mCurrentOffsetTop = mTarget.getTop();
        }
    };


    //    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        final int action = MotionEventCompat.getActionMasked(ev);
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                mActivePointerId = MotionEventCompat.getPointerId(ev,0);
//                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
//                mInitialMotionY = initialMotionY;
//                break;
//            case MotionEvent.ACTION_MOVE:
//
//                return true;
//        }
//        return false;
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        final int action = MotionEventCompat.getActionMasked(ev);
//        switch (action) {
//            case MotionEvent.ACTION_MOVE:
//                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
//                final float y = MotionEventCompat.getY(ev, pointerIndex);
//                final float yDiff = y - mInitialMotionY;
//                final float scrollTop = yDiff * DRAG_RATE;
//                mCurrentDragPercent = scrollTop / mTotalDragDistance;
//                if (mCurrentDragPercent < 0) {
//                    return false;
//                }
//                float boundedDragPercent = Math.min(1f, Math.abs(mCurrentDragPercent));
//                float extraOS = Math.abs(scrollTop) - mTotalDragDistance;
//                float slingshotDist = mTotalDragDistance;
//                float tensionSlingshotPercent = Math.max(0,
//                        Math.min(extraOS, slingshotDist * 2) / slingshotDist);
//                float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
//                        (tensionSlingshotPercent / 4), 2)) * 2f;
//                float extraMove = (slingshotDist) * tensionPercent / 2;
//                int targetY = (int) ((slingshotDist * boundedDragPercent) + extraMove);
//
////                mBaseRefreshView.setPercent(mCurrentDragPercent, true);
//                setTargetOffsetTop(targetY - mCurrentOffsetTop, true);
//
//                break;
//
//        }
//        return super.onTouchEvent(ev);
//    }

    /**
     * 移动target
     * @param offset
     * @param requiresUpdate
     */
    private void setTargetOffsetTop(int offset, boolean requiresUpdate) {
        mTarget.offsetTopAndBottom(offset);
        mBaseRefreshView.offsetTopAndBottom(offset);
        mCurrentOffsetTop = mTarget.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ensureTarget();
        if (mTarget == null)
            return;

        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();

        mTarget.layout(left, top + mCurrentOffsetTop, left + width - right, top + height - bottom + mCurrentOffsetTop);
        mRefreshView.layout(left, top, left + width - right, top + height - bottom);
    }

    public int getTotalDragDistance() {
        return mTotalDragDistance;
    }

}
