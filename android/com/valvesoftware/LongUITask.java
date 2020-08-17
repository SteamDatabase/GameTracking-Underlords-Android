package com.valvesoftware;

import android.os.AsyncTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class LongUITask {
    /* access modifiers changed from: private */
    public long m_BackgroundThreadID;
    private Semaphore m_BlockingUICallSemaphore = new Semaphore(0);
    /* access modifiers changed from: private */
    public Lock m_Lock = new ReentrantLock();
    /* access modifiers changed from: private */
    public Object m_ProgressObject;
    /* access modifiers changed from: private */
    public Object m_TaskResult;
    /* access modifiers changed from: private */
    public Internal_UIUpdatekRunnable m_UIUpdateRunnable;
    /* access modifiers changed from: private */
    public boolean m_bTaskFinished = false;

    public abstract Object BackgroundThread_Task();

    public int UIThread_Update(Object obj) {
        return -1;
    }

    public abstract void UIThread_onTaskFinished(Object obj);

    public static abstract class BlockingUICallRunnable {
        protected Semaphore m_BlockSem = new Semaphore(0);

        public abstract void run();

        /* access modifiers changed from: protected */
        public void RunUnderTask(LongUITask longUITask) {
            run();
            this.m_BlockSem.release(2);
        }

        /* access modifiers changed from: private */
        public void BlockUntilComplete() {
            Semaphore semaphore = this.m_BlockSem;
            if (semaphore != null) {
                semaphore.acquireUninterruptibly(2);
            }
        }
    }

    public static abstract class BlockingUICallRunnable_AsyncReturn<ResultType_t> extends BlockingUICallRunnable {
        public ResultType_t m_Result;

        public void AsyncReturn(ResultType_t resulttype_t) {
            Semaphore semaphore = this.m_BlockSem;
            this.m_BlockSem = null;
            if (semaphore != null) {
                this.m_Result = resulttype_t;
                semaphore.release();
            }
        }

        /* access modifiers changed from: protected */
        public void RunUnderTask(LongUITask longUITask) {
            Semaphore semaphore = this.m_BlockSem;
            run();
            semaphore.release(1);
        }
    }

    public void BlockingUICall(final BlockingUICallRunnable blockingUICallRunnable) {
        if (Thread.currentThread().getId() == this.m_BackgroundThreadID) {
            this.m_Lock.lock();
            JNI_Environment.m_OSHandler.post(new Runnable() {
                public void run() {
                    LongUITask.this.m_Lock.lock();
                    blockingUICallRunnable.RunUnderTask(LongUITask.this);
                    LongUITask.this.m_Lock.unlock();
                }
            });
            this.m_Lock.unlock();
            blockingUICallRunnable.BlockUntilComplete();
            return;
        }
        throw new AssertionError("com.valvesoftware.LongUITask.BlockingUICall() is intended to be called from the background task thread only");
    }

    public void SetProgressObject(Object obj) {
        this.m_Lock.lock();
        this.m_ProgressObject = obj;
        this.m_Lock.unlock();
    }

    public void QueueImmediateUIUpdate() {
        this.m_Lock.lock();
        JNI_Environment.m_OSHandler.removeCallbacks(this.m_UIUpdateRunnable);
        JNI_Environment.m_OSHandler.post(this.m_UIUpdateRunnable);
        this.m_Lock.unlock();
    }

    public void CancelPendingUIUpdates() {
        this.m_Lock.lock();
        if (this.m_UIUpdateRunnable != null && this.m_TaskResult == null) {
            JNI_Environment.m_OSHandler.removeCallbacks(this.m_UIUpdateRunnable);
        }
        this.m_Lock.unlock();
    }

    public void Start(boolean z) {
        this.m_Lock.lock();
        if (this.m_UIUpdateRunnable == null) {
            this.m_UIUpdateRunnable = new Internal_UIUpdatekRunnable();
            AsyncTask.execute(new Internal_AsyncTaskRunnable());
            if (z) {
                JNI_Environment.m_OSHandler.post(this.m_UIUpdateRunnable);
            }
        }
        this.m_Lock.unlock();
    }

    private class Internal_AsyncTaskRunnable implements Runnable {
        private Internal_AsyncTaskRunnable() {
        }

        public void run() {
            long unused = LongUITask.this.m_BackgroundThreadID = Thread.currentThread().getId();
            Object BackgroundThread_Task = LongUITask.this.BackgroundThread_Task();
            long unused2 = LongUITask.this.m_BackgroundThreadID = 0;
            LongUITask.this.m_Lock.lock();
            Object unused3 = LongUITask.this.m_TaskResult = BackgroundThread_Task;
            boolean unused4 = LongUITask.this.m_bTaskFinished = true;
            LongUITask.this.m_Lock.unlock();
            while (LongUITask.this.m_bTaskFinished) {
                JNI_Environment.m_OSHandler.post(LongUITask.this.m_UIUpdateRunnable);
                try {
                    Thread.sleep(10);
                } catch (Throwable unused5) {
                }
            }
        }
    }

    private class Internal_UIUpdatekRunnable implements Runnable {
        private Internal_UIUpdatekRunnable() {
        }

        public void run() {
            if (LongUITask.this.m_UIUpdateRunnable != null) {
                LongUITask.this.m_Lock.lock();
                if (!LongUITask.this.m_bTaskFinished) {
                    LongUITask longUITask = LongUITask.this;
                    int UIThread_Update = longUITask.UIThread_Update(longUITask.m_ProgressObject);
                    if (UIThread_Update >= 0) {
                        JNI_Environment.m_OSHandler.postDelayed(this, (long) UIThread_Update);
                    }
                } else {
                    Object unused = LongUITask.this.m_ProgressObject = null;
                    Internal_UIUpdatekRunnable unused2 = LongUITask.this.m_UIUpdateRunnable = null;
                    Object access$500 = LongUITask.this.m_TaskResult;
                    Object unused3 = LongUITask.this.m_TaskResult = null;
                    boolean unused4 = LongUITask.this.m_bTaskFinished = false;
                    LongUITask.this.UIThread_onTaskFinished(access$500);
                }
                LongUITask.this.m_Lock.unlock();
            }
        }
    }
}
