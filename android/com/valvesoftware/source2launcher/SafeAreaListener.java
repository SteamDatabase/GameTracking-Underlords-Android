package com.valvesoftware.source2launcher;

import android.view.DisplayCutout;
import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;

/* compiled from: appmain */
class SafeAreaListener implements OnApplyWindowInsetsListener {
    private static native void setSafeAreaInsets(int i, int i2, int i3, int i4);

    SafeAreaListener() {
    }

    public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
        if (windowInsets != null) {
            DisplayCutout displayCutout = windowInsets.getDisplayCutout();
            if (displayCutout != null) {
                setSafeAreaInsets(displayCutout.getSafeInsetBottom(), displayCutout.getSafeInsetLeft(), displayCutout.getSafeInsetRight(), displayCutout.getSafeInsetTop());
            }
        }
        return windowInsets;
    }
}
