package com.valvesoftware.source2launcher;

import android.util.Log;
import com.valvesoftware.BootStrapClient;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem;
import com.valvesoftware.Resources;
import com.valvesoftware.SelfInstall;

public class BootStrapClientContentSyncAsyncTask extends IContentSyncAsyncTask {
    private IStreamingBootStrap m_streamingBootStrapConnection;

    public BootStrapClientContentSyncAsyncTask(IStreamingBootStrap iStreamingBootStrap) {
        this.m_streamingBootStrapConnection = iStreamingBootStrap;
    }

    private boolean SyncContent() {
        String GetString = Resources.GetString("GameName");
        boolean z = JNI_Environment.GetAvailableStorageBytes(JNI_Environment.GetPublicPath()) < 0;
        String absolutePath = JNI_Environment.GetPublicPath().getAbsolutePath();
        String[] strArr = {"so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter = new BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, new String[]{"win32", "win64", "linuxsteamrt64", "osx32", "osx64"});
        IStreamingBootStrap iStreamingBootStrap = this.m_streamingBootStrapConnection;
        StringBuilder sb = new StringBuilder();
        sb.append(absolutePath);
        sb.append("/game/bin");
        boolean z2 = BootStrapClient.DownloadDirectory(iStreamingBootStrap, "game:/bin", sb.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter);
        if (z) {
            strArr = new String[]{"vsnd_c", "so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        }
        BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2 = new BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, (String[]) null);
        IStreamingBootStrap iStreamingBootStrap2 = this.m_streamingBootStrapConnection;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(absolutePath);
        sb2.append("/game/core");
        boolean z3 = BootStrapClient.DownloadDirectory(iStreamingBootStrap2, "game:/core", sb2.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z2;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 10);
        IStreamingBootStrap iStreamingBootStrap3 = this.m_streamingBootStrapConnection;
        StringBuilder sb3 = new StringBuilder();
        sb3.append(absolutePath);
        sb3.append("/game/mobile/core");
        boolean z4 = BootStrapClient.DownloadDirectory(iStreamingBootStrap3, "game:/mobile/core", sb3.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z3;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 20);
        IStreamingBootStrap iStreamingBootStrap4 = this.m_streamingBootStrapConnection;
        StringBuilder sb4 = new StringBuilder();
        sb4.append(absolutePath);
        sb4.append("/game/mobile/commandlines");
        boolean z5 = BootStrapClient.DownloadDirectory(iStreamingBootStrap4, "game:/mobile/commandlines", sb4.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z4;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 30);
        String str = "/game/";
        String str2 = "game:/";
        boolean z6 = BootStrapClient.DownloadDirectory(this.m_streamingBootStrapConnection, "game:/" + GetString, absolutePath + "/game/" + GetString, true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z5;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 40);
        IStreamingBootStrap iStreamingBootStrap5 = this.m_streamingBootStrapConnection;
        String str3 = str2 + GetString + "_addons";
        StringBuilder sb5 = new StringBuilder();
        sb5.append(absolutePath);
        sb5.append(str);
        sb5.append(GetString);
        sb5.append("_addons");
        boolean z7 = BootStrapClient.DownloadDirectory(iStreamingBootStrap5, str3, sb5.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z6;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 50);
        String str4 = "/game/mobile/";
        String str5 = "game:/mobile/";
        boolean z8 = BootStrapClient.DownloadDirectory(this.m_streamingBootStrapConnection, "game:/mobile/" + GetString, absolutePath + "/game/mobile/" + GetString, true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z7;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 60);
        IStreamingBootStrap iStreamingBootStrap6 = this.m_streamingBootStrapConnection;
        String str6 = str5 + GetString + "_addons";
        StringBuilder sb6 = new StringBuilder();
        sb6.append(absolutePath);
        sb6.append(str4);
        sb6.append(GetString);
        sb6.append("_addons");
        boolean z9 = BootStrapClient.DownloadDirectory(iStreamingBootStrap6, str6, sb6.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z8;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 70);
        if (z) {
            return z9;
        }
        IStreamingBootStrap iStreamingBootStrap7 = this.m_streamingBootStrapConnection;
        StringBuilder sb7 = new StringBuilder();
        sb7.append(absolutePath);
        sb7.append("/game_otherplatforms/etc/core");
        boolean z10 = BootStrapClient.DownloadDirectory(iStreamingBootStrap7, "game:../game_otherplatforms/etc/core", sb7.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z9;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 80);
        IStreamingBootStrap iStreamingBootStrap8 = this.m_streamingBootStrapConnection;
        String str7 = "game:../game_otherplatforms/etc/" + GetString;
        StringBuilder sb8 = new StringBuilder();
        sb8.append(absolutePath);
        sb8.append("/game_otherplatforms/etc/");
        sb8.append(GetString);
        boolean z11 = BootStrapClient.DownloadDirectory(iStreamingBootStrap8, str7, sb8.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z10;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 90);
        IStreamingBootStrap iStreamingBootStrap9 = this.m_streamingBootStrapConnection;
        String str8 = "game:../game_otherplatforms/etc/mobile/" + GetString;
        StringBuilder sb9 = new StringBuilder();
        sb9.append(absolutePath);
        sb9.append("/game_otherplatforms/etc/mobile/");
        sb9.append(GetString);
        boolean z12 = BootStrapClient.DownloadDirectory(iStreamingBootStrap9, str8, sb9.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z11;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 100);
        IStreamingBootStrap iStreamingBootStrap10 = this.m_streamingBootStrapConnection;
        StringBuilder sb10 = new StringBuilder();
        sb10.append(absolutePath);
        sb10.append("/game_otherplatforms/low_bitrate/core");
        boolean z13 = BootStrapClient.DownloadDirectory(iStreamingBootStrap10, "game:../game_otherplatforms/low_bitrate/core", sb10.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z12;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 80);
        IStreamingBootStrap iStreamingBootStrap11 = this.m_streamingBootStrapConnection;
        String str9 = "game:../game_otherplatforms/low_bitrate/" + GetString;
        StringBuilder sb11 = new StringBuilder();
        sb11.append(absolutePath);
        sb11.append("/game_otherplatforms/low_bitrate/");
        sb11.append(GetString);
        boolean z14 = BootStrapClient.DownloadDirectory(iStreamingBootStrap11, str9, sb11.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z13;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 90);
        IStreamingBootStrap iStreamingBootStrap12 = this.m_streamingBootStrapConnection;
        String str10 = "game:../game_otherplatforms/low_bitrate/mobile/" + GetString;
        StringBuilder sb12 = new StringBuilder();
        sb12.append(absolutePath);
        sb12.append("/game_otherplatforms/low_bitrate/mobile/");
        sb12.append(GetString);
        boolean z15 = BootStrapClient.DownloadDirectory(iStreamingBootStrap12, str10, sb12.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z14;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 100);
        return z15;
    }

    /* access modifiers changed from: protected */
    public final void onPostExecute(Boolean bool) {
        this.m_status.m_nState = PatchSystem.EState.Done;
        this.m_status.m_nErrorCode = PatchSystem.EErrorCode.None;
        this.m_status.m_nProgress = 100;
    }

    /* access modifiers changed from: protected */
    public Boolean doInBackground(Void... voidArr) {
        boolean z = false;
        updateProgress(PatchSystem.EState.AssetsDownloading, PatchSystem.EErrorCode.None, 0);
        try {
            if (SelfInstall.ShouldSyncContentFromBootstrap(this.m_streamingBootStrapConnection)) {
                boolean SyncContent = SyncContent();
                if (!SyncContent) {
                    for (int i = 0; i < 10; i++) {
                        Log.i("com.valvesoftware.source2launcher.BootStrapClientContentSyncAsyncTask", "====================Content Sync FAILED!====================");
                    }
                }
                z = SyncContent;
            } else {
                SelfInstall.OnStartup();
                updateProgress(PatchSystem.EState.Done, PatchSystem.EErrorCode.None, 100);
                z = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(z);
    }
}
