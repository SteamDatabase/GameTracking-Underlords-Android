package com.valvesoftware.source2launcher;

import android.util.Log;
import com.valvesoftware.BootStrapClient;
import com.valvesoftware.BootStrapClient.RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter;
import com.valvesoftware.IStreamingBootStrap;
import com.valvesoftware.JNI_Environment;
import com.valvesoftware.PatchSystem.EErrorCode;
import com.valvesoftware.PatchSystem.EState;
import com.valvesoftware.Resources;
import com.valvesoftware.SelfInstall;

public class BootStrapClientContentSyncAsyncTask extends IContentSyncAsyncTask {
    private IStreamingBootStrap m_streamingBootStrapConnection;

    public BootStrapClientContentSyncAsyncTask(IStreamingBootStrap iStreamingBootStrap) {
        this.m_streamingBootStrapConnection = iStreamingBootStrap;
    }

    private boolean SyncContent() {
        String GetString = Resources.GetString("VPC_GameName");
        boolean z = JNI_Environment.GetAvailableStorageBytes(JNI_Environment.GetPublicPath()) < 0;
        String absolutePath = JNI_Environment.GetPublicPath().getAbsolutePath();
        String[] strArr = {"so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter = new RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, new String[]{"win32", "win64", "linuxsteamrt64", "osx32", "osx64"});
        IStreamingBootStrap iStreamingBootStrap = this.m_streamingBootStrapConnection;
        StringBuilder sb = new StringBuilder();
        sb.append(absolutePath);
        sb.append("/game/bin");
        boolean z2 = BootStrapClient.DownloadDirectory(iStreamingBootStrap, "game:/bin", sb.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter);
        if (z) {
            strArr = new String[]{"vsnd_c", "so", "dbg", "exe", "dll", "pdb", "dylib", "dmp", "mdmp", "bat", "cmd", "so.0", "so.1", "so.2", "so.3", "so.4", "so.5", "so.6"};
        }
        RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2 = new RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(strArr, null);
        IStreamingBootStrap iStreamingBootStrap2 = this.m_streamingBootStrapConnection;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(absolutePath);
        sb2.append("/game/core");
        boolean z3 = BootStrapClient.DownloadDirectory(iStreamingBootStrap2, "game:/core", sb2.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z2;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 10);
        IStreamingBootStrap iStreamingBootStrap3 = this.m_streamingBootStrapConnection;
        StringBuilder sb3 = new StringBuilder();
        sb3.append(absolutePath);
        sb3.append("/game/mobile/core");
        boolean z4 = BootStrapClient.DownloadDirectory(iStreamingBootStrap3, "game:/mobile/core", sb3.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z3;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 20);
        IStreamingBootStrap iStreamingBootStrap4 = this.m_streamingBootStrapConnection;
        StringBuilder sb4 = new StringBuilder();
        sb4.append(absolutePath);
        sb4.append("/game/mobile/commandlines");
        boolean z5 = BootStrapClient.DownloadDirectory(iStreamingBootStrap4, "game:/mobile/commandlines", sb4.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z4;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 30);
        IStreamingBootStrap iStreamingBootStrap5 = this.m_streamingBootStrapConnection;
        StringBuilder sb5 = new StringBuilder();
        String str = "game:/";
        sb5.append(str);
        sb5.append(GetString);
        String sb6 = sb5.toString();
        StringBuilder sb7 = new StringBuilder();
        sb7.append(absolutePath);
        String str2 = "/game/";
        sb7.append(str2);
        sb7.append(GetString);
        String sb8 = sb7.toString();
        String str3 = str2;
        String str4 = str;
        boolean z6 = BootStrapClient.DownloadDirectory(iStreamingBootStrap5, sb6, sb8, true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z5;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 40);
        IStreamingBootStrap iStreamingBootStrap6 = this.m_streamingBootStrapConnection;
        StringBuilder sb9 = new StringBuilder();
        sb9.append(str4);
        sb9.append(GetString);
        String str5 = "_addons";
        sb9.append(str5);
        String sb10 = sb9.toString();
        StringBuilder sb11 = new StringBuilder();
        sb11.append(absolutePath);
        sb11.append(str3);
        sb11.append(GetString);
        sb11.append(str5);
        boolean z7 = BootStrapClient.DownloadDirectory(iStreamingBootStrap6, sb10, sb11.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z6;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 50);
        IStreamingBootStrap iStreamingBootStrap7 = this.m_streamingBootStrapConnection;
        StringBuilder sb12 = new StringBuilder();
        String str6 = "game:/mobile/";
        sb12.append(str6);
        sb12.append(GetString);
        String sb13 = sb12.toString();
        StringBuilder sb14 = new StringBuilder();
        sb14.append(absolutePath);
        String str7 = "/game/mobile/";
        sb14.append(str7);
        sb14.append(GetString);
        String sb15 = sb14.toString();
        String str8 = str7;
        String str9 = str6;
        boolean z8 = BootStrapClient.DownloadDirectory(iStreamingBootStrap7, sb13, sb15, true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z7;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 60);
        IStreamingBootStrap iStreamingBootStrap8 = this.m_streamingBootStrapConnection;
        StringBuilder sb16 = new StringBuilder();
        sb16.append(str9);
        sb16.append(GetString);
        sb16.append(str5);
        String sb17 = sb16.toString();
        StringBuilder sb18 = new StringBuilder();
        sb18.append(absolutePath);
        sb18.append(str8);
        sb18.append(GetString);
        sb18.append(str5);
        boolean z9 = BootStrapClient.DownloadDirectory(iStreamingBootStrap8, sb17, sb18.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z8;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 70);
        if (z) {
            return z9;
        }
        IStreamingBootStrap iStreamingBootStrap9 = this.m_streamingBootStrapConnection;
        StringBuilder sb19 = new StringBuilder();
        sb19.append(absolutePath);
        sb19.append("/game_otherplatforms/etc/core");
        boolean z10 = BootStrapClient.DownloadDirectory(iStreamingBootStrap9, "game:../game_otherplatforms/etc/core", sb19.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z9;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 80);
        IStreamingBootStrap iStreamingBootStrap10 = this.m_streamingBootStrapConnection;
        StringBuilder sb20 = new StringBuilder();
        sb20.append("game:../game_otherplatforms/etc/");
        sb20.append(GetString);
        String sb21 = sb20.toString();
        StringBuilder sb22 = new StringBuilder();
        sb22.append(absolutePath);
        sb22.append("/game_otherplatforms/etc/");
        sb22.append(GetString);
        boolean z11 = BootStrapClient.DownloadDirectory(iStreamingBootStrap10, sb21, sb22.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z10;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 90);
        IStreamingBootStrap iStreamingBootStrap11 = this.m_streamingBootStrapConnection;
        StringBuilder sb23 = new StringBuilder();
        sb23.append("game:../game_otherplatforms/etc/mobile/");
        sb23.append(GetString);
        String sb24 = sb23.toString();
        StringBuilder sb25 = new StringBuilder();
        sb25.append(absolutePath);
        sb25.append("/game_otherplatforms/etc/mobile/");
        sb25.append(GetString);
        boolean z12 = BootStrapClient.DownloadDirectory(iStreamingBootStrap11, sb24, sb25.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z11;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 100);
        IStreamingBootStrap iStreamingBootStrap12 = this.m_streamingBootStrapConnection;
        StringBuilder sb26 = new StringBuilder();
        sb26.append(absolutePath);
        sb26.append("/game_otherplatforms/low_bitrate/core");
        boolean z13 = BootStrapClient.DownloadDirectory(iStreamingBootStrap12, "game:../game_otherplatforms/low_bitrate/core", sb26.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z12;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 80);
        IStreamingBootStrap iStreamingBootStrap13 = this.m_streamingBootStrapConnection;
        StringBuilder sb27 = new StringBuilder();
        sb27.append("game:../game_otherplatforms/low_bitrate/");
        sb27.append(GetString);
        String sb28 = sb27.toString();
        StringBuilder sb29 = new StringBuilder();
        sb29.append(absolutePath);
        sb29.append("/game_otherplatforms/low_bitrate/");
        sb29.append(GetString);
        boolean z14 = BootStrapClient.DownloadDirectory(iStreamingBootStrap13, sb28, sb29.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z13;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 90);
        IStreamingBootStrap iStreamingBootStrap14 = this.m_streamingBootStrapConnection;
        StringBuilder sb30 = new StringBuilder();
        sb30.append("game:../game_otherplatforms/low_bitrate/mobile/");
        sb30.append(GetString);
        String sb31 = sb30.toString();
        StringBuilder sb32 = new StringBuilder();
        sb32.append(absolutePath);
        sb32.append("/game_otherplatforms/low_bitrate/mobile/");
        sb32.append(GetString);
        boolean z15 = BootStrapClient.DownloadDirectory(iStreamingBootStrap14, sb31, sb32.toString(), true, 60000, recursiveDownloadExtensionExlusionAndRelativeDirectoryFilter2) && z14;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 100);
        return z15;
    }

    /* access modifiers changed from: protected */
    public final void onPostExecute(Boolean bool) {
        this.m_status.m_nState = EState.Done;
        this.m_status.m_nErrorCode = EErrorCode.None;
        this.m_status.m_nProgress = 100;
    }

    /* access modifiers changed from: protected */
    public Boolean doInBackground(Void... voidArr) {
        boolean z = false;
        updateProgress(EState.AssetsDownloading, EErrorCode.None, 0);
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
                updateProgress(EState.Done, EErrorCode.None, 100);
                z = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(z);
    }
}
