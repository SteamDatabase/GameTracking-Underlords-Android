package com.valvesoftware.underlords;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask;
import com.valvesoftware.source2launcher.application;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Random;

public class applauncher extends com.valvesoftware.source2launcher.applauncher {
    protected static final int DEFAULT_FONT_SIZE = 18;
    protected static final int SMALLER_FONT_SIZE = 14;
    protected Typeface m_Font = null;
    protected ImageView m_Logo = null;
    Handler m_LoopHandler;
    Runnable m_LoopRunnable;
    Random m_Random = new Random();
    protected Point m_ScreenSize = new Point(1, 1);
    protected int m_nLoadingBarFillWidth = 1;
    PatchSystem.EState m_nState = PatchSystem.EState.Unstarted;
    protected ImageView m_progressBarBg = null;
    protected ImageView m_progressBarFill = null;
    protected TextView m_progressPctLabel = null;

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

    private void setupUpdateLoop() {
        if (this.m_LoopHandler == null || this.m_LoopRunnable == null) {
            this.m_LoopHandler = new Handler();
            this.m_LoopRunnable = new Runnable() {
                public void run() {
                }
            };
            this.m_LoopHandler.postDelayed(this.m_LoopRunnable, 30);
        }
    }

    private LinearLayout setupCommonUI() {
        return setupCommonUI((String) null, (String) null, false);
    }

    private LinearLayout setupCommonUI(String str, String str2, boolean z) {
        String str3 = str2;
        setupUpdateLoop();
        this.m_progressPctLabel = null;
        this.m_progressBarFill = null;
        this.m_progressBarBg = null;
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        this.m_ScreenSize = new Point();
        defaultDisplay.getSize(this.m_ScreenSize);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        frameLayout.setBackgroundColor(Color.parseColor("#000000"));
        ImageView imageView = new ImageView(this);
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
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int i = (int) (((float) this.m_ScreenSize.x) * 0.86f);
        int i2 = (int) (((float) this.m_ScreenSize.y) * 0.016f);
        float f = (float) i;
        int i3 = (int) (f * 1.0f);
        int i4 = (int) (((float) i2) * 1.0f);
        this.m_nLoadingBarFillWidth = i3;
        this.m_Logo.setPadding((int) (((float) this.m_ScreenSize.x) * 0.11f), 80, 20, 20);
        int i5 = (int) (((float) this.m_ScreenSize.y) * 0.18f);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(i, i5, 81));
        if (z) {
            RelativeLayout relativeLayout = new RelativeLayout(this);
            relativeLayout.setLayoutParams(new FrameLayout.LayoutParams(i, i2));
            ImageView imageView2 = new ImageView(this);
            int[] GetDrawable3 = Resources.GetDrawable("loadingbar_bg");
            if (GetDrawable3 != null) {
                imageView2.setImageResource(GetDrawable3[0]);
            }
            imageView2.setScaleType(ImageView.ScaleType.FIT_XY);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(i, i2);
            layoutParams.addRule(14);
            layoutParams.addRule(15);
            relativeLayout.addView(imageView2, layoutParams);
            ImageView imageView3 = new ImageView(this);
            int[] GetDrawable4 = Resources.GetDrawable("loadingbar_fill");
            if (GetDrawable4 != null) {
                imageView3.setImageResource(GetDrawable4[0]);
            }
            imageView3.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView3.setPadding((i - i3) / 2, 0, 0, 0);
            this.m_progressBarFill = imageView3;
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(0, i4);
            layoutParams2.addRule(15);
            relativeLayout.addView(imageView3, layoutParams2);
            linearLayout.addView(relativeLayout);
        }
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(0);
        int i6 = i5 - i2;
        linearLayout2.setLayoutParams(new FrameLayout.LayoutParams(i, i6, 49));
        LinearLayout linearLayout3 = new LinearLayout(this);
        linearLayout3.setOrientation(1);
        linearLayout3.setLayoutParams(new FrameLayout.LayoutParams((int) (0.75f * f), i6, 48));
        if (str != null) {
            TextView createTextfield = createTextfield(str);
            createTextfield.setGravity(3);
            linearLayout3.addView(createTextfield);
        }
        if (str3 != null) {
            TextView createTextfield2 = createTextfield(str3);
            createTextfield2.setGravity(3);
            linearLayout3.addView(createTextfield2);
        }
        linearLayout2.addView(linearLayout3);
        if (z) {
            LinearLayout linearLayout4 = new LinearLayout(this);
            linearLayout4.setOrientation(1);
            linearLayout4.setLayoutParams(new FrameLayout.LayoutParams((int) (f * 0.25f), i6, 53));
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
        frameLayout.addView(imageView);
        frameLayout.addView(this.m_Logo);
        frameLayout.addView(linearLayout);
        frameLayout.addView(createTextfield4);
        setContentView(frameLayout);
        return linearLayout;
    }

