package com.valvesoftware;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.io.File;

public class JNI_Environment {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String k_sSpewPackageName = "com.valvesoftware.JNI_Environment";
    private static INativeLibraryPathResolver m_NativeLibraryPathResolver;
    public static Handler m_OSHandler;
    public static Application m_application;
    private static boolean m_bSetupCalled;
    private static String[] m_sNativeLibrarySearchPaths;
    private static File m_sPrivatePath;
    private static File m_sPublicPath;
    public static String m_sVPCPlatformName;
    private static RequestQueue sm_VolleyQueue;

    public interface INativeLibraryPathResolver {
        String ResolveNativeLibraryPath(String str);
    }

    private static native long CRC32FileNative(String str);

    public static native String[] GetNeededSharedObjects(String str, boolean z);

    private static native boolean IsSharedObjectLoadedNative(String str);

    private static native String setupNative(int i, Object obj, Class<?> cls, String str, String str2);

    public static void onApplicationCreate(Application application, String str) {
        m_OSHandler = new Handler();
        m_application = application;
        m_sPrivatePath = new File(m_application.getFilesDir(), BuildConfig.APPLICATION_ID);
        if (!m_sPrivatePath.exists()) {
            m_sPrivatePath.mkdirs();
        }
        m_sPublicPath = SelfInstall.GetContentDirectory(Resources.GetString("BRANCH_ID"), str);
        if (m_sPublicPath == null) {
            Log.e(k_sSpewPackageName, "Could not determine a content directory");
            System.exit(1);
        }
        if (!m_sPublicPath.exists()) {
            m_sPublicPath.mkdirs();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("m_sPrivatePath = \"");
        sb.append(m_sPrivatePath.getAbsolutePath());
        sb.append("\"\nm_sPublicPath = \"");
        sb.append(m_sPublicPath.getAbsolutePath());
        sb.append("\"");
        sb.append(Environment.isExternalStorageEmulated(m_sPublicPath) ? " (EMULATED)" : "");
        sb.append(" (");
        sb.append(GetAvailableStorageBytes(m_sPublicPath));
        sb.append(" Bytes total)");
        Log.i(k_sSpewPackageName, sb.toString());
        AddNativeLibrarySearchPath(m_application.getApplicationContext().getApplicationInfo().nativeLibraryDir);
    }

    public static void setup() {
        boolean z;
        try {
            System.loadLibrary("jni_environment");
            z = true;
        } catch (Throwable unused) {
            z = false;
        }
        if (!z) {
            FindAndLoadNativeLibrary("libjni_environment.so");
        }
        Class<?> cls = null;
        try {
            cls = Class.forName(m_application.getPackageName() + ".R", false, m_application.getClassLoader());
        } catch (Throwable unused2) {
        }
        m_sVPCPlatformName = setupNative(Build.VERSION.SDK_INT, m_application, cls, m_sPrivatePath.getAbsolutePath(), m_sPublicPath.getAbsolutePath());
        m_bSetupCalled = true;
        if (sm_VolleyQueue == null) {
            sm_VolleyQueue = Volley.newRequestQueue((Context) m_application, (BaseHttpStack) new HurlStack());
        }
    }

    public static String[] GetProgramArguments() {
        Application GetInstance = Application.GetInstance();
        if (GetInstance != null) {
            return GetInstance.GetProgramArguments();
        }
        return null;
    }

    /* JADX WARNING: Missing exception handler attribute for start block: B:12:0x0019 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:17:0x0024 */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x001d A[Catch:{ Throwable -> 0x0003, Throwable -> 0x0019, Throwable -> 0x0024, Throwable -> 0x002f }] */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0028 A[Catch:{ Throwable -> 0x0003, Throwable -> 0x0019, Throwable -> 0x0024, Throwable -> 0x002f }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String[] GetSupportedABIs() {
        /*
            java.lang.String[] r0 = android.os.Build.SUPPORTED_ABIS     // Catch:{ Throwable -> 0x0003 }
            return r0
        L_0x0003:
            r0 = 1
            r1 = 0
            java.lang.String r2 = android.os.Build.CPU_ABI     // Catch:{ Throwable -> 0x0019 }
            if (r2 == 0) goto L_0x0019
            java.lang.String r2 = android.os.Build.CPU_ABI2     // Catch:{ Throwable -> 0x0019 }
            if (r2 == 0) goto L_0x0019
            r2 = 2
            java.lang.String[] r2 = new java.lang.String[r2]     // Catch:{ Throwable -> 0x0019 }
            java.lang.String r3 = android.os.Build.CPU_ABI     // Catch:{ Throwable -> 0x0019 }
            r2[r1] = r3     // Catch:{ Throwable -> 0x0019 }
            java.lang.String r3 = android.os.Build.CPU_ABI2     // Catch:{ Throwable -> 0x0019 }
            r2[r0] = r3     // Catch:{ Throwable -> 0x0019 }
            return r2
        L_0x0019:
            java.lang.String r2 = android.os.Build.CPU_ABI     // Catch:{ Throwable -> 0x0024 }
            if (r2 == 0) goto L_0x0024
            java.lang.String[] r2 = new java.lang.String[r0]     // Catch:{ Throwable -> 0x0024 }
            java.lang.String r3 = android.os.Build.CPU_ABI     // Catch:{ Throwable -> 0x0024 }
            r2[r1] = r3     // Catch:{ Throwable -> 0x0024 }
            return r2
        L_0x0024:
            java.lang.String r2 = android.os.Build.CPU_ABI2     // Catch:{ Throwable -> 0x002f }
            if (r2 == 0) goto L_0x002f
            java.lang.String[] r0 = new java.lang.String[r0]     // Catch:{ Throwable -> 0x002f }
            java.lang.String r2 = android.os.Build.CPU_ABI2     // Catch:{ Throwable -> 0x002f }
            r0[r1] = r2     // Catch:{ Throwable -> 0x002f }
            return r0
        L_0x002f:
            java.lang.String[] r0 = new java.lang.String[r1]
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.JNI_Environment.GetSupportedABIs():java.lang.String[]");
    }

    public static String GetVPCPlatform() {
        String[] GetSupportedABIs = GetSupportedABIs();
        for (String GetVPCPlatformForABI : GetSupportedABIs) {
            String GetVPCPlatformForABI2 = GetVPCPlatformForABI(GetVPCPlatformForABI);
            if (GetVPCPlatformForABI2 != null) {
                return GetVPCPlatformForABI2;
            }
        }
        return null;
    }

    public static String GetVPCPlatformForABI(String str) {
        if (str.equals("armeabi-v7a")) {
            return "androidarm32";
        }
        if (str.equals("arm64-v8a")) {
            return "androidarm64";
        }
        if (str.equals("mips")) {
            return "androidmips32";
        }
        if (str.equals("mips64")) {
            return "androidmips64";
        }
        if (str.equals("x86")) {
            return "androidx8632";
        }
        if (str.equals("x86_64")) {
            return "androidx8664";
        }
        if (str.startsWith("arm")) {
            if (str.contains("64")) {
                return "androidarm64";
            }
            return "androidarm32";
        } else if (str.startsWith("mips")) {
            if (str.contains("64")) {
                return "androidmips64";
            }
            return "androidmips32";
        } else if (!str.startsWith("x86")) {
            return null;
        } else {
            if (str.contains("64")) {
                return "androidx8664";
            }
            return "androidx8632";
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:116:0x04a8  */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x051d  */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x0524 A[ADDED_TO_REGION] */
    /* JADX WARNING: Removed duplicated region for block: B:148:0x0566  */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x0581  */
    /* JADX WARNING: Removed duplicated region for block: B:155:0x05a9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean LoadNativeLibrary(java.lang.String r52) {
        /*
            r1 = r52
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "Start loading native module \""
            r0.append(r2)
            r0.append(r1)
            java.lang.String r2 = "\""
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            java.lang.String r3 = "com.valvesoftware.JNI_Environment.LoadNativeLibrary"
            android.util.Log.i(r3, r0)
            r4 = 0
            java.lang.String r0 = "search:/"
            boolean r0 = r1.startsWith(r0, r4)
            r6 = 2
            java.lang.String r7 = "Native Module \""
            java.lang.String r8 = "com.valvesoftware.JNI_Environment"
            r9 = 1
            if (r0 != 0) goto L_0x05a1
            java.io.File r0 = new java.io.File
            r0.<init>(r1)
            java.lang.String r10 = r0.getName()
            boolean r11 = r0.exists()
            if (r11 == 0) goto L_0x0584
            boolean r11 = m_bSetupCalled
            if (r11 == 0) goto L_0x007d
            java.lang.String[] r11 = GetNeededSharedObjects(r1, r9)
            if (r11 == 0) goto L_0x007d
            r12 = 0
        L_0x0046:
            int r13 = r11.length
            if (r12 >= r13) goto L_0x007d
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r14 = "Module \""
            r13.append(r14)
            r13.append(r1)
            java.lang.String r14 = "\" needs shared object \""
            r13.append(r14)
            r14 = r11[r12]
            r13.append(r14)
            java.lang.String r14 = "\" to be loaded first. Detouring..."
            r13.append(r14)
            java.lang.String r13 = r13.toString()
            android.util.Log.i(r3, r13)
            java.io.File r13 = new java.io.File
            r14 = r11[r12]
            r13.<init>(r14)
            java.lang.String r13 = r13.getName()
            FindAndLoadNativeLibrary(r13)
            int r12 = r12 + 1
            goto L_0x0046
        L_0x007d:
            android.app.Application r11 = m_application
            android.content.Context r11 = r11.getApplicationContext()
            android.content.pm.ApplicationInfo r11 = r11.getApplicationInfo()
            java.lang.String r11 = r11.nativeLibraryDir
            boolean r11 = r1.startsWith(r11)
            if (r11 == 0) goto L_0x00a1
            java.lang.String r12 = "lib"
            boolean r12 = r10.startsWith(r12)
            if (r12 == 0) goto L_0x00a1
            java.lang.String r12 = ".so"
            boolean r12 = r10.endsWith(r12)
            if (r12 == 0) goto L_0x00a1
            r12 = 1
            goto L_0x00a2
        L_0x00a1:
            r12 = 0
        L_0x00a2:
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r14 = "Module \""
            r13.append(r14)
            r13.append(r1)
            java.lang.String r14 = "\" already in native search path ["
            r13.append(r14)
            r13.append(r12)
            java.lang.String r14 = "]"
            r13.append(r14)
            java.lang.String r13 = r13.toString()
            android.util.Log.i(r3, r13)
            if (r11 != 0) goto L_0x00cd
            int r11 = android.os.Build.VERSION.SDK_INT
            r13 = 23
            if (r11 < r13) goto L_0x00cd
            r11 = 1
            goto L_0x00ce
        L_0x00cd:
            r11 = 0
        L_0x00ce:
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            r13.append(r1)
            java.lang.String r14 = ".zip"
            r13.append(r14)
            java.lang.String r13 = r13.toString()
            if (r11 == 0) goto L_0x0496
            java.io.File r14 = new java.io.File
            r14.<init>(r13)
            boolean r15 = r14.exists()
            if (r15 == 0) goto L_0x0134
            long r15 = r14.lastModified()
            long r17 = r0.lastModified()
            int r19 = (r15 > r17 ? 1 : (r15 == r17 ? 0 : -1))
            if (r19 >= 0) goto L_0x00f9
            goto L_0x0134
        L_0x00f9:
            java.lang.StringBuilder r15 = new java.lang.StringBuilder
            r15.<init>()
            java.lang.String r5 = "Zip \""
            r15.append(r5)
            r15.append(r13)
            java.lang.String r5 = "\" is up to date for binary \""
            r15.append(r5)
            r15.append(r1)
            java.lang.String r5 = "\". ("
            r15.append(r5)
            long r4 = r14.lastModified()
            r15.append(r4)
            java.lang.String r4 = ">="
            r15.append(r4)
            long r4 = r0.lastModified()
            r15.append(r4)
            java.lang.String r0 = ")"
            r15.append(r0)
            java.lang.String r0 = r15.toString()
            android.util.Log.i(r3, r0)
            goto L_0x0496
        L_0x0134:
            boolean r4 = r14.exists()
            if (r4 == 0) goto L_0x013d
            r14.delete()
        L_0x013d:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Zipping \""
            r4.append(r5)
            r4.append(r1)
            java.lang.String r5 = "\" for subsequent loading attempt"
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            android.util.Log.i(r3, r4)
            long r3 = r0.length()
            int r4 = (int) r3
            r3 = 0
            java.io.FileInputStream r5 = new java.io.FileInputStream     // Catch:{ Throwable -> 0x0162 }
            r5.<init>(r1)     // Catch:{ Throwable -> 0x0162 }
            r3 = r5
        L_0x0162:
            r5 = 0
            java.io.RandomAccessFile r14 = new java.io.RandomAccessFile     // Catch:{ Throwable -> 0x016b }
            java.lang.String r15 = "rw"
            r14.<init>(r13, r15)     // Catch:{ Throwable -> 0x016b }
            r5 = r14
        L_0x016b:
            if (r5 == 0) goto L_0x0481
            r14 = 4096(0x1000, float:5.74E-42)
            byte[] r15 = new byte[r14]
            r19 = r15
            long r14 = r0.lastModified()
            java.util.GregorianCalendar r0 = new java.util.GregorianCalendar
            r0.<init>()
            r0.setTimeInMillis(r14)
            int r14 = r0.get(r9)
            int r14 = r14 + -1980
            int r15 = r0.get(r6)
            int r15 = r15 + r9
            r9 = 5
            int r21 = r0.get(r9)
            r14 = r14 & 127(0x7f, float:1.78E-43)
            int r14 = r14 << 9
            r17 = 0
            r14 = r14 | 0
            r15 = r15 & 15
            int r15 = r15 << r9
            r14 = r14 | r15
            r15 = 31
            r21 = r21 & 31
            r14 = r14 | r21
            r6 = 11
            int r6 = r0.get(r6)
            r9 = 12
            int r9 = r0.get(r9)
            r23 = r11
            r11 = 13
            int r0 = r0.get(r11)
            r6 = r6 & r15
            int r6 = r6 << 11
            r11 = 0
            r6 = r6 | r11
            r9 = r9 & 63
            r11 = 5
            int r9 = r9 << r11
            r6 = r6 | r9
            r9 = 2
            int r0 = r0 / r9
            r0 = r0 & r15
            r0 = r0 | r6
            java.nio.charset.Charset r6 = java.nio.charset.StandardCharsets.UTF_8
            byte[] r6 = r10.getBytes(r6)
            int r11 = r6.length
            r21 = 80
            r17 = 0
            r19[r17] = r21
            r21 = 75
            r20 = 1
            r19[r20] = r21
            r16 = 3
            r19[r9] = r16
            r9 = 4
            r19[r16] = r9
            r24 = 10
            r19[r9] = r24
            r22 = 5
            r19[r22] = r17
            r25 = 6
            r19[r25] = r17
            r26 = 7
            r19[r26] = r17
            r27 = 8
            r19[r27] = r17
            r28 = 9
            r19[r28] = r17
            r15 = r0 & 255(0xff, float:3.57E-43)
            byte r15 = (byte) r15
            r19[r24] = r15
            r29 = 11
            int r0 = r0 >> 8
            r0 = r0 & 255(0xff, float:3.57E-43)
            byte r0 = (byte) r0
            r19[r29] = r0
            r30 = 12
            r9 = r14 & 255(0xff, float:3.57E-43)
            byte r9 = (byte) r9
            r19[r30] = r9
            r31 = 13
            int r14 = r14 >> 8
            r14 = r14 & 255(0xff, float:3.57E-43)
            byte r14 = (byte) r14
            r19[r31] = r14
            r32 = r7
            r33 = 18
            r7 = r4 & 255(0xff, float:3.57E-43)
            byte r7 = (byte) r7
            r19[r33] = r7
            r33 = 19
            int r1 = r4 >> 8
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r19[r33] = r1
            r33 = 20
            r34 = r12
            int r12 = r4 >> 16
            r12 = r12 & 255(0xff, float:3.57E-43)
            byte r12 = (byte) r12
            r19[r33] = r12
            r33 = 21
            r35 = r8
            int r8 = r4 >> 24
            r8 = r8 & 255(0xff, float:3.57E-43)
            byte r8 = (byte) r8
            r19[r33] = r8
            r33 = 22
            r19[r33] = r7
            r33 = 23
            r19[r33] = r1
            r33 = 24
            r19[r33] = r12
            r36 = 25
            r19[r36] = r8
            r36 = 26
            r37 = r2
            r2 = r11 & 255(0xff, float:3.57E-43)
            byte r2 = (byte) r2
            r19[r36] = r2
            r36 = 27
            r38 = r10
            int r10 = r11 >> 8
            r10 = r10 & 255(0xff, float:3.57E-43)
            byte r10 = (byte) r10
            r19[r36] = r10
            r36 = r13
            int r13 = r11 + 30
            r39 = r10
            r10 = 4096(0x1000, float:5.74E-42)
            int r10 = r10 - r13
            r13 = 28
            r18 = r2
            r2 = r10 & 255(0xff, float:3.57E-43)
            byte r2 = (byte) r2
            r19[r13] = r2
            r2 = 29
            int r10 = r10 >> 8
            r10 = r10 & 255(0xff, float:3.57E-43)
            byte r10 = (byte) r10
            r19[r2] = r10
            r2 = 30
            r10 = r19
            r13 = 0
            java.lang.System.arraycopy(r6, r13, r10, r2, r11)
            int r2 = r2 + r11
            r19 = r6
        L_0x0285:
            int r6 = r10.length
            if (r2 >= r6) goto L_0x028d
            r10[r2] = r13
            int r2 = r2 + 1
            goto L_0x0285
        L_0x028d:
            r5.write(r10, r13, r2)     // Catch:{ Throwable -> 0x0292 }
            r6 = 1
            goto L_0x0293
        L_0x0292:
            r6 = 0
        L_0x0293:
            int r2 = r2 + r13
            r13 = 65536(0x10000, float:9.18355E-41)
            int r13 = java.lang.Math.min(r4, r13)
            r40 = r2
            byte[] r2 = new byte[r13]
            r41 = r4
            java.util.zip.CRC32 r4 = new java.util.zip.CRC32
            r4.<init>()
            r51 = r40
            r40 = r6
            r6 = r41
            r41 = r51
        L_0x02ad:
            if (r3 == 0) goto L_0x02c9
            r42 = r11
            int r11 = java.lang.Math.min(r13, r6)     // Catch:{ Throwable -> 0x02c2 }
            r44 = r8
            r43 = r13
            r13 = 0
            int r8 = r3.read(r2, r13, r11)     // Catch:{ Throwable -> 0x02c7 }
            if (r8 != r11) goto L_0x02c7
            r8 = 1
            goto L_0x02d1
        L_0x02c2:
            r44 = r8
            r43 = r13
            r11 = 0
        L_0x02c7:
            r8 = 0
            goto L_0x02d1
        L_0x02c9:
            r44 = r8
            r42 = r11
            r43 = r13
            r8 = 0
            r11 = 0
        L_0x02d1:
            if (r8 != 0) goto L_0x02d8
            r2 = r41
            r40 = 0
            goto L_0x02ed
        L_0x02d8:
            if (r40 == 0) goto L_0x02e2
            r8 = 0
            r5.write(r2, r8, r11)     // Catch:{ Throwable -> 0x02df }
            goto L_0x02e3
        L_0x02df:
            r40 = 0
            goto L_0x02e3
        L_0x02e2:
            r8 = 0
        L_0x02e3:
            r4.update(r2, r8, r11)
            int r41 = r41 + r11
            int r6 = r6 - r11
            if (r6 > 0) goto L_0x0472
            r2 = r41
        L_0x02ed:
            long r45 = r4.getValue()
            r47 = 255(0xff, double:1.26E-321)
            r8 = r5
            long r4 = r45 & r47
            int r5 = (int) r4
            byte r4 = (byte) r5
            r5 = 14
            r10[r5] = r4
            r5 = 15
            long r47 = r45 >> r27
            r49 = 255(0xff, double:1.26E-321)
            r11 = r7
            long r6 = r47 & r49
            int r7 = (int) r6
            byte r6 = (byte) r7
            r10[r5] = r6
            r7 = 16
            long r47 = r45 >> r7
            r43 = r6
            long r5 = r47 & r49
            int r6 = (int) r5
            byte r5 = (byte) r6
            r10[r7] = r5
            r6 = 17
            long r45 = r45 >> r33
            r47 = 255(0xff, double:1.26E-321)
            r49 = r14
            long r13 = r45 & r47
            int r14 = (int) r13
            byte r13 = (byte) r14
            r10[r6] = r13
            r45 = r11
            r14 = r12
            r6 = 14
            long r11 = (long) r6
            r8.seek(r11)     // Catch:{ Throwable -> 0x0335 }
            r11 = 4
            r8.write(r10, r6, r11)     // Catch:{ Throwable -> 0x0335 }
            long r11 = (long) r2     // Catch:{ Throwable -> 0x0335 }
            r8.seek(r11)     // Catch:{ Throwable -> 0x0335 }
            goto L_0x0337
        L_0x0335:
            r40 = 0
        L_0x0337:
            r6 = 80
            r11 = 0
            r10[r11] = r6
            r6 = 75
            r12 = 1
            r10[r12] = r6
            r6 = 2
            r10[r6] = r12
            r12 = 3
            r10[r12] = r6
            r6 = 4
            r10[r6] = r24
            r6 = 5
            r10[r6] = r11
            r10[r25] = r24
            r10[r26] = r11
            r10[r27] = r11
            r10[r28] = r11
            r10[r24] = r11
            r10[r29] = r11
            r10[r30] = r15
            r10[r31] = r0
            r0 = 14
            r10[r0] = r9
            r0 = 15
            r10[r0] = r49
            r10[r7] = r4
            r0 = 17
            r10[r0] = r43
            r0 = 18
            r10[r0] = r5
            r0 = 19
            r10[r0] = r13
            r0 = 20
            r10[r0] = r45
            r0 = 21
            r10[r0] = r1
            r0 = 22
            r10[r0] = r14
            r0 = 23
            r10[r0] = r44
            r10[r33] = r45
            r0 = 25
            r10[r0] = r1
            r0 = 26
            r10[r0] = r14
            r0 = 27
            r10[r0] = r44
            r0 = 28
            r10[r0] = r18
            r0 = 29
            r10[r0] = r39
            r0 = 30
            r1 = 0
            r10[r0] = r1
            r5 = 31
            r10[r5] = r1
            r0 = 32
            r10[r0] = r1
            r0 = 33
            r10[r0] = r1
            r0 = 34
            r10[r0] = r1
            r0 = 35
            r10[r0] = r1
            r0 = 36
            byte r4 = (byte) r1
            r10[r0] = r4
            r0 = 37
            byte r5 = (byte) r1
            r10[r0] = r5
            r0 = 38
            r10[r0] = r4
            r0 = 39
            r10[r0] = r5
            r0 = 40
            byte r4 = (byte) r1
            r10[r0] = r4
            r0 = 41
            byte r4 = (byte) r1
            r10[r0] = r4
            r0 = 42
            r10[r0] = r1
            r0 = 43
            r10[r0] = r1
            r0 = 44
            r10[r0] = r1
            r0 = 45
            r10[r0] = r1
            r0 = 46
            r11 = r19
            r12 = r42
            java.lang.System.arraycopy(r11, r1, r10, r0, r12)
            int r0 = r0 + r12
            if (r40 == 0) goto L_0x03f0
            r8.write(r10, r1, r0)     // Catch:{ Throwable -> 0x03ee }
            goto L_0x03f0
        L_0x03ee:
            r4 = 0
            goto L_0x03f2
        L_0x03f0:
            r4 = r40
        L_0x03f2:
            int r0 = r0 + r1
            r5 = 80
            r10[r1] = r5
            r5 = 75
            r6 = 1
            r10[r6] = r5
            r5 = 2
            r13 = 5
            r10[r5] = r13
            r5 = 6
            r16 = 3
            r10[r16] = r5
            r19 = 4
            r10[r19] = r1
            r10[r13] = r1
            r10[r25] = r1
            r10[r26] = r1
            r10[r27] = r6
            r10[r28] = r1
            r10[r24] = r6
            r10[r29] = r1
            r1 = r0 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r10[r30] = r1
            int r1 = r0 >> 8
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r10[r31] = r1
            int r1 = r0 >> 16
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r22 = 14
            r10[r22] = r1
            r1 = 15
            int r0 = r0 >> 24
            r0 = r0 & 255(0xff, float:3.57E-43)
            byte r0 = (byte) r0
            r10[r1] = r0
            r0 = r2 & 255(0xff, float:3.57E-43)
            byte r0 = (byte) r0
            r10[r7] = r0
            r0 = 17
            int r1 = r2 >> 8
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r10[r0] = r1
            r0 = 18
            int r1 = r2 >> 16
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r10[r0] = r1
            r0 = 19
            int r1 = r2 >> 24
            r1 = r1 & 255(0xff, float:3.57E-43)
            byte r1 = (byte) r1
            r10[r0] = r1
            r0 = 20
            r1 = 0
            r10[r0] = r1
            r0 = 21
            r10[r0] = r1
            r0 = 22
            if (r4 == 0) goto L_0x0467
            r8.write(r10, r1, r0)     // Catch:{ Throwable -> 0x0466 }
            goto L_0x0467
        L_0x0466:
            r4 = 0
        L_0x0467:
            r8.close()     // Catch:{ Throwable -> 0x046a }
        L_0x046a:
            if (r4 != 0) goto L_0x046e
            r4 = 0
            goto L_0x0470
        L_0x046e:
            r4 = r23
        L_0x0470:
            r11 = r4
            goto L_0x0490
        L_0x0472:
            r49 = r14
            r11 = r19
            r13 = 5
            r16 = 3
            r11 = r42
            r13 = r43
            r8 = r44
            goto L_0x02ad
        L_0x0481:
            r37 = r2
            r32 = r7
            r35 = r8
            r38 = r10
            r34 = r12
            r36 = r13
            r16 = 3
            r11 = 0
        L_0x0490:
            r3.close()     // Catch:{ Throwable -> 0x0493 }
        L_0x0493:
            r23 = r11
            goto L_0x04a6
        L_0x0496:
            r37 = r2
            r32 = r7
            r35 = r8
            r38 = r10
            r23 = r11
            r34 = r12
            r36 = r13
            r16 = 3
        L_0x04a6:
            if (r23 == 0) goto L_0x051d
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r1 = r36
            r0.append(r1)
            java.lang.String r1 = "!/"
            r0.append(r1)
            r1 = r38
            r0.append(r1)
            java.lang.String r1 = r0.toString()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x04e6 }
            r0.<init>()     // Catch:{ Throwable -> 0x04e6 }
            java.lang.String r2 = "Trying zip load path \""
            r0.append(r2)     // Catch:{ Throwable -> 0x04e6 }
            r0.append(r1)     // Catch:{ Throwable -> 0x04e6 }
            r2 = r37
            r0.append(r2)     // Catch:{ Throwable -> 0x04e2 }
            java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x04e2 }
            r3 = r35
            android.util.Log.i(r3, r0)     // Catch:{ Throwable -> 0x04e0 }
            java.lang.System.load(r1)     // Catch:{ Throwable -> 0x04e0 }
            r1 = 1
            goto L_0x0522
        L_0x04e0:
            r0 = move-exception
            goto L_0x04eb
        L_0x04e2:
            r0 = move-exception
            r3 = r35
            goto L_0x04eb
        L_0x04e6:
            r0 = move-exception
            r3 = r35
            r2 = r37
        L_0x04eb:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Exception during zip load: "
            r4.append(r5)
            java.lang.String r0 = r0.getMessage()
            r4.append(r0)
            java.lang.String r0 = r4.toString()
            android.util.Log.i(r3, r0)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "\tZip load of \""
            r0.append(r4)
            r0.append(r1)
            java.lang.String r1 = "\" failed"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            goto L_0x0521
        L_0x051d:
            r3 = r35
            r2 = r37
        L_0x0521:
            r1 = 0
        L_0x0522:
            if (r1 != 0) goto L_0x0562
            if (r34 != 0) goto L_0x0562
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0546 }
            r0.<init>()     // Catch:{ Throwable -> 0x0546 }
            java.lang.String r4 = "Trying plain module path \""
            r0.append(r4)     // Catch:{ Throwable -> 0x0546 }
            r4 = r52
            r0.append(r4)     // Catch:{ Throwable -> 0x0544 }
            r0.append(r2)     // Catch:{ Throwable -> 0x0544 }
            java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x0544 }
            android.util.Log.i(r3, r0)     // Catch:{ Throwable -> 0x0544 }
            java.lang.System.load(r52)     // Catch:{ Throwable -> 0x0544 }
            r1 = 1
            goto L_0x0564
        L_0x0544:
            r0 = move-exception
            goto L_0x0549
        L_0x0546:
            r0 = move-exception
            r4 = r52
        L_0x0549:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "Exception during plain load: "
            r5.append(r6)
            java.lang.String r0 = r0.getMessage()
            r5.append(r0)
            java.lang.String r0 = r5.toString()
            android.util.Log.i(r3, r0)
            goto L_0x0564
        L_0x0562:
            r4 = r52
        L_0x0564:
            if (r1 == 0) goto L_0x0581
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r5 = r32
            r0.append(r5)
            r0.append(r4)
            java.lang.String r1 = "\" loaded"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            r1 = 1
            return r1
        L_0x0581:
            r5 = r32
            goto L_0x05a7
        L_0x0584:
            r4 = r1
            r5 = r7
            r3 = r8
            r16 = 3
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r5)
            r0.append(r4)
            java.lang.String r1 = "\" does not exist"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            goto L_0x05a6
        L_0x05a1:
            r4 = r1
            r5 = r7
            r3 = r8
            r16 = 3
        L_0x05a6:
            r1 = 0
        L_0x05a7:
            if (r1 != 0) goto L_0x0666
            java.lang.String r0 = "/"
            int r0 = r4.lastIndexOf(r0)
            r1 = 1
            int r0 = r0 + r1
            java.lang.String r1 = r4.substring(r0)
            r6 = 0
            r7 = 2
        L_0x05b7:
            if (r6 == r7) goto L_0x0666
            if (r6 != 0) goto L_0x05bd
            r0 = r1
            goto L_0x05c1
        L_0x05bd:
            java.lang.String r0 = MakeNativeLibraryExtensionCompatible(r1)
        L_0x05c1:
            java.lang.String r8 = "lib"
            boolean r8 = r0.startsWith(r8)
            if (r8 == 0) goto L_0x05cb
            r8 = 3
            goto L_0x05cc
        L_0x05cb:
            r8 = 0
        L_0x05cc:
            int r9 = r0.length()
            java.lang.String r10 = ".so"
            boolean r10 = r0.endsWith(r10)
            if (r10 == 0) goto L_0x05da
            r10 = 3
            goto L_0x05db
        L_0x05da:
            r10 = 0
        L_0x05db:
            int r9 = r9 - r10
            java.lang.String r8 = r0.substring(r8, r9)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0642 }
            r0.<init>()     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r9 = "Trying search method with name \""
            r0.append(r9)     // Catch:{ Throwable -> 0x0642 }
            r0.append(r8)     // Catch:{ Throwable -> 0x0642 }
            r0.append(r2)     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x0642 }
            android.util.Log.i(r3, r0)     // Catch:{ Throwable -> 0x0642 }
            java.io.File r0 = new java.io.File     // Catch:{ Throwable -> 0x0642 }
            android.app.Application r9 = m_application     // Catch:{ Throwable -> 0x0642 }
            android.content.Context r9 = r9.getApplicationContext()     // Catch:{ Throwable -> 0x0642 }
            android.content.pm.ApplicationInfo r9 = r9.getApplicationInfo()     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r9 = r9.nativeLibraryDir     // Catch:{ Throwable -> 0x0642 }
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0642 }
            r10.<init>()     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r11 = "lib"
            r10.append(r11)     // Catch:{ Throwable -> 0x0642 }
            r10.append(r8)     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r11 = ".so"
            r10.append(r11)     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r10 = r10.toString()     // Catch:{ Throwable -> 0x0642 }
            r0.<init>(r9, r10)     // Catch:{ Throwable -> 0x0642 }
            boolean r0 = r0.exists()     // Catch:{ Throwable -> 0x0642 }
            if (r0 == 0) goto L_0x0640
            java.lang.System.loadLibrary(r8)     // Catch:{ Throwable -> 0x0642 }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0642 }
            r0.<init>()     // Catch:{ Throwable -> 0x0642 }
            r0.append(r5)     // Catch:{ Throwable -> 0x0642 }
            r0.append(r4)     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r9 = "\" loaded"
            r0.append(r9)     // Catch:{ Throwable -> 0x0642 }
            java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x0642 }
            android.util.Log.i(r3, r0)     // Catch:{ Throwable -> 0x0642 }
            r9 = 1
            return r9
        L_0x0640:
            r9 = 1
            goto L_0x0662
        L_0x0642:
            r0 = move-exception
            r9 = 1
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            r10.append(r5)
            r10.append(r8)
            java.lang.String r8 = "\" failed "
            r10.append(r8)
            java.lang.String r0 = r0.getMessage()
            r10.append(r0)
            java.lang.String r0 = r10.toString()
            android.util.Log.i(r3, r0)
        L_0x0662:
            int r6 = r6 + 1
            goto L_0x05b7
        L_0x0666:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r5)
            r0.append(r4)
            java.lang.String r1 = "\" failed to load"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            r1 = 0
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.JNI_Environment.LoadNativeLibrary(java.lang.String):boolean");
    }

    public static INativeLibraryPathResolver SetPathResolver(INativeLibraryPathResolver iNativeLibraryPathResolver) {
        INativeLibraryPathResolver iNativeLibraryPathResolver2 = m_NativeLibraryPathResolver;
        m_NativeLibraryPathResolver = iNativeLibraryPathResolver;
        return iNativeLibraryPathResolver2;
    }

    public static void AddNativeLibrarySearchPath(String str) {
        String[] strArr = m_sNativeLibrarySearchPaths;
        int length = strArr != null ? strArr.length : 0;
        m_sNativeLibrarySearchPaths = new String[(length + 1)];
        for (int i = 0; i < length; i++) {
            m_sNativeLibrarySearchPaths[i] = strArr[i];
        }
        m_sNativeLibrarySearchPaths[length] = str;
    }

    private static String MakeNativeLibraryExtensionCompatible(String str) {
        if (str.endsWith(".so")) {
            return str;
        }
        do {
            str = str.substring(0, str.lastIndexOf(46));
        } while (!str.endsWith(".so"));
        return str;
    }

    public static boolean FindAndLoadNativeLibrary(String str) {
        boolean z;
        String str2;
        String str3;
        if (IsSharedObjectLoaded(str)) {
            return true;
        }
        int indexOf = str.indexOf("/");
        int indexOf2 = str.indexOf("\\");
        if (indexOf < 0 || (indexOf2 >= 0 && indexOf2 < indexOf)) {
            indexOf = indexOf2;
        }
        if (indexOf >= 0) {
            int indexOf3 = str.indexOf(":");
            if (indexOf3 < 0 || indexOf3 != indexOf - 1) {
                Log.i("com.valvesoftware.JNI_Environment.NativeSupport.FindAndLoadNativeLibrary", "Library \"" + str + "\" must either be unqualified or an absolute abstract \"game:/bin/$(PLATFORM_ARCH)/*****\" style path");
                return false;
            }
            z = true;
        } else {
            z = false;
        }
        if (z) {
            INativeLibraryPathResolver iNativeLibraryPathResolver = m_NativeLibraryPathResolver;
            str2 = iNativeLibraryPathResolver != null ? iNativeLibraryPathResolver.ResolveNativeLibraryPath(str) : str;
        } else if (m_sNativeLibrarySearchPaths != null) {
            str2 = null;
            for (int i = 0; i != 2; i++) {
                if (i == 0) {
                    str3 = str;
                } else {
                    str3 = MakeNativeLibraryExtensionCompatible(str);
                }
                String str4 = str2;
                for (int i2 = 0; i2 < m_sNativeLibrarySearchPaths.length; i2++) {
                    INativeLibraryPathResolver iNativeLibraryPathResolver2 = m_NativeLibraryPathResolver;
                    if (iNativeLibraryPathResolver2 != null) {
                        str4 = iNativeLibraryPathResolver2.ResolveNativeLibraryPath(m_sNativeLibrarySearchPaths[i2] + "/" + str3);
                    } else {
                        str4 = m_sNativeLibrarySearchPaths[i2] + "/" + str3;
                        if (!new File(str4).exists()) {
                            str4 = null;
                        }
                    }
                    if (str4 != null) {
                        break;
                    }
                }
                str2 = str4;
                if (str2 != null) {
                    break;
                }
            }
        } else {
            str2 = null;
        }
        if (str2 == null) {
            if (z) {
                str2 = "search:/" + str.substring(str.lastIndexOf("/") + 1);
            } else {
                str2 = "search:/" + str;
            }
        }
        return LoadNativeLibrary(str2);
    }

    public static File GetPrivatePath() {
        return m_sPrivatePath;
    }

    public static File GetPublicPath() {
        return m_sPublicPath;
    }

    public static boolean LogMemory() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) m_application.getBaseContext().getSystemService("activity")).getMemoryInfo(memoryInfo);
        Log.i("JBAPSYS", "MI avail " + (((double) memoryInfo.availMem) / 1048576.0d) + ", low Mem:" + memoryInfo.lowMemory + ", threshold: " + (((double) memoryInfo.threshold) / 1048576.0d) + ", total" + (((double) memoryInfo.totalMem) / 1048576.0d) + "[end]");
        Debug.MemoryInfo memoryInfo2 = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo2);
        Log.i(k_sSpewPackageName, "Memory log HeapMB=" + (((double) Debug.getNativeHeapSize()) / 1048576.0d) + ", HeapUsedMB=" + (((double) Debug.getNativeHeapAllocatedSize()) / 1048576.0d) + ", PSS MB=" + (((float) memoryInfo2.getTotalPss()) / 1024.0f) + ", dpss:" + memoryInfo2.dalvikPss + ", otherpss:" + memoryInfo2.otherPss + ", nativepss:" + memoryInfo2.nativePss);
        return true;
    }

    public static boolean OpenURL(String str) {
        Log.i(BuildConfig.APPLICATION_ID, "Opening URL: " + str);
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(str));
        intent.addFlags(268435456);
        m_application.startActivity(intent);
        return true;
    }

    public static long GetAvailableStorageBytes(File file) {
        StatFs statFs = new StatFs(file.getPath());
        return statFs.getBlockCountLong() * statFs.getBlockSizeLong();
    }

    public static boolean GetInAppPurchasePricingAsync(String str) {
        return Application.GetInstance().QuerySkuDetailsAsync(str);
    }

    public static boolean QueryExistingPurchases() {
        return Application.GetInstance().QueryExistingPurchases();
    }

    public static boolean PurchaseSku(String str) {
        return Application.GetInstance().PurchaseSku(str);
    }

    public static boolean ConsumePurchase(String str) {
        return Application.GetInstance().ConsumePurchase(str);
    }

    public static RequestQueue GetVolleyQueue() {
        return sm_VolleyQueue;
    }

    public static boolean HttpGet(String str) {
        Log.i(k_sSpewPackageName, "HttpGet( " + str + " )");
        GetVolleyQueue().add(new StringRequest(0, str, new Response.Listener<String>() {
            public String m_URL;

            public void onResponse(String str) {
                Log.i(JNI_Environment.k_sSpewPackageName, "HttpGet succeded for \"" + this.m_URL + "\" with response " + str);
            }

            public Response.Listener<String> init(String str) {
                this.m_URL = str;
                return this;
            }
        }.init(str), new Response.ErrorListener() {
            public String m_URL;

            public void onErrorResponse(VolleyError volleyError) {
                Log.i(JNI_Environment.k_sSpewPackageName, "HttpGet failed for \"" + this.m_URL + "\" with error " + volleyError.getMessage());
            }

            public Response.ErrorListener init(String str) {
                this.m_URL = str;
                return this;
            }
        }.init(str)));
        return true;
    }

    public static String GetDeviceCountryCode() {
        String str;
        TelephonyManager telephonyManager = (TelephonyManager) m_application.getBaseContext().getSystemService("phone");
        if (telephonyManager != null) {
            String simCountryIso = telephonyManager.getSimCountryIso();
            if (simCountryIso != null && simCountryIso.length() == 2) {
                return simCountryIso.toUpperCase();
            }
            String networkCountryIso = telephonyManager.getNetworkCountryIso();
            if (networkCountryIso != null && networkCountryIso.length() == 2) {
                return networkCountryIso.toUpperCase();
            }
        }
        if (Build.VERSION.SDK_INT >= 24) {
            str = m_application.getBaseContext().getResources().getConfiguration().getLocales().get(0).getCountry();
        } else {
            str = m_application.getBaseContext().getResources().getConfiguration().locale.getCountry();
        }
        return (str == null || str.length() != 2) ? "US" : str.toUpperCase();
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0046  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static long GetDeviceID() {
        /*
            java.lang.String r0 = "com.valvesoftware.JNI_Environment"
            java.lang.String r1 = "/DeviceID.bin"
            r2 = 0
            java.io.File r4 = new java.io.File     // Catch:{ Exception -> 0x003c }
            java.io.File r5 = GetPrivatePath()     // Catch:{ Exception -> 0x003c }
            r4.<init>(r5, r1)     // Catch:{ Exception -> 0x003c }
            java.util.Scanner r5 = new java.util.Scanner     // Catch:{ Exception -> 0x003c }
            r5.<init>(r4)     // Catch:{ Exception -> 0x003c }
            boolean r4 = r5.hasNextLong()     // Catch:{ Exception -> 0x003c }
            if (r4 == 0) goto L_0x001f
            long r4 = r5.nextLong()     // Catch:{ Exception -> 0x003c }
            goto L_0x0020
        L_0x001f:
            r4 = r2
        L_0x0020:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x003a }
            r6.<init>()     // Catch:{ Exception -> 0x003a }
            java.lang.String r7 = "GetDeviceID() read "
            r6.append(r7)     // Catch:{ Exception -> 0x003a }
            r6.append(r4)     // Catch:{ Exception -> 0x003a }
            java.lang.String r7 = " from file"
            r6.append(r7)     // Catch:{ Exception -> 0x003a }
            java.lang.String r6 = r6.toString()     // Catch:{ Exception -> 0x003a }
            android.util.Log.i(r0, r6)     // Catch:{ Exception -> 0x003a }
            goto L_0x0042
        L_0x003a:
            r6 = move-exception
            goto L_0x003f
        L_0x003c:
            r4 = move-exception
            r6 = r4
            r4 = r2
        L_0x003f:
            r6.printStackTrace()
        L_0x0042:
            int r6 = (r4 > r2 ? 1 : (r4 == r2 ? 0 : -1))
            if (r6 != 0) goto L_0x0092
            java.util.UUID r2 = java.util.UUID.randomUUID()
            long r3 = r2.getLeastSignificantBits()
            long r5 = r2.getMostSignificantBits()
            long r4 = r3 ^ r5
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "GetDeviceID() generated "
            r2.append(r3)
            r2.append(r4)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r0, r2)
            java.io.File r0 = new java.io.File     // Catch:{ Exception -> 0x008e }
            java.io.File r2 = GetPrivatePath()     // Catch:{ Exception -> 0x008e }
            r0.<init>(r2, r1)     // Catch:{ Exception -> 0x008e }
            java.io.FileWriter r1 = new java.io.FileWriter     // Catch:{ Exception -> 0x008e }
            r1.<init>(r0)     // Catch:{ Exception -> 0x008e }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x008e }
            r0.<init>()     // Catch:{ Exception -> 0x008e }
            r0.append(r4)     // Catch:{ Exception -> 0x008e }
            java.lang.String r2 = "\n"
            r0.append(r2)     // Catch:{ Exception -> 0x008e }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x008e }
            r1.write(r0)     // Catch:{ Exception -> 0x008e }
            r1.close()     // Catch:{ Exception -> 0x008e }
            goto L_0x0092
        L_0x008e:
            r0 = move-exception
            r0.printStackTrace()
        L_0x0092:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.JNI_Environment.GetDeviceID():long");
    }

    public static boolean IsSharedObjectLoaded(String str) {
        try {
            return IsSharedObjectLoadedNative(str);
        } catch (Throwable unused) {
            return false;
        }
    }

    /* JADX WARNING: Missing exception handler attribute for start block: B:51:0x00e5 */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0091 A[SYNTHETIC, Splitter:B:24:0x0091] */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0095  */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x00b8 A[SYNTHETIC, Splitter:B:37:0x00b8] */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00cb A[Catch:{ Throwable -> 0x0023, Throwable -> 0x002f, Throwable -> 0x006e, Throwable -> 0x0094, Throwable -> 0x00b5, Throwable -> 0x00c7, Throwable -> 0x00e5, Throwable -> 0x00eb, Throwable -> 0x00f2 }] */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00dc A[Catch:{ Throwable -> 0x0023, Throwable -> 0x002f, Throwable -> 0x006e, Throwable -> 0x0094, Throwable -> 0x00b5, Throwable -> 0x00c7, Throwable -> 0x00e5, Throwable -> 0x00eb, Throwable -> 0x00f2 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static long CRC32File(java.io.File r19) {
        /*
            r1 = r19
            java.lang.String r2 = "CRC32File file \""
            java.lang.String r3 = "com.valvesoftware.JNI_Environment"
            r4 = 0
            if (r1 == 0) goto L_0x00f7
            boolean r0 = r19.exists()
            if (r0 == 0) goto L_0x00f7
            long r6 = r19.length()
            int r0 = (r6 > r4 ? 1 : (r6 == r4 ? 0 : -1))
            if (r0 != 0) goto L_0x001a
            goto L_0x00f7
        L_0x001a:
            java.lang.String r0 = r19.getAbsolutePath()     // Catch:{ Throwable -> 0x0023 }
            long r0 = CRC32FileNative(r0)     // Catch:{ Throwable -> 0x0023 }
            return r0
        L_0x0023:
            long r6 = r19.length()
            r8 = 0
            java.io.FileInputStream r0 = new java.io.FileInputStream     // Catch:{ Throwable -> 0x002f }
            r0.<init>(r1)     // Catch:{ Throwable -> 0x002f }
            r9 = r0
            goto L_0x004f
        L_0x002f:
            r0 = move-exception
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            r9.append(r2)
            r9.append(r1)
            java.lang.String r10 = "\" stream exception "
            r9.append(r10)
            java.lang.String r0 = r0.getMessage()
            r9.append(r0)
            java.lang.String r0 = r9.toString()
            android.util.Log.i(r3, r0)
            r9 = r8
        L_0x004f:
            if (r9 != 0) goto L_0x0069
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r2)
            r0.append(r1)
            java.lang.String r1 = "\" no stream"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            return r4
        L_0x0069:
            java.nio.channels.FileChannel r0 = r9.getChannel()     // Catch:{ Throwable -> 0x006e }
            goto L_0x008f
        L_0x006e:
            r0 = move-exception
            r10 = r0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r2)
            r0.append(r1)
            java.lang.String r1 = "\" channel exception "
            r0.append(r1)
            java.lang.String r1 = r10.getMessage()
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r3, r0)
            r0 = r8
        L_0x008f:
            if (r0 != 0) goto L_0x0095
            r9.close()     // Catch:{ Throwable -> 0x0094 }
        L_0x0094:
            return r4
        L_0x0095:
            java.util.zip.CRC32 r1 = new java.util.zip.CRC32
            r1.<init>()
            r2 = r4
            r16 = r8
        L_0x009d:
            int r10 = (r2 > r6 ? 1 : (r2 == r6 ? 0 : -1))
            if (r10 >= 0) goto L_0x00ec
            r10 = 131072(0x20000, double:6.47582E-319)
            long r12 = r6 - r2
            long r17 = java.lang.Math.min(r10, r12)
            java.nio.channels.FileChannel$MapMode r11 = java.nio.channels.FileChannel.MapMode.READ_ONLY     // Catch:{ Throwable -> 0x00b5 }
            r10 = r0
            r12 = r2
            r14 = r17
            java.nio.MappedByteBuffer r10 = r10.map(r11, r12, r14)     // Catch:{ Throwable -> 0x00b5 }
            goto L_0x00b6
        L_0x00b5:
            r10 = r8
        L_0x00b6:
            if (r10 == 0) goto L_0x00c9
            r10.load()     // Catch:{ Throwable -> 0x00c7 }
            boolean r11 = r10.isLoaded()     // Catch:{ Throwable -> 0x00c7 }
            if (r11 == 0) goto L_0x00c9
            r1.update(r10)     // Catch:{ Throwable -> 0x00c7 }
            long r2 = r2 + r17
            goto L_0x009d
        L_0x00c7:
            goto L_0x009d
        L_0x00c9:
            if (r16 != 0) goto L_0x00d0
            r10 = 131072(0x20000, float:1.83671E-40)
            byte[] r10 = new byte[r10]     // Catch:{ Throwable -> 0x00c7 }
            goto L_0x00d2
        L_0x00d0:
            r10 = r16
        L_0x00d2:
            java.nio.ByteBuffer r11 = java.nio.ByteBuffer.wrap(r10)     // Catch:{ Throwable -> 0x00e5 }
            int r11 = r0.read(r11, r2)     // Catch:{ Throwable -> 0x00e5 }
            if (r11 <= 0) goto L_0x00e5
            r12 = 0
            r1.update(r10, r12, r11)     // Catch:{ Throwable -> 0x00e5 }
            long r11 = (long) r11
            long r2 = r2 + r11
            r16 = r10
            goto L_0x009d
        L_0x00e5:
            r0.close()     // Catch:{ Throwable -> 0x00eb }
            r9.close()     // Catch:{ Throwable -> 0x00eb }
        L_0x00eb:
            return r4
        L_0x00ec:
            r0.close()     // Catch:{ Throwable -> 0x00f2 }
            r9.close()     // Catch:{ Throwable -> 0x00f2 }
        L_0x00f2:
            long r0 = r1.getValue()
            return r0
        L_0x00f7:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.JNI_Environment.CRC32File(java.io.File):long");
    }
}
