package com.valvesoftware;

import android.os.Looper;
import android.util.Log;
import com.valvesoftware.IStreamingBootStrap.FileSystemDirectoryListEntry_t;
import com.valvesoftware.IStreamingBootStrap.FileSystemQueryResult_t;
import com.valvesoftware.IStreamingBootStrap.IAccessContextCallback;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_FileSystem;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_FileSystem.IDirectoryListCallback;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Ping;
import com.valvesoftware.IStreamingBootStrap.IStreamHandler;
import com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIOImpl;
import com.valvesoftware.IStreamingBootStrap.StaticHelpers;
import com.valvesoftware.JNI_Environment.INativeLibraryPathResolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONObject;

public class BootStrapClient {
    private static final int SYNC_STATE_VERSION = 1;
    public static byte[] defaultDevPCAddress = {10, 0, 2, 2};
    public static byte[] defaultDevPCAddress2 = {Byte.MAX_VALUE, 0, 0, 1};
    private static JSONObject m_SyncState;
    public static int nDefaultDevPCPort = 53277;
    private static long s_nLastSyncSaveTime = 0;

    public static class ConnectionResult {
        public IStreamingBootStrap connection;
        public int nRemoteVersionNumber;
        public String sFailureMessageIfFailed;
        public String sRemoteImplementationName;
    }

    public static class DefaultFileSystemHandler extends IRequestHandler_FileSystem {
        public String PerformPathSubstitutions(String str) {
            return str;
        }

        public int RetrieveFile(IStreamingBootStrap iStreamingBootStrap, String str, long[] jArr) {
            return 1;
        }

        public int StoreFile(IStreamingBootStrap iStreamingBootStrap, String str, int i, IStreamHandler iStreamHandler) {
            return 1;
        }

        public static boolean QueryFileHelper(File file, int i, FileSystemQueryResult_t fileSystemQueryResult_t) {
            boolean z = file.isFile() || file.isHidden();
            boolean isDirectory = file.isDirectory();
            if (!z && !isDirectory) {
                return false;
            }
            if ((i & 1) != 0) {
                fileSystemQueryResult_t.nSetFields |= 1;
                fileSystemQueryResult_t.nAttributeFlags = 0;
                if (isDirectory) {
                    fileSystemQueryResult_t.nAttributeFlags |= 1;
                }
                if (file.canRead()) {
                    fileSystemQueryResult_t.nAttributeFlags |= 2;
                }
                if (file.canWrite()) {
                    fileSystemQueryResult_t.nAttributeFlags |= 4;
                }
                if (file.canExecute()) {
                    fileSystemQueryResult_t.nAttributeFlags |= 8;
                }
            }
            if (!isDirectory) {
                if ((i & 2) != 0) {
                    fileSystemQueryResult_t.nSetFields |= 2;
                    fileSystemQueryResult_t.nFileSize = file.length();
                }
                if ((i & 4) != 0) {
                    fileSystemQueryResult_t.nSetFields |= 4;
                    fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch = file.lastModified() / 1000;
                }
            }
            return true;
        }

        public int QueryFile(IStreamingBootStrap iStreamingBootStrap, String str, int i, FileSystemQueryResult_t fileSystemQueryResult_t) {
            return QueryFileHelper(new File(PerformPathSubstitutions(str)), i, fileSystemQueryResult_t) ? 0 : 3;
        }

        public int ListDirectory(IStreamingBootStrap iStreamingBootStrap, String str, int i, IDirectoryListCallback iDirectoryListCallback) {
            File file = new File(PerformPathSubstitutions(str));
            if (!file.isDirectory()) {
                return 3;
            }
            File[] listFiles = file.listFiles();
            FileSystemDirectoryListEntry_t fileSystemDirectoryListEntry_t = new FileSystemDirectoryListEntry_t();
            for (File file2 : listFiles) {
                fileSystemDirectoryListEntry_t.sName = file2.getName();
                fileSystemDirectoryListEntry_t.nSetFields = 0;
                if (QueryFileHelper(file2, i, fileSystemDirectoryListEntry_t)) {
                    iDirectoryListCallback.AddResult(fileSystemDirectoryListEntry_t);
                }
            }
            return 0;
        }

        public int DeleteFileOrDirectory(IStreamingBootStrap iStreamingBootStrap, String str) {
            File file = new File(PerformPathSubstitutions(str));
            if (file.delete()) {
                return 0;
            }
            return file.exists() ? 2 : 3;
        }
    }

    public static class FileDownloadEntry_t {
        public int nSetAttributes;
        public String sLocalDestPath;
        public String sRemotePath;

        public FileDownloadEntry_t() {
        }

        public FileDownloadEntry_t(String str, String str2, int i) {
            this.sRemotePath = str;
            this.sLocalDestPath = str2;
            this.nSetAttributes = i;
        }
    }

    public static class NativeLibraryPathResolver implements INativeLibraryPathResolver {
        private Hashtable<String, String> m_AlreadyTried;
        private IStreamingBootStrap m_Connection;
        private Lock m_DownloadMutex = new ReentrantLock();
        private File m_SyncPath;
        private VariableReplacementPair[] m_VariableReplacements;
        private CharSequence m_sPlatformArch_Replace;
        private CharSequence m_sPlatformArch_Search;

        private static class VariableReplacementPair {
            CharSequence replace;
            CharSequence search;

            VariableReplacementPair(CharSequence charSequence, CharSequence charSequence2) {
                this.search = charSequence;
                this.replace = charSequence2;
            }
        }

        public NativeLibraryPathResolver(IStreamingBootStrap iStreamingBootStrap, File file) {
            this.m_Connection = iStreamingBootStrap;
            this.m_SyncPath = file;
            this.m_VariableReplacements = null;
            this.m_AlreadyTried = new Hashtable<>();
        }

