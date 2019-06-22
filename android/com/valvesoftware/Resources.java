package com.valvesoftware;

import java.lang.reflect.Field;

public class Resources {
    public static Class<?> R_array;
    public static Class<?> R_bool;
    public static Class<?> R_color;
    public static Class<?> R_dimen;
    public static Class<?> R_drawable;
    public static Class<?> R_id;
    public static Class<?> R_integer;
    public static Class<?> R_string;
    public static android.content.res.Resources app_resources;
    private static boolean s_bInitialized;

    /* JADX WARNING: Can't wrap try/catch for region: R(19:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|19) */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0099 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x00b0 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:15:0x00c7 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x003d */
    /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x0054 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x006b */
    /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0082 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static <App_t extends android.app.Application> void Initialize() {
        /*
            android.app.Application r0 = com.valvesoftware.JNI_Environment.m_application
            android.content.res.Resources r1 = r0.getResources()
            app_resources = r1
            java.lang.Class r1 = r0.getClass()
            java.lang.ClassLoader r1 = r1.getClassLoader()
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r0 = r0.getPackageName()
            r2.append(r0)
            java.lang.String r0 = ".R$"
            r2.append(r0)
            java.lang.String r0 = r2.toString()
            r2 = 0
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x003d }
            r3.<init>()     // Catch:{ Throwable -> 0x003d }
            r3.append(r0)     // Catch:{ Throwable -> 0x003d }
            java.lang.String r4 = "array"
            r3.append(r4)     // Catch:{ Throwable -> 0x003d }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x003d }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x003d }
            R_array = r3     // Catch:{ Throwable -> 0x003d }
        L_0x003d:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0054 }
            r3.<init>()     // Catch:{ Throwable -> 0x0054 }
            r3.append(r0)     // Catch:{ Throwable -> 0x0054 }
            java.lang.String r4 = "bool"
            r3.append(r4)     // Catch:{ Throwable -> 0x0054 }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x0054 }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x0054 }
            R_bool = r3     // Catch:{ Throwable -> 0x0054 }
        L_0x0054:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x006b }
            r3.<init>()     // Catch:{ Throwable -> 0x006b }
            r3.append(r0)     // Catch:{ Throwable -> 0x006b }
            java.lang.String r4 = "color"
            r3.append(r4)     // Catch:{ Throwable -> 0x006b }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x006b }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x006b }
            R_color = r3     // Catch:{ Throwable -> 0x006b }
        L_0x006b:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0082 }
            r3.<init>()     // Catch:{ Throwable -> 0x0082 }
            r3.append(r0)     // Catch:{ Throwable -> 0x0082 }
            java.lang.String r4 = "dimen"
            r3.append(r4)     // Catch:{ Throwable -> 0x0082 }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x0082 }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x0082 }
            R_dimen = r3     // Catch:{ Throwable -> 0x0082 }
        L_0x0082:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0099 }
            r3.<init>()     // Catch:{ Throwable -> 0x0099 }
            r3.append(r0)     // Catch:{ Throwable -> 0x0099 }
            java.lang.String r4 = "id"
            r3.append(r4)     // Catch:{ Throwable -> 0x0099 }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x0099 }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x0099 }
            R_id = r3     // Catch:{ Throwable -> 0x0099 }
        L_0x0099:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00b0 }
            r3.<init>()     // Catch:{ Throwable -> 0x00b0 }
            r3.append(r0)     // Catch:{ Throwable -> 0x00b0 }
            java.lang.String r4 = "integer"
            r3.append(r4)     // Catch:{ Throwable -> 0x00b0 }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x00b0 }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x00b0 }
            R_integer = r3     // Catch:{ Throwable -> 0x00b0 }
        L_0x00b0:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00c7 }
            r3.<init>()     // Catch:{ Throwable -> 0x00c7 }
            r3.append(r0)     // Catch:{ Throwable -> 0x00c7 }
            java.lang.String r4 = "string"
            r3.append(r4)     // Catch:{ Throwable -> 0x00c7 }
            java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x00c7 }
            java.lang.Class r3 = java.lang.Class.forName(r3, r2, r1)     // Catch:{ Throwable -> 0x00c7 }
            R_string = r3     // Catch:{ Throwable -> 0x00c7 }
        L_0x00c7:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00de }
            r3.<init>()     // Catch:{ Throwable -> 0x00de }
            r3.append(r0)     // Catch:{ Throwable -> 0x00de }
            java.lang.String r0 = "drawable"
            r3.append(r0)     // Catch:{ Throwable -> 0x00de }
            java.lang.String r0 = r3.toString()     // Catch:{ Throwable -> 0x00de }
            java.lang.Class r0 = java.lang.Class.forName(r0, r2, r1)     // Catch:{ Throwable -> 0x00de }
            R_drawable = r0     // Catch:{ Throwable -> 0x00de }
        L_0x00de:
            r0 = 1
            s_bInitialized = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.Resources.Initialize():void");
    }

    public static boolean[] GetBoolean(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_bool, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new boolean[]{app_resources.getBoolean(FindFieldId)};
    }

    public static int[] GetColor(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_color, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{app_resources.getColor(FindFieldId)};
    }

    public static float[] GetDimension(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_dimen, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new float[]{app_resources.getDimension(FindFieldId)};
    }

    public static int[] GetID(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_id, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{FindFieldId};
    }

    public static int[] GetInteger(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_integer, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{app_resources.getInteger(FindFieldId)};
    }

    public static String GetString(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_string, str);
        if (FindFieldId < 0) {
            return null;
        }
        return app_resources.getString(FindFieldId);
    }

    public static String GetStringSafe(String str, String str2) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_string, str);
        return FindFieldId < 0 ? str2 : app_resources.getString(FindFieldId);
    }

    public static String GetStringSafe(String str) {
        return GetStringSafe(str, str);
    }

    public static String[] GetStringArray(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        int FindFieldId = FindFieldId(R_array, str);
        if (FindFieldId < 0) {
            return null;
        }
        return app_resources.getStringArray(FindFieldId);
    }

    public static int[] GetDrawable(String str) {
        if (!s_bInitialized) {
            Initialize();
        }
        try {
            Field FindField = FindField(R_drawable, str);
            if (FindField != null) {
                return new int[]{FindField.getInt(null)};
            }
        } catch (Throwable unused) {
        }
        return null;
    }

    private static Field FindField(Class<?> cls, String str) {
        if (!(cls == null || str == null)) {
            try {
                return cls.getField(str);
            } catch (Throwable unused) {
            }
        }
        return null;
    }

    private static int FindFieldId(Class<?> cls, String str) {
        try {
            Field FindField = FindField(cls, str);
            if (FindField != null) {
                return FindField.getInt(null);
            }
        } catch (Throwable unused) {
        }
        return -1;
    }
}
