package com.valvesoftware;

import java.lang.reflect.Field;

public class Resources {
    public static Class<?> R_array;
    public static Class<?> R_bool;
    public static Class<?> R_color;
    public static Class<?> R_dimen;
    public static Class<?> R_drawable;
    public static Class<?> R_font;
    public static Class<?> R_id;
    public static Class<?> R_integer;
    public static Class<?> R_string;
    public static android.content.res.Resources app_resources;

    /* JADX WARNING: Can't wrap try/catch for region: R(18:3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|(3:20|21|23)) */
    /* JADX WARNING: Can't wrap try/catch for region: R(20:3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|23) */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:10:0x006e */
    /* JADX WARNING: Missing exception handler attribute for start block: B:12:0x0085 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:14:0x009c */
    /* JADX WARNING: Missing exception handler attribute for start block: B:16:0x00b3 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:18:0x00ca */
    /* JADX WARNING: Missing exception handler attribute for start block: B:20:0x00e1 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:6:0x0040 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:8:0x0057 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void Initialize(android.app.Application r4) {
        /*
            android.content.res.Resources r0 = app_resources
            if (r0 == 0) goto L_0x0005
            return
        L_0x0005:
            android.content.res.Resources r0 = r4.getResources()
            app_resources = r0
            java.lang.Class r0 = r4.getClass()
            java.lang.ClassLoader r0 = r0.getClassLoader()
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r4 = r4.getPackageName()
            r1.append(r4)
            java.lang.String r4 = ".R$"
            r1.append(r4)
            java.lang.String r4 = r1.toString()
            r1 = 0
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0040 }
            r2.<init>()     // Catch:{ Throwable -> 0x0040 }
            r2.append(r4)     // Catch:{ Throwable -> 0x0040 }
            java.lang.String r3 = "array"
            r2.append(r3)     // Catch:{ Throwable -> 0x0040 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x0040 }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x0040 }
            R_array = r2     // Catch:{ Throwable -> 0x0040 }
        L_0x0040:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0057 }
            r2.<init>()     // Catch:{ Throwable -> 0x0057 }
            r2.append(r4)     // Catch:{ Throwable -> 0x0057 }
            java.lang.String r3 = "bool"
            r2.append(r3)     // Catch:{ Throwable -> 0x0057 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x0057 }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x0057 }
            R_bool = r2     // Catch:{ Throwable -> 0x0057 }
        L_0x0057:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x006e }
            r2.<init>()     // Catch:{ Throwable -> 0x006e }
            r2.append(r4)     // Catch:{ Throwable -> 0x006e }
            java.lang.String r3 = "color"
            r2.append(r3)     // Catch:{ Throwable -> 0x006e }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x006e }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x006e }
            R_color = r2     // Catch:{ Throwable -> 0x006e }
        L_0x006e:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0085 }
            r2.<init>()     // Catch:{ Throwable -> 0x0085 }
            r2.append(r4)     // Catch:{ Throwable -> 0x0085 }
            java.lang.String r3 = "dimen"
            r2.append(r3)     // Catch:{ Throwable -> 0x0085 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x0085 }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x0085 }
            R_dimen = r2     // Catch:{ Throwable -> 0x0085 }
        L_0x0085:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x009c }
            r2.<init>()     // Catch:{ Throwable -> 0x009c }
            r2.append(r4)     // Catch:{ Throwable -> 0x009c }
            java.lang.String r3 = "id"
            r2.append(r3)     // Catch:{ Throwable -> 0x009c }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x009c }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x009c }
            R_id = r2     // Catch:{ Throwable -> 0x009c }
        L_0x009c:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00b3 }
            r2.<init>()     // Catch:{ Throwable -> 0x00b3 }
            r2.append(r4)     // Catch:{ Throwable -> 0x00b3 }
            java.lang.String r3 = "integer"
            r2.append(r3)     // Catch:{ Throwable -> 0x00b3 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x00b3 }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x00b3 }
            R_integer = r2     // Catch:{ Throwable -> 0x00b3 }
        L_0x00b3:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00ca }
            r2.<init>()     // Catch:{ Throwable -> 0x00ca }
            r2.append(r4)     // Catch:{ Throwable -> 0x00ca }
            java.lang.String r3 = "string"
            r2.append(r3)     // Catch:{ Throwable -> 0x00ca }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x00ca }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x00ca }
            R_string = r2     // Catch:{ Throwable -> 0x00ca }
        L_0x00ca:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00e1 }
            r2.<init>()     // Catch:{ Throwable -> 0x00e1 }
            r2.append(r4)     // Catch:{ Throwable -> 0x00e1 }
            java.lang.String r3 = "drawable"
            r2.append(r3)     // Catch:{ Throwable -> 0x00e1 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x00e1 }
            java.lang.Class r2 = java.lang.Class.forName(r2, r1, r0)     // Catch:{ Throwable -> 0x00e1 }
            R_drawable = r2     // Catch:{ Throwable -> 0x00e1 }
        L_0x00e1:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00f8 }
            r2.<init>()     // Catch:{ Throwable -> 0x00f8 }
            r2.append(r4)     // Catch:{ Throwable -> 0x00f8 }
            java.lang.String r4 = "font"
            r2.append(r4)     // Catch:{ Throwable -> 0x00f8 }
            java.lang.String r4 = r2.toString()     // Catch:{ Throwable -> 0x00f8 }
            java.lang.Class r4 = java.lang.Class.forName(r4, r1, r0)     // Catch:{ Throwable -> 0x00f8 }
            R_font = r4     // Catch:{ Throwable -> 0x00f8 }
        L_0x00f8:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.Resources.Initialize(android.app.Application):void");
    }

    public static boolean[] GetBoolean(String str) {
        int FindFieldId = FindFieldId(R_bool, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new boolean[]{app_resources.getBoolean(FindFieldId)};
    }

    public static int[] GetColor(String str) {
        int FindFieldId = FindFieldId(R_color, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{app_resources.getColor(FindFieldId)};
    }

    public static float[] GetDimension(String str) {
        int FindFieldId = FindFieldId(R_dimen, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new float[]{app_resources.getDimension(FindFieldId)};
    }

    public static int[] GetID(String str) {
        int FindFieldId = FindFieldId(R_id, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{FindFieldId};
    }

    public static int[] GetInteger(String str) {
        int FindFieldId = FindFieldId(R_integer, str);
        if (FindFieldId < 0) {
            return null;
        }
        return new int[]{app_resources.getInteger(FindFieldId)};
    }

    public static String GetString(String str) {
        int FindFieldId = FindFieldId(R_string, str);
        if (FindFieldId < 0) {
            return null;
        }
        return app_resources.getString(FindFieldId);
    }

    public static String GetStringSafe(String str, String str2) {
        int FindFieldId = FindFieldId(R_string, str);
        return FindFieldId < 0 ? str2 : app_resources.getString(FindFieldId);
    }

    public static String GetStringSafe(String str) {
        return GetStringSafe(str, str);
    }

    public static String[] GetStringArray(String str) {
        int FindFieldId = FindFieldId(R_array, str);
        if (FindFieldId < 0) {
            return null;
        }
        return app_resources.getStringArray(FindFieldId);
    }

    public static int[] GetDrawable(String str) {
        try {
            Field FindField = FindField(R_drawable, str);
            if (FindField != null) {
                return new int[]{FindField.getInt((Object) null)};
            }
        } catch (Throwable unused) {
        }
        return null;
    }

    public static int[] GetFont(String str) {
        try {
            Field FindField = FindField(R_font, str);
            if (FindField != null) {
                return new int[]{FindField.getInt((Object) null)};
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
                return FindField.getInt((Object) null);
            }
            return -1;
        } catch (Throwable unused) {
            return -1;
        }
    }
}
