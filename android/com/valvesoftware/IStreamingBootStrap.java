package com.valvesoftware;

import android.util.Log;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface IStreamingBootStrap {
    public static final int AttributeOp_Delete = 2;
    public static final int AttributeOp_GetFlags = 3;
    public static final int AttributeOp_GetValue = 0;
    public static final int AttributeOp_ModifyFlags = 4;
    public static final int AttributeOp_SetValue = 1;
    public static final int AttributeSpace_Count = 2;
    public static final int AttributeSpace_Receiver = 0;
    public static final int AttributeSpace_Sender = 1;
    public static final int AttributeStatus_GENERAL_FAILURE = 2;
    public static final int AttributeStatus_LAST = 3;
    public static final int AttributeStatus_NotFound = 3;
    public static final int AttributeStatus_OK = 0;
    public static final int AttributeStatus_UNIMPLEMENTED = 1;
    public static final int BOOTSTRAP_PROTOCOL_VERSION = 4;
    public static final int CERF_ChannelUnreadable = 1;
    public static final int CERF_DroppedPacket = 0;
    public static final int CERF_FullShutdown = 1073741824;
    public static final int CERF_SharedStreamChannelsUnreadable = 2;
    public static final int CERF_StreamUnreadable = 4;
    public static final int CERF_StreamUnwritable = 8;
    public static final int ERROR_STATE = -1;
    public static final int FSOS_GENERAL_FAILURE = 2;
    public static final int FSOS_LAST = 3;
    public static final int FSOS_OK = 0;
    public static final int FSOS_PATH_ERROR = 3;
    public static final int FSOS_UNIMPLEMENTED = 1;
    public static final int FSQF_ATTRIBUTES = 1;
    public static final int FSQF_CRC = 8;
    public static final int FSQF_LAST_MODIFIED_TIMESTAMP = 4;
    public static final int FSQF_NONE = 0;
    public static final int FSQF_SIZE = 2;
    public static final int FSSFF_APPEND = 1;
    public static final int FULLY_CONNECTED = 0;
    public static final int FULLY_DISCONNECTED = 3;
    public static final int FileSystemOp_CHMOD = 2;
    public static final int FileSystemOp_Delete = 3;
    public static final int FileSystemOp_List = 1;
    public static final int FileSystemOp_Query = 0;
    public static final int FileSystemOp_Retrieve = 5;
    public static final int FileSystemOp_Store = 4;
    public static final long INT64_MAX = Long.MAX_VALUE;
    public static final long INT64_MIN = Long.MIN_VALUE;
    public static final int ImplementationChannelsStart = 128;
    public static final Charset NETWORK_STRING_CHARSET = StandardCharsets.UTF_8;
    public static final int RECEIVED_GOODBYE = 1;
    public static final int ReportOp_ChannelError = 1;
    public static final int ReportOp_Goodbye = 2;
    public static final int ReportOp_ProtocolAssertionFailure = 0;
    public static final int Requests = 0;
    public static final int Responses = 1;
    public static final int SENT_GOODBYE = 2;
    public static final int SO_AttributeOperations = 1;
    public static final int SO_ChannelOperations = 4;
    public static final int SO_FileSystemOperations = 2;
    public static final int SO_Ping = 0;
    public static final int SO_Report = 5;
    public static final int SO_StreamOperations = 3;
    public static final int SO_TunnelOperations = 6;
    public static final int STATUS_EXPANDED_START = 3;
    public static final int STATUS_GENERAL_FAILURE = 2;
    public static final int STATUS_OK = 0;
    public static final int STATUS_UNIMPLEMENTED = 1;
    public static final int Space_Count = 2;
    public static final int Space_Local = 0;
    public static final int Space_Remote = 1;
    public static final int StandardChannelCount = 2;
    public static final int StreamEndStatus_Close_Serial = 1;
    public static final int StreamEndStatus_Interrupted = 2;
    public static final int StreamOp_Cancel = 0;
    public static final int StreamOp_PauseResume = 1;
    public static final int Stream_Chunk = 1;
    public static final int Stream_Close_By_Handle = 2;
    public static final int Stream_Close_By_Serial = 3;
    public static final int Stream_Start = 0;
    public static final int Streams = 2;
    public static final int TCS_DENIED_BY_HANDLER = 3;
    public static final int TCS_GENERAL_FAILURE = 2;
    public static final int TCS_LAST = 4;
    public static final int TCS_OK = 0;
    public static final int TCS_TARGET_UNAVAILABLE = 4;
    public static final int TCS_UNIMPLEMENTED = 1;
    public static final int TES_DENIED_VERSION_MISMATCH = 3;
    public static final int TES_GENERAL_FAILURE = 2;
    public static final int TES_OK = 0;
    public static final int TES_UNIMPLEMENTED = 1;
    public static final int TIMEOUT_INFINITE = Integer.MIN_VALUE;
    public static final int TLS_BINDING_UNAVAILABLE = 4;
    public static final int TLS_DENIED_BY_HANDLER = 3;
    public static final int TLS_GENERAL_FAILURE = 2;
    public static final int TLS_LAST = 4;
    public static final int TLS_OK = 0;
    public static final int TLS_UNIMPLEMENTED = 1;
    public static final int TP_TCP = 0;
    public static final int TP_UDP = 1;
    public static final int TunnelOp_Connect = 0;
    public static final int TunnelOp_Embed = 2;
    public static final int TunnelOp_Listen = 1;
    public static final int VarInt64_Max_Bytes = 9;
    public static final int sizeof_AttributeOperations_t = 1;
    public static final int sizeof_AttributeSpace_t = 1;
    public static final int sizeof_AttributeStatus_t = 1;
    public static final int sizeof_ChannelErrorRecoveryFlags_t = 1;
    public static final int sizeof_ConnectionSpace_t = 1;
    public static final int sizeof_ConnectionState_t = 1;
    public static final int sizeof_FileSystemOperationStatus_t = 1;
    public static final int sizeof_FileSystemOperations_t = 1;
    public static final int sizeof_FileSystemQueryFlags_t = 1;
    public static final int sizeof_ReportOperations_t = 1;
    public static final int sizeof_StandardChannels_t = 1;
    public static final int sizeof_StandardOperations_t = 1;
    public static final int sizeof_StandardStatus_t = 1;
    public static final int sizeof_StreamChannelOperations_t = 1;
    public static final int sizeof_StreamEndStatus_t = 1;
    public static final int sizeof_StreamOperations_t = 1;
    public static final int sizeof_TunnelConnectStatus_t = 1;
    public static final int sizeof_TunnelEmbedStatus_t = 1;
    public static final int sizeof_TunnelListenStatus_t = 1;
    public static final int sizeof_TunnelOperations_t = 1;
    public static final int sizeof_TunnelProtocol_t = 1;
    public static final int sizeof_uint16 = 2;
    public static final int sizeof_uint32 = 4;
    public static final int sizeof_uint64 = 8;
    public static final int sizeof_uint8 = 1;

    public static class CBufferingStreamHandler extends IStreamHandler {
        private Lock m_Mutex = new ReentrantLock();
        private boolean m_bSerialClosed = false;
        private boolean m_bTrackPacketSizes;
        private int m_nPacketBytesRemaining = 0;
        private int m_nReceiveRead = 0;
        private int m_nReceiveWrite = 0;
        private byte[] m_receiveMemory = new byte[1024];

        public CBufferingStreamHandler(boolean z) {
            this.m_bTrackPacketSizes = z;
        }

        public boolean IsSerialClosed() {
            return this.m_bSerialClosed;
        }

        public boolean WaitForStreamData(int i) {
            this.m_Mutex.lock();
            if (this.m_nReceiveRead != this.m_nReceiveWrite) {
                this.m_Mutex.unlock();
                return true;
            } else if (this.m_bSerialClosed) {
                FreeBuffer(true);
                this.m_Mutex.unlock();
                return false;
            } else {
                if (i == 0) {
                    this.m_Mutex.unlock();
                } else {
                    long currentTimeMillis = i == -1 ? -1 : System.currentTimeMillis() + ((long) i);
                    do {
                        this.m_Mutex.unlock();
                        Thread.yield();
                        this.m_Mutex.lock();
                        if (this.m_nReceiveRead != this.m_nReceiveWrite) {
                            this.m_Mutex.unlock();
                            return true;
                        } else if (this.m_bSerialClosed) {
                            FreeBuffer(true);
                            this.m_Mutex.unlock();
                            return false;
                        }
                    } while (currentTimeMillis >= System.currentTimeMillis());
                }
                this.m_Mutex.unlock();
                return false;
            }
        }

        public int ReadBufferedData(byte[] bArr, int i, int[] iArr) {
            this.m_Mutex.lock();
            int i2 = this.m_nReceiveWrite;
            int i3 = this.m_nReceiveRead;
            int i4 = i2 - i3;
            if (i4 > 0) {
                if (this.m_bTrackPacketSizes) {
                    if (this.m_nPacketBytesRemaining == 0 && i4 > 4) {
                        this.m_nPacketBytesRemaining = (int) IStreamingBootStrapIO.ReadUINT32(this.m_receiveMemory, i3);
                        this.m_nReceiveRead += 4;
                        i4 -= 4;
                    }
                    int i5 = this.m_nPacketBytesRemaining;
                    if (i5 < i4) {
                        i4 = i5;
                    }
                }
                int min = Math.min(bArr.length - i, i4);
                System.arraycopy(this.m_receiveMemory, this.m_nReceiveRead, bArr, i, min);
                int i6 = this.m_nReceiveWrite;
                int i7 = this.m_nReceiveRead;
                if (min == i6 - i7) {
                    this.m_nReceiveRead = 0;
                    this.m_nReceiveWrite = 0;
                } else {
                    this.m_nReceiveRead = i7 + min;
                }
                if (this.m_bTrackPacketSizes) {
                    this.m_nPacketBytesRemaining -= min;
                }
                this.m_Mutex.unlock();
                if (iArr != null) {
                    iArr[0] = this.m_nPacketBytesRemaining;
                }
                return min;
            }
            if (this.m_bSerialClosed) {
                FreeBuffer(true);
            }
            this.m_Mutex.unlock();
            return 0;
        }

        public void OnStreamEnd(boolean z, boolean z2, String str) {
            this.m_Mutex.lock();
            if (z) {
                this.m_bSerialClosed = true;
                FreeBuffer(true);
            }
            this.m_Mutex.unlock();
        }

        public boolean HandleStreamChunk(byte[] bArr, int i, int i2) {
            int i3 = (this.m_bTrackPacketSizes ? 4 : 0) + i2;
            this.m_Mutex.lock();
            byte[] bArr2 = this.m_receiveMemory;
            int length = bArr2.length;
            int i4 = this.m_nReceiveWrite;
            if (length - i4 < i3) {
                int i5 = i4 - this.m_nReceiveRead;
                if (bArr2.length - i5 < i3) {
                    byte[] bArr3 = new byte[StaticHelpers.RoundUpToPowerOfTwo(i5 + i3)];
                    byte[] bArr4 = this.m_receiveMemory;
                    int i6 = this.m_nReceiveRead;
                    System.arraycopy(bArr4, i6, bArr3, i6, i5);
                    this.m_receiveMemory = bArr3;
                    this.m_receiveMemory = bArr3;
                }
                byte[] bArr5 = this.m_receiveMemory;
                if (bArr5.length - this.m_nReceiveWrite < i3) {
                    System.arraycopy(bArr5, this.m_nReceiveRead, bArr5, 0, i5);
                    this.m_nReceiveWrite -= this.m_nReceiveRead;
                    this.m_nReceiveRead = 0;
                }
            }
            if (this.m_bTrackPacketSizes) {
                IStreamingBootStrapIO.WriteUINT32ToBuffer((long) i2, this.m_receiveMemory, this.m_nReceiveWrite);
                this.m_nReceiveWrite += 4;
            }
            System.arraycopy(bArr, i, this.m_receiveMemory, this.m_nReceiveWrite, i2);
            this.m_nReceiveWrite += i2;
            this.m_Mutex.unlock();
            return true;
        }

        private void FreeBuffer(boolean z) {
            if (this.m_receiveMemory == null) {
                return;
            }
            if (this.m_nReceiveWrite == 0 || !z) {
                this.m_receiveMemory = null;
                this.m_nReceiveRead = 0;
                this.m_nReceiveWrite = 0;
            }
        }
    }

    public static abstract class CEmbeddedIOImpl_Base extends IStreamingBootStrapIOImpl {
        private CBufferingStreamHandler m_ReceiveStream = null;
        private byte[] m_SendMemory;
        private boolean m_bAutoPumpInput;
        private int m_nSendWrite;

        public abstract boolean OnFullPacketBuffered(byte[] bArr, int i, int i2);

        /* access modifiers changed from: protected */
        public abstract void PumpIncomingStreamOwnerConnection(int i);

        public CEmbeddedIOImpl_Base(boolean z) {
            this.m_bAutoPumpInput = z;
            this.m_SendMemory = new byte[1024];
            this.m_nSendWrite = 0;
        }

        public void SetReceiveStream(CBufferingStreamHandler cBufferingStreamHandler) {
            this.m_ReceiveStream = cBufferingStreamHandler;
        }

        public void OnPacketStart(long j) {
            int length = this.m_SendMemory.length;
            int i = this.m_nSendWrite;
            if (((long) (length - i)) < j) {
                byte[] bArr = new byte[StaticHelpers.RoundUpToPowerOfTwo(i + ((int) j))];
                System.arraycopy(this.m_SendMemory, 0, bArr, 0, this.m_nSendWrite);
                this.m_SendMemory = bArr;
            }
        }

        public void WriteRaw(byte[] bArr, int i, int i2) {
            StaticHelpers.Assert(i2 <= this.m_SendMemory.length - this.m_nSendWrite);
            System.arraycopy(bArr, i, this.m_SendMemory, this.m_nSendWrite, i2);
            this.m_nSendWrite += i2;
        }

        public void OnPacketEnd() {
            if (OnFullPacketBuffered(this.m_SendMemory, 0, this.m_nSendWrite)) {
                this.m_nSendWrite = 0;
            }
        }

        public boolean WaitForData(int i) {
            CBufferingStreamHandler cBufferingStreamHandler = this.m_ReceiveStream;
            if (cBufferingStreamHandler == null) {
                return false;
            }
            if (!this.m_bAutoPumpInput) {
                return cBufferingStreamHandler.WaitForStreamData(i);
            }
            if (cBufferingStreamHandler.WaitForStreamData(0)) {
                return true;
            }
            PumpIncomingStreamOwnerConnection(i);
            return this.m_ReceiveStream.WaitForStreamData(0);
        }

        public long ReadData(byte[] bArr, int i) {
            if (this.m_ReceiveStream == null) {
                return 0;
            }
            if (this.m_bAutoPumpInput) {
                PumpIncomingStreamOwnerConnection(0);
            }
            return (long) this.m_ReceiveStream.ReadBufferedData(bArr, i, null);
        }
    }

    public static class CJavaStreamIOImpl extends CEmbeddedIOImpl_Base {
        protected CBufferingStreamHandler m_InputStream = new CBufferingStreamHandler(false);
        protected IStreamingBootStrap m_jog_OuterConnection;
        protected IStreamHandler m_jog_SendStream = null;

        public CJavaStreamIOImpl(IStreamingBootStrap iStreamingBootStrap, IStreamHandler iStreamHandler, long j) {
            super(true);
            this.m_jog_OuterConnection = iStreamingBootStrap;
            this.m_jog_SendStream = iStreamHandler;
            SetReceiveStream(this.m_InputStream);
            StaticHelpers.Assert(this.m_jog_OuterConnection.RegisterIncomingStreamHandler(j, this.m_InputStream));
        }

        public void finalize() {
            CloseOutgoingStream(false, null);
            StaticHelpers.Assert(this.m_jog_OuterConnection.UnregisterIncomingStreamHandler(this.m_InputStream));
            SetReceiveStream(null);
        }

        public void CloseOutgoingStream(boolean z, String str) {
            IStreamHandler iStreamHandler = this.m_jog_SendStream;
            if (iStreamHandler != null) {
                iStreamHandler.OnStreamEnd(true, z, str);
                this.m_jog_SendStream = null;
            }
        }

        public boolean OnFullPacketBuffered(byte[] bArr, int i, int i2) {
            IStreamHandler iStreamHandler = this.m_jog_SendStream;
            if (iStreamHandler != null && !iStreamHandler.HandleStreamChunk(bArr, i, i2)) {
                CloseOutgoingStream(true, "Outgoing stream handler requested closure");
            }
            return true;
        }

        public void OwnerInstanceDestructing(IStreamingBootStrap iStreamingBootStrap) {
            CloseOutgoingStream(false, null);
        }

        /* access modifiers changed from: protected */
        public void PumpIncomingStreamOwnerConnection(int i) {
            this.m_jog_OuterConnection.ProcessIncomingData(i);
        }
    }

    public static class FileSystemDirectoryListEntry_t extends FileSystemQueryResult_t {
        public String sName;
    }

    public static class FileSystemQueryResult_t {
        public static final int DIRECTORY = 1;
        public static final int EXECUTABLE = 8;
        public static final int NONE = 0;
        public static final int READABLE = 2;
        public static final int WRITABLE = 4;
        public static final int sizeof_Attributes_t = 1;
        public int nAttributeFlags;
        public int nCRC;
        public long nFileSize;
        public long nLastModifiedSecondsSinceEpoch;
        public int nSetFields;

        public void Clear() {
            this.nSetFields = 0;
            this.nAttributeFlags = 0;
            this.nLastModifiedSecondsSinceEpoch = 0;
            this.nFileSize = 0;
            this.nCRC = 0;
        }
    }

    public interface IAccessContextCallback {
        Object OnAccessContext(String str, Object obj);
    }

    public static class IRequestHandler_Attribute {
        public int DeleteAttribute(IStreamingBootStrap iStreamingBootStrap, int i, String str) {
            return 1;
        }

        public int GetAttributeFlags(IStreamingBootStrap iStreamingBootStrap, String str, int[] iArr) {
            return 1;
        }

        public int GetAttributeValue(IStreamingBootStrap iStreamingBootStrap, String str, StringBuilder sb) {
            return 1;
        }

        public int ModifyAttributeFlags(IStreamingBootStrap iStreamingBootStrap, String str, int i, int i2) {
            return 1;
        }

        public int SetAttributeValue(IStreamingBootStrap iStreamingBootStrap, int i, String str, String str2) {
            return 1;
        }
    }

    public static abstract class IRequestHandler_FileSystem {

        public interface IDirectoryListCallback {
            void AddResult(FileSystemDirectoryListEntry_t fileSystemDirectoryListEntry_t);
        }

        public int DeleteFileOrDirectory(IStreamingBootStrap iStreamingBootStrap, String str) {
            return 1;
        }

        public int ListDirectory(IStreamingBootStrap iStreamingBootStrap, String str, int i, IDirectoryListCallback iDirectoryListCallback) {
            return 1;
        }

        public int QueryFile(IStreamingBootStrap iStreamingBootStrap, String str, int i, FileSystemQueryResult_t fileSystemQueryResult_t) {
            return 1;
        }

        public int RetrieveFile(IStreamingBootStrap iStreamingBootStrap, String str, long[] jArr) {
            return 1;
        }

        public int StoreFile(IStreamingBootStrap iStreamingBootStrap, String str, int i, long j) {
            return 1;
        }
    }

    public static abstract class IRequestHandler_Ping {
        public abstract long GetLocalTimeMicroSeconds(IStreamingBootStrap iStreamingBootStrap);
    }

    public static class IRequestHandler_Report {
        public void AssertionFailure(IStreamingBootStrap iStreamingBootStrap, String str) {
        }

        public void ChannelError(IStreamingBootStrap iStreamingBootStrap, int i, long j, int i2) {
        }

        public void Goodbye(IStreamingBootStrap iStreamingBootStrap, String str) {
        }
    }

    public static class IRequestHandler_Tunnel {

        public interface IListenRequestCallback {

            public static class ComplexOutput_t {
                long nOutgoingStreamHandle;
                String sFailureReasonOnFail;
            }

            boolean ShouldAcceptConnection(long j, String[] strArr, long j2, long j3, ComplexOutput_t complexOutput_t);
        }

        public interface IListenRequestResponseCallback {
            void OnListenRequestResponse(long j, boolean z, long j2, String str);
        }

        public int Connect(IStreamingBootStrap iStreamingBootStrap, int i, String str, long j, long j2, TunnelResponse_t tunnelResponse_t) {
            return 1;
        }

        public int Embed(IStreamingBootStrap iStreamingBootStrap, String str, long j, String str2, long j2, long j3, TunnelEmbedResponse_t tunnelEmbedResponse_t) {
            return 1;
        }

        public int Listen(IStreamingBootStrap iStreamingBootStrap, int i, String str, long j, long j2, TunnelResponse_t tunnelResponse_t) {
            return 1;
        }

        public static void EncodeListenRequest(IStreamHandler iStreamHandler, long j, String[] strArr, long j2, long j3) {
            int i = 0;
            for (String SBS_strlen : strArr) {
                i += StaticHelpers.SBS_strlen(SBS_strlen) + 1;
            }
            byte[] bArr = new byte[(i + 36)];
            int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j, bArr, 0) + 0;
            int EncodeUVarInt2 = EncodeUVarInt + StaticHelpers.EncodeUVarInt((long) r0, bArr, EncodeUVarInt);
            for (String SBS_WriteStringToBuffer : strArr) {
                EncodeUVarInt2 += StaticHelpers.SBS_WriteStringToBuffer(SBS_WriteStringToBuffer, bArr, EncodeUVarInt2);
            }
            int EncodeUVarInt3 = EncodeUVarInt2 + StaticHelpers.EncodeUVarInt(j2, bArr, EncodeUVarInt2);
            iStreamHandler.HandleStreamChunk(bArr, 0, EncodeUVarInt3 + StaticHelpers.EncodeUVarInt(j3, bArr, EncodeUVarInt3));
        }

        /* JADX WARNING: Code restructure failed: missing block: B:3:0x001c, code lost:
            r21 = r6;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public static int DecodeListenRequests(byte[] r23, int r24, int r25, com.valvesoftware.IStreamingBootStrap.IRequestHandler_Tunnel.IListenRequestCallback r26, com.valvesoftware.IStreamingBootStrap.IStreamHandler r27) {
            /*
                r0 = r23
                r1 = r25
                r2 = r27
                r3 = 1
                long[] r4 = new long[r3]
                r5 = 1024(0x400, float:1.435E-42)
                byte[] r6 = new byte[r5]
                r7 = 32
                java.lang.String[] r7 = new java.lang.String[r7]
                r8 = 0
                r9 = r7
                r10 = 0
                r7 = r24
            L_0x0016:
                int r11 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r7, r1, r4)
                if (r11 != 0) goto L_0x0020
            L_0x001c:
                r21 = r6
            L_0x001e:
                r9 = 0
                goto L_0x006b
            L_0x0020:
                r13 = r4[r8]
                int r11 = r11 + r7
                int r12 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r11, r1, r4)
                if (r12 != 0) goto L_0x002a
                goto L_0x001c
            L_0x002a:
                r21 = r6
                r5 = r4[r8]
                int r11 = r11 + r12
                int r12 = r9.length
                r15 = r9
                long r8 = (long) r12
                int r12 = (r5 > r8 ? 1 : (r5 == r8 ? 0 : -1))
                if (r12 <= 0) goto L_0x003c
                int r8 = (int) r5
                java.lang.String[] r8 = new java.lang.String[r8]
                r22 = r4
                goto L_0x003f
            L_0x003c:
                r22 = r4
                r8 = r15
            L_0x003f:
                r3 = 0
                r15 = r11
                r11 = r3
            L_0x0043:
                int r16 = (r11 > r5 ? 1 : (r11 == r5 ? 0 : -1))
                if (r16 >= 0) goto L_0x0055
                int r9 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r15, r1)
                if (r9 != r1) goto L_0x004e
                goto L_0x0055
            L_0x004e:
                int r15 = r9 + 1
                r16 = 1
                long r11 = r11 + r16
                goto L_0x0043
            L_0x0055:
                if (r15 != r1) goto L_0x0058
            L_0x0057:
                goto L_0x001e
            L_0x0058:
                r5 = r22
                int r6 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r15, r1, r5)
                if (r6 != 0) goto L_0x0061
                goto L_0x0057
            L_0x0061:
                r9 = 0
                r16 = r5[r9]
                int r15 = r15 + r6
                int r6 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r15, r1, r5)
                if (r6 != 0) goto L_0x006e
            L_0x006b:
                r11 = r21
                goto L_0x00d9
            L_0x006e:
                r18 = r5[r9]
                int r7 = r15 + r6
                com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel$IListenRequestCallback$ComplexOutput_t r6 = new com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel$IListenRequestCallback$ComplexOutput_t
                r6.<init>()
                r6.nOutgoingStreamHandle = r3
                java.lang.String r3 = ""
                r6.sFailureReasonOnFail = r3
                r12 = r26
                r3 = r13
                r15 = r8
                r20 = r6
                boolean r9 = r12.ShouldAcceptConnection(r13, r15, r16, r18, r20)
                if (r9 == 0) goto L_0x00b0
                r9 = 1024(0x400, float:1.435E-42)
                int r11 = 1024 - r10
                r9 = 19
                if (r11 >= r9) goto L_0x0099
                r11 = r21
                r9 = 0
                r2.HandleStreamChunk(r11, r9, r10)
                r10 = 0
                goto L_0x009c
            L_0x0099:
                r11 = r21
                r9 = 0
            L_0x009c:
                int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r3, r11, r10)
                int r10 = r10 + r3
                r11[r10] = r9
                r9 = 1
                int r10 = r10 + r9
                long r3 = r6.nOutgoingStreamHandle
                int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r3, r11, r10)
                int r10 = r10 + r3
                r3 = 1
                r13 = 1024(0x400, float:1.435E-42)
                goto L_0x00d7
            L_0x00b0:
                r11 = r21
                r9 = 1
                java.lang.String r12 = r6.sFailureReasonOnFail
                int r12 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strlen(r12)
                int r12 = r12 + r9
                r13 = 1024(0x400, float:1.435E-42)
                int r14 = 1024 - r10
                int r15 = r12 + 10
                if (r14 >= r15) goto L_0x00c7
                r14 = 0
                r2.HandleStreamChunk(r11, r14, r10)
                r10 = 0
            L_0x00c7:
                int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r3, r11, r10)
                int r10 = r10 + r3
                r3 = 2
                r11[r10] = r3
                r3 = 1
                int r10 = r10 + r3
                java.lang.String r4 = r6.sFailureReasonOnFail
                com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_WriteStringToBuffer(r4, r11, r10)
                int r10 = r10 + r12
            L_0x00d7:
                if (r7 < r1) goto L_0x00e2
            L_0x00d9:
                if (r10 == 0) goto L_0x00df
                r4 = 0
                r2.HandleStreamChunk(r11, r4, r10)
            L_0x00df:
                int r7 = r7 - r24
                return r7
            L_0x00e2:
                r4 = r5
                r9 = r8
                r6 = r11
                r5 = 1024(0x400, float:1.435E-42)
                r8 = 0
                goto L_0x0016
            */
            throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.IStreamingBootStrap.IRequestHandler_Tunnel.DecodeListenRequests(byte[], int, int, com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel$IListenRequestCallback, com.valvesoftware.IStreamingBootStrap$IStreamHandler):int");
        }

        public static int DecodeListenRequestResponse(byte[] bArr, int i, int i2, IListenRequestResponseCallback iListenRequestResponseCallback) {
            String str;
            long[] jArr = new long[1];
            int i3 = i;
            do {
                int DecodeUVarInt = StaticHelpers.DecodeUVarInt(bArr, i3, i2, jArr);
                if (DecodeUVarInt == 0) {
                    break;
                }
                long j = jArr[0];
                int i4 = DecodeUVarInt + i3;
                if (i4 == i2) {
                    break;
                }
                byte b = bArr[i4];
                int i5 = i4 + 1;
                if (b == 0) {
                    int DecodeUVarInt2 = StaticHelpers.DecodeUVarInt(bArr, i5, i2, jArr);
                    if (DecodeUVarInt2 == 0) {
                        break;
                    }
                    i5 += DecodeUVarInt2;
                    iListenRequestResponseCallback.OnListenRequestResponse(j, true, jArr[0], null);
                } else {
                    if (b > 1) {
                        int SBS_strend = StaticHelpers.SBS_strend(bArr, i5, i2);
                        if (SBS_strend == i2) {
                            break;
                        }
                        String str2 = new String(bArr, i5, SBS_strend - i5, IStreamingBootStrap.NETWORK_STRING_CHARSET);
                        i5 = SBS_strend + 1;
                        str = str2;
                    } else {
                        str = null;
                    }
                    iListenRequestResponseCallback.OnListenRequestResponse(j, false, 0, str);
                }
                i3 = i5;
            } while (i3 < i2);
            return i3 - i;
        }
    }

    public static abstract class IResponseHandler_Attribute implements IResponseHandler_Base {
        public void OnDeleteAttributeResponse(IStreamingBootStrap iStreamingBootStrap, int i) {
        }

        public void OnGetAttributeFlagsResponse(IStreamingBootStrap iStreamingBootStrap, int i, int i2) {
        }

        public void OnGetAttributeValueResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str) {
        }

        public void OnModifyAttributeFlagsResponse(IStreamingBootStrap iStreamingBootStrap, int i) {
        }

        public void OnSetAttributeValueResponse(IStreamingBootStrap iStreamingBootStrap, int i) {
        }
    }

    public interface IResponseHandler_Base {
        void ResponseAborted(IStreamingBootStrap iStreamingBootStrap);
    }

    public static abstract class IResponseHandler_FileSystem implements IResponseHandler_Base {
        public void OnDeleteFileOrDirectoryResponse(IStreamingBootStrap iStreamingBootStrap, int i) {
        }

        public void OnListDirectoryResponse(IStreamingBootStrap iStreamingBootStrap, int i, FileSystemDirectoryListEntry_t[] fileSystemDirectoryListEntry_tArr) {
        }

        public void OnQueryFileResponse(IStreamingBootStrap iStreamingBootStrap, int i, FileSystemQueryResult_t fileSystemQueryResult_t) {
        }

        public void OnRetrieveFileResponse(IStreamingBootStrap iStreamingBootStrap, int i, long j) {
        }

        public void OnStoreFileResponse(IStreamingBootStrap iStreamingBootStrap, int i, IStreamHandler iStreamHandler) {
        }
    }

    public static abstract class IResponseHandler_Ping implements IResponseHandler_Base {
        public abstract void OnPingResponse(IStreamingBootStrap iStreamingBootStrap, long j);
    }

    public static abstract class IResponseHandler_Tunnel implements IResponseHandler_Base {
        public void OnConnectResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j) {
        }

        public void OnEmbedResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, long j, String str2, IStreamHandler iStreamHandler, long j2) {
        }

        public void OnListenResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j) {
        }
    }

    public static abstract class IStreamHandler {
        protected int m_nFlags = 0;
        protected long m_nStreamHandle = Long.MIN_VALUE;
        protected long m_nStreamSerial = Long.MIN_VALUE;

        public abstract boolean HandleStreamChunk(byte[] bArr, int i, int i2);

        public void OnStreamEnd(boolean z, boolean z2, String str) {
        }

        public void OnStreamStart(long j) {
        }

        public long GetStreamSerial() {
            return this.m_nStreamSerial;
        }
    }

    public interface IStreamingBootStrapDataHandler {
        long HandleBootstrapData(IStreamingBootStrap iStreamingBootStrap, int i, byte[] bArr, int i2, int i3, long j, long j2);
    }

    public static abstract class IStreamingBootStrapIO {
        public abstract long ReadData(byte[] bArr, int i);

        public abstract boolean WaitForData(int i);

        public abstract void WriteRaw(byte[] bArr, int i, int i2);

        public void WriteRaw(byte[] bArr) {
            try {
                WriteRaw(bArr, 0, bArr.length);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public static int GetStringPayloadSize(String str) {
            return StaticHelpers.SBS_strlen(str) + 1;
        }

        public void WriteString(String str) {
            try {
                WriteRaw(str.getBytes(IStreamingBootStrap.NETWORK_STRING_CHARSET));
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
            WriteUINT8(0);
        }

        public static void WriteUINT8ToBuffer(int i, byte[] bArr, int i2) {
            bArr[i2 + 0] = (byte) i;
        }

        public static void WriteUINT16ToBuffer(int i, byte[] bArr, int i2) {
            bArr[i2 + 0] = (byte) (i >> 8);
            bArr[i2 + 1] = (byte) i;
        }

        public static void WriteUINT32ToBuffer(long j, byte[] bArr, int i) {
            bArr[i + 0] = (byte) ((int) (j >> 24));
            bArr[i + 1] = (byte) ((int) (j >> 16));
            bArr[i + 2] = (byte) ((int) (j >> 8));
            bArr[i + 3] = (byte) ((int) j);
        }

        public static void WriteUINT64ToBuffer(long j, byte[] bArr, int i) {
            bArr[i + 0] = (byte) ((int) (j >> 56));
            bArr[i + 1] = (byte) ((int) (j >> 48));
            bArr[i + 2] = (byte) ((int) (j >> 40));
            bArr[i + 3] = (byte) ((int) (j >> 32));
            bArr[i + 4] = (byte) ((int) (j >> 24));
            bArr[i + 5] = (byte) ((int) (j >> 16));
            bArr[i + 6] = (byte) ((int) (j >> 8));
            bArr[i + 7] = (byte) ((int) j);
        }

        public void WriteUINT8(int i) {
            try {
                WriteRaw(new byte[]{(byte) i}, 0, 1);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public void WriteUINT16(int i) {
            byte[] bArr = new byte[2];
            WriteUINT16ToBuffer(i, bArr, 0);
            try {
                WriteRaw(bArr, 0, 2);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public void WriteUINT32(long j) {
            byte[] bArr = new byte[4];
            WriteUINT32ToBuffer(j, bArr, 0);
            try {
                WriteRaw(bArr, 0, 4);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public void WriteUINT64(long j) {
            byte[] bArr = new byte[8];
            WriteUINT64ToBuffer(j, bArr, 0);
            try {
                WriteRaw(bArr, 0, 8);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public static long ReadUINT8(byte[] bArr, int i) {
            return (long) (bArr[i + 0] & 255);
        }

        public static long ReadUINT16(byte[] bArr, int i) {
            return ((long) (bArr[i + 1] & 255)) | ((long) ((bArr[i + 0] & 255) << 8));
        }

        public static long ReadUINT32(byte[] bArr, int i) {
            return ((long) (bArr[i + 3] & 255)) | (((long) (bArr[i + 0] & 255)) << 24) | (((long) (bArr[i + 1] & 255)) << 16) | (((long) (bArr[i + 2] & 255)) << 8);
        }

        public static long ReadUINT64(byte[] bArr, int i) {
            return ((long) (bArr[i + 7] & 255)) | (((long) (bArr[i + 0] & 255)) << 56) | (((long) (bArr[i + 1] & 255)) << 48) | (((long) (bArr[i + 2] & 255)) << 40) | (((long) (bArr[i + 3] & 255)) << 32) | (((long) (bArr[i + 4] & 255)) << 24) | (((long) (bArr[i + 5] & 255)) << 16) | (((long) (bArr[i + 6] & 255)) << 8);
        }
    }

    public static abstract class IStreamingBootStrapIOImpl extends IStreamingBootStrapIO {
        public static final int IOCSF_CANNOT_READ = 2;
        public static final int IOCSF_CANNOT_WRITE = 8;
        public static final int IOCSF_CAN_READ = 1;
        public static final int IOCSF_CAN_WRITE = 4;

        public int GetConnectionState() {
            return 5;
        }

        public void OnPacketEnd() {
        }

        public void OnPacketStart(long j) {
        }

        public void OwnerInstanceDestructing(IStreamingBootStrap iStreamingBootStrap) {
        }

        public void SendKeepAlivePacket() {
            byte[] bArr = new byte[9];
            int EncodeUVarInt = StaticHelpers.EncodeUVarInt(0, bArr, 0);
            OnPacketStart((long) EncodeUVarInt);
            WriteRaw(bArr, 0, EncodeUVarInt);
            OnPacketEnd();
        }
    }

    public static class NativeStreamHandler extends IStreamHandler {
        private long m_nativePointer;

        private static native boolean nativeHandleStreamChunk(long j, byte[] bArr, int i, int i2);

        private static native void nativeOnStreamEnd(long j, boolean z, boolean z2, String str);

        private static native void nativeOnStreamStart(long j, long j2);

        public NativeStreamHandler(long j) {
            this.m_nativePointer = j;
        }

        public void OnStreamStart(long j) {
            nativeOnStreamStart(this.m_nativePointer, j);
        }

        public void OnStreamEnd(boolean z, boolean z2, String str) {
            nativeOnStreamEnd(this.m_nativePointer, z, z2, str);
        }

        public boolean HandleStreamChunk(byte[] bArr, int i, int i2) {
            return nativeHandleStreamChunk(this.m_nativePointer, bArr, i, i2);
        }
    }

    public static class ResponseHandler_Native_Tunnel extends IResponseHandler_Tunnel {
        private long m_nativePointer;

        private static native void nativeOnConnectResponse(long j, IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j2);

        private static native void nativeOnEmbedResponse(long j, IStreamingBootStrap iStreamingBootStrap, int i, String str, long j2, String str2, IStreamHandler iStreamHandler, long j3);

        private static native void nativeOnListenResponse(long j, IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j2);

        private static native void nativeResponseAborted(long j, IStreamingBootStrap iStreamingBootStrap);

        public ResponseHandler_Native_Tunnel(long j) {
            this.m_nativePointer = j;
        }

        public void ResponseAborted(IStreamingBootStrap iStreamingBootStrap) {
            nativeResponseAborted(this.m_nativePointer, iStreamingBootStrap);
        }

        public void OnConnectResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j) {
            nativeOnConnectResponse(this.m_nativePointer, iStreamingBootStrap, i, str, iStreamHandler, j);
        }

        public void OnListenResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, IStreamHandler iStreamHandler, long j) {
            nativeOnListenResponse(this.m_nativePointer, iStreamingBootStrap, i, str, iStreamHandler, j);
        }

        public void OnEmbedResponse(IStreamingBootStrap iStreamingBootStrap, int i, String str, long j, String str2, IStreamHandler iStreamHandler, long j2) {
            nativeOnEmbedResponse(this.m_nativePointer, iStreamingBootStrap, i, str, j, str2, iStreamHandler, j2);
        }
    }

    public static class StaticHelpers {
        public static IStreamingBootStrap m_PrimaryConnection;

        public static int RoundUpToPowerOfTwo(int i) {
            if (((i - 1) & i) != 0) {
                int i2 = i << 2;
                i = i2 & (i2 - 1);
                while (true) {
                    int i3 = (i - 1) & i;
                    if (i3 == 0) {
                        break;
                    }
                    i = i3;
                }
            }
            return i;
        }

        public static int EncodeUVarInt(long j, byte[] bArr, int i) {
            long j2 = j;
            int i2 = i;
            do {
                bArr[i2] = (byte) ((int) ((127 & j2) | 128));
                i2++;
                j2 >>= 7;
            } while (j2 != 0);
            int i3 = i2 - 1;
            bArr[i3] = (byte) (bArr[i3] & Byte.MAX_VALUE);
            return i2 - i;
        }

        public static int DecodeUVarInt(byte[] bArr, int i, int i2, long[] jArr) {
            long j = 0;
            int i3 = 0;
            int i4 = i;
            while (i4 < i2) {
                Assert(i4 < i2);
                j |= ((long) (bArr[i4] & Byte.MAX_VALUE)) << i3;
                if ((bArr[i4] & 128) != 0) {
                    i3 += 7;
                    i4++;
                } else {
                    jArr[0] = j;
                    return (i4 - i) + 1;
                }
            }
            return 0;
        }

        public static int EncodeSVarInt(long j, byte[] bArr, int i) {
            long j2 = j < 0 ? ((-j) << 1) | 1 : j << 1;
            int i2 = i;
            do {
                bArr[i2] = (byte) ((int) ((127 & j2) | 128));
                i2++;
                j2 >>= 7;
            } while (j2 != 0);
            int i3 = i2 - 1;
            bArr[i3] = (byte) (bArr[i3] & Byte.MAX_VALUE);
            return i2 - i;
        }

        public static int DecodeSVarInt(byte[] bArr, int i, int i2, long[] jArr) {
            int i3 = i;
            long j = 0;
            int i4 = 0;
            while (i3 < i2) {
                Assert(i3 < i2);
                j |= ((long) (bArr[i3] & Byte.MAX_VALUE)) << i4;
                if ((bArr[i3] & 128) != 0) {
                    i4 += 7;
                    i3++;
                } else {
                    jArr[0] = (1 & j) != 0 ? -((j >> 1) & IStreamingBootStrap.INT64_MAX) : (j >> 1) & IStreamingBootStrap.INT64_MAX;
                    return (i3 - i) + 1;
                }
            }
            return 0;
        }

        public static int SBS_strlen(String str) {
            return str.getBytes(IStreamingBootStrap.NETWORK_STRING_CHARSET).length;
        }

        public static int SBS_strend(byte[] bArr, int i, int i2) {
            while (i != i2 && bArr[i] != 0) {
                i++;
            }
            return i;
        }

        public static int SBS_WriteStringToBuffer(String str, byte[] bArr, int i) {
            byte[] bytes = str.getBytes(IStreamingBootStrap.NETWORK_STRING_CHARSET);
            int length = bytes.length;
            System.arraycopy(bytes, 0, bArr, i, length);
            bArr[i + length] = 0;
            return length + 1;
        }

        public static void CaughtExternalCodeException(Throwable th) {
            StringBuilder sb = new StringBuilder();
            sb.append("External code exception \"");
            sb.append(th.getMessage());
            sb.append("\"");
            Log.i("com.valvesoftware.IStreamingBootStrap", sb.toString());
            th.printStackTrace();
        }

        public static void Assert(boolean z) {
            if (!z) {
                throw new AssertionError("IStreamingBootStrap.StaticHelpers.Assert() failed");
            }
        }

        public static void SetPrimaryJavaConnection(IStreamingBootStrap iStreamingBootStrap) {
            m_PrimaryConnection = iStreamingBootStrap;
        }
    }

    public static class TunnelEmbedResponse_t extends TunnelResponse_t {
        long nHandlerProtocol = 0;
        String sResponseHandlerIdentifier = null;
    }

    public static class TunnelResponse_t {
        long nSendStreamSerial = Long.MIN_VALUE;
        String sFailureReasonOnFail = null;
    }

    void AccessContext(String str, IAccessContextCallback iAccessContextCallback);

    IStreamHandler CreateSendStream();

    void DeleteFileOrDirectory(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem);

    void GetAttributeValue(String str, IResponseHandler_Attribute iResponseHandler_Attribute);

    int GetConnectionState();

    int GetIOHandlerConnectionState();

    long GetIncomingPacketID(int i);

    long GetOutgoingPacketID(int i);

    IRequestHandler_Attribute GetRequestHandler_Attribute();

    IRequestHandler_FileSystem GetRequestHandler_FileSystem();

    IRequestHandler_Ping GetRequestHandler_Ping();

    IRequestHandler_Report GetRequestHandler_Report();

    IRequestHandler_Tunnel GetRequestHandler_Tunnel();

    boolean IsWaitingForProcessing();

    long LastReceivedDataMS();

    long LastSentdDataMS();

    void ListDirectory(String str, int i, IResponseHandler_FileSystem iResponseHandler_FileSystem);

    void PacketCompleted();

    IStreamingBootStrapIO PacketStart(int i, long j);

    boolean ProcessIncomingData(int i);

    void QueryFile(String str, int i, IResponseHandler_FileSystem iResponseHandler_FileSystem);

    boolean RegisterIncomingStreamHandler(long j, IStreamHandler iStreamHandler);

    void ReportAssertionFailure(String str);

    void ReportDroppedPacket(int i, long j, int i2);

    void RetrieveFile(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem);

    void SendGoodbye(String str);

    void SendKeepAlivePacket(long j);

    void SendPing(IResponseHandler_Ping iResponseHandler_Ping);

    void SetAttributeValue(String str, int i, String str2, IResponseHandler_Attribute iResponseHandler_Attribute);

    IRequestHandler_Attribute SetRequestHandler_Attribute(IRequestHandler_Attribute iRequestHandler_Attribute);

    IRequestHandler_FileSystem SetRequestHandler_FileSystem(IRequestHandler_FileSystem iRequestHandler_FileSystem);

    IRequestHandler_Ping SetRequestHandler_Ping(IRequestHandler_Ping iRequestHandler_Ping);

    IRequestHandler_Report SetRequestHandler_Report(IRequestHandler_Report iRequestHandler_Report);

    IRequestHandler_Tunnel SetRequestHandler_Tunnel(IRequestHandler_Tunnel iRequestHandler_Tunnel);

    IStreamHandler StoreFile(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem);

    void Tunnel_Connect(int i, String str, long j, IResponseHandler_Tunnel iResponseHandler_Tunnel);

    void Tunnel_EmbedConnection(String str, long j, String str2, long j2, IResponseHandler_Tunnel iResponseHandler_Tunnel);

    void Tunnel_Listen(int i, String str, long j, IResponseHandler_Tunnel iResponseHandler_Tunnel);

    boolean UnregisterIncomingStreamHandler(IStreamHandler iStreamHandler);
}
