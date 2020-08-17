package com.valvesoftware;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;
import com.valvesoftware.BootStrapClient;
import com.valvesoftware.IStreamingBootStrap;
import java.net.URLDecoder;
import java.util.LinkedHashMap;

public abstract class Application extends android.app.Application {
    private static final String k_sSpewPackageName = "com.valvesoftware.Application";
    private static PowerManager.WakeLock m_DebugWakeLock;
    private static Application sm_instance;
    /* access modifiers changed from: private */
    public InstallTask m_RunningInstallTask;
    /* access modifiers changed from: private */
    public int m_nInstallStatus;
    String m_strCmdLineAccessCode = null;
    String m_strCmdLineAuthority = null;
    public IStreamingBootStrap m_streamingBootStrapConnection;

    public static class SteamLoginInfo_t {
        public String accessCode;
        public String authority;
    }

    private static native void onLowMemoryNative();

    private static native void onTrimMemoryNative(int i);

    public boolean ConsumePurchase(String str) {
        return false;
    }

    public InstallTask GetInstallTask(Activity activity, boolean z) {
        return null;
    }

    public boolean PurchaseSku(String str) {
        return false;
    }

    public boolean QueryExistingPurchases() {
        return false;
    }

    public boolean QuerySkuDetailsAsync(String str) {
        return false;
    }

    public static Application GetInstance() {
        return sm_instance;
    }

    public static void ForceQuit(int i) {
        Activity.KillAllActivities();
        Process.killProcess(Process.myPid());
        System.exit(i);
    }

    public String[] GetNativeBinarySearchPaths(String str) {
        return new String[]{"game:/bin/" + str};
    }

    public String[] GetProgramArguments() {
        String str;
        String str2 = this.m_strCmdLineAuthority;
        if (str2 == null || (str = this.m_strCmdLineAccessCode) == null) {
            return null;
        }
        return new String[]{"-steamlogin_authority", str2, "-steamlogin_accesscode", str};
    }

