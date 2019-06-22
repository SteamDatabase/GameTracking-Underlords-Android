package com.valvesoftware.source2launcher;

import android.os.Build.VERSION;
import android.view.WindowManager.LayoutParams;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.Resources;
import org.libsdl.app.SDLActivity;

public class appmain extends SDLActivity {
    private static native void onDestroyNative();

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        if (VERSION.SDK_INT >= 28) {
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new SafeAreaListener());
            LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = 1;
            getWindow().setAttributes(attributes);
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        onDestroyNative();
    }

    /* access modifiers changed from: protected */
    public String[] getLibraries() {
        return new String[0];
    }

    /* access modifiers changed from: protected */
    public String getMainSharedObject() {
        String GetString = Resources.GetString("VPC_LauncherBinaryName");
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