        public void AddVariableReplacement(String str, String str2) {
            int i = 0;
            CharSequence subSequence = str.subSequence(0, str.length());
            CharSequence subSequence2 = str2.subSequence(0, str2.length());
            VariableReplacementPair[] variableReplacementPairArr = this.m_VariableReplacements;
            int length = variableReplacementPairArr != null ? variableReplacementPairArr.length : 0;
            VariableReplacementPair[] variableReplacementPairArr2 = new VariableReplacementPair[(length + 1)];
            while (i < length) {
                if (this.m_VariableReplacements[i].search.equals(subSequence)) {
                    this.m_VariableReplacements[i].replace = subSequence2;
                    return;
                } else {
                    variableReplacementPairArr2[i] = this.m_VariableReplacements[i];
                    i++;
                }
            }
            variableReplacementPairArr2[length] = new VariableReplacementPair(subSequence, subSequence2);
            this.m_VariableReplacements = variableReplacementPairArr2;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:30:0x00e6, code lost:
            if (com.valvesoftware.BootStrapClient.DownloadFiles(r10.m_Connection, new com.valvesoftware.BootStrapClient.FileDownloadEntry_t[]{new com.valvesoftware.BootStrapClient.FileDownloadEntry_t(r4, r5, 14)}, true, 5000)[0] != false) goto L_0x00f4;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x00f2, code lost:
            if (new java.io.File(r5).exists() != false) goto L_0x00f4;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public java.lang.String ResolveNativeLibraryPath(java.lang.String r11) {
            /*
                r10 = this;
                java.util.concurrent.locks.Lock r0 = r10.m_DownloadMutex
                r0.lock()
                java.util.Hashtable<java.lang.String, java.lang.String> r0 = r10.m_AlreadyTried
                java.lang.Object r0 = r0.get(r11)
                java.lang.String r0 = (java.lang.String) r0
                if (r0 == 0) goto L_0x0015
                java.util.concurrent.locks.Lock r11 = r10.m_DownloadMutex
                r11.unlock()
                return r0
            L_0x0015:
                com.valvesoftware.BootStrapClient$NativeLibraryPathResolver$VariableReplacementPair[] r1 = r10.m_VariableReplacements
                r2 = 1
                r3 = 0
                if (r1 == 0) goto L_0x0039
                r4 = r11
                r1 = 0
            L_0x001d:
                com.valvesoftware.BootStrapClient$NativeLibraryPathResolver$VariableReplacementPair[] r5 = r10.m_VariableReplacements
                int r6 = r5.length
                if (r1 >= r6) goto L_0x0033
                r5 = r5[r1]
                java.lang.CharSequence r5 = r5.search
                com.valvesoftware.BootStrapClient$NativeLibraryPathResolver$VariableReplacementPair[] r6 = r10.m_VariableReplacements
                r6 = r6[r1]
                java.lang.CharSequence r6 = r6.replace
                java.lang.String r4 = r4.replace(r5, r6)
                int r1 = r1 + 1
                goto L_0x001d
            L_0x0033:
                boolean r1 = r4.equals(r11)
                r1 = r1 ^ r2
                goto L_0x003b
            L_0x0039:
                r4 = r11
                r1 = 0
            L_0x003b:
                if (r1 == 0) goto L_0x0052
                java.util.Hashtable<java.lang.String, java.lang.String> r0 = r10.m_AlreadyTried
                java.lang.Object r0 = r0.get(r11)
                java.lang.String r0 = (java.lang.String) r0
                if (r0 == 0) goto L_0x0052
                java.util.Hashtable<java.lang.String, java.lang.String> r1 = r10.m_AlreadyTried
                r1.put(r11, r0)
                java.util.concurrent.locks.Lock r11 = r10.m_DownloadMutex
                r11.unlock()
                return r0
            L_0x0052:
                r5 = 0
                java.lang.String r6 = "game:/"
                boolean r6 = r4.startsWith(r6)
                if (r6 == 0) goto L_0x007b
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.io.File r6 = r10.m_SyncPath
                java.lang.String r6 = r6.getAbsolutePath()
                r5.append(r6)
                java.lang.String r6 = "/game/"
                r5.append(r6)
                r6 = 6
                java.lang.String r6 = r4.substring(r6)
                r5.append(r6)
                java.lang.String r5 = r5.toString()
                goto L_0x00cb
            L_0x007b:
                java.lang.String r6 = "src:/"
                boolean r6 = r4.startsWith(r6)
                if (r6 == 0) goto L_0x00a3
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.io.File r6 = r10.m_SyncPath
                java.lang.String r6 = r6.getAbsolutePath()
                r5.append(r6)
                java.lang.String r6 = "/src/"
                r5.append(r6)
                r6 = 5
                java.lang.String r6 = r4.substring(r6)
                r5.append(r6)
                java.lang.String r5 = r5.toString()
                goto L_0x00cb
            L_0x00a3:
                java.lang.String r6 = "android-ndk:/"
                boolean r6 = r4.startsWith(r6)
                if (r6 == 0) goto L_0x00cb
                java.lang.StringBuilder r5 = new java.lang.StringBuilder
                r5.<init>()
                java.io.File r6 = r10.m_SyncPath
                java.lang.String r6 = r6.getAbsolutePath()
                r5.append(r6)
                java.lang.String r6 = "/android-ndk/"
                r5.append(r6)
                r6 = 13
                java.lang.String r6 = r4.substring(r6)
                r5.append(r6)
                java.lang.String r5 = r5.toString()
            L_0x00cb:
                if (r5 == 0) goto L_0x00f5
                com.valvesoftware.IStreamingBootStrap r6 = r10.m_Connection
                if (r6 == 0) goto L_0x00e9
                com.valvesoftware.BootStrapClient$FileDownloadEntry_t[] r6 = new com.valvesoftware.BootStrapClient.FileDownloadEntry_t[r2]
                com.valvesoftware.BootStrapClient$FileDownloadEntry_t r7 = new com.valvesoftware.BootStrapClient$FileDownloadEntry_t
                r8 = 14
                r7.<init>(r4, r5, r8)
                r6[r3] = r7
                com.valvesoftware.IStreamingBootStrap r7 = r10.m_Connection
                r8 = 5000(0x1388, double:2.4703E-320)
                boolean[] r2 = com.valvesoftware.BootStrapClient.DownloadFiles(r7, r6, r2, r8)
                boolean r2 = r2[r3]
                if (r2 == 0) goto L_0x00f5
                goto L_0x00f4
            L_0x00e9:
                java.io.File r2 = new java.io.File
                r2.<init>(r5)
                boolean r2 = r2.exists()
                if (r2 == 0) goto L_0x00f5
            L_0x00f4:
                r0 = r5
            L_0x00f5:
                if (r0 == 0) goto L_0x0103
                java.util.Hashtable<java.lang.String, java.lang.String> r2 = r10.m_AlreadyTried
                r2.put(r4, r0)
                if (r1 == 0) goto L_0x0103
                java.util.Hashtable<java.lang.String, java.lang.String> r1 = r10.m_AlreadyTried
                r1.put(r11, r0)
            L_0x0103:
                java.util.concurrent.locks.Lock r11 = r10.m_DownloadMutex
                r11.unlock()
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.BootStrapClient.NativeLibraryPathResolver.ResolveNativeLibraryPath(java.lang.String):java.lang.String");
        }
    }

    public static abstract class NotifyProgressMade {
        public void OnPartialProgressMade(Object obj) {
        }

        public abstract void OnProgressResolved(Object obj);
    }

    public static class RecursiveDownload extends IResponseHandler_FileSystem {
        public static final int STATUS_ABORTED = 8;
        public static final int STATUS_PENDING_DOWNLOADS = 7;
        public static final int STATUS_RECEIVED_LIST = 6;
        public static final int STATUS_REQUESTED_LIST = 5;
        public static final int STATUS_SUCCEEDED = 9;
        public static final int STATUS_UNISSUED = 4;
        private IStreamingBootStrap m_Connection;
        /* access modifiers changed from: private */
        public StreamingFileDownload[] m_Downloads;
        private IRecursiveDownloadFilter m_Filter;
        /* access modifiers changed from: private */
        public NotifyProgressMade m_NotifyOnProgress;
        /* access modifiers changed from: private */
        public RecursiveDownload[] m_SubDirectories;
        private JSONObject m_SyncEntry;
        private RemoteTimeDelta_t m_TimeDelta;
        /* access modifiers changed from: private */
        public boolean m_bAnyErrors;
        /* access modifiers changed from: private */
        public boolean m_bSaveSyncState;
        /* access modifiers changed from: private */
        public long m_nLastMadeProgress;
        /* access modifiers changed from: private */
        public long m_nRemoteLastModifiedTime;
        /* access modifiers changed from: private */
        public int m_nRequestStatus = 4;
        private JSONObject m_prevSyncRoot;
        private String m_sLocalPath;
        private String m_sRecursiveRelativePath;
        public String m_sRemotePath;

        public interface IRecursiveDownloadFilter {
            boolean ShouldDownloadFile(String str);

            boolean ShouldRecurseIntoDirectory(String str);
        }

        private class NotifyReceiver extends NotifyProgressMade {
            private NotifyReceiver() {
            }

            public void OnPartialProgressMade(Object obj) {
                boolean z;
                boolean z2;
                RecursiveDownload.this.m_nLastMadeProgress = System.currentTimeMillis();
                if (RecursiveDownload.this.m_NotifyOnProgress != null) {
                    RecursiveDownload.this.m_NotifyOnProgress.OnPartialProgressMade(RecursiveDownload.this);
                }
                if (RecursiveDownload.this.m_nRequestStatus == 7) {
                    if (RecursiveDownload.this.m_SubDirectories != null) {
                        int i = 0;
                        boolean z3 = false;
                        while (true) {
                            if (i >= RecursiveDownload.this.m_SubDirectories.length) {
                                break;
                            }
                            if (RecursiveDownload.this.m_SubDirectories[i] != null) {
                                int access$1400 = RecursiveDownload.this.m_SubDirectories[i].m_nRequestStatus;
                                if (RecursiveDownload.this.m_SubDirectories[i] == obj) {
                                    if (access$1400 != 6) {
                                        break;
                                    }
                                    z3 = true;
                                }
                                if (!(access$1400 == 6 || access$1400 == 9)) {
                                    z2 = false;
                                    break;
                                }
                            }
                            i++;
                        }
                        z2 = true;
                        if (z3 && z2) {
                            z = true;
                            if (z && RecursiveDownload.this.StartNextItem(false, false) != 1 && RecursiveDownload.this.m_NotifyOnProgress != null) {
                                RecursiveDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                                return;
                            }
                            return;
                        }
                    }
                    z = false;
                    if (z) {
                    }
                }
            }

            public void OnProgressResolved(Object obj) {
                boolean z;
                RecursiveDownload.this.m_nLastMadeProgress = System.currentTimeMillis();
                if (RecursiveDownload.this.m_nRequestStatus != 8) {
                    if (RecursiveDownload.this.m_Downloads != null) {
                        int i = 0;
                        while (true) {
                            if (i >= RecursiveDownload.this.m_Downloads.length) {
                                break;
                            } else if (RecursiveDownload.this.m_Downloads[i] == obj) {
                                if (!RecursiveDownload.this.m_Downloads[i].HasSucceeded()) {
                                    RecursiveDownload.this.InternalFailure(8);
                                    RecursiveDownload.this.m_bAnyErrors = true;
                                } else if (RecursiveDownload.this.m_Downloads[i].m_nRemoteLastModifiedTime > RecursiveDownload.this.m_nRemoteLastModifiedTime) {
                                    RecursiveDownload recursiveDownload = RecursiveDownload.this;
                                    recursiveDownload.m_nRemoteLastModifiedTime = recursiveDownload.m_Downloads[i].m_nRemoteLastModifiedTime;
                                    RecursiveDownload.this.m_bSaveSyncState = true;
                                }
                                RecursiveDownload.this.m_Downloads[i] = null;
                                z = true;
                            } else {
                                i++;
                            }
                        }
                    }
                    z = false;
                    if (!z && RecursiveDownload.this.m_SubDirectories != null) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= RecursiveDownload.this.m_SubDirectories.length) {
                                break;
                            } else if (RecursiveDownload.this.m_SubDirectories[i2] == obj) {
                                if (!RecursiveDownload.this.m_SubDirectories[i2].HasSucceeded()) {
                                    RecursiveDownload.this.InternalFailure(8);
                                } else if (RecursiveDownload.this.m_SubDirectories[i2].m_nRemoteLastModifiedTime > RecursiveDownload.this.m_nRemoteLastModifiedTime) {
                                    RecursiveDownload recursiveDownload2 = RecursiveDownload.this;
                                    recursiveDownload2.m_nRemoteLastModifiedTime = recursiveDownload2.m_SubDirectories[i2].m_nRemoteLastModifiedTime;
                                    RecursiveDownload.this.m_bSaveSyncState = true;
                                }
                                RecursiveDownload.this.m_SubDirectories[i2] = null;
                                z = true;
                            } else {
                                i2++;
                            }
                        }
                    }
                    if (!z) {
                        String str = "\"";
                        String str2 = "Unmatched resolution for dir \"";
                        String str3 = "com.valvesoftware.BootStrapClient.Recursive";
                        if (obj.getClass().getCanonicalName().compareTo("com.valvesoftware.BootStrapClient.RecursiveDownload") == 0) {
                            RecursiveDownload recursiveDownload3 = (RecursiveDownload) obj;
                            StringBuilder sb = new StringBuilder();
                            sb.append(str2);
                            sb.append(RecursiveDownload.this.m_sRemotePath);
                            sb.append("\" RecursiveDownload of \"");
                            sb.append(recursiveDownload3.m_sRemotePath);
                            sb.append(str);
                            Log.i(str3, sb.toString());
                        } else if (obj.getClass().getCanonicalName().compareTo("com.valvesoftware.BootStrapClient.StreamingFileDownload") == 0) {
                            StreamingFileDownload streamingFileDownload = (StreamingFileDownload) obj;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(str2);
                            sb2.append(RecursiveDownload.this.m_sRemotePath);
                            sb2.append("\" StreamingFileDownload of \"");
                            sb2.append(streamingFileDownload.m_sRemotePath);
                            sb2.append(str);
                            Log.i(str3, sb2.toString());
                        } else {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append(str2);
                            sb3.append(RecursiveDownload.this.m_sRemotePath);
                            sb3.append("\" unknown object \"");
                            sb3.append(obj.getClass().getCanonicalName());
                            sb3.append(str);
                            Log.i(str3, sb3.toString());
                        }
                        try {
                            throw new Throwable();
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                    if (RecursiveDownload.this.StartNextItem(false, false) != 1) {
                        if (RecursiveDownload.this.m_bAnyErrors) {
                            RecursiveDownload.this.InternalFailure(8);
                        } else {
                            RecursiveDownload.this.InternalSuccess();
                        }
                        if (RecursiveDownload.this.m_NotifyOnProgress != null) {
                            RecursiveDownload.this.m_NotifyOnProgress.OnProgressResolved(RecursiveDownload.this);
                        }
                    }
                }
            }
        }

        public RecursiveDownload(IStreamingBootStrap iStreamingBootStrap, JSONObject jSONObject, String str, String str2, String str3, RemoteTimeDelta_t remoteTimeDelta_t, NotifyProgressMade notifyProgressMade, IRecursiveDownloadFilter iRecursiveDownloadFilter, FileSystemQueryResult_t fileSystemQueryResult_t) {
            this.m_Connection = iStreamingBootStrap;
            this.m_sRemotePath = str;
            this.m_sLocalPath = str2;
            this.m_sRecursiveRelativePath = str3;
            this.m_NotifyOnProgress = notifyProgressMade;
            this.m_TimeDelta = remoteTimeDelta_t;
            this.m_Filter = iRecursiveDownloadFilter;
            this.m_nLastMadeProgress = System.currentTimeMillis();
            this.m_nRemoteLastModifiedTime = -1;
            this.m_prevSyncRoot = jSONObject;
            this.m_SyncEntry = null;
            this.m_bSaveSyncState = false;
            this.m_bAnyErrors = false;
            this.m_SyncEntry = jSONObject.optJSONObject(str);
            if (this.m_SyncEntry != null && !new File(str2).exists()) {
                jSONObject.remove(str);
                this.m_SyncEntry = null;
            }
            String str4 = "Directory \"";
            String str5 = "com.valvesoftware.BootStrapClient.RecursiveDownload";
            if (this.m_SyncEntry == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(str4);
                sb.append(str);
                sb.append("\" did not have a sync entry");
                Log.i(str5, sb.toString());
                this.m_SyncEntry = new JSONObject();
                try {
                    jSONObject.put(str, (Object) this.m_SyncEntry);
                } catch (Throwable unused) {
                }
            }
            this.m_nRemoteLastModifiedTime = this.m_SyncEntry.optLong("modified", -1);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str4);
            sb2.append(str);
            sb2.append("\" newest previously synced file age: ");
            sb2.append(this.m_nRemoteLastModifiedTime);
            Log.i(str5, sb2.toString());
            if (fileSystemQueryResult_t == null) {
                return;
            }
            if ((4 & fileSystemQueryResult_t.nSetFields) != 0) {
                String str6 = " )";
                if (fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch <= this.m_nRemoteLastModifiedTime) {
                    InternalSuccess();
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(str4);
                    sb3.append(str);
                    sb3.append("\" is already up to date ( ");
                    sb3.append(this.m_nRemoteLastModifiedTime);
                    sb3.append(" >= ");
                    sb3.append(fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch);
                    sb3.append(str6);
                    Log.i(str5, sb3.toString());
                    return;
                }
                StringBuilder sb4 = new StringBuilder();
                sb4.append(str4);
                sb4.append(str);
                sb4.append("\" is out of date ( ");
                sb4.append(this.m_nRemoteLastModifiedTime);
                sb4.append(" < ");
                sb4.append(fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch);
                sb4.append(str6);
                Log.i(str5, sb4.toString());
                return;
            }
            StringBuilder sb5 = new StringBuilder();
            sb5.append(str4);
            sb5.append(str);
            sb5.append("\" did not send a timestamp");
            Log.i(str5, sb5.toString());
        }

