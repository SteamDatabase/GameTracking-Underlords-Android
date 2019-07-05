package com.valvesoftware.source2launcher;

import android.util.Log;
import com.valvesoftware.Application;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem.EErrorCode;
import com.valvesoftware.PatchSystem.EState;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask.TaskStatus;
import java.util.Locale;

public class application extends Application {
    IContentSyncAsyncTask m_ContentSyncAsyncTask = null;
    private boolean m_bTriedBootStrap = false;
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

    public application() {
        String str = "es";
        String str2 = "zh";
        String str3 = "pt";
        this.m_languageMap = new LanguageCountryMap[]{new LanguageCountryMap("en", "US", "english"), new LanguageCountryMap("de", "DE", "german"), new LanguageCountryMap("fr", "FR", "french"), new LanguageCountryMap("it", "IT", "italian"), new LanguageCountryMap("ko", "KR", "koreana"), new LanguageCountryMap(str, "ES", "spanish"), new LanguageCountryMap(str2, "CN", "schinese"), new LanguageCountryMap(str2, "TW", "tchinese"), new LanguageCountryMap("ru", "RU", "russian"), new LanguageCountryMap("th", "TH", "thai"), new LanguageCountryMap("ja", "JP", "japanese"), new LanguageCountryMap(str3, "PT", "portuguese"), new LanguageCountryMap("pl", "PL", "polish"), new LanguageCountryMap("da", "DK", "danish"), new LanguageCountryMap("nl", "NL", "dutch"), new LanguageCountryMap("fi", "FI", "finnish"), new LanguageCountryMap("no", "NO", "norwegian"), new LanguageCountryMap("sv", "SE", "swedish"), new LanguageCountryMap("hu", "HU", "hungarian"), new LanguageCountryMap("cs", "CZ", "czech"), new LanguageCountryMap("ro", "RO", "romanian"), new LanguageCountryMap("tr", "TR", "turkish"), new LanguageCountryMap(str3, "BR", "brazilian"), new LanguageCountryMap("bg", "BG", "bulgarian"), new LanguageCountryMap("el", "GR", "greek"), new LanguageCountryMap("uk", "UA", "ukrainian"), new LanguageCountryMap(str, "MX", "latam"), new LanguageCountryMap("vi", "VN", "vietnamese")};
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

    public String[] GetNativeBinarySearchPaths(String str) {
        String GetString = Resources.GetString("VPC_GameName");
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
        String GetString = Resources.GetString("VPC_LauncherBinaryName");
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String GetEngineLanguage = GetEngineLanguage(language, country);
        String str = "-launchersublanguage";
        String str2 = "-launcherlanguage";
        String str3 = ".txt";
        String str4 = "@mobile/commandlines/android/source2launcher_";
        if (this.m_strCmdLineAuthority == null || this.m_strCmdLineAccessCode == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(str4);
            sb.append(GetString);
            sb.append(str3);
            return new String[]{sb.toString(), str2, GetEngineLanguage, str, country};
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str4);
        sb2.append(GetString);
        sb2.append(str3);
        return new String[]{sb2.toString(), str2, GetEngineLanguage, str, country, "-steamlogin_authority", this.m_strCmdLineAuthority, "-steamlogin_accesscode", this.m_strCmdLineAccessCode};
    }

    public boolean InstallFiles(IStreamingBootStrap iStreamingBootStrap) {
        String str;
        boolean[] GetBoolean = Resources.GetBoolean("VPC_PatchSystemEnabled");
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

    /* access modifiers changed from: protected */
    public int GetEffectiveApplicationVersion() {
        String GetString = Resources.GetString("VPC_VersionCodeString");
        if (GetString != null) {
            return Integer.parseInt(GetString) % 1000000;
        }
        return 1000001;
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
        String GetString = Resources.GetString("VPC_LauncherBinaryName");
        JNI_Environment.FindAndLoadNativeLibrary("libSDL2.so");
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
}
