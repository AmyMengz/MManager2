package com.donson.xx.mmlmanager.widget;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

/**
 * Created by Administrator on 2017/4/27.
 */

public abstract class BaseRefreshView extends Drawable implements Drawable.Callback, Animatable {
    private PullToRefreshView mRefreshLayout;
    private boolean mEndOfRefreshing;

    BaseRefreshView(Context context, PullToRefreshView layout) {
        mRefreshLayout = layout;
    }

    public Context getContext() {
        return mRefreshLayout != null ? mRefreshLayout.getContext() : null;
    }

    public PullToRefreshView getRefreshLayout() {
        return mRefreshLayout;
    }

    public abstract void setPercent(float percent, boolean invalidate);

    public abstract void offsetTopAndBottom(int offset);

    @Override
    public void invalidateDrawable(Drawable who) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }
    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        final Callback callback = getCallback();
        if(callback !=null){
            callback.scheduleDrawable(this,what,when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }

    }
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {

    }
    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    public void setEndOfRefreshing(boolean endOfRefreshing) {
        this.mEndOfRefreshing = endOfRefreshing;
    }
}
