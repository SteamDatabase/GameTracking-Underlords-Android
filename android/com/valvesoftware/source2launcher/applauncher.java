package com.valvesoftware.source2launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.valvesoftware.Activity;
import com.valvesoftware.Application;

public class applauncher extends Activity {
    private static final String k_sSpewPackageName = "com.valvesoftware.source2launcher.applauncher";
    private static Context s_context;

    protected static native void queueSteamLoginWithAccessCode(String str, String str2);

    public boolean IsLaunchActivity() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent != null && intent.hasCategory("android.intent.category.LAUNCHER") && action != null && action.equals("android.intent.action.MAIN")) {
                Log.i("com.valvesoftware.applauncher", "Not task root, finish the activity.");
                finish();
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(6);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
        }
        Application.WakeForDebugging(this);
        s_context = this;
        application GetInstance = application.GetInstance();
        if (GetInstance != null) {
            GetInstance.setupCommonUI(this);
        }
        getWindow().addFlags(128);
        HandleSteamLogin();
        application application = (application) Application.GetInstance();
        if (application.HasInstallFinished()) {
            application.LaunchMainActivity(true, this);
            finish();
        }
    }

    public void onResume() {
        super.onResume();
        application application = (application) Application.GetInstance();
        if (Build.VERSION.SDK_INT >= 24 && Build.VERSION.SDK_INT <= 25 && application == null) {
            forceRestart();
        } else if (!application.HasInstallStarted()) {
            Log.i("com.valvesoftware.applauncher", "We have read/write access");
            StartInstallProcess();
        }
    }

    /* access modifiers changed from: protected */
    public void StartInstallProcess() {
        Log.i("com.valvesoftware.StartInstallProcess", "Attempting install");
        Application.GetInstance().TryInstall(this, true);
    }

    private static void forceRestart() {
        Log.i("com.valvesoftware.applauncher", "Forcing restart.");
        Context context = s_context;
        if (context == null) {
            Log.i("com.valvesoftware.applauncher", "null context, unable to force restart.");
            return;
        }
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(s_context.getPackageName());
            if (launchIntentForPackage != null) {
                launchIntentForPackage.addFlags(268435456);
                Log.i("com.valvesoftware.applauncher", "Queue activity restart.");
                s_context.startActivity(launchIntentForPackage);
                Context context2 = s_context;
                if (context2 instanceof android.app.Activity) {
                    ((android.app.Activity) context2).finish();
                }
                Log.i("com.valvesoftware.applauncher", "Exit process.");
                Runtime.getRuntime().exit(0);
                return;
            }
            Log.e("com.valvesoftware.applauncher", "Could not getLaunchIntentForPackage().");
            return;
        }
        Log.e("com.valvesoftware.applauncher", "Could not getPackageManager().");
    }

    private void HandleSteamLogin() {
        Application.SteamLoginInfo_t GetSteamLoginFromIntentUrl = Application.GetSteamLoginFromIntentUrl(getIntent());
        if (GetSteamLoginFromIntentUrl != null && Application.GetInstance().HasInstallFinished()) {
            queueSteamLoginWithAccessCode(GetSteamLoginFromIntentUrl.authority, GetSteamLoginFromIntentUrl.accessCode);
        }
    }
}
