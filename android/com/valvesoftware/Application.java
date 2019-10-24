package com.valvesoftware;

public abstract class Application extends android.app.Application {
    public boolean ConsumePurchase(String str) {
        return false;
    }

    public abstract String[] GetProgramArguments();

    public abstract boolean InstallFiles(IStreamingBootStrap iStreamingBootStrap);

    public boolean PurchaseSku(String str) {
        return false;
    }

    public boolean QueryExistingPurchases() {
        return false;
    }

    public boolean QuerySkuDetailsAsync(String str) {
        return false;
    }

    public String[] GetNativeBinarySearchPaths(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("game:/bin/");
        sb.append(str);
        return new String[]{sb.toString()};
    }

    public void onCreate() {
        JNI_Environment.setApplication(this);
        super.onCreate();
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x0121  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x013c  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0175  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onBootStrap() {
        /*
            r11 = this;
            com.valvesoftware.JNI_Environment.onBootStrap()
            java.lang.String r0 = r11.getPackageName()
            java.lang.String r1 = "Launching Hardware info:"
            android.util.Log.i(r0, r1)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "\tPRODUCT: \""
            r1.append(r2)
            java.lang.String r2 = android.os.Build.PRODUCT
            r1.append(r2)
            java.lang.String r2 = "\""
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "\tBRAND: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.BRAND
            r1.append(r3)
            java.lang.String r3 = "\" MANUFACTURER: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.MANUFACTURER
            r1.append(r3)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "\tMODEL: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.MODEL
            r1.append(r3)
            java.lang.String r3 = "\" DEVICE: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.DEVICE
            r1.append(r3)
            java.lang.String r3 = "\" BOARD: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.BOARD
            r1.append(r3)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "\tDISPLAY: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.DISPLAY
            r1.append(r3)
            java.lang.String r3 = "\" FINGERPRINT: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.FINGERPRINT
            r1.append(r3)
            java.lang.String r3 = "\" HARDWARE: \""
            r1.append(r3)
            java.lang.String r3 = android.os.Build.HARDWARE
            r1.append(r3)
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
            java.lang.String r1 = "VPC_BinariesOmittedFromAPK"
            boolean[] r1 = com.valvesoftware.Resources.GetBoolean(r1)
            r3 = 1
            r4 = 0
            if (r1 == 0) goto L_0x00b4
            boolean r1 = r1[r4]
            if (r1 == 0) goto L_0x00b4
            r1 = 1
            goto L_0x00b5
        L_0x00b4:
            r1 = 0
        L_0x00b5:
            java.lang.String[] r5 = com.valvesoftware.JNI_Environment.GetSupportedABIs()
            int r6 = r5.length
            if (r6 <= 0) goto L_0x00bf
            r6 = r5[r4]
            goto L_0x00c1
        L_0x00bf:
            java.lang.String r6 = ""
        L_0x00c1:
            int r7 = r5.length
            if (r3 >= r7) goto L_0x00dd
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r7.append(r6)
            java.lang.String r6 = ";"
            r7.append(r6)
            r6 = r5[r3]
            r7.append(r6)
            java.lang.String r6 = r7.toString()
            int r3 = r3 + 1
            goto L_0x00c1
        L_0x00dd:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r5 = " cpu: \""
            r3.append(r5)
            java.lang.String r5 = "os.arch"
            java.lang.String r7 = java.lang.System.getProperty(r5)
            r3.append(r7)
            java.lang.String r7 = "\", ABI's: \""
            r3.append(r7)
            r3.append(r6)
            java.lang.String r7 = "\", omitting: "
            r3.append(r7)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            android.util.Log.i(r0, r3)
            r3 = 500(0x1f4, float:7.0E-43)
            r7 = 5000(0x1388, float:7.006E-42)
            r8 = 0
            com.valvesoftware.BootStrapClient$ConnectionResult r3 = com.valvesoftware.BootStrapClient.connectToDevPC(r3, r7)     // Catch:{ Throwable -> 0x0118 }
            if (r3 == 0) goto L_0x0118
            com.valvesoftware.IStreamingBootStrap r3 = r3.connection     // Catch:{ Throwable -> 0x0118 }
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.SetPrimaryJavaConnection(r3)     // Catch:{ Throwable -> 0x0119 }
            goto L_0x0119
        L_0x0118:
            r3 = r8
        L_0x0119:
            java.lang.String r7 = "VPC_TargetPlatformName"
            java.lang.String r7 = com.valvesoftware.Resources.GetString(r7)
            if (r7 != 0) goto L_0x0123
            java.lang.String r7 = "unknown"
        L_0x0123:
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r10 = "APK Targets VPC Platform \""
            r9.append(r10)
            r9.append(r7)
            r9.append(r2)
            java.lang.String r2 = r9.toString()
            android.util.Log.i(r0, r2)
            if (r3 == 0) goto L_0x0151
            java.lang.String r0 = "PLATFORM"
            java.lang.String r2 = "android"
            r3.SetAttributeValue(r0, r4, r2, r8)
            java.lang.String r0 = java.lang.System.getProperty(r5)
            java.lang.String r2 = "CPU_ARCH"
            r3.SetAttributeValue(r2, r4, r0, r8)
            java.lang.String r0 = "SUPPORTED_ABIS"
            r3.SetAttributeValue(r0, r4, r6, r8)
        L_0x0151:
            if (r1 == 0) goto L_0x0175
            com.valvesoftware.BootStrapClient$NativeLibraryPathResolver r0 = new com.valvesoftware.BootStrapClient$NativeLibraryPathResolver
            if (r1 == 0) goto L_0x0158
            r8 = r3
        L_0x0158:
            java.io.File r1 = com.valvesoftware.JNI_Environment.GetPrivatePath()
            r0.<init>(r8, r1)
            java.lang.String r1 = "$(PLATFORM_ARCH)"
            r0.AddVariableReplacement(r1, r7)
            java.lang.String[] r1 = r11.GetNativeBinarySearchPaths(r1)
            if (r1 == 0) goto L_0x0176
        L_0x016a:
            int r2 = r1.length
            if (r4 >= r2) goto L_0x0176
            r2 = r1[r4]
            com.valvesoftware.JNI_Environment.AddNativeLibrarySearchPath(r2)
            int r4 = r4 + 1
            goto L_0x016a
        L_0x0175:
            r0 = r8
        L_0x0176:
            java.lang.String[] r1 = r11.GetProgramArguments()
            com.valvesoftware.JNI_Environment.setup(r0, r1)
            r11.InstallFiles(r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.Application.onBootStrap():void");
    }
}
