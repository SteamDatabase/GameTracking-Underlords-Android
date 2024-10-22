package com.valvesoftware;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class VulkanWhitelist {
    private static String TAG = "com.valvesoftware.VulkanWhitelist";
    private ArrayList<DeviceInfo> m_compatibleDevices;

    public static boolean DeviceIsVulkanCompatible(JSONObject jSONObject, Context context) {
        if (jSONObject == null) {
            Log.e(TAG, "jsonManifest is null.");
            return false;
        } else if (context == null) {
            Log.e(TAG, "appContext is null.");
            return false;
        } else {
            try {
                VulkanWhitelist vulkanWhitelist = new VulkanWhitelist();
                JSONArray jSONArray = jSONObject.getJSONArray("vulkan_whitelist");
                if (jSONArray == null) {
                    Log.e(TAG, "Unable to get JSONArray \"vulkan_whitelist\".");
                    return false;
                } else if (!vulkanWhitelist.InitializeFromJSONObject(jSONArray).booleanValue()) {
                    Log.e(TAG, "VulkanWhitelist failed to load from JSONObject");
                    return false;
                } else if (!vulkanWhitelist.IsDeviceCompatible(context).booleanValue()) {
                    return false;
                } else {
                    Log.i(TAG, "Device is Vulkan compatible.");
                    return true;
                }
            } catch (Exception e) {
                String str = TAG;
                Log.e(str, "VulkanWhitelist Exception: " + e.toString());
                return false;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x006e  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0091  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0094 A[SYNTHETIC, Splitter:B:14:0x0094] */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x00e2  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0101  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Boolean InitializeFromJSONFile(java.lang.String r10) {
        /*
            r9 = this;
            java.lang.String r0 = "Config json \""
            java.io.File r1 = new java.io.File
            r1.<init>(r10)
            boolean r10 = r1.exists()
            r2 = 0
            r3 = 0
            if (r10 == 0) goto L_0x00e0
            java.lang.String r10 = TAG
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Vulkan white list file \""
            r4.append(r5)
            java.lang.String r6 = r1.getPath()
            r4.append(r6)
            java.lang.String r6 = "\" exists"
            r4.append(r6)
            java.lang.String r4 = r4.toString()
            android.util.Log.i(r10, r4)
            long r6 = r1.length()
            int r10 = (int) r6
            byte[] r4 = new byte[r10]
            java.io.FileInputStream r6 = new java.io.FileInputStream     // Catch:{ Exception -> 0x0046 }
            java.lang.String r7 = r1.getPath()     // Catch:{ Exception -> 0x0046 }
            r6.<init>(r7)     // Catch:{ Exception -> 0x0046 }
            int r6 = r6.read(r4, r3, r10)     // Catch:{ Exception -> 0x0046 }
            if (r6 != r10) goto L_0x006b
            r10 = 1
            goto L_0x006c
        L_0x0046:
            r10 = move-exception
            java.lang.String r6 = TAG
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            r7.append(r5)
            java.lang.String r8 = r1.getPath()
            r7.append(r8)
            java.lang.String r8 = "\" could not be read: "
            r7.append(r8)
            java.lang.String r10 = r10.toString()
            r7.append(r10)
            java.lang.String r10 = r7.toString()
            android.util.Log.e(r6, r10)
        L_0x006b:
            r10 = 0
        L_0x006c:
            if (r10 == 0) goto L_0x0091
            java.lang.String r10 = new java.lang.String
            r10.<init>(r4)
            java.lang.String r4 = TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            r6.append(r5)
            java.lang.String r5 = r1.getPath()
            r6.append(r5)
            java.lang.String r5 = "\" was read"
            r6.append(r5)
            java.lang.String r5 = r6.toString()
            android.util.Log.i(r4, r5)
            goto L_0x0092
        L_0x0091:
            r10 = r2
        L_0x0092:
            if (r10 == 0) goto L_0x00e0
            org.json.JSONObject r4 = new org.json.JSONObject     // Catch:{ Exception -> 0x00bb }
            r4.<init>((java.lang.String) r10)     // Catch:{ Exception -> 0x00bb }
            java.lang.String r10 = TAG     // Catch:{ Exception -> 0x00b8 }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x00b8 }
            r2.<init>()     // Catch:{ Exception -> 0x00b8 }
            r2.append(r0)     // Catch:{ Exception -> 0x00b8 }
            java.lang.String r5 = r1.getPath()     // Catch:{ Exception -> 0x00b8 }
            r2.append(r5)     // Catch:{ Exception -> 0x00b8 }
            java.lang.String r5 = "\" loaded from file data"
            r2.append(r5)     // Catch:{ Exception -> 0x00b8 }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x00b8 }
            android.util.Log.i(r10, r2)     // Catch:{ Exception -> 0x00b8 }
            r2 = r4
            goto L_0x00e0
        L_0x00b8:
            r10 = move-exception
            r2 = r4
            goto L_0x00bc
        L_0x00bb:
            r10 = move-exception
        L_0x00bc:
            java.lang.String r4 = TAG
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            r5.append(r0)
            java.lang.String r0 = r1.getPath()
            r5.append(r0)
            java.lang.String r0 = "\" failed to create JSONObject: "
            r5.append(r0)
            java.lang.String r10 = r10.toString()
            r5.append(r10)
            java.lang.String r10 = r5.toString()
            android.util.Log.e(r4, r10)
        L_0x00e0:
            if (r2 != 0) goto L_0x0101
            java.lang.String r10 = TAG
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r2 = "Failed to load \""
            r0.append(r2)
            java.lang.String r1 = r1.getPath()
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.e(r10, r0)
            java.lang.Boolean r10 = java.lang.Boolean.valueOf(r3)
            return r10
        L_0x0101:
            java.lang.String r10 = "vulkan_whitelist"
            org.json.JSONArray r10 = r2.getJSONArray(r10)     // Catch:{ Exception -> 0x010c }
            java.lang.Boolean r10 = r9.InitializeFromJSONObject(r10)
            return r10
        L_0x010c:
            r10 = move-exception
            java.lang.String r0 = TAG
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r4 = "Failed to find `vulkan_whitelist' in \""
            r2.append(r4)
            java.lang.String r1 = r1.getPath()
            r2.append(r1)
            java.lang.String r1 = " exception: "
            r2.append(r1)
            java.lang.String r10 = r10.toString()
            r2.append(r10)
            java.lang.String r10 = r2.toString()
            android.util.Log.e(r0, r10)
            java.lang.Boolean r10 = java.lang.Boolean.valueOf(r3)
            return r10
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.VulkanWhitelist.InitializeFromJSONFile(java.lang.String):java.lang.Boolean");
    }

    public Boolean InitializeFromJSONObject(JSONArray jSONArray) {
        try {
            this.m_compatibleDevices = new ArrayList<>();
            for (int i = 0; i < jSONArray.length(); i++) {
                DeviceInfo PopulateFromJSON = DeviceInfo.PopulateFromJSON(jSONArray.getJSONObject(i));
                if (PopulateFromJSON != null) {
                    this.m_compatibleDevices.add(PopulateFromJSON);
                }
            }
            return true;
        } catch (Exception e) {
            String str = TAG;
            Log.e(str, "Failed to parse Vulkan whitelist exception: " + e.toString());
            return false;
        }
    }

    public Boolean IsDeviceCompatible(Context context) {
        DeviceInfo CollectThisDeviceInfo = CollectThisDeviceInfo(context);
        if (CollectThisDeviceInfo == null) {
            return false;
        }
        if (this.m_compatibleDevices != null) {
            for (int i = 0; i < this.m_compatibleDevices.size(); i++) {
                if (DeviceInfo.DevicesCompatible(CollectThisDeviceInfo, this.m_compatibleDevices.get(i))) {
                    Log.i(TAG, "Device determined to be compatible with Vulkan.");
                    return true;
                }
            }
        }
        Log.i(TAG, "Device determined not to be compatible with Vulkan.");
        return false;
    }

    private DeviceInfo CollectThisDeviceInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        if (!GetOpenGLDriverInfo(sb, sb2).booleanValue()) {
            Log.e(TAG, "GetOpenGLDriverInfo() failed  - can't determine device info");
            return null;
        }
        DeviceInfo deviceInfo = new DeviceInfo();
        int i = Build.VERSION.SDK_INT;
        deviceInfo.m_nMaxOS = i;
        deviceInfo.m_nMinOS = i;
        deviceInfo.m_sDeviceName = Build.MANUFACTURER + " " + Build.PRODUCT;
        String sb3 = sb2.toString();
        deviceInfo.m_sMaxDriverVersion = sb3;
        deviceInfo.m_sMinDriverVersion = sb3;
        deviceInfo.m_sRenderer = sb.toString();
        String str = TAG;
        Log.i(str, "DeviceInfo OS: " + deviceInfo.m_nMinOS);
        String str2 = TAG;
        Log.i(str2, "DeviceInfo Device Name: " + deviceInfo.m_sDeviceName);
        String str3 = TAG;
        Log.i(str3, "DeviceInfo Driver Version: " + deviceInfo.m_sMinDriverVersion);
        String str4 = TAG;
        Log.i(str4, "DeviceInfo Renderer: " + deviceInfo.m_sRenderer);
        return deviceInfo;
    }

    private Boolean GetOpenGLDriverInfo(StringBuilder sb, StringBuilder sb2) {
        EGLDisplay eglGetDisplay = EGL14.eglGetDisplay(0);
        if (eglGetDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed.");
            return false;
        }
        int[] iArr = new int[2];
        if (!EGL14.eglInitialize(eglGetDisplay, iArr, 0, iArr, 1)) {
            Log.e(TAG, "eglInitialize failed.");
            return false;
        }
        EGLConfig[] eGLConfigArr = new EGLConfig[1];
        if (!EGL14.eglChooseConfig(eglGetDisplay, new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 68, 12344}, 0, eGLConfigArr, 0, eGLConfigArr.length, new int[1], 0)) {
            EGL14.eglTerminate(eglGetDisplay);
            Log.e(TAG, "eglChooseConfig failed.");
            return false;
        }
        EGLContext eglCreateContext = EGL14.eglCreateContext(eglGetDisplay, eGLConfigArr[0], EGL14.EGL_NO_CONTEXT, new int[]{12440, 3, 12344}, 0);
        if (eglCreateContext == EGL14.EGL_NO_CONTEXT || EGL14.eglGetError() != 12288) {
            EGL14.eglTerminate(eglGetDisplay);
            Log.e(TAG, "eglCreateContext failed.");
            return false;
        }
        EGLSurface eglCreatePbufferSurface = EGL14.eglCreatePbufferSurface(eglGetDisplay, eGLConfigArr[0], new int[]{12375, 4, 12374, 4, 12344}, 0);
        if (eglCreatePbufferSurface == null) {
            EGL14.eglDestroyContext(eglGetDisplay, eglCreateContext);
            EGL14.eglDestroySurface(eglGetDisplay, eglCreatePbufferSurface);
            EGL14.eglTerminate(eglGetDisplay);
            Log.e(TAG, "eglCreatePbufferSurface failed.");
            return false;
        } else if (!EGL14.eglMakeCurrent(eglGetDisplay, eglCreatePbufferSurface, eglCreatePbufferSurface, eglCreateContext)) {
            EGL14.eglDestroyContext(eglGetDisplay, eglCreateContext);
            EGL14.eglDestroySurface(eglGetDisplay, eglCreatePbufferSurface);
            EGL14.eglTerminate(eglGetDisplay);
            Log.e(TAG, "eglMakeCurrent failed.");
            return false;
        } else {
            String glGetString = GLES20.glGetString(7938);
            String glGetString2 = GLES20.glGetString(7937);
            EGL14.eglDestroyContext(eglGetDisplay, eglCreateContext);
            EGL14.eglDestroySurface(eglGetDisplay, eglCreatePbufferSurface);
            EGL14.eglTerminate(eglGetDisplay);
            if (glGetString == null || glGetString2 == null) {
                return false;
            }
            sb.append(glGetString2);
            sb2.append(glGetString);
            return true;
        }
    }
}