        public void PrintStatus(String str) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("Dir ");
            sb.append(this.m_nRequestStatus);
            sb.append(" \"");
            sb.append(this.m_sRemotePath);
            sb.append("\" -> \"");
            sb.append(this.m_sLocalPath);
            sb.append("\"");
            Log.i("com.valvesoftware.BootStrapClient", sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);
            sb2.append("\t");
            String sb3 = sb2.toString();
            int i = 0;
            if (this.m_Downloads != null) {
                int i2 = 0;
                while (true) {
                    StreamingFileDownload[] streamingFileDownloadArr = this.m_Downloads;
                    if (i2 >= streamingFileDownloadArr.length) {
                        break;
                    }
                    if (streamingFileDownloadArr[i2] != null) {
                        streamingFileDownloadArr[i2].PrintStatus(sb3);
                    }
                    i2++;
                }
            }
            if (this.m_SubDirectories != null) {
                while (true) {
                    RecursiveDownload[] recursiveDownloadArr = this.m_SubDirectories;
                    if (i < recursiveDownloadArr.length) {
                        if (recursiveDownloadArr[i] != null) {
                            recursiveDownloadArr[i].PrintStatus(sb3);
                        }
                        i++;
                    } else {
                        return;
                    }
                }
            }
        }

        public void OnParentAbort() {
            if (!HasAborted() && !HasSucceeded()) {
                JSONObject jSONObject = this.m_SyncEntry;
                if (jSONObject != null) {
                    jSONObject.remove("modified");
                }
                if (this.m_SubDirectories != null) {
                    int i = 0;
                    while (true) {
                        RecursiveDownload[] recursiveDownloadArr = this.m_SubDirectories;
                        if (i < recursiveDownloadArr.length) {
                            if (recursiveDownloadArr[i] != null) {
                                recursiveDownloadArr[i].OnParentAbort();
                            }
                            i++;
                        } else {
                            return;
                        }
                    }
                }
            }
        }

        public void RequestListOnly() {
            if (this.m_nRequestStatus == 4) {
                this.m_nRequestStatus = 5;
                this.m_Connection.ListDirectory(this.m_sRemotePath, 7, this);
            }
        }

        public int Start() {
            int i = this.m_nRequestStatus;
            if (!(i == 5 || i == 7)) {
                if (i != 4 && i != 6) {
                    return i == 9 ? 2 : 0;
                }
                int i2 = this.m_nRequestStatus;
                RequestListOnly();
                this.m_nRequestStatus = 7;
                if (i2 == 6) {
                    return StartNextItem(false, false);
                }
            }
            return 1;
        }

