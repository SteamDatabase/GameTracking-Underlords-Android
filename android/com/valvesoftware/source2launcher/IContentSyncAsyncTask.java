package com.valvesoftware.source2launcher;

import android.os.AsyncTask;
import com.valvesoftware.PatchSystem;

public abstract class IContentSyncAsyncTask extends AsyncTask<Void, TaskStatus, Boolean> {
    TaskStatus m_status = new TaskStatus();

    public class TaskStatus {
        public PatchSystem.EErrorCode m_nErrorCode;
        public int m_nProgress;
        public PatchSystem.EState m_nState;

        public TaskStatus() {
            this.m_nState = PatchSystem.EState.Unstarted;
            this.m_nErrorCode = PatchSystem.EErrorCode.None;
            this.m_nProgress = 0;
            this.m_nState = PatchSystem.EState.Unstarted;
            this.m_nErrorCode = PatchSystem.EErrorCode.None;
            this.m_nProgress = 0;
        }

        public TaskStatus(PatchSystem.EState eState, PatchSystem.EErrorCode eErrorCode, int i) {
            this.m_nState = PatchSystem.EState.Unstarted;
            this.m_nErrorCode = PatchSystem.EErrorCode.None;
            this.m_nProgress = 0;
            this.m_nState = eState;
            this.m_nErrorCode = eErrorCode;
            this.m_nProgress = i;
        }
    }

    /* access modifiers changed from: protected */
    public final void onPreExecute() {
        this.m_status = new TaskStatus();
    }

    public final boolean IsDone() {
        return this.m_status.m_nState == PatchSystem.EState.Done;
    }

    /* access modifiers changed from: protected */
    public final void onProgressUpdate(TaskStatus... taskStatusArr) {
        this.m_status = taskStatusArr[0];
    }

    public final TaskStatus GetStatus() {
        return this.m_status;
    }

    /* access modifiers changed from: protected */
    public void updateProgress(PatchSystem.EState eState, PatchSystem.EErrorCode eErrorCode, int i) {
        publishProgress(new TaskStatus[]{new TaskStatus(eState, eErrorCode, i)});
    }
}
