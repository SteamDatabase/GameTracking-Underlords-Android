package com.valvesoftware.source2launcher;

import com.valvesoftware.PatchSystem;

public class PatchSystemContentSyncAsyncTask extends IContentSyncAsyncTask {
    private PatchSystem m_PatchSystem = PatchSystem.GetInstance();

    public void Initialize(String str, int i) {
        this.m_PatchSystem.Start(str, i);
    }

    /* access modifiers changed from: protected */
    public Boolean doInBackground(Void... voidArr) {
        updateProgress(PatchSystem.EState.Unstarted, PatchSystem.EErrorCode.None, 0);
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException unused) {
            }
            PatchSystem.EState GetState = this.m_PatchSystem.GetState();
            PatchSystem.EErrorCode GetErrorCode = this.m_PatchSystem.GetErrorCode();
            if (GetState == PatchSystem.EState.Done) {
                updateProgress(GetState, GetErrorCode, 100);
                return true;
            } else if (GetState == PatchSystem.EState.Error) {
                updateProgress(GetState, GetErrorCode, 0);
            } else if (GetState == PatchSystem.EState.AssetsDownloading) {
                updateProgress(GetState, GetErrorCode, (int) (this.m_PatchSystem.GetDownloadProgress() * 100.0f));
            } else {
                updateProgress(GetState, GetErrorCode, 0);
            }
        }
    }
}
