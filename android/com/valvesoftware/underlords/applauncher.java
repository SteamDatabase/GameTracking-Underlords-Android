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

    /* renamed from: com.valvesoftware.underlords.applauncher$7 reason: invalid class name */
    static /* synthetic */ class AnonymousClass7 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode = new int[EErrorCode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(22:0|(2:1|2)|3|(2:5|6)|7|(2:9|10)|11|(2:13|14)|15|17|18|(2:19|20)|21|23|24|25|26|27|28|29|30|(3:31|32|34)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(26:0|1|2|3|(2:5|6)|7|(2:9|10)|11|13|14|15|17|18|(2:19|20)|21|23|24|25|26|27|28|29|30|31|32|34) */
        /* JADX WARNING: Can't wrap try/catch for region: R(27:0|1|2|3|5|6|7|(2:9|10)|11|13|14|15|17|18|(2:19|20)|21|23|24|25|26|27|28|29|30|31|32|34) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:19:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:25:0x005e */
        /* JADX WARNING: Missing exception handler attribute for start block: B:27:0x0068 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:29:0x0072 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:31:0x007c */
        static {
            /*
                com.valvesoftware.PatchSystem$EState[] r0 = com.valvesoftware.PatchSystem.EState.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$valvesoftware$PatchSystem$EState = r0
                r0 = 1
                int[] r1 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.valvesoftware.PatchSystem$EState r2 = com.valvesoftware.PatchSystem.EState.ManifestDownloading     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r2 = r2.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r1[r2] = r0     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                r1 = 2
                int[] r2 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x001f }
                com.valvesoftware.PatchSystem$EState r3 = com.valvesoftware.PatchSystem.EState.APKOutOfDateWaitingOnUser     // Catch:{ NoSuchFieldError -> 0x001f }
                int r3 = r3.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2[r3] = r1     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                r2 = 3
                int[] r3 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x002a }
                com.valvesoftware.PatchSystem$EState r4 = com.valvesoftware.PatchSystem.EState.APKDownloadedWaitingOnUser     // Catch:{ NoSuchFieldError -> 0x002a }
                int r4 = r4.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r3[r4] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                r3 = 4
                int[] r4 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.valvesoftware.PatchSystem$EState r5 = com.valvesoftware.PatchSystem.EState.ManifestDownloadedWaitingOnUser     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r5 = r5.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r4[r5] = r3     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                r4 = 5
                int[] r5 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.AssetsDownloading     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r6 = r6.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r5[r6] = r4     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                int[] r5 = $SwitchMap$com$valvesoftware$PatchSystem$EState     // Catch:{ NoSuchFieldError -> 0x004b }
                com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.Error     // Catch:{ NoSuchFieldError -> 0x004b }
                int r6 = r6.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r7 = 6
                r5[r6] = r7     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                com.valvesoftware.PatchSystem$EErrorCode[] r5 = com.valvesoftware.PatchSystem.EErrorCode.values()
                int r5 = r5.length
                int[] r5 = new int[r5]
                $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode = r5
                int[] r5 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x005e }
                com.valvesoftware.PatchSystem$EErrorCode r6 = com.valvesoftware.PatchSystem.EErrorCode.None     // Catch:{ NoSuchFieldError -> 0x005e }
                int r6 = r6.ordinal()     // Catch:{ NoSuchFieldError -> 0x005e }
                r5[r6] = r0     // Catch:{ NoSuchFieldError -> 0x005e }
            L_0x005e:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0068 }
                com.valvesoftware.PatchSystem$EErrorCode r5 = com.valvesoftware.PatchSystem.EErrorCode.Manifest     // Catch:{ NoSuchFieldError -> 0x0068 }
                int r5 = r5.ordinal()     // Catch:{ NoSuchFieldError -> 0x0068 }
                r0[r5] = r1     // Catch:{ NoSuchFieldError -> 0x0068 }
            L_0x0068:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0072 }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Download     // Catch:{ NoSuchFieldError -> 0x0072 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0072 }
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0072 }
            L_0x0072:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x007c }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Storage     // Catch:{ NoSuchFieldError -> 0x007c }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x007c }
                r0[r1] = r3     // Catch:{ NoSuchFieldError -> 0x007c }
            L_0x007c:
                int[] r0 = $SwitchMap$com$valvesoftware$PatchSystem$EErrorCode     // Catch:{ NoSuchFieldError -> 0x0086 }
                com.valvesoftware.PatchSystem$EErrorCode r1 = com.valvesoftware.PatchSystem.EErrorCode.Unknown     // Catch:{ NoSuchFieldError -> 0x0086 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0086 }
                r0[r1] = r4     // Catch:{ NoSuchFieldError -> 0x0086 }
            L_0x0086:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.underlords.applauncher.AnonymousClass7.<clinit>():void");
        }
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
        String str2;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setGravity(17);
        linearLayout.setScaleX(0.75f);
        if (z) {
            Button button = new Button(this);
            button.setTextSize(18.0f);
            button.setTypeface(this.m_Font);
            if (z2) {
                str2 = Resources.GetStringSafe("Native_AppOutOfDateReq");
            } else {
                str2 = Resources.GetStringSafe("Native_AppOutOfDateOpt");
            }
            button.setText(str2.toUpperCase());
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
        long GetDownloadSizeBytes = PatchSystem.GetInstance().GetDownloadSizeBytes() / 1048576;
        LinearLayout linearLayout = setupCommonUI(Resources.GetStringSafe("Native_AppUpdateTitle"), null, false);
        linearLayout.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.7f);
        linearLayout.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.55f);
        this.m_Logo.getLayoutParams().width = (int) (((float) this.m_ScreenSize.x) * 0.5f);
        this.m_Logo.getLayoutParams().height = (int) (((float) this.m_ScreenSize.y) * 0.33f);
        linearLayout.addView(setupAPKButtons(true, PatchSystem.GetInstance().UpdateRequiredForOnlinePlay(), GetDownloadSizeBytes, EUserDownloadResponse.DownloadAPK, EUserDownloadResponse.SkipDownloadAPK));
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
        String GetStringSafe = Resources.GetStringSafe("Native_DownloadError");
        int i = AnonymousClass7.$SwitchMap$com$valvesoftware$PatchSystem$EErrorCode[eErrorCode.ordinal()];
        String str = "Native_DownloadErrorUnknown";
        String str2 = i != 1 ? i != 2 ? i != 3 ? i != 4 ? i != 5 ? null : Resources.GetStringSafe(str) : Resources.GetStringSafe("Native_DownloadErrorStorage") : Resources.GetStringSafe("Native_DownloadErrorDownload") : Resources.GetStringSafe("Native_DownloadErrorManifest") : Resources.GetStringSafe(str);
        LinearLayout linearLayout = setupCommonUI(GetStringSafe, str2, false);
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