        public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
            InternalFailure(8);
            NotifyProgressMade notifyProgressMade = this.m_NotifyOnProgress;
            if (notifyProgressMade != null) {
                notifyProgressMade.OnProgressResolved(this);
            }
        }

        public void OnListDirectoryResponse(IStreamingBootStrap iStreamingBootStrap, int i, FileSystemDirectoryListEntry_t[] fileSystemDirectoryListEntry_tArr) {
            int i2;
            int i3;
            long j;
            long j2;
            long j3;
            FileSystemDirectoryListEntry_t[] fileSystemDirectoryListEntry_tArr2 = fileSystemDirectoryListEntry_tArr;
            StringBuilder sb = new StringBuilder();
            sb.append("Directory Response \"");
            sb.append(this.m_sRemotePath);
            sb.append("\" start");
            Log.i("com.valvesoftware.BootStrapClient.Recursive.OnListDirectoryResponse", sb.toString());
            NotifyProgressMade notifyProgressMade = this.m_NotifyOnProgress;
            if (notifyProgressMade != null) {
                notifyProgressMade.OnPartialProgressMade(this);
            }
            String str = "";
            String str2 = "/";
            if (this.m_sRecursiveRelativePath.compareTo(str) != 0) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(this.m_sRecursiveRelativePath);
                sb2.append(str2);
                str = sb2.toString();
            }
            int i4 = 1;
            if (fileSystemDirectoryListEntry_tArr2 == null || fileSystemDirectoryListEntry_tArr2.length <= 0) {
                if (i == 0 && this.m_nRemoteLastModifiedTime == -1) {
                    this.m_nRemoteLastModifiedTime = 0;
                    this.m_bSaveSyncState = true;
                }
                i3 = 0;
                i2 = 0;
            } else {
                i3 = 0;
                i2 = 0;
                for (int i5 = 0; i5 < fileSystemDirectoryListEntry_tArr2.length; i5++) {
                    if ((fileSystemDirectoryListEntry_tArr2[i5].nSetFields & 1) == 0) {
                        InternalFailure(8);
                        return;
                    }
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(str2);
                    sb3.append(fileSystemDirectoryListEntry_tArr2[i5].sName);
                    String sb4 = sb3.toString();
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append(str);
                    sb5.append(fileSystemDirectoryListEntry_tArr2[i5].sName);
                    String sb6 = sb5.toString();
                    if ((fileSystemDirectoryListEntry_tArr2[i5].nSetFields & 4) != 0) {
                        long j4 = fileSystemDirectoryListEntry_tArr2[i5].nLastModifiedSecondsSinceEpoch;
                        if (j4 > this.m_nRemoteLastModifiedTime) {
                            this.m_nRemoteLastModifiedTime = j4;
                            this.m_bSaveSyncState = true;
                        }
                        j = j4;
                    } else {
                        j = 0;
                    }
                    long j5 = (fileSystemDirectoryListEntry_tArr2[i5].nSetFields & 2) != 0 ? fileSystemDirectoryListEntry_tArr2[i5].nFileSize : 0;
                    String str3 = "com.valvesoftware.BootStrapClient.RecursiveDownloader";
                    if ((fileSystemDirectoryListEntry_tArr2[i5].nAttributeFlags & 1) == 0) {
                        try {
                            if (this.m_Filter != null && !this.m_Filter.ShouldDownloadFile(sb6)) {
                                fileSystemDirectoryListEntry_tArr2[i5] = null;
                            }
                        } catch (Throwable th) {
                            StringBuilder sb7 = new StringBuilder();
                            sb7.append("Exceptional ShouldDownloadFile ");
                            sb7.append(th.getMessage());
                            Log.i(str3, sb7.toString());
                        }
                        StringBuilder sb8 = new StringBuilder();
                        sb8.append(this.m_sLocalPath);
                        sb8.append(sb4);
                        File file = new File(sb8.toString());
                        if (file.exists()) {
                            j3 = file.length();
                            j2 = file.lastModified() / 1000;
                        } else {
                            j3 = 0;
                            j2 = 0;
                        }
                        if (StreamingFileDownload.ShouldDownloadRemoteFile(j3, j2, this.m_TimeDelta, j5, j)) {
                            i3++;
                        } else {
                            fileSystemDirectoryListEntry_tArr2[i5] = null;
                        }
                    } else {
                        try {
                            if (fileSystemDirectoryListEntry_tArr2[i5].sName.equals(".") || fileSystemDirectoryListEntry_tArr2[i5].sName.equals("..") || (this.m_Filter != null && !this.m_Filter.ShouldRecurseIntoDirectory(sb6))) {
                                fileSystemDirectoryListEntry_tArr2[i5] = null;
                            }
                        } catch (Throwable th2) {
                            StringBuilder sb9 = new StringBuilder();
                            sb9.append("Exceptional ShouldRecurseIntoDirectory ");
                            sb9.append(th2.getMessage());
                            Log.i(str3, sb9.toString());
                        }
                        i2++;
                    }
                }
            }
            if (i3 == 0 && i2 == 0) {
                InternalSuccess();
                NotifyProgressMade notifyProgressMade2 = this.m_NotifyOnProgress;
                if (notifyProgressMade2 != null) {
                    notifyProgressMade2.OnProgressResolved(this);
                }
            } else {
                NotifyReceiver notifyReceiver = new NotifyReceiver();
                if (i3 != 0) {
                    this.m_Downloads = new StreamingFileDownload[i3];
                }
                if (i2 != 0) {
                    this.m_SubDirectories = new RecursiveDownload[i2];
                }
                int i6 = 0;
                int i7 = 0;
                int i8 = 0;
                while (i6 < fileSystemDirectoryListEntry_tArr2.length) {
                    if (fileSystemDirectoryListEntry_tArr2[i6] != null) {
                        StringBuilder sb10 = new StringBuilder();
                        sb10.append(str2);
                        sb10.append(fileSystemDirectoryListEntry_tArr2[i6].sName);
                        String sb11 = sb10.toString();
                        StringBuilder sb12 = new StringBuilder();
                        sb12.append(str);
                        sb12.append(fileSystemDirectoryListEntry_tArr2[i6].sName);
                        String sb13 = sb12.toString();
                        if ((fileSystemDirectoryListEntry_tArr2[i6].nAttributeFlags & i4) == 0) {
                            StreamingFileDownload[] streamingFileDownloadArr = this.m_Downloads;
                            StringBuilder sb14 = new StringBuilder();
                            sb14.append(this.m_sRemotePath);
                            sb14.append(sb11);
                            String sb15 = sb14.toString();
                            StringBuilder sb16 = new StringBuilder();
                            sb16.append(this.m_sLocalPath);
                            sb16.append(sb11);
                            StreamingFileDownload[] streamingFileDownloadArr2 = streamingFileDownloadArr;
                            StreamingFileDownload streamingFileDownload = new StreamingFileDownload(iStreamingBootStrap, sb15, sb16.toString(), null, notifyReceiver, fileSystemDirectoryListEntry_tArr2[i6]);
                            streamingFileDownloadArr2[i8] = streamingFileDownload;
                            i8++;
                        } else {
                            RecursiveDownload[] recursiveDownloadArr = this.m_SubDirectories;
                            JSONObject jSONObject = this.m_prevSyncRoot;
                            StringBuilder sb17 = new StringBuilder();
                            sb17.append(this.m_sRemotePath);
                            sb17.append(sb11);
                            String sb18 = sb17.toString();
                            StringBuilder sb19 = new StringBuilder();
                            sb19.append(this.m_sLocalPath);
                            sb19.append(sb11);
                            String sb20 = sb19.toString();
                            RemoteTimeDelta_t remoteTimeDelta_t = this.m_TimeDelta;
                            RecursiveDownload[] recursiveDownloadArr2 = recursiveDownloadArr;
                            RecursiveDownload recursiveDownload = new RecursiveDownload(iStreamingBootStrap, jSONObject, sb18, sb20, sb13, remoteTimeDelta_t, notifyReceiver, this.m_Filter, fileSystemDirectoryListEntry_tArr2[i6]);
                            recursiveDownloadArr2[i7] = recursiveDownload;
                            i7++;
                        }
                    }
                    i6++;
                    i4 = 1;
                }
                for (int i9 = 0; i9 < i7; i9++) {
                    this.m_SubDirectories[i9].RequestListOnly();
                }
                if (this.m_nRequestStatus == 5) {
                    this.m_nRequestStatus = 6;
                    NotifyProgressMade notifyProgressMade3 = this.m_NotifyOnProgress;
                    if (notifyProgressMade3 != null) {
                        notifyProgressMade3.OnPartialProgressMade(this);
                    }
                } else if (StartNextItem(false, false) != 1) {
                    NotifyProgressMade notifyProgressMade4 = this.m_NotifyOnProgress;
                    if (notifyProgressMade4 != null) {
                        notifyProgressMade4.OnProgressResolved(this);
                    }
                }
            }
        }

        /* access modifiers changed from: private */
        public void InternalSuccess() {
            String str = "modified";
            this.m_nRequestStatus = 9;
            if (this.m_bSaveSyncState && !this.m_bAnyErrors) {
                try {
                    this.m_SyncEntry.put(str, this.m_nRemoteLastModifiedTime);
                    if (this.m_SyncEntry.optLong(str, -1) == -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Setlong of ");
                        sb.append(this.m_nRemoteLastModifiedTime);
                        sb.append(" failed");
                        Log.i("com.valvesoftware.BootStrapClient.RecursiveDownload", sb.toString());
                    }
                    BootStrapClient.SaveSyncState(this.m_NotifyOnProgress != null);
                    this.m_bSaveSyncState = false;
                } catch (Throwable unused) {
                }
            }
        }

        /* access modifiers changed from: private */
        public void InternalFailure(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed with directory \"");
            sb.append(this.m_sRemotePath);
            sb.append("\"");
            Log.i("com.valvesoftware.BootStrapClient.RecursiveDownloader", sb.toString());
            try {
                throw new Throwable();
            } catch (Throwable th) {
                th.printStackTrace();
                OnParentAbort();
                this.m_nRequestStatus = i;
            }
        }

        /* access modifiers changed from: private */
        public int StartNextItem(boolean z, boolean z2) {
            String str = "Resolution for dir \"";
            String str2 = "\"";
            String str3 = "com.valvesoftware.BootStrapClient.Recursive";
            if (!z && this.m_Downloads != null) {
                long j = 0;
                long j2 = 0;
                int i = 0;
                while (true) {
                    StreamingFileDownload[] streamingFileDownloadArr = this.m_Downloads;
                    if (i < streamingFileDownloadArr.length) {
                        if (streamingFileDownloadArr[i] != null) {
                            int access$1100 = streamingFileDownloadArr[i].m_nRequestStatus;
                            int Start = this.m_Downloads[i].Start();
                            if (Start == 1) {
                                if (access$1100 == 4) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Downloading file \"");
                                    sb.append(this.m_Downloads[i].m_sRemotePath);
                                    sb.append(str2);
                                    Log.i(str3, sb.toString());
                                }
                                j2 += this.m_Downloads[i].m_nRemoteSize;
                                j++;
                                if (j2 >= 20971520 || j >= 20) {
                                    return 1;
                                }
                            } else {
                                if (Start != 2) {
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append(str);
                                    sb2.append(this.m_sRemotePath);
                                    sb2.append("\" aborted file \"");
                                    sb2.append(this.m_Downloads[i].m_sRemotePath);
                                    sb2.append(str2);
                                    Log.i(str3, sb2.toString());
                                    this.m_bAnyErrors = true;
                                }
                                this.m_Downloads[i] = null;
                            }
                        }
                        i++;
                    } else if (j > 0) {
                        return 1;
                    }
                }
            }
            if (!z2 && this.m_SubDirectories != null) {
                int i2 = 0;
                while (true) {
                    RecursiveDownload[] recursiveDownloadArr = this.m_SubDirectories;
                    if (i2 >= recursiveDownloadArr.length) {
                        break;
                    }
                    if (recursiveDownloadArr[i2] != null) {
                        int Start2 = recursiveDownloadArr[i2].Start();
                        if (Start2 == 1) {
                            return 1;
                        }
                        if (Start2 == 2) {
                            RecursiveDownload[] recursiveDownloadArr2 = this.m_SubDirectories;
                            if (recursiveDownloadArr2[i2].m_nRemoteLastModifiedTime > this.m_nRemoteLastModifiedTime) {
                                this.m_nRemoteLastModifiedTime = recursiveDownloadArr2[i2].m_nRemoteLastModifiedTime;
                                this.m_bSaveSyncState = true;
                            }
                        } else if (Start2 == 0) {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append(str);
                            sb3.append(this.m_sRemotePath);
                            sb3.append("\" failed directory \"");
                            sb3.append(this.m_SubDirectories[i2].m_sRemotePath);
                            sb3.append(str2);
                            Log.i(str3, sb3.toString());
                            this.m_bAnyErrors = true;
                        }
                        this.m_SubDirectories[i2] = null;
                    }
                    i2++;
                }
            }
            if (this.m_bAnyErrors) {
                InternalFailure(8);
                return 2;
            }
            InternalSuccess();
            return 0;
        }

        public boolean HasBeenIssued() {
            return this.m_nRequestStatus != 4;
        }

        public boolean HasSucceeded() {
            return this.m_nRequestStatus == 9;
        }

        public boolean HasAborted() {
            int i = this.m_nRequestStatus;
            return i == 8 || i == 3 || i == 2 || i == 1;
        }

        public long LastMadeProgress() {
            return this.m_nLastMadeProgress;
        }
    }

    public static class RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter implements IRecursiveDownloadFilter {
        String[] m_sExcludeExtensions;
        String[] m_sRelativeDirectories;

        public RecursiveDownloadExtensionExlusionAndRelativeDirectoryFilter(String[] strArr, String[] strArr2) {
            if (strArr != null) {
                this.m_sExcludeExtensions = new String[strArr.length];
                for (int i = 0; i < strArr.length; i++) {
                    String[] strArr3 = this.m_sExcludeExtensions;
                    StringBuilder sb = new StringBuilder();
                    sb.append(".");
                    sb.append(strArr[i]);
                    strArr3[i] = sb.toString();
                }
            } else {
                this.m_sExcludeExtensions = null;
            }
            this.m_sRelativeDirectories = strArr2;
        }

        public boolean ShouldRecurseIntoDirectory(String str) {
            if (this.m_sRelativeDirectories != null) {
                int i = 0;
                while (true) {
                    String[] strArr = this.m_sRelativeDirectories;
                    if (i >= strArr.length) {
                        break;
                    } else if (str.compareTo(strArr[i]) == 0) {
                        return false;
                    } else {
                        i++;
                    }
                }
            }
            return true;
        }

        public boolean ShouldDownloadFile(String str) {
            if (this.m_sExcludeExtensions != null) {
                int i = 0;
                while (true) {
                    String[] strArr = this.m_sExcludeExtensions;
                    if (i >= strArr.length) {
                        break;
                    } else if (str.endsWith(strArr[i])) {
                        return false;
                    } else {
                        i++;
                    }
                }
            }
            return true;
        }
    }

    public static class RemoteTimeDelta_t {
        public long nEarliestOffsetUS = Long.MIN_VALUE;
        public long nLatestOffsetUS = Long.MIN_VALUE;
    }

    private static class StreamingBootStrapIOHandler extends IStreamingBootStrapIOImpl {
        /* access modifiers changed from: private */
        public InputStream m_Input;
        private Socket m_NetworkSocket;
        private Thread m_NetworkingThread;
        /* access modifiers changed from: private */
        public OutputStream m_Output;
        /* access modifiers changed from: private */
        public Semaphore m_ReadCompletedSemaphore;
        /* access modifiers changed from: private */
        public Semaphore m_ReadOrWriteWaitingSemaphore;
        /* access modifiers changed from: private */
        public byte[] m_ReadTarget;
        /* access modifiers changed from: private */
        public Semaphore m_WriteCompletedSemaphore;
        /* access modifiers changed from: private */
        public byte[] m_WriteSource;
        /* access modifiers changed from: private */
        public boolean m_bRun = true;
        /* access modifiers changed from: private */
        public boolean m_bWriteError = false;
        /* access modifiers changed from: private */
        public int m_nReadTargetInt1;
        /* access modifiers changed from: private */
        public int m_nWriteSourceInt1;
        /* access modifiers changed from: private */
        public int m_nWriteSourceInt2;

        private class NetworkingThreadJob implements Runnable {
            private NetworkingThreadJob() {
            }

            public void run() {
                do {
                    try {
                        StreamingBootStrapIOHandler.this.m_ReadOrWriteWaitingSemaphore.acquire();
                    } catch (Throwable th) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("network thread semaphore exception: ");
                        sb.append(th.getMessage());
                        Log.i("com.valvesoftware.BootStrapClient.StreamingBootStrapIOHandler", sb.toString());
                    }
                    String str = "com.valvesoftware.BootStrapClient";
                    if (StreamingBootStrapIOHandler.this.m_ReadTarget != null) {
                        int access$2700 = StreamingBootStrapIOHandler.this.m_nReadTargetInt1;
                        try {
                            StreamingBootStrapIOHandler.this.m_nReadTargetInt1 = StreamingBootStrapIOHandler.this.m_Input.read(StreamingBootStrapIOHandler.this.m_ReadTarget, access$2700, StreamingBootStrapIOHandler.this.m_ReadTarget.length - access$2700);
                        } catch (Throwable th2) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("ReadData exception ");
                            sb2.append(th2.getMessage());
                            Log.i(str, sb2.toString());
                            StreamingBootStrapIOHandler.this.m_nReadTargetInt1 = 0;
                        }
                        StreamingBootStrapIOHandler.this.m_ReadTarget = null;
                        StreamingBootStrapIOHandler.this.m_ReadCompletedSemaphore.release();
                    }
                    if (StreamingBootStrapIOHandler.this.m_WriteSource != null) {
                        try {
                            StreamingBootStrapIOHandler.this.m_Output.write(StreamingBootStrapIOHandler.this.m_WriteSource, StreamingBootStrapIOHandler.this.m_nWriteSourceInt1, StreamingBootStrapIOHandler.this.m_nWriteSourceInt2);
                        } catch (Throwable th3) {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append("WriteRaw exception ");
                            sb3.append(th3.getMessage());
                            Log.i(str, sb3.toString());
                            StreamingBootStrapIOHandler.this.m_bWriteError = true;
                        }
                        StreamingBootStrapIOHandler.this.m_WriteSource = null;
                        StreamingBootStrapIOHandler.this.m_WriteCompletedSemaphore.release();
                    }
                } while (StreamingBootStrapIOHandler.this.m_bRun);
            }
        }

        public void OnPacketStart(long j) {
        }

        public StreamingBootStrapIOHandler(Socket socket) {
            this.m_NetworkSocket = socket;
            this.m_ReadOrWriteWaitingSemaphore = new Semaphore(0);
            this.m_ReadCompletedSemaphore = new Semaphore(0);
            this.m_WriteCompletedSemaphore = new Semaphore(0);
            this.m_ReadTarget = null;
            this.m_WriteSource = null;
            try {
                this.m_Input = this.m_NetworkSocket.getInputStream();
                this.m_Output = this.m_NetworkSocket.getOutputStream();
                this.m_NetworkingThread = new Thread(new NetworkingThreadJob());
                this.m_NetworkingThread.setName("com.valvesoftware.BootStrapClient.StreamingBootStrapIOHandler networking thread");
                this.m_NetworkingThread.start();
            } catch (Throwable unused) {
                this.m_Input = null;
                this.m_Output = null;
                this.m_bWriteError = true;
                this.m_NetworkingThread = null;
                this.m_ReadOrWriteWaitingSemaphore = null;
            }
        }

        /* access modifiers changed from: protected */
        public void finalize() {
            Socket socket = this.m_NetworkSocket;
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable unused) {
                }
            }
        }

        public void WriteRaw(byte[] bArr, int i, int i2) {
            if (!this.m_bWriteError) {
                this.m_nWriteSourceInt1 = i;
                this.m_nWriteSourceInt2 = i2;
                this.m_WriteSource = bArr;
                this.m_ReadOrWriteWaitingSemaphore.release();
                try {
                    this.m_WriteCompletedSemaphore.acquire();
                } catch (Throwable th) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("ReadData semaphore exception: ");
                    sb.append(th.getMessage());
                    Log.i("com.valvesoftware.BootStrapClient.StreamingBootStrapIOHandler", sb.toString());
                }
            }
        }

        public boolean WaitForData(int i) {
            long j;
            boolean z = true;
            if (i == 0) {
                try {
                    if (this.m_Input.available() <= 0) {
                        z = false;
                    }
                    return z;
                } catch (Throwable unused) {
                }
            } else {
                if (i == Integer.MIN_VALUE) {
                    j = IStreamingBootStrap.INT64_MAX;
                } else {
                    j = System.currentTimeMillis() + ((long) i);
                }
                while (this.m_Input.available() <= 0) {
                    Thread.yield();
                    if (j <= System.currentTimeMillis()) {
                        return false;
                    }
                }
                return true;
            }
        }

        public long ReadData(byte[] bArr, int i) {
            this.m_nReadTargetInt1 = i;
            this.m_ReadTarget = bArr;
            this.m_ReadOrWriteWaitingSemaphore.release();
            try {
                this.m_ReadCompletedSemaphore.acquire();
            } catch (Throwable th) {
                StringBuilder sb = new StringBuilder();
                sb.append("ReadData semaphore exception: ");
                sb.append(th.getMessage());
                Log.i("com.valvesoftware.BootStrapClient.StreamingBootStrapIOHandler", sb.toString());
            }
            return (long) this.m_nReadTargetInt1;
        }

        public void OnPacketEnd() {
            try {
                this.m_Output.flush();
            } catch (Throwable unused) {
            }
        }
    }

    public static class StreamingFileDownload extends IStreamHandler {
        public static final int STATUS_ABORTED = 7;
        public static final int STATUS_PENDING = 6;
        public static final int STATUS_QUERIED = 5;
        public static final int STATUS_SUCCEEDED = 8;
        public static final int STATUS_UNISSUED = 4;
        private static File m_hTempFileDir;
        private IStreamingBootStrap m_Connection;
        /* access modifiers changed from: private */
        public NotifyProgressMade m_NotifyOnProgress;
        private RemoteTimeDelta_t m_TimeDelta;
        /* access modifiers changed from: private */
        public File m_localTempFile;
        /* access modifiers changed from: private */
        public FileOutputStream m_localTempFileWriteStream;
        private long m_nLastMadeProgress;
        /* access modifiers changed from: private */
        public long m_nRemoteLastModifiedTime;
        /* access modifiers changed from: private */
        public long m_nRemoteSize;
        /* access modifiers changed from: private */
        public int m_nRequestStatus;
        private int m_nSetAttributes;
        private long m_nStreamBytesReceived;
        /* access modifiers changed from: private */
        public String m_sLocalPath;
        /* access modifiers changed from: private */
        public String m_sRemotePath;

        private class QueryFileResponseHandler extends IResponseHandler_FileSystem {
            private RemoteTimeDelta_t m_TimeDelta;
            private long m_nLocalLastModifiedMS;
            private long m_nLocalSize;

            QueryFileResponseHandler(RemoteTimeDelta_t remoteTimeDelta_t, long j, long j2) {
                this.m_TimeDelta = remoteTimeDelta_t;
                this.m_nLocalLastModifiedMS = j;
                this.m_nLocalSize = j2;
            }

            public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
                StringBuilder sb = new StringBuilder();
                sb.append("Download stream of file \"");
                sb.append(StreamingFileDownload.this.m_sRemotePath);
                sb.append("\" -> \"");
                sb.append(StreamingFileDownload.this.m_sLocalPath);
                sb.append("\" response aborted during query phase");
                Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload", sb.toString());
                StreamingFileDownload.this.InternalFailure(7);
                if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                    StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                }
            }

            public void OnQueryFileResponse(IStreamingBootStrap iStreamingBootStrap, int i, FileSystemQueryResult_t fileSystemQueryResult_t) {
                if ((fileSystemQueryResult_t.nSetFields & 4) != 0) {
                    StreamingFileDownload.this.m_nRemoteLastModifiedTime = fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch;
                }
                if ((fileSystemQueryResult_t.nSetFields & 2) != 0) {
                    StreamingFileDownload.this.m_nRemoteSize = fileSystemQueryResult_t.nFileSize;
                }
                if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                    StreamingFileDownload.this.m_NotifyOnProgress.OnPartialProgressMade(StreamingFileDownload.this);
                }
                if (i != 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Download stream of file \"");
                    sb.append(StreamingFileDownload.this.m_sRemotePath);
                    sb.append("\" -> \"");
                    sb.append(StreamingFileDownload.this.m_sLocalPath);
                    sb.append("\" response failed during query phase: ");
                    sb.append(i);
                    Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload", sb.toString());
                    StreamingFileDownload.this.InternalFailure(i);
                    if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                        StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                    }
                    return;
                }
                if (StreamingFileDownload.ShouldDownloadRemoteFile(this.m_nLocalSize, this.m_nLocalLastModifiedMS / 1000, this.m_TimeDelta, StreamingFileDownload.this.m_nRemoteSize, StreamingFileDownload.this.m_nRemoteLastModifiedTime)) {
                    StreamingFileDownload.this.IssueDownloadRequest(iStreamingBootStrap);
                } else {
                    StreamingFileDownload.this.InternalSuccess();
                    if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                        StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                    }
                }
            }
        }

        private class RetrieveResponseHandler extends IResponseHandler_FileSystem {
            private RetrieveResponseHandler() {
            }

            public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
                StringBuilder sb = new StringBuilder();
                sb.append("Download stream of file \"");
                sb.append(StreamingFileDownload.this.m_sRemotePath);
                sb.append("\" -> \"");
                sb.append(StreamingFileDownload.this.m_sLocalPath);
                sb.append("\" response aborted during stream phase");
                Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload", sb.toString());
                StreamingFileDownload.this.InternalFailure(7);
                if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                    StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                }
            }

            public void OnRetrieveFileResponse(IStreamingBootStrap iStreamingBootStrap, int i, long j) {
                String str;
                if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                    StreamingFileDownload.this.m_NotifyOnProgress.OnPartialProgressMade(StreamingFileDownload.this);
                }
                String str2 = "\" -> \"";
                String str3 = "Download stream of file \"";
                String str4 = "com.valvesoftware.BootStrapClient.StreamingFileDownload";
                if (i == 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str3);
                    sb.append(StreamingFileDownload.this.m_sRemotePath);
                    sb.append(str2);
                    sb.append(StreamingFileDownload.this.m_sLocalPath);
                    sb.append("\" incoming on stream ");
                    sb.append(j);
                    Log.i(str4, sb.toString());
                    try {
                        StreamingFileDownload.this.m_localTempFileWriteStream = new FileOutputStream(StreamingFileDownload.this.m_localTempFile);
                        StreamingFileDownload.this.m_nRequestStatus = 6;
                        BootStrapClient.Assert(iStreamingBootStrap.RegisterIncomingStreamHandler(j, StreamingFileDownload.this));
                    } catch (Throwable th) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(str3);
                        sb2.append(StreamingFileDownload.this.m_sRemotePath);
                        sb2.append(str2);
                        sb2.append(StreamingFileDownload.this.m_sLocalPath);
                        sb2.append("\" fired an exception while attempting to write the local file. ");
                        sb2.append(th.getMessage());
                        Log.i(str4, sb2.toString());
                        StreamingFileDownload.this.InternalFailure(7);
                        if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                            StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                        }
                    }
                } else {
                    if (i == 1) {
                        str = "UNIMPLEMENTED";
                    } else if (i == 2) {
                        str = "GENERAL_FAILURE";
                    } else if (i != 3) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("code ");
                        sb3.append(i);
                        str = sb3.toString();
                    } else {
                        str = "PATH_ERROR";
                    }
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(str3);
                    sb4.append(StreamingFileDownload.this.m_sRemotePath);
                    sb4.append(str2);
                    sb4.append(StreamingFileDownload.this.m_sLocalPath);
                    sb4.append("\" returned error \"");
                    sb4.append(str);
                    sb4.append("\"");
                    Log.i(str4, sb4.toString());
                    StreamingFileDownload.this.InternalFailure(i);
                    if (StreamingFileDownload.this.m_NotifyOnProgress != null) {
                        StreamingFileDownload.this.m_NotifyOnProgress.OnProgressResolved(this);
                    }
                }
            }
        }

        public void OnDownloadFailure(String str, String str2) {
        }

        public void OnDownloadSuccess(String str, String str2) {
        }

        /* access modifiers changed from: protected */
        public void OnFinalRelease() {
        }

        public StreamingFileDownload(IStreamingBootStrap iStreamingBootStrap, String str, String str2, RemoteTimeDelta_t remoteTimeDelta_t, int i, NotifyProgressMade notifyProgressMade, FileSystemQueryResult_t fileSystemQueryResult_t) {
            this.m_Connection = iStreamingBootStrap;
            this.m_sRemotePath = str;
            this.m_sLocalPath = str2;
            this.m_nSetAttributes = i;
            this.m_localTempFile = null;
            this.m_nRequestStatus = 4;
            this.m_TimeDelta = remoteTimeDelta_t;
            this.m_NotifyOnProgress = notifyProgressMade;
            this.m_nLastMadeProgress = System.currentTimeMillis();
            this.m_nRemoteLastModifiedTime = 0;
            this.m_nRemoteSize = 0;
            this.m_nStreamBytesReceived = 0;
            if (fileSystemQueryResult_t != null) {
                if ((4 & fileSystemQueryResult_t.nSetFields) != 0) {
                    this.m_nRemoteLastModifiedTime = fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch;
                }
                if ((fileSystemQueryResult_t.nSetFields & 2) != 0) {
                    this.m_nRemoteSize = fileSystemQueryResult_t.nFileSize;
                }
            }
        }

        public StreamingFileDownload(IStreamingBootStrap iStreamingBootStrap, String str, String str2, RemoteTimeDelta_t remoteTimeDelta_t, NotifyProgressMade notifyProgressMade, FileSystemQueryResult_t fileSystemQueryResult_t) {
            this(iStreamingBootStrap, str, str2, remoteTimeDelta_t, 6, notifyProgressMade, fileSystemQueryResult_t);
        }

        public void PrintStatus(String str) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("File ");
            sb.append(this.m_nRequestStatus);
            sb.append(" \"");
            sb.append(this.m_sRemotePath);
            sb.append("\" -> \"");
            sb.append(this.m_sLocalPath);
            sb.append("\"");
            Log.i("com.valvesoftware.BootStrapClient", sb.toString());
        }

        public int Start() {
            if (this.m_nRequestStatus == 4) {
                if (this.m_TimeDelta != null) {
                    File file = new File(this.m_sLocalPath);
                    if (file.exists()) {
                        this.m_nRequestStatus = 5;
                        IStreamingBootStrap iStreamingBootStrap = this.m_Connection;
                        String str = this.m_sRemotePath;
                        QueryFileResponseHandler queryFileResponseHandler = new QueryFileResponseHandler(this.m_TimeDelta, file.lastModified(), file.length());
                        iStreamingBootStrap.QueryFile(str, 6, queryFileResponseHandler);
                    } else {
                        this.m_nRequestStatus = 6;
                        IssueDownloadRequest(this.m_Connection);
                    }
                } else {
                    this.m_nRequestStatus = 6;
                    IssueDownloadRequest(this.m_Connection);
                }
                return 1;
            } else if (HasSucceeded() || HasAborted()) {
                return 2;
            } else {
                return 1;
            }
        }

        /* access modifiers changed from: private */
        public void IssueDownloadRequest(IStreamingBootStrap iStreamingBootStrap) {
            File file = new File(this.m_sLocalPath);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                this.m_localTempFile = file;
            } else {
                this.m_localTempFile = GetTempFile();
            }
            this.m_nRequestStatus = 6;
            iStreamingBootStrap.RetrieveFile(this.m_sRemotePath, new RetrieveResponseHandler());
        }

        public static boolean ShouldDownloadRemoteFile(long j, long j2, RemoteTimeDelta_t remoteTimeDelta_t, long j3, long j4) {
            return j != j3 || j2 + (remoteTimeDelta_t.nLatestOffsetUS / 1000000) < j4;
        }

        private boolean WriteFinalFile() {
            boolean z;
            FileOutputStream fileOutputStream = this.m_localTempFileWriteStream;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    this.m_localTempFileWriteStream.close();
                    this.m_localTempFileWriteStream = null;
                } catch (Throwable unused) {
                    this.m_localTempFileWriteStream = null;
                    return false;
                }
            }
            File file = new File(this.m_sLocalPath);
            String str = "com.valvesoftware.BootStrapClient.java";
            if (this.m_localTempFile.getAbsolutePath().compareTo(file.getAbsolutePath()) == 0) {
                this.m_localTempFile = null;
            } else {
                String str2 = "Creating final file threw exception ";
                String str3 = "com.valvesoftware.BootStrapClient.StreamingFileDownload";
                if (!file.exists()) {
                    try {
                        file.getParentFile().mkdirs();
                        z = file.createNewFile();
                    } catch (Throwable th) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(str2);
                        sb.append(th.getMessage());
                        Log.i(str3, sb.toString());
                        return false;
                    }
                } else {
                    file.delete();
                    try {
                        z = file.createNewFile();
                    } catch (Throwable th2) {
                        Throwable th3 = th2;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(str2);
                        sb2.append(th3.getMessage());
                        Log.i(str3, sb2.toString());
                        return false;
                    }
                }
                File file2 = this.m_localTempFile;
                if (file2 != null && z) {
                    try {
                        FileInputStream fileInputStream = new FileInputStream(file2);
                        FileOutputStream fileOutputStream2 = new FileOutputStream(file);
                        FileChannel channel = fileInputStream.getChannel();
                        FileChannel channel2 = fileOutputStream2.getChannel();
                        long size = channel.size();
                        long j = 0;
                        do {
                            j += channel2.transferFrom(channel, j, size - j);
                            if (this.m_NotifyOnProgress != null) {
                                this.m_NotifyOnProgress.OnPartialProgressMade(this);
                            }
                        } while (j < size);
                        fileOutputStream2.flush();
                        fileInputStream.close();
                        fileOutputStream2.close();
                        System.gc();
                        this.m_localTempFile.delete();
                        this.m_localTempFile = null;
                    } catch (Throwable th4) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("File move threw exception: ");
                        sb3.append(th4.getMessage());
                        Log.i(str, sb3.toString());
                    }
                }
            }
            if (!file.exists()) {
                return false;
            }
            boolean z2 = (this.m_nSetAttributes & 2) != 0;
            boolean z3 = (this.m_nSetAttributes & 4) != 0;
            boolean z4 = (this.m_nSetAttributes & 8) != 0;
            if (z2 != file.canRead() && !file.setReadable(z2)) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("File can't set readable: ");
                sb4.append(file.getPath());
                Log.i(str, sb4.toString());
                return false;
            } else if (z3 != file.canWrite() && !file.setWritable(z3)) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("File can't set writable: ");
                sb5.append(file.getPath());
                Log.i(str, sb5.toString());
                return false;
            } else if (z4 == file.canExecute() || file.setExecutable(z4)) {
                return true;
            } else {
                StringBuilder sb6 = new StringBuilder();
                sb6.append("File can't set executable: ");
                sb6.append(file.getPath());
                Log.i(str, sb6.toString());
                return false;
            }
        }

        /* access modifiers changed from: private */
        public void InternalSuccess() {
            this.m_nRequestStatus = 8;
            OnDownloadSuccess(this.m_sRemotePath, this.m_sLocalPath);
        }

        /* access modifiers changed from: private */
        public void InternalFailure(int i) {
            if (i != 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("set ");
                sb.append(i);
                Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload.InternalFailure", sb.toString());
                try {
                    throw new Throwable();
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
            this.m_nRequestStatus = i;
            this.m_localTempFileWriteStream = null;
            File file = this.m_localTempFile;
            if (file != null) {
                file.delete();
                this.m_localTempFile = null;
            }
            OnDownloadFailure(this.m_sRemotePath, this.m_sLocalPath);
        }

        public boolean HasBeenIssued() {
            return this.m_nRequestStatus != 4;
        }

        public boolean HasSucceeded() {
            return this.m_nRequestStatus == 8;
        }

        public boolean HasAborted() {
            int i = this.m_nRequestStatus;
            return (i == 5 || i == 6 || i == 0 || i == 8 || i == 4) ? false : true;
        }

        public long LastMadeProgress() {
            return this.m_nLastMadeProgress;
        }

        public void OnStreamStart(long j) {
            this.m_nLastMadeProgress = System.currentTimeMillis();
            NotifyProgressMade notifyProgressMade = this.m_NotifyOnProgress;
            if (notifyProgressMade != null) {
                notifyProgressMade.OnPartialProgressMade(this);
            }
        }

        public void OnStreamEnd(boolean z, boolean z2, String str) {
            String str2 = "Download stream for \"";
            String str3 = "com.valvesoftware.BootStrapClient.StreamingFileDownload";
            if (!z2) {
                StringBuilder sb = new StringBuilder();
                sb.append(str2);
                sb.append(this.m_sRemotePath);
                sb.append("\" succeeded.");
                Log.i(str3, sb.toString());
                if (WriteFinalFile()) {
                    InternalSuccess();
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str2);
                    sb2.append(this.m_sRemotePath);
                    sb2.append("\" failed to write final file.");
                    Log.i(str3, sb2.toString());
                    InternalFailure(7);
                }
            } else {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(str2);
                sb3.append(this.m_sRemotePath);
                sb3.append("\" was interrupted. \"");
                if (str == null) {
                    str = "No reason given";
                }
                sb3.append(str);
                sb3.append("\"");
                Log.i(str3, sb3.toString());
                InternalFailure(7);
            }
            NotifyProgressMade notifyProgressMade = this.m_NotifyOnProgress;
            if (notifyProgressMade != null) {
                notifyProgressMade.OnProgressResolved(this);
            }
        }

        public boolean HandleStreamChunk(byte[] bArr, int i, int i2) {
            this.m_nLastMadeProgress = System.currentTimeMillis();
            NotifyProgressMade notifyProgressMade = this.m_NotifyOnProgress;
            if (notifyProgressMade != null) {
                notifyProgressMade.OnPartialProgressMade(this);
            }
            FileOutputStream fileOutputStream = this.m_localTempFileWriteStream;
            if (fileOutputStream == null) {
                return false;
            }
            try {
                fileOutputStream.write(bArr, i, i2);
                this.m_nStreamBytesReceived += (long) i2;
                return true;
            } catch (Throwable th) {
                StringBuilder sb = new StringBuilder();
                sb.append("Download stream for \"");
                sb.append(this.m_sRemotePath);
                sb.append("\" fired an exception while writing the local temp \"");
                sb.append(this.m_localTempFileWriteStream);
                sb.append("\". ");
                sb.append(th.getMessage());
                Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload", sb.toString());
                InternalFailure(7);
                NotifyProgressMade notifyProgressMade2 = this.m_NotifyOnProgress;
                if (notifyProgressMade2 != null) {
                    notifyProgressMade2.OnProgressResolved(this);
                }
                return false;
            }
        }

        static {
            File file = new File(JNI_Environment.GetPublicPath(), "temp");
            if (file.exists() || file.mkdir()) {
                m_hTempFileDir = file;
            }
        }

        private static File GetTempFile() {
            File file = null;
            try {
                String str = "BootStrap_Client_StreamingFileDownload";
                if (m_hTempFileDir != null) {
                    file = File.createTempFile(str, null, m_hTempFileDir);
                } else {
                    file = File.createTempFile(str, null);
                }
                file.deleteOnExit();
            } catch (Throwable th) {
                StringBuilder sb = new StringBuilder();
                sb.append("exception creating temp file ");
                sb.append(th.getMessage());
                Log.i("com.valvesoftware.BootStrapClient.StreamingFileDownload", sb.toString());
            }
            return file;
        }
    }

    public static ConnectionResult connectToDevPC(SocketAddress socketAddress, int i, int i2) {
        String str;
        StringBuilder sb;
        ConnectionResult connectionResult = new ConnectionResult();
        StreamingBootStrapIOHandler[] streamingBootStrapIOHandlerArr = {null};
        Runnable init = new Runnable() {
            private SocketAddress m_devPCAddress;
            private StreamingBootStrapIOHandler[] m_ioHandlerResult;
            private int m_nConnectionTimeout;

            /* JADX WARNING: Removed duplicated region for block: B:14:0x002b A[SYNTHETIC, Splitter:B:14:0x002b] */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                    r4 = this;
                    r0 = 0
                    java.net.Socket r1 = new java.net.Socket     // Catch:{ Throwable -> 0x0028 }
                    r1.<init>()     // Catch:{ Throwable -> 0x0028 }
                    r0 = 1
                    r1.setKeepAlive(r0)     // Catch:{ Throwable -> 0x0026 }
                    java.net.SocketAddress r0 = r4.m_devPCAddress     // Catch:{ Throwable -> 0x0026 }
                    int r2 = r4.m_nConnectionTimeout     // Catch:{ Throwable -> 0x0026 }
                    r1.connect(r0, r2)     // Catch:{ Throwable -> 0x0026 }
                    boolean r0 = r1.isConnected()     // Catch:{ Throwable -> 0x0026 }
                    if (r0 != 0) goto L_0x001b
                    r1.close()     // Catch:{ Throwable -> 0x0026 }
                    return
                L_0x001b:
                    com.valvesoftware.BootStrapClient$StreamingBootStrapIOHandler[] r0 = r4.m_ioHandlerResult     // Catch:{ Throwable -> 0x0026 }
                    r2 = 0
                    com.valvesoftware.BootStrapClient$StreamingBootStrapIOHandler r3 = new com.valvesoftware.BootStrapClient$StreamingBootStrapIOHandler     // Catch:{ Throwable -> 0x0026 }
                    r3.<init>(r1)     // Catch:{ Throwable -> 0x0026 }
                    r0[r2] = r3     // Catch:{ Throwable -> 0x0026 }
                    goto L_0x002e
                L_0x0026:
                    goto L_0x0029
                L_0x0028:
                    r1 = r0
                L_0x0029:
                    if (r1 == 0) goto L_0x002e
                    r1.close()     // Catch:{ Throwable -> 0x002e }
                L_0x002e:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.BootStrapClient.AnonymousClass1.run():void");
            }

            public Runnable init(SocketAddress socketAddress, int i, StreamingBootStrapIOHandler[] streamingBootStrapIOHandlerArr) {
                this.m_devPCAddress = socketAddress;
                this.m_nConnectionTimeout = i;
                this.m_ioHandlerResult = streamingBootStrapIOHandlerArr;
                return this;
            }
        }.init(socketAddress, i, streamingBootStrapIOHandlerArr);
        if (!CanNetworkOnCurrentThread()) {
            Thread thread = new Thread(init);
            thread.setName("BootStrapClient.connectToDevPC work thread");
            thread.start();
            try {
                thread.join();
            } catch (Throwable th) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Exception Thrown: ");
                sb2.append(th.getMessage());
                connectionResult.sFailureMessageIfFailed = sb2.toString();
            }
        } else {
            init.run();
        }
        StreamingBootStrapIOHandler streamingBootStrapIOHandler = streamingBootStrapIOHandlerArr[0];
        if (streamingBootStrapIOHandler == null) {
            return connectionResult;
        }
        String str2 = new String("Boot Strap");
        String str3 = new String("Android: com.valvesoftware.BootStrapClient");
        short length = (short) (str2.length() + 8 + str3.length() + 2);
        streamingBootStrapIOHandler.OnPacketStart((long) (length + 2));
        streamingBootStrapIOHandler.WriteUINT16(length);
        streamingBootStrapIOHandler.WriteString(str2);
        streamingBootStrapIOHandler.WriteUINT32(4);
        streamingBootStrapIOHandler.WriteString(str3);
        streamingBootStrapIOHandler.WriteUINT32(1);
        streamingBootStrapIOHandler.OnPacketEnd();
        long currentTimeMillis = System.currentTimeMillis();
        long j = ((long) i2) + currentTimeMillis;
        byte[] bArr = new byte[2];
        int i3 = 0;
        boolean z = true;
        while (currentTimeMillis < j) {
            if (streamingBootStrapIOHandler.WaitForData((int) (j - currentTimeMillis))) {
                i3 += (int) streamingBootStrapIOHandler.ReadData(bArr, i3);
                if (i3 == bArr.length) {
                    if (!z) {
                        break;
                    }
                    bArr = new byte[((short) (bArr[1] | (bArr[0] << 8)))];
                    i3 -= 2;
                    z = false;
                } else {
                    continue;
                }
            }
            currentTimeMillis = System.currentTimeMillis();
        }
        if (i3 != bArr.length || z) {
            if (z) {
                sb = new StringBuilder();
                sb.append("Timed out waiting for message size (");
                sb.append(i3);
                str = ")";
            } else {
                sb = new StringBuilder();
                sb.append("Time out waiting for all ");
                sb.append(bArr.length);
                str = " bytes of greeting";
            }
            sb.append(str);
            connectionResult.sFailureMessageIfFailed = sb.toString();
            return connectionResult;
        }
        int SBS_strend = StaticHelpers.SBS_strend(bArr, 0, i3);
        if (SBS_strend == i3) {
            connectionResult.sFailureMessageIfFailed = "Non-terminated hello string";
            return connectionResult;
        }
        String str4 = new String(bArr, 0, SBS_strend + 0, IStreamingBootStrap.NETWORK_STRING_CHARSET);
        int i4 = SBS_strend + 1;
        if (!str4.matches(str2)) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Hello mismatch: \"");
            sb3.append(str4);
            sb3.append("\" (Expected \"");
            sb3.append(str2);
            sb3.append("\")");
            connectionResult.sFailureMessageIfFailed = sb3.toString();
            return connectionResult;
        }
        String str5 = "Truncated greeting";
        if (i3 - i4 < 4) {
            connectionResult.sFailureMessageIfFailed = str5;
            return connectionResult;
        }
        byte b = (bArr[i4 + 0] << 24) | (bArr[i4 + 1] << 16) | (bArr[i4 + 2] << 8) | bArr[i4 + 3];
        int i5 = i4 + 4;
        int SBS_strend2 = StaticHelpers.SBS_strend(bArr, i5, i3);
        if (SBS_strend2 == i3) {
            connectionResult.sFailureMessageIfFailed = "Non-terminated implementation string";
            return connectionResult;
        }
        String str6 = new String(bArr, i5, SBS_strend2 - i5, IStreamingBootStrap.NETWORK_STRING_CHARSET);
        int i6 = SBS_strend2 + 1;
        if (i3 - i6 != 4) {
            connectionResult.sFailureMessageIfFailed = str5;
            return connectionResult;
        }
        byte b2 = bArr[i6 + 0];
        byte b3 = bArr[i6 + 1];
        byte b4 = bArr[i6 + 2];
        byte b5 = bArr[i6 + 3];
        StreamingBootStrap_JavaImpl streamingBootStrap_JavaImpl = new StreamingBootStrap_JavaImpl(streamingBootStrapIOHandler, b, true);
        if (streamingBootStrap_JavaImpl.GetConnectionState() != 0) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Connection Immediately entered failure state. Remote Implementation \"");
            sb4.append(str6);
            sb4.append("\" : ");
            sb4.append(b);
            connectionResult.sFailureMessageIfFailed = sb4.toString();
            return connectionResult;
        }
        streamingBootStrap_JavaImpl.SetRequestHandler_FileSystem(new DefaultFileSystemHandler());
        connectionResult.connection = streamingBootStrap_JavaImpl;
        connectionResult.sRemoteImplementationName = str6;
        connectionResult.nRemoteVersionNumber = b;
        return connectionResult;
    }

    public static ConnectionResult connectToDevPC(int i, int i2, int i3) throws IOException, SocketException, UnknownHostException {
        ConnectionResult connectToDevPC = connectToDevPC((SocketAddress) new InetSocketAddress(InetAddress.getByAddress(defaultDevPCAddress), i), i2, i3);
        return connectToDevPC.connection == null ? connectToDevPC((SocketAddress) new InetSocketAddress(InetAddress.getByAddress(defaultDevPCAddress2), i), i2, i3) : connectToDevPC;
    }

    public static ConnectionResult connectToDevPC(int i, int i2) throws IOException, SocketException, UnknownHostException {
        ConnectionResult connectToDevPC = connectToDevPC((SocketAddress) new InetSocketAddress(InetAddress.getByAddress(defaultDevPCAddress), nDefaultDevPCPort), i, i2);
        return connectToDevPC.connection == null ? connectToDevPC((SocketAddress) new InetSocketAddress(InetAddress.getByAddress(defaultDevPCAddress2), nDefaultDevPCPort), i, i2) : connectToDevPC;
    }

    public static RemoteTimeDelta_t GetConnectionTimeDelta(IStreamingBootStrap iStreamingBootStrap, long j, boolean z) {
        RemoteTimeDelta_t remoteTimeDelta_t = new RemoteTimeDelta_t();
        String str = "BootStrapClient.CachedTimeDelta";
        if (z) {
            RemoteTimeDelta_t[] remoteTimeDelta_tArr = {null};
            iStreamingBootStrap.AccessContext(str, new IAccessContextCallback() {
                RemoteTimeDelta_t[] m_Output;

                public Object OnAccessContext(String str, Object obj) {
                    this.m_Output[0] = (RemoteTimeDelta_t) obj;
                    return obj;
                }

                /* access modifiers changed from: 0000 */
                public IAccessContextCallback Init(RemoteTimeDelta_t[] remoteTimeDelta_tArr) {
                    this.m_Output = remoteTimeDelta_tArr;
                    return this;
                }
            }.Init(remoteTimeDelta_tArr));
            RemoteTimeDelta_t remoteTimeDelta_t2 = remoteTimeDelta_tArr[0];
            if (remoteTimeDelta_t2 != null) {
                remoteTimeDelta_t.nEarliestOffsetUS = remoteTimeDelta_t2.nEarliestOffsetUS;
                remoteTimeDelta_t.nLatestOffsetUS = remoteTimeDelta_t2.nLatestOffsetUS;
                return remoteTimeDelta_t;
            }
        }
        long currentTimeMillis = System.currentTimeMillis() * 1000;
        boolean[] zArr = {false};
        iStreamingBootStrap.SendPing(new IResponseHandler_Ping() {
            RemoteTimeDelta_t m_RetVal;
            boolean[] m_bDone;
            long m_nStartTimeUS;

            public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
                this.m_bDone[0] = true;
            }

            public void OnPingResponse(IStreamingBootStrap iStreamingBootStrap, long j) {
                long currentTimeMillis = System.currentTimeMillis() * 1000;
                RemoteTimeDelta_t remoteTimeDelta_t = this.m_RetVal;
                remoteTimeDelta_t.nEarliestOffsetUS = j - currentTimeMillis;
                remoteTimeDelta_t.nLatestOffsetUS = j - this.m_nStartTimeUS;
                StringBuilder sb = new StringBuilder();
                sb.append("Received time delta data microseconds: ");
                sb.append((currentTimeMillis + this.m_nStartTimeUS) / 2);
                sb.append(" -> ");
                sb.append(j);
                sb.append(" ( ");
                sb.append(this.m_RetVal.nEarliestOffsetUS);
                sb.append("us / ");
                sb.append(this.m_RetVal.nLatestOffsetUS);
                sb.append("us [");
                sb.append(this.m_RetVal.nLatestOffsetUS - this.m_RetVal.nEarliestOffsetUS);
                sb.append("us] ).");
                Log.i("com.valvesoftware.BootStrapClient.java", sb.toString());
                this.m_bDone[0] = true;
            }

            public IResponseHandler_Ping Init(RemoteTimeDelta_t remoteTimeDelta_t, long j, boolean[] zArr) {
                this.m_RetVal = remoteTimeDelta_t;
                this.m_nStartTimeUS = j;
                this.m_bDone = zArr;
                return this;
            }
        }.Init(remoteTimeDelta_t, currentTimeMillis, zArr));
        long j2 = (currentTimeMillis / 1000) + j;
        do {
            iStreamingBootStrap.ProcessIncomingData(0);
            if (zArr[0]) {
                break;
            }
            Thread.yield();
        } while (j2 > System.currentTimeMillis());
        if (remoteTimeDelta_t.nLatestOffsetUS == Long.MIN_VALUE) {
            return null;
        }
        iStreamingBootStrap.AccessContext(str, new IAccessContextCallback() {
            RemoteTimeDelta_t m_Store;

            public Object OnAccessContext(String str, Object obj) {
                RemoteTimeDelta_t remoteTimeDelta_t = (RemoteTimeDelta_t) obj;
                if (obj == null) {
                    RemoteTimeDelta_t remoteTimeDelta_t2 = new RemoteTimeDelta_t();
                    remoteTimeDelta_t2.nEarliestOffsetUS = this.m_Store.nEarliestOffsetUS;
                    remoteTimeDelta_t2.nLatestOffsetUS = this.m_Store.nLatestOffsetUS;
                    return remoteTimeDelta_t2;
                }
                if (this.m_Store.nEarliestOffsetUS > remoteTimeDelta_t.nEarliestOffsetUS) {
                    remoteTimeDelta_t.nEarliestOffsetUS = this.m_Store.nEarliestOffsetUS;
                }
                if (this.m_Store.nLatestOffsetUS < remoteTimeDelta_t.nLatestOffsetUS) {
                    remoteTimeDelta_t.nLatestOffsetUS = this.m_Store.nLatestOffsetUS;
                }
                return remoteTimeDelta_t;
            }

            /* access modifiers changed from: 0000 */
            public IAccessContextCallback Init(RemoteTimeDelta_t remoteTimeDelta_t) {
                this.m_Store = remoteTimeDelta_t;
                return this;
            }
        }.Init(remoteTimeDelta_t));
        return remoteTimeDelta_t;
    }

    public static RemoteTimeDelta_t GetConnectionTimeDelta(IStreamingBootStrap iStreamingBootStrap, long j) {
        return GetConnectionTimeDelta(iStreamingBootStrap, j, false);
    }

    public static boolean DownloadDirectory(IStreamingBootStrap iStreamingBootStrap, String str, String str2, boolean z, long j, IRecursiveDownloadFilter iRecursiveDownloadFilter) {
        boolean z2;
        IStreamingBootStrap iStreamingBootStrap2 = iStreamingBootStrap;
        String str3 = str;
        long j2 = j;
        if (iStreamingBootStrap2 != null) {
            RemoteTimeDelta_t remoteTimeDelta_t = null;
            if (z) {
                remoteTimeDelta_t = GetConnectionTimeDelta(iStreamingBootStrap2, j2, true);
            }
            IStreamingBootStrap iStreamingBootStrap3 = iStreamingBootStrap;
            String str4 = str;
            String str5 = str2;
            RecursiveDownload recursiveDownload = r0;
            RecursiveDownload recursiveDownload2 = new RecursiveDownload(iStreamingBootStrap3, GetSyncState(iStreamingBootStrap), str4, str5, new String(""), remoteTimeDelta_t, null, iRecursiveDownloadFilter, null);
            if (recursiveDownload.Start() == 1) {
                long currentTimeMillis = System.currentTimeMillis();
                while (true) {
                    iStreamingBootStrap2.ProcessIncomingData(0);
                    if (recursiveDownload.LastMadeProgress() > currentTimeMillis) {
                        currentTimeMillis = recursiveDownload.LastMadeProgress();
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    if (recursiveDownload.HasSucceeded()) {
                        return true;
                    }
                    String str6 = "Download of \"";
                    String str7 = "com.valvesoftware.BootStrapClient.DownloadDirectory";
                    if (recursiveDownload.HasAborted()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(str6);
                        sb.append(str3);
                        sb.append("\" aborted");
                        Log.i(str7, sb.toString());
                        break;
                    } else if (System.currentTimeMillis() - currentTimeMillis > j2) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(str6);
                        sb2.append(str3);
                        sb2.append("\" timed out");
                        Log.i(str7, sb2.toString());
                        break;
                    } else {
                        RecursiveDownload recursiveDownload3 = recursiveDownload;
                        if (!z2) {
                            Thread.yield();
                        }
                        recursiveDownload = recursiveDownload3;
                    }
                }
                RecursiveDownload recursiveDownload4 = recursiveDownload;
                recursiveDownload4.PrintStatus("\t");
                recursiveDownload4.OnParentAbort();
                return false;
            }
        }
        return false;
    }

    public static boolean[] DownloadFiles(IStreamingBootStrap iStreamingBootStrap, FileDownloadEntry_t[] fileDownloadEntry_tArr, boolean z, long j) {
        String str;
        IStreamingBootStrap iStreamingBootStrap2 = iStreamingBootStrap;
        FileDownloadEntry_t[] fileDownloadEntry_tArr2 = fileDownloadEntry_tArr;
        long j2 = j;
        boolean[] zArr = new boolean[fileDownloadEntry_tArr2.length];
        for (int i = 0; i < zArr.length; i++) {
            zArr[i] = false;
        }
        if (iStreamingBootStrap2 != null) {
            RemoteTimeDelta_t GetConnectionTimeDelta = z ? GetConnectionTimeDelta(iStreamingBootStrap2, j2, true) : null;
            StreamingFileDownload[] streamingFileDownloadArr = new StreamingFileDownload[fileDownloadEntry_tArr2.length];
            int i2 = 0;
            while (i2 < fileDownloadEntry_tArr2.length) {
                int i3 = i2;
                StreamingFileDownload[] streamingFileDownloadArr2 = streamingFileDownloadArr;
                StreamingFileDownload streamingFileDownload = new StreamingFileDownload(iStreamingBootStrap, fileDownloadEntry_tArr2[i2].sRemotePath, fileDownloadEntry_tArr2[i2].sLocalDestPath, GetConnectionTimeDelta, fileDownloadEntry_tArr2[i2].nSetAttributes, null, null);
                streamingFileDownloadArr2[i3] = streamingFileDownload;
                streamingFileDownloadArr2[i3].Start();
                i2 = i3 + 1;
                streamingFileDownloadArr = streamingFileDownloadArr2;
            }
            StreamingFileDownload[] streamingFileDownloadArr3 = streamingFileDownloadArr;
            long currentTimeMillis = System.currentTimeMillis();
            while (true) {
                iStreamingBootStrap2.ProcessIncomingData(0);
                long j3 = currentTimeMillis;
                int i4 = 0;
                boolean z2 = false;
                while (true) {
                    str = "com.valvesoftware.BootStrapClient";
                    if (i4 >= streamingFileDownloadArr3.length) {
                        break;
                    }
                    StreamingFileDownload streamingFileDownload2 = streamingFileDownloadArr3[i4];
                    if (streamingFileDownload2 != null) {
                        if (streamingFileDownload2.LastMadeProgress() > j3) {
                            j3 = streamingFileDownload2.LastMadeProgress();
                        }
                        boolean HasSucceeded = streamingFileDownload2.HasSucceeded();
                        if (HasSucceeded || streamingFileDownload2.HasAborted()) {
                            zArr[i4] = HasSucceeded;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Download finished for \"");
                            sb.append(streamingFileDownload2.m_sRemotePath);
                            sb.append("\" with status ");
                            sb.append(HasSucceeded);
                            sb.append(" / ");
                            sb.append(streamingFileDownload2.m_nRequestStatus);
                            Log.i(str, sb.toString());
                            streamingFileDownloadArr3[i4] = null;
                        } else {
                            z2 = true;
                        }
                    }
                    i4++;
                }
                if (!z2) {
                    break;
                } else if (System.currentTimeMillis() - j3 > j2) {
                    Log.i(str, "Timed out");
                    break;
                } else {
                    Thread.yield();
                    currentTimeMillis = j3;
                }
            }
        }
        return zArr;
    }

    public static boolean CanNetworkOnCurrentThread() {
        return Looper.getMainLooper().getThread() != Thread.currentThread();
    }

    public static String GetAttributeValue_Wait(IStreamingBootStrap iStreamingBootStrap, String str) {
        AnonymousClass1AttributeResult r0 = new IResponseHandler_Attribute() {
            public boolean m_bFinished = false;
            public String m_sResult = null;

            public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
                this.m_bFinished = true;
            }

            public void OnGetAttributeValueResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str) {
                if (i == 0) {
                    this.m_sResult = str;
                }
                this.m_bFinished = true;
            }
        };
        iStreamingBootStrap.GetAttributeValue(str, r0);
        while (true) {
            iStreamingBootStrap.ProcessIncomingData(0);
            if (r0.m_bFinished) {
                return r0.m_sResult;
            }
            Thread.yield();
        }
    }

    /* JADX WARNING: Can't wrap try/catch for region: R(7:39|40|41|42|43|44|45) */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:42:0x0127 */
    /* JADX WARNING: Missing exception handler attribute for start block: B:44:0x012a */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x00ad A[SYNTHETIC, Splitter:B:17:0x00ad] */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00fd  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0139  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected static org.json.JSONObject GetSyncState(com.valvesoftware.IStreamingBootStrap r12) {
        /*
            org.json.JSONObject r0 = m_SyncState
            java.lang.String r1 = "paths"
            if (r0 != 0) goto L_0x0143
            java.io.File r0 = new java.io.File
            java.io.File r2 = com.valvesoftware.JNI_Environment.GetPrivatePath()
            java.lang.String r3 = "/.BootStrapClientSync"
            r0.<init>(r2, r3)
            boolean r2 = r0.exists()
            r3 = 0
            r4 = 1
            java.lang.String r5 = "Sync State \""
            java.lang.String r6 = "com.valvesoftware.BootStrapClient.GetSyncState"
            r7 = 0
            if (r2 == 0) goto L_0x0075
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r5)
            java.lang.String r8 = r0.getPath()
            r2.append(r8)
            java.lang.String r8 = "\" exists"
            r2.append(r8)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r6, r2)
            long r8 = r0.length()
            int r2 = (int) r8
            byte[] r8 = new byte[r2]
            java.io.FileInputStream r9 = new java.io.FileInputStream     // Catch:{ Throwable -> 0x0051 }
            java.lang.String r10 = r0.getPath()     // Catch:{ Throwable -> 0x0051 }
            r9.<init>(r10)     // Catch:{ Throwable -> 0x0051 }
            int r9 = r9.read(r8, r7, r2)     // Catch:{ Throwable -> 0x0051 }
            if (r9 != r2) goto L_0x0051
            r2 = 1
            goto L_0x0052
        L_0x0051:
            r2 = 0
        L_0x0052:
            if (r2 == 0) goto L_0x0090
            java.lang.String r2 = new java.lang.String
            r2.<init>(r8)
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r5)
            java.lang.String r9 = r0.getPath()
            r8.append(r9)
            java.lang.String r9 = "\" was read"
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            android.util.Log.i(r6, r8)
            goto L_0x0091
        L_0x0075:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r5)
            java.lang.String r8 = r0.getPath()
            r2.append(r8)
            java.lang.String r8 = "\" does not exist"
            r2.append(r8)
            java.lang.String r2 = r2.toString()
            android.util.Log.i(r6, r2)
        L_0x0090:
            r2 = r3
        L_0x0091:
            java.lang.String[] r8 = new java.lang.String[]{r3}
            com.valvesoftware.BootStrapClient$5 r9 = new com.valvesoftware.BootStrapClient$5
            r9.<init>()
            com.valvesoftware.IStreamingBootStrap$IAccessContextCallback r9 = r9.Init(r8, r12)
            java.lang.String r10 = "BootStrapClient.RemoteSourceID"
            r12.AccessContext(r10, r9)
            r12 = r8[r7]
            java.lang.String r8 = "remote id"
            java.lang.String r9 = "meta"
            java.lang.String r10 = "version"
            if (r2 == 0) goto L_0x00f9
            org.json.JSONObject r11 = new org.json.JSONObject     // Catch:{ Throwable -> 0x00cf }
            r11.<init>(r2)     // Catch:{ Throwable -> 0x00cf }
            m_SyncState = r11     // Catch:{ Throwable -> 0x00cf }
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x00cf }
            r2.<init>()     // Catch:{ Throwable -> 0x00cf }
            r2.append(r5)     // Catch:{ Throwable -> 0x00cf }
            java.lang.String r11 = r0.getPath()     // Catch:{ Throwable -> 0x00cf }
            r2.append(r11)     // Catch:{ Throwable -> 0x00cf }
            java.lang.String r11 = "\" created from file data"
            r2.append(r11)     // Catch:{ Throwable -> 0x00cf }
            java.lang.String r2 = r2.toString()     // Catch:{ Throwable -> 0x00cf }
            android.util.Log.i(r6, r2)     // Catch:{ Throwable -> 0x00cf }
        L_0x00cf:
            org.json.JSONObject r2 = m_SyncState
            if (r2 == 0) goto L_0x00f9
            org.json.JSONObject r2 = r2.optJSONObject(r9)
            if (r2 == 0) goto L_0x00f5
            r11 = -1
            int r11 = r2.optInt(r10, r11)
            if (r11 == r4) goto L_0x00e2
            r11 = 0
            goto L_0x00e3
        L_0x00e2:
            r11 = 1
        L_0x00e3:
            java.lang.String r2 = r2.optString(r8)
            if (r11 == 0) goto L_0x00f4
            if (r2 == 0) goto L_0x00f5
            if (r12 == 0) goto L_0x00f5
            int r2 = r2.compareTo(r12)
            if (r2 == 0) goto L_0x00f4
            goto L_0x00f5
        L_0x00f4:
            r7 = r11
        L_0x00f5:
            if (r7 != 0) goto L_0x00f9
            m_SyncState = r3
        L_0x00f9:
            org.json.JSONObject r2 = m_SyncState
            if (r2 != 0) goto L_0x0131
            org.json.JSONObject r2 = new org.json.JSONObject
            r2.<init>()
            m_SyncState = r2
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r5)
            java.lang.String r0 = r0.getPath()
            r2.append(r0)
            java.lang.String r0 = "\" created empty"
            r2.append(r0)
            java.lang.String r0 = r2.toString()
            android.util.Log.i(r6, r0)
            org.json.JSONObject r0 = new org.json.JSONObject
            r0.<init>()
            r0.put(r10, r4)     // Catch:{ Throwable -> 0x0127 }
        L_0x0127:
            r0.put(r8, r12)     // Catch:{ Throwable -> 0x012a }
        L_0x012a:
            org.json.JSONObject r12 = m_SyncState     // Catch:{ Throwable -> 0x0130 }
            r12.put(r9, r0)     // Catch:{ Throwable -> 0x0130 }
            goto L_0x0131
        L_0x0130:
        L_0x0131:
            org.json.JSONObject r12 = m_SyncState
            org.json.JSONObject r12 = r12.optJSONObject(r1)
            if (r12 != 0) goto L_0x0143
            org.json.JSONObject r12 = new org.json.JSONObject
            r12.<init>()
            org.json.JSONObject r0 = m_SyncState     // Catch:{ Throwable -> 0x0143 }
            r0.put(r1, r12)     // Catch:{ Throwable -> 0x0143 }
        L_0x0143:
            org.json.JSONObject r12 = m_SyncState
            org.json.JSONObject r12 = r12.optJSONObject(r1)
            return r12
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.BootStrapClient.GetSyncState(com.valvesoftware.IStreamingBootStrap):org.json.JSONObject");
    }

    protected static void SaveSyncState(boolean z) {
        String str;
        long currentTimeMillis = System.currentTimeMillis();
        if (z) {
            boolean z2 = s_nLastSyncSaveTime == 0;
            if (currentTimeMillis - s_nLastSyncSaveTime >= 60000) {
                s_nLastSyncSaveTime = currentTimeMillis;
                if (z2) {
                    return;
                }
            } else {
                return;
            }
        } else {
            s_nLastSyncSaveTime = currentTimeMillis;
        }
        JSONObject jSONObject = m_SyncState;
        if (jSONObject != null && jSONObject.length() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(JNI_Environment.GetPrivatePath());
            sb.append("/.BootStrapClientSync");
            File file = new File(sb.toString());
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fileOutputStream = null;
            try {
                str = m_SyncState.toString(4);
            } catch (Throwable unused) {
                str = null;
            }
            if (str != null) {
                try {
                    fileOutputStream = new FileOutputStream(file.getPath(), false);
                } catch (Throwable unused2) {
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.write(str.getBytes());
                        fileOutputStream.close();
                    } catch (Throwable unused3) {
                    }
                }
            }
        }
    }

    public static void Assert(boolean z) {
        if (!z) {
            throw new AssertionError("com.valvesoftware.BootStrapClient.Assert() failed");
        }
    }
}
