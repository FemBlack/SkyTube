package free.rm.skytube.gui.dialog;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

public abstract class BottomTopBaseDialog<T extends BottomTopBaseDialog<T>> extends BaseDialog<T> {
    protected View mAnimateView;
    protected int mLeft, mTop, mRight, mBottom;

    public BottomTopBaseDialog(Context context) {
        super(context);
    }

    /** set duration for inner com.flyco.animation of mAnimateView(设置animateView内置动画时长) */
    public T innerAnimDuration(long innerAnimDuration) {
        return (T) this;
    }

    public T padding(int left, int top, int right, int bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        return (T) this;
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}