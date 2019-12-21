package com.valvesoftware.source2launcher;

import android.view.View.OnApplyWindowInsetsListener;

/* compiled from: appmain */
class SafeAreaListener implements OnApplyWindowInsetsListener {
    private int mScreenHeight = 0;
    private int mScreenWidth = 0;

    private static native void setSafeAreaInsets(int i, int i2, int i3, int i4);

    public SafeAreaListener(int i, int i2) {
        this.mScreenWidth = i;
        this.mScreenHeight = i2;
    }

    /* JADX WARNING: Removed duplicated region for block: B:8:0x0024  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.view.WindowInsets onApplyWindowInsets(android.view.View r6, android.view.WindowInsets r7) {
        /*
            r5 = this;
            r6 = 0
            if (r7 == 0) goto L_0x001e
            android.view.DisplayCutout r0 = r7.getDisplayCutout()
            if (r0 == 0) goto L_0x001e
            int r6 = r0.getSafeInsetLeft()
            int r1 = r0.getSafeInsetRight()
            int r6 = java.lang.Math.max(r6, r1)
            int r1 = r0.getSafeInsetBottom()
            int r0 = r0.getSafeInsetTop()
            goto L_0x0020
        L_0x001e:
            r0 = 0
            r1 = 0
        L_0x0020:
            int r2 = r5.mScreenWidth
            if (r2 <= 0) goto L_0x003c
            int r3 = r5.mScreenHeight
            if (r3 <= 0) goto L_0x003c
            float r4 = (float) r2
            float r3 = (float) r3
            float r4 = r4 / r3
            r3 = 1072809756(0x3ff1c71c, float:1.8888888)
            int r3 = (r4 > r3 ? 1 : (r4 == r3 ? 0 : -1))
            if (r3 <= 0) goto L_0x003c
            r3 = 1440(0x5a0, float:2.018E-42)
            if (r2 < r3) goto L_0x003c
            r2 = 128(0x80, float:1.794E-43)
            int r6 = java.lang.Math.max(r6, r2)
        L_0x003c:
            setSafeAreaInsets(r1, r6, r6, r0)
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.source2launcher.SafeAreaListener.onApplyWindowInsets(android.view.View, android.view.WindowInsets):android.view.WindowInsets");
    }
}
