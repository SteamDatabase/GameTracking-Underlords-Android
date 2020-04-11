package com.valvesoftware.source2launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask.TaskStatus;
import com.valvesoftware.source2launcher.application.EPermissionsState;

public class applauncher extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static Context s_context;
    private final String k_sSpewPackageName = "com.valvesoftware.source2launcher.applauncher";
    private boolean m_bMadeAPIChoice = false;
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
            applauncher.this.m_timerHandler.postDelayed(this, 30);
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
        try {
            boolean[] GetBoolean = Resources.GetBoolean("RETAIL");
            if (GetBoolean == null || !GetBoolean[0]) {
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService("keyguard");
                if (keyguardManager != null) {
                    super.setTurnScreenOn(true);
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            }
        } catch (Throwable unused) {
        }
        super.onCreate(bundle);
        s_context = this;
    }

    public void onResume() {
        super.onResume();
        application application = (application) JNI_Environment.m_application;
        if (VERSION.SDK_INT < 24 || VERSION.SDK_INT > 25 || application != null) {
            String str = "com.valvesoftware.applauncher";
            Log.i(str, "Checking permissions");
            String str2 = "android.permission.WRITE_EXTERNAL_STORAGE";
            if (VERSION.SDK_INT >= 23) {
                this.m_bWriteAccess = checkSelfPermission(str2) == 0;
            } else {
                this.m_bWriteAccess = true;
            }
            if (this.m_bWriteAccess) {
                application.SetPermissionsState(EPermissionsState.EHavePermissions);
            }
            EPermissionsState GetPermissionsState = application.GetPermissionsState();
            if (GetPermissionsState == EPermissionsState.EHavePermissions) {
                if (!application.TriedBootStrap()) {
                    application.SetTriedBootStrap(true);
                    Log.i(str, "We have read/write access");
                    bootStrapIntoGame();
                } else {
                    this.m_timerHandler.postDelayed(this.m_timerRunnable, 1000);
                }
            } else if (GetPermissionsState == EPermissionsState.ENeedPermissions) {
                Log.i(str, "Showing permissions explanation to user");
                showPermissionExplanation(Resources.GetStringSafe("Native_PermissionsTitle"), Resources.GetStringSafe("Native_PermissionsText"), str2, 1);
                application.SetPermissionsState(EPermissionsState.ERequestedPermisions);
            } else {
                Log.i(str, "Requesting write access");
                showPermissionExitSettingsOption();
            }
            return;
        }
        forceRestart();
    }

    public void onStop() {
        this.m_timerHandler.removeCallbacks(this.m_timerRunnable);
        super.onStop();
    }

    /* access modifiers changed from: private */
    public void requestPermission(String str, int i) {
        requestPermissions(new String[]{str}, i);
    }

    private void showPermissionExplanation(String str, String str2, final String str3, final int i) {
        Builder builder = new Builder(this);
        builder.setTitle(str).setMessage(str2).setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                applauncher.this.requestPermission(str3, i);
            }
        });
        AlertDialog create = builder.create();
        create.setCanceledOnTouchOutside(false);
        create.show();
    }

    private void showPermissionExitSettingsOption() {
        Log.i("com.valvesoftware.applauncher", "showing exit/settings option dialog");
        String GetStringSafe = Resources.GetStringSafe("Native_Exit");
        String GetStringSafe2 = Resources.GetStringSafe("Native_Settings");
        String GetStringSafe3 = Resources.GetStringSafe("Native_NeedsStoragePermissionsToContinueTitle");
        String GetStringSafe4 = Resources.GetStringSafe("Native_NeedsStoragePermissionsToContinueText");
        Builder builder = new Builder(this);
        builder.setTitle(GetStringSafe3).setMessage(GetStringSafe4).setNegativeButton(GetStringSafe, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        }).setPositiveButton(GetStringSafe2, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", applauncher.this.getPackageName(), null));
                dialogInterface.dismiss();
                applauncher.this.startActivity(intent);
            }
        });
        builder.create().show();
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1) {
            String str = "com.valvesoftware.applauncher";
            if (iArr.length <= 0 || iArr[0] != 0) {
                Log.i(str, "Write permission denied.");
                return;
            }
            Log.i(str, "Write permission granted");
            this.m_bWriteAccess = true;
        }
    }

    /* access modifiers changed from: protected */
    public void bootStrapIntoGame() {
        if (!this.m_bMadeAPIChoice && VERSION.SDK_INT >= 24) {
            boolean[] GetBoolean = Resources.GetBoolean("PatchSystemEnabled");
            boolean z = false;
            if (GetBoolean != null && GetBoolean[0]) {
                z = true;
            }
            if (!z) {
                ChooseRenderingAPI();
                this.m_bMadeAPIChoice = true;
                return;
            }
        }
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

    /* access modifiers changed from: protected */
    public void ChooseRenderingAPI() {
        boolean[] GetBoolean = Resources.GetBoolean("Graphics_UseVulkan");
        if (GetBoolean != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Graphics choice supplied by vpc resource Graphics_UseVulkan=");
            sb.append(GetBoolean[0]);
            Log.i("com.valvesoftware.source2launcher.applauncher", sb.toString());
            ((application) JNI_Environment.m_application).SetUseVulkan(GetBoolean[0]);
            this.m_bMadeAPIChoice = true;
            bootStrapIntoGame();
            return;
        }
        Builder builder = new Builder(this);
        String str = "Vulkan";
        builder.setTitle("Rendering API").setMessage("Please choose a rendering API").setNegativeButton("OpenGL ES", new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                ((application) JNI_Environment.m_application).SetUseVulkan(false);
                applauncher.this.bootStrapIntoGame();
            }
        }).setPositiveButton(str, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                ((application) JNI_Environment.m_application).SetUseVulkan(true);
                applauncher.this.bootStrapIntoGame();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }
}
