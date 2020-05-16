package com.valvesoftware.source2launcher;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import com.valvesoftware.Application;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.InAppPurchases;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class application extends Application implements Application.ActivityLifecycleCallbacks {
    IContentSyncAsyncTask m_ContentSyncAsyncTask = null;
    private boolean m_bHasRunLauncher = false;
    private boolean m_bTriedBootStrap = false;
    private boolean m_bUseVulkan = false;
    private Activity m_currentActivity = null;
    private InAppPurchases m_inAppPurchases = null;
    LanguageCountryMap[] m_languageMap = {new LanguageCountryMap("en", "US", "english"), new LanguageCountryMap("de", "DE", "german"), new LanguageCountryMap("fr", "FR", "french"), new LanguageCountryMap("it", "IT", "italian"), new LanguageCountryMap("ko", "KR", "koreana"), new LanguageCountryMap("es", "ES", "spanish"), new LanguageCountryMap("zh", "CN", "schinese"), new LanguageCountryMap("zh", "TW", "tchinese"), new LanguageCountryMap("zh", "HK", "tchinese"), new LanguageCountryMap("ru", "RU", "russian"), new LanguageCountryMap("th", "TH", "thai"), new LanguageCountryMap("ja", "JP", "japanese"), new LanguageCountryMap("pt", "PT", "portuguese"), new LanguageCountryMap("pl", "PL", "polish"), new LanguageCountryMap("da", "DK", "danish"), new LanguageCountryMap("nl", "NL", "dutch"), new LanguageCountryMap("fi", "FI", "finnish"), new LanguageCountryMap("no", "NO", "norwegian"), new LanguageCountryMap("sv", "SE", "swedish"), new LanguageCountryMap("hu", "HU", "hungarian"), new LanguageCountryMap("cs", "CZ", "czech"), new LanguageCountryMap("ro", "RO", "romanian"), new LanguageCountryMap("tr", "TR", "turkish"), new LanguageCountryMap("pt", "BR", "brazilian"), new LanguageCountryMap("bg", "BG", "bulgarian"), new LanguageCountryMap("el", "GR", "greek"), new LanguageCountryMap("uk", "UA", "ukrainian"), new LanguageCountryMap("es", "MX", "latam"), new LanguageCountryMap("vi", "VN", "vietnamese")};
    private EPermissionsState m_nPermissionState = EPermissionsState.ENeedPermissions;
    String m_strCmdLineAccessCode = null;
    String m_strCmdLineAuthority = null;

    public enum EPermissionsState {
        ENeedPermissions,
        ERequestedPermisions,
        EHavePermissions
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

    /* access modifiers changed from: package-private */
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
        this.m_inAppPurchases.connectToBillingClient((Runnable) null);
    }

    public String[] GetNativeBinarySearchPaths(String str) {
        String GetString = Resources.GetString("GameName");
        return new String[]{"game:/bin/" + str, "game:/" + GetString + "/bin/" + str};
    }

    public void SetSteamLoginLaunchArgs(String str, String str2) {
        this.m_strCmdLineAuthority = str;
        this.m_strCmdLineAccessCode = str2;
    }

    public String[] GetProgramArguments() {
        String str;
        String GetString = Resources.GetString("LauncherBinaryName");
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String GetEngineLanguage = GetEngineLanguage(language, country);
        String[] strArr = {"@mobile/commandlines/android/source2launcher_" + GetString + ".txt"};
        String[] strArr2 = {"-launcherlanguage", GetEngineLanguage, "-launchersublanguage", country};
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(strArr));
        arrayList.addAll(Arrays.asList(strArr2));
        String str2 = this.m_strCmdLineAuthority;
        if (!(str2 == null || (str = this.m_strCmdLineAccessCode) == null)) {
            arrayList.addAll(Arrays.asList(new String[]{"-steamlogin_authority", str2, "-steamlogin_accesscode", str}));
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
            return true;
        } catch (Throwable unused) {
            this.m_ContentSyncAsyncTask.updateProgress(PatchSystem.EState.Error, PatchSystem.EErrorCode.Unknown, 0);
            return true;
        }
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

    public IContentSyncAsyncTask.TaskStatus GetBootStrapStatus() {
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
        JNI_Environment.FindAndLoadNativeLibrary("lib" + GetString + ".so");
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
        this.m_inAppPurchases.querySkuDetails(arrayList, (Runnable) null);
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
