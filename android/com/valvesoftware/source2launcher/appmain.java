package com.valvesoftware.source2launcher;

import android.app.Application;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager.LayoutParams;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.Resources;
import org.libsdl.app.SDLActivity;

public class appmain extends SDLActivity {
    private boolean m_bFastCleanup = false;

    private static native void enterHibernationNative();

    private static native void exitHibernationNative();

    private static native void onDestroyNative();

    /* access modifiers changed from: protected */
    public void onLaunchLauncherActivity() {
        Class<appmain> cls;
        Application application = JNI_Environment.m_application;
        StringBuilder sb = new StringBuilder();
        sb.append(application.getPackageName());
        sb.append(".applauncher");
        try {
            cls = Class.forName(sb.toString(), false, application.getClassLoader());
        } catch (Throwable unused) {
            cls = null;
        }
        if (cls == null) {
            cls = appmain.class;
        }
        startActivity(new Intent(this, cls));
        finish();
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        this.m_bFastCleanup = false;
        if (!((application) JNI_Environment.m_application).HasRunLauncher()) {
            this.m_bFastCleanup = true;
            onLaunchLauncherActivity();
            try {
                super.onCreate(bundle);
            } catch (Throwable unused) {
            }
        } else {
            super.onCreate(bundle);
        }
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        if (VERSION.SDK_INT >= 28) {
            Display defaultDisplay = getWindowManager().getDefaultDisplay();
            Point point = new Point();
            defaultDisplay.getSize(point);
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new SafeAreaListener(Math.max(point.x, point.y), Math.min(point.x, point.y)));
            LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = 1;
            getWindow().setAttributes(attributes);
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        if (this.m_bFastCleanup) {
            mBrokenLibraries = true;
            super.onDestroy();
            mBrokenLibraries = false;
            return;
        }
        super.onDestroy();
        onDestroyNative();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        Log.v("com.valvesoftware.source2launcher.appmain", "onPause()");
        enterHibernationNative();
        super.onPause();
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        Log.v("com.valvesoftware.source2launcher.appmain", "onResume()");
        exitHibernationNative();
        super.onResume();
    }

    /* access modifiers changed from: protected */
    public String[] getLibraries() {
        return new String[0];
    }

    /* access modifiers changed from: protected */
    public String getMainSharedObject() {
        String GetString = Resources.GetString("LauncherBinaryName");
        StringBuilder sb = new StringBuilder();
        sb.append("lib");
        sb.append(GetString);
        sb.append(".so");
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public String[] getArguments() {
        return JNI_Environment.sm_ProgramArguments;
    }
}
