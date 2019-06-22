package com.valvesoftware.source2launcher;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask.TaskStatus;

public class applauncher extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static Context s_context;
    private boolean m_bWriteAccess = false;
    Handler m_timerHandler = new Handler();
    Runnable m_timerRunnable = new Runnable() {
        public boolean m_bIsDone;

        public void run() {
            application application = (application) JNI_Environment.m_application;
            applauncher.this.setInstallStatus(application.GetBootStrapStatus());
            if (application.IsDoneBootStrapping()) {
                applauncher.this.m_timerHandler.removeCallbacks(applauncher.this.m_timerRunnable);
                applauncher.this.onBootStrapFinished();
                return;
            }
            applauncher.this.m_timerHandler.postDelayed(this, 500);
        }
    };

    protected static native void queueSteamLoginWithAccessCode(String str, String str2);

    /* access modifiers changed from: protected */
    public void setInstallStatus(TaskStatus taskStatus) {
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        if (!isTaskRoot()) {
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent != null && intent.hasCategory("android.intent.category.LAUNCHER") && action != null && action.equals("android.intent.action.MAIN")) {
                Log.i("com.valvesoftware.applauncher", "Not task root, finish the activity.");
                finish();
            }
        }
        super.onCreate(bundle);
        s_context = this;
    }

    public void onResume() {
        super.onResume();
        application application = (application) JNI_Environment.m_application;
        if (!application.TriedBootStrap()) {
            application.SetTriedBootStrap(true);
            String str = "com.valvesoftware.applauncher";
            Log.i(str, "Checking permissions");
            String str2 = "android.permission.WRITE_EXTERNAL_STORAGE";
            if (VERSION.SDK_INT >= 23) {
                this.m_bWriteAccess = checkSelfPermission(str2) == 0;
            } else {
                this.m_bWriteAccess = true;
            }
            boolean z = this.m_bWriteAccess;
            if (z) {
                Log.i(str, "We have read/write access");
                bootStrapIntoGame();
            } else if (!z) {
                Log.i(str, "Requesting write access");
                String GetStringSafe = Resources.GetStringSafe("Native_PermissionsTitle");
                String GetStringSafe2 = Resources.GetStringSafe("Native_PermissionsText");
                Log.i(str, "Showing explanation to user");
                showExplanation(GetStringSafe, GetStringSafe2, str2, 1);
            }
        } else {
            this.m_timerHandler.postDelayed(this.m_timerRunnable, 1000);
        }
    }

    public void onStop() {
        this.m_timerHandler.removeCallbacks(this.m_timerRunnable);
        super.onStop();
    }

    /* access modifiers changed from: private */
    public void requestPermission(String str, int i) {
        requestPermissions(new String[]{str}, i);
    }

    private void showExplanation(String str, String str2, final String str3, final int i) {
        Builder builder = new Builder(this);
        builder.setTitle(str).setMessage(str2).setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                applauncher.this.requestPermission(str3, i);
            }
        });
        builder.create().show();
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1) {
            String str = "com.valvesoftware.applauncher";
            if (iArr.length <= 0 || iArr[0] != 0) {
                Log.i(str, "Write permission denied. Stuck here forever.");
            } else {
                Log.i(str, "Write permission granted");
                this.m_bWriteAccess = true;
            }
        }
        if (this.m_bWriteAccess) {
            bootStrapIntoGame();
        }
    }

    /* access modifiers changed from: protected */
    public void bootStrapIntoGame() {
        Log.i("com.valvesoftware.SelfInstall", "Bootstrapping");
        this.m_timerHandler.postDelayed(this.m_timerRunnable, 1000);
        ((application) JNI_Environment.m_application).onBootStrap();
    }

    /* access modifiers changed from: protected */
    public void onBootStrapFinished() {
        ((application) JNI_Environment.m_application).onBootStrapFinished();
    }

    /* access modifiers changed from: protected */
    public boolean isConnectedToWifi() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) ((application) JNI_Environment.m_application).getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetworkInfo == null || activeNetworkInfo.getType() != 1) {
            return false;
        }
        return true;
    }

    private static void forceRestart() {
        String str = "com.valvesoftware.applauncher";
        Log.i(str, "Forcing restart.");
        Context context = s_context;
        if (context == null) {
            Log.i(str, "null context, unable to force restart.");
            return;
        }
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(s_context.getPackageName());
            if (launchIntentForPackage != null) {
                launchIntentForPackage.addFlags(268435456);
                Log.i(str, "Queue activity restart.");
                s_context.startActivity(launchIntentForPackage);
                Context context2 = s_context;
                if (context2 instanceof Activity) {
                    ((Activity) context2).finish();
                }
                Log.i(str, "Exit process.");
                Runtime.getRuntime().exit(0);
            } else {
                Log.e(str, "Could not getLaunchIntentForPackage().");
            }
        } else {
            Log.e(str, "Could not getPackageManager().");
        }
    }
}
