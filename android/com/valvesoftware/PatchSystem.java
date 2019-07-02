package com.valvesoftware;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONObject;

public class PatchSystem {
    private static PatchSystem s_Instance;
    private final String k_sSpewPackageName = "com.valvesoftware.PatchSystem";
    private JSONObject m_JSONManifest = null;
    private CRegistry m_Registry;
    /* access modifiers changed from: private */
    public boolean m_bDownloadingAPK = false;
    private volatile Uri m_downloadedAPKLocation;
    private volatile EState m_eState = EState.Unstarted;
    /* access modifiers changed from: private */
    public volatile EUserDownloadResponse m_eUserResponse = EUserDownloadResponse.Waiting;
    /* access modifiers changed from: private */
    public HashMap<Long, PendingDownload> m_mapPendingDownloads = new HashMap<>();
    private int m_nApplicationVersion;
    private volatile long m_nCompletedDownloadBytes = 0;
    private volatile EErrorCode m_nErrorCode = EErrorCode.Unknown;
    private volatile long m_nPotentialDownloadBytes = 0;
    /* access modifiers changed from: private */
    public volatile long m_nTotalDownloadBytes = 0;
    private volatile String m_newAPKUrl;
    private String m_strSyncPath = null;
    Handler m_timerUserReponseHandler = new Handler();
    Runnable m_timerUserResponseRunnable = new Runnable() {
        public boolean m_bIsDone;

        public void run() {
            String str = "com.valvesoftware.PatchSystem";
            if (PatchSystem.this.m_eUserResponse == EUserDownloadResponse.SkipDownloadAPK) {
                PatchSystem.this.m_timerUserReponseHandler.removeCallbacks(PatchSystem.this.m_timerUserResponseRunnable);
                Log.i(str, "Skipping APK download, we're done.");
                PatchSystem.this.OnHaveCurrentAPK();
            } else if (PatchSystem.this.m_eUserResponse == EUserDownloadResponse.DownloadAPK) {
                PatchSystem.this.m_timerUserReponseHandler.removeCallbacks(PatchSystem.this.m_timerUserResponseRunnable);
                Log.i(str, "Downloading APK.");
                PatchSystem.this.OnContinueAPKDownload();
            } else if (PatchSystem.this.m_eUserResponse == EUserDownloadResponse.SkipDownloadVPK) {
                PatchSystem.this.m_timerUserReponseHandler.removeCallbacks(PatchSystem.this.m_timerUserResponseRunnable);
                Log.i(str, "Skipping VPK download, we're done.");
                PatchSystem.this.SetState(EState.Done);
            } else if (PatchSystem.this.m_eUserResponse == EUserDownloadResponse.DownloadVPK) {
                PatchSystem.this.m_timerUserReponseHandler.removeCallbacks(PatchSystem.this.m_timerUserResponseRunnable);
                Log.i(str, "Downloading VPKs.");
                PatchSystem.this.OnContinueFileDownload();
            } else {
                PatchSystem.this.m_timerUserReponseHandler.postDelayed(this, 200);
            }
        }
    };
    ArrayList<PendingDownload> m_vecPendingDownloads = null;

    private class CAsyncDownloadManagerTask extends AsyncTask<Void, Void, Boolean> {
        private CAsyncDownloadManagerTask() {
        }

