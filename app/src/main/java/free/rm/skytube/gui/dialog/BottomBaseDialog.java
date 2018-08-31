package free.rm.skytube.gui.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

public abstract class BottomBaseDialog<T extends BottomBaseDialog<T>> extends BottomTopBaseDialog<T> {
    public BottomBaseDialog(Context context, View animateView) {
        super(context);
    }

    public BottomBaseDialog(Context context) {
        this(context, null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLlTop.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mLlTop.setGravity(Gravity.BOTTOM);
        getWindow().setGravity(Gravity.BOTTOM);
        mLlTop.setPadding(mLeft, mTop, mRight, mBottom);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
