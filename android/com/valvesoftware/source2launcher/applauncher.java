package com.valvesoftware.source2launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.valvesoftware.Activity;
import com.valvesoftware.Application;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.application;

public class applauncher extends Activity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final String k_sSpewPackageName = "com.valvesoftware.source2launcher.applauncher";
    private static Context s_context;
    private boolean m_bWriteAccess = false;

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
        if (Build.VERSION.SDK_INT < 24 || Build.VERSION.SDK_INT > 25 || application != null) {
            Log.i("com.valvesoftware.applauncher", "Checking permissions");
            if (Build.VERSION.SDK_INT >= 23) {
                this.m_bWriteAccess = checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == 0;
            } else {
                this.m_bWriteAccess = true;
            }
            if (this.m_bWriteAccess) {
                application.SetPermissionsState(application.EPermissionsState.EHavePermissions);
            }
            application.EPermissionsState GetPermissionsState = application.GetPermissionsState();
            if (GetPermissionsState == application.EPermissionsState.EHavePermissions) {
                if (!application.HasInstallStarted()) {
                    Log.i("com.valvesoftware.applauncher", "We have read/write access");
                    StartInstallProcess();
                }
            } else if (GetPermissionsState == application.EPermissionsState.ENeedPermissions) {
                Log.i("com.valvesoftware.applauncher", "Showing permissions explanation to user");
                showPermissionExplanation(Resources.GetStringSafe("Native_PermissionsTitle"), Resources.GetStringSafe("Native_PermissionsText"), "android.permission.WRITE_EXTERNAL_STORAGE", 1);
                application.SetPermissionsState(application.EPermissionsState.ERequestedPermisions);
            } else {
                Log.i("com.valvesoftware.applauncher", "Requesting write access");
                showPermissionExitSettingsOption();
            }
        } else {
            forceRestart();
        }
    }

    /* access modifiers changed from: private */
    public void requestPermission(String str, int i) {
        requestPermissions(new String[]{str}, i);
    }

    private void showPermissionExplanation(String str, String str2, final String str3, final int i) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(str).setMessage(str2).setPositiveButton(17039370, new DialogInterface.OnClickListener() {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(GetStringSafe3).setMessage(GetStringSafe4).setNegativeButton(GetStringSafe, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                System.exit(0);
            }
        }).setPositiveButton(GetStringSafe2, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", applauncher.this.getPackageName(), (String) null));
                dialogInterface.dismiss();
                applauncher.this.startActivity(intent);
            }
        });
        builder.create().show();
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 1) {
            if (iArr.length <= 0 || iArr[0] != 0) {
                Log.i("com.valvesoftware.applauncher", "Write permission denied.");
                return;
            }
            Log.i("com.valvesoftware.applauncher", "Write permission granted");
            this.m_bWriteAccess = true;
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