    public void onCreate() {
        String str;
        BootStrapClient.NativeLibraryPathResolver nativeLibraryPathResolver;
        sm_instance = this;
        super.onCreate();
        Activity.RegisterActivityListener(this);
        Resources.Initialize(this);
        String packageName = getPackageName();
        Log.i(packageName, "Launching Hardware info:");
        Log.i(packageName, "\tPRODUCT: \"" + Build.PRODUCT + "\"");
        Log.i(packageName, "\tBRAND: \"" + Build.BRAND + "\" MANUFACTURER: \"" + Build.MANUFACTURER + "\"");
        Log.i(packageName, "\tMODEL: \"" + Build.MODEL + "\" DEVICE: \"" + Build.DEVICE + "\" BOARD: \"" + Build.BOARD + "\"");
        Log.i(packageName, "\tDISPLAY: \"" + Build.DISPLAY + "\" FINGERPRINT: \"" + Build.FINGERPRINT + "\" HARDWARE: \"" + Build.HARDWARE + "\"");
        boolean[] GetBoolean = Resources.GetBoolean("BinariesOmittedFromAPK");
        boolean z = GetBoolean != null && GetBoolean[0];
        String[] GetSupportedABIs = JNI_Environment.GetSupportedABIs();
        String str2 = "";
        String str3 = GetSupportedABIs.length > 0 ? GetSupportedABIs[0] : str2;
        for (int i = 1; i < GetSupportedABIs.length; i++) {
            str3 = str3 + ";" + GetSupportedABIs[i];
        }
        Log.i(packageName, "\t\tcpu: \"" + System.getProperty("os.arch") + "\", ABI's: \"" + str3 + "\", omitting: " + z);
        String GetString = Resources.GetString("TargetPlatformName");
        if (GetString != null) {
            Log.i(packageName, "APK targets vpc platform \"" + GetString + "\"");
        }
        IStreamingBootStrap iStreamingBootStrap = null;
        if (ShouldConnectToStreamingBootStrap()) {
            try {
                BootStrapClient.ConnectionResult connectToDevPC = BootStrapClient.connectToDevPC(500, 5000);
                if (connectToDevPC != null) {
                    this.m_streamingBootStrapConnection = connectToDevPC.connection;
                    IStreamingBootStrap.StaticHelpers.SetPrimaryJavaConnection(this.m_streamingBootStrapConnection);
                    String GetString2 = Resources.GetString("BRANCH_ID");
                    if (GetString2 != null) {
                        this.m_streamingBootStrapConnection.SetAttributeValue("BRANCH_ID", 0, GetString2, (IStreamingBootStrap.IResponseHandler_Attribute) null);
                    }
                    String GetString3 = Resources.GetString("VPC_SRCDIR");
                    if (GetString3 != null) {
                        this.m_streamingBootStrapConnection.SetAttributeValue("VPC_SRCDIR", 0, GetString3, (IStreamingBootStrap.IResponseHandler_Attribute) null);
                    }
                }
            } catch (Throwable unused) {
            }
        }
        IStreamingBootStrap iStreamingBootStrap2 = this.m_streamingBootStrapConnection;
        if (iStreamingBootStrap2 != null) {
            iStreamingBootStrap2.SetAttributeValue("PLATFORM", 0, "android", (IStreamingBootStrap.IResponseHandler_Attribute) null);
            this.m_streamingBootStrapConnection.SetAttributeValue("CPU_ARCH", 0, System.getProperty("os.arch"), (IStreamingBootStrap.IResponseHandler_Attribute) null);
            this.m_streamingBootStrapConnection.SetAttributeValue("SUPPORTED_ABIS", 0, str3, (IStreamingBootStrap.IResponseHandler_Attribute) null);
            str = BootStrapClient.GetAttributeValue_Wait(this.m_streamingBootStrapConnection, "PC_IDENTIFIER");
        } else {
            str = null;
        }
        JNI_Environment.onApplicationCreate(this, str);
        if (z) {
            if (z) {
                iStreamingBootStrap = this.m_streamingBootStrapConnection;
            }
            nativeLibraryPathResolver = new BootStrapClient.NativeLibraryPathResolver(iStreamingBootStrap, JNI_Environment.GetPrivatePath());
            if (GetString != null) {
                nativeLibraryPathResolver.AddVariableReplacement("$(PLATFORM_ARCH)", GetString);
                String[] GetNativeBinarySearchPaths = GetNativeBinarySearchPaths("$(PLATFORM_ARCH)");
                if (GetNativeBinarySearchPaths != null) {
                    for (String AddNativeLibrarySearchPath : GetNativeBinarySearchPaths) {
                        JNI_Environment.AddNativeLibrarySearchPath(AddNativeLibrarySearchPath);
                    }
                }
            }
        } else {
            nativeLibraryPathResolver = null;
        }
        if (nativeLibraryPathResolver != null) {
            JNI_Environment.SetPathResolver(nativeLibraryPathResolver);
        }
        JNI_Environment.setup();
        if (GetString == null) {
            String str4 = JNI_Environment.m_sVPCPlatformName;
            if (str4 != null) {
                str2 = "via jni native binary loading";
            } else {
                int i2 = 0;
                while (str4 == null && i2 < Build.SUPPORTED_ABIS.length) {
                    str4 = JNI_Environment.GetVPCPlatformForABI(Build.SUPPORTED_ABIS[i2]);
                    i2++;
                }
                if (str4 != null) {
                    str2 = "via supported abi iteration";
                }
            }
            if (str4 != null) {
                Log.i(packageName, "Universal APK detected vpc platform \"" + str4 + "\" " + str2);
                if (nativeLibraryPathResolver != null) {
                    nativeLibraryPathResolver.AddVariableReplacement("$(PLATFORM_ARCH)", str4);
                    String[] GetNativeBinarySearchPaths2 = GetNativeBinarySearchPaths("$(PLATFORM_ARCH)");
                    if (GetNativeBinarySearchPaths2 != null) {
                        for (String AddNativeLibrarySearchPath2 : GetNativeBinarySearchPaths2) {
                            JNI_Environment.AddNativeLibrarySearchPath(AddNativeLibrarySearchPath2);
                        }
                        return;
                    }
                    return;
                }
                return;
            }
            Log.i(packageName, "Universal APK could not determine the platform");
        }
    }

    /* access modifiers changed from: package-private */
    public void PauseInstall() {
        InstallTask installTask = this.m_RunningInstallTask;
        if (installTask != null) {
            installTask.CancelPendingUIUpdates();
        }
    }

    /* access modifiers changed from: package-private */
    public void ResumeInstall() {
        InstallTask installTask = this.m_RunningInstallTask;
        if (installTask != null) {
            installTask.QueueImmediateUIUpdate();
        }
    }

    public boolean ShouldConnectToStreamingBootStrap() {
        boolean[] GetBoolean = Resources.GetBoolean("RETAIL");
        if (GetBoolean == null || !GetBoolean[0]) {
            return true;
        }
        return false;
    }

    public void onLowMemory() {
        try {
            onLowMemoryNative();
        } catch (Throwable unused) {
        }
        super.onLowMemory();
    }

    public void onTrimMemory(int i) {
        try {
            onTrimMemoryNative(i);
        } catch (Throwable unused) {
        }
        super.onTrimMemory(i);
    }

    public abstract class InstallTask extends LongUITask {
        /* access modifiers changed from: private */
        public Activity m_InstallActivity;
        private boolean m_bFinishInstallActivity;

        public Object BackgroundThread_Task() {
            return null;
        }

        public int UIThread_Update(Object obj) {
            return -1;
        }

        public InstallTask(Activity activity, boolean z) {
            this.m_InstallActivity = activity;
            this.m_bFinishInstallActivity = z;
        }

