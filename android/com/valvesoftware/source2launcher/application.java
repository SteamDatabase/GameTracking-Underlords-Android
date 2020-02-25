package com.valvesoftware.source2launcher;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.util.Log;
import com.valvesoftware.Application;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.InAppPurchases;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.PatchSystem.EErrorCode;
import com.valvesoftware.PatchSystem.EState;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask.TaskStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class application extends Application implements ActivityLifecycleCallbacks {
    IContentSyncAsyncTask m_ContentSyncAsyncTask = null;
    private boolean m_bHasRunLauncher = false;
    private boolean m_bTriedBootStrap = false;
    private boolean m_bUseVulkan = false;
    private Activity m_currentActivity = null;
    private InAppPurchases m_inAppPurchases = null;
    LanguageCountryMap[] m_languageMap;
    private EPermissionsState m_nPermissionState = EPermissionsState.ENeedPermissions;
    String m_strCmdLineAccessCode = null;
    String m_strCmdLineAuthority = null;

    public enum EPermissionsState {
        ENeedPermissions,
        ERequestedPermisions,
        EHavePermissions
    }

    public class LanguageCountryMap {
        String m_country;
        String m_lang;
        String m_value;

        public LanguageCountryMap(String str, String str2, String str3) {
            this.m_lang = str;
            this.m_country = str2;
            this.m_value = str3;
        }
    }

    /* access modifiers changed from: protected */
    public String GetManifestURL() {
        return null;
    }

    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    public void onActivityDestroyed(Activity activity) {
    }

    public void onActivityPaused(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    public void onActivityStarted(Activity activity) {
    }

    public void onActivityStopped(Activity activity) {
    }

    public application() {
        String str = "es";
        String str2 = "zh";
        String str3 = "tchinese";
        String str4 = "pt";
        this.m_languageMap = new LanguageCountryMap[]{new LanguageCountryMap("en", "US", "english"), new LanguageCountryMap("de", "DE", "german"), new LanguageCountryMap("fr", "FR", "french"), new LanguageCountryMap("it", "IT", "italian"), new LanguageCountryMap("ko", "KR", "koreana"), new LanguageCountryMap(str, "ES", "spanish"), new LanguageCountryMap(str2, "CN", "schinese"), new LanguageCountryMap(str2, "TW", str3), new LanguageCountryMap(str2, "HK", str3), new LanguageCountryMap("ru", "RU", "russian"), new LanguageCountryMap("th", "TH", "thai"), new LanguageCountryMap("ja", "JP", "japanese"), new LanguageCountryMap(str4, "PT", "portuguese"), new LanguageCountryMap("pl", "PL", "polish"), new LanguageCountryMap("da", "DK", "danish"), new LanguageCountryMap("nl", "NL", "dutch"), new LanguageCountryMap("fi", "FI", "finnish"), new LanguageCountryMap("no", "NO", "norwegian"), new LanguageCountryMap("sv", "SE", "swedish"), new LanguageCountryMap("hu", "HU", "hungarian"), new LanguageCountryMap("cs", "CZ", "czech"), new LanguageCountryMap("ro", "RO", "romanian"), new LanguageCountryMap("tr", "TR", "turkish"), new LanguageCountryMap(str4, "BR", "brazilian"), new LanguageCountryMap("bg", "BG", "bulgarian"), new LanguageCountryMap("el", "GR", "greek"), new LanguageCountryMap("uk", "UA", "ukrainian"), new LanguageCountryMap(str, "MX", "latam"), new LanguageCountryMap("vi", "VN", "vietnamese")};
    }

    /* access modifiers changed from: 0000 */
    public String GetEngineLanguage(String str, String str2) {
        int i = 0;
        int i2 = 0;
        while (true) {
            LanguageCountryMap[] languageCountryMapArr = this.m_languageMap;
            if (i2 >= languageCountryMapArr.length) {
                while (true) {
                    LanguageCountryMap[] languageCountryMapArr2 = this.m_languageMap;
                    if (i >= languageCountryMapArr2.length) {
                        return "none";
                    }
                    if (languageCountryMapArr2[i].m_lang == str) {
                        return this.m_languageMap[i].m_value;
                    }
                    i++;
                }
            } else if (languageCountryMapArr[i2].m_lang == str && this.m_languageMap[i2].m_country == str2) {
                return this.m_languageMap[i2].m_value;
            } else {
                i2++;
            }
        }
    }

    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        this.m_inAppPurchases = new InAppPurchases(JNI_Environment.m_application.getApplicationContext());
        this.m_inAppPurchases.connectToBillingClient(null);
    }

    public String[] GetNativeBinarySearchPaths(String str) {
        String GetString = Resources.GetString("GameName");
        StringBuilder sb = new StringBuilder();
        sb.append("game:/bin/");
        sb.append(str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("game:/");
        sb2.append(GetString);
        sb2.append("/bin/");
        sb2.append(str);
        return new String[]{sb.toString(), sb2.toString()};
    }

    public void SetSteamLoginLaunchArgs(String str, String str2) {
        this.m_strCmdLineAuthority = str;
        this.m_strCmdLineAccessCode = str2;
    }

    public String[] GetProgramArguments() {
        String GetString = Resources.GetString("LauncherBinaryName");
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String GetEngineLanguage = GetEngineLanguage(language, country);
        StringBuilder sb = new StringBuilder();
        sb.append("@mobile/commandlines/android/source2launcher_");
        sb.append(GetString);
        sb.append(".txt");
        String[] strArr = {sb.toString()};
        String[] strArr2 = {"-launcherlanguage", GetEngineLanguage, "-launchersublanguage", country};
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(strArr));
        arrayList.addAll(Arrays.asList(strArr2));
        String str = this.m_strCmdLineAuthority;
        if (str != null) {
            String str2 = this.m_strCmdLineAccessCode;
            if (str2 != null) {
                arrayList.addAll(Arrays.asList(new String[]{"-steamlogin_authority", str, "-steamlogin_accesscode", str2}));
            }
        }
        if (PatchSystem.IsSelfInstallAPKEnabled()) {
            arrayList.addAll(Arrays.asList(new String[]{"-sideloadedapk"}));
        }
        if (this.m_bUseVulkan) {
            arrayList.addAll(Arrays.asList(new String[]{"-vulkan"}));
        }
        String[] strArr3 = new String[arrayList.size()];
        arrayList.toArray(strArr3);
        return strArr3;
    }

    public boolean InstallFiles(IStreamingBootStrap iStreamingBootStrap) {
        String str;
        boolean[] GetBoolean = Resources.GetBoolean("PatchSystemEnabled");
        if (GetBoolean == null || !GetBoolean[0]) {
            str = null;
        } else {
            str = GetManifestURL();
            if (str == null) {
                Log.e("com.valvesoftware.source2launcher.application", "Patch System enabled but no Manifest URL constructed!");
            }
        }
        if (str != null) {
            PatchSystemContentSyncAsyncTask patchSystemContentSyncAsyncTask = new PatchSystemContentSyncAsyncTask();
            patchSystemContentSyncAsyncTask.Initialize(str, GetEffectiveApplicationVersion());
            this.m_ContentSyncAsyncTask = patchSystemContentSyncAsyncTask;
        } else {
            this.m_ContentSyncAsyncTask = new BootStrapClientContentSyncAsyncTask(iStreamingBootStrap);
        }
        try {
            this.m_ContentSyncAsyncTask.execute(new Void[0]);
        } catch (Throwable unused) {
            this.m_ContentSyncAsyncTask.updateProgress(EState.Error, EErrorCode.Unknown, 0);
        }
        return true;
    }

    public void SetUseVulkan(boolean z) {
        this.m_bUseVulkan = z;
    }

    /* access modifiers changed from: protected */
    public int GetEffectiveApplicationVersion() {
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() <= 0) {
            return 1000001;
        }
        return Integer.parseInt(GetString) % 1000000;
    }

    public TaskStatus GetBootStrapStatus() {
        return this.m_ContentSyncAsyncTask.GetStatus();
    }

    public boolean IsDoneBootStrapping() {
        IContentSyncAsyncTask iContentSyncAsyncTask = this.m_ContentSyncAsyncTask;
        if (iContentSyncAsyncTask == null) {
            return false;
        }
        return iContentSyncAsyncTask.IsDone();
    }

    public void onBootStrapFinished() {
        this.m_ContentSyncAsyncTask.cancel(false);
        String GetString = Resources.GetString("LauncherBinaryName");
        StringBuilder sb = new StringBuilder();
        sb.append("lib");
        sb.append(GetString);
        sb.append(".so");
        JNI_Environment.FindAndLoadNativeLibrary(sb.toString());
        JNI_Environment.FindAndLoadNativeLibrary("libengine2.so");
    }

    public boolean TriedBootStrap() {
        return this.m_bTriedBootStrap;
    }

    public void SetTriedBootStrap(boolean z) {
        this.m_bTriedBootStrap = z;
    }

    public EPermissionsState GetPermissionsState() {
        return this.m_nPermissionState;
    }

    public void SetPermissionsState(EPermissionsState ePermissionsState) {
        this.m_nPermissionState = ePermissionsState;
    }

    public void SetHasRunLauncher(boolean z) {
        this.m_bHasRunLauncher = z;
    }

    public boolean HasRunLauncher() {
        return this.m_bHasRunLauncher;
    }

    public boolean QuerySkuDetailsAsync(String str) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(str);
        this.m_inAppPurchases.querySkuDetails(arrayList, null);
        return true;
    }

    public boolean QueryExistingPurchases() {
        this.m_inAppPurchases.queryExistingPurchases();
        return true;
    }

    public boolean PurchaseSku(String str) {
        this.m_inAppPurchases.purchaseSku(this.m_currentActivity, str);
        return false;
    }

    public boolean ConsumePurchase(String str) {
        this.m_inAppPurchases.consumePurchase(str);
        return true;
    }

    public void onActivityResumed(Activity activity) {
        this.m_currentActivity = activity;
    }
}
