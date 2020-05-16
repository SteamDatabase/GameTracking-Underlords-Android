package com.valvesoftware;

import java.io.File;
import java.io.FileOutputStream;
import java.util.StringTokenizer;
import org.json.JSONObject;

public abstract class Configuration {
    private static final int GLOBAL_CONFIG_VERSION = 1;
    private static JSONObject sm_GlobalConfig;

    public static JSONObject GetGlobalKey(String str, boolean z) {
        if (sm_GlobalConfig != null || LoadGlobalConfiguration()) {
            return GetSubKey(sm_GlobalConfig, str, z);
        }
        return null;
    }

    public static JSONObject GetSubKey(JSONObject jSONObject, String str, boolean z) {
        if (jSONObject == null || str == null) {
            return null;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(str, "/", false);
        if (!stringTokenizer.hasMoreTokens()) {
            return null;
        }
        while (jSONObject != null && stringTokenizer.hasMoreTokens()) {
            String nextToken = stringTokenizer.nextToken();
            JSONObject optJSONObject = jSONObject.optJSONObject(nextToken);
            if (z && optJSONObject == null) {
                JSONObject jSONObject2 = new JSONObject();
                try {
                    jSONObject.put(nextToken, (Object) jSONObject2);
                    jSONObject = jSONObject2;
                } catch (Throwable unused) {
                }
            }
            jSONObject = optJSONObject;
        }
        return jSONObject;
    }

    public static void MarkGlobalConfigurationDirty() {
        JSONObject jSONObject = sm_GlobalConfig;
        if (jSONObject != null) {
            SaveConfigurationToFile(jSONObject, new File(JNI_Environment.GetPrivatePath(), "/Configuration.json"));
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0082 A[SYNTHETIC, Splitter:B:15:0x0082] */
    /* JADX WARNING: Removed duplicated region for block: B:24:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static org.json.JSONObject LoadConfigurationFromFile(java.io.File r8) {
        /*
            r0 = 0
            java.lang.String r1 = "Config file \""
            java.lang.String r2 = "com.valvesoftware.Configuration"
            if (r8 == 0) goto L_0x0064
            boolean r3 = r8.exists()
            if (r3 == 0) goto L_0x0064
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r4 = r8.getPath()
            r3.append(r4)
            java.lang.String r4 = "\" exists"
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            android.util.Log.i(r2, r3)
            long r3 = r8.length()
            int r4 = (int) r3
            byte[] r3 = new byte[r4]
            r5 = 0
            java.io.FileInputStream r6 = new java.io.FileInputStream     // Catch:{ Throwable -> 0x0041 }
            java.lang.String r7 = r8.getPath()     // Catch:{ Throwable -> 0x0041 }
            r6.<init>(r7)     // Catch:{ Throwable -> 0x0041 }
            int r6 = r6.read(r3, r5, r4)     // Catch:{ Throwable -> 0x0041 }
            if (r6 != r4) goto L_0x0041
            r4 = 1
            r5 = 1
        L_0x0041:
            if (r5 == 0) goto L_0x007f
            java.lang.String r4 = new java.lang.String
            r4.<init>(r3)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r1 = r8.getPath()
            r3.append(r1)
            java.lang.String r1 = "\" was read"
            r3.append(r1)
            java.lang.String r1 = r3.toString()
            android.util.Log.i(r2, r1)
            goto L_0x0080
        L_0x0064:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r1 = r8.getPath()
            r3.append(r1)
            java.lang.String r1 = "\" does not exist"
            r3.append(r1)
            java.lang.String r1 = r3.toString()
            android.util.Log.i(r2, r1)
        L_0x007f:
            r4 = r0
        L_0x0080:
            if (r4 == 0) goto L_0x00a5
            org.json.JSONObject r1 = new org.json.JSONObject     // Catch:{ Throwable -> 0x00a5 }
            r1.<init>((java.lang.String) r4)     // Catch:{ Throwable -> 0x00a5 }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00a4 }
            r0.<init>()     // Catch:{ Throwable -> 0x00a4 }
            java.lang.String r3 = "Config json \""
            r0.append(r3)     // Catch:{ Throwable -> 0x00a4 }
            java.lang.String r8 = r8.getPath()     // Catch:{ Throwable -> 0x00a4 }
            r0.append(r8)     // Catch:{ Throwable -> 0x00a4 }
            java.lang.String r8 = "\" loaded from file data"
            r0.append(r8)     // Catch:{ Throwable -> 0x00a4 }
            java.lang.String r8 = r0.toString()     // Catch:{ Throwable -> 0x00a4 }
            android.util.Log.i(r2, r8)     // Catch:{ Throwable -> 0x00a4 }
        L_0x00a4:
            r0 = r1
        L_0x00a5:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.Configuration.LoadConfigurationFromFile(java.io.File):org.json.JSONObject");
    }

    public static void SaveConfigurationToFile(JSONObject jSONObject, File file) {
        String str;
        FileOutputStream fileOutputStream;
        if (file.exists()) {
            file.delete();
        }
        if (jSONObject != null && jSONObject.length() > 0) {
            file.getParentFile().mkdirs();
            try {
                str = jSONObject.toString(4);
            } catch (Throwable unused) {
                str = null;
            }
            if (str != null) {
                try {
                    fileOutputStream = new FileOutputStream(file.getPath(), false);
                } catch (Throwable unused2) {
                    fileOutputStream = null;
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.write(str.getBytes());
                        fileOutputStream.close();
                    } catch (Throwable unused3) {
                    }
                }
            }
        }
    }

    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:15:0x005c */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean LoadGlobalConfiguration() {
        /*
            org.json.JSONObject r0 = sm_GlobalConfig
            r1 = 1
            if (r0 == 0) goto L_0x0006
            return r1
        L_0x0006:
            java.io.File r0 = new java.io.File
            java.io.File r2 = com.valvesoftware.JNI_Environment.GetPrivatePath()
            java.lang.String r3 = "/Configuration.json"
            r0.<init>(r2, r3)
            org.json.JSONObject r2 = LoadConfigurationFromFile(r0)
            java.lang.String r3 = "version"
            java.lang.String r4 = ".meta"
            if (r2 == 0) goto L_0x002a
            org.json.JSONObject r5 = r2.optJSONObject(r4)
            if (r5 == 0) goto L_0x002a
            r6 = -1
            int r5 = r5.optInt(r3, r6)
            if (r5 != r1) goto L_0x002a
            sm_GlobalConfig = r2
        L_0x002a:
            org.json.JSONObject r2 = sm_GlobalConfig
            if (r2 != 0) goto L_0x0063
            org.json.JSONObject r2 = new org.json.JSONObject
            r2.<init>()
            sm_GlobalConfig = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r5 = "Config json \""
            r2.append(r5)
            java.lang.String r0 = r0.getPath()
            r2.append(r0)
            java.lang.String r0 = "\" created empty"
            r2.append(r0)
            java.lang.String r0 = r2.toString()
            java.lang.String r2 = "com.valvesoftware.Configuration"
            android.util.Log.i(r2, r0)
            org.json.JSONObject r0 = new org.json.JSONObject
            r0.<init>()
            r0.put((java.lang.String) r3, (int) r1)     // Catch:{ Throwable -> 0x005c }
        L_0x005c:
            org.json.JSONObject r2 = sm_GlobalConfig     // Catch:{ Throwable -> 0x0062 }
            r2.put((java.lang.String) r4, (java.lang.Object) r0)     // Catch:{ Throwable -> 0x0062 }
            goto L_0x0063
        L_0x0062:
        L_0x0063:
            org.json.JSONObject r0 = sm_GlobalConfig
            if (r0 == 0) goto L_0x0068
            goto L_0x0069
        L_0x0068:
            r1 = 0
        L_0x0069:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.Configuration.LoadGlobalConfiguration():boolean");
    }
}
