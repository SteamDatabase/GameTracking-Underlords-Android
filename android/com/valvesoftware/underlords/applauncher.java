package com.valvesoftware.underlords;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.PatchSystem.EErrorCode;
import com.valvesoftware.PatchSystem.EState;
import com.valvesoftware.PatchSystem.EUserDownloadResponse;
import com.valvesoftware.Resources;
import com.valvesoftware.source2launcher.IContentSyncAsyncTask.TaskStatus;
import com.valvesoftware.source2launcher.application;
import java.net.URLDecoder;
import java.util.LinkedHashMap;

public class applauncher extends com.valvesoftware.source2launcher.applauncher {
    protected static final int DEFAULT_FONT_SIZE = 18;
    protected Typeface m_Font = null;
    protected ImageView m_Logo = null;
    protected Point m_ScreenSize = new Point(1, 1);
    protected int m_nLoadingBarFillWidth = 1;
    EState m_nState = EState.Unstarted;
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

    private LinearLayout setupCommonUI(String str, String str2, boolean z) {
        this.m_progressPctLabel = null;
        this.m_progressBarFill = null;
        this.m_progressBarBg = null;
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        this.m_ScreenSize = new Point();
        defaultDisplay.getSize(this.m_ScreenSize);
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new LayoutParams(-1, -1));
        frameLayout.setBackgroundColor(Color.parseColor("#000000"));
        ImageView imageView = new ImageView(this);
        int[] GetDrawable = Resources.GetDrawable("launch_background");
        if (GetDrawable != null) {
            imageView.setImageResource(GetDrawable[0]);
        }
        imageView.setLayoutParams(new LayoutParams(-1, -1));
        imageView.setScaleType(ScaleType.CENTER_CROP);
        this.m_Logo = new ImageView(this);
        int[] GetDrawable2 = Resources.GetDrawable("clean_logo");
        if (GetDrawable2 != null) {
            this.m_Logo.setImageResource(GetDrawable2[0]);
        }
        this.m_Logo.setLayoutParams(new LayoutParams((int) (((float) this.m_ScreenSize.x) * 0.62f), (int) (((float) this.m_ScreenSize.y) * 0.65f), 49));
        this.m_Logo.setPadding(20, 20, 20, 20);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int i = (int) (((float) this.m_ScreenSize.x) * 0.38f);
        int i2 = (int) (((float) this.m_ScreenSize.y) * 0.033f);
        int i3 = (int) (((float) i) * 0.9808219f);
        int i4 = (int) (((float) i2) * 0.5555556f);
        this.m_nLoadingBarFillWidth = i3;
        linearLayout.setLayoutParams(new LayoutParams(i, (int) (((float) this.m_ScreenSize.y) * 0.35f), 81));
        if (z) {
            RelativeLayout relativeLayout = new RelativeLayout(this);
            relativeLayout.setLayoutParams(new LayoutParams(i, i2));
            ImageView imageView2 = new ImageView(this);
            int[] GetDrawable3 = Resources.GetDrawable("loadingbar_bg");
            if (GetDrawable3 != null) {
                imageView2.setImageResource(GetDrawable3[0]);
            }
            imageView2.setScaleType(ScaleType.FIT_XY);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(i, i2);
            layoutParams.addRule(14);
            layoutParams.addRule(15);
            relativeLayout.addView(imageView2, layoutParams);
            ImageView imageView3 = new ImageView(this);
            int[] GetDrawable4 = Resources.GetDrawable("loadingbar_fill");
            if (GetDrawable4 != null) {
                imageView3.setImageResource(GetDrawable4[0]);
            }
            int i5 = (i - i3) / 2;
            imageView3.setScaleType(ScaleType.FIT_XY);
            imageView3.setPadding(i5, 0, 0, 0);
            this.m_progressBarFill = imageView3;
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(0, i4);
            layoutParams2.addRule(15);
            relativeLayout.addView(imageView3, layoutParams2);
            linearLayout.addView(relativeLayout);
        }
        if (str != null) {
            linearLayout.addView(createTextfield(str));
        }
        if (z) {
            TextView createTextfield = createTextfield("");
            linearLayout.addView(createTextfield);
            this.m_progressPctLabel = createTextfield;
        }
        if (str2 != null) {
            linearLayout.addView(createTextfield(str2));
        }
        String GetStringSafe = Resources.GetStringSafe("VPC_VersionCodeString", "DEBUG");
        String GetStringSafe2 = Resources.GetStringSafe("Native_VersionLabel");
        StringBuilder sb = new StringBuilder();
        sb.append(GetStringSafe2);
        sb.append(GetStringSafe);
        TextView createTextfield2 = createTextfield(sb.toString());
        createTextfield2.setGravity(85);
        createTextfield2.setPadding(12, 12, 26, 12);
        frameLayout.addView(imageView);
        frameLayout.addView(this.m_Logo);
        frameLayout.addView(linearLayout);
        frameLayout.addView(createTextfield2);
        setContentView(frameLayout);
        return linearLayout;
    }

    private void setProgress(float f) {
        if (this.m_progressPctLabel != null && this.m_progressBarFill != null) {
            float max = Math.max(0.0f, Math.min(1.0f, f));
            TextView textView = this.m_progressPctLabel;
            StringBuilder sb = new StringBuilder();
            sb.append((int) (100.0f * max));
            sb.append("%");
            textView.setText(sb.toString());
            this.m_progressBarFill.getLayoutParams().width = (int) (max * ((float) this.m_nLoadingBarFillWidth));
            this.m_progressBarFill.requestLayout();
        }
    }

    private void setupContactingServerScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_ContactingServer"), null, false);
    }

    /* access modifiers changed from: private */
    public void requestDownloadOverMobileData(EUserDownloadResponse eUserDownloadResponse) {
        String GetStringSafe = Resources.GetStringSafe("Native_DownloadOverMobileLabel");
        String GetStringSafe2 = Resources.GetStringSafe("Native_DownloadOverMobileMessage");
        Builder builder = new Builder(this);
        builder.setTitle(GetStringSafe).setMessage(GetStringSafe2).setPositiveButton(17039379, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                PatchSystem.GetInstance().SetUserDownloadResponse(EUserDownloadResponse.DownloadVPK);
                applauncher.this.setupPreparingToDownloadScreen();
            }
        }).setNegativeButton(17039369, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.create().show();
    }

    private LinearLayout setupPlayButtons(boolean z, boolean z2, long j, final EUserDownloadResponse eUserDownloadResponse, final EUserDownloadResponse eUserDownloadResponse2) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setScaleX(0.75f);
        if (z) {
            Button button = new Button(this);
            button.setTextSize(18.0f);
            button.setTypeface(this.m_Font);
            String GetStringSafe = Resources.GetStringSafe("Native_DownloadUpdate");
            StringBuilder sb = new StringBuilder();
            sb.append(GetStringSafe);
            sb.append(" ");
            sb.append(j);
            sb.append("mb");
            button.setText(sb.toString().toUpperCase());
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (applauncher.this.isConnectedToWifi()) {
                        PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse);
                        applauncher.this.setupPreparingToDownloadScreen();
                        return;
                    }
                    applauncher.this.requestDownloadOverMobileData(eUserDownloadResponse);
                }
            });
            linearLayout.addView(button);
        }
        if (z2) {
            Button button2 = new Button(this);
            button2.setTextSize(18.0f);
            button2.setTypeface(this.m_Font);
            button2.setText(Resources.GetStringSafe("Native_PlayOffline").toUpperCase());
            button2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse2);
                }
            });
            linearLayout.addView(button2);
        }
        return linearLayout;
    }

    private LinearLayout setupAPKButtons(boolean z, boolean z2, long j, final EUserDownloadResponse eUserDownloadResponse, final EUserDownloadResponse eUserDownloadResponse2) {
        String str;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setScaleX(0.75f);
        if (z) {
            Button button = new Button(this);
            button.setTextSize(18.0f);
            button.setTypeface(this.m_Font);
            button.setText(Resources.GetStringSafe("Native_DownloadAppUpdate").toUpperCase());
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (applauncher.this.isConnectedToWifi()) {
                        PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse);
                        applauncher.this.setupPreparingToDownloadScreen();
                        return;
                    }
                    applauncher.this.requestDownloadOverMobileData(eUserDownloadResponse);
                }
            });
            linearLayout.addView(button);
        }
        Button button2 = new Button(this);
        button2.setTextSize(18.0f);
        button2.setTypeface(this.m_Font);
        if (z2) {
            str = Resources.GetStringSafe("Native_PlayAppOutOfDateReq");
        } else {
            str = Resources.GetStringSafe("Native_PlayAppOutOfDateOpt");
        }
        button2.setText(str.toUpperCase());
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PatchSystem.GetInstance().SetUserDownloadResponse(eUserDownloadResponse2);
            }
        });
        linearLayout.addView(button2);
        return linearLayout;
    }

    private void setupAPKOutOfDateScreen() {
        String str;
        long GetDownloadSizeBytes = PatchSystem.GetInstance().GetDownloadSizeBytes() / 1048576;
        boolean UpdateRequiredForOnlinePlay = PatchSystem.GetInstance().UpdateRequiredForOnlinePlay();
        if (UpdateRequiredForOnlinePlay) {
            str = Resources.GetStringSafe("Native_AppOutOfDateReq");
        } else {
            str = Resources.GetStringSafe("Native_AppOutOfDateOpt");
        }
        LinearLayout linearLayout = setupCommonUI(str, null, false);
        linearLayout.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.7f);
        linearLayout.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.55f);
        this.m_Logo.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.5f);
        this.m_Logo.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.33f);
        linearLayout.addView(setupAPKButtons(true, UpdateRequiredForOnlinePlay, GetDownloadSizeBytes, EUserDownloadResponse.DownloadAPK, EUserDownloadResponse.SkipDownloadAPK));
    }

    private void installAPK() {
        if (PatchSystem.IsSelfInstallAPKEnabled()) {
            Uri GetDownloadedAPKLocation = PatchSystem.GetInstance().GetDownloadedAPKLocation();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setFlags(335544320);
            intent.setDataAndType(GetDownloadedAPKLocation, "application/vnd.android.package-archive");
            intent.addFlags(1);
            startActivity(intent);
            return;
        }
        JNI_Environment.OpenURL("market://details?id=com.valvesoftware.underlords");
    }

    private void setupManifestDownloadedScreen() {
        LinearLayout linearLayout = setupCommonUI(Resources.GetStringSafe("Native_ManifestDownloaded"), null, false);
        linearLayout.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.7f);
        linearLayout.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.55f);
        this.m_Logo.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.5f);
        this.m_Logo.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.33f);
        linearLayout.addView(setupPlayButtons(true, PatchSystem.GetInstance().CanPlayOffline(), PatchSystem.GetInstance().GetDownloadSizeBytes() / 1048576, EUserDownloadResponse.DownloadVPK, EUserDownloadResponse.SkipDownloadVPK));
    }

    /* access modifiers changed from: private */
    public void setupPreparingToDownloadScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_PreparingToDownload"), null, true);
    }

    private void setupDownloadingScreen() {
        setupCommonUI(Resources.GetStringSafe("Native_DownloadingContent"), null, true);
    }

    private void setupErrorScreen(EErrorCode eErrorCode) {
        String str;
        String GetStringSafe = Resources.GetStringSafe("Native_DownloadError");
        String str2 = "Native_DownloadErrorUnknown";
        switch (eErrorCode) {
            case None:
                str = Resources.GetStringSafe(str2);
                break;
            case Manifest:
                str = Resources.GetStringSafe("Native_DownloadErrorManifest");
                break;
            case Download:
                str = Resources.GetStringSafe("Native_DownloadErrorDownload");
                break;
            case Storage:
                str = Resources.GetStringSafe("Native_DownloadErrorStorage");
                break;
            case QueueDownload:
                str = Resources.GetStringSafe("Native_DownloadErrorQueueDownload");
                break;
            case Unknown:
                str = Resources.GetStringSafe(str2);
                break;
            default:
                str = null;
                break;
        }
        LinearLayout linearLayout = setupCommonUI(GetStringSafe, str, false);
        linearLayout.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.7f);
        linearLayout.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.55f);
        this.m_Logo.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.5f);
        this.m_Logo.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.33f);
        linearLayout.addView(setupPlayButtons(false, PatchSystem.GetInstance().CanPlayOffline(), 0, EUserDownloadResponse.DownloadVPK, EUserDownloadResponse.SkipDownloadVPK));
    }

    public void UpdateState(EState eState, EErrorCode eErrorCode) {
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
        String[] split;
        String str = "UTF-8";
        Intent intent = getIntent();
        intent.getAction();
        Uri data = intent.getData();
        if (data != null) {
            try {
                LinkedHashMap linkedHashMap = new LinkedHashMap();
                for (String str2 : data.getQuery().split("&")) {
                    int indexOf = str2.indexOf("=");
                    linkedHashMap.put(URLDecoder.decode(str2.substring(0, indexOf), str), URLDecoder.decode(str2.substring(indexOf + 1), str));
                }
                String str3 = (String) linkedHashMap.get("authority");
                String str4 = (String) linkedHashMap.get("access_code");
                application application = (application) JNI_Environment.m_application;
                boolean IsDoneBootStrapping = application.IsDoneBootStrapping();
                String str5 = " access code: ";
                String str6 = "handleSteamLoginCode authority: ";
                String str7 = BuildConfig.APPLICATION_ID;
                if (IsDoneBootStrapping) {
                    Log.e(str7, "handleSteamLoginCode - boostrapping DONE");
                    StringBuilder sb = new StringBuilder();
                    sb.append(str6);
                    sb.append(str3);
                    sb.append(str5);
                    sb.append(str4);
                    Log.e(str7, sb.toString());
                    queueSteamLoginWithAccessCode(str3, str4);
                    onLaunchMainActivity(true);
                    finish();
                } else {
                    Log.e(str7, "handleSteamLoginCode - boostrapping NOT_DONE");
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str6);
                    sb2.append(str3);
                    sb2.append(str5);
                    sb2.append(str4);
                    Log.e(str7, sb2.toString());
                    application.SetSteamLoginLaunchArgs(str3, str4);
                }
            } catch (Throwable unused) {
            }
        }
    }

    public void onCreate(Bundle bundle) {
        getWindow().getDecorView().setSystemUiVisibility(6);
        if (VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 1;
        }
        super.onCreate(bundle);
        if (JNI_Environment.m_application != null) {
            this.m_Font = Typeface.create("sans-serif", 1);
            int[] GetFont = Resources.GetFont("radiance_bold");
            if (GetFont != null) {
                this.m_Font = ResourcesCompat.getFont(this, GetFont[0]);
            }
            setupCommonUI(null, null, false);
        }
        getWindow().addFlags(128);
        HandleSteamLogin();
    }

    /* access modifiers changed from: protected */
    public void onLaunchMainActivity(boolean z) {
        Class<appmain> cls;
        application application = (application) JNI_Environment.m_application;
        StringBuilder sb = new StringBuilder();
        sb.append(application.getPackageName());
        sb.append(".appmain");
        try {
            cls = Class.forName(sb.toString(), false, application.getClassLoader());
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
    public void setInstallStatus(TaskStatus taskStatus) {
        UpdateState(taskStatus.m_nState, taskStatus.m_nErrorCode);
        setProgress(((float) taskStatus.m_nProgress) * 0.01f);
    }
}