        /* access modifiers changed from: protected */
        public Boolean doInBackground(Void... voidArr) {
            Context applicationContext = JNI_Environment.m_application.getApplicationContext();
            final DownloadManager downloadManager = (DownloadManager) applicationContext.getSystemService("download");
            applicationContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    long longExtra = intent.getLongExtra("extra_download_id", -1);
                    boolean z = true;
                    boolean z2 = false;
                    Cursor query = downloadManager.query(new Query().setFilterById(new long[]{longExtra}));
                    int i = -1;
                    String str = null;
                    if (query.moveToFirst()) {
                        if (query.getInt(query.getColumnIndex(NotificationCompat.CATEGORY_STATUS)) == 8) {
                            z2 = true;
                        }
                        if (!z2) {
                            i = query.getInt(query.getColumnIndex("reason"));
                        } else {
                            str = query.getString(query.getColumnIndex("local_uri"));
                        }
                    } else {
                        Log.i("com.valvesoftware.PatchSystem", "moveToFirst failed");
                        z = false;
                    }
                    query.close();
                    if (!z) {
                        return;
                    }
                    if (z2) {
                        PatchSystem.this.OnDownloadResponseSuccess(longExtra, str);
                    } else {
                        PatchSystem.this.OnDownloadResponseFailure(longExtra, i);
                    }
                }
            }, new IntentFilter("android.intent.action.DOWNLOAD_COMPLETE"));
            Iterator it = PatchSystem.this.m_vecPendingDownloads.iterator();
            while (it.hasNext()) {
                PendingDownload pendingDownload = (PendingDownload) it.next();
                Request request = new Request(Uri.parse(pendingDownload.strURL));
                if (PatchSystem.this.m_bDownloadingAPK) {
                    request.setDestinationUri(pendingDownload.uriDestinationPath);
                } else {
                    File externalCacheDir = applicationContext.getExternalCacheDir();
                    StringBuilder sb = new StringBuilder();
                    sb.append("download_");
                    sb.append(Integer.toHexString(pendingDownload.strFilePath.hashCode()));
                    sb.append("_");
                    sb.append(pendingDownload.strVersionCode);
                    sb.append(".tmp");
                    request.setDestinationUri(Uri.fromFile(new File(externalCacheDir, sb.toString())));
                }
                request.setTitle(pendingDownload.strFilePath);
                request.setDescription(pendingDownload.strFilePath);
                request.setNotificationVisibility(0);
                pendingDownload.nDownloadID = downloadManager.enqueue(request);
                synchronized (PatchSystem.this.m_mapPendingDownloads) {
                    PatchSystem.this.m_mapPendingDownloads.put(Long.valueOf(pendingDownload.nDownloadID), pendingDownload);
                    PatchSystem.this.m_nTotalDownloadBytes = PatchSystem.this.m_nTotalDownloadBytes + pendingDownload.nByteSize;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Download Queued: ");
                sb2.append(pendingDownload.strURL);
                sb2.append(" (DownloadID: ");
                sb2.append(String.valueOf(pendingDownload.nDownloadID));
                sb2.append(")");
                Log.i("com.valvesoftware.PatchSystem", sb2.toString());
            }
            PatchSystem.this.m_vecPendingDownloads = null;
            return Boolean.valueOf(true);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Boolean bool) {
            super.onPostExecute(bool);
            PatchSystem.this.SetState(EState.AssetsDownloading);
        }
    }

    private class CRegistry {
        private SharedPreferences m_SharedPreferences;

        public CRegistry() {
            Context applicationContext = JNI_Environment.m_application.getApplicationContext();
            String GetString = Resources.GetString("VPC_GameName");
            StringBuilder sb = new StringBuilder();
            sb.append("PatchSystemRegistry.");
            sb.append(GetString);
            this.m_SharedPreferences = applicationContext.getSharedPreferences(sb.toString(), 0);
        }

        public int GetLastFullyInstalledAppVersion() {
            return this.m_SharedPreferences.getInt("lastfullyinstalledappversion", 0);
        }

        public void SetLastFullyInstalledAppVersion(int i) {
            Editor edit = this.m_SharedPreferences.edit();
            edit.putInt("lastfullyinstalledappversion", i);
            edit.apply();
        }

        public boolean HasAssetVersion(String str, String str2) {
            StringBuilder sb = new StringBuilder();
            sb.append("assetversion:");
            sb.append(str);
            String string = this.m_SharedPreferences.getString(sb.toString(), null);
            if (string == null) {
                return false;
            }
            return str2.equals(string);
        }

        public void SetAssetVersion(String str, String str2) {
            StringBuilder sb = new StringBuilder();
            sb.append("assetversion:");
            sb.append(str);
            String sb2 = sb.toString();
            Editor edit = this.m_SharedPreferences.edit();
            edit.putString(sb2, str2);
            edit.apply();
        }
    }

    public enum EErrorCode {
        None,
        Manifest,
        Download,
        Storage,
        Unknown
    }

    public enum EState {
        Unstarted,
        ManifestDownloading,
        APKOutOfDateWaitingOnUser,
        APKDownloadedWaitingOnUser,
        ManifestDownloadedWaitingOnUser,
        AssetsQueueing,
        AssetsDownloading,
        Done,
        Error
    }

    public enum EUserDownloadResponse {
        Waiting,
        DownloadAPK,
        DownloadVPK,
        SkipDownloadAPK,
        SkipDownloadVPK
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

    private void WaitForUserInput(EState eState, EErrorCode eErrorCode) {
        this.m_eUserResponse = EUserDownloadResponse.Waiting;
        this.m_nErrorCode = eErrorCode;
        SetState(eState);
        this.m_timerUserReponseHandler.postDelayed(this.m_timerUserResponseRunnable, 1000);
    }

    private void ClearPendingDownloads() {
        int i;
        File[] listFiles;
        Context applicationContext = JNI_Environment.m_application.getApplicationContext();
        DownloadManager downloadManager = (DownloadManager) applicationContext.getSystemService("download");
        Query query = new Query();
        query.setFilterByStatus(31);
        Cursor query2 = downloadManager.query(query);
        while (true) {
            if (!query2.moveToNext()) {
                break;
            }
            downloadManager.remove(new long[]{query2.getLong(query2.getColumnIndex("_id"))});
        }
        query2.close();
        for (File file : applicationContext.getExternalCacheDir().listFiles()) {
            if (file.getName().startsWith("download_") && file.getName().endsWith(".tmp")) {
                boolean delete = file.delete();
                StringBuilder sb = new StringBuilder();
                sb.append("Cleaning up download cache file: ");
                sb.append(file.getAbsolutePath());
                sb.append(delete ? " (Success)" : " (Failure)");
                Log.i("com.valvesoftware.PatchSystem", sb.toString());
            }
        }
    }

    public void Start(String str, int i) {
        this.m_strSyncPath = JNI_Environment.GetPublicPath().getAbsolutePath();
        String str2 = "com.valvesoftware.PatchSystem";
        Log.i(str2, "Starting...");
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest URL: ");
        sb.append(str);
        Log.i(str2, sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Sync Path: ");
        sb2.append(this.m_strSyncPath);
        Log.i(str2, sb2.toString());
        this.m_Registry = new CRegistry();
        this.m_nApplicationVersion = i;
        ClearPendingDownloads();
        SetState(EState.ManifestDownloading);
        Volley.newRequestQueue(JNI_Environment.m_application).add(new JsonObjectRequest(str, null, new Listener<JSONObject>() {
            public void onResponse(JSONObject jSONObject) {
                PatchSystem.this.OnManifestResponseSuccess(jSONObject);
            }
        }, new ErrorListener() {
            public void onErrorResponse(VolleyError volleyError) {
                PatchSystem.this.OnManifestResponseFailure(volleyError.toString());
            }
        }));
    }

    private long GetAvailableStorageBytes() {
        return new StatFs(this.m_strSyncPath).getAvailableBytes();
    }

    /* access modifiers changed from: private */
    public void OnManifestResponseSuccess(JSONObject jSONObject) {
        this.m_JSONManifest = jSONObject;
        boolean z = false;
        try {
            JSONObject jSONObject2 = this.m_JSONManifest.getJSONObject("packages");
            if (jSONObject2 != null) {
                JSONObject jSONObject3 = null;
                try {
                    String str = Build.SUPPORTED_ABIS[0];
                    if (str.equals("armeabi-v7a")) {
                        jSONObject3 = jSONObject2.getJSONObject("androidarm32");
                    } else if (str.equals("arm64-v8a")) {
                        jSONObject3 = jSONObject2.getJSONObject("androidarm64");
                    }
                    if (jSONObject3 == null) {
                        WaitForUserInput(EState.Error, EErrorCode.Manifest);
                        return;
                    }
                    this.m_newAPKUrl = jSONObject3.getString("url");
                    this.m_nPotentialDownloadBytes = (long) jSONObject3.getInt("size");
                    if (GetAvailableStorageBytes() <= this.m_nPotentialDownloadBytes) {
                        WaitForUserInput(EState.Error, EErrorCode.Storage);
                        return;
                    }
                    z = true;
                } catch (Exception unused) {
                }
            }
        } catch (Exception unused2) {
        }
        if (z) {
            WaitForUserInput(EState.APKOutOfDateWaitingOnUser, EErrorCode.None);
        } else {
            OnHaveCurrentAPK();
        }
    }

    /* access modifiers changed from: private */
    public void OnHaveCurrentAPK() {
        String str = "com.valvesoftware.PatchSystem";
        this.m_vecPendingDownloads = new ArrayList<>();
        try {
            String string = this.m_JSONManifest.getString("cdnroot");
            JSONObject jSONObject = this.m_JSONManifest.getJSONObject("assets");
            Iterator keys = jSONObject.keys();
            while (keys.hasNext()) {
                String str2 = (String) keys.next();
                JSONObject jSONObject2 = jSONObject.getJSONObject(str2);
                int i = jSONObject2.getInt("bytesize");
                String string2 = jSONObject2.getString("version");
                File file = new File(this.m_strSyncPath, str2);
                if (!file.exists()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Forcing Download for Missing Asset: ");
                    sb.append(str2);
                    Log.i(str, sb.toString());
                } else if (this.m_Registry.HasAssetVersion(str2, string2)) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Skipping Download for Existing Asset: ");
                    sb2.append(str2);
                    Log.i(str, sb2.toString());
                }
                PendingDownload pendingDownload = new PendingDownload();
                pendingDownload.strFilePath = str2;
                StringBuilder sb3 = new StringBuilder();
                sb3.append(string);
                sb3.append(str2);
                pendingDownload.strURL = sb3.toString();
                pendingDownload.strVersionCode = string2;
                pendingDownload.uriDestinationPath = Uri.fromFile(file);
                pendingDownload.nByteSize = (long) i;
                this.m_vecPendingDownloads.add(pendingDownload);
            }
            if (this.m_vecPendingDownloads.isEmpty()) {
                Log.i(str, "All files up-to-date, we're done.");
                SetState(EState.Done);
                return;
            }
            this.m_Registry.SetLastFullyInstalledAppVersion(0);
            this.m_nPotentialDownloadBytes = 0;
            Iterator it = this.m_vecPendingDownloads.iterator();
            while (it.hasNext()) {
                this.m_nPotentialDownloadBytes += ((PendingDownload) it.next()).nByteSize;
            }
            if (GetAvailableStorageBytes() <= this.m_nPotentialDownloadBytes) {
                WaitForUserInput(EState.Error, EErrorCode.Storage);
            } else {
                WaitForUserInput(EState.ManifestDownloadedWaitingOnUser, EErrorCode.None);
            }
        } catch (Exception e) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Manifest Exception: ");
            sb4.append(e.toString());
            Log.e(str, sb4.toString());
            WaitForUserInput(EState.Error, EErrorCode.Manifest);
        }
    }

    private boolean DeleteExistingFile(File file) {
        String str = "com.valvesoftware.PatchSystem";
        if (file.exists()) {
            if (!file.delete()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Couldn't Delete Existing File: ");
                sb.append(file.getAbsolutePath());
                Log.e(str, sb.toString());
                return false;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Deleted Existing File: ");
            sb2.append(file.getAbsolutePath());
            Log.i(str, sb2.toString());
        }
        if (!file.exists()) {
            return true;
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Didn't Actually Delete Existing File: ");
        sb3.append(file.getAbsolutePath());
        Log.e(str, sb3.toString());
        return false;
    }

    /* access modifiers changed from: private */
    public void OnContinueAPKDownload() {
        boolean[] GetBoolean = Resources.GetBoolean("VPC_SelfInstallAPK");
        boolean z = false;
        if (GetBoolean != null && GetBoolean[0]) {
            z = true;
        }
        if (z) {
            SetState(EState.AssetsDownloading);
            this.m_bDownloadingAPK = true;
            Context applicationContext = JNI_Environment.m_application.getApplicationContext();
            String str = "_underlords_tmp_install_.apk";
            File file = new File(applicationContext.getExternalCacheDir(), str);
            if (!DeleteExistingFile(file)) {
                WaitForUserInput(EState.Error, EErrorCode.Manifest);
                return;
            }
            this.m_vecPendingDownloads = new ArrayList<>();
            try {
                PendingDownload pendingDownload = new PendingDownload();
                pendingDownload.strFilePath = str;
                pendingDownload.strURL = this.m_newAPKUrl;
                pendingDownload.strVersionCode = "01";
                pendingDownload.uriDestinationPath = Uri.fromFile(file);
                pendingDownload.nByteSize = this.m_nPotentialDownloadBytes;
                this.m_vecPendingDownloads.add(pendingDownload);
                StringBuilder sb = new StringBuilder();
                sb.append(applicationContext.getApplicationContext().getPackageName());
                sb.append(".provider");
                this.m_downloadedAPKLocation = FileProvider.getUriForFile(applicationContext, sb.toString(), file);
                OnContinueFileDownload();
            } catch (Exception e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Manifest Exception: ");
                sb2.append(e.toString());
                Log.e("com.valvesoftware.PatchSystem", sb2.toString());
                WaitForUserInput(EState.Error, EErrorCode.Manifest);
            }
        } else {
            SetState(EState.APKDownloadedWaitingOnUser);
        }
    }

    /* access modifiers changed from: private */
    public void OnContinueFileDownload() {
        SetState(EState.AssetsQueueing);
        new CAsyncDownloadManagerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void OnManifestResponseFailure(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error: ");
        sb.append(str.toString());
        Log.e("com.valvesoftware.PatchSystem", sb.toString());
        WaitForUserInput(EState.Error, EErrorCode.Manifest);
    }

    private boolean ProcessCompletedDownload(PendingDownload pendingDownload, String str) {
        File file = new File(Uri.parse(str).getPath());
        String str2 = "com.valvesoftware.PatchSystem";
        if (!file.exists()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Download Not Found: ");
            sb.append(file.getAbsolutePath());
            Log.e(str2, sb.toString());
            return false;
        } else if (file.length() <= 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Downloaded Zero Bytes: ");
            sb2.append(file.getAbsolutePath());
            Log.e(str2, sb2.toString());
            return false;
        } else if (this.m_bDownloadingAPK) {
            return true;
        } else {
            File file2 = new File(pendingDownload.uriDestinationPath.getPath());
            file2.mkdirs();
            if (!DeleteExistingFile(file2)) {
                return false;
            }
            String str3 = " to ";
            if (!file.renameTo(file2)) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Couldn't Move File: ");
                sb3.append(file.getAbsolutePath());
                sb3.append(str3);
                sb3.append(file2.getAbsolutePath());
                Log.e(str2, sb3.toString());
                StringBuilder sb4 = new StringBuilder();
                sb4.append("Attempting to Copy then Delete: ");
                sb4.append(file.getAbsolutePath());
                sb4.append(str3);
                sb4.append(file2.getAbsolutePath());
                Log.e(str2, sb4.toString());
                if (!MoveFile(file, file2)) {
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append("Couldn't Copy then Delete: ");
                    sb5.append(file.getAbsolutePath());
                    sb5.append(str3);
                    sb5.append(file2.getAbsolutePath());
                    Log.e(str2, sb5.toString());
                    return false;
                }
            }
            StringBuilder sb6 = new StringBuilder();
            sb6.append("Moved File: ");
            sb6.append(file.getAbsolutePath());
            sb6.append(str3);
            sb6.append(file2.getAbsolutePath());
            Log.i(str2, sb6.toString());
            this.m_Registry.SetAssetVersion(pendingDownload.strFilePath, pendingDownload.strVersionCode);
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0105, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void OnDownloadResponseSuccess(long r6, java.lang.String r8) {
        /*
            r5 = this;
            com.valvesoftware.PatchSystem$EState r0 = r5.m_eState
            com.valvesoftware.PatchSystem$EState r1 = com.valvesoftware.PatchSystem.EState.AssetsDownloading
            if (r0 == r1) goto L_0x0029
            com.valvesoftware.PatchSystem$EState r0 = r5.m_eState
            com.valvesoftware.PatchSystem$EState r1 = com.valvesoftware.PatchSystem.EState.AssetsQueueing
            if (r0 == r1) goto L_0x0029
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "OnDownloadResponseSuccess: Skipping due to being in the wrong state! Current state: "
            r6.append(r7)
            com.valvesoftware.PatchSystem$EState r7 = r5.m_eState
            java.lang.String r7 = r7.toString()
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            java.lang.String r7 = "com.valvesoftware.PatchSystem"
            android.util.Log.w(r7, r6)
            return
        L_0x0029:
            android.app.Application r0 = com.valvesoftware.JNI_Environment.m_application
            android.content.Context r0 = r0.getApplicationContext()
            java.lang.String r1 = "download"
            java.lang.Object r0 = r0.getSystemService(r1)
            android.app.DownloadManager r0 = (android.app.DownloadManager) r0
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r1 = r5.m_mapPendingDownloads
            monitor-enter(r1)
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r2 = r5.m_mapPendingDownloads     // Catch:{ all -> 0x0106 }
            java.lang.Long r3 = java.lang.Long.valueOf(r6)     // Catch:{ all -> 0x0106 }
            boolean r2 = r2.containsKey(r3)     // Catch:{ all -> 0x0106 }
            if (r2 != 0) goto L_0x0067
            java.lang.String r8 = "com.valvesoftware.PatchSystem"
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ all -> 0x0106 }
            r0.<init>()     // Catch:{ all -> 0x0106 }
            java.lang.String r2 = "Download Error: "
            r0.append(r2)     // Catch:{ all -> 0x0106 }
            java.lang.String r6 = java.lang.String.valueOf(r6)     // Catch:{ all -> 0x0106 }
            r0.append(r6)     // Catch:{ all -> 0x0106 }
            java.lang.String r6 = " (Not found in Pending Downloads)"
            r0.append(r6)     // Catch:{ all -> 0x0106 }
            java.lang.String r6 = r0.toString()     // Catch:{ all -> 0x0106 }
            android.util.Log.e(r8, r6)     // Catch:{ all -> 0x0106 }
            monitor-exit(r1)     // Catch:{ all -> 0x0106 }
            return
        L_0x0067:
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r2 = r5.m_mapPendingDownloads     // Catch:{ all -> 0x0106 }
            java.lang.Long r3 = java.lang.Long.valueOf(r6)     // Catch:{ all -> 0x0106 }
            java.lang.Object r2 = r2.get(r3)     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$PendingDownload r2 = (com.valvesoftware.PatchSystem.PendingDownload) r2     // Catch:{ all -> 0x0106 }
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r3 = r5.m_mapPendingDownloads     // Catch:{ all -> 0x0106 }
            java.lang.Long r6 = java.lang.Long.valueOf(r6)     // Catch:{ all -> 0x0106 }
            r3.remove(r6)     // Catch:{ all -> 0x0106 }
            java.lang.String r6 = "com.valvesoftware.PatchSystem"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x0106 }
            r7.<init>()     // Catch:{ all -> 0x0106 }
            java.lang.String r3 = "Download Success: "
            r7.append(r3)     // Catch:{ all -> 0x0106 }
            java.lang.String r3 = r2.strURL     // Catch:{ all -> 0x0106 }
            r7.append(r3)     // Catch:{ all -> 0x0106 }
            java.lang.String r3 = " (DownloadID: "
            r7.append(r3)     // Catch:{ all -> 0x0106 }
            long r3 = r2.nDownloadID     // Catch:{ all -> 0x0106 }
            java.lang.String r3 = java.lang.String.valueOf(r3)     // Catch:{ all -> 0x0106 }
            r7.append(r3)     // Catch:{ all -> 0x0106 }
            java.lang.String r3 = ")"
            r7.append(r3)     // Catch:{ all -> 0x0106 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x0106 }
            android.util.Log.i(r6, r7)     // Catch:{ all -> 0x0106 }
            boolean r6 = r5.ProcessCompletedDownload(r2, r8)     // Catch:{ all -> 0x0106 }
            if (r6 != 0) goto L_0x00b9
            r5.ClearPendingDownloads()     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.Error     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$EErrorCode r7 = com.valvesoftware.PatchSystem.EErrorCode.Download     // Catch:{ all -> 0x0106 }
            r5.WaitForUserInput(r6, r7)     // Catch:{ all -> 0x0106 }
            monitor-exit(r1)     // Catch:{ all -> 0x0106 }
            return
        L_0x00b9:
            boolean r6 = r5.m_bDownloadingAPK     // Catch:{ all -> 0x0106 }
            if (r6 != 0) goto L_0x00c8
            r6 = 1
            long[] r6 = new long[r6]     // Catch:{ all -> 0x0106 }
            r7 = 0
            long r3 = r2.nDownloadID     // Catch:{ all -> 0x0106 }
            r6[r7] = r3     // Catch:{ all -> 0x0106 }
            r0.remove(r6)     // Catch:{ all -> 0x0106 }
        L_0x00c8:
            long r6 = r5.m_nCompletedDownloadBytes     // Catch:{ all -> 0x0106 }
            long r2 = r2.nByteSize     // Catch:{ all -> 0x0106 }
            long r6 = r6 + r2
            r5.m_nCompletedDownloadBytes = r6     // Catch:{ all -> 0x0106 }
            boolean r6 = r5.m_bDownloadingAPK     // Catch:{ all -> 0x0106 }
            if (r6 == 0) goto L_0x00f0
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r6 = r5.m_mapPendingDownloads     // Catch:{ all -> 0x0106 }
            boolean r6 = r6.isEmpty()     // Catch:{ all -> 0x0106 }
            if (r6 == 0) goto L_0x00e1
            com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.APKDownloadedWaitingOnUser     // Catch:{ all -> 0x0106 }
            r5.SetState(r6)     // Catch:{ all -> 0x0106 }
            goto L_0x0104
        L_0x00e1:
            java.lang.String r6 = "com.valvesoftware.PatchSystem"
            java.lang.String r7 = "More than one pending APK download."
            android.util.Log.i(r6, r7)     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.Error     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$EErrorCode r7 = com.valvesoftware.PatchSystem.EErrorCode.Unknown     // Catch:{ all -> 0x0106 }
            r5.WaitForUserInput(r6, r7)     // Catch:{ all -> 0x0106 }
            goto L_0x0104
        L_0x00f0:
            java.util.HashMap<java.lang.Long, com.valvesoftware.PatchSystem$PendingDownload> r6 = r5.m_mapPendingDownloads     // Catch:{ all -> 0x0106 }
            boolean r6 = r6.isEmpty()     // Catch:{ all -> 0x0106 }
            if (r6 == 0) goto L_0x0104
            com.valvesoftware.PatchSystem$CRegistry r6 = r5.m_Registry     // Catch:{ all -> 0x0106 }
            int r7 = r5.m_nApplicationVersion     // Catch:{ all -> 0x0106 }
            r6.SetLastFullyInstalledAppVersion(r7)     // Catch:{ all -> 0x0106 }
            com.valvesoftware.PatchSystem$EState r6 = com.valvesoftware.PatchSystem.EState.Done     // Catch:{ all -> 0x0106 }
            r5.SetState(r6)     // Catch:{ all -> 0x0106 }
        L_0x0104:
            monitor-exit(r1)     // Catch:{ all -> 0x0106 }
            return
        L_0x0106:
            r6 = move-exception
            monitor-exit(r1)     // Catch:{ all -> 0x0106 }
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.OnDownloadResponseSuccess(long, java.lang.String):void");
    }

    /* access modifiers changed from: private */
    public void OnDownloadResponseFailure(long j, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Download Failure: ");
        sb.append(String.valueOf(j));
        sb.append(" (Reason: ");
        sb.append(String.valueOf(i));
        sb.append(")");
        Log.e("com.valvesoftware.PatchSystem", sb.toString());
        ClearPendingDownloads();
        WaitForUserInput(EState.Error, EErrorCode.Download);
    }

    /* access modifiers changed from: private */
    public void SetState(EState eState) {
        StringBuilder sb = new StringBuilder();
        sb.append("State Transition from ");
        sb.append(this.m_eState.toString());
        sb.append(" to ");
        sb.append(eState.toString());
        Log.i("com.valvesoftware.PatchSystem", sb.toString());
        this.m_eState = eState;
    }

    public EState GetState() {
        return this.m_eState;
    }

    public EErrorCode GetErrorCode() {
        return this.m_nErrorCode;
    }

    public boolean CanPlayOffline() {
        return this.m_Registry.GetLastFullyInstalledAppVersion() == this.m_nApplicationVersion;
    }

    public long GetDownloadSizeBytes() {
        return this.m_nPotentialDownloadBytes;
    }

    public Uri GetDownloadedAPKLocation() {
        return this.m_downloadedAPKLocation;
    }

    public void SetUserDownloadResponse(EUserDownloadResponse eUserDownloadResponse) {
        this.m_eUserResponse = eUserDownloadResponse;
    }

    public float GetDownloadProgress() {
        synchronized (this.m_mapPendingDownloads) {
            if (this.m_mapPendingDownloads.isEmpty()) {
                return 1.0f;
            }
            long[] jArr = new long[this.m_mapPendingDownloads.size()];
            int i = 0;
            for (PendingDownload pendingDownload : this.m_mapPendingDownloads.values()) {
                int i2 = i + 1;
                jArr[i] = pendingDownload.nDownloadID;
                i = i2;
            }
            DownloadManager downloadManager = (DownloadManager) JNI_Environment.m_application.getApplicationContext().getSystemService("download");
            long j = this.m_nCompletedDownloadBytes;
            Query query = new Query();
            query.setFilterById(jArr);
            Cursor query2 = downloadManager.query(query);
            while (query2.moveToNext()) {
                j += query2.getLong(query2.getColumnIndex("bytes_so_far"));
            }
            query2.close();
            float f = ((float) j) / ((float) this.m_nTotalDownloadBytes);
            return f;
        }
    }

    public static PatchSystem GetInstance() {
        if (s_Instance == null) {
            s_Instance = new PatchSystem();
        }
        return s_Instance;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0067, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0069, code lost:
        r0 = e;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0067 A[ExcHandler: all (th java.lang.Throwable), Splitter:B:4:0x0009] */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00ab A[SYNTHETIC, Splitter:B:44:0x00ab] */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x00b3 A[Catch:{ Exception -> 0x00af }] */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d7 A[SYNTHETIC, Splitter:B:56:0x00d7] */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00df A[Catch:{ Exception -> 0x00db }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean MoveFile(java.io.File r19, java.io.File r20) {
        /*
            r18 = this;
            java.lang.String r1 = "MoveFile cleanup Exception: "
            java.lang.String r2 = "com.valvesoftware.PatchSystem"
            r3 = 0
            java.io.FileInputStream r0 = new java.io.FileInputStream     // Catch:{ Exception -> 0x0072, all -> 0x006b }
            r4 = r19
            r0.<init>(r4)     // Catch:{ Exception -> 0x0069, all -> 0x0067 }
            java.io.FileOutputStream r5 = new java.io.FileOutputStream     // Catch:{ Exception -> 0x0069, all -> 0x0067 }
            r6 = r20
            r5.<init>(r6)     // Catch:{ Exception -> 0x0065, all -> 0x0067 }
            java.nio.channels.FileChannel r13 = r0.getChannel()     // Catch:{ Exception -> 0x0065, all -> 0x0067 }
            java.nio.channels.FileChannel r3 = r5.getChannel()     // Catch:{ Exception -> 0x0061, all -> 0x005e }
            long r14 = r13.size()     // Catch:{ Exception -> 0x0061, all -> 0x005e }
            r7 = 0
            r16 = r7
        L_0x0023:
            int r0 = (r16 > r14 ? 1 : (r16 == r14 ? 0 : -1))
            if (r0 >= 0) goto L_0x0034
            long r10 = r14 - r16
            r7 = r13
            r8 = r16
            r12 = r3
            long r7 = r7.transferTo(r8, r10, r12)     // Catch:{ Exception -> 0x0061, all -> 0x005e }
            long r16 = r16 + r7
            goto L_0x0023
        L_0x0034:
            if (r13 == 0) goto L_0x003c
            r13.close()     // Catch:{ Exception -> 0x003a }
            goto L_0x003c
        L_0x003a:
            r0 = move-exception
            goto L_0x0045
        L_0x003c:
            if (r3 == 0) goto L_0x0041
            r3.close()     // Catch:{ Exception -> 0x003a }
        L_0x0041:
            r19.delete()     // Catch:{ Exception -> 0x003a }
            goto L_0x005b
        L_0x0045:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r0 = r0.toString()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.i(r2, r0)
        L_0x005b:
            r0 = 1
            goto L_0x00d1
        L_0x005e:
            r0 = move-exception
            r5 = r3
            goto L_0x0070
        L_0x0061:
            r0 = move-exception
            r5 = r3
            r3 = r13
            goto L_0x0078
        L_0x0065:
            r0 = move-exception
            goto L_0x0077
        L_0x0067:
            r0 = move-exception
            goto L_0x006e
        L_0x0069:
            r0 = move-exception
            goto L_0x0075
        L_0x006b:
            r0 = move-exception
            r4 = r19
        L_0x006e:
            r5 = r3
            r13 = r5
        L_0x0070:
            r3 = r0
            goto L_0x00d5
        L_0x0072:
            r0 = move-exception
            r4 = r19
        L_0x0075:
            r6 = r20
        L_0x0077:
            r5 = r3
        L_0x0078:
            r7 = 0
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ all -> 0x00d2 }
            r8.<init>()     // Catch:{ all -> 0x00d2 }
            java.lang.String r9 = "Couldn't Copy File: "
            r8.append(r9)     // Catch:{ all -> 0x00d2 }
            java.lang.String r9 = r19.getAbsolutePath()     // Catch:{ all -> 0x00d2 }
            r8.append(r9)     // Catch:{ all -> 0x00d2 }
            java.lang.String r9 = " to "
            r8.append(r9)     // Catch:{ all -> 0x00d2 }
            java.lang.String r6 = r20.getAbsolutePath()     // Catch:{ all -> 0x00d2 }
            r8.append(r6)     // Catch:{ all -> 0x00d2 }
            java.lang.String r6 = " Exception: "
            r8.append(r6)     // Catch:{ all -> 0x00d2 }
            java.lang.String r0 = r0.toString()     // Catch:{ all -> 0x00d2 }
            r8.append(r0)     // Catch:{ all -> 0x00d2 }
            java.lang.String r0 = r8.toString()     // Catch:{ all -> 0x00d2 }
            android.util.Log.e(r2, r0)     // Catch:{ all -> 0x00d2 }
            if (r3 == 0) goto L_0x00b1
            r3.close()     // Catch:{ Exception -> 0x00af }
            goto L_0x00b1
        L_0x00af:
            r0 = move-exception
            goto L_0x00ba
        L_0x00b1:
            if (r5 == 0) goto L_0x00b6
            r5.close()     // Catch:{ Exception -> 0x00af }
        L_0x00b6:
            r19.delete()     // Catch:{ Exception -> 0x00af }
            goto L_0x00d0
        L_0x00ba:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r1)
            java.lang.String r0 = r0.toString()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.i(r2, r0)
        L_0x00d0:
            r0 = 0
        L_0x00d1:
            return r0
        L_0x00d2:
            r0 = move-exception
            r13 = r3
            goto L_0x0070
        L_0x00d5:
            if (r13 == 0) goto L_0x00dd
            r13.close()     // Catch:{ Exception -> 0x00db }
            goto L_0x00dd
        L_0x00db:
            r0 = move-exception
            goto L_0x00e6
        L_0x00dd:
            if (r5 == 0) goto L_0x00e2
            r5.close()     // Catch:{ Exception -> 0x00db }
        L_0x00e2:
            r19.delete()     // Catch:{ Exception -> 0x00db }
            goto L_0x00fc
        L_0x00e6:
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r4.append(r1)
            java.lang.String r0 = r0.toString()
            r4.append(r0)
            java.lang.String r0 = r4.toString()
            android.util.Log.i(r2, r0)
        L_0x00fc:
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.PatchSystem.MoveFile(java.io.File, java.io.File):boolean");
    }
}
