package ind.riaz.iap;


import android.view.View;

public class ViewUtils {

	public static void setVisible(View view, boolean visible) {
		if (view != null) {
			setVisible(view, visible, View.GONE);
		}
        
    }

    public static void setVisible(View view, boolean visible, int invisibleFlag) {
    	if (view != null) {
    		int visibility = view.getVisibility();
            int visibilityNew = visible ? View.VISIBLE : invisibleFlag;

            if (visibility != visibilityNew) {
                view.setVisibility(visibilityNew);
            }
    	}
        
    }
}