    private void setProgress(float f) {
        if (this.m_progressPctLabel != null && this.m_progressBarFill != null) {
            float max = Math.max(0.0f, Math.min(1.0f, f));
            TextView textView = this.m_progressPctLabel;
            textView.setText(((int) (100.0f * max)) + "%");
            this.m_progressPctLabel.requestLayout();
            this.m_progressBarFill.getLayoutParams().width = (int) (max * ((float) this.m_nLoadingBarFillWidth));
            this.m_progressBarFill.requestLayout();
        }
    }

    private void setupContactingServerScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_ContactingServer"), (String) null, false);
    }

    /* access modifiers changed from: private */
    public void requestDownloadOverMobileData(PatchSystem.EUserDownloadResponse eUserDownloadResponse) {
        String GetStringSafe = Resources.GetStringSafe("Native_DownloadOverMobileLabel");
        String GetStringSafe2 = Resources.GetStringSafe("Native_DownloadOverMobileMessage");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(GetStringSafe).setMessage(GetStringSafe2).setPositiveButton(17039379, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                PatchSystem.GetInstance().SetUserDownloadResponse(PatchSystem.EUserDownloadResponse.DownloadVPK);
                applauncher.this.setupPreparingToDownloadScreen();
            }
        }).setNegativeButton(17039369, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.create().show();
    }

    private void setupPlayPopup(String str, String str2, boolean z, boolean z2, long j, final PatchSystem.EUserDownloadResponse eUserDownloadResponse, final PatchSystem.EUserDownloadResponse eUserDownloadResponse2) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (str != null) {
            builder.setTitle(str);
        }
        if (str2 != null) {
            builder.setMessage(str2);
        }
        if (!z && !z2) {
            builder.setPositiveButton(Resources.GetStringSafe("Native_OK"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
        }
        if (z) {
            String GetStringSafe = Resources.GetStringSafe("Native_DownloadUpdate");
            builder.setPositiveButton((GetStringSafe + " " + j + "mb").toUpperCase(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (applauncher.this.isConnectedToWifi()) {
                        PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse);
                        applauncher.this.setupPreparingToDownloadScreen();
                        return;
                    }
                    applauncher.this.requestDownloadOverMobileData(eUserDownloadResponse);
                }
            });
        }
        if (z2) {
            builder.setNegativeButton(Resources.GetStringSafe("Native_PlayOffline").toUpperCase(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse2);
                }
            });
        }
        builder.setCancelable(false);
        builder.create().show();
    }

    private void showAPKPopup(boolean z, boolean z2, long j, final PatchSystem.EUserDownloadResponse eUserDownloadResponse, final PatchSystem.EUserDownloadResponse eUserDownloadResponse2) {
        String str;
        String str2;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (z2) {
            str = Resources.GetStringSafe("Native_AppOutOfDateReq");
        } else {
            str = Resources.GetStringSafe("Native_AppOutOfDateOpt");
        }
        builder.setTitle(str);
        if (z) {
            builder.setPositiveButton(Resources.GetStringSafe("Native_DownloadAppUpdate"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (applauncher.this.isConnectedToWifi()) {
                        PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse);
                        applauncher.this.setupPreparingToDownloadScreen();
                        return;
                    }
                    applauncher.this.requestDownloadOverMobileData(eUserDownloadResponse);
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
                PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse2);
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void setupAPKOutOfDateScreen() {
        long GetDownloadSizeBytes = PatchSystem.GetInstance().GetDownloadSizeBytes() / 1048576;
        boolean UpdateRequiredForOnlinePlay = PatchSystem.GetInstance().UpdateRequiredForOnlinePlay();
        setupCommonUI();
        showAPKPopup(true, UpdateRequiredForOnlinePlay, GetDownloadSizeBytes, PatchSystem.EUserDownloadResponse.DownloadAPK, PatchSystem.EUserDownloadResponse.SkipDownloadAPK);
    }

    private void installAPK() {
        if (PatchSystem.IsSelfInstallAPKEnabled()) {
            Uri GetDownloadedAPKLocation = PatchSystem.GetInstance().GetDownloadedAPKLocation();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setFlags(335544320);
            intent.setDataAndType(GetDownloadedAPKLocation, "application/vnd.android.package-archive");
            if (Build.VERSION.SDK_INT >= 24) {
                intent.addFlags(1);
            }
            startActivity(intent);
            return;
        }
        JNI_Environment.OpenURL("market://details?id=com.valvesoftware.underlords");
    }

    private void setupManifestDownloadedScreen() {
        setupCommonUI();
        setupPlayPopup(Resources.GetStringSafe("Native_DownloadTitle"), (String) null, true, PatchSystem.GetInstance().CanPlayOffline(), PatchSystem.GetInstance().GetDownloadSizeBytes() / 1048576, PatchSystem.EUserDownloadResponse.DownloadVPK, PatchSystem.EUserDownloadResponse.SkipDownloadVPK);
    }

    /* access modifiers changed from: private */
    public void setupPreparingToDownloadScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_PreparingToDownload"), (String) null, true);
    }

    private void setupDownloadingScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_DownloadingContent"), (String) null, true);
    }

    private void setupErrorScreen(PatchSystem.EErrorCode eErrorCode) {
        String GetStringSafe;
        String GetStringSafe2 = Resources.GetStringSafe("Native_DownloadError");
        switch (eErrorCode) {
            case None:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorUnknown");
                break;
            case Manifest:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorManifest");
                break;
            case Download:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorDownload");
                break;
            case Storage:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorStorage");
                break;
            case QueueDownload:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorQueueDownload");
                break;
            case Unknown:
                GetStringSafe = Resources.GetStringSafe("Native_DownloadErrorUnknown");
                break;
            default:
                GetStringSafe = null;
                break;
        }
        String str = GetStringSafe;
        setupCommonUI();
        setupPlayPopup(GetStringSafe2, str, false, PatchSystem.GetInstance().CanPlayOffline(), 0, PatchSystem.EUserDownloadResponse.DownloadVPK, PatchSystem.EUserDownloadResponse.SkipDownloadVPK);
    }

    public void UpdateState(PatchSystem.EState eState, PatchSystem.EErrorCode eErrorCode) {
        if (this.m_nState != eState) {
            switch (eState) {
                case ManifestDownloading:
                    setupContactingServerScreen();
                    break;
                case APKOutOfDateWaitingOnUser:
                    setupAPKOutOfDateScreen();
                    break;
                case APKDownloadedWaitingOnUser:
                    installAPK();
                    break;
                case ManifestDownloadedWaitingOnUser:
                    setupManifestDownloadedScreen();
                    break;
                case AssetsDownloading:
                    setupDownloadingScreen();
                    break;
                case Error:
                    setupErrorScreen(eErrorCode);
                    break;
            }
            this.m_nState = eState;
        }
    }

    private void HandleSteamLogin() {
        Intent intent = getIntent();
        intent.getAction();
        Uri data = intent.getData();
        if (data != null) {
            try {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (String str : data.getQuery().split("&")) {
                    int indexOf = str.indexOf("=");
                    linkedHashMap.put(URLDecoder.decode(str.substring(0, indexOf), "UTF-8"), URLDecoder.decode(str.substring(indexOf + 1), "UTF-8"));
                }
                String str2 = (String) linkedHashMap.get("authority");
                String str3 = (String) linkedHashMap.get("access_code");
                application application = (application) JNI_Environment.m_application;
                if (application.IsDoneBootStrapping()) {
                    Log.e(BuildConfig.APPLICATION_ID, "handleSteamLoginCode - boostrapping DONE");
                    Log.e(BuildConfig.APPLICATION_ID, "handleSteamLoginCode authority: " + str2 + " access code: " + str3);
                    queueSteamLoginWithAccessCode(str2, str3);
                    onLaunchMainActivity(true);
                    finish();
                    return;
                }
                Log.e(BuildConfig.APPLICATION_ID, "handleSteamLoginCode - boostrapping NOT_DONE");
                Log.e(BuildConfig.APPLICATION_ID, "handleSteamLoginCode authority: " + str2 + " access code: " + str3);
                application.SetSteamLoginLaunchArgs(str2, str3);
            } catch (Throwable unused) {
            }
        }
    }

    public void onCreate(Bundle bundle) {
        getWindow().getDecorView().setSystemUiVisibility(6);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
        }
        super.onCreate(bundle);
        if (JNI_Environment.m_application != null) {
            this.m_Font = Typeface.create("sans-serif", 1);
            int[] GetFont = Resources.GetFont("radiance_bold");
            if (GetFont != null) {
                this.m_Font = ResourcesCompat.getFont(this, GetFont[0]);
            }
            setupCommonUI();
        }
        getWindow().addFlags(128);
        HandleSteamLogin();
    }

    /* access modifiers changed from: protected */
    public void onLaunchMainActivity(boolean z) {
        Class cls;
        application application = (application) JNI_Environment.m_application;
        try {
            cls = Class.forName(application.getPackageName() + ".appmain", false, application.getClassLoader());
        } catch (Throwable unused) {
            cls = null;
        }
        if (cls == null) {
            cls = appmain.class;
        }
        Intent intent = new Intent(this, cls);
        if (z) {
            intent.setFlags(131072);
        }
        application.SetHasRunLauncher(true);
        startActivity(intent);
        finish();
    }

    /* access modifiers changed from: protected */
    public void onBootStrapFinished() {
        super.onBootStrapFinished();
        onLaunchMainActivity(false);
    }

    /* access modifiers changed from: protected */
    public void setInstallStatus(IContentSyncAsyncTask.TaskStatus taskStatus) {
        UpdateState(taskStatus.m_nState, taskStatus.m_nErrorCode);
        setProgress(((float) taskStatus.m_nProgress) * 0.01f);
    }
}
