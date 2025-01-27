package com.valvesoftware;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import org.json.JSONObject;

public class PatchSystem {
    private static final String k_sSpewPackageName = "com.valvesoftware.PatchSystem";
    private JSONObject m_JSONManifest = null;
    private CRegistry m_Registry;
    private boolean m_bDownloadingAPK = false;
    private File m_manifestPath = null;
    /* access modifiers changed from: private */
    public HashMap<Long, PendingDownload> m_mapPendingDownloads = new HashMap<>();
    private int m_nApplicationVersion;
    private volatile long m_nCompletedDownloadBytes = 0;
    private volatile long m_nMinApkVersion = 0;
    private volatile long m_nNewApkVersion = 0;
    private volatile long m_nOptionalApkVersion = 0;
    private volatile long m_nPotentialDownloadBytes = 0;
    private volatile long m_nTotalDownloadBytes = 0;
    private long m_nWholeManifestCRC;
    private volatile String m_newAPKUrl;
    ArrayList<PendingDownload> m_vecPendingDownloads = new ArrayList<>();

    public enum EErrorCode {
        Manifest,
        Download,
        Storage,
        QueueDownload,
        Unknown
    }

    public static abstract class PatchSystemCallbacks {
        public abstract void BlockingRunOnUIThread(Runnable runnable);

        public abstract void ExecuteAPKUpdate(Uri uri);

        public void OnCalculatingDownloadSet() {
        }

        public void OnContactingPatchServer() {
        }

        public abstract void OnFatalError(EErrorCode eErrorCode);

        public boolean OnRecoverableError(EErrorCode eErrorCode) {
            return true;
        }

        public void OnStartDownloadingAPK() {
        }

        public void OnStartDownloadingContent() {
        }

        public void SetNonDownloadTaskProgress(float f) {
        }

        public abstract boolean ShouldDownloadManifestUpdate(boolean z, long j);

        public abstract boolean ShouldUpdatedAPK(boolean z, long j);

        public abstract boolean ShouldUseVulkanRendering(boolean z);
    }

    private class PendingDownload {
        long nByteSize;
        long nDownloadID;
        String strFilePath;
        String strURL;
        String strVersionCode;
        Uri uriDestinationPath;

        private PendingDownload() {
        }
    }

    private class CRegistry {
        /* access modifiers changed from: private */
        public SharedPreferences m_SharedPreferences;

        public CRegistry() {
            Context applicationContext = JNI_Environment.m_application.getApplicationContext();
            String GetString = Resources.GetString("GameName");
            this.m_SharedPreferences = applicationContext.getSharedPreferences("PatchSystemRegistry." + GetString, 0);
        }

        public long GetManifestCRC() {
            return this.m_SharedPreferences.getLong("manifestcrc", 0);
        }

        public void SetManifestCRC(long j) {
            SharedPreferences.Editor edit = this.m_SharedPreferences.edit();
            edit.putLong("manifestcrc", j);
            edit.apply();
        }

        public int GetLastFullyInstalledAppVersion() {
            return this.m_SharedPreferences.getInt("lastfullyinstalledappversion", 0);
        }

        public void SetLastFullyInstalledAppVersion(int i) {
            SharedPreferences.Editor edit = this.m_SharedPreferences.edit();
            edit.putInt("lastfullyinstalledappversion", i);
            edit.apply();
        }
    }

    private void ClearPendingDownloads() {
        Context applicationContext = JNI_Environment.m_application.getApplicationContext();
        DownloadManager downloadManager = (DownloadManager) applicationContext.getSystemService("download");
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(31);
        Cursor query2 = downloadManager.query(query);
        if (query2 != null) {
            while (query2.moveToNext()) {
                downloadManager.remove(new long[]{query2.getLong(query2.getColumnIndex("_id"))});
            }
            query2.close();
        }
        for (File file : applicationContext.getExternalCacheDir().listFiles()) {
            if (file.getName().startsWith("download_") && file.getName().endsWith(".tmp")) {
                boolean delete = file.delete();
                StringBuilder sb = new StringBuilder();
                sb.append("Cleaning up download cache file: ");
                sb.append(file.getAbsolutePath());
                sb.append(delete ? " (Success)" : " (Failure)");
                Log.i(k_sSpewPackageName, sb.toString());
            }
        }
    }

    private void EncounteredError(boolean z, EErrorCode eErrorCode, PatchSystemCallbacks patchSystemCallbacks) {
        if (z) {
            if (!patchSystemCallbacks.OnRecoverableError(eErrorCode)) {
                Application.ForceQuit(-1);
            } else {
                return;
            }
        }
        while (true) {
            patchSystemCallbacks.OnFatalError(eErrorCode);
            Application.ForceQuit(-1);
        }
    }

    public boolean Start(String str, int i, PatchSystemCallbacks patchSystemCallbacks) {
        this.m_manifestPath = new File(JNI_Environment.GetPublicPath(), "WRITABLE_MANIFEST");
        if (!this.m_manifestPath.exists()) {
            this.m_manifestPath.mkdirs();
        }
        Log.i(k_sSpewPackageName, "Starting...");
        Log.i(k_sSpewPackageName, "Manifest URL: " + str);
        Log.i(k_sSpewPackageName, "Manifest Path: " + this.m_manifestPath.getAbsolutePath());
        this.m_Registry = new CRegistry();
        this.m_nApplicationVersion = i;
        ClearPendingDownloads();
        if (!UpdateOldInstallationPaths(patchSystemCallbacks)) {
            return false;
        }
        patchSystemCallbacks.OnContactingPatchServer();
        final Semaphore semaphore = new Semaphore(0);
        boolean z = true;
        final VolleyError[] volleyErrorArr = {null};
        final JSONObject[] jSONObjectArr = {null};
        JNI_Environment.GetVolleyQueue().add(new JsonObjectRequest(str, (JSONObject) null, new Response.Listener<JSONObject>() {
            public void onResponse(JSONObject jSONObject) {
                jSONObjectArr[0] = jSONObject;
                semaphore.release();
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError volleyError) {
                volleyErrorArr[0] = volleyError;
                semaphore.release();
            }
        }));
        semaphore.acquireUninterruptibly();
        this.m_JSONManifest = jSONObjectArr[0];
        if (this.m_JSONManifest == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error: ");
            sb.append(volleyErrorArr[0] != null ? volleyErrorArr[0].toString() : "No error");
            Log.e(k_sSpewPackageName, sb.toString());
            if (CanPlayOffline()) {
                return true;
            }
            EncounteredError(CanPlayOffline(), EErrorCode.Manifest, patchSystemCallbacks);
            return false;
        }
        boolean z2 = this.m_nMinApkVersion <= ((long) this.m_nApplicationVersion);
        if (CheckForAPKUpdate(patchSystemCallbacks)) {
            if (!IsSelfInstallAPKEnabled() || GetAvailableStorageBytes() > this.m_nPotentialDownloadBytes) {
                if (this.m_nMinApkVersion <= ((long) this.m_nApplicationVersion)) {
                    z = false;
                }
                if (patchSystemCallbacks.ShouldUpdatedAPK(z, this.m_nPotentialDownloadBytes)) {
                    this.m_nTotalDownloadBytes = 0;
                    Log.i(k_sSpewPackageName, "Downloading APK.");
                    UpdateAPK(patchSystemCallbacks, semaphore);
                } else {
                    Log.i(k_sSpewPackageName, "Skipping APK download, we're done.");
                }
            } else {
                EncounteredError(true, EErrorCode.Storage, patchSystemCallbacks);
            }
        }
        if (!CheckForAssetUpdates(patchSystemCallbacks)) {
            int GetLastFullyInstalledAppVersion = this.m_Registry.GetLastFullyInstalledAppVersion();
            int i2 = this.m_nApplicationVersion;
            if (GetLastFullyInstalledAppVersion != i2) {
                this.m_Registry.SetLastFullyInstalledAppVersion(i2);
            }
            long GetManifestCRC = this.m_Registry.GetManifestCRC();
            long j = this.m_nWholeManifestCRC;
            if (GetManifestCRC == j) {
                return z2;
            }
            this.m_Registry.SetManifestCRC(j);
            return z2;
        } else if (patchSystemCallbacks.ShouldDownloadManifestUpdate(CanPlayOffline(), this.m_nPotentialDownloadBytes)) {
            this.m_nTotalDownloadBytes = 0;
            patchSystemCallbacks.OnStartDownloadingContent();
            if (!BlockingPerformQueuedDownloads(patchSystemCallbacks, semaphore)) {
                return z2;
            }
            this.m_Registry.SetLastFullyInstalledAppVersion(this.m_nApplicationVersion);
            this.m_Registry.SetManifestCRC(this.m_nWholeManifestCRC);
            return z2;
        } else {
            if (!CanPlayOffline()) {
                EncounteredError(false, EErrorCode.Download, patchSystemCallbacks);
            }
            return false;
        }
    }