        public Activity GetInstallActivity() {
            return this.m_InstallActivity;
        }

        public void UIThread_onTaskFinished(Object obj) {
            if (Application.this.m_nInstallStatus == 1) {
                int unused = Application.this.m_nInstallStatus = 3;
            }
            InstallTask unused2 = Application.this.m_RunningInstallTask = null;
            if (this.m_bFinishInstallActivity && this.m_InstallActivity != null) {
                JNI_Environment.m_OSHandler.post(new Runnable() {
                    public void run() {
                        InstallTask.this.m_InstallActivity.finish();
                    }
                });
            }
        }
    }

    public void TryInstall(Activity activity, boolean z) {
        int i = this.m_nInstallStatus;
        if (i == 0 || i == 2) {
            this.m_nInstallStatus = 1;
            InstallTask GetInstallTask = GetInstallTask(activity, z);
            if (GetInstallTask != null) {
                this.m_RunningInstallTask = GetInstallTask;
                GetInstallTask.Start(true);
                return;
            }
            this.m_nInstallStatus = 3;
        }
    }

    public void SetInstallFailed() {
        this.m_nInstallStatus = 2;
    }

    public boolean HasInstallStarted() {
        return this.m_nInstallStatus > 0;
    }

    public boolean HasInstallFinished() {
        return this.m_nInstallStatus > 1;
    }

    public boolean HasInstallFailed() {
        return this.m_nInstallStatus == 2;
    }

    public boolean HasInstallSucceeded() {
        return this.m_nInstallStatus == 3;
    }

    public static void WakeForDebugging(Activity activity) {
        PowerManager powerManager;
        try {
            boolean[] GetBoolean = Resources.GetBoolean("RETAIL");
            if (GetBoolean == null || !GetBoolean[0]) {
                if (m_DebugWakeLock == null && (powerManager = (PowerManager) activity.getSystemService("power")) != null) {
                    Log.i(k_sSpewPackageName, "WakeForDebugging() powerManager exists");
                    m_DebugWakeLock = powerManager.newWakeLock(805306374, "com.valvesoftware.Application.WakeForDebugging()");
                    m_DebugWakeLock.acquire();
                }
                if (Build.VERSION.SDK_INT >= 27) {
                    activity.setTurnScreenOn(true);
                }
                activity.getWindow().addFlags(2097280);
                KeyguardManager keyguardManager = (KeyguardManager) activity.getSystemService("keyguard");
                if (keyguardManager != null) {
                    Log.i(k_sSpewPackageName, "WakeForDebugging() keyguardManager exists");
                    keyguardManager.requestDismissKeyguard(activity, new KeyguardManager.KeyguardDismissCallback() {
                        public void onDismissCancelled() {
                            Log.i(Application.k_sSpewPackageName, "WakeForDebugging() keyguardManager.requestDismissKeyguard onDismissCancelled");
                        }

                        public void onDismissError() {
                            Log.i(Application.k_sSpewPackageName, "WakeForDebugging() keyguardManager.requestDismissKeyguard onDismissError");
                        }

                        public void onDismissSucceeded() {
                            Log.i(Application.k_sSpewPackageName, "WakeForDebugging() keyguardManager.requestDismissKeyguard onDismissSucceeded");
                        }
                    });
                }
            }
        } catch (Throwable th) {
            Log.i(k_sSpewPackageName, "WakeForDebugging() exception " + th.getMessage());
        }
    }

    public static SteamLoginInfo_t GetSteamLoginFromIntentUrl(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return null;
        }
        try {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (String str : data.getQuery().split("&")) {
                int indexOf = str.indexOf("=");
                linkedHashMap.put(URLDecoder.decode(str.substring(0, indexOf), "UTF-8"), URLDecoder.decode(str.substring(indexOf + 1), "UTF-8"));
            }
            SteamLoginInfo_t steamLoginInfo_t = new SteamLoginInfo_t();
            steamLoginInfo_t.authority = (String) linkedHashMap.get("authority");
            steamLoginInfo_t.accessCode = (String) linkedHashMap.get("access_code");
            Log.i("com.valvesoftware.Application.HandleSteamLogin", "authority: " + steamLoginInfo_t.authority + " access code: " + steamLoginInfo_t.accessCode);
            GetInstance().SetSteamLoginLaunchArgs(steamLoginInfo_t.authority, steamLoginInfo_t.accessCode);
            return steamLoginInfo_t;
        } catch (Throwable unused) {
            return null;
        }
    }

    public void SetSteamLoginLaunchArgs(String str, String str2) {
        this.m_strCmdLineAuthority = str;
        this.m_strCmdLineAccessCode = str2;
    }

    public static boolean isConnectedToWifi() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) JNI_Environment.m_application.getSystemService("connectivity")).getActiveNetworkInfo();
        if (activeNetworkInfo == null || activeNetworkInfo.getType() != 1) {
            return false;
        }
        return true;
    }
}
