package com.valvesoftware.source2launcher;

import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

/* compiled from: appmain */
class SafeAreaListener implements View.OnApplyWindowInsetsListener {
    private int mScreenHeight = 0;
    private int mScreenWidth = 0;

    private static native void setSafeAreaInsets(int i, int i2, int i3, int i4);

    public SafeAreaListener(int i, int i2) {
        this.mScreenWidth = i;
        this.mScreenHeight = i2;
    }

    public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
        int i;
        int i2;
        int i3;
        DisplayCutout displayCutout;
        int i4 = 0;
        if (windowInsets == null || (displayCutout = windowInsets.getDisplayCutout()) == null) {
            i2 = 0;
            i = 0;
        } else {
            i4 = Math.max(displayCutout.getSafeInsetLeft(), displayCutout.getSafeInsetRight());
            i = displayCutout.getSafeInsetBottom();
            i2 = displayCutout.getSafeInsetTop();
        }
        int i5 = this.mScreenWidth;
        if (i5 > 0 && (i3 = this.mScreenHeight) > 0 && ((float) i5) / ((float) i3) > 1.8888888f && i5 >= 1440) {
            i4 = Math.max(i4, 128);
        }
        setSafeAreaInsets(i, i4, i4, i2);
        return windowInsets;
    }
}
