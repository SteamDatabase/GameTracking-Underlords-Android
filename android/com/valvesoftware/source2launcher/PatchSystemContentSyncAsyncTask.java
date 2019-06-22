package com.valvesoftware.source2launcher;

import com.valvesoftware.PatchSystem;
import com.valvesoftware.PatchSystem.EErrorCode;
import com.valvesoftware.PatchSystem.EState;

public class PatchSystemContentSyncAsyncTask extends IContentSyncAsyncTask {
    private PatchSystem m_PatchSystem = PatchSystem.GetInstance();

    public void Initialize(String str, int i) {
        this.m_PatchSystem.Start(str, i);
    }

    /* access modifiers changed from: protected */
    public Boolean doInBackground(Void... voidArr) {
        updateProgress(EState.Unstarted, EErrorCode.None, 0);
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException unused) {
            }
            EState GetState = this.m_PatchSystem.GetState();
            EErrorCode GetErrorCode = this.m_PatchSystem.GetErrorCode();
            if (GetState == EState.Done) {
                updateProgress(GetState, GetErrorCode, 100);
                return Boolean.valueOf(true);
            } else if (GetState == EState.Error) {
                updateProgress(GetState, GetErrorCode, 0);
            } else if (GetState == EState.AssetsDownloading) {
                updateProgress(GetState, GetErrorCode, (int) (this.m_PatchSystem.GetDownloadProgress() * 100.0f));
            } else {
                updateProgress(GetState, GetErrorCode, 0);
            }
        }
    }
}