    private long GetAvailableStorageBytes() {
        File file = this.m_manifestPath;
        while (file != null) {
            try {
                if (file.exists() || file.getAbsolutePath().length() <= 1) {
                    break;
                }
                file = file.getParentFile();
            } catch (Throwable unused) {
            }
        }
        if (file != null && file.exists()) {
            try {
                return new StatFs(this.m_manifestPath.getAbsolutePath()).getAvailableBytes();
            } catch (Throwable unused2) {
            }
        }
        return 0;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0092, code lost:
        if (r7.m_nNewApkVersion > ((long) r7.m_nApplicationVersion)) goto L_0x0094;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00a5, code lost:
        if (r7.m_nOptionalApkVersion > ((long) r7.m_nApplicationVersion)) goto L_0x0094;
     */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00ac  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00b2  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean CheckForAPKUpdate(com.valvesoftware.PatchSystem.PatchSystemCallbacks r8) {
        /*
            r7 = this;
            org.json.JSONObject r0 = r7.m_JSONManifest
            int r1 = r7.m_nApplicationVersion
            java.lang.String r2 = "minversion"
            int r0 = r0.optInt(r2, r1)
            long r0 = (long) r0
            r7.m_nMinApkVersion = r0
            org.json.JSONObject r0 = r7.m_JSONManifest
            long r1 = r7.m_nMinApkVersion
            int r2 = (int) r1
            java.lang.String r1 = "latestversion"
            int r0 = r0.optInt(r1, r2)
            long r0 = (long) r0
            r7.m_nOptionalApkVersion = r0
            boolean r0 = IsSelfInstallAPKEnabled()
            r1 = 1
            r2 = 0
            if (r0 == 0) goto L_0x009e
            org.json.JSONObject r0 = r7.m_JSONManifest     // Catch:{ Exception -> 0x0098 }
            java.lang.String r3 = "packages"
            org.json.JSONObject r0 = r0.getJSONObject(r3)     // Catch:{ Exception -> 0x0098 }
            if (r0 == 0) goto L_0x0096
            r3 = 0
            java.lang.String r4 = com.valvesoftware.JNI_Environment.m_sVPCPlatformName     // Catch:{ Exception -> 0x0098 }
            if (r4 == 0) goto L_0x0038
            java.lang.String r3 = com.valvesoftware.JNI_Environment.m_sVPCPlatformName     // Catch:{ Exception -> 0x0098 }
            org.json.JSONObject r3 = r0.getJSONObject(r3)     // Catch:{ Exception -> 0x0098 }
        L_0x0038:
            if (r3 != 0) goto L_0x0046
            java.lang.String r4 = "TargetPlatformName"
            java.lang.String r4 = com.valvesoftware.Resources.GetString(r4)     // Catch:{ Exception -> 0x0098 }
            if (r4 == 0) goto L_0x0046
            org.json.JSONObject r3 = r0.getJSONObject(r4)     // Catch:{ Exception -> 0x0098 }
        L_0x0046:
            if (r3 != 0) goto L_0x0061
            r4 = 0
        L_0x0049:
            if (r3 != 0) goto L_0x0061
            java.lang.String[] r5 = android.os.Build.SUPPORTED_ABIS     // Catch:{ Exception -> 0x0098 }
            int r5 = r5.length     // Catch:{ Exception -> 0x0098 }
            if (r4 >= r5) goto L_0x0061
            java.lang.String[] r5 = android.os.Build.SUPPORTED_ABIS     // Catch:{ Exception -> 0x0098 }
            r5 = r5[r4]     // Catch:{ Exception -> 0x0098 }
            java.lang.String r5 = com.valvesoftware.JNI_Environment.GetVPCPlatformForABI(r5)     // Catch:{ Exception -> 0x0098 }
            if (r5 == 0) goto L_0x005e
            org.json.JSONObject r3 = r0.getJSONObject(r5)     // Catch:{ Exception -> 0x0098 }
        L_0x005e:
            int r4 = r4 + 1
            goto L_0x0049
        L_0x0061:
            if (r3 != 0) goto L_0x0069
            java.lang.String r3 = "androiduniversal"
            org.json.JSONObject r3 = r0.getJSONObject(r3)     // Catch:{ Exception -> 0x0098 }
        L_0x0069:
            if (r3 != 0) goto L_0x0071
            com.valvesoftware.PatchSystem$EErrorCode r0 = com.valvesoftware.PatchSystem.EErrorCode.Manifest     // Catch:{ Exception -> 0x0098 }
            r7.EncounteredError(r1, r0, r8)     // Catch:{ Exception -> 0x0098 }
            return r2
        L_0x0071:
            java.lang.String r0 = "url"
            java.lang.String r0 = r3.getString(r0)     // Catch:{ Exception -> 0x0098 }
            r7.m_newAPKUrl = r0     // Catch:{ Exception -> 0x0098 }
            java.lang.String r0 = "size"
            int r0 = r3.getInt(r0)     // Catch:{ Exception -> 0x0098 }
            long r4 = (long) r0     // Catch:{ Exception -> 0x0098 }
            r7.m_nPotentialDownloadBytes = r4     // Catch:{ Exception -> 0x0098 }
            java.lang.String r0 = "version"
            int r0 = r3.getInt(r0)     // Catch:{ Exception -> 0x0098 }
            long r3 = (long) r0     // Catch:{ Exception -> 0x0098 }
            r7.m_nNewApkVersion = r3     // Catch:{ Exception -> 0x0098 }
            long r3 = r7.m_nNewApkVersion     // Catch:{ Exception -> 0x0098 }
            int r8 = r7.m_nApplicationVersion     // Catch:{ Exception -> 0x0098 }
            long r5 = (long) r8
            int r8 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r8 <= 0) goto L_0x0096
        L_0x0094:
            r8 = 1
            goto L_0x00a8
        L_0x0096:
            r8 = 0
            goto L_0x00a8
        L_0x0098:
            com.valvesoftware.PatchSystem$EErrorCode r0 = com.valvesoftware.PatchSystem.EErrorCode.Manifest
            r7.EncounteredError(r1, r0, r8)
            return r2
        L_0x009e:
            long r3 = r7.m_nOptionalApkVersion
            int r8 = r7.m_nApplicationVersion
            long r5 = (long) r8
            int r8 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r8 <= 0) goto L_0x0096
            goto L_0x0094
        L_0x00a8:
            java.lang.String r0 = "com.valvesoftware.PatchSystem"
            if (r8 == 0) goto L_0x00b2
            java.lang.String r8 = "APK is out of date."
            android.util.Log.i(r0, r8)
            return r1
        L_0x00b2:
            java.lang.String r8 = "APK is up to date."
            android.util.Log.i(r0, r8)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.CheckForAPKUpdate(com.valvesoftware.PatchSystem$PatchSystemCallbacks):boolean");
    }

    /* JADX WARNING: type inference failed for: r24v13, types: [boolean] */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00bc, code lost:
        if (r9.equals(r4) != false) goto L_0x00c5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00c3, code lost:
        if (r9.equals(r5) != false) goto L_0x00c5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x0238, code lost:
        if (r9.equals(r15) != false) goto L_0x023a;
     */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x03db  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x03e2  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x01c7 A[Catch:{ Exception -> 0x0186 }] */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x01c8 A[Catch:{ Exception -> 0x0186 }] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean CheckForAssetUpdates(com.valvesoftware.PatchSystem.PatchSystemCallbacks r32) {
        /*
            r31 = this;
            r1 = r31
            r2 = r32
            java.lang.String r3 = " Local CRC:"
            java.lang.String r4 = "client_gles"
            java.lang.String r5 = "client_vulkan_androidall"
            java.lang.String r6 = "client_vulkan_iosall"
            java.lang.String r7 = "common"
            java.lang.String r8 = "depotgroup"
            java.lang.String r9 = "cdnroot"
            org.json.JSONObject r0 = r1.m_JSONManifest
            android.app.Application r10 = com.valvesoftware.JNI_Environment.m_application
            android.content.Context r10 = r10.getApplicationContext()
            boolean r0 = com.valvesoftware.VulkanWhitelist.DeviceIsVulkanCompatible(r0, r10)
            boolean r10 = r2.ShouldUseVulkanRendering(r0)
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r12 = "Rendering API chosen: "
            r11.append(r12)
            if (r10 == 0) goto L_0x0031
            java.lang.String r12 = "Vulkan"
            goto L_0x0033
        L_0x0031:
            java.lang.String r12 = "OpenGL ES"
        L_0x0033:
            r11.append(r12)
            java.lang.String r12 = ", whitelisted="
            r11.append(r12)
            r11.append(r0)
            java.lang.String r0 = r11.toString()
            java.lang.String r11 = "com.valvesoftware.PatchSystem"
            android.util.Log.i(r11, r0)
            r12 = 0
            int r0 = android.os.Build.VERSION.SDK_INT     // Catch:{ Throwable -> 0x005a }
            r13 = 24
            if (r0 < r13) goto L_0x0057
            android.security.NetworkSecurityPolicy r0 = android.security.NetworkSecurityPolicy.getInstance()     // Catch:{ Throwable -> 0x005a }
            boolean r0 = r0.isCleartextTrafficPermitted()     // Catch:{ Throwable -> 0x005a }
            goto L_0x0058
        L_0x0057:
            r0 = 0
        L_0x0058:
            r13 = r0
            goto L_0x005b
        L_0x005a:
            r13 = 0
        L_0x005b:
            r1.m_bDownloadingAPK = r12
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r1.m_vecPendingDownloads = r0
            org.json.JSONObject r0 = r1.m_JSONManifest     // Catch:{ Exception -> 0x0421 }
            r14 = 0
            java.lang.String r0 = r0.optString(r9, r14)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r15 = "cdnroot_insecure"
            if (r13 == 0) goto L_0x0075
            org.json.JSONObject r12 = r1.m_JSONManifest     // Catch:{ Exception -> 0x0421 }
            java.lang.String r0 = r12.optString(r15, r0)     // Catch:{ Exception -> 0x0421 }
        L_0x0075:
            r12 = r0
            org.json.JSONObject r0 = r1.m_JSONManifest     // Catch:{ Exception -> 0x0421 }
            java.lang.String r14 = "assets"
            org.json.JSONObject r14 = r0.getJSONObject(r14)     // Catch:{ Exception -> 0x0421 }
            r17 = r12
            r16 = r13
            r12 = 0
            r1.m_nWholeManifestCRC = r12     // Catch:{ Exception -> 0x0421 }
            r18 = 1
            java.util.Iterator r0 = r14.keys()     // Catch:{ Throwable -> 0x018b }
            java.lang.String r12 = ""
            r13 = r12
            r19 = 0
            r20 = 0
        L_0x0093:
            boolean r21 = r0.hasNext()     // Catch:{ Throwable -> 0x017c }
            if (r21 == 0) goto L_0x0169
            java.lang.Object r21 = r0.next()     // Catch:{ Throwable -> 0x017c }
            r22 = r0
            r0 = r21
            java.lang.String r0 = (java.lang.String) r0     // Catch:{ Throwable -> 0x017c }
            r21 = r15
            org.json.JSONObject r15 = r14.getJSONObject(r0)     // Catch:{ Throwable -> 0x0161 }
            r23 = r9
            java.lang.String r9 = r15.optString(r8, r7)     // Catch:{  }
            boolean r24 = r9.equals(r6)     // Catch:{  }
            if (r24 == 0) goto L_0x00b6
            goto L_0x00c5
        L_0x00b6:
            if (r10 == 0) goto L_0x00bf
            boolean r9 = r9.equals(r4)     // Catch:{ Throwable -> 0x015b }
            if (r9 == 0) goto L_0x00cc
            goto L_0x00c5
        L_0x00bf:
            boolean r9 = r9.equals(r5)     // Catch:{  }
            if (r9 == 0) goto L_0x00cc
        L_0x00c5:
            r15 = r21
            r0 = r22
            r9 = r23
            goto L_0x0093
        L_0x00cc:
            int r20 = r20 + 1
            java.lang.String r9 = "bytesize"
            r24 = r4
            r25 = r5
            long r4 = r15.getLong(r9)     // Catch:{ Throwable -> 0x017a }
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x017a }
            r9.<init>()     // Catch:{ Throwable -> 0x017a }
            r9.append(r12)     // Catch:{ Throwable -> 0x017a }
            r9.append(r4)     // Catch:{ Throwable -> 0x017a }
            java.lang.String r9 = r9.toString()     // Catch:{ Throwable -> 0x017a }
            r26 = r12
            java.lang.String r12 = "version"
            java.lang.String r12 = r15.getString(r12)     // Catch:{ Throwable -> 0x017a }
            java.io.File r15 = new java.io.File     // Catch:{ Throwable -> 0x017a }
            java.io.File r2 = r1.m_manifestPath     // Catch:{ Throwable -> 0x017a }
            r15.<init>(r2, r0)     // Catch:{ Throwable -> 0x017a }
            boolean r2 = r15.exists()     // Catch:{ Throwable -> 0x017a }
            if (r2 == 0) goto L_0x0104
            long r27 = r15.length()     // Catch:{ Throwable -> 0x017a }
            int r2 = (r27 > r4 ? 1 : (r27 == r4 ? 0 : -1))
            if (r2 == 0) goto L_0x011a
        L_0x0104:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0157 }
            r2.<init>()     // Catch:{ Throwable -> 0x0157 }
            java.lang.String r4 = "Manifest file missing or resized: "
            r2.append(r4)     // Catch:{ Throwable -> 0x0157 }
            r2.append(r15)     // Catch:{ Throwable -> 0x0157 }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x0157 }
            android.util.Log.e(r11, r2)     // Catch:{ Throwable -> 0x0157 }
            r19 = 1
        L_0x011a:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x017a }
            r2.<init>()     // Catch:{ Throwable -> 0x017a }
            r2.append(r13)     // Catch:{ Throwable -> 0x017a }
            r2.append(r0)     // Catch:{ Throwable -> 0x017a }
            java.lang.String r0 = r2.toString()     // Catch:{ Throwable -> 0x017a }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x017a }
            r2.<init>()     // Catch:{ Throwable -> 0x017a }
            r2.append(r0)     // Catch:{ Throwable -> 0x017a }
            r2.append(r9)     // Catch:{ Throwable -> 0x017a }
            java.lang.String r0 = r2.toString()     // Catch:{ Throwable -> 0x017a }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x017a }
            r2.<init>()     // Catch:{ Throwable -> 0x017a }
            r2.append(r0)     // Catch:{ Throwable -> 0x017a }
            r2.append(r12)     // Catch:{ Throwable -> 0x017a }
            java.lang.String r13 = r2.toString()     // Catch:{ Throwable -> 0x017a }
            r2 = r32
            r15 = r21
            r0 = r22
            r9 = r23
            r4 = r24
            r5 = r25
            r12 = r26
            goto L_0x0093
        L_0x0157:
            r0 = move-exception
            r19 = 1
            goto L_0x0198
        L_0x015b:
            r0 = move-exception
            r24 = r4
            r25 = r5
            goto L_0x0198
        L_0x0161:
            r0 = move-exception
            r24 = r4
            r25 = r5
            r23 = r9
            goto L_0x0198
        L_0x0169:
            r24 = r4
            r25 = r5
            r23 = r9
            r21 = r15
            long r4 = CRC32String(r13)     // Catch:{ Throwable -> 0x017a }
            r1.m_nWholeManifestCRC = r4     // Catch:{ Throwable -> 0x017a }
        L_0x0177:
            r0 = r20
            goto L_0x01b1
        L_0x017a:
            r0 = move-exception
            goto L_0x0198
        L_0x017c:
            r0 = move-exception
            r24 = r4
            r25 = r5
            r23 = r9
            r21 = r15
            goto L_0x0198
        L_0x0186:
            r0 = move-exception
            r2 = r32
            goto L_0x0422
        L_0x018b:
            r0 = move-exception
            r24 = r4
            r25 = r5
            r23 = r9
            r21 = r15
            r19 = 0
            r20 = 0
        L_0x0198:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0186 }
            r2.<init>()     // Catch:{ Exception -> 0x0186 }
            java.lang.String r4 = "Manifest CRC exception: "
            r2.append(r4)     // Catch:{ Exception -> 0x0186 }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0186 }
            r2.append(r0)     // Catch:{ Exception -> 0x0186 }
            java.lang.String r0 = r2.toString()     // Catch:{ Exception -> 0x0186 }
            android.util.Log.e(r11, r0)     // Catch:{ Exception -> 0x0186 }
            goto L_0x0177
        L_0x01b1:
            if (r19 != 0) goto L_0x01d1
            long r4 = r1.m_nWholeManifestCRC     // Catch:{ Exception -> 0x0186 }
            r12 = 0
            int r2 = (r4 > r12 ? 1 : (r4 == r12 ? 0 : -1))
            if (r2 == 0) goto L_0x01d1
            long r4 = r1.m_nWholeManifestCRC     // Catch:{ Exception -> 0x0186 }
            com.valvesoftware.PatchSystem$CRegistry r2 = r1.m_Registry     // Catch:{ Exception -> 0x0186 }
            long r12 = r2.GetManifestCRC()     // Catch:{ Exception -> 0x0186 }
            int r2 = (r4 > r12 ? 1 : (r4 == r12 ? 0 : -1))
            if (r2 == 0) goto L_0x01c8
            goto L_0x01d1
        L_0x01c8:
            java.lang.String r0 = "Manifest CRC is unchanged"
            android.util.Log.i(r11, r0)     // Catch:{ Exception -> 0x0186 }
        L_0x01cd:
            r2 = r32
            goto L_0x03d3
        L_0x01d1:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0186 }
            r2.<init>()     // Catch:{ Exception -> 0x0186 }
            java.lang.String r4 = "Whole manifest CRC changed. Manifest CRC:"
            r2.append(r4)     // Catch:{ Exception -> 0x0186 }
            long r4 = r1.m_nWholeManifestCRC     // Catch:{ Exception -> 0x0186 }
            java.lang.String r4 = java.lang.Long.toHexString(r4)     // Catch:{ Exception -> 0x0186 }
            r2.append(r4)     // Catch:{ Exception -> 0x0186 }
            r2.append(r3)     // Catch:{ Exception -> 0x0186 }
            com.valvesoftware.PatchSystem$CRegistry r4 = r1.m_Registry     // Catch:{ Exception -> 0x0186 }
            long r4 = r4.GetManifestCRC()     // Catch:{ Exception -> 0x0186 }
            java.lang.String r4 = java.lang.Long.toHexString(r4)     // Catch:{ Exception -> 0x0186 }
            r2.append(r4)     // Catch:{ Exception -> 0x0186 }
            java.lang.String r2 = r2.toString()     // Catch:{ Exception -> 0x0186 }
            android.util.Log.i(r11, r2)     // Catch:{ Exception -> 0x0186 }
            r32.OnCalculatingDownloadSet()     // Catch:{ Exception -> 0x0186 }
            java.util.Iterator r2 = r14.keys()     // Catch:{ Exception -> 0x0186 }
            r12 = 0
        L_0x0203:
            boolean r4 = r2.hasNext()     // Catch:{ Exception -> 0x0186 }
            if (r4 == 0) goto L_0x01cd
            java.lang.Object r4 = r2.next()     // Catch:{ Exception -> 0x0186 }
            java.lang.String r4 = (java.lang.String) r4     // Catch:{ Exception -> 0x0186 }
            org.json.JSONObject r5 = r14.getJSONObject(r4)     // Catch:{ Exception -> 0x0186 }
            java.lang.String r9 = r5.optString(r8, r7)     // Catch:{ Exception -> 0x0186 }
            boolean r13 = r9.equals(r6)     // Catch:{ Exception -> 0x0186 }
            if (r13 == 0) goto L_0x0222
            r13 = r24
        L_0x021f:
            r15 = r25
            goto L_0x023a
        L_0x0222:
            if (r10 == 0) goto L_0x0230
            r13 = r24
            boolean r9 = r9.equals(r13)     // Catch:{ Exception -> 0x0186 }
            if (r9 == 0) goto L_0x022d
            goto L_0x021f
        L_0x022d:
            r15 = r25
            goto L_0x023f
        L_0x0230:
            r13 = r24
            r15 = r25
            boolean r9 = r9.equals(r15)     // Catch:{ Exception -> 0x0186 }
            if (r9 == 0) goto L_0x023f
        L_0x023a:
            r24 = r13
        L_0x023c:
            r25 = r15
            goto L_0x0203
        L_0x023f:
            int r12 = r12 + 1
            float r9 = (float) r12
            r19 = r2
            float r2 = (float) r0
            float r9 = r9 / r2
            r2 = r32
            r2.SetNonDownloadTaskProgress(r9)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r9 = "version"
            java.lang.String r9 = r5.getString(r9)     // Catch:{ Exception -> 0x0421 }
            r20 = r0
            java.lang.String r0 = "bytesize"
            int r0 = r5.getInt(r0)     // Catch:{ Exception -> 0x0421 }
            r22 = r6
            java.io.File r6 = new java.io.File     // Catch:{ Exception -> 0x0421 }
            r24 = r7
            java.io.File r7 = r1.m_manifestPath     // Catch:{ Exception -> 0x0421 }
            r6.<init>(r7, r4)     // Catch:{ Exception -> 0x0421 }
            boolean r7 = r6.exists()     // Catch:{ Exception -> 0x0421 }
            r25 = r8
            java.lang.String r8 = "Download Asset: \""
            if (r7 != 0) goto L_0x0289
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r7.<init>()     // Catch:{ Exception -> 0x0421 }
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            r7.append(r4)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = "\". Which is missing locally."
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r7 = r7.toString()     // Catch:{ Exception -> 0x0421 }
            android.util.Log.i(r11, r7)     // Catch:{ Exception -> 0x0421 }
            r26 = r12
            goto L_0x0307
        L_0x0289:
            r26 = r12
            r7 = r13
            long r12 = (long) r0     // Catch:{ Exception -> 0x0421 }
            long r27 = r6.length()     // Catch:{ Exception -> 0x0421 }
            int r29 = (r12 > r27 ? 1 : (r12 == r27 ? 0 : -1))
            if (r29 == 0) goto L_0x02bd
            java.lang.StringBuilder r12 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r12.<init>()     // Catch:{ Exception -> 0x0421 }
            r12.append(r8)     // Catch:{ Exception -> 0x0421 }
            r12.append(r4)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = "\". Manifest Size:"
            r12.append(r8)     // Catch:{ Exception -> 0x0421 }
            r12.append(r0)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = " Local Size: "
            r12.append(r8)     // Catch:{ Exception -> 0x0421 }
            r13 = r7
            long r7 = r6.length()     // Catch:{ Exception -> 0x0421 }
            r12.append(r7)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r7 = r12.toString()     // Catch:{ Exception -> 0x0421 }
            android.util.Log.i(r11, r7)     // Catch:{ Exception -> 0x0421 }
            goto L_0x0307
        L_0x02bd:
            r13 = r7
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r7.<init>()     // Catch:{ Exception -> 0x0421 }
            java.lang.String r12 = "0x"
            r7.append(r12)     // Catch:{ Exception -> 0x0421 }
            r7.append(r9)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r7 = r7.toString()     // Catch:{ Exception -> 0x0421 }
            java.lang.Long r7 = java.lang.Long.decode(r7)     // Catch:{ Exception -> 0x0421 }
            long r27 = r7.longValue()     // Catch:{ Exception -> 0x0421 }
            long r29 = CRC32File(r6)     // Catch:{ Exception -> 0x0421 }
            int r7 = (r29 > r27 ? 1 : (r29 == r27 ? 0 : -1))
            if (r7 == 0) goto L_0x038a
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r7.<init>()     // Catch:{ Exception -> 0x0421 }
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            r7.append(r4)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = "\". Manifest CRC:"
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = java.lang.Long.toHexString(r27)     // Catch:{ Exception -> 0x0421 }
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            r7.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r8 = java.lang.Long.toHexString(r29)     // Catch:{ Exception -> 0x0421 }
            r7.append(r8)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r7 = r7.toString()     // Catch:{ Exception -> 0x0421 }
            android.util.Log.i(r11, r7)     // Catch:{ Exception -> 0x0421 }
        L_0x0307:
            r7 = r17
            r8 = r23
            java.lang.String r12 = r5.optString(r8, r7)     // Catch:{ Exception -> 0x0421 }
            if (r16 == 0) goto L_0x031a
            r17 = r3
            r3 = r21
            java.lang.String r12 = r5.optString(r3, r12)     // Catch:{ Exception -> 0x0421 }
            goto L_0x031e
        L_0x031a:
            r17 = r3
            r3 = r21
        L_0x031e:
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r5.<init>()     // Catch:{ Exception -> 0x0421 }
            r5.append(r12)     // Catch:{ Exception -> 0x0421 }
            r21 = r3
            java.lang.String r3 = " "
            r23 = r7
            java.lang.String r7 = "%20"
            java.lang.String r3 = r4.replace(r3, r7)     // Catch:{ Exception -> 0x0421 }
            r5.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r3 = r5.toString()     // Catch:{ Exception -> 0x0421 }
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r5.<init>()     // Catch:{ Exception -> 0x0421 }
            r5.append(r12)     // Catch:{ Exception -> 0x0421 }
            r5.append(r4)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r5 = r5.toString()     // Catch:{ Exception -> 0x0421 }
            int r7 = r3.compareTo(r5)     // Catch:{ Exception -> 0x0421 }
            if (r7 == 0) goto L_0x036f
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r7.<init>()     // Catch:{ Exception -> 0x0421 }
            java.lang.String r12 = "Sanitizing Manifest URL \""
            r7.append(r12)     // Catch:{ Exception -> 0x0421 }
            r7.append(r5)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r5 = "\" to \""
            r7.append(r5)     // Catch:{ Exception -> 0x0421 }
            r7.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r5 = "\""
            r7.append(r5)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r5 = r7.toString()     // Catch:{ Exception -> 0x0421 }
            android.util.Log.i(r11, r5)     // Catch:{ Exception -> 0x0421 }
        L_0x036f:
            com.valvesoftware.PatchSystem$PendingDownload r5 = new com.valvesoftware.PatchSystem$PendingDownload     // Catch:{ Exception -> 0x0421 }
            r7 = 0
            r5.<init>()     // Catch:{ Exception -> 0x0421 }
            r5.strFilePath = r4     // Catch:{ Exception -> 0x0421 }
            r5.strURL = r3     // Catch:{ Exception -> 0x0421 }
            r5.strVersionCode = r9     // Catch:{ Exception -> 0x0421 }
            android.net.Uri r3 = android.net.Uri.fromFile(r6)     // Catch:{ Exception -> 0x0421 }
            r5.uriDestinationPath = r3     // Catch:{ Exception -> 0x0421 }
            long r3 = (long) r0     // Catch:{ Exception -> 0x0421 }
            r5.nByteSize = r3     // Catch:{ Exception -> 0x0421 }
            java.util.ArrayList<com.valvesoftware.PatchSystem$PendingDownload> r0 = r1.m_vecPendingDownloads     // Catch:{ Exception -> 0x0421 }
            r0.add(r5)     // Catch:{ Exception -> 0x0421 }
            goto L_0x03bd
        L_0x038a:
            r8 = r23
            r7 = 0
            r23 = r17
            r17 = r3
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x0421 }
            r0.<init>()     // Catch:{ Exception -> 0x0421 }
            java.lang.String r3 = "Skipping Download for Existing Asset: \""
            r0.append(r3)     // Catch:{ Exception -> 0x0421 }
            r0.append(r4)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r3 = "\". With matching size: "
            r0.append(r3)     // Catch:{ Exception -> 0x0421 }
            long r3 = r6.length()     // Catch:{ Exception -> 0x0421 }
            r0.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r3 = " and CRC: "
            r0.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r3 = java.lang.Long.toHexString(r29)     // Catch:{ Exception -> 0x0421 }
            r0.append(r3)     // Catch:{ Exception -> 0x0421 }
            java.lang.String r0 = r0.toString()     // Catch:{ Exception -> 0x0421 }
            android.util.Log.i(r11, r0)     // Catch:{ Exception -> 0x0421 }
        L_0x03bd:
            r3 = r17
            r2 = r19
            r0 = r20
            r6 = r22
            r17 = r23
            r7 = r24
            r12 = r26
            r23 = r8
            r24 = r13
            r8 = r25
            goto L_0x023c
        L_0x03d3:
            java.util.ArrayList<com.valvesoftware.PatchSystem$PendingDownload> r0 = r1.m_vecPendingDownloads
            boolean r0 = r0.isEmpty()
            if (r0 == 0) goto L_0x03e2
            java.lang.String r0 = "All files up-to-date, we're done."
            android.util.Log.i(r11, r0)
            r3 = 0
            return r3
        L_0x03e2:
            r3 = 0
            com.valvesoftware.PatchSystem$CRegistry r0 = r1.m_Registry
            r0.SetLastFullyInstalledAppVersion(r3)
            r3 = 0
            r1.m_nPotentialDownloadBytes = r3
            java.util.ArrayList<com.valvesoftware.PatchSystem$PendingDownload> r0 = r1.m_vecPendingDownloads
            java.util.Iterator r0 = r0.iterator()
        L_0x03f2:
            boolean r3 = r0.hasNext()
            if (r3 == 0) goto L_0x0406
            java.lang.Object r3 = r0.next()
            com.valvesoftware.PatchSystem$PendingDownload r3 = (com.valvesoftware.PatchSystem.PendingDownload) r3
            long r4 = r1.m_nPotentialDownloadBytes
            long r6 = r3.nByteSize
            long r4 = r4 + r6
            r1.m_nPotentialDownloadBytes = r4
            goto L_0x03f2
        L_0x0406:
            long r3 = r31.GetAvailableStorageBytes()
            long r5 = r1.m_nPotentialDownloadBytes
            int r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r0 > 0) goto L_0x0420
            boolean r0 = r31.CanPlayOffline()
            com.valvesoftware.PatchSystem$EErrorCode r3 = com.valvesoftware.PatchSystem.EErrorCode.Storage
            r1.EncounteredError(r0, r3, r2)
            java.util.ArrayList<com.valvesoftware.PatchSystem$PendingDownload> r0 = r1.m_vecPendingDownloads
            r0.clear()
        L_0x041e:
            r2 = 0
            return r2
        L_0x0420:
            return r18
        L_0x0421:
            r0 = move-exception
        L_0x0422:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Manifest Exception: "
            r3.append(r4)
            java.lang.String r0 = r0.toString()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.e(r11, r0)
            java.util.ArrayList<com.valvesoftware.PatchSystem$PendingDownload> r0 = r1.m_vecPendingDownloads
            r0.clear()
            boolean r0 = r31.CanPlayOffline()
            com.valvesoftware.PatchSystem$EErrorCode r3 = com.valvesoftware.PatchSystem.EErrorCode.Manifest
            r1.EncounteredError(r0, r3, r2)
            goto L_0x041e
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.CheckForAssetUpdates(com.valvesoftware.PatchSystem$PatchSystemCallbacks):boolean");
    }

    private boolean DeleteExistingFile(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                Log.e(k_sSpewPackageName, "Couldn't Delete Existing File: " + file.getAbsolutePath());
                return false;
            }
            Log.i(k_sSpewPackageName, "Deleted Existing File: " + file.getAbsolutePath());
        }
        if (!file.exists()) {
            return true;
        }
        Log.e(k_sSpewPackageName, "Didn't Actually Delete Existing File: " + file.getAbsolutePath());
        return false;
    }

    private void UpdateAPK(PatchSystemCallbacks patchSystemCallbacks, Semaphore semaphore) {
        Uri uri;
        if (IsSelfInstallAPKEnabled()) {
            Context applicationContext = JNI_Environment.m_application.getApplicationContext();
            File file = new File(applicationContext.getExternalCacheDir(), "_underlords_tmp_install_.apk");
            if (!DeleteExistingFile(file)) {
                EncounteredError(true, EErrorCode.Manifest, patchSystemCallbacks);
                return;
            }
            if (!this.m_vecPendingDownloads.isEmpty()) {
                Log.e(k_sSpewPackageName, "State exception - start download with non-empty pending list");
                this.m_vecPendingDownloads.clear();
            }
            try {
                PendingDownload pendingDownload = new PendingDownload();
                pendingDownload.strFilePath = "_underlords_tmp_install_.apk";
                pendingDownload.strURL = this.m_newAPKUrl;
                pendingDownload.strVersionCode = "01";
                pendingDownload.uriDestinationPath = Uri.fromFile(file);
                pendingDownload.nByteSize = this.m_nPotentialDownloadBytes;
                this.m_vecPendingDownloads.add(pendingDownload);
                if (Build.VERSION.SDK_INT >= 24) {
                    uri = FileProvider.getUriForFile(applicationContext, applicationContext.getApplicationContext().getPackageName() + ".provider", file);
                } else {
                    uri = Uri.parse("file://" + file);
                }
                this.m_bDownloadingAPK = true;
                patchSystemCallbacks.OnStartDownloadingAPK();
                if (BlockingPerformQueuedDownloads(patchSystemCallbacks, semaphore)) {
                    patchSystemCallbacks.ExecuteAPKUpdate(uri);
                }
            } catch (Exception e) {
                Log.e(k_sSpewPackageName, "Manifest Exception: " + e.toString());
                this.m_vecPendingDownloads.clear();
                EncounteredError(true, EErrorCode.Manifest, patchSystemCallbacks);
                return;
            }
        } else {
            patchSystemCallbacks.ExecuteAPKUpdate((Uri) null);
        }
        this.m_bDownloadingAPK = false;
    }

    private boolean BlockingPerformQueuedDownloads(PatchSystemCallbacks patchSystemCallbacks, Semaphore semaphore) {
        boolean z;
        PatchSystemCallbacks patchSystemCallbacks2 = patchSystemCallbacks;
        long j = 0;
        this.m_nTotalDownloadBytes = 0;
        Context applicationContext = JNI_Environment.m_application.getApplicationContext();
        DownloadManager downloadManager = (DownloadManager) applicationContext.getSystemService("download");
        boolean[] zArr = {true};
        final DownloadManager downloadManager2 = downloadManager;
        final boolean[] zArr2 = zArr;
        final PatchSystemCallbacks patchSystemCallbacks3 = patchSystemCallbacks;
        final Semaphore semaphore2 = semaphore;
        applicationContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean z;
                PendingDownload pendingDownload;
                long longExtra = intent.getLongExtra("extra_download_id", -1);
                boolean z2 = true;
                Cursor query = downloadManager2.query(new DownloadManager.Query().setFilterById(new long[]{longExtra}));
                int i = -1;
                String str = null;
                if (query.moveToFirst()) {
                    z = query.getInt(query.getColumnIndex(NotificationCompat.CATEGORY_STATUS)) == 8;
                    if (!z) {
                        i = query.getInt(query.getColumnIndex("reason"));
                    } else {
                        str = query.getString(query.getColumnIndex("local_uri"));
                    }
                } else {
                    Log.i(PatchSystem.k_sSpewPackageName, "moveToFirst failed");
                    z = false;
                    z2 = false;
                }
                query.close();
                if (z2) {
                    synchronized (PatchSystem.this.m_mapPendingDownloads) {
                        if (!PatchSystem.this.m_mapPendingDownloads.containsKey(Long.valueOf(longExtra))) {
                            Log.e(PatchSystem.k_sSpewPackageName, "Download Error: " + String.valueOf(longExtra) + " (Not found in Pending Downloads)");
                            zArr2[0] = false;
                        }
                        pendingDownload = (PendingDownload) PatchSystem.this.m_mapPendingDownloads.get(Long.valueOf(longExtra));
                        PatchSystem.this.m_mapPendingDownloads.remove(Long.valueOf(longExtra));
                    }
                    if (!z) {
                        zArr2[0] = false;
                        Log.e(PatchSystem.k_sSpewPackageName, "Download Failure: " + String.valueOf(longExtra) + " (Reason: " + String.valueOf(i) + ")");
                    } else if (!PatchSystem.this.OnDownloadResponseSuccess(patchSystemCallbacks3, pendingDownload, str)) {
                        zArr2[0] = false;
                    }
                    if (PatchSystem.this.m_mapPendingDownloads.isEmpty()) {
                        semaphore2.release();
                    }
                }
            }
        }, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
        Iterator<PendingDownload> it = this.m_vecPendingDownloads.iterator();
        while (it.hasNext()) {
            this.m_nTotalDownloadBytes += it.next().nByteSize;
        }
        Iterator<PendingDownload> it2 = this.m_vecPendingDownloads.iterator();
        while (it2.hasNext()) {
            PendingDownload next = it2.next();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(next.strURL));
            if (this.m_bDownloadingAPK) {
                request.setDestinationUri(next.uriDestinationPath);
            } else {
                request.setDestinationUri(Uri.fromFile(new File(applicationContext.getExternalCacheDir(), "download_" + Integer.toHexString(next.strFilePath.hashCode()) + "_" + next.strVersionCode + ".tmp")));
            }
            request.setTitle(next.strFilePath);
            request.setDescription(next.strFilePath);
            request.setNotificationVisibility(0);
            synchronized (this.m_mapPendingDownloads) {
                try {
                    next.nDownloadID = downloadManager.enqueue(request);
                    this.m_mapPendingDownloads.put(Long.valueOf(next.nDownloadID), next);
                    j += next.nByteSize;
                } catch (Throwable th) {
                    Log.i(k_sSpewPackageName, "Download Queue Failed: " + next.strURL + " with exception: " + th.getMessage());
                    z = true;
                }
            }
            Log.i(k_sSpewPackageName, "Download Queued: " + next.strURL + " (DownloadID: " + String.valueOf(next.nDownloadID) + ")");
        }
        z = false;
        this.m_nTotalDownloadBytes = j;
        this.m_vecPendingDownloads.clear();
        if (z) {
            ClearPendingDownloads();
            EncounteredError(false, EErrorCode.QueueDownload, patchSystemCallbacks2);
        } else {
            semaphore.acquireUninterruptibly();
            if (zArr[0]) {
                return true;
            }
            EncounteredError(CanPlayOffline(), EErrorCode.Download, patchSystemCallbacks2);
        }
        return false;
    }

    private boolean ProcessCompletedDownload(PendingDownload pendingDownload, String str) {
        File file = new File(Uri.parse(str).getPath());
        if (!file.exists()) {
            Log.e(k_sSpewPackageName, "Download Not Found: \"" + file.getAbsolutePath() + "\" (DownloadID: " + pendingDownload.nDownloadID + ")");
            return false;
        } else if (file.length() <= 0) {
            Log.e(k_sSpewPackageName, "Downloaded Zero Bytes: \"" + file.getAbsolutePath() + "\" (DownloadID: " + pendingDownload.nDownloadID + ")");
            return false;
        } else if (this.m_bDownloadingAPK) {
            return true;
        } else {
            File file2 = new File(pendingDownload.uriDestinationPath.getPath());
            file2.mkdirs();
            if (!DeleteExistingFile(file2)) {
                return false;
            }
            if (this.m_Registry.GetLastFullyInstalledAppVersion() != 0) {
                this.m_Registry.SetLastFullyInstalledAppVersion(0);
            }
            if (!MoveFile(file, file2)) {
                Log.e(k_sSpewPackageName, "Couldn't move: \"" + file.getAbsolutePath() + "\" to \"" + file2.getAbsolutePath() + "\" (DownloadID: " + pendingDownload.nDownloadID + ")");
                return false;
            }
            Log.i(k_sSpewPackageName, "Moved File: \"" + file.getAbsolutePath() + "\" to \"" + file2.getAbsolutePath() + "\" (DownloadID: " + pendingDownload.nDownloadID + ")");
            return true;
        }
    }

    /* access modifiers changed from: private */
    public boolean OnDownloadResponseSuccess(PatchSystemCallbacks patchSystemCallbacks, PendingDownload pendingDownload, String str) {
        DownloadManager downloadManager = (DownloadManager) JNI_Environment.m_application.getApplicationContext().getSystemService("download");
        Log.i(k_sSpewPackageName, "Download Success: " + pendingDownload.strURL + " (DownloadID: " + String.valueOf(pendingDownload.nDownloadID) + ")");
        boolean ProcessCompletedDownload = ProcessCompletedDownload(pendingDownload, str);
        if (!this.m_bDownloadingAPK) {
            downloadManager.remove(new long[]{pendingDownload.nDownloadID});
        }
        this.m_nCompletedDownloadBytes += pendingDownload.nByteSize;
        if (this.m_bDownloadingAPK && !this.m_mapPendingDownloads.isEmpty()) {
            Log.i(k_sSpewPackageName, "More than one pending APK download.");
            EncounteredError(false, EErrorCode.Unknown, patchSystemCallbacks);
        }
        return ProcessCompletedDownload;
    }

    public boolean CanPlayOffline() {
        return this.m_Registry.GetLastFullyInstalledAppVersion() == this.m_nApplicationVersion;
    }

    public float GetDownloadProgress() {
        if (this.m_nTotalDownloadBytes == 0) {
            return 0.0f;
        }
        synchronized (this.m_mapPendingDownloads) {
            if (this.m_mapPendingDownloads.isEmpty()) {
                return 1.0f;
            }
            long[] jArr = new long[this.m_mapPendingDownloads.size()];
            int i = 0;
            for (PendingDownload pendingDownload : this.m_mapPendingDownloads.values()) {
                jArr[i] = pendingDownload.nDownloadID;
                i++;
            }
            long j = this.m_nCompletedDownloadBytes;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(jArr);
            Cursor query2 = ((DownloadManager) JNI_Environment.m_application.getApplicationContext().getSystemService("download")).query(query);
            if (query2 != null) {
                while (query2.moveToNext()) {
                    j += query2.getLong(query2.getColumnIndex("bytes_so_far"));
                }
                query2.close();
            }
            float f = ((float) j) / ((float) this.m_nTotalDownloadBytes);
            return f;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:?, code lost:
        r22.getParentFile().mkdirs();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x002a, code lost:
        if (r21.renameTo(r22) != false) goto L_0x002c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x002c, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x002d, code lost:
        android.util.Log.e(k_sSpewPackageName, "Couldn't renameTo File: \"" + r21.getAbsolutePath() + "\" to \"" + r22.getAbsolutePath() + "\"");
        android.util.Log.e(k_sSpewPackageName, "Attempting to Copy then Delete: \"" + r21.getAbsolutePath() + "\" to \"" + r22.getAbsolutePath() + "\"");
        r4 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:?, code lost:
        r0 = new java.io.FileInputStream(r21);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:?, code lost:
        r7 = new java.io.FileOutputStream(r22);
        r15 = r0.getChannel();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:?, code lost:
        r4 = r7.getChannel();
        r16 = r15.size();
        r18 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x009e, code lost:
        if (r18 < r16) goto L_0x00a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00aa, code lost:
        r18 = r18 + r15.transferTo(r18, r16 - r18, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00ad, code lost:
        if (r15 != null) goto L_0x00af;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:?, code lost:
        r15.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00b3, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00b5, code lost:
        if (r4 != null) goto L_0x00b7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00b7, code lost:
        r4.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00ba, code lost:
        r21.delete();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00be, code lost:
        android.util.Log.i(k_sSpewPackageName, "MoveFile cleanup Exception: " + r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00d4, code lost:
        return true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00d7, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00d8, code lost:
        r2 = r0;
        r3 = null;
        r4 = r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00dd, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00de, code lost:
        r3 = null;
        r4 = r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00e1, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00e3, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00e5, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00e7, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00e8, code lost:
        r5 = r21;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00ea, code lost:
        r2 = r0;
        r3 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00ed, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00ee, code lost:
        r5 = r21;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00f0, code lost:
        r8 = r22;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x00f2, code lost:
        r3 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:?, code lost:
        android.util.Log.e(k_sSpewPackageName, "Couldn't Copy File: " + r21.getAbsolutePath() + " to " + r22.getAbsolutePath() + " Exception: " + r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x0123, code lost:
        if (r4 != null) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:?, code lost:
        r4.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x0129, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x012b, code lost:
        if (r3 != null) goto L_0x012d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x012d, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0130, code lost:
        r21.delete();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0134, code lost:
        android.util.Log.i(k_sSpewPackageName, "MoveFile cleanup Exception: " + r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x014b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x014c, code lost:
        r2 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x014d, code lost:
        if (r4 != null) goto L_0x014f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:?, code lost:
        r4.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0153, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0155, code lost:
        if (r3 != null) goto L_0x0157;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x0157, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x015a, code lost:
        r21.delete();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x015e, code lost:
        android.util.Log.i(k_sSpewPackageName, "MoveFile cleanup Exception: " + r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x0174, code lost:
        throw r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:?, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:?, code lost:
        return false;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x001f */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00e3 A[ExcHandler: all (th java.lang.Throwable), Splitter:B:19:0x0082] */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0125 A[SYNTHETIC, Splitter:B:57:0x0125] */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x012d A[Catch:{ Exception -> 0x0129 }] */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x014f A[SYNTHETIC, Splitter:B:68:0x014f] */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0157 A[Catch:{ Exception -> 0x0153 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean MoveFile(java.io.File r21, java.io.File r22) {
        /*
            r20 = this;
            java.lang.String r1 = "MoveFile cleanup Exception: "
            boolean r0 = r22.exists()
            if (r0 == 0) goto L_0x000b
            r22.delete()     // Catch:{ Throwable -> 0x000b }
        L_0x000b:
            r2 = 0
            r3 = 1
            java.nio.file.Path r0 = r21.toPath()     // Catch:{ Throwable -> 0x001f }
            java.nio.file.Path r4 = r22.toPath()     // Catch:{ Throwable -> 0x001f }
            java.nio.file.CopyOption[] r5 = new java.nio.file.CopyOption[r3]     // Catch:{ Throwable -> 0x001f }
            java.nio.file.StandardCopyOption r6 = java.nio.file.StandardCopyOption.REPLACE_EXISTING     // Catch:{ Throwable -> 0x001f }
            r5[r2] = r6     // Catch:{ Throwable -> 0x001f }
            java.nio.file.Files.move(r0, r4, r5)     // Catch:{ Throwable -> 0x001f }
            return r3
        L_0x001f:
            java.io.File r0 = r22.getParentFile()     // Catch:{ Throwable -> 0x0026 }
            r0.mkdirs()     // Catch:{ Throwable -> 0x0026 }
        L_0x0026:
            boolean r0 = r21.renameTo(r22)
            if (r0 == 0) goto L_0x002d
            return r3
        L_0x002d:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "Couldn't renameTo File: \""
            r0.append(r4)
            java.lang.String r4 = r21.getAbsolutePath()
            r0.append(r4)
            java.lang.String r4 = "\" to \""
            r0.append(r4)
            java.lang.String r5 = r22.getAbsolutePath()
            r0.append(r5)
            java.lang.String r5 = "\""
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            java.lang.String r6 = "com.valvesoftware.PatchSystem"
            android.util.Log.e(r6, r0)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r7 = "Attempting to Copy then Delete: \""
            r0.append(r7)
            java.lang.String r7 = r21.getAbsolutePath()
            r0.append(r7)
            r0.append(r4)
            java.lang.String r4 = r22.getAbsolutePath()
            r0.append(r4)
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            android.util.Log.e(r6, r0)
            r4 = 0
            java.io.FileInputStream r0 = new java.io.FileInputStream     // Catch:{ Exception -> 0x00ed, all -> 0x00e7 }
            r5 = r21
            r0.<init>(r5)     // Catch:{ Exception -> 0x00e5, all -> 0x00e3 }
            java.io.FileOutputStream r7 = new java.io.FileOutputStream     // Catch:{ Exception -> 0x00e5, all -> 0x00e3 }
            r8 = r22
            r7.<init>(r8)     // Catch:{ Exception -> 0x00e1, all -> 0x00e3 }
            java.nio.channels.FileChannel r15 = r0.getChannel()     // Catch:{ Exception -> 0x00e1, all -> 0x00e3 }
            java.nio.channels.FileChannel r4 = r7.getChannel()     // Catch:{ Exception -> 0x00dd, all -> 0x00d7 }
            long r16 = r15.size()     // Catch:{ Exception -> 0x00dd, all -> 0x00d7 }
            r9 = 0
            r18 = r9
        L_0x009c:
            int r0 = (r18 > r16 ? 1 : (r18 == r16 ? 0 : -1))
            if (r0 >= 0) goto L_0x00ad
            long r12 = r16 - r18
            r9 = r15
            r10 = r18
            r14 = r4
            long r9 = r9.transferTo(r10, r12, r14)     // Catch:{ Exception -> 0x00dd, all -> 0x00d7 }
            long r18 = r18 + r9
            goto L_0x009c
        L_0x00ad:
            if (r15 == 0) goto L_0x00b5
            r15.close()     // Catch:{ Exception -> 0x00b3 }
            goto L_0x00b5
        L_0x00b3:
            r0 = move-exception
            goto L_0x00be
        L_0x00b5:
            if (r4 == 0) goto L_0x00ba
            r4.close()     // Catch:{ Exception -> 0x00b3 }
        L_0x00ba:
            r21.delete()     // Catch:{ Exception -> 0x00b3 }
            goto L_0x00d4
        L_0x00be:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r1)
            java.lang.String r0 = r0.toString()
            r2.append(r0)
            java.lang.String r0 = r2.toString()
            android.util.Log.i(r6, r0)
        L_0x00d4:
            r2 = 1
            goto L_0x014a
        L_0x00d7:
            r0 = move-exception
            r2 = r0
            r3 = r4
            r4 = r15
            goto L_0x014d
        L_0x00dd:
            r0 = move-exception
            r3 = r4
            r4 = r15
            goto L_0x00f3
        L_0x00e1:
            r0 = move-exception
            goto L_0x00f2
        L_0x00e3:
            r0 = move-exception
            goto L_0x00ea
        L_0x00e5:
            r0 = move-exception
            goto L_0x00f0
        L_0x00e7:
            r0 = move-exception
            r5 = r21
        L_0x00ea:
            r2 = r0
            r3 = r4
            goto L_0x014d
        L_0x00ed:
            r0 = move-exception
            r5 = r21
        L_0x00f0:
            r8 = r22
        L_0x00f2:
            r3 = r4
        L_0x00f3:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x014b }
            r7.<init>()     // Catch:{ all -> 0x014b }
            java.lang.String r9 = "Couldn't Copy File: "
            r7.append(r9)     // Catch:{ all -> 0x014b }
            java.lang.String r9 = r21.getAbsolutePath()     // Catch:{ all -> 0x014b }
            r7.append(r9)     // Catch:{ all -> 0x014b }
            java.lang.String r9 = " to "
            r7.append(r9)     // Catch:{ all -> 0x014b }
            java.lang.String r8 = r22.getAbsolutePath()     // Catch:{ all -> 0x014b }
            r7.append(r8)     // Catch:{ all -> 0x014b }
            java.lang.String r8 = " Exception: "
            r7.append(r8)     // Catch:{ all -> 0x014b }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x014b }
            r7.append(r0)     // Catch:{ all -> 0x014b }
            java.lang.String r0 = r7.toString()     // Catch:{ all -> 0x014b }
            android.util.Log.e(r6, r0)     // Catch:{ all -> 0x014b }
            if (r4 == 0) goto L_0x012b
            r4.close()     // Catch:{ Exception -> 0x0129 }
            goto L_0x012b
        L_0x0129:
            r0 = move-exception
            goto L_0x0134
        L_0x012b:
            if (r3 == 0) goto L_0x0130
            r3.close()     // Catch:{ Exception -> 0x0129 }
        L_0x0130:
            r21.delete()     // Catch:{ Exception -> 0x0129 }
            goto L_0x014a
        L_0x0134:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r0 = r0.toString()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.i(r6, r0)
        L_0x014a:
            return r2
        L_0x014b:
            r0 = move-exception
            r2 = r0
        L_0x014d:
            if (r4 == 0) goto L_0x0155
            r4.close()     // Catch:{ Exception -> 0x0153 }
            goto L_0x0155
        L_0x0153:
            r0 = move-exception
            goto L_0x015e
        L_0x0155:
            if (r3 == 0) goto L_0x015a
            r3.close()     // Catch:{ Exception -> 0x0153 }
        L_0x015a:
            r21.delete()     // Catch:{ Exception -> 0x0153 }
            goto L_0x0174
        L_0x015e:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r0 = r0.toString()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.i(r6, r0)
        L_0x0174:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.MoveFile(java.io.File, java.io.File):boolean");
    }

    public static boolean IsSelfInstallAPKEnabled() {
        boolean[] GetBoolean = Resources.GetBoolean("SelfInstallAPK");
        if (GetBoolean == null || !GetBoolean[0]) {
            return false;
        }
        return true;
    }

    public static long CRC32File(File file) {
        return JNI_Environment.CRC32File(file);
    }

    public static long CRC32Buffer(byte[] bArr, long j, long j2) {
        if (bArr == null || j2 == 0) {
            return 0;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(bArr, (int) j, (int) j2);
        return crc32.getValue();
    }

    public static long CRC32String(String str) {
        if (str == null) {
            return 0;
        }
        byte[] bytes = str.getBytes();
        return CRC32Buffer(bytes, 0, (long) bytes.length);
    }

    /* JADX WARNING: Can't wrap try/catch for region: R(11:10|11|12|(4:15|(2:17|(2:19|41)(2:20|(2:22|43)(1:42)))(1:40)|39|13)|23|24|(6:28|29|30|45|44|25)|31|32|33|34) */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:23:0x00eb */
    /* JADX WARNING: Missing exception handler attribute for start block: B:31:0x0120 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:33:0x0123 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:35:0x0126 */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00f5 A[Catch:{ Throwable -> 0x0120 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean UpdateOldInstallationPaths(com.valvesoftware.PatchSystem.PatchSystemCallbacks r12) {
        /*
            r11 = this;
            com.valvesoftware.PatchSystem$CRegistry r12 = r11.m_Registry
            int r12 = r12.GetLastFullyInstalledAppVersion()
            java.lang.String r0 = "patchsystemver"
            r1 = 1
            if (r12 != 0) goto L_0x001c
            com.valvesoftware.PatchSystem$CRegistry r12 = r11.m_Registry     // Catch:{ Throwable -> 0x001b }
            android.content.SharedPreferences r12 = r12.m_SharedPreferences     // Catch:{ Throwable -> 0x001b }
            android.content.SharedPreferences$Editor r12 = r12.edit()     // Catch:{ Throwable -> 0x001b }
            r12.putInt(r0, r1)     // Catch:{ Throwable -> 0x001b }
            r12.apply()     // Catch:{ Throwable -> 0x001b }
        L_0x001b:
            return r1
        L_0x001c:
            com.valvesoftware.PatchSystem$CRegistry r12 = r11.m_Registry
            android.content.SharedPreferences r12 = r12.m_SharedPreferences
            r2 = 0
            int r12 = r12.getInt(r0, r2)
            if (r12 >= r1) goto L_0x0136
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "UpdateOldInstallationPaths() "
            r2.append(r3)
            r2.append(r12)
            java.lang.String r3 = " -> "
            r2.append(r3)
            r2.append(r1)
            java.lang.String r2 = r2.toString()
            java.lang.String r3 = "com.valvesoftware.PatchSystem"
            android.util.Log.i(r3, r2)
            if (r12 >= r1) goto L_0x0126
            java.io.File r12 = new java.io.File
            java.io.File r2 = com.valvesoftware.JNI_Environment.GetPublicPath()
            java.lang.String r4 = "WRITABLE_MANIFEST"
            r12.<init>(r2, r4)
            java.io.File r2 = new java.io.File
            java.io.File r4 = com.valvesoftware.JNI_Environment.GetPrivatePath()
            java.lang.String r5 = "PERSISTENT_STORAGE"
            r2.<init>(r4, r5)
            java.io.File r4 = new java.io.File
            java.io.File r5 = com.valvesoftware.JNI_Environment.GetPublicPath()
            java.lang.String r6 = "game"
            r4.<init>(r5, r6)
            java.io.File r5 = new java.io.File
            r5.<init>(r2, r6)
            java.util.ArrayList r2 = new java.util.ArrayList
            r2.<init>()
            com.valvesoftware.PatchSystem$CRegistry r6 = r11.m_Registry     // Catch:{ Throwable -> 0x00eb }
            android.content.SharedPreferences r6 = r6.m_SharedPreferences     // Catch:{ Throwable -> 0x00eb }
            java.util.Map r6 = r6.getAll()     // Catch:{ Throwable -> 0x00eb }
            java.util.Set r6 = r6.entrySet()     // Catch:{ Throwable -> 0x00eb }
            java.util.Iterator r6 = r6.iterator()     // Catch:{ Throwable -> 0x00eb }
        L_0x0086:
            boolean r7 = r6.hasNext()     // Catch:{ Throwable -> 0x00eb }
            if (r7 == 0) goto L_0x00eb
            java.lang.Object r7 = r6.next()     // Catch:{ Throwable -> 0x00eb }
            java.util.Map$Entry r7 = (java.util.Map.Entry) r7     // Catch:{ Throwable -> 0x00eb }
            java.lang.Object r7 = r7.getKey()     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r7 = (java.lang.String) r7     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r8 = "assetversion:"
            boolean r8 = r7.startsWith(r8)     // Catch:{ Throwable -> 0x00eb }
            if (r8 == 0) goto L_0x0086
            r2.add(r7)     // Catch:{ Throwable -> 0x00eb }
            r8 = 13
            int r9 = r7.length()     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r7 = r7.substring(r8, r9)     // Catch:{ Throwable -> 0x00eb }
            java.io.File r8 = new java.io.File     // Catch:{ Throwable -> 0x00eb }
            java.io.File r9 = com.valvesoftware.JNI_Environment.GetPublicPath()     // Catch:{ Throwable -> 0x00eb }
            r8.<init>(r9, r7)     // Catch:{ Throwable -> 0x00eb }
            boolean r9 = r8.exists()     // Catch:{ Throwable -> 0x00eb }
            if (r9 != 0) goto L_0x00bd
            goto L_0x0086
        L_0x00bd:
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00eb }
            r9.<init>()     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r10 = "UpdateOldInstallationPaths() moving registry file "
            r9.append(r10)     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r10 = r8.getAbsolutePath()     // Catch:{ Throwable -> 0x00eb }
            r9.append(r10)     // Catch:{ Throwable -> 0x00eb }
            java.lang.String r9 = r9.toString()     // Catch:{ Throwable -> 0x00eb }
            android.util.Log.i(r3, r9)     // Catch:{ Throwable -> 0x00eb }
            java.io.File r9 = new java.io.File     // Catch:{ Throwable -> 0x00eb }
            r9.<init>(r12, r7)     // Catch:{ Throwable -> 0x00eb }
            java.io.File r7 = r9.getParentFile()     // Catch:{ Throwable -> 0x00eb }
            r7.mkdirs()     // Catch:{ Throwable -> 0x00eb }
            boolean r7 = r11.MoveFile(r8, r9)     // Catch:{ Throwable -> 0x00eb }
            if (r7 != 0) goto L_0x0086
            r8.delete()     // Catch:{ Throwable -> 0x00eb }
            goto L_0x0086
        L_0x00eb:
            java.util.Iterator r12 = r2.iterator()     // Catch:{ Throwable -> 0x0120 }
        L_0x00ef:
            boolean r2 = r12.hasNext()     // Catch:{ Throwable -> 0x0120 }
            if (r2 == 0) goto L_0x0120
            java.lang.Object r2 = r12.next()     // Catch:{ Throwable -> 0x0120 }
            java.lang.String r2 = (java.lang.String) r2     // Catch:{ Throwable -> 0x0120 }
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x0120 }
            r6.<init>()     // Catch:{ Throwable -> 0x0120 }
            java.lang.String r7 = "UpdateOldInstallationPaths() removing registry key "
            r6.append(r7)     // Catch:{ Throwable -> 0x0120 }
            r6.append(r2)     // Catch:{ Throwable -> 0x0120 }
            java.lang.String r6 = r6.toString()     // Catch:{ Throwable -> 0x0120 }
            android.util.Log.i(r3, r6)     // Catch:{ Throwable -> 0x0120 }
            com.valvesoftware.PatchSystem$CRegistry r6 = r11.m_Registry     // Catch:{ Throwable -> 0x00ef }
            android.content.SharedPreferences r6 = r6.m_SharedPreferences     // Catch:{ Throwable -> 0x00ef }
            android.content.SharedPreferences$Editor r6 = r6.edit()     // Catch:{ Throwable -> 0x00ef }
            r6.remove(r2)     // Catch:{ Throwable -> 0x00ef }
            r6.apply()     // Catch:{ Throwable -> 0x00ef }
            goto L_0x00ef
        L_0x0120:
            r11.MoveRecursive(r4, r5)     // Catch:{ Throwable -> 0x0123 }
        L_0x0123:
            r4.delete()     // Catch:{ Throwable -> 0x0126 }
        L_0x0126:
            com.valvesoftware.PatchSystem$CRegistry r12 = r11.m_Registry     // Catch:{ Throwable -> 0x0136 }
            android.content.SharedPreferences r12 = r12.m_SharedPreferences     // Catch:{ Throwable -> 0x0136 }
            android.content.SharedPreferences$Editor r12 = r12.edit()     // Catch:{ Throwable -> 0x0136 }
            r12.putInt(r0, r1)     // Catch:{ Throwable -> 0x0136 }
            r12.apply()     // Catch:{ Throwable -> 0x0136 }
        L_0x0136:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.UpdateOldInstallationPaths(com.valvesoftware.PatchSystem$PatchSystemCallbacks):boolean");
    }

    /* access modifiers changed from: package-private */
    public void MoveRecursive(File file, File file2) {
        try {
            if (file.exists()) {
                int i = 0;
                if (file.isDirectory()) {
                    File[] listFiles = file.listFiles();
                    int length = listFiles.length;
                    while (i < length) {
                        File file3 = listFiles[i];
                        MoveRecursive(file3, new File(file2, file3.getName()));
                        i++;
                    }
                    return;
                }
                String absolutePath = file.getAbsolutePath();
                String substring = absolutePath.substring(JNI_Environment.GetPublicPath().getAbsolutePath().length() + 1, absolutePath.length());
                if (substring.endsWith(".gi") || substring.endsWith(".vpk") || substring.endsWith(".vdacdefs_c") || substring.endsWith(".webm") || substring.contains("/.fontconfig/") || substring.contains("/fonts/") || substring.contains("/localization/") || substring.contains("/resource/") || substring.contains("/WINDOWSTEMPDIR_FONTCONFIG_CACHE/")) {
                    i = 1;
                }
                if (i != 0) {
                    Log.i(k_sSpewPackageName, "MoveRecursive() filtering file " + file.getAbsolutePath());
                    return;
                }
                SharedPreferences access$300 = this.m_Registry.m_SharedPreferences;
                if (access$300.getString("assetversion:" + substring, (String) null) == null) {
                    Log.i(k_sSpewPackageName, "MoveRecursive() moving remaining file " + file.getAbsolutePath() + " to " + file2.getAbsolutePath());
                    if (!MoveFile(file, file2)) {
                        file.delete();
                    }
                }
            }
        } catch (Throwable unused) {
        }
    }
}
