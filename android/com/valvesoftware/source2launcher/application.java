package com.valvesoftware.source2launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import com.valvesoftware.Application;
import com.valvesoftware.BootStrapClient;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.InAppPurchases;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.LongUITask;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.Resources;
import com.valvesoftware.SelfInstall;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class application extends Application {
    protected static final int DEFAULT_FONT_SIZE = 18;
    protected static final int SMALLER_FONT_SIZE = 14;
    private static final String k_sSpewPackageName = "com.valvesoftware.source2launcher.application";
    private static application m_instance;
    protected Typeface m_Font = null;
    protected ImageView m_Logo = null;
    Random m_Random = new Random();
    protected Point m_ScreenSize = new Point(1, 1);
    private boolean m_bUseVulkan = false;
    private InAppPurchases m_inAppPurchases = null;
    LanguageCountryMap[] m_languageMap = {new LanguageCountryMap("en", "US", "english"), new LanguageCountryMap("de", "DE", "german"), new LanguageCountryMap("fr", "FR", "french"), new LanguageCountryMap("it", "IT", "italian"), new LanguageCountryMap("ko", "KR", "koreana"), new LanguageCountryMap("es", "ES", "spanish"), new LanguageCountryMap("zh", "CN", "schinese"), new LanguageCountryMap("zh", "TW", "tchinese"), new LanguageCountryMap("zh", "HK", "tchinese"), new LanguageCountryMap("ru", "RU", "russian"), new LanguageCountryMap("th", "TH", "thai"), new LanguageCountryMap("ja", "JP", "japanese"), new LanguageCountryMap("pt", "PT", "portuguese"), new LanguageCountryMap("pl", "PL", "polish"), new LanguageCountryMap("da", "DK", "danish"), new LanguageCountryMap("nl", "NL", "dutch"), new LanguageCountryMap("fi", "FI", "finnish"), new LanguageCountryMap("no", "NO", "norwegian"), new LanguageCountryMap("sv", "SE", "swedish"), new LanguageCountryMap("hu", "HU", "hungarian"), new LanguageCountryMap("cs", "CZ", "czech"), new LanguageCountryMap("ro", "RO", "romanian"), new LanguageCountryMap("tr", "TR", "turkish"), new LanguageCountryMap("pt", "BR", "brazilian"), new LanguageCountryMap("bg", "BG", "bulgarian"), new LanguageCountryMap("el", "GR", "greek"), new LanguageCountryMap("uk", "UA", "ukrainian"), new LanguageCountryMap("es", "MX", "latam"), new LanguageCountryMap("vi", "VN", "vietnamese")};
    protected int m_nLoadingBarFillWidth = 1;
    private EPermissionsState m_nPermissionState = EPermissionsState.ENeedPermissions;
    protected ImageView m_progressBarBg = null;
    protected ImageView m_progressBarFill = null;
    protected TextView m_progressPctLabel = null;

    public enum EPermissionsState {
        ENeedPermissions,
        ERequestedPermisions,
        EHavePermissions
    }

    /* access modifiers changed from: protected */
    public String GetManifestBaseURL(boolean z) {
        return null;
    }

    public static application GetInstance() {
        return m_instance;
    }

    public boolean UseBetaSteamUniverse() {
        boolean[] GetBoolean = Resources.GetBoolean("BRANCH_MAIN");
        if (GetBoolean == null || !GetBoolean[0]) {
            return false;
        }
        return true;
    }

    public boolean UseIntranetManifestURL() {
        boolean[] GetBoolean = Resources.GetBoolean("BRANCH_MAIN");
        if (GetBoolean == null || !GetBoolean[0]) {
            return false;
        }
        return true;
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
        m_instance = this;
        super.onCreate();
        this.m_inAppPurchases = new InAppPurchases(getApplicationContext());
        this.m_inAppPurchases.connectToBillingClient((Runnable) null);
    }

    public void LaunchMainActivity(boolean z, Context context) {
        if (!z) {
            Class cls = null;
            try {
                cls = Class.forName(getPackageName() + ".appmain", false, getClassLoader());
            } catch (Throwable unused) {
            }
            if (cls == null) {
                cls = appmain.class;
            }
            Intent intent = new Intent(context, cls);
            if (z) {
                intent.setFlags(131072);
            }
            context.startActivity(intent);
        }
    }

    public String[] GetNativeBinarySearchPaths(String str) {
        String GetString = Resources.GetString("GameName");
        return new String[]{"game:/bin/" + str, "game:/" + GetString + "/bin/" + str};
    }

    public String[] GetProgramArguments() {
        String[] GetProgramArguments = super.GetProgramArguments();
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
        if (GetProgramArguments != null) {
            arrayList.addAll(Arrays.asList(GetProgramArguments));
        }
        if (PatchSystem.IsSelfInstallAPKEnabled()) {
            arrayList.addAll(Arrays.asList(new String[]{"-sideloadedapk"}));
        }
        if (this.m_bUseVulkan) {
            arrayList.addAll(Arrays.asList(new String[]{"-vulkan"}));
        }
        if (UseBetaSteamUniverse()) {
            arrayList.add("-init_universe");
            arrayList.add("beta");
        }
        String[] strArr3 = new String[arrayList.size()];
        arrayList.toArray(strArr3);
        return strArr3;
    }

    /* access modifiers changed from: private */
    public boolean SyncContent(InstallTask installTask) {
        InstallTask installTask2 = installTask;
        String GetString = Resources.GetString("GameName");
        boolean z = JNI_Environment.GetAvailableStorageBytes(JNI_Environment.GetPublicPath()) < 0;
        String absolutePath = JNI_Environment.GetPublicPath().getAbsolutePath();
        String[] strArr = {"so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter = new BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, new String[]{"win32", "win64", "linuxsteamrt64", "osx32", "osx64"});
        IStreamingBootStrap iStreamingBootStrap = this.m_streamingBootStrapConnection;
        StringBuilder sb = new StringBuilder();
        sb.append(absolutePath);
        sb.append("/game/bin");
        boolean z2 = BootStrapClient.DownloadDirectory(iStreamingBootStrap, "game:/bin", sb.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter);
        if (z) {
            strArr = new String[]{"vsnd_c", "so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        }
        BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2 = new BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, (String[]) null);
        float f = z ? 7.0f : 13.0f;
        installTask2.SetProgressObject(new Float(0.0f));
        IStreamingBootStrap iStreamingBootStrap2 = this.m_streamingBootStrapConnection;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(absolutePath);
        sb2.append("/game/core");
        BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3 = recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2;
        boolean z3 = BootStrapClient.DownloadDirectory(iStreamingBootStrap2, "game:/core", sb2.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z2;
        installTask2.SetProgressObject(new Float(1.0f / f));
        IStreamingBootStrap iStreamingBootStrap3 = this.m_streamingBootStrapConnection;
        StringBuilder sb3 = new StringBuilder();
        sb3.append(absolutePath);
        sb3.append("/game/mobile/core");
        boolean z4 = BootStrapClient.DownloadDirectory(iStreamingBootStrap3, "game:/mobile/core", sb3.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z3;
        installTask2.SetProgressObject(new Float(2.0f / f));
        IStreamingBootStrap iStreamingBootStrap4 = this.m_streamingBootStrapConnection;
        StringBuilder sb4 = new StringBuilder();
        sb4.append(absolutePath);
        sb4.append("/game/mobile/commandlines");
        boolean z5 = BootStrapClient.DownloadDirectory(iStreamingBootStrap4, "game:/mobile/commandlines", sb4.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z4;
        installTask2.SetProgressObject(new Float(3.0f / f));
        IStreamingBootStrap iStreamingBootStrap5 = this.m_streamingBootStrapConnection;
        String str = "game:/" + GetString;
        StringBuilder sb5 = new StringBuilder();
        sb5.append(absolutePath);
        sb5.append("/game/");
        sb5.append(GetString);
        String str2 = "/game/";
        String str3 = "game:/";
        boolean z6 = BootStrapClient.DownloadDirectory(iStreamingBootStrap5, str, sb5.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z5;
        installTask2.SetProgressObject(new Float(4.0f / f));
        IStreamingBootStrap iStreamingBootStrap6 = this.m_streamingBootStrapConnection;
        String str4 = str3 + GetString + "_addons";
        StringBuilder sb6 = new StringBuilder();
        sb6.append(absolutePath);
        sb6.append(str2);
        sb6.append(GetString);
        sb6.append("_addons");
        boolean z7 = BootStrapClient.DownloadDirectory(iStreamingBootStrap6, str4, sb6.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z6;
        installTask2.SetProgressObject(new Float(5.0f / f));
        String str5 = "/game/mobile/";
        String str6 = "game:/mobile/";
        boolean z8 = BootStrapClient.DownloadDirectory(this.m_streamingBootStrapConnection, "game:/mobile/" + GetString, absolutePath + "/game/mobile/" + GetString, true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z7;
        installTask2.SetProgressObject(new Float(6.0f / f));
        IStreamingBootStrap iStreamingBootStrap7 = this.m_streamingBootStrapConnection;
        String str7 = str6 + GetString + "_addons";
        StringBuilder sb7 = new StringBuilder();
        sb7.append(absolutePath);
        sb7.append(str5);
        sb7.append(GetString);
        sb7.append("_addons");
        boolean z9 = BootStrapClient.DownloadDirectory(iStreamingBootStrap7, str7, sb7.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z8;
        installTask2.SetProgressObject(new Float(7.0f / f));
        if (!z) {
            IStreamingBootStrap iStreamingBootStrap8 = this.m_streamingBootStrapConnection;
            StringBuilder sb8 = new StringBuilder();
            sb8.append(absolutePath);
            sb8.append("/game_otherplatforms/etc/core");
            boolean z10 = BootStrapClient.DownloadDirectory(iStreamingBootStrap8, "game:../game_otherplatforms/etc/core", sb8.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z9;
            installTask2.SetProgressObject(new Float(8.0f / f));
            IStreamingBootStrap iStreamingBootStrap9 = this.m_streamingBootStrapConnection;
            String str8 = "game:../game_otherplatforms/etc/" + GetString;
            StringBuilder sb9 = new StringBuilder();
            sb9.append(absolutePath);
            sb9.append("/game_otherplatforms/etc/");
            sb9.append(GetString);
            boolean z11 = BootStrapClient.DownloadDirectory(iStreamingBootStrap9, str8, sb9.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z10;
            installTask2.SetProgressObject(new Float(9.0f / f));
            IStreamingBootStrap iStreamingBootStrap10 = this.m_streamingBootStrapConnection;
            String str9 = "game:../game_otherplatforms/etc/mobile/" + GetString;
            StringBuilder sb10 = new StringBuilder();
            sb10.append(absolutePath);
            sb10.append("/game_otherplatforms/etc/mobile/");
            sb10.append(GetString);
            boolean z12 = BootStrapClient.DownloadDirectory(iStreamingBootStrap10, str9, sb10.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z11;
            installTask2.SetProgressObject(new Float(10.0f / f));
            IStreamingBootStrap iStreamingBootStrap11 = this.m_streamingBootStrapConnection;
            StringBuilder sb11 = new StringBuilder();
            sb11.append(absolutePath);
            sb11.append("/game_otherplatforms/low_bitrate/core");
            boolean z13 = BootStrapClient.DownloadDirectory(iStreamingBootStrap11, "game:../game_otherplatforms/low_bitrate/core", sb11.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z12;
            installTask2.SetProgressObject(new Float(11.0f / f));
            IStreamingBootStrap iStreamingBootStrap12 = this.m_streamingBootStrapConnection;
            String str10 = "game:../game_otherplatforms/low_bitrate/" + GetString;
            StringBuilder sb12 = new StringBuilder();
            sb12.append(absolutePath);
            sb12.append("/game_otherplatforms/low_bitrate/");
            sb12.append(GetString);
            boolean z14 = BootStrapClient.DownloadDirectory(iStreamingBootStrap12, str10, sb12.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z13;
            installTask2.SetProgressObject(new Float(12.0f / f));
            IStreamingBootStrap iStreamingBootStrap13 = this.m_streamingBootStrapConnection;
            String str11 = "game:../game_otherplatforms/low_bitrate/mobile/" + GetString;
            StringBuilder sb13 = new StringBuilder();
            sb13.append(absolutePath);
            sb13.append("/game_otherplatforms/low_bitrate/mobile/");
            sb13.append(GetString);
            z9 = BootStrapClient.DownloadDirectory(iStreamingBootStrap13, str11, sb13.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter3) && z14;
            installTask2.SetProgressObject(new Float(13.0f / f));
        }
        installTask2.SetProgressObject(new Float(1.0f));
        return z9;
    }

    public class InstallTask extends Application.InstallTask {
        public InstallTask(Activity activity, boolean z) {
            super(activity, z);
        }

        public void UIThread_onTaskFinished(Object obj) {
            super.UIThread_onTaskFinished(obj);
            String GetString = Resources.GetString("LauncherBinaryName");
            JNI_Environment.FindAndLoadNativeLibrary("lib" + GetString + ".so");
            JNI_Environment.FindAndLoadNativeLibrary("libengine2.so");
            application.this.LaunchMainActivity(false, GetInstallActivity());
        }
    }

    public class StreamingBootStrapInstallTask extends InstallTask {
        public StreamingBootStrapInstallTask(Activity activity, boolean z) {
            super(activity, z);
            SetProgressObject(new Float(0.0f));
        }

        public Object BackgroundThread_Task() {
            super.BackgroundThread_Task();
            application.this.ChooseRenderingAPI(this);
            BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                public void run() {
                    application.this.setupDownloadingScreen(StreamingBootStrapInstallTask.this.GetInstallActivity());
                }
            });
            boolean unused = application.this.SyncContent(this);
            return null;
        }

        public int UIThread_Update(Object obj) {
            super.UIThread_Update(obj);
            if (obj == null) {
                return 16;
            }
            application.this.setProgress(((Float) obj).floatValue());
            return 16;
        }
    }

    public class SelfInstallTask extends InstallTask {
        public SelfInstallTask(Activity activity, boolean z) {
            super(activity, z);
        }

        public Object BackgroundThread_Task() {
            super.BackgroundThread_Task();
            SelfInstall.OnStartup(this);
            return null;
        }
    }

    public class PatchSystemInstallTask extends InstallTask {
        private PatchSystem m_PatchSystem = new PatchSystem();
        private String m_strManifestURL;

        public PatchSystemInstallTask(String str, Activity activity, boolean z) {
            super(activity, z);
            this.m_strManifestURL = str;
        }

        public class PatchSystemCallbacks extends PatchSystem.PatchSystemCallbacks {
            private boolean m_bUpdateWasRequiredForOnline;

            public PatchSystemCallbacks() {
            }

            public void OnFatalError(final PatchSystem.EErrorCode eErrorCode) {
                PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean>() {
                    public void run() {
                        application.this.setupErrorScreen(PatchSystemInstallTask.this.GetInstallActivity(), eErrorCode, false, this);
                    }
                });
            }

            public boolean OnRecoverableError(final PatchSystem.EErrorCode eErrorCode) {
                AnonymousClass2 r0 = new LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean>() {
                    public void run() {
                        application.this.setupErrorScreen(PatchSystemInstallTask.this.GetInstallActivity(), eErrorCode, true, this);
                    }
                };
                PatchSystemInstallTask.this.BlockingUICall(r0);
                return ((Boolean) r0.m_Result).booleanValue();
            }

            public boolean ShouldUpdatedAPK(final boolean z, final long j) {
                this.m_bUpdateWasRequiredForOnline = z;
                AnonymousClass3 r0 = new LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean>() {
                    public void run() {
                        application.this.setupAPKOutOfDateScreen(PatchSystemInstallTask.this.GetInstallActivity(), z, j, this);
                    }
                };
                PatchSystemInstallTask.this.BlockingUICall(r0);
                return ((Boolean) r0.m_Result).booleanValue();
            }

            public void ExecuteAPKUpdate(final Uri uri) {
                PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                    public void run() {
                        application.this.installApk(PatchSystemInstallTask.this.GetInstallActivity(), uri);
                    }
                });
                while (ShouldUpdatedAPK(this.m_bUpdateWasRequiredForOnline, 0)) {
                    PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                        public void run() {
                            application.this.installApk(PatchSystemInstallTask.this.GetInstallActivity(), uri);
                        }
                    });
                }
            }

            public boolean ShouldDownloadManifestUpdate(final boolean z, final long j) {
                AnonymousClass6 r0 = new LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean>() {
                    public void run() {
                        application.this.setupManifestDownloadedScreen(PatchSystemInstallTask.this.GetInstallActivity(), z, j, this);
                    }
                };
                PatchSystemInstallTask.this.BlockingUICall(r0);
                return ((Boolean) r0.m_Result).booleanValue();
            }

            public void BlockingRunOnUIThread(final Runnable runnable) {
                PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                    public void run() {
                        runnable.run();
                    }
                });
            }

            public void OnContactingPatchServer() {
                PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                    public void run() {
                        application.this.setupContactingServerScreen(PatchSystemInstallTask.this.GetInstallActivity());
                    }
                });
            }

            public void OnStartDownloadingAPK() {
                SetupDownloadingScreen();
            }

            public void OnStartDownloadingContent() {
                SetupDownloadingScreen();
            }

            private void SetupDownloadingScreen() {
                PatchSystemInstallTask.this.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                    public void run() {
                        application.this.setupDownloadingScreen(PatchSystemInstallTask.this.GetInstallActivity());
                    }
                });
            }
        }

        public Object BackgroundThread_Task() {
            super.BackgroundThread_Task();
            application.this.ChooseRenderingAPI(this);
            this.m_PatchSystem.Start(this.m_strManifestURL, application.this.GetEffectiveApplicationVersion(), new PatchSystemCallbacks());
            return null;
        }

        public int UIThread_Update(Object obj) {
            super.UIThread_Update(obj);
            application.this.setProgress(this.m_PatchSystem.GetDownloadProgress());
            return 16;
        }
    }

    public Application.InstallTask GetInstallTask(Activity activity, boolean z) {
        boolean[] GetBoolean = Resources.GetBoolean("PatchSystemEnabled");
        if (GetBoolean != null && GetBoolean[0]) {
            String GetManifestURL = GetManifestURL();
            if (GetManifestURL != null) {
                return new PatchSystemInstallTask(GetManifestURL, activity, z);
            }
            Log.e(k_sSpewPackageName, "Patch System enabled but no Manifest URL constructed!");
        }
        if (SelfInstall.ShouldSyncContentFromBootstrap(this.m_streamingBootStrapConnection)) {
            return new StreamingBootStrapInstallTask(activity, z);
        }
        return new SelfInstallTask(activity, z);
    }

    private TextView createTextfield(String str) {
        TextView textView = new TextView(this);
        textView.setTypeface(this.m_Font);
        textView.setTextColor(-1);
        textView.setShadowLayer(0.06f, -2.0f, 2.0f, ViewCompat.MEASURED_STATE_MASK);
        textView.setTextSize(18.0f);
        textView.setText(str.toUpperCase());
        textView.setGravity(17);
        return textView;
    }

    public LinearLayout setupCommonUI(Activity activity) {
        return setupCommonUI(activity, (String) null, (String) null, false);
    }

    private LinearLayout setupCommonUI(Activity activity, String str, String str2, boolean z) {
        FrameLayout frameLayout;
        int i;
        Activity activity2 = activity;
        String str3 = str;
        String str4 = str2;
        if (this.m_Font == null) {
            this.m_Font = Typeface.create("sans-serif", 1);
            int[] GetFont = Resources.GetFont("radiance_bold");
            if (GetFont != null) {
                this.m_Font = ResourcesCompat.getFont(activity2, GetFont[0]);
            }
        }
        this.m_progressPctLabel = null;
        this.m_progressBarFill = null;
        this.m_progressBarBg = null;
        Activity GetNewestActivity = com.valvesoftware.Activity.GetNewestActivity();
        Display defaultDisplay = GetNewestActivity.getWindowManager().getDefaultDisplay();
        this.m_ScreenSize = new Point();
        defaultDisplay.getSize(this.m_ScreenSize);
        FrameLayout frameLayout2 = new FrameLayout(activity2);
        frameLayout2.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        frameLayout2.setBackgroundColor(Color.parseColor("#000000"));
        ImageView imageView = new ImageView(activity2);
        int[] GetDrawable = Resources.GetDrawable("launch_background");
        if (GetDrawable != null) {
            imageView.setImageResource(GetDrawable[0]);
        }
        imageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        this.m_Logo = new ImageView(this);
        int[] GetDrawable2 = Resources.GetDrawable("clean_logo");
        if (GetDrawable2 != null) {
            this.m_Logo.setImageResource(GetDrawable2[0]);
        }
        this.m_Logo.setLayoutParams(new FrameLayout.LayoutParams((int) (((float) this.m_ScreenSize.x) * 0.32f), (int) (((float) this.m_ScreenSize.y) * 0.35f), 51));
        LinearLayout linearLayout = new LinearLayout(activity2);
        linearLayout.setOrientation(1);
        int i2 = (int) (((float) this.m_ScreenSize.x) * 0.86f);
        int i3 = (int) (((float) this.m_ScreenSize.y) * 0.016f);
        float f = (float) i2;
        int i4 = (int) (f * 1.0f);
        int i5 = (int) (((float) i3) * 1.0f);
        this.m_nLoadingBarFillWidth = i4;
        Activity activity3 = GetNewestActivity;
        this.m_Logo.setPadding((int) (((float) this.m_ScreenSize.x) * 0.11f), 80, 20, 20);
        int i6 = (int) (((float) this.m_ScreenSize.y) * 0.18f);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(i2, i6, 81));
        if (z) {
            RelativeLayout relativeLayout = new RelativeLayout(activity2);
            relativeLayout.setLayoutParams(new FrameLayout.LayoutParams(i2, i3));
            ImageView imageView2 = new ImageView(activity2);
            int[] GetDrawable3 = Resources.GetDrawable("loadingbar_bg");
            if (GetDrawable3 != null) {
                imageView2.setImageResource(GetDrawable3[0]);
            }
            imageView2.setScaleType(ImageView.ScaleType.FIT_XY);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(i2, i3);
            frameLayout = frameLayout2;
            layoutParams.addRule(14);
            layoutParams.addRule(15);
            relativeLayout.addView(imageView2, layoutParams);
            ImageView imageView3 = new ImageView(activity2);
            int[] GetDrawable4 = Resources.GetDrawable("loadingbar_fill");
            if (GetDrawable4 != null) {
                i = 0;
                imageView3.setImageResource(GetDrawable4[0]);
            } else {
                i = 0;
            }
            imageView3.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView3.setPadding((i2 - i4) / 2, i, i, i);
            this.m_progressBarFill = imageView3;
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(i, i5);
            layoutParams2.addRule(15);
            relativeLayout.addView(imageView3, layoutParams2);
            linearLayout.addView(relativeLayout);
        } else {
            frameLayout = frameLayout2;
            i = 0;
        }
        LinearLayout linearLayout2 = new LinearLayout(activity2);
        linearLayout2.setOrientation(i);
        int i7 = i6 - i3;
        linearLayout2.setLayoutParams(new FrameLayout.LayoutParams(i2, i7, 49));
        LinearLayout linearLayout3 = new LinearLayout(activity2);
        linearLayout3.setOrientation(1);
        linearLayout3.setLayoutParams(new FrameLayout.LayoutParams((int) (0.75f * f), i7, 48));
        if (str3 != null) {
            TextView createTextfield = createTextfield(str3);
            createTextfield.setGravity(3);
            linearLayout3.addView(createTextfield);
        }
        if (str4 != null) {
            TextView createTextfield2 = createTextfield(str4);
            createTextfield2.setGravity(3);
            linearLayout3.addView(createTextfield2);
        }
        linearLayout2.addView(linearLayout3);
        if (z) {
            LinearLayout linearLayout4 = new LinearLayout(activity2);
            linearLayout4.setOrientation(1);
            linearLayout4.setLayoutParams(new FrameLayout.LayoutParams((int) (f * 0.25f), i7, 53));
            TextView createTextfield3 = createTextfield("");
            createTextfield3.setGravity(85);
            linearLayout4.addView(createTextfield3);
            this.m_progressPctLabel = createTextfield3;
            linearLayout2.addView(linearLayout4);
        }
        linearLayout.addView(linearLayout2);
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() == 0) {
            GetString = "DEBUG";
        }
        String GetStringSafe = Resources.GetStringSafe("Native_VersionLabel");
        TextView createTextfield4 = createTextfield(GetStringSafe + GetString);
        createTextfield4.setTextSize(14.0f);
        createTextfield4.setGravity(85);
        createTextfield4.setPadding(12, 12, 166, 12);
        FrameLayout frameLayout3 = frameLayout;
        frameLayout3.addView(imageView);
        frameLayout3.addView(this.m_Logo);
        frameLayout3.addView(linearLayout);
        frameLayout3.addView(createTextfield4);
        activity3.setContentView(frameLayout3);
        setProgress(0.0f);
        return linearLayout;
    }

    /* access modifiers changed from: private */
    public void setProgress(float f) {
        if (this.m_progressPctLabel != null && this.m_progressBarFill != null) {
            float max = Math.max(0.0f, Math.min(1.0f, f));
            TextView textView = this.m_progressPctLabel;
            textView.setText(Math.round(100.0f * max) + "%");
            this.m_progressPctLabel.requestLayout();
            this.m_progressBarFill.getLayoutParams().width = (int) (max * ((float) this.m_nLoadingBarFillWidth));
            this.m_progressBarFill.requestLayout();
        }
    }

    /* access modifiers changed from: private */
    public void setupContactingServerScreen(Activity activity) {
        setupCommonUI(activity, Resources.GetStringSafe("Native_ContactingServer"), (String) null, false);
    }

    /* access modifiers changed from: private */
    public void requestDownloadOverMobileData(final Activity activity, final LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        String GetStringSafe = Resources.GetStringSafe("Native_DownloadOverMobileLabel");
        String GetStringSafe2 = Resources.GetStringSafe("Native_DownloadOverMobileMessage");
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(GetStringSafe).setMessage(GetStringSafe2).setPositiveButton(17039379, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                application.this.setupPreparingToDownloadScreen(activity);
                blockingUICallRunnable_AsyncReturn.AsyncReturn(Boolean.TRUE);
            }
        }).setNegativeButton(17039369, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                blockingUICallRunnable_AsyncReturn.AsyncReturn(Boolean.FALSE);
            }
        });
        builder.create().show();
    }

    private void setupPlayPopup(final Activity activity, String str, String str2, final boolean z, boolean z2, long j, final LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (str != null) {
            builder.setTitle(str);
        }
        if (str2 != null) {
            builder.setMessage(str2);
        }
        if (!z && !z2) {
            builder.setPositiveButton(Resources.GetStringSafe("Native_OK"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    blockingUICallRunnable_AsyncReturn.AsyncReturn(null);
                }
            });
        }
        if (z) {
            String GetStringSafe = Resources.GetStringSafe("Native_DownloadUpdate");
            builder.setPositiveButton((GetStringSafe + " " + j + "mb").toUpperCase(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (Application.isConnectedToWifi()) {
                        application.this.setupPreparingToDownloadScreen(activity);
                        blockingUICallRunnable_AsyncReturn.AsyncReturn(Boolean.TRUE);
                        return;
                    }
                    application.this.requestDownloadOverMobileData(activity, blockingUICallRunnable_AsyncReturn);
                }
            });
        }
        if (z2) {
            builder.setNegativeButton(Resources.GetStringSafe("Native_PlayOffline").toUpperCase(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    blockingUICallRunnable_AsyncReturn.AsyncReturn(z ? Boolean.FALSE : Boolean.TRUE);
                }
            });
        }
        builder.setCancelable(false);
        try {
            builder.create().show();
        } catch (Throwable th) {
            Log.i(k_sSpewPackageName, "setupPlayPopup() encountered throwable " + th.getMessage());
            throw th;
        }
    }

    private void showAPKPopup(final Activity activity, boolean z, boolean z2, long j, final LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        String str;
        String str2;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (z2) {
            str = Resources.GetStringSafe("Native_AppOutOfDateReq");
        } else {
            str = Resources.GetStringSafe("Native_AppOutOfDateOpt");
        }
        builder.setTitle(str);
        if (z) {
            builder.setPositiveButton(Resources.GetStringSafe("Native_DownloadAppUpdate"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (Application.isConnectedToWifi()) {
                        application.this.setupPreparingToDownloadScreen(activity);
                        blockingUICallRunnable_AsyncReturn.AsyncReturn(Boolean.TRUE);
                        return;
                    }
                    application.this.requestDownloadOverMobileData(activity, blockingUICallRunnable_AsyncReturn);
                }
            });
        }
        if (z2) {
            str2 = Resources.GetStringSafe("Native_PlayAppOutOfDateReq");
        } else {
            str2 = Resources.GetStringSafe("Native_PlayAppOutOfDateOpt");
        }
        builder.setNegativeButton(str2, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                blockingUICallRunnable_AsyncReturn.AsyncReturn(Boolean.FALSE);
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    /* access modifiers changed from: private */
    public void setupAPKOutOfDateScreen(Activity activity, boolean z, long j, LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        setupCommonUI(activity);
        showAPKPopup(activity, true, z, j / 1048576, blockingUICallRunnable_AsyncReturn);
    }

    /* access modifiers changed from: private */
    public void installApk(Activity activity, Uri uri) {
        if (uri != null) {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setFlags(335544320);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            if (Build.VERSION.SDK_INT >= 24) {
                intent.addFlags(1);
            }
            startActivity(intent);
            return;
        }
        JNI_Environment.OpenURL("market://details?id=com.valvesoftware.underlords");
    }

    /* access modifiers changed from: private */
    public void setupManifestDownloadedScreen(Activity activity, boolean z, long j, LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        setupCommonUI(activity);
        setupPlayPopup(activity, Resources.GetStringSafe("Native_DownloadTitle"), (String) null, true, z, j / 1048576, blockingUICallRunnable_AsyncReturn);
    }

    /* access modifiers changed from: private */
    public void setupPreparingToDownloadScreen(Activity activity) {
        setupCommonUI(activity, Resources.GetStringSafe("Native_PreparingToDownload"), (String) null, true);
    }

    /* access modifiers changed from: private */
    public void setupDownloadingScreen(Activity activity) {
        setupCommonUI(activity, Resources.GetStringSafe("Native_DownloadingContent"), (String) null, true);
    }

    /* access modifiers changed from: private */
    public void setupErrorScreen(Activity activity, PatchSystem.EErrorCode eErrorCode, boolean z, LongUITask.BlockingUICallRunnable_AsyncReturn<Boolean> blockingUICallRunnable_AsyncReturn) {
        String GetStringSafe;
        String GetStringSafe2 = Resources.GetStringSafe("Native_DownloadError");
        int i = AnonymousClass9.$SwitchMap$com$valvesoftware$PatchSystem$EErrorCode[eErrorCode.ordinal()];
        if (i == 1) {
            GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorManifest");
        } else if (i == 2) {
            GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorDownload");
        } else if (i == 3) {
            GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorStorage");
        } else if (i != 4) {
            GetStringSafe = i != 5 ? null : Resources.GetStringSafe("Native_DownloadErrorUnknown");
        } else {
            GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorQueueDownload");
        }
        String str = GetStringSafe;
        setupCommonUI(activity);
        setupPlayPopup(activity, GetStringSafe2, str, false, z, 0, blockingUICallRunnable_AsyncReturn);
    }

    /* renamed from: com.valvesoftware.source2launcher.application$9  reason: invalid class name */
    static /* synthetic */ class AnonymousClass9 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode = new int[PatchSystem.EErrorCode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(12:0|1|2|3|4|5|6|7|8|9|10|12) */
        /* JADX WARNING: Code restructure failed: missing block: B:13:?, code lost:
            return;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
                com.valvesoftware.PatchSystem$EErrorCode[] r0 = com.valvesoftware.PatchSystem.EErrorCode.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode = r0
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Manifest     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x001f }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Download     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x002a }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Storage     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.QueueDownload     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Unknown     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r2 = 5
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.source2launcher.application.AnonymousClass9.<clinit>():void");
        }
    }

    public void SetUseVulkan(boolean z) {
        this.m_bUseVulkan = z;
    }

    /* access modifiers changed from: protected */
    public void ChooseRenderingAPI(Application.InstallTask installTask) {
        boolean z;
        this.m_bUseVulkan = false;
        if (Build.VERSION.SDK_INT >= 24) {
            boolean[] GetBoolean = Resources.GetBoolean("PatchSystemEnabled");
            if (!(GetBoolean != null && GetBoolean[0])) {
                boolean[] GetBoolean2 = Resources.GetBoolean("Graphics_UseVulkan");
                if (GetBoolean2 != null) {
                    Log.i(k_sSpewPackageName, "Graphics choice supplied by vpc resource Graphics_UseVulkan=" + GetBoolean2[0]);
                    GetInstance();
                    this.m_bUseVulkan = GetBoolean2[0];
                    return;
                }
                Semaphore semaphore = new Semaphore(0);
                boolean[] zArr = {false};
                AlertDialog[] alertDialogArr = {null};
                final Application.InstallTask installTask2 = installTask;
                final boolean[] zArr2 = zArr;
                final Semaphore semaphore2 = semaphore;
                final AlertDialog[] alertDialogArr2 = alertDialogArr;
                installTask.BlockingUICall(new LongUITask.BlockingUICallRunnable() {
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(installTask2.GetInstallActivity());
                        builder.setTitle("Rendering API").setMessage("Please choose a rendering API").setNegativeButton("OpenGL ES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                zArr2[0] = false;
                                semaphore2.release();
                            }
                        }).setPositiveButton("Vulkan", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                zArr2[0] = true;
                                semaphore2.release();
                            }
                        });
                        builder.setCancelable(false);
                        AlertDialog create = builder.create();
                        if (create != null) {
                            create.show();
                            alertDialogArr2[0] = create;
                        }
                    }
                });
                try {
                    z = semaphore.tryAcquire(10, TimeUnit.SECONDS);
                } catch (Throwable unused) {
                    z = false;
                }
                this.m_bUseVulkan = zArr[0];
                if (!z) {
                    alertDialogArr[0].cancel();
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public int GetEffectiveApplicationVersion() {
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() <= 0) {
            return 1000001;
        }
        return Integer.parseInt(GetString) % 1000000;
    }

    public String GetManifestURL() {
        String GetManifestBaseURL;
        boolean UseIntranetManifestURL = UseIntranetManifestURL();
        int GetEffectiveApplicationVersion = GetEffectiveApplicationVersion();
        String GetString = Resources.GetString("ManifestPasswordString");
        int[] GetInteger = Resources.GetInteger("APP_ID");
        int i = 0;
        if (GetInteger != null) {
            i = GetInteger[0];
        }
        if (i == 0 || (GetManifestBaseURL = GetManifestBaseURL(UseIntranetManifestURL)) == null) {
            return null;
        }
        String str = (GetManifestBaseURL + "&appid=" + i) + "&version=" + GetEffectiveApplicationVersion;
        if (GetString == null) {
            return str;
        }
        return str + "&password=" + GetString;
    }

    public EPermissionsState GetPermissionsState() {
        return this.m_nPermissionState;
    }

    public void SetPermissionsState(EPermissionsState ePermissionsState) {
        this.m_nPermissionState = ePermissionsState;
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
        this.m_inAppPurchases.purchaseSku(com.valvesoftware.Activity.GetPurchaseActivity(), str);
        return false;
    }

    public boolean ConsumePurchase(String str) {
        this.m_inAppPurchases.consumePurchase(str);
        return true;
    }
}
