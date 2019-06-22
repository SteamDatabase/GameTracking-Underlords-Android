package com.valvesoftware;

import android.util.Log;
import com.valvesoftware.IStreamingBootStrap.FileSystemQueryResult_t;
import com.valvesoftware.IStreamingBootStrap.IAccessContextCallback;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_Attribute;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_FileSystem;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_Ping;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_Report;
import com.valvesoftware.IStreamingBootStrap.IRequestHandler_Tunnel;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Base;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Ping;
import com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel;
import com.valvesoftware.IStreamingBootStrap.IStreamHandler;
import com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapDataHandler;
import com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO;
import com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIOImpl;
import com.valvesoftware.IStreamingBootStrap.StaticHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StreamingBootStrap_JavaImpl implements IStreamingBootStrap {
    public static final int BOOTSTRAP_PROTOCOL_MINIMUM_REMOTE_VERSION = 4;
    public static final int MAX_READ_BUFFER_SIZE = 8388608;
    public static final int MaxChannelCount = 256;
    public static final int SH_BoundToHandle = 2;
    public static final int SH_RegisteredForSerial = 1;
    private long[][] m_ChannelPacketCounters = ((long[][]) Array.newInstance(long.class, new int[]{2, 256}));
    private Vector<ContextData_t> m_ContextData = new Vector<>();
    private Lock m_ContextDataMutex = new ReentrantLock();
    private IStreamingBootStrapIOImpl m_IOHandler;
    private Lock m_IOReadMutex = new ReentrantLock();
    private Lock m_IOWriteMutex = new ReentrantLock();
    private IStreamingBootStrapDataHandler[] m_ImplementationHandlers = new IStreamingBootStrapDataHandler[128];
    private Vector<IStreamHandler> m_IncomingStreamsByHandle = new Vector<>();
    private Vector<IStreamHandler> m_IncomingStreamsBySerial = new Vector<>();
    private ResponseBase_t m_MidResponse = null;
    private Vector<WeakReference<COutgoingStreamImp>> m_OutgoingStreamsByHandle = new Vector<>();
    private Vector<WeakReference<COutgoingStreamImp>> m_OutgoingStreamsBySerial = new Vector<>();
    private long[] m_PreviousResponseID = new long[2];
    private byte[] m_ReadBuffer = null;
    private Lock m_RequestHandlerMutex = new ReentrantLock();
    private IRequestHandler_Attribute m_RequestHandler_Attribute = null;
    private IRequestHandler_FileSystem m_RequestHandler_FileSystem = null;
    private IRequestHandler_Ping m_RequestHandler_Ping = null;
    private IRequestHandler_Report m_RequestHandler_Report = null;
    private IRequestHandler_Tunnel m_RequestHandler_Tunnel = null;
    private ConcurrentLinkedQueue<ResponseBase_t> m_ResponseQueue = new ConcurrentLinkedQueue<>();
    private ReadWriteLock m_StreamHandlerRWLock = new ReentrantReadWriteLock();
    private boolean m_bDebug;
    private boolean m_bDiscardWaitingBytes = false;
    private boolean m_bErrorState = false;
    private boolean m_bOutgoingStreamsEverOverriddenOrWrapped = false;
    private boolean m_bReceivedGoodbye = false;
    private boolean m_bSentGoodbye = false;
    private long m_nLastReceivedDataMS = 0;
    private long m_nLastSentDataMS = 0;
    private int m_nMidPacketChannel = 256;
    private long m_nMidPacketOffset = 0;
    private long m_nMidPacketSize = 0;
    private int m_nMidResponseHeaderBytes = 0;
    private int m_nOutgoingPacketChannel = 255;
    private AtomicLong m_nOutgoingStreamSerialIncrementer = new AtomicLong(0);
    private int m_nReadBufferPut = 0;
    private int m_nRemoteProtocolVersion;
    private long m_nWaitingOnInputBytes = 1;

    private static class AttributeResponse_t extends ResponseBase_t {
        int m_nSpace;
        int m_nSubOperation;

        private AttributeResponse_t() {
            super();
        }
    }

    private static class CDebugBootstrapIOHandler extends IStreamingBootStrapIOImpl {
        public IStreamingBootStrapIOImpl m_RealHandler;
        public boolean m_bSentGoodbye = false;
        public boolean m_bWritable = false;
        public long m_nByteCountdown = 0;

        CDebugBootstrapIOHandler(IStreamingBootStrapIOImpl iStreamingBootStrapIOImpl) {
            this.m_RealHandler = iStreamingBootStrapIOImpl;
        }

        public void WriteRaw(byte[] bArr, int i, int i2) {
            StreamingBootStrap_JavaImpl.Assert(this.m_bWritable);
            long j = (long) i2;
            StreamingBootStrap_JavaImpl.Assert(this.m_nByteCountdown >= j);
            this.m_nByteCountdown -= j;
            try {
                this.m_RealHandler.WriteRaw(bArr, i, i2);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public boolean WaitForData(int i) {
            try {
                return this.m_RealHandler.WaitForData(i);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
                return false;
            }
        }

        public long ReadData(byte[] bArr, int i) {
            try {
                return this.m_RealHandler.ReadData(bArr, i);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
                return 0;
            }
        }

        public void OnPacketStart(long j) {
            StreamingBootStrap_JavaImpl.Assert(!this.m_bSentGoodbye);
            StreamingBootStrap_JavaImpl.Assert(!this.m_bWritable);
            this.m_nByteCountdown = j;
            this.m_bWritable = true;
            try {
                this.m_RealHandler.OnPacketStart(j);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }

        public void OnPacketEnd() {
            StreamingBootStrap_JavaImpl.Assert(this.m_bWritable);
            StreamingBootStrap_JavaImpl.Assert(this.m_nByteCountdown == 0);
            try {
                this.m_RealHandler.OnPacketEnd();
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
            this.m_bWritable = false;
        }

        public int GetConnectionState() {
            return this.m_RealHandler.GetConnectionState();
        }

        public void OwnerInstanceDestructing(IStreamingBootStrap iStreamingBootStrap) {
            this.m_RealHandler.OwnerInstanceDestructing(iStreamingBootStrap);
        }
    }

    private static class CFileSystemQueryReplyEncoder {
        public byte[] m_Buffer = new byte[19];
        public int m_nWrittenBytes;

        public void EncodeResult(FileSystemQueryResult_t fileSystemQueryResult_t) {
            this.m_Buffer[0] = (byte) fileSystemQueryResult_t.nSetFields;
            int i = 1;
            if ((fileSystemQueryResult_t.nSetFields & 1) != 0) {
                this.m_Buffer[1] = (byte) fileSystemQueryResult_t.nAttributeFlags;
                i = 2;
            }
            if ((fileSystemQueryResult_t.nSetFields & 2) != 0) {
                i += StaticHelpers.EncodeUVarInt(fileSystemQueryResult_t.nFileSize, this.m_Buffer, i);
            }
            if ((fileSystemQueryResult_t.nSetFields & 4) != 0) {
                IStreamingBootStrapIO.WriteUINT32ToBuffer(fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch, this.m_Buffer, i);
                i += 4;
            }
            if ((fileSystemQueryResult_t.nSetFields & 8) != 0) {
                IStreamingBootStrapIO.WriteUINT32ToBuffer((long) fileSystemQueryResult_t.nCRC, this.m_Buffer, i);
                i += 4;
            }
            this.m_nWrittenBytes = i;
        }

        public static int DecodeResult(byte[] bArr, int i, int i2, FileSystemQueryResult_t fileSystemQueryResult_t) {
            if (i == i2) {
                return -1;
            }
            fileSystemQueryResult_t.nSetFields = bArr[i];
            int i3 = i + 1;
            if ((fileSystemQueryResult_t.nSetFields & 1) != 0) {
                if (i3 == i2) {
                    return -1;
                }
                fileSystemQueryResult_t.nAttributeFlags = bArr[i3];
                i3++;
            }
            if ((fileSystemQueryResult_t.nSetFields & 2) != 0) {
                long[] jArr = new long[1];
                int DecodeUVarInt = StaticHelpers.DecodeUVarInt(bArr, i3, i2, jArr);
                if (DecodeUVarInt == 0) {
                    return -1;
                }
                fileSystemQueryResult_t.nFileSize = jArr[0];
                i3 += DecodeUVarInt;
            }
            if ((fileSystemQueryResult_t.nSetFields & 4) != 0) {
                int i4 = i2 - i3;
                if (i4 < 4) {
                    return i4 - 4;
                }
                fileSystemQueryResult_t.nLastModifiedSecondsSinceEpoch = IStreamingBootStrapIO.ReadUINT32(bArr, i3);
                i3 += 4;
            }
            if ((fileSystemQueryResult_t.nSetFields & 8) != 0) {
                int i5 = i2 - i3;
                if (i5 < 4) {
                    return i5 - 4;
                }
                fileSystemQueryResult_t.nCRC = (int) IStreamingBootStrapIO.ReadUINT32(bArr, i3);
                i3 += 4;
            }
            return i3 - i;
        }
    }

    private static class COutgoingStreamImp extends IStreamHandler {
        public StreamingBootStrap_JavaImpl m_Owner;

        public COutgoingStreamImp(StreamingBootStrap_JavaImpl streamingBootStrap_JavaImpl) {
            this.m_Owner = streamingBootStrap_JavaImpl;
        }

        public void finalize() {
            if ((this.m_nFlags & 3) != 0) {
                OnStreamEnd(true, true, "Sender interface abandoned");
            }
        }

        public void ConnectionShuttingDown() {
            this.m_nFlags &= -4;
            this.m_Owner = null;
        }

        public void OnStreamStart(long j) {
            StreamingBootStrap_JavaImpl.Assert(this.m_Owner != null);
            if (this.m_Owner != null) {
                if ((this.m_nFlags & 2) != 0) {
                    StreamingBootStrap_JavaImpl.Assert(false);
                    return;
                }
                this.m_Owner.OutgoingStreamBindToHandle(this);
                byte[] bArr = new byte[9];
                int EncodeUVarInt = StaticHelpers.EncodeUVarInt(this.m_nStreamHandle, bArr, 0);
                byte[] bArr2 = new byte[9];
                int EncodeUVarInt2 = StaticHelpers.EncodeUVarInt(this.m_nStreamSerial, bArr2, 0);
                byte[] bArr3 = new byte[9];
                int EncodeSVarInt = StaticHelpers.EncodeSVarInt(j, bArr3, 0);
                IStreamingBootStrapIO PacketStart = this.m_Owner.PacketStart(2, (long) (EncodeUVarInt + 1 + EncodeUVarInt2 + EncodeSVarInt));
                try {
                    PacketStart.WriteUINT8(0);
                    PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
                    PacketStart.WriteRaw(bArr2, 0, EncodeUVarInt2);
                    PacketStart.WriteRaw(bArr3, 0, EncodeSVarInt);
                } catch (RuntimeException e) {
                    StaticHelpers.CaughtExternalCodeException(e);
                }
                this.m_Owner.PacketCompleted();
            }
        }

        public void OnStreamEnd(boolean z, boolean z2, String str) {
            if (this.m_Owner == null || ((this.m_nFlags & 2) == 0 && ((this.m_nFlags & 1) == 0 || !z))) {
                StreamingBootStrap_JavaImpl.Assert(false);
                return;
            }
            SendStreamEnd(this.m_Owner, (this.m_nFlags & 2) != 0, z, this.m_nStreamSerial, this.m_nStreamHandle, z2, str);
            if ((this.m_nFlags & 2) != 0) {
                this.m_Owner.OutgoingStreamUnbindHandle(this);
            }
            if (z) {
                this.m_Owner.OutgoingStreamClosed(this);
                this.m_Owner = null;
            }
        }

        public static void SendStreamEnd(StreamingBootStrap_JavaImpl streamingBootStrap_JavaImpl, boolean z, boolean z2, long j, long j2, boolean z3, String str) {
            byte[] bArr = new byte[9];
            if (z) {
                j = j2;
            }
            int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j, bArr, 0);
            int i = z2 ? 1 : 0;
            if (z3) {
                i |= 2;
            }
            int i2 = 2;
            IStreamingBootStrapIO PacketStart = streamingBootStrap_JavaImpl.PacketStart(2, (long) (EncodeUVarInt + 1 + 1 + (z3 ? IStreamingBootStrapIO.GetStringPayloadSize(str) : 0)));
            if (!z) {
                i2 = 3;
            }
            try {
                PacketStart.WriteUINT8(i2);
                PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
                PacketStart.WriteUINT8(i);
                if (z3) {
                    PacketStart.WriteString(str);
                }
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
            streamingBootStrap_JavaImpl.PacketCompleted();
        }

        public boolean HandleStreamChunk(byte[] bArr, int i, int i2) {
            if ((this.m_nFlags & 2) == 0) {
                OnStreamStart(-1);
            }
            if (i2 == 0) {
                return true;
            }
            StreamingBootStrap_JavaImpl.Assert((this.m_nFlags & 2) != 0);
            byte[] bArr2 = new byte[9];
            int EncodeUVarInt = StaticHelpers.EncodeUVarInt(this.m_nStreamHandle, bArr2, 0);
            IStreamingBootStrapIO PacketStart = this.m_Owner.PacketStart(2, (long) (EncodeUVarInt + 1 + i2));
            try {
                PacketStart.WriteUINT8(1);
                PacketStart.WriteRaw(bArr2, 0, EncodeUVarInt);
                PacketStart.WriteRaw(bArr, i, i2);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
            this.m_Owner.PacketCompleted();
            return true;
        }
    }

    private static class ContextData_t {
        Object context;
        String sName;

        private ContextData_t() {
        }
    }

    private static class FileSystemResponse_t extends ResponseBase_t {
        IStreamHandler m_OutgoingStream;
        int m_nSubOperation;

        private FileSystemResponse_t() {
            super();
        }
    }

    private static class PingResponse_t extends ResponseBase_t {
        private PingResponse_t() {
            super();
        }
    }

    private static class ResponseBase_t {
        public IResponseHandler_Base m_ResponseHandler;
        public int m_nOperation;
        public long m_nPacketID;

        private ResponseBase_t() {
        }
    }

    private static class TunnelWaitResponse_t extends ResponseBase_t {
        IStreamHandler m_OutgoingStream;
        int m_nSubOperation;

        private TunnelWaitResponse_t() {
            super();
        }
    }

    public void AccessContext(String str, IAccessContextCallback iAccessContextCallback) {
        boolean z;
        Object obj;
        this.m_ContextDataMutex.lock();
        boolean z2 = false;
        int i = 0;
        while (true) {
            if (i >= this.m_ContextData.size()) {
                break;
            }
            int compareTo = ((ContextData_t) this.m_ContextData.elementAt(i)).sName.compareTo(str);
            if (compareTo < 0) {
                i++;
            } else if (compareTo == 0) {
                z = true;
            }
        }
        z = false;
        Object obj2 = z ? ((ContextData_t) this.m_ContextData.elementAt(i)).context : null;
        try {
            obj = iAccessContextCallback.OnAccessContext(str, obj2);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
            obj = obj2;
        }
        if (obj != obj2) {
            if (!z) {
                if (obj2 == null && obj != null) {
                    z2 = true;
                }
                Assert(z2);
                ContextData_t contextData_t = new ContextData_t();
                contextData_t.sName = str;
                contextData_t.context = obj;
                this.m_ContextData.add(i, contextData_t);
            } else if (obj != null) {
                ((ContextData_t) this.m_ContextData.elementAt(i)).context = obj;
            } else {
                this.m_ContextData.removeElementAt(i);
            }
        }
        this.m_ContextDataMutex.unlock();
    }

    public long LastReceivedDataMS() {
        return this.m_nLastReceivedDataMS;
    }

    public long LastSentdDataMS() {
        return this.m_nLastSentDataMS;
    }

    public boolean IsWaitingForProcessing() {
        this.m_IOReadMutex.lock();
        boolean z = false;
        if (this.m_nWaitingOnInputBytes <= ((long) this.m_nReadBufferPut)) {
            z = true;
        } else {
            try {
                z = this.m_IOHandler.WaitForData(0);
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }
        this.m_IOReadMutex.unlock();
        return z;
    }

    /* JADX WARNING: Removed duplicated region for block: B:114:0x016f  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x017e  */
    /* JADX WARNING: Removed duplicated region for block: B:124:0x0176 A[EDGE_INSN: B:124:0x0176->B:118:0x0176 ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0046  */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0068  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0082  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean ProcessIncomingData(int r19) {
        /*
            r18 = this;
            r1 = r18
            r0 = r19
            r2 = -2147483648(0xffffffff80000000, float:-0.0)
            r4 = 0
            if (r0 != r2) goto L_0x000b
            r5 = 1
            goto L_0x000c
        L_0x000b:
            r5 = 0
        L_0x000c:
            if (r0 != 0) goto L_0x0010
            r6 = 1
            goto L_0x0011
        L_0x0010:
            r6 = 0
        L_0x0011:
            r7 = 0
            if (r5 != 0) goto L_0x001f
            if (r6 == 0) goto L_0x0018
            goto L_0x001f
        L_0x0018:
            long r9 = java.lang.System.currentTimeMillis()
            long r11 = (long) r0
            long r9 = r9 + r11
            goto L_0x0020
        L_0x001f:
            r9 = r7
        L_0x0020:
            r0 = 0
            r11 = 0
        L_0x0022:
            boolean r12 = r1.m_bErrorState
            if (r12 == 0) goto L_0x002e
            if (r0 == 0) goto L_0x002e
            java.util.concurrent.locks.Lock r0 = r1.m_IOReadMutex
            r0.unlock()
            r0 = 0
        L_0x002e:
            boolean r12 = r1.m_bErrorState
            if (r12 != 0) goto L_0x0172
            if (r0 != 0) goto L_0x0043
            if (r5 == 0) goto L_0x003d
            java.util.concurrent.locks.Lock r0 = r1.m_IOReadMutex
            r0.lock()
            r12 = 1
            goto L_0x0044
        L_0x003d:
            java.util.concurrent.locks.Lock r0 = r1.m_IOReadMutex
            boolean r0 = r0.tryLock()
        L_0x0043:
            r12 = r0
        L_0x0044:
            if (r12 == 0) goto L_0x016f
            long r13 = r1.m_nWaitingOnInputBytes
            r15 = 8388608(0x800000, float:1.17549435E-38)
            int r0 = (r13 > r7 ? 1 : (r13 == r7 ? 0 : -1))
            if (r0 == 0) goto L_0x00db
            boolean r0 = r1.m_bDiscardWaitingBytes
            if (r0 == 0) goto L_0x0060
            byte[] r0 = r1.m_ReadBuffer
            if (r0 == 0) goto L_0x005e
            int r0 = r0.length
            if (r0 == 0) goto L_0x005e
            int r0 = r1.m_nReadBufferPut
            if (r0 != 0) goto L_0x005e
            goto L_0x0060
        L_0x005e:
            r0 = 0
            goto L_0x0061
        L_0x0060:
            r0 = 1
        L_0x0061:
            Assert(r0)
            if (r6 == 0) goto L_0x0068
            r0 = 0
            goto L_0x0074
        L_0x0068:
            if (r5 == 0) goto L_0x006d
            r0 = -2147483648(0xffffffff80000000, float:-0.0)
            goto L_0x0074
        L_0x006d:
            long r13 = java.lang.System.currentTimeMillis()
            long r13 = r9 - r13
            int r0 = (int) r13
        L_0x0074:
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIOImpl r13 = r1.m_IOHandler     // Catch:{ RuntimeException -> 0x007b }
            boolean r0 = r13.WaitForData(r0)     // Catch:{ RuntimeException -> 0x007b }
            goto L_0x0080
        L_0x007b:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            r0 = 0
        L_0x0080:
            if (r0 == 0) goto L_0x00db
            byte[] r0 = r1.m_ReadBuffer
            if (r0 == 0) goto L_0x0095
            int r13 = r0.length
            long r13 = (long) r13
            long r2 = r1.m_nWaitingOnInputBytes
            int r16 = (r13 > r2 ? 1 : (r13 == r2 ? 0 : -1))
            if (r16 >= 0) goto L_0x00b6
            int r0 = r0.length
            if (r0 >= r15) goto L_0x00b6
            boolean r0 = r1.m_bDiscardWaitingBytes
            if (r0 != 0) goto L_0x00b6
        L_0x0095:
            byte[] r0 = r1.m_ReadBuffer
            if (r0 == 0) goto L_0x009b
            int r0 = r0.length
            goto L_0x009d
        L_0x009b:
            r0 = 8192(0x2000, float:1.14794E-41)
        L_0x009d:
            long r2 = r1.m_nWaitingOnInputBytes
            long r13 = (long) r0
            int r16 = (r2 > r13 ? 1 : (r2 == r13 ? 0 : -1))
            if (r16 <= 0) goto L_0x00a9
            if (r0 >= r15) goto L_0x00a9
            int r0 = r0 << 1
            goto L_0x009d
        L_0x00a9:
            byte[] r0 = new byte[r0]
            int r2 = r1.m_nReadBufferPut
            if (r2 == 0) goto L_0x00b4
            byte[] r3 = r1.m_ReadBuffer
            java.lang.System.arraycopy(r3, r4, r0, r4, r2)
        L_0x00b4:
            r1.m_ReadBuffer = r0
        L_0x00b6:
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIOImpl r0 = r1.m_IOHandler     // Catch:{ RuntimeException -> 0x00c1 }
            byte[] r2 = r1.m_ReadBuffer     // Catch:{ RuntimeException -> 0x00c1 }
            int r3 = r1.m_nReadBufferPut     // Catch:{ RuntimeException -> 0x00c1 }
            long r2 = r0.ReadData(r2, r3)     // Catch:{ RuntimeException -> 0x00c1 }
            goto L_0x00c6
        L_0x00c1:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            r2 = r7
        L_0x00c6:
            int r0 = (r2 > r7 ? 1 : (r2 == r7 ? 0 : -1))
            if (r0 == 0) goto L_0x00cc
            r0 = 1
            goto L_0x00cd
        L_0x00cc:
            r0 = 0
        L_0x00cd:
            r11 = r11 | r0
            long r13 = java.lang.System.currentTimeMillis()
            r1.m_nLastReceivedDataMS = r13
            int r0 = r1.m_nReadBufferPut
            long r13 = (long) r0
            long r13 = r13 + r2
            int r0 = (int) r13
            r1.m_nReadBufferPut = r0
        L_0x00db:
            boolean r0 = r1.m_bDiscardWaitingBytes
            r2 = 1
            if (r0 == 0) goto L_0x00f5
            int r0 = r1.m_nReadBufferPut
            long r13 = (long) r0
            long r7 = r1.m_nWaitingOnInputBytes
            int r0 = (r13 > r7 ? 1 : (r13 == r7 ? 0 : -1))
            if (r0 < 0) goto L_0x00ef
            r1.m_bDiscardWaitingBytes = r4
            r1.m_nWaitingOnInputBytes = r2
            goto L_0x00f3
        L_0x00ef:
            long r7 = r7 - r13
            r1.m_nWaitingOnInputBytes = r7
            r7 = r13
        L_0x00f3:
            r3 = 0
            goto L_0x0135
        L_0x00f5:
            long r7 = r1.m_nWaitingOnInputBytes
            int r0 = r1.m_nReadBufferPut
            long r13 = (long) r0
            int r17 = (r7 > r13 ? 1 : (r7 == r13 ? 0 : -1))
            if (r17 > 0) goto L_0x0132
            byte[] r7 = r1.m_ReadBuffer
            long r7 = r1.TryHandlingWaitingMessage(r7, r4, r0)
            r13 = 0
            int r0 = (r7 > r13 ? 1 : (r7 == r13 ? 0 : -1))
            if (r0 == 0) goto L_0x010c
            r13 = 1
            goto L_0x010d
        L_0x010c:
            r13 = 0
        L_0x010d:
            Assert(r13)
            if (r0 <= 0) goto L_0x0115
            r1.m_nWaitingOnInputBytes = r2
            goto L_0x0130
        L_0x0115:
            int r0 = r1.m_nReadBufferPut
            if (r0 == r15) goto L_0x011b
            r0 = 1
            goto L_0x011c
        L_0x011b:
            r0 = 0
        L_0x011c:
            Assert(r0)
            int r0 = r1.m_nReadBufferPut
            long r2 = (long) r0
            long r2 = r2 - r7
            r1.m_nWaitingOnInputBytes = r2
            long r2 = r1.m_nWaitingOnInputBytes
            r13 = 8388608(0x800000, double:4.144523E-317)
            int r0 = (r2 > r13 ? 1 : (r2 == r13 ? 0 : -1))
            if (r0 <= 0) goto L_0x0130
            r1.m_nWaitingOnInputBytes = r13
        L_0x0130:
            r3 = 1
            goto L_0x0135
        L_0x0132:
            r3 = 0
            r7 = 0
        L_0x0135:
            r13 = 0
            int r0 = (r7 > r13 ? 1 : (r7 == r13 ? 0 : -1))
            if (r0 <= 0) goto L_0x016c
            int r0 = r1.m_nReadBufferPut
            long r13 = (long) r0
            int r2 = (r7 > r13 ? 1 : (r7 == r13 ? 0 : -1))
            if (r2 < 0) goto L_0x015b
            long r13 = (long) r0
            int r0 = (r7 > r13 ? 1 : (r7 == r13 ? 0 : -1))
            if (r0 <= 0) goto L_0x0157
            boolean r0 = r1.m_bDiscardWaitingBytes
            r2 = 1
            r0 = r0 ^ r2
            Assert(r0)
            int r0 = r1.m_nReadBufferPut
            long r13 = (long) r0
            long r7 = r7 - r13
            r1.m_nWaitingOnInputBytes = r7
            r1.m_bDiscardWaitingBytes = r2
            goto L_0x0158
        L_0x0157:
            r2 = 1
        L_0x0158:
            r1.m_nReadBufferPut = r4
            goto L_0x016d
        L_0x015b:
            r2 = 1
            long r13 = (long) r0
            long r13 = r13 - r7
            int r0 = (int) r13
            r1.m_nReadBufferPut = r0
            byte[] r0 = r1.m_ReadBuffer
            int r3 = (int) r7
            int r7 = r1.m_nReadBufferPut
            java.lang.System.arraycopy(r0, r3, r0, r4, r7)
            r0 = r12
            r3 = 1
            goto L_0x0174
        L_0x016c:
            r2 = 1
        L_0x016d:
            r0 = r12
            goto L_0x0174
        L_0x016f:
            r2 = 1
            r0 = r12
            goto L_0x0173
        L_0x0172:
            r2 = 1
        L_0x0173:
            r3 = 0
        L_0x0174:
            if (r6 == 0) goto L_0x017e
            if (r0 == 0) goto L_0x017d
            java.util.concurrent.locks.Lock r0 = r1.m_IOReadMutex
            r0.unlock()
        L_0x017d:
            return r11
        L_0x017e:
            if (r3 != 0) goto L_0x0183
            java.lang.Thread.yield()
        L_0x0183:
            r2 = -2147483648(0xffffffff80000000, float:-0.0)
            r7 = 0
            goto L_0x0022
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.ProcessIncomingData(int):boolean");
    }

    public IStreamingBootStrapIO PacketStart(int i, long j) {
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j + 1, bArr, 0);
        long j2 = j + ((long) EncodeUVarInt) + 1;
        this.m_IOWriteMutex.lock();
        this.m_nOutgoingPacketChannel = i;
        try {
            this.m_IOHandler.OnPacketStart(j2);
            this.m_IOHandler.WriteRaw(bArr, 0, EncodeUVarInt);
            this.m_IOHandler.WriteUINT8(i);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        this.m_nLastSentDataMS = System.currentTimeMillis();
        return this.m_IOHandler;
    }

    public void PacketCompleted() {
        try {
            this.m_IOHandler.OnPacketEnd();
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        long[] jArr = this.m_ChannelPacketCounters[0];
        int i = this.m_nOutgoingPacketChannel;
        jArr[i] = jArr[i] + 1;
        this.m_nLastSentDataMS = System.currentTimeMillis();
        this.m_IOWriteMutex.unlock();
    }

    public long GetOutgoingPacketID(int i) {
        return this.m_ChannelPacketCounters[0][i];
    }

    public long GetIncomingPacketID(int i) {
        return this.m_ChannelPacketCounters[1][i];
    }

    public boolean RegisterIncomingStreamHandler(long j, IStreamHandler iStreamHandler) {
        boolean z = false;
        Assert((iStreamHandler.m_nFlags & 1) == 0);
        if ((iStreamHandler.m_nFlags & 1) != 0) {
            Log.i("com.valvesoftware.StreamingBootStrap_JavaImpl.RegisterIncomingStreamHandler", "Handler already registered for a serial");
            return false;
        }
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        int size = this.m_IncomingStreamsBySerial.size();
        int i = 0;
        while (true) {
            if (i >= this.m_IncomingStreamsBySerial.size()) {
                break;
            }
            int i2 = (((IStreamHandler) this.m_IncomingStreamsBySerial.elementAt(i)).m_nStreamSerial > j ? 1 : (((IStreamHandler) this.m_IncomingStreamsBySerial.elementAt(i)).m_nStreamSerial == j ? 0 : -1));
            if (i2 >= 0) {
                if (i2 > 0) {
                    size = i;
                    break;
                } else if (i2 == 0) {
                    z = true;
                    break;
                }
            }
            i++;
        }
        if (!z) {
            iStreamHandler.m_nStreamSerial = j;
            iStreamHandler.m_nFlags |= 1;
            this.m_IncomingStreamsBySerial.add(size, iStreamHandler);
        }
        writeLock.unlock();
        return !z;
    }

    public boolean UnregisterIncomingStreamHandler(IStreamHandler iStreamHandler) {
        boolean z = true;
        Assert((iStreamHandler.m_nFlags & 1) != 0);
        if ((iStreamHandler.m_nFlags & 1) == 0) {
            return false;
        }
        long j = iStreamHandler.m_nStreamSerial;
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        if ((iStreamHandler.m_nFlags & 2) != 0) {
            IncomingStreamUnbindHandle(iStreamHandler);
        }
        int i = 0;
        while (true) {
            if (i >= this.m_IncomingStreamsBySerial.size()) {
                break;
            }
            IStreamHandler iStreamHandler2 = (IStreamHandler) this.m_IncomingStreamsBySerial.elementAt(i);
            if (iStreamHandler2.m_nStreamSerial < j) {
                i++;
            } else if (iStreamHandler != iStreamHandler2) {
                while (i < this.m_IncomingStreamsBySerial.size()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unfound remaining element [");
                    sb.append(i);
                    sb.append("] serial:");
                    sb.append(((IStreamHandler) this.m_IncomingStreamsBySerial.elementAt(i)).m_nStreamSerial);
                    Log.i("com.valvesoftware.StreamingBootStrap_JavaImpl.UnregisterIncomingStreamHandler", sb.toString());
                    i++;
                }
            }
        }
        i = -1;
        if (i != -1) {
            this.m_IncomingStreamsBySerial.removeElementAt(i);
        }
        iStreamHandler.m_nFlags &= -2;
        iStreamHandler.m_nStreamSerial = IStreamingBootStrap.INT64_MAX;
        writeLock.unlock();
        if (i == -1) {
            z = false;
        }
        return z;
    }

    public IStreamHandler CreateSendStream() {
        return GenerateOutgoingStreamHandler(null);
    }

    public int GetConnectionState() {
        if (this.m_bErrorState) {
            return -1;
        }
        int i = 0;
        if (this.m_bReceivedGoodbye) {
            i = 1;
        }
        if (this.m_bSentGoodbye) {
            i |= 2;
        }
        return i;
    }

    public int GetIOHandlerConnectionState() {
        return this.m_IOHandler.GetConnectionState();
    }

    public void SendKeepAlivePacket(long j) {
        if (j <= 0 || System.currentTimeMillis() - this.m_nLastSentDataMS >= j) {
            this.m_IOWriteMutex.lock();
            this.m_IOHandler.SendKeepAlivePacket();
            this.m_nLastSentDataMS = System.currentTimeMillis();
            this.m_IOWriteMutex.unlock();
        }
    }

    public IRequestHandler_Ping GetRequestHandler_Ping() {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Ping iRequestHandler_Ping = this.m_RequestHandler_Ping;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Ping;
    }

    public IRequestHandler_Ping SetRequestHandler_Ping(IRequestHandler_Ping iRequestHandler_Ping) {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Ping iRequestHandler_Ping2 = this.m_RequestHandler_Ping;
        this.m_RequestHandler_Ping = iRequestHandler_Ping;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Ping2;
    }

    public void SendPing(IResponseHandler_Ping iResponseHandler_Ping) {
        PingResponse_t pingResponse_t = null;
        if (iResponseHandler_Ping != null) {
            PingResponse_t pingResponse_t2 = new PingResponse_t();
            pingResponse_t2.m_ResponseHandler = iResponseHandler_Ping;
            pingResponse_t2.m_nOperation = 0;
            pingResponse_t = pingResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, 1);
        if (pingResponse_t != null) {
            pingResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(pingResponse_t);
        }
        try {
            PacketStart.WriteUINT8(0);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public IRequestHandler_Attribute GetRequestHandler_Attribute() {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Attribute iRequestHandler_Attribute = this.m_RequestHandler_Attribute;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Attribute;
    }

    public IRequestHandler_Attribute SetRequestHandler_Attribute(IRequestHandler_Attribute iRequestHandler_Attribute) {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Attribute iRequestHandler_Attribute2 = this.m_RequestHandler_Attribute;
        this.m_RequestHandler_Attribute = iRequestHandler_Attribute;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Attribute2;
    }

    public void GetAttributeValue(String str, IResponseHandler_Attribute iResponseHandler_Attribute) {
        AttributeResponse_t attributeResponse_t = null;
        if (iResponseHandler_Attribute != null) {
            AttributeResponse_t attributeResponse_t2 = new AttributeResponse_t();
            attributeResponse_t2.m_ResponseHandler = iResponseHandler_Attribute;
            attributeResponse_t2.m_nOperation = 1;
            attributeResponse_t2.m_nSubOperation = 0;
            attributeResponse_t2.m_nSpace = 0;
            attributeResponse_t = attributeResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2));
        if (attributeResponse_t != null) {
            attributeResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(attributeResponse_t);
        }
        try {
            PacketStart.WriteUINT8(1);
            PacketStart.WriteUINT8(0);
            PacketStart.WriteString(str);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void SetAttributeValue(String str, int i, String str2, IResponseHandler_Attribute iResponseHandler_Attribute) {
        AttributeResponse_t attributeResponse_t = null;
        if (iResponseHandler_Attribute != null) {
            AttributeResponse_t attributeResponse_t2 = new AttributeResponse_t();
            attributeResponse_t2.m_ResponseHandler = iResponseHandler_Attribute;
            attributeResponse_t2.m_nOperation = 1;
            attributeResponse_t2.m_nSubOperation = 1;
            attributeResponse_t2.m_nSpace = i;
            attributeResponse_t = attributeResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2 + 1 + IStreamingBootStrapIO.GetStringPayloadSize(str2)));
        if (attributeResponse_t != null) {
            attributeResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(attributeResponse_t);
        }
        try {
            PacketStart.WriteUINT8(1);
            PacketStart.WriteUINT8(1);
            PacketStart.WriteString(str);
            PacketStart.WriteUINT8(i);
            PacketStart.WriteString(str2);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public IRequestHandler_FileSystem GetRequestHandler_FileSystem() {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_FileSystem iRequestHandler_FileSystem = this.m_RequestHandler_FileSystem;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_FileSystem;
    }

    public IRequestHandler_FileSystem SetRequestHandler_FileSystem(IRequestHandler_FileSystem iRequestHandler_FileSystem) {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_FileSystem iRequestHandler_FileSystem2 = this.m_RequestHandler_FileSystem;
        this.m_RequestHandler_FileSystem = iRequestHandler_FileSystem;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_FileSystem2;
    }

    public void QueryFile(String str, int i, IResponseHandler_FileSystem iResponseHandler_FileSystem) {
        FileSystemResponse_t fileSystemResponse_t = null;
        if (iResponseHandler_FileSystem != null) {
            FileSystemResponse_t fileSystemResponse_t2 = new FileSystemResponse_t();
            fileSystemResponse_t2.m_ResponseHandler = iResponseHandler_FileSystem;
            fileSystemResponse_t2.m_nOperation = 2;
            fileSystemResponse_t2.m_nSubOperation = 0;
            fileSystemResponse_t = fileSystemResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2 + 1));
        if (fileSystemResponse_t != null) {
            fileSystemResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(fileSystemResponse_t);
        }
        try {
            PacketStart.WriteUINT8(2);
            PacketStart.WriteUINT8(0);
            PacketStart.WriteString(str);
            PacketStart.WriteUINT8(i);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void ListDirectory(String str, int i, IResponseHandler_FileSystem iResponseHandler_FileSystem) {
        FileSystemResponse_t fileSystemResponse_t = null;
        if (iResponseHandler_FileSystem != null) {
            FileSystemResponse_t fileSystemResponse_t2 = new FileSystemResponse_t();
            fileSystemResponse_t2.m_ResponseHandler = iResponseHandler_FileSystem;
            fileSystemResponse_t2.m_nOperation = 2;
            fileSystemResponse_t2.m_nSubOperation = 1;
            fileSystemResponse_t = fileSystemResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2 + 1));
        if (fileSystemResponse_t != null) {
            fileSystemResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(fileSystemResponse_t);
        }
        try {
            PacketStart.WriteUINT8(2);
            PacketStart.WriteUINT8(1);
            PacketStart.WriteString(str);
            PacketStart.WriteUINT8(i);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void DeleteFileOrDirectory(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem) {
        FileSystemResponse_t fileSystemResponse_t = null;
        if (iResponseHandler_FileSystem != null) {
            FileSystemResponse_t fileSystemResponse_t2 = new FileSystemResponse_t();
            fileSystemResponse_t2.m_ResponseHandler = iResponseHandler_FileSystem;
            fileSystemResponse_t2.m_nOperation = 2;
            fileSystemResponse_t2.m_nSubOperation = 3;
            fileSystemResponse_t = fileSystemResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2));
        if (fileSystemResponse_t != null) {
            fileSystemResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(fileSystemResponse_t);
        }
        try {
            PacketStart.WriteUINT8(2);
            PacketStart.WriteUINT8(3);
            PacketStart.WriteString(str);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public IStreamHandler StoreFile(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem) {
        FileSystemResponse_t fileSystemResponse_t = null;
        IStreamHandler GenerateOutgoingStreamHandler = GenerateOutgoingStreamHandler(null);
        if (iResponseHandler_FileSystem != null) {
            FileSystemResponse_t fileSystemResponse_t2 = new FileSystemResponse_t();
            fileSystemResponse_t2.m_ResponseHandler = iResponseHandler_FileSystem;
            fileSystemResponse_t2.m_nOperation = 2;
            fileSystemResponse_t2.m_nSubOperation = 4;
            fileSystemResponse_t2.m_OutgoingStream = GenerateOutgoingStreamHandler;
            fileSystemResponse_t = fileSystemResponse_t2;
        }
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(GenerateOutgoingStreamHandler.m_nStreamSerial, bArr, 0);
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2 + EncodeUVarInt + 1));
        if (fileSystemResponse_t != null) {
            fileSystemResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(fileSystemResponse_t);
        }
        try {
            PacketStart.WriteUINT8(2);
            PacketStart.WriteUINT8(4);
            PacketStart.WriteString(str);
            PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
            PacketStart.WriteUINT8(0);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
        return GenerateOutgoingStreamHandler;
    }

    public void RetrieveFile(String str, IResponseHandler_FileSystem iResponseHandler_FileSystem) {
        FileSystemResponse_t fileSystemResponse_t = null;
        if (iResponseHandler_FileSystem != null) {
            FileSystemResponse_t fileSystemResponse_t2 = new FileSystemResponse_t();
            fileSystemResponse_t2.m_ResponseHandler = iResponseHandler_FileSystem;
            fileSystemResponse_t2.m_nOperation = 2;
            fileSystemResponse_t2.m_nSubOperation = 5;
            fileSystemResponse_t = fileSystemResponse_t2;
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2));
        if (fileSystemResponse_t != null) {
            fileSystemResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(fileSystemResponse_t);
        }
        try {
            PacketStart.WriteUINT8(2);
            PacketStart.WriteUINT8(5);
            PacketStart.WriteString(str);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public IRequestHandler_Report GetRequestHandler_Report() {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Report iRequestHandler_Report = this.m_RequestHandler_Report;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Report;
    }

    public IRequestHandler_Report SetRequestHandler_Report(IRequestHandler_Report iRequestHandler_Report) {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Report iRequestHandler_Report2 = this.m_RequestHandler_Report;
        this.m_RequestHandler_Report = iRequestHandler_Report;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Report2;
    }

    public void ReportAssertionFailure(String str) {
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2));
        try {
            PacketStart.WriteUINT8(5);
            PacketStart.WriteUINT8(0);
            PacketStart.WriteString(str);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void ReportDroppedPacket(int i, long j, int i2) {
        IStreamingBootStrapIO PacketStart = PacketStart(0, 15);
        try {
            PacketStart.WriteUINT8(5);
            PacketStart.WriteUINT8(1);
            PacketStart.WriteUINT8(i);
            PacketStart.WriteUINT64(j);
            PacketStart.WriteUINT32((long) i2);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void SendGoodbye(String str) {
        if (str == null) {
            str = "";
        }
        for (int i = 0; i < this.m_OutgoingStreamsBySerial.size(); i++) {
            ((COutgoingStreamImp) ((WeakReference) this.m_OutgoingStreamsBySerial.elementAt(i)).get()).ConnectionShuttingDown();
        }
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2));
        try {
            PacketStart.WriteUINT8(5);
            PacketStart.WriteUINT8(2);
            PacketStart.WriteString(str);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        this.m_bSentGoodbye = true;
        if (this.m_bDebug) {
            ((CDebugBootstrapIOHandler) this.m_IOHandler).m_bSentGoodbye = true;
        }
        PacketCompleted();
    }

    public void OnGoodbyeReceived() {
        this.m_bReceivedGoodbye = true;
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        while (this.m_IncomingStreamsBySerial.size() != 0) {
            IStreamHandler iStreamHandler = (IStreamHandler) this.m_IncomingStreamsBySerial.elementAt(0);
            iStreamHandler.OnStreamEnd(true, true, "Remote bootstrap connection shutdown");
            UnregisterIncomingStreamHandler(iStreamHandler);
        }
        writeLock.unlock();
    }

    public IRequestHandler_Tunnel GetRequestHandler_Tunnel() {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Tunnel iRequestHandler_Tunnel = this.m_RequestHandler_Tunnel;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Tunnel;
    }

    public IRequestHandler_Tunnel SetRequestHandler_Tunnel(IRequestHandler_Tunnel iRequestHandler_Tunnel) {
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Tunnel iRequestHandler_Tunnel2 = this.m_RequestHandler_Tunnel;
        this.m_RequestHandler_Tunnel = iRequestHandler_Tunnel;
        this.m_RequestHandlerMutex.unlock();
        return iRequestHandler_Tunnel2;
    }

    public void Tunnel_Connect(int i, String str, long j, IResponseHandler_Tunnel iResponseHandler_Tunnel) {
        if (str == null) {
            str = "";
        }
        TunnelWaitResponse_t tunnelWaitResponse_t = null;
        IStreamHandler GenerateOutgoingStreamHandler = GenerateOutgoingStreamHandler(null);
        if (iResponseHandler_Tunnel != null) {
            TunnelWaitResponse_t tunnelWaitResponse_t2 = new TunnelWaitResponse_t();
            tunnelWaitResponse_t2.m_ResponseHandler = iResponseHandler_Tunnel;
            tunnelWaitResponse_t2.m_nOperation = 6;
            tunnelWaitResponse_t2.m_nSubOperation = 0;
            tunnelWaitResponse_t2.m_OutgoingStream = GenerateOutgoingStreamHandler;
            tunnelWaitResponse_t = tunnelWaitResponse_t2;
        }
        int SBS_strlen = StaticHelpers.SBS_strlen(str) + 1;
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j, bArr, 0);
        byte[] bArr2 = new byte[9];
        int EncodeUVarInt2 = StaticHelpers.EncodeUVarInt(GenerateOutgoingStreamHandler.m_nStreamSerial, bArr2, 0);
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (SBS_strlen + 3 + EncodeUVarInt + EncodeUVarInt2));
        if (tunnelWaitResponse_t != null) {
            tunnelWaitResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(tunnelWaitResponse_t);
        }
        try {
            PacketStart.WriteUINT8(6);
            PacketStart.WriteUINT8(0);
            PacketStart.WriteUINT8(i);
            PacketStart.WriteString(str);
            PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
            PacketStart.WriteRaw(bArr2, 0, EncodeUVarInt2);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void Tunnel_Listen(int i, String str, long j, IResponseHandler_Tunnel iResponseHandler_Tunnel) {
        if (str == null) {
            str = "";
        }
        TunnelWaitResponse_t tunnelWaitResponse_t = null;
        IStreamHandler GenerateOutgoingStreamHandler = GenerateOutgoingStreamHandler(null);
        if (iResponseHandler_Tunnel != null) {
            TunnelWaitResponse_t tunnelWaitResponse_t2 = new TunnelWaitResponse_t();
            tunnelWaitResponse_t2.m_ResponseHandler = iResponseHandler_Tunnel;
            tunnelWaitResponse_t2.m_nOperation = 6;
            tunnelWaitResponse_t2.m_nSubOperation = 1;
            tunnelWaitResponse_t2.m_OutgoingStream = GenerateOutgoingStreamHandler;
            tunnelWaitResponse_t = tunnelWaitResponse_t2;
        }
        int SBS_strlen = StaticHelpers.SBS_strlen(str) + 1;
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j, bArr, 0);
        byte[] bArr2 = new byte[9];
        int EncodeUVarInt2 = StaticHelpers.EncodeUVarInt(GenerateOutgoingStreamHandler.m_nStreamSerial, bArr2, 0);
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (SBS_strlen + 3 + EncodeUVarInt + EncodeUVarInt2));
        if (tunnelWaitResponse_t != null) {
            tunnelWaitResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(tunnelWaitResponse_t);
        }
        try {
            PacketStart.WriteUINT8(6);
            PacketStart.WriteUINT8(1);
            PacketStart.WriteUINT8(i);
            PacketStart.WriteString(str);
            PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
            PacketStart.WriteRaw(bArr2, 0, EncodeUVarInt2);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public void Tunnel_EmbedConnection(String str, long j, String str2, long j2, IResponseHandler_Tunnel iResponseHandler_Tunnel) {
        String str3 = "";
        if (str == null) {
            str = str3;
        }
        if (str2 == null) {
            str2 = str3;
        }
        TunnelWaitResponse_t tunnelWaitResponse_t = null;
        IStreamHandler GenerateOutgoingStreamHandler = GenerateOutgoingStreamHandler(null);
        if (iResponseHandler_Tunnel != null) {
            TunnelWaitResponse_t tunnelWaitResponse_t2 = new TunnelWaitResponse_t();
            tunnelWaitResponse_t2.m_ResponseHandler = iResponseHandler_Tunnel;
            tunnelWaitResponse_t2.m_nOperation = 6;
            tunnelWaitResponse_t2.m_nSubOperation = 2;
            tunnelWaitResponse_t2.m_OutgoingStream = GenerateOutgoingStreamHandler;
            tunnelWaitResponse_t = tunnelWaitResponse_t2;
        }
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j, bArr, 0);
        byte[] bArr2 = new byte[9];
        int EncodeUVarInt2 = StaticHelpers.EncodeUVarInt(j2, bArr2, 0);
        byte[] bArr3 = new byte[9];
        int EncodeUVarInt3 = StaticHelpers.EncodeUVarInt(GenerateOutgoingStreamHandler.m_nStreamSerial, bArr3, 0);
        IStreamingBootStrapIO PacketStart = PacketStart(0, (long) (IStreamingBootStrapIO.GetStringPayloadSize(str) + 2 + EncodeUVarInt + IStreamingBootStrapIO.GetStringPayloadSize(str2) + EncodeUVarInt2 + EncodeUVarInt3));
        if (tunnelWaitResponse_t != null) {
            tunnelWaitResponse_t.m_nPacketID = GetOutgoingPacketID(0);
            this.m_ResponseQueue.add(tunnelWaitResponse_t);
        }
        try {
            PacketStart.WriteUINT8(6);
            PacketStart.WriteUINT8(2);
            PacketStart.WriteString(str);
            PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
            PacketStart.WriteString(str2);
            PacketStart.WriteRaw(bArr2, 0, EncodeUVarInt2);
            PacketStart.WriteRaw(bArr3, 0, EncodeUVarInt3);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        PacketCompleted();
    }

    public StreamingBootStrap_JavaImpl(IStreamingBootStrapIOImpl iStreamingBootStrapIOImpl, int i, boolean z) {
        this.m_IOHandler = iStreamingBootStrapIOImpl;
        this.m_nRemoteProtocolVersion = i;
        this.m_bDebug = z;
        if (z) {
            this.m_IOHandler = new CDebugBootstrapIOHandler(iStreamingBootStrapIOImpl);
        }
        for (int i2 = 0; i2 < 2; i2++) {
            this.m_PreviousResponseID[i2] = -1;
            for (int i3 = 0; i3 < 256; i3++) {
                this.m_ChannelPacketCounters[i2][i3] = 0;
            }
        }
        for (int i4 = 0; i4 < 128; i4++) {
            this.m_ImplementationHandlers[i4] = null;
        }
        if (i < 4) {
            Assert(false);
            SetErrorState();
        }
    }

    public void finalize() {
        if (!this.m_bSentGoodbye) {
            SendGoodbye("Destroying connection interface");
        }
        this.m_IOHandler.OwnerInstanceDestructing(this);
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0057, code lost:
        if (r8 == 0) goto L_0x005e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x005d, code lost:
        r7 = -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.valvesoftware.IStreamingBootStrap.IStreamHandler GenerateOutgoingStreamHandler(long[] r13) {
        /*
            r12 = this;
            com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp r0 = new com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp
            r0.<init>(r12)
            java.util.concurrent.locks.ReadWriteLock r1 = r12.m_StreamHandlerRWLock
            java.util.concurrent.locks.Lock r1 = r1.writeLock()
            boolean r2 = r12.m_bOutgoingStreamsEverOverriddenOrWrapped
            r3 = 0
            r4 = 1
            if (r2 != 0) goto L_0x001f
            if (r13 != 0) goto L_0x001f
            java.util.concurrent.atomic.AtomicLong r13 = r12.m_nOutgoingStreamSerialIncrementer
            long r5 = r13.getAndIncrement()
            r0.m_nStreamSerial = r5
            r1.lock()
            goto L_0x0073
        L_0x001f:
            if (r13 == 0) goto L_0x0026
            r12.m_bOutgoingStreamsEverOverriddenOrWrapped = r4
            r5 = r13[r3]
            goto L_0x002c
        L_0x0026:
            java.util.concurrent.atomic.AtomicLong r13 = r12.m_nOutgoingStreamSerialIncrementer
            long r5 = r13.getAndIncrement()
        L_0x002c:
            java.util.concurrent.locks.ReadWriteLock r13 = r12.m_StreamHandlerRWLock
            java.util.concurrent.locks.Lock r13 = r13.readLock()
            r13.lock()
            r2 = 0
        L_0x0036:
            r7 = 2
            if (r2 >= r7) goto L_0x0073
        L_0x0039:
            r7 = 0
        L_0x003a:
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r8 = r12.m_OutgoingStreamsBySerial
            int r8 = r8.size()
            r9 = -1
            if (r7 >= r8) goto L_0x005d
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r8 = r12.m_OutgoingStreamsBySerial
            java.lang.Object r8 = r8.elementAt(r7)
            java.lang.ref.WeakReference r8 = (java.lang.ref.WeakReference) r8
            java.lang.Object r8 = r8.get()
            com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp r8 = (com.valvesoftware.StreamingBootStrap_JavaImpl.COutgoingStreamImp) r8
            long r10 = r8.m_nStreamSerial
            int r8 = (r10 > r5 ? 1 : (r10 == r5 ? 0 : -1))
            if (r8 < 0) goto L_0x005a
            if (r8 != 0) goto L_0x005d
            goto L_0x005e
        L_0x005a:
            int r7 = r7 + 1
            goto L_0x003a
        L_0x005d:
            r7 = -1
        L_0x005e:
            if (r7 != r9) goto L_0x006c
            if (r2 <= 0) goto L_0x0063
            goto L_0x0073
        L_0x0063:
            r13.unlock()
            r1.lock()
            int r2 = r2 + 1
            goto L_0x0036
        L_0x006c:
            java.util.concurrent.atomic.AtomicLong r5 = r12.m_nOutgoingStreamSerialIncrementer
            long r5 = r5.getAndIncrement()
            goto L_0x0039
        L_0x0073:
            r0.m_nStreamSerial = r5
            int r13 = r0.m_nFlags
            r13 = r13 | r4
            r0.m_nFlags = r13
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r13 = r12.m_OutgoingStreamsBySerial
            int r13 = r13.size()
        L_0x0080:
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r2 = r12.m_OutgoingStreamsBySerial
            int r2 = r2.size()
            if (r3 >= r2) goto L_0x00a1
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r2 = r12.m_OutgoingStreamsBySerial
            java.lang.Object r2 = r2.elementAt(r3)
            java.lang.ref.WeakReference r2 = (java.lang.ref.WeakReference) r2
            java.lang.Object r2 = r2.get()
            com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp r2 = (com.valvesoftware.StreamingBootStrap_JavaImpl.COutgoingStreamImp) r2
            long r7 = r2.m_nStreamSerial
            int r2 = (r7 > r5 ? 1 : (r7 == r5 ? 0 : -1))
            if (r2 <= 0) goto L_0x009e
            r13 = r3
            goto L_0x00a1
        L_0x009e:
            int r3 = r3 + 1
            goto L_0x0080
        L_0x00a1:
            java.util.Vector<java.lang.ref.WeakReference<com.valvesoftware.StreamingBootStrap_JavaImpl$COutgoingStreamImp>> r2 = r12.m_OutgoingStreamsBySerial
            java.lang.ref.WeakReference r3 = new java.lang.ref.WeakReference
            r3.<init>(r0)
            r2.add(r13, r3)
            r1.unlock()
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.GenerateOutgoingStreamHandler(long[]):com.valvesoftware.IStreamingBootStrap$IStreamHandler");
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0023, code lost:
        if (r7 == r4) goto L_0x002a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0029, code lost:
        r1 = -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public com.valvesoftware.IStreamingBootStrap.IStreamHandler GetIncomingStreamBySerial(long r7) {
        /*
            r6 = this;
            java.util.concurrent.locks.ReadWriteLock r0 = r6.m_StreamHandlerRWLock
            java.util.concurrent.locks.Lock r0 = r0.readLock()
            r0.lock()
            r1 = 0
        L_0x000a:
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r2 = r6.m_IncomingStreamsBySerial
            int r2 = r2.size()
            r3 = -1
            if (r1 >= r2) goto L_0x0029
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r2 = r6.m_IncomingStreamsBySerial
            java.lang.Object r2 = r2.elementAt(r1)
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r2 = (com.valvesoftware.IStreamingBootStrap.IStreamHandler) r2
            long r4 = r2.m_nStreamSerial
            int r2 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r2 < 0) goto L_0x0026
            int r2 = (r7 > r4 ? 1 : (r7 == r4 ? 0 : -1))
            if (r2 != 0) goto L_0x0029
            goto L_0x002a
        L_0x0026:
            int r1 = r1 + 1
            goto L_0x000a
        L_0x0029:
            r1 = -1
        L_0x002a:
            r7 = 0
            if (r1 == r3) goto L_0x0035
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r7 = r6.m_IncomingStreamsBySerial
            java.lang.Object r7 = r7.elementAt(r1)
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r7 = (com.valvesoftware.IStreamingBootStrap.IStreamHandler) r7
        L_0x0035:
            r0.unlock()
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.GetIncomingStreamBySerial(long):com.valvesoftware.IStreamingBootStrap$IStreamHandler");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0023, code lost:
        if (r7 == r4) goto L_0x002a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0029, code lost:
        r1 = -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.valvesoftware.IStreamingBootStrap.IStreamHandler GetIncomingStreamByHandle(long r7) {
        /*
            r6 = this;
            java.util.concurrent.locks.ReadWriteLock r0 = r6.m_StreamHandlerRWLock
            java.util.concurrent.locks.Lock r0 = r0.readLock()
            r0.lock()
            r1 = 0
        L_0x000a:
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r2 = r6.m_IncomingStreamsByHandle
            int r2 = r2.size()
            r3 = -1
            if (r1 >= r2) goto L_0x0029
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r2 = r6.m_IncomingStreamsByHandle
            java.lang.Object r2 = r2.elementAt(r1)
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r2 = (com.valvesoftware.IStreamingBootStrap.IStreamHandler) r2
            long r4 = r2.m_nStreamHandle
            int r2 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r2 < 0) goto L_0x0026
            int r2 = (r7 > r4 ? 1 : (r7 == r4 ? 0 : -1))
            if (r2 != 0) goto L_0x0029
            goto L_0x002a
        L_0x0026:
            int r1 = r1 + 1
            goto L_0x000a
        L_0x0029:
            r1 = -1
        L_0x002a:
            r7 = 0
            if (r1 == r3) goto L_0x0035
            java.util.Vector<com.valvesoftware.IStreamingBootStrap$IStreamHandler> r7 = r6.m_IncomingStreamsByHandle
            java.lang.Object r7 = r7.elementAt(r1)
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r7 = (com.valvesoftware.IStreamingBootStrap.IStreamHandler) r7
        L_0x0035:
            r0.unlock()
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.GetIncomingStreamByHandle(long):com.valvesoftware.IStreamingBootStrap$IStreamHandler");
    }

    /* access modifiers changed from: 0000 */
    public IStreamHandler IncomingStreamBindToHandle(long j, long j2) {
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        IStreamHandler GetIncomingStreamBySerial = GetIncomingStreamBySerial(j);
        if (GetIncomingStreamBySerial != null) {
            boolean z = true;
            Assert((GetIncomingStreamBySerial.m_nFlags & 2) == 0);
            int size = this.m_IncomingStreamsByHandle.size();
            int i = 0;
            while (true) {
                if (i >= this.m_IncomingStreamsByHandle.size()) {
                    break;
                }
                int i2 = (((IStreamHandler) this.m_IncomingStreamsByHandle.elementAt(i)).m_nStreamHandle > j2 ? 1 : (((IStreamHandler) this.m_IncomingStreamsByHandle.elementAt(i)).m_nStreamHandle == j2 ? 0 : -1));
                if (i2 >= 0) {
                    if (i2 == 0) {
                        z = false;
                    }
                    Assert(z);
                    size = i;
                } else {
                    i++;
                }
            }
            GetIncomingStreamBySerial.m_nStreamHandle = j2;
            GetIncomingStreamBySerial.m_nFlags |= 2;
            this.m_IncomingStreamsByHandle.add(size, GetIncomingStreamBySerial);
        }
        writeLock.unlock();
        return GetIncomingStreamBySerial;
    }

    /* access modifiers changed from: 0000 */
    public void IncomingStreamUnbindHandle(IStreamHandler iStreamHandler) {
        if ((iStreamHandler.m_nFlags & 2) != 0) {
            Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
            writeLock.lock();
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= this.m_IncomingStreamsByHandle.size()) {
                    i = -1;
                    break;
                } else if (this.m_IncomingStreamsByHandle.elementAt(i) == iStreamHandler) {
                    break;
                } else {
                    i++;
                }
            }
            if (i != -1) {
                z = true;
            }
            Assert(z);
            if (i != -1) {
                this.m_IncomingStreamsByHandle.removeElementAt(i);
            }
            iStreamHandler.m_nFlags &= -3;
            iStreamHandler.m_nStreamHandle = IStreamingBootStrap.INT64_MAX;
            writeLock.unlock();
        }
    }

    /* access modifiers changed from: 0000 */
    public void OutgoingStreamBindToHandle(COutgoingStreamImp cOutgoingStreamImp) {
        int i = 0;
        Assert((cOutgoingStreamImp.m_nFlags & 2) == 0);
        WeakReference weakReference = new WeakReference(cOutgoingStreamImp);
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        int size = this.m_OutgoingStreamsByHandle.size();
        while (true) {
            if (i >= this.m_OutgoingStreamsByHandle.size()) {
                i = size;
                break;
            } else if (((COutgoingStreamImp) ((WeakReference) this.m_OutgoingStreamsByHandle.elementAt(i)).get()).m_nStreamHandle > ((long) i)) {
                break;
            } else {
                i++;
            }
        }
        cOutgoingStreamImp.m_nStreamHandle = (long) i;
        cOutgoingStreamImp.m_nFlags |= 2;
        this.m_OutgoingStreamsByHandle.add(i, weakReference);
        writeLock.unlock();
    }

    /* access modifiers changed from: 0000 */
    public void OutgoingStreamUnbindHandle(COutgoingStreamImp cOutgoingStreamImp) {
        Assert((cOutgoingStreamImp.m_nFlags & 2) != 0);
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        int size = this.m_OutgoingStreamsByHandle.size();
        while (true) {
            size--;
            if (size >= 0) {
                COutgoingStreamImp cOutgoingStreamImp2 = (COutgoingStreamImp) ((WeakReference) this.m_OutgoingStreamsByHandle.elementAt(size)).get();
                if (cOutgoingStreamImp2 == null || cOutgoingStreamImp2 == cOutgoingStreamImp) {
                    this.m_OutgoingStreamsByHandle.removeElementAt(size);
                }
            } else {
                cOutgoingStreamImp.m_nFlags &= -3;
                cOutgoingStreamImp.m_nStreamHandle = IStreamingBootStrap.INT64_MAX;
                writeLock.unlock();
                return;
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void OutgoingStreamClosed(COutgoingStreamImp cOutgoingStreamImp) {
        Assert((cOutgoingStreamImp.m_nFlags & 2) == 0);
        Lock writeLock = this.m_StreamHandlerRWLock.writeLock();
        writeLock.lock();
        int size = this.m_OutgoingStreamsBySerial.size();
        while (true) {
            size--;
            if (size >= 0) {
                COutgoingStreamImp cOutgoingStreamImp2 = (COutgoingStreamImp) ((WeakReference) this.m_OutgoingStreamsBySerial.elementAt(size)).get();
                if (cOutgoingStreamImp2 == null || cOutgoingStreamImp2 == cOutgoingStreamImp) {
                    this.m_OutgoingStreamsBySerial.removeElementAt(size);
                }
            } else {
                cOutgoingStreamImp.m_nFlags &= -2;
                cOutgoingStreamImp.m_nStreamSerial = IStreamingBootStrap.INT64_MAX;
                writeLock.unlock();
                return;
            }
        }
    }

    private IStreamingBootStrapIO BeginRequestResponse(long j) {
        long j2 = this.m_ChannelPacketCounters[1][0];
        byte[] bArr = new byte[9];
        int EncodeUVarInt = StaticHelpers.EncodeUVarInt(j2 - this.m_PreviousResponseID[1], bArr, 0);
        IStreamingBootStrapIO PacketStart = PacketStart(1, ((long) EncodeUVarInt) + j);
        try {
            PacketStart.WriteRaw(bArr, 0, EncodeUVarInt);
        } catch (RuntimeException e) {
            StaticHelpers.CaughtExternalCodeException(e);
        }
        this.m_PreviousResponseID[1] = j2;
        return PacketStart;
    }

    private long OnRequestReceived_Ping(byte[] bArr, int i, int i2, long j, long j2) {
        if (j2 != 0) {
            return Long.MIN_VALUE;
        }
        this.m_RequestHandlerMutex.lock();
        IRequestHandler_Ping iRequestHandler_Ping = this.m_RequestHandler_Ping;
        Long valueOf = Long.valueOf(0);
        if (iRequestHandler_Ping != null) {
            try {
                valueOf = Long.valueOf(iRequestHandler_Ping.GetLocalTimeMicroSeconds(this));
            } catch (RuntimeException e) {
                StaticHelpers.CaughtExternalCodeException(e);
            }
        }
        this.m_RequestHandlerMutex.unlock();
        try {
            BeginRequestResponse(8).WriteUINT64(valueOf.longValue());
        } catch (RuntimeException e2) {
            StaticHelpers.CaughtExternalCodeException(e2);
        }
        PacketCompleted();
        return 0;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x002c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long ProcessResponse_Ping(com.valvesoftware.StreamingBootStrap_JavaImpl.ResponseBase_t r3, byte[] r4, int r5, int r6, long r7, long r9) {
        /*
            r2 = this;
            com.valvesoftware.StreamingBootStrap_JavaImpl$PingResponse_t r3 = (com.valvesoftware.StreamingBootStrap_JavaImpl.PingResponse_t) r3
            r7 = -9223372036854775808
            if (r4 == 0) goto L_0x0027
            int r9 = r6 - r5
            r10 = 8
            if (r9 >= r10) goto L_0x000f
            int r9 = r9 - r10
            long r3 = (long) r9
            return r3
        L_0x000f:
            long r0 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.ReadUINT64(r4, r5)
            int r5 = r5 + r10
            if (r5 != r6) goto L_0x0027
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r4 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0020 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Ping r4 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Ping) r4     // Catch:{ RuntimeException -> 0x0020 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Ping r4 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Ping) r4     // Catch:{ RuntimeException -> 0x0020 }
            r4.OnPingResponse(r2, r0)     // Catch:{ RuntimeException -> 0x0020 }
            goto L_0x0024
        L_0x0020:
            r4 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r4)
        L_0x0024:
            r4 = 0
            goto L_0x0028
        L_0x0027:
            r4 = r7
        L_0x0028:
            int r6 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r6 != 0) goto L_0x003a
            r6 = 0
            Assert(r6)
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r3 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0036 }
            r3.ResponseAborted(r2)     // Catch:{ RuntimeException -> 0x0036 }
            goto L_0x003a
        L_0x0036:
            r3 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r3)
        L_0x003a:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.ProcessResponse_Ping(com.valvesoftware.StreamingBootStrap_JavaImpl$ResponseBase_t, byte[], int, int, long, long):long");
    }

    private long OnRequestReceived_Attribute(byte[] bArr, int i, int i2, long j, long j2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        byte[] bArr2 = bArr;
        int i8 = i2;
        int i9 = i8 - i;
        if (i9 < 1) {
            return (long) (i9 - 1);
        }
        byte b = bArr2[i];
        int i10 = i + 1;
        int SBS_strend = StaticHelpers.SBS_strend(bArr2, i10, i8);
        int i11 = 0;
        if (b != 0) {
            if (b != 1) {
                if (b != 2) {
                    int i12 = 4;
                    if (b != 3) {
                        if (b != 4) {
                            Assert(false);
                            return Long.MIN_VALUE;
                        } else if (SBS_strend == i8) {
                            return -1;
                        } else {
                            String str = new String(bArr2, i10, SBS_strend - i10, NETWORK_STRING_CHARSET);
                            int i13 = SBS_strend + 1;
                            int i14 = i8 - i13;
                            if (i14 < 8) {
                                return (long) (i14 - 8);
                            }
                            int ReadUINT32 = (int) IStreamingBootStrapIO.ReadUINT32(bArr2, i13);
                            int i15 = i13 + 4;
                            int ReadUINT322 = (int) IStreamingBootStrapIO.ReadUINT32(bArr2, i15);
                            if (i15 + 4 != i8) {
                                Assert(false);
                                return Long.MIN_VALUE;
                            }
                            this.m_RequestHandlerMutex.lock();
                            try {
                                i7 = this.m_RequestHandler_Attribute != null ? this.m_RequestHandler_Attribute.ModifyAttributeFlags(this, str, ReadUINT32, ReadUINT322) : 1;
                            } catch (RuntimeException e) {
                                StaticHelpers.CaughtExternalCodeException(e);
                                i7 = 2;
                            }
                            this.m_RequestHandlerMutex.unlock();
                            try {
                                BeginRequestResponse(1).WriteUINT8(i7);
                            } catch (RuntimeException e2) {
                                StaticHelpers.CaughtExternalCodeException(e2);
                            }
                            PacketCompleted();
                            return 0;
                        }
                    } else if (SBS_strend == i8) {
                        return -1;
                    } else {
                        String str2 = new String(bArr2, i10, SBS_strend - i10, NETWORK_STRING_CHARSET);
                        if (SBS_strend + 1 != i8) {
                            Assert(false);
                            return Long.MIN_VALUE;
                        }
                        int[] iArr = new int[1];
                        this.m_RequestHandlerMutex.lock();
                        try {
                            i6 = this.m_RequestHandler_Attribute != null ? this.m_RequestHandler_Attribute.GetAttributeFlags(this, str2, iArr) : 1;
                        } catch (RuntimeException e3) {
                            StaticHelpers.CaughtExternalCodeException(e3);
                            i6 = 2;
                        }
                        this.m_RequestHandlerMutex.unlock();
                        if (i6 != 0) {
                            i12 = 0;
                        }
                        IStreamingBootStrapIO BeginRequestResponse = BeginRequestResponse((long) (i12 + 1));
                        try {
                            BeginRequestResponse.WriteUINT8(i6);
                            if (i6 == 0) {
                                BeginRequestResponse.WriteUINT32((long) iArr[0]);
                            }
                        } catch (RuntimeException e4) {
                            StaticHelpers.CaughtExternalCodeException(e4);
                        }
                        PacketCompleted();
                        return 0;
                    }
                } else if (SBS_strend == i8) {
                    return -2;
                } else {
                    String str3 = new String(bArr2, i10, SBS_strend - i10, NETWORK_STRING_CHARSET);
                    int i16 = SBS_strend + 1;
                    byte b2 = bArr2[i16];
                    if (i16 + 1 != i8) {
                        Assert(false);
                        return Long.MIN_VALUE;
                    }
                    this.m_RequestHandlerMutex.lock();
                    try {
                        i5 = this.m_RequestHandler_Attribute != null ? this.m_RequestHandler_Attribute.DeleteAttribute(this, b2, str3) : 1;
                    } catch (RuntimeException e5) {
                        StaticHelpers.CaughtExternalCodeException(e5);
                        i5 = 2;
                    }
                    this.m_RequestHandlerMutex.unlock();
                    try {
                        BeginRequestResponse(1).WriteUINT8(i5);
                    } catch (RuntimeException e6) {
                        StaticHelpers.CaughtExternalCodeException(e6);
                    }
                    PacketCompleted();
                    return 0;
                }
            } else if (SBS_strend == i8) {
                return -3;
            } else {
                String str4 = new String(bArr2, i10, SBS_strend - i10, NETWORK_STRING_CHARSET);
                int i17 = SBS_strend + 1;
                byte b3 = bArr2[i17];
                int i18 = i17 + 1;
                int SBS_strend2 = StaticHelpers.SBS_strend(bArr2, i18, i8);
                if (SBS_strend2 == i8) {
                    return -1;
                }
                String str5 = new String(bArr2, i18, SBS_strend2 - i18, NETWORK_STRING_CHARSET);
                if (SBS_strend2 + 1 != i8) {
                    Assert(false);
                    return Long.MIN_VALUE;
                }
                this.m_RequestHandlerMutex.lock();
                try {
                    i4 = this.m_RequestHandler_Attribute != null ? this.m_RequestHandler_Attribute.SetAttributeValue(this, b3, str4, str5) : 1;
                } catch (RuntimeException e7) {
                    StaticHelpers.CaughtExternalCodeException(e7);
                    i4 = 2;
                }
                this.m_RequestHandlerMutex.unlock();
                try {
                    BeginRequestResponse(1).WriteUINT8(i4);
                } catch (RuntimeException e8) {
                    StaticHelpers.CaughtExternalCodeException(e8);
                }
                PacketCompleted();
                return 0;
            }
        } else if (SBS_strend == i8) {
            return -1;
        } else {
            String str6 = new String(bArr2, i10, SBS_strend - i10, NETWORK_STRING_CHARSET);
            if (SBS_strend + 1 != i8) {
                Assert(false);
                return Long.MIN_VALUE;
            }
            String str7 = null;
            this.m_RequestHandlerMutex.lock();
            IRequestHandler_Attribute iRequestHandler_Attribute = this.m_RequestHandler_Attribute;
            if (iRequestHandler_Attribute != null) {
                StringBuilder sb = new StringBuilder();
                try {
                    i3 = iRequestHandler_Attribute.GetAttributeValue(this, str6, sb);
                } catch (RuntimeException e9) {
                    StaticHelpers.CaughtExternalCodeException(e9);
                    i3 = 2;
                }
                if (i3 == 0) {
                    str7 = sb.toString();
                }
            } else {
                i3 = 1;
            }
            this.m_RequestHandlerMutex.unlock();
            if (str7 != null) {
                i11 = IStreamingBootStrapIO.GetStringPayloadSize(str7);
            }
            IStreamingBootStrapIO BeginRequestResponse2 = BeginRequestResponse((long) (i11 + 1));
            try {
                BeginRequestResponse2.WriteUINT8(i3);
                if (i3 == 0) {
                    BeginRequestResponse2.WriteString(str7);
                }
            } catch (RuntimeException e10) {
                StaticHelpers.CaughtExternalCodeException(e10);
            }
            PacketCompleted();
            return 0;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:66:0x00ce  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long ProcessResponse_Attribute(com.valvesoftware.StreamingBootStrap_JavaImpl.ResponseBase_t r8, byte[] r9, int r10, int r11, long r12, long r14) {
        /*
            r7 = this;
            com.valvesoftware.StreamingBootStrap_JavaImpl$AttributeResponse_t r8 = (com.valvesoftware.StreamingBootStrap_JavaImpl.AttributeResponse_t) r8
            r12 = -9223372036854775808
            r14 = 0
            r0 = 0
            if (r9 == 0) goto L_0x00c9
            int r1 = r11 - r10
            r2 = 1
            if (r1 >= r2) goto L_0x0011
            int r1 = r1 - r2
            long r8 = (long) r1
            return r8
        L_0x0011:
            byte r1 = r9[r10]
            int r10 = r10 + r2
            int r3 = r8.m_nSubOperation
            if (r3 == 0) goto L_0x0092
            if (r3 == r2) goto L_0x007f
            r2 = 2
            if (r3 == r2) goto L_0x006c
            r2 = 3
            r4 = 4
            if (r3 == r2) goto L_0x003e
            if (r3 == r4) goto L_0x0028
            Assert(r0)
            goto L_0x00c9
        L_0x0028:
            if (r10 != r11) goto L_0x0039
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0033 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x0033 }
            r9.OnModifyAttributeFlagsResponse(r7, r1)     // Catch:{ RuntimeException -> 0x0033 }
            goto L_0x00ca
        L_0x0033:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x0039:
            Assert(r0)
            goto L_0x00c9
        L_0x003e:
            if (r1 != 0) goto L_0x0057
            long r2 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.ReadUINT32(r9, r10)
            int r9 = (int) r2
            int r10 = r10 + r4
            if (r10 != r11) goto L_0x0068
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r10 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0051 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r10 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r10     // Catch:{ RuntimeException -> 0x0051 }
            r10.OnGetAttributeFlagsResponse(r7, r1, r9)     // Catch:{ RuntimeException -> 0x0051 }
            goto L_0x00ca
        L_0x0051:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x0057:
            if (r10 != r11) goto L_0x0068
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0062 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x0062 }
            r9.OnGetAttributeFlagsResponse(r7, r1, r0)     // Catch:{ RuntimeException -> 0x0062 }
            goto L_0x00ca
        L_0x0062:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x0068:
            Assert(r0)
            goto L_0x00c9
        L_0x006c:
            if (r10 != r11) goto L_0x007b
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0076 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x0076 }
            r9.OnDeleteAttributeResponse(r7, r1)     // Catch:{ RuntimeException -> 0x0076 }
            goto L_0x00ca
        L_0x0076:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x007b:
            Assert(r0)
            goto L_0x00c9
        L_0x007f:
            if (r10 != r11) goto L_0x008e
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0089 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x0089 }
            r9.OnSetAttributeValueResponse(r7, r1)     // Catch:{ RuntimeException -> 0x0089 }
            goto L_0x00ca
        L_0x0089:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x008e:
            Assert(r0)
            goto L_0x00c9
        L_0x0092:
            if (r1 != 0) goto L_0x00b6
            int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r9, r10, r11)
            if (r3 != r11) goto L_0x009d
            r8 = -1
            return r8
        L_0x009d:
            java.lang.String r4 = new java.lang.String
            int r5 = r3 - r10
            java.nio.charset.Charset r6 = NETWORK_STRING_CHARSET
            r4.<init>(r9, r10, r5, r6)
            int r3 = r3 + r2
            if (r3 != r11) goto L_0x00c6
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00b1 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x00b1 }
            r9.OnGetAttributeValueResponse(r7, r1, r4)     // Catch:{ RuntimeException -> 0x00b1 }
            goto L_0x00ca
        L_0x00b1:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x00b6:
            if (r10 != r11) goto L_0x00c6
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r9 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00c1 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Attribute r9 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Attribute) r9     // Catch:{ RuntimeException -> 0x00c1 }
            r10 = 0
            r9.OnGetAttributeValueResponse(r7, r1, r10)     // Catch:{ RuntimeException -> 0x00c1 }
            goto L_0x00ca
        L_0x00c1:
            r9 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r9)
            goto L_0x00ca
        L_0x00c6:
            Assert(r0)
        L_0x00c9:
            r14 = r12
        L_0x00ca:
            int r9 = (r14 > r12 ? 1 : (r14 == r12 ? 0 : -1))
            if (r9 != 0) goto L_0x00db
            Assert(r0)
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r8 = r8.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00d7 }
            r8.ResponseAborted(r7)     // Catch:{ RuntimeException -> 0x00d7 }
            goto L_0x00db
        L_0x00d7:
            r8 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r8)
        L_0x00db:
            return r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.ProcessResponse_Attribute(com.valvesoftware.StreamingBootStrap_JavaImpl$ResponseBase_t, byte[], int, int, long, long):long");
    }

    /* JADX WARNING: Removed duplicated region for block: B:145:0x01e3  */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x01e7  */
    /* JADX WARNING: Removed duplicated region for block: B:151:0x01f8 A[Catch:{ RuntimeException -> 0x0200 }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long OnRequestReceived_FileSystem(byte[] r21, int r22, int r23, long r24, long r26) {
        /*
            r20 = this;
            r1 = r20
            r0 = r21
            r2 = r23
            int r3 = r2 - r22
            r4 = 1
            if (r3 >= r4) goto L_0x000e
            int r3 = r3 - r4
            long r2 = (long) r3
            return r2
        L_0x000e:
            byte r3 = r0[r22]
            int r5 = r22 + 1
            int r6 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r5, r2)
            r7 = -2
            r9 = 0
            r11 = -9223372036854775808
            r13 = 0
            if (r3 == 0) goto L_0x019d
            if (r3 == r4) goto L_0x0128
            r7 = 2
            if (r3 == r7) goto L_0x0124
            r7 = 3
            r14 = 1
            r16 = -1
            if (r3 == r7) goto L_0x00e6
            r7 = 4
            if (r3 == r7) goto L_0x008c
            r7 = 5
            if (r3 == r7) goto L_0x0035
            Assert(r13)
            return r11
        L_0x0035:
            if (r6 != r2) goto L_0x0038
            return r16
        L_0x0038:
            java.lang.String r3 = new java.lang.String
            int r7 = r6 - r5
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r3.<init>(r0, r5, r7, r8)
            int r6 = r6 + r4
            if (r6 == r2) goto L_0x0048
            Assert(r13)
            return r11
        L_0x0048:
            long[] r2 = new long[r4]
            r2[r13] = r9
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x005d }
            if (r0 == 0) goto L_0x0061
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x005d }
            int r0 = r0.RetrieveFile(r1, r3, r2)     // Catch:{ RuntimeException -> 0x005d }
            r4 = r0
            goto L_0x0061
        L_0x005d:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0061:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.unlock()
            r3 = 9
            byte[] r0 = new byte[r3]
            if (r4 != 0) goto L_0x0073
            r5 = r2[r13]
            int r2 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r5, r0, r13)
            goto L_0x0074
        L_0x0073:
            r2 = 0
        L_0x0074:
            int r3 = r2 + 1
            long r5 = (long) r3
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r3 = r1.BeginRequestResponse(r5)
            r3.WriteUINT8(r4)     // Catch:{ RuntimeException -> 0x0084 }
            if (r2 == 0) goto L_0x0088
            r3.WriteRaw(r0, r13, r2)     // Catch:{ RuntimeException -> 0x0084 }
            goto L_0x0088
        L_0x0084:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0088:
            r20.PacketCompleted()
            return r9
        L_0x008c:
            if (r6 != r2) goto L_0x008f
            return r16
        L_0x008f:
            java.lang.String r3 = new java.lang.String
            int r7 = r6 - r5
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r3.<init>(r0, r5, r7, r8)
            int r6 = r6 + r4
            long[] r5 = new long[r4]
            int r7 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r6, r2, r5)
            r18 = r5[r13]
            if (r7 != 0) goto L_0x00a4
            return r16
        L_0x00a4:
            int r6 = r6 + r7
            if (r6 != r2) goto L_0x00a8
            return r16
        L_0x00a8:
            byte r0 = r0[r6]
            int r6 = r6 + r4
            if (r6 == r2) goto L_0x00b1
            Assert(r13)
            return r11
        L_0x00b1:
            java.util.concurrent.locks.Lock r2 = r1.m_RequestHandlerMutex
            r2.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r2 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x00cc }
            if (r2 == 0) goto L_0x00d0
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r2 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x00cc }
            r21 = r2
            r22 = r20
            r23 = r3
            r24 = r0
            r25 = r18
            int r0 = r21.StoreFile(r22, r23, r24, r25)     // Catch:{ RuntimeException -> 0x00cc }
            r4 = r0
            goto L_0x00d0
        L_0x00cc:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x00d0:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.unlock()
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r0 = r1.BeginRequestResponse(r14)
            r0.WriteUINT8(r4)     // Catch:{ RuntimeException -> 0x00dd }
            goto L_0x00e2
        L_0x00dd:
            r0 = move-exception
            r2 = r0
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r2)
        L_0x00e2:
            r20.PacketCompleted()
            return r9
        L_0x00e6:
            if (r6 != r2) goto L_0x00e9
            return r16
        L_0x00e9:
            java.lang.String r3 = new java.lang.String
            int r7 = r6 - r5
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r3.<init>(r0, r5, r7, r8)
            int r6 = r6 + r4
            if (r6 == r2) goto L_0x00f9
            Assert(r13)
            return r11
        L_0x00f9:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x010a }
            if (r0 == 0) goto L_0x010e
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x010a }
            int r0 = r0.DeleteFileOrDirectory(r1, r3)     // Catch:{ RuntimeException -> 0x010a }
            r4 = r0
            goto L_0x010e
        L_0x010a:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x010e:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.unlock()
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r0 = r1.BeginRequestResponse(r14)
            r0.WriteUINT8(r4)     // Catch:{ RuntimeException -> 0x011b }
            goto L_0x0120
        L_0x011b:
            r0 = move-exception
            r2 = r0
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r2)
        L_0x0120:
            r20.PacketCompleted()
            return r9
        L_0x0124:
            Assert(r13)
            return r11
        L_0x0128:
            if (r6 != r2) goto L_0x012b
            return r7
        L_0x012b:
            java.lang.String r3 = new java.lang.String
            int r7 = r6 - r5
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r3.<init>(r0, r5, r7, r8)
            int r6 = r6 + r4
            int r5 = r2 - r6
            if (r5 >= r4) goto L_0x013c
            int r5 = r5 - r4
            long r2 = (long) r5
            return r2
        L_0x013c:
            com.valvesoftware.StreamingBootStrap_JavaImpl$1CListResultGatherer r5 = new com.valvesoftware.StreamingBootStrap_JavaImpl$1CListResultGatherer
            r5.<init>()
            byte r0 = r0[r6]
            r5.m_queryFlags = r0
            int r6 = r6 + r4
            if (r6 == r2) goto L_0x014c
            Assert(r13)
            return r11
        L_0x014c:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x015f }
            if (r0 == 0) goto L_0x0163
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r0 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x015f }
            int r2 = r5.m_queryFlags     // Catch:{ RuntimeException -> 0x015f }
            int r0 = r0.ListDirectory(r1, r3, r2, r5)     // Catch:{ RuntimeException -> 0x015f }
            r4 = r0
            goto L_0x0163
        L_0x015f:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0163:
            java.util.concurrent.locks.Lock r0 = r1.m_RequestHandlerMutex
            r0.unlock()
            r2 = 9
            byte[] r0 = new byte[r2]
            if (r4 == 0) goto L_0x0174
            r5.m_nWritePos = r13
            r5.m_nResults = r13
            r2 = 0
            goto L_0x017b
        L_0x0174:
            int r2 = r5.m_nResults
            long r2 = (long) r2
            int r2 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r2, r0, r13)
        L_0x017b:
            int r3 = r2 + 1
            int r6 = r5.m_nWritePos
            int r3 = r3 + r6
            long r6 = (long) r3
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r3 = r1.BeginRequestResponse(r6)
            r3.WriteUINT8(r4)     // Catch:{ RuntimeException -> 0x0195 }
            if (r4 != 0) goto L_0x0199
            r3.WriteRaw(r0, r13, r2)     // Catch:{ RuntimeException -> 0x0195 }
            byte[] r0 = r5.m_Buffer     // Catch:{ RuntimeException -> 0x0195 }
            int r2 = r5.m_nWritePos     // Catch:{ RuntimeException -> 0x0195 }
            r3.WriteRaw(r0, r13, r2)     // Catch:{ RuntimeException -> 0x0195 }
            goto L_0x0199
        L_0x0195:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0199:
            r20.PacketCompleted()
            return r9
        L_0x019d:
            if (r6 != r2) goto L_0x01a0
            return r7
        L_0x01a0:
            java.lang.String r3 = new java.lang.String
            int r7 = r6 - r5
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r3.<init>(r0, r5, r7, r8)
            int r6 = r6 + r4
            int r5 = r2 - r6
            if (r5 >= r4) goto L_0x01b1
            int r5 = r5 - r4
            long r2 = (long) r5
            return r2
        L_0x01b1:
            byte r0 = r0[r6]
            int r6 = r6 + r4
            if (r6 == r2) goto L_0x01ba
            Assert(r13)
            return r11
        L_0x01ba:
            com.valvesoftware.IStreamingBootStrap$FileSystemQueryResult_t r2 = new com.valvesoftware.IStreamingBootStrap$FileSystemQueryResult_t
            r2.<init>()
            r2.Clear()
            java.util.concurrent.locks.Lock r5 = r1.m_RequestHandlerMutex
            r5.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r5 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x01d2 }
            if (r5 == 0) goto L_0x01d6
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_FileSystem r5 = r1.m_RequestHandler_FileSystem     // Catch:{ RuntimeException -> 0x01d2 }
            int r0 = r5.QueryFile(r1, r3, r0, r2)     // Catch:{ RuntimeException -> 0x01d2 }
            goto L_0x01d7
        L_0x01d2:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x01d6:
            r0 = 1
        L_0x01d7:
            java.util.concurrent.locks.Lock r3 = r1.m_RequestHandlerMutex
            r3.unlock()
            com.valvesoftware.StreamingBootStrap_JavaImpl$CFileSystemQueryReplyEncoder r3 = new com.valvesoftware.StreamingBootStrap_JavaImpl$CFileSystemQueryReplyEncoder
            r3.<init>()
            if (r0 != 0) goto L_0x01e7
            r3.EncodeResult(r2)
            goto L_0x01e9
        L_0x01e7:
            r3.m_nWrittenBytes = r13
        L_0x01e9:
            int r2 = r3.m_nWrittenBytes
            int r2 = r2 + r4
            long r4 = (long) r2
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r2 = r1.BeginRequestResponse(r4)
            r2.WriteUINT8(r0)     // Catch:{ RuntimeException -> 0x0200 }
            int r0 = r3.m_nWrittenBytes     // Catch:{ RuntimeException -> 0x0200 }
            if (r0 == 0) goto L_0x0204
            byte[] r0 = r3.m_Buffer     // Catch:{ RuntimeException -> 0x0200 }
            int r3 = r3.m_nWrittenBytes     // Catch:{ RuntimeException -> 0x0200 }
            r2.WriteRaw(r0, r13, r3)     // Catch:{ RuntimeException -> 0x0200 }
            goto L_0x0204
        L_0x0200:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0204:
            r20.PacketCompleted()
            return r9
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.OnRequestReceived_FileSystem(byte[], int, int, long, long):long");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:71:0x00f5, code lost:
        if (r11 != r2) goto L_0x0109;
     */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x014c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long ProcessResponse_FileSystem(com.valvesoftware.StreamingBootStrap_JavaImpl.ResponseBase_t r21, byte[] r22, int r23, int r24, long r25, long r27) {
        /*
            r20 = this;
            r1 = r20
            r0 = r22
            r2 = r24
            r3 = r21
            com.valvesoftware.StreamingBootStrap_JavaImpl$FileSystemResponse_t r3 = (com.valvesoftware.StreamingBootStrap_JavaImpl.FileSystemResponse_t) r3
            r8 = 0
            if (r0 == 0) goto L_0x0144
            int r9 = r2 - r23
            r10 = 1
            if (r9 >= r10) goto L_0x0015
            int r9 = r9 - r10
            long r2 = (long) r9
            return r2
        L_0x0015:
            byte r9 = r0[r23]
            int r11 = r23 + 1
            int r12 = r3.m_nSubOperation
            r13 = 0
            if (r12 == 0) goto L_0x010d
            r14 = -1
            if (r12 == r10) goto L_0x008c
            r13 = 2
            if (r12 == r13) goto L_0x0087
            r13 = 3
            if (r12 == r13) goto L_0x006e
            r13 = 4
            if (r12 == r13) goto L_0x0058
            r13 = 5
            if (r12 == r13) goto L_0x0033
            Assert(r8)
            goto L_0x0144
        L_0x0033:
            if (r9 != 0) goto L_0x0042
            long[] r10 = new long[r10]
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r11, r2, r10)
            if (r0 != 0) goto L_0x003e
            return r14
        L_0x003e:
            r12 = r10[r8]
            int r11 = r11 + r0
            goto L_0x0044
        L_0x0042:
            r12 = 0
        L_0x0044:
            if (r11 != r2) goto L_0x0053
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x004e }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x004e }
            r0.OnRetrieveFileResponse(r1, r9, r12)     // Catch:{ RuntimeException -> 0x004e }
            goto L_0x007c
        L_0x004e:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x007c
        L_0x0053:
            Assert(r8)
            goto L_0x0144
        L_0x0058:
            if (r11 != r2) goto L_0x0069
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0064 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x0064 }
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r2 = r3.m_OutgoingStream     // Catch:{ RuntimeException -> 0x0064 }
            r0.OnStoreFileResponse(r1, r9, r2)     // Catch:{ RuntimeException -> 0x0064 }
            goto L_0x007c
        L_0x0064:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x007c
        L_0x0069:
            Assert(r8)
            goto L_0x0144
        L_0x006e:
            if (r11 != r2) goto L_0x0082
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0078 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x0078 }
            r0.OnDeleteFileOrDirectoryResponse(r1, r9)     // Catch:{ RuntimeException -> 0x0078 }
            goto L_0x007c
        L_0x0078:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x007c:
            r4 = -9223372036854775808
            r18 = 0
            goto L_0x0148
        L_0x0082:
            Assert(r8)
            goto L_0x0144
        L_0x0087:
            Assert(r8)
            goto L_0x0144
        L_0x008c:
            r16 = 1
            long r16 = r25 + r16
            int r12 = r2 - r11
            long r6 = (long) r12
            long r16 = r16 + r6
            int r6 = (r16 > r27 ? 1 : (r16 == r27 ? 0 : -1))
            if (r6 == 0) goto L_0x009c
            long r16 = r16 - r27
            return r16
        L_0x009c:
            if (r9 != 0) goto L_0x00f8
            long[] r6 = new long[r10]
            int r7 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r11, r2, r6)
            if (r7 != 0) goto L_0x00a7
            return r14
        L_0x00a7:
            r14 = r6[r8]
            int r6 = (int) r14
            int r11 = r11 + r7
            com.valvesoftware.IStreamingBootStrap$FileSystemDirectoryListEntry_t[] r7 = new com.valvesoftware.IStreamingBootStrap.FileSystemDirectoryListEntry_t[r6]
            r10 = 0
        L_0x00ae:
            if (r10 >= r6) goto L_0x00df
            com.valvesoftware.IStreamingBootStrap$FileSystemDirectoryListEntry_t r12 = new com.valvesoftware.IStreamingBootStrap$FileSystemDirectoryListEntry_t
            r12.<init>()
            r7[r10] = r12
            r12 = r7[r10]
            r12.nSetFields = r8
            int r12 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r11, r2)
            if (r12 != r2) goto L_0x00c3
        L_0x00c1:
            r6 = 0
            goto L_0x00df
        L_0x00c3:
            r14 = r7[r10]
            java.lang.String r15 = new java.lang.String
            int r4 = r12 - r11
            java.nio.charset.Charset r5 = NETWORK_STRING_CHARSET
            r15.<init>(r0, r11, r4, r5)
            r14.sName = r15
            int r11 = r12 + 1
            r4 = r7[r10]
            int r4 = com.valvesoftware.StreamingBootStrap_JavaImpl.CFileSystemQueryReplyEncoder.DecodeResult(r0, r11, r2, r4)
            if (r4 >= 0) goto L_0x00db
            goto L_0x00c1
        L_0x00db:
            int r11 = r11 + r4
            int r10 = r10 + 1
            goto L_0x00ae
        L_0x00df:
            if (r11 != r2) goto L_0x00f2
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00ed }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x00ed }
            if (r6 == 0) goto L_0x00e8
            goto L_0x00e9
        L_0x00e8:
            r7 = r13
        L_0x00e9:
            r0.OnListDirectoryResponse(r1, r9, r7)     // Catch:{ RuntimeException -> 0x00ed }
            goto L_0x00f5
        L_0x00ed:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x00f5
        L_0x00f2:
            Assert(r8)
        L_0x00f5:
            if (r11 != r2) goto L_0x0109
            goto L_0x007c
        L_0x00f8:
            if (r11 != r2) goto L_0x0109
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0103 }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x0103 }
            r0.OnListDirectoryResponse(r1, r9, r13)     // Catch:{ RuntimeException -> 0x0103 }
            goto L_0x007c
        L_0x0103:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x007c
        L_0x0109:
            Assert(r8)
            goto L_0x0144
        L_0x010d:
            if (r9 != 0) goto L_0x0130
            com.valvesoftware.IStreamingBootStrap$FileSystemQueryResult_t r4 = new com.valvesoftware.IStreamingBootStrap$FileSystemQueryResult_t
            r4.<init>()
            r4.nSetFields = r8
            int r0 = com.valvesoftware.StreamingBootStrap_JavaImpl.CFileSystemQueryReplyEncoder.DecodeResult(r0, r11, r2, r4)
            if (r0 >= 0) goto L_0x011e
            long r2 = (long) r0
            return r2
        L_0x011e:
            int r11 = r11 + r0
            if (r11 != r2) goto L_0x0141
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x012a }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x012a }
            r0.OnQueryFileResponse(r1, r9, r4)     // Catch:{ RuntimeException -> 0x012a }
            goto L_0x007c
        L_0x012a:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x007c
        L_0x0130:
            if (r11 != r2) goto L_0x0141
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x013b }
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_FileSystem r0 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_FileSystem) r0     // Catch:{ RuntimeException -> 0x013b }
            r0.OnQueryFileResponse(r1, r9, r13)     // Catch:{ RuntimeException -> 0x013b }
            goto L_0x007c
        L_0x013b:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x007c
        L_0x0141:
            Assert(r8)
        L_0x0144:
            r4 = -9223372036854775808
            r18 = -9223372036854775808
        L_0x0148:
            int r0 = (r18 > r4 ? 1 : (r18 == r4 ? 0 : -1))
            if (r0 != 0) goto L_0x0159
            Assert(r8)
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r3.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0155 }
            r0.ResponseAborted(r1)     // Catch:{ RuntimeException -> 0x0155 }
            goto L_0x0159
        L_0x0155:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0159:
            return r18
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.ProcessResponse_FileSystem(com.valvesoftware.StreamingBootStrap_JavaImpl$ResponseBase_t, byte[], int, int, long, long):long");
    }

    /* access modifiers changed from: 0000 */
    public long OnRequestReceived_Stream(byte[] bArr, int i, int i2, long j, long j2) {
        Assert(false);
        return Long.MIN_VALUE;
    }

    /* access modifiers changed from: 0000 */
    public long ProcessResponse_Stream(ResponseBase_t responseBase_t, byte[] bArr, int i, int i2, long j, long j2) {
        Assert(false);
        return Long.MIN_VALUE;
    }

    /* access modifiers changed from: 0000 */
    public long OnRequestReceived_Channel(byte[] bArr, int i, int i2, long j, long j2) {
        Assert(false);
        return Long.MIN_VALUE;
    }

    /* access modifiers changed from: 0000 */
    public long ProcessResponse_Channel(ResponseBase_t responseBase_t, byte[] bArr, int i, int i2, long j, long j2) {
        Assert(false);
        return Long.MIN_VALUE;
    }

    private long OnRequestReceived_Report(byte[] bArr, int i, int i2, long j, long j2) {
        int i3;
        int i4 = i2 - i;
        if (i4 < 1) {
            i3 = i4 - 1;
        } else {
            byte b = bArr[i];
            int i5 = i + 1;
            if (b == 0) {
                int SBS_strend = StaticHelpers.SBS_strend(bArr, i5, i2);
                if (SBS_strend == i2) {
                    return -1;
                }
                String str = new String(bArr, i5, SBS_strend - i5, NETWORK_STRING_CHARSET);
                if (SBS_strend + 1 != i2) {
                    Assert(false);
                    return Long.MIN_VALUE;
                }
                this.m_RequestHandlerMutex.lock();
                IRequestHandler_Report iRequestHandler_Report = this.m_RequestHandler_Report;
                if (iRequestHandler_Report != null) {
                    try {
                        iRequestHandler_Report.AssertionFailure(this, str);
                    } catch (RuntimeException e) {
                        StaticHelpers.CaughtExternalCodeException(e);
                    }
                }
                this.m_RequestHandlerMutex.unlock();
                return 0;
            } else if (b == 1) {
                int i6 = i2 - i5;
                if (i6 < 13) {
                    i3 = i6 - 13;
                } else {
                    byte b2 = bArr[i5];
                    int i7 = i5 + 1;
                    long ReadUINT64 = IStreamingBootStrapIO.ReadUINT64(bArr, i7);
                    int i8 = i7 + 8;
                    int ReadUINT32 = (int) IStreamingBootStrapIO.ReadUINT32(bArr, i8);
                    if (i8 + 4 != i2) {
                        Assert(false);
                        return Long.MIN_VALUE;
                    }
                    this.m_RequestHandlerMutex.lock();
                    IRequestHandler_Report iRequestHandler_Report2 = this.m_RequestHandler_Report;
                    if (iRequestHandler_Report2 != null) {
                        try {
                            iRequestHandler_Report2.ChannelError(this, b2, ReadUINT64, ReadUINT32);
                        } catch (RuntimeException e2) {
                            StaticHelpers.CaughtExternalCodeException(e2);
                        }
                    }
                    this.m_RequestHandlerMutex.unlock();
                    return 0;
                }
            } else if (b != 2) {
                Assert(false);
                return Long.MIN_VALUE;
            } else {
                int SBS_strend2 = StaticHelpers.SBS_strend(bArr, i5, i2);
                if (SBS_strend2 == i2) {
                    return -1;
                }
                String str2 = new String(bArr, i5, SBS_strend2 - i5, NETWORK_STRING_CHARSET);
                if (SBS_strend2 + 1 != i2) {
                    Assert(false);
                    return Long.MIN_VALUE;
                }
                this.m_RequestHandlerMutex.lock();
                IRequestHandler_Report iRequestHandler_Report3 = this.m_RequestHandler_Report;
                if (iRequestHandler_Report3 != null) {
                    try {
                        iRequestHandler_Report3.Goodbye(this, str2);
                    } catch (RuntimeException e3) {
                        StaticHelpers.CaughtExternalCodeException(e3);
                    }
                }
                this.m_RequestHandlerMutex.unlock();
                OnGoodbyeReceived();
                return 0;
            }
        }
        return (long) i3;
    }

    /* access modifiers changed from: 0000 */
    public long ProcessResponse_Report(ResponseBase_t responseBase_t, byte[] bArr, int i, int i2, long j, long j2) {
        Assert(false);
        return Long.MIN_VALUE;
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x017a A[Catch:{ RuntimeException -> 0x0187 }] */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x017e A[Catch:{ RuntimeException -> 0x0187 }] */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x01f3  */
    /* JADX WARNING: Removed duplicated region for block: B:136:0x01fa  */
    /* JADX WARNING: Removed duplicated region for block: B:138:0x01fd  */
    /* JADX WARNING: Removed duplicated region for block: B:139:0x0204  */
    /* JADX WARNING: Removed duplicated region for block: B:144:0x0212 A[Catch:{ RuntimeException -> 0x021f }] */
    /* JADX WARNING: Removed duplicated region for block: B:145:0x0216 A[Catch:{ RuntimeException -> 0x021f }] */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0162  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x0165  */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x016c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long OnRequestReceived_Tunnel(byte[] r21, int r22, int r23, long r24, long r26) {
        /*
            r20 = this;
            r12 = r20
            r0 = r21
            r1 = r23
            int r2 = r1 - r22
            r13 = 1
            if (r2 >= r13) goto L_0x000e
            int r2 = r2 - r13
            long r0 = (long) r2
            return r0
        L_0x000e:
            byte r2 = r0[r22]
            int r3 = r22 + 1
            r4 = -9223372036854775808
            r6 = -1
            r9 = 0
            if (r2 == 0) goto L_0x0190
            if (r2 == r13) goto L_0x00f8
            r8 = 2
            if (r2 == r8) goto L_0x0022
            Assert(r9)
            return r4
        L_0x0022:
            long[] r2 = new long[r13]
            int r8 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r3, r1)
            if (r8 != r1) goto L_0x002b
            return r6
        L_0x002b:
            java.lang.String r10 = new java.lang.String
            int r11 = r8 - r3
            java.nio.charset.Charset r14 = NETWORK_STRING_CHARSET
            r10.<init>(r0, r3, r11, r14)
            int r8 = r8 + r13
            int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r2)
            if (r3 != 0) goto L_0x003c
            return r6
        L_0x003c:
            r14 = r2[r9]
            int r8 = r8 + r3
            int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r8, r1)
            if (r3 != r1) goto L_0x0046
            return r6
        L_0x0046:
            java.lang.String r11 = new java.lang.String
            int r4 = r3 - r8
            java.nio.charset.Charset r5 = NETWORK_STRING_CHARSET
            r11.<init>(r0, r8, r4, r5)
            int r3 = r3 + r13
            int r4 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r3, r1, r2)
            if (r4 != 0) goto L_0x0057
            return r6
        L_0x0057:
            r16 = r2[r9]
            int r3 = r3 + r4
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r3, r1, r2)
            if (r0 != 0) goto L_0x0061
            return r6
        L_0x0061:
            r18 = r2[r9]
            int r3 = r3 + r0
            if (r3 == r1) goto L_0x006c
            Assert(r9)
            r0 = -9223372036854775808
            return r0
        L_0x006c:
            com.valvesoftware.IStreamingBootStrap$TunnelEmbedResponse_t r7 = new com.valvesoftware.IStreamingBootStrap$TunnelEmbedResponse_t
            r7.<init>()
            java.util.concurrent.locks.Lock r0 = r12.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel r1 = r12.m_RequestHandler_Tunnel
            if (r1 == 0) goto L_0x0093
            r2 = r20
            r3 = r10
            r4 = r14
            r6 = r11
            r14 = r7
            r7 = r16
            r15 = 0
            r9 = r18
            r13 = 9
            r11 = r14
            int r0 = r1.Embed(r2, r3, r4, r6, r7, r9, r11)     // Catch:{ RuntimeException -> 0x008d }
            goto L_0x0098
        L_0x008d:
            r0 = move-exception
            r1 = r0
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r1)
            goto L_0x0097
        L_0x0093:
            r14 = r7
            r13 = 9
            r15 = 0
        L_0x0097:
            r0 = 1
        L_0x0098:
            java.util.concurrent.locks.Lock r1 = r12.m_RequestHandlerMutex
            r1.unlock()
            byte[] r1 = new byte[r13]
            if (r0 != 0) goto L_0x00a8
            long r2 = r14.nHandlerProtocol
            int r9 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r2, r1, r15)
            goto L_0x00a9
        L_0x00a8:
            r9 = 0
        L_0x00a9:
            if (r0 != 0) goto L_0x00b2
            java.lang.String r2 = r14.sResponseHandlerIdentifier
            int r2 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.GetStringPayloadSize(r2)
            goto L_0x00b3
        L_0x00b2:
            r2 = 0
        L_0x00b3:
            byte[] r3 = new byte[r13]
            if (r0 != 0) goto L_0x00be
            long r4 = r14.nSendStreamSerial
            int r4 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r4, r3, r15)
            goto L_0x00bf
        L_0x00be:
            r4 = 0
        L_0x00bf:
            r5 = 1
            if (r0 <= r5) goto L_0x00c9
            java.lang.String r5 = r14.sFailureReasonOnFail
            int r5 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.GetStringPayloadSize(r5)
            goto L_0x00ca
        L_0x00c9:
            r5 = 0
        L_0x00ca:
            int r6 = r9 + 1
            int r6 = r6 + r2
            int r6 = r6 + r4
            int r6 = r6 + r5
            long r5 = (long) r6
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r2 = r12.BeginRequestResponse(r5)
            r2.WriteUINT8(r0)     // Catch:{ RuntimeException -> 0x00ee }
            if (r0 != 0) goto L_0x00e5
            r2.WriteRaw(r1, r15, r9)     // Catch:{ RuntimeException -> 0x00ee }
            java.lang.String r0 = r14.sResponseHandlerIdentifier     // Catch:{ RuntimeException -> 0x00ee }
            r2.WriteString(r0)     // Catch:{ RuntimeException -> 0x00ee }
            r2.WriteRaw(r3, r15, r4)     // Catch:{ RuntimeException -> 0x00ee }
            goto L_0x00f2
        L_0x00e5:
            r1 = 1
            if (r0 <= r1) goto L_0x00f2
            java.lang.String r0 = r14.sFailureReasonOnFail     // Catch:{ RuntimeException -> 0x00ee }
            r2.WriteString(r0)     // Catch:{ RuntimeException -> 0x00ee }
            goto L_0x00f2
        L_0x00ee:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x00f2:
            r20.PacketCompleted()
        L_0x00f5:
            r1 = 0
            return r1
        L_0x00f8:
            r2 = 1
            r13 = 9
            r15 = 0
            long[] r4 = new long[r2]
            if (r1 != r3) goto L_0x0101
            return r6
        L_0x0101:
            byte r5 = r0[r3]
            int r3 = r3 + r2
            int r8 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r3, r1)
            if (r8 != r1) goto L_0x010b
            return r6
        L_0x010b:
            java.lang.String r9 = new java.lang.String
            int r10 = r8 - r3
            java.nio.charset.Charset r11 = NETWORK_STRING_CHARSET
            r9.<init>(r0, r3, r10, r11)
            int r8 = r8 + r2
            int r2 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r4)
            if (r2 != 0) goto L_0x011c
            return r6
        L_0x011c:
            r10 = r4[r15]
            int r8 = r8 + r2
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r4)
            if (r0 != 0) goto L_0x0126
            return r6
        L_0x0126:
            r17 = r4[r15]
            int r8 = r8 + r0
            if (r8 == r1) goto L_0x0131
            Assert(r15)
            r0 = -9223372036854775808
            return r0
        L_0x0131:
            com.valvesoftware.IStreamingBootStrap$TunnelResponse_t r14 = new com.valvesoftware.IStreamingBootStrap$TunnelResponse_t
            r14.<init>()
            java.util.concurrent.locks.Lock r0 = r12.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel r1 = r12.m_RequestHandler_Tunnel
            if (r1 == 0) goto L_0x0151
            r2 = r20
            r3 = r5
            r4 = r9
            r5 = r10
            r7 = r17
            r9 = r14
            int r0 = r1.Listen(r2, r3, r4, r5, r7, r9)     // Catch:{ RuntimeException -> 0x014c }
            goto L_0x0152
        L_0x014c:
            r0 = move-exception
            r1 = r0
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r1)
        L_0x0151:
            r0 = 1
        L_0x0152:
            java.util.concurrent.locks.Lock r1 = r12.m_RequestHandlerMutex
            r1.unlock()
            byte[] r1 = new byte[r13]
            if (r0 != 0) goto L_0x0162
            long r2 = r14.nSendStreamSerial
            int r9 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r2, r1, r15)
            goto L_0x0163
        L_0x0162:
            r9 = 0
        L_0x0163:
            if (r0 != 0) goto L_0x016c
            java.lang.String r2 = r14.sFailureReasonOnFail
            int r2 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.GetStringPayloadSize(r2)
            goto L_0x016d
        L_0x016c:
            r2 = 0
        L_0x016d:
            int r3 = r9 + 1
            int r3 = r3 + r2
            long r2 = (long) r3
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r2 = r12.BeginRequestResponse(r2)
            r2.WriteUINT8(r0)     // Catch:{ RuntimeException -> 0x0187 }
            if (r0 != 0) goto L_0x017e
            r2.WriteRaw(r1, r15, r9)     // Catch:{ RuntimeException -> 0x0187 }
            goto L_0x018b
        L_0x017e:
            r1 = 1
            if (r0 <= r1) goto L_0x018b
            java.lang.String r0 = r14.sFailureReasonOnFail     // Catch:{ RuntimeException -> 0x0187 }
            r2.WriteString(r0)     // Catch:{ RuntimeException -> 0x0187 }
            goto L_0x018b
        L_0x0187:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x018b:
            r20.PacketCompleted()
            goto L_0x00f5
        L_0x0190:
            r2 = 1
            r13 = 9
            r15 = 0
            long[] r4 = new long[r2]
            if (r1 != r3) goto L_0x0199
            return r6
        L_0x0199:
            byte r5 = r0[r3]
            int r3 = r3 + r2
            int r8 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r3, r1)
            if (r8 != r1) goto L_0x01a3
            return r6
        L_0x01a3:
            java.lang.String r9 = new java.lang.String
            int r10 = r8 - r3
            java.nio.charset.Charset r11 = NETWORK_STRING_CHARSET
            r9.<init>(r0, r3, r10, r11)
            int r8 = r8 + r2
            int r2 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r4)
            if (r2 != 0) goto L_0x01b4
            return r6
        L_0x01b4:
            r10 = r4[r15]
            int r8 = r8 + r2
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r4)
            if (r0 != 0) goto L_0x01be
            return r6
        L_0x01be:
            r17 = r4[r15]
            int r8 = r8 + r0
            if (r8 == r1) goto L_0x01c9
            Assert(r15)
            r0 = -9223372036854775808
            return r0
        L_0x01c9:
            com.valvesoftware.IStreamingBootStrap$TunnelResponse_t r14 = new com.valvesoftware.IStreamingBootStrap$TunnelResponse_t
            r14.<init>()
            java.util.concurrent.locks.Lock r0 = r12.m_RequestHandlerMutex
            r0.lock()
            com.valvesoftware.IStreamingBootStrap$IRequestHandler_Tunnel r1 = r12.m_RequestHandler_Tunnel
            if (r1 == 0) goto L_0x01e9
            r2 = r20
            r3 = r5
            r4 = r9
            r5 = r10
            r7 = r17
            r9 = r14
            int r0 = r1.Connect(r2, r3, r4, r5, r7, r9)     // Catch:{ RuntimeException -> 0x01e4 }
            goto L_0x01ea
        L_0x01e4:
            r0 = move-exception
            r1 = r0
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r1)
        L_0x01e9:
            r0 = 1
        L_0x01ea:
            java.util.concurrent.locks.Lock r1 = r12.m_RequestHandlerMutex
            r1.unlock()
            byte[] r1 = new byte[r13]
            if (r0 != 0) goto L_0x01fa
            long r2 = r14.nSendStreamSerial
            int r9 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.EncodeUVarInt(r2, r1, r15)
            goto L_0x01fb
        L_0x01fa:
            r9 = 0
        L_0x01fb:
            if (r0 != 0) goto L_0x0204
            java.lang.String r2 = r14.sFailureReasonOnFail
            int r2 = com.valvesoftware.IStreamingBootStrap.IStreamingBootStrapIO.GetStringPayloadSize(r2)
            goto L_0x0205
        L_0x0204:
            r2 = 0
        L_0x0205:
            int r3 = r9 + 1
            int r3 = r3 + r2
            long r2 = (long) r3
            com.valvesoftware.IStreamingBootStrap$IStreamingBootStrapIO r2 = r12.BeginRequestResponse(r2)
            r2.WriteUINT8(r0)     // Catch:{ RuntimeException -> 0x021f }
            if (r0 != 0) goto L_0x0216
            r2.WriteRaw(r1, r15, r9)     // Catch:{ RuntimeException -> 0x021f }
            goto L_0x0223
        L_0x0216:
            r1 = 1
            if (r0 <= r1) goto L_0x0223
            java.lang.String r0 = r14.sFailureReasonOnFail     // Catch:{ RuntimeException -> 0x021f }
            r2.WriteString(r0)     // Catch:{ RuntimeException -> 0x021f }
            goto L_0x0223
        L_0x021f:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x0223:
            r20.PacketCompleted()
            goto L_0x00f5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.OnRequestReceived_Tunnel(byte[], int, int, long, long):long");
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:109:0x017f  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x016b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long ProcessResponse_Tunnel(com.valvesoftware.StreamingBootStrap_JavaImpl.ResponseBase_t r19, byte[] r20, int r21, int r22, long r23, long r25) {
        /*
            r18 = this;
            r0 = r20
            r1 = r22
            r2 = r19
            com.valvesoftware.StreamingBootStrap_JavaImpl$TunnelWaitResponse_t r2 = (com.valvesoftware.StreamingBootStrap_JavaImpl.TunnelWaitResponse_t) r2
            r7 = 0
            if (r0 == 0) goto L_0x0163
            int r8 = r2.m_nSubOperation
            r9 = 0
            r10 = -1
            r12 = 1
            if (r8 == 0) goto L_0x0104
            if (r8 == r12) goto L_0x00a4
            r13 = 2
            if (r8 == r13) goto L_0x001d
            Assert(r7)
            goto L_0x0163
        L_0x001d:
            int r8 = r1 - r21
            if (r8 >= r12) goto L_0x0024
            int r8 = r8 - r12
            long r0 = (long) r8
            return r0
        L_0x0024:
            byte r13 = r0[r21]
            int r8 = r21 + 1
            if (r13 != 0) goto L_0x006e
            long[] r9 = new long[r12]
            int r14 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r8, r1, r9)
            if (r14 != 0) goto L_0x0033
            return r10
        L_0x0033:
            r15 = r9[r7]
            int r8 = r8 + r14
            int r14 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r8, r1)
            if (r14 != r1) goto L_0x003d
            return r10
        L_0x003d:
            java.lang.String r5 = new java.lang.String
            int r6 = r14 - r8
            java.nio.charset.Charset r3 = NETWORK_STRING_CHARSET
            r5.<init>(r0, r8, r6, r3)
            int r14 = r14 + r12
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r14, r1, r9)
            if (r0 != 0) goto L_0x004e
            return r10
        L_0x004e:
            r3 = r9[r7]
            int r14 = r14 + r0
            if (r14 != r1) goto L_0x009f
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0068 }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x0068 }
            r11 = 0
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r0 = r2.m_OutgoingStream     // Catch:{ RuntimeException -> 0x0068 }
            r9 = r18
            r10 = r13
            r12 = r15
            r14 = r5
            r15 = r0
            r16 = r3
            r8.OnEmbedResponse(r9, r10, r11, r12, r14, r15, r16)     // Catch:{ RuntimeException -> 0x0068 }
            goto L_0x015d
        L_0x0068:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x015d
        L_0x006e:
            if (r13 <= r12) goto L_0x0082
            int r3 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r8, r1)
            if (r3 != r1) goto L_0x0077
            return r10
        L_0x0077:
            java.lang.String r9 = new java.lang.String
            int r4 = r3 - r8
            java.nio.charset.Charset r5 = NETWORK_STRING_CHARSET
            r9.<init>(r0, r8, r4, r5)
            int r8 = r3 + 1
        L_0x0082:
            r11 = r9
            if (r8 != r1) goto L_0x009f
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0099 }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x0099 }
            r0 = 0
            r14 = 0
            r15 = 0
            r16 = 0
            r9 = r18
            r10 = r13
            r12 = r0
            r8.OnEmbedResponse(r9, r10, r11, r12, r14, r15, r16)     // Catch:{ RuntimeException -> 0x0099 }
            goto L_0x015d
        L_0x0099:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x015d
        L_0x009f:
            Assert(r7)
            goto L_0x0163
        L_0x00a4:
            int r3 = r1 - r21
            if (r3 >= r12) goto L_0x00ab
            int r3 = r3 - r12
            long r0 = (long) r3
            return r0
        L_0x00ab:
            byte r3 = r0[r21]
            int r4 = r21 + 1
            if (r3 != 0) goto L_0x00d5
            long[] r5 = new long[r12]
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r4, r1, r5)
            if (r0 != 0) goto L_0x00ba
            return r10
        L_0x00ba:
            r13 = r5[r7]
            int r4 = r4 + r0
            if (r4 != r1) goto L_0x0100
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00cf }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x00cf }
            r11 = 0
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r12 = r2.m_OutgoingStream     // Catch:{ RuntimeException -> 0x00cf }
            r9 = r18
            r10 = r3
            r8.OnListenResponse(r9, r10, r11, r12, r13)     // Catch:{ RuntimeException -> 0x00cf }
            goto L_0x015d
        L_0x00cf:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x015d
        L_0x00d5:
            if (r3 <= r12) goto L_0x00e9
            int r5 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r4, r1)
            if (r5 != r1) goto L_0x00de
            return r10
        L_0x00de:
            java.lang.String r9 = new java.lang.String
            int r6 = r5 - r4
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r9.<init>(r0, r4, r6, r8)
            int r4 = r5 + 1
        L_0x00e9:
            r11 = r9
            if (r4 != r1) goto L_0x0100
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x00fb }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x00fb }
            r12 = 0
            r13 = 0
            r9 = r18
            r10 = r3
            r8.OnListenResponse(r9, r10, r11, r12, r13)     // Catch:{ RuntimeException -> 0x00fb }
            goto L_0x015d
        L_0x00fb:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x015d
        L_0x0100:
            Assert(r7)
            goto L_0x0163
        L_0x0104:
            int r3 = r1 - r21
            if (r3 >= r12) goto L_0x010b
            int r3 = r3 - r12
            long r0 = (long) r3
            return r0
        L_0x010b:
            byte r3 = r0[r21]
            int r4 = r21 + 1
            if (r3 != 0) goto L_0x0133
            long[] r5 = new long[r12]
            int r0 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r0, r4, r1, r5)
            if (r0 != 0) goto L_0x011a
            return r10
        L_0x011a:
            r13 = r5[r7]
            int r4 = r4 + r0
            if (r4 != r1) goto L_0x0160
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x012e }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x012e }
            r11 = 0
            com.valvesoftware.IStreamingBootStrap$IStreamHandler r12 = r2.m_OutgoingStream     // Catch:{ RuntimeException -> 0x012e }
            r9 = r18
            r10 = r3
            r8.OnConnectResponse(r9, r10, r11, r12, r13)     // Catch:{ RuntimeException -> 0x012e }
            goto L_0x015d
        L_0x012e:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x015d
        L_0x0133:
            if (r3 <= r12) goto L_0x0147
            int r5 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.SBS_strend(r0, r4, r1)
            if (r5 != r1) goto L_0x013c
            return r10
        L_0x013c:
            java.lang.String r9 = new java.lang.String
            int r6 = r5 - r4
            java.nio.charset.Charset r8 = NETWORK_STRING_CHARSET
            r9.<init>(r0, r4, r6, r8)
            int r4 = r5 + 1
        L_0x0147:
            r11 = r9
            if (r4 != r1) goto L_0x0160
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0159 }
            r8 = r0
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Tunnel r8 = (com.valvesoftware.IStreamingBootStrap.IResponseHandler_Tunnel) r8     // Catch:{ RuntimeException -> 0x0159 }
            r12 = 0
            r13 = 0
            r9 = r18
            r10 = r3
            r8.OnConnectResponse(r9, r10, r11, r12, r13)     // Catch:{ RuntimeException -> 0x0159 }
            goto L_0x015d
        L_0x0159:
            r0 = move-exception
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
        L_0x015d:
            r3 = 0
            goto L_0x0165
        L_0x0160:
            Assert(r7)
        L_0x0163:
            r3 = -9223372036854775808
        L_0x0165:
            r5 = -9223372036854775808
            int r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1))
            if (r0 != 0) goto L_0x017f
            Assert(r7)
            com.valvesoftware.IStreamingBootStrap$IResponseHandler_Base r0 = r2.m_ResponseHandler     // Catch:{ RuntimeException -> 0x0178 }
            r1 = r18
            r0.ResponseAborted(r1)     // Catch:{ RuntimeException -> 0x0176 }
            goto L_0x0181
        L_0x0176:
            r0 = move-exception
            goto L_0x017b
        L_0x0178:
            r0 = move-exception
            r1 = r18
        L_0x017b:
            com.valvesoftware.IStreamingBootStrap.StaticHelpers.CaughtExternalCodeException(r0)
            goto L_0x0181
        L_0x017f:
            r1 = r18
        L_0x0181:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.ProcessResponse_Tunnel(com.valvesoftware.StreamingBootStrap_JavaImpl$ResponseBase_t, byte[], int, int, long, long):long");
    }

    private long CallResponseHandler(ResponseBase_t responseBase_t, byte[] bArr, int i, int i2, long j, long j2) {
        switch (responseBase_t.m_nOperation) {
            case 0:
                return ProcessResponse_Ping(responseBase_t, bArr, i, i2, j, j2);
            case 1:
                return ProcessResponse_Attribute(responseBase_t, bArr, i, i2, j, j2);
            case 2:
                return ProcessResponse_FileSystem(responseBase_t, bArr, i, i2, j, j2);
            case 3:
                return ProcessResponse_Stream(responseBase_t, bArr, i, i2, j, j2);
            case 4:
                return ProcessResponse_Channel(responseBase_t, bArr, i, i2, j, j2);
            case 5:
                return ProcessResponse_Report(responseBase_t, bArr, i, i2, j, j2);
            case 6:
                return ProcessResponse_Tunnel(responseBase_t, bArr, i, i2, j, j2);
            default:
                Assert(false);
                return Long.MIN_VALUE;
        }
    }

    private long StandardChannelHandler_Requests(byte[] bArr, int i, int i2, long j, long j2) {
        long j3;
        boolean z = false;
        if (bArr == null) {
            Assert(false);
            return Long.MIN_VALUE;
        }
        Assert(j == 0);
        Assert(j2 >= 1);
        long j4 = (long) (i2 - i);
        if (j4 < j2) {
            return j4 - j2;
        }
        int i3 = i + 1;
        switch (bArr[i]) {
            case 0:
                j3 = OnRequestReceived_Ping(bArr, i3, i2, j, j2 - 1);
                break;
            case 1:
                j3 = OnRequestReceived_Attribute(bArr, i3, i2, j, j2 - 1);
                break;
            case 2:
                j3 = OnRequestReceived_FileSystem(bArr, i3, i2, j, j2 - 1);
                break;
            case 3:
                j3 = OnRequestReceived_Stream(bArr, i3, i2, j, j2 - 1);
                break;
            case 4:
                j3 = OnRequestReceived_Channel(bArr, i3, i2, j, j2 - 1);
                break;
            case 5:
                j3 = OnRequestReceived_Report(bArr, i3, i2, j, j2 - 1);
                break;
            case 6:
                j3 = OnRequestReceived_Tunnel(bArr, i3, i2, j, j2 - 1);
                break;
            default:
                Assert(false);
                return Long.MIN_VALUE;
        }
        if (j3 > 0) {
            j3++;
        }
        if (j3 == 0 || j3 == Long.MIN_VALUE || j3 == j2) {
            z = true;
        }
        Assert(z);
        return j3;
    }

    private long StandardChannelHandler_Responses(byte[] bArr, int i, int i2, long j, long j2) {
        long j3;
        int i3;
        int i4;
        ResponseBase_t responseBase_t;
        boolean z;
        int i5;
        int i6;
        int i7;
        ResponseBase_t responseBase_t2;
        byte[] bArr2 = bArr;
        int i8 = i;
        int i9 = i2;
        boolean z2 = false;
        if (bArr2 == null) {
            Assert(false);
            ResponseBase_t responseBase_t3 = this.m_MidResponse;
            if (responseBase_t3 != null) {
                int i10 = this.m_nMidResponseHeaderBytes;
                boolean z3 = true;
                if (CallResponseHandler(responseBase_t3, null, 0, 0, j + ((long) i10), j2 - ((long) i10)) != Long.MIN_VALUE) {
                    z3 = false;
                }
                Assert(z3);
                this.m_MidResponse = null;
            }
            return Long.MIN_VALUE;
        } else if (i8 == i9) {
            return -1;
        } else {
            ResponseBase_t responseBase_t4 = this.m_MidResponse;
            if (responseBase_t4 == null) {
                long[] jArr = new long[1];
                int DecodeUVarInt = StaticHelpers.DecodeUVarInt(bArr2, i8, i9, jArr);
                if (DecodeUVarInt == 0) {
                    return -1;
                }
                long j4 = jArr[0];
                if (j4 == 0) {
                    i6 = DecodeUVarInt + i8;
                    if (i6 == i9) {
                        return -1;
                    }
                    int DecodeUVarInt2 = StaticHelpers.DecodeUVarInt(bArr2, i6, i9, jArr);
                    Assert(DecodeUVarInt2 > 0);
                    if (DecodeUVarInt2 == 0) {
                        return -1;
                    }
                    j4 += jArr[0];
                    i5 = DecodeUVarInt2;
                    z = true;
                } else {
                    i5 = DecodeUVarInt;
                    i6 = i8;
                    z = false;
                }
                int i11 = i6 + i5;
                long[] jArr2 = this.m_PreviousResponseID;
                long j5 = jArr2[0] + j4;
                jArr2[0] = j5;
                ResponseBase_t responseBase_t5 = (ResponseBase_t) this.m_ResponseQueue.peek();
                if (responseBase_t5 == null) {
                    return j2;
                }
                ResponseBase_t responseBase_t6 = responseBase_t5;
                while (true) {
                    if (responseBase_t6.m_nPacketID >= j5) {
                        i7 = i5;
                        responseBase_t2 = responseBase_t6;
                        break;
                    }
                    ResponseBase_t responseBase_t7 = responseBase_t6;
                    i7 = i5;
                    Assert(CallResponseHandler(responseBase_t6, null, 0, 0, 0, 0) == Long.MIN_VALUE);
                    Assert(((ResponseBase_t) this.m_ResponseQueue.poll()) == responseBase_t7);
                    responseBase_t6 = (ResponseBase_t) this.m_ResponseQueue.peek();
                    if (responseBase_t6 == null) {
                        responseBase_t2 = responseBase_t6;
                        break;
                    }
                    i5 = i7;
                }
                if (responseBase_t2 == null || responseBase_t2.m_nPacketID != j5) {
                    return j2;
                }
                Assert(((ResponseBase_t) this.m_ResponseQueue.poll()) == responseBase_t2);
                if (z) {
                    if (CallResponseHandler(responseBase_t2, null, 0, 0, 0, 0) == Long.MIN_VALUE) {
                        z2 = true;
                    }
                    Assert(z2);
                    return j2;
                }
                int i12 = i7;
                responseBase_t = responseBase_t2;
                i4 = i12;
                i3 = i11;
                j3 = (long) i12;
            } else {
                int i13 = this.m_nMidResponseHeaderBytes;
                if (j == 0) {
                    j3 = j;
                    i3 = i8 + i13;
                    responseBase_t = responseBase_t4;
                    i4 = i13;
                } else {
                    j3 = j;
                    responseBase_t = responseBase_t4;
                    i4 = i13;
                    i3 = i8;
                }
            }
            ResponseBase_t responseBase_t8 = responseBase_t;
            long CallResponseHandler = CallResponseHandler(responseBase_t, bArr, i3, i2, j3 > 0 ? j3 - ((long) i4) : 0, j2 - ((long) i4));
            int i14 = (CallResponseHandler > 0 ? 1 : (CallResponseHandler == 0 ? 0 : -1));
            if (i14 > 0) {
                CallResponseHandler += (long) (i3 - i8);
                if (j3 + CallResponseHandler < j2) {
                    this.m_MidResponse = responseBase_t8;
                    this.m_nMidResponseHeaderBytes = i4;
                } else {
                    this.m_MidResponse = null;
                }
            } else if (CallResponseHandler == Long.MIN_VALUE || i14 == 0) {
                this.m_MidResponse = null;
            } else {
                this.m_MidResponse = responseBase_t8;
                this.m_nMidResponseHeaderBytes = i4;
            }
            return CallResponseHandler;
        }
    }

    private long StandardChannelHandler_Streams(byte[] bArr, int i, int i2, long j, long j2) {
        String str;
        byte[] bArr2 = bArr;
        int i3 = i;
        int i4 = i2;
        if (bArr2 == null) {
            Assert(false);
            return Long.MIN_VALUE;
        }
        boolean z = true;
        Assert(j == 0);
        int i5 = i4 - i3;
        long j3 = (long) i5;
        if (j3 < j2) {
            return j3 - j2;
        }
        if (i5 < 1) {
            return (long) (i5 - 1);
        }
        long[] jArr = new long[1];
        long j4 = (long) i3;
        byte b = bArr2[i3];
        int i6 = i3 + 1;
        if (b == 0) {
            int DecodeUVarInt = StaticHelpers.DecodeUVarInt(bArr2, i6, i4, jArr);
            if (DecodeUVarInt == 0) {
                return -1;
            }
            long j5 = jArr[0];
            int i7 = i6 + DecodeUVarInt;
            long[] jArr2 = new long[1];
            int DecodeUVarInt2 = StaticHelpers.DecodeUVarInt(bArr2, i7, i4, jArr2);
            if (DecodeUVarInt2 == 0) {
                return -1;
            }
            long j6 = jArr2[0];
            int i8 = i7 + DecodeUVarInt2;
            long[] jArr3 = new long[1];
            int DecodeSVarInt = StaticHelpers.DecodeSVarInt(bArr2, i8, i4, jArr3);
            if (DecodeSVarInt == 0) {
                return -1;
            }
            int i9 = i8 + DecodeSVarInt;
            long j7 = j2 - (((long) i9) - j4);
            long j8 = (long) (i4 - i9);
            if (j8 < j7) {
                return j8 - j7;
            }
            IStreamHandler IncomingStreamBindToHandle = IncomingStreamBindToHandle(j6, j5);
            if (IncomingStreamBindToHandle != null) {
                try {
                    IncomingStreamBindToHandle.OnStreamStart(jArr3[0]);
                    if (j7 != 0) {
                        IncomingStreamBindToHandle.HandleStreamChunk(bArr2, i9, (int) j7);
                    }
                } catch (RuntimeException e) {
                    StaticHelpers.CaughtExternalCodeException(e);
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Incoming stream handle ");
                sb.append(j5);
                sb.append(" with serial ");
                sb.append(j6);
                sb.append(" start has no handler!");
                Log.i("com.valvesoftware.StreamingBootStrap_JavaImpl", sb.toString());
            }
            return 0;
        } else if (b == 1) {
            int DecodeUVarInt3 = StaticHelpers.DecodeUVarInt(bArr2, i6, i4, jArr);
            if (DecodeUVarInt3 == 0) {
                return -1;
            }
            long j9 = jArr[0];
            int i10 = i6 + DecodeUVarInt3;
            long j10 = j2 - (((long) i10) - j4);
            long j11 = (long) (i4 - i10);
            if (j11 < j10) {
                return j11 - j10;
            }
            IStreamHandler GetIncomingStreamByHandle = GetIncomingStreamByHandle(j9);
            if (GetIncomingStreamByHandle != null) {
                try {
                    GetIncomingStreamByHandle.HandleStreamChunk(bArr2, i10, (int) j10);
                } catch (RuntimeException e2) {
                    StaticHelpers.CaughtExternalCodeException(e2);
                }
            }
            return 0;
        } else if (b == 2 || b == 3) {
            boolean z2 = b == 2;
            int DecodeUVarInt4 = StaticHelpers.DecodeUVarInt(bArr2, i6, i4, jArr);
            if (DecodeUVarInt4 == 0) {
                return -1;
            }
            long j12 = jArr[0];
            int i11 = i6 + DecodeUVarInt4;
            if (i11 == i4) {
                return -1;
            }
            byte b2 = bArr2[i11];
            int i12 = i11 + 1;
            boolean z3 = (b2 & 1) != 0;
            byte b3 = b2 & 2;
            if (b3 != 0) {
                int SBS_strend = StaticHelpers.SBS_strend(bArr2, i12, i4);
                if (SBS_strend == i4) {
                    return -1;
                }
                str = new String(bArr2, i12, SBS_strend - i12, NETWORK_STRING_CHARSET);
                i12 = SBS_strend + 1;
            } else {
                str = null;
            }
            if (i12 != i4) {
                Assert(false);
                return Long.MIN_VALUE;
            }
            IStreamHandler GetIncomingStreamByHandle2 = z2 ? GetIncomingStreamByHandle(j12) : GetIncomingStreamBySerial(j12);
            if (GetIncomingStreamByHandle2 != null) {
                if (b3 == 0) {
                    z = false;
                }
                try {
                    GetIncomingStreamByHandle2.OnStreamEnd(z3, z, str);
                } catch (RuntimeException e3) {
                    StaticHelpers.CaughtExternalCodeException(e3);
                }
                if (z3) {
                    UnregisterIncomingStreamHandler(GetIncomingStreamByHandle2);
                } else {
                    IncomingStreamUnbindHandle(GetIncomingStreamByHandle2);
                }
            }
            return 0;
        } else {
            Assert(false);
            return Long.MIN_VALUE;
        }
    }

    private long CallChannelHandler(int i, byte[] bArr, int i2, int i3, long j, long j2) {
        int i4 = i;
        if (i4 >= 128) {
            IStreamingBootStrapDataHandler iStreamingBootStrapDataHandler = this.m_ImplementationHandlers[i4 - 128];
            if (iStreamingBootStrapDataHandler != null) {
                try {
                    return iStreamingBootStrapDataHandler.HandleBootstrapData(this, i, bArr, i2, i3, j, j2);
                } catch (RuntimeException e) {
                    StaticHelpers.CaughtExternalCodeException(e);
                }
            }
        } else if (i4 == 0) {
            return StandardChannelHandler_Requests(bArr, i2, i3, j, j2);
        } else {
            if (i4 == 1) {
                return StandardChannelHandler_Responses(bArr, i2, i3, j, j2);
            }
            if (i4 == 2) {
                return StandardChannelHandler_Streams(bArr, i2, i3, j, j2);
            }
        }
        return Long.MIN_VALUE;
    }

    /* JADX WARNING: type inference failed for: r2v0, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r8v0 */
    /* JADX WARNING: type inference failed for: r1v1, types: [int] */
    /* JADX WARNING: type inference failed for: r24v1 */
    /* JADX WARNING: type inference failed for: r3v7, types: [int] */
    /* JADX WARNING: type inference failed for: r3v8 */
    /* JADX WARNING: type inference failed for: r3v9, types: [int] */
    /* JADX WARNING: type inference failed for: r1v7, types: [int] */
    /* JADX WARNING: type inference failed for: r8v1 */
    /* JADX WARNING: type inference failed for: r8v2 */
    /* JADX WARNING: type inference failed for: r3v23 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a2  */
    /* JADX WARNING: Unknown variable types count: 6 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private long TryHandlingWaitingMessage(byte[] r24, int r25, int r26) {
        /*
            r23 = this;
            r9 = r23
            r2 = r24
            r0 = r25
            r1 = r26
            boolean r3 = r9.m_bErrorState
            r10 = 1
            r3 = r3 ^ r10
            Assert(r3)
            r11 = 0
            if (r1 < r10) goto L_0x0014
            r3 = 1
            goto L_0x0015
        L_0x0014:
            r3 = 0
        L_0x0015:
            Assert(r3)
            boolean r3 = r9.m_bDiscardWaitingBytes
            r3 = r3 ^ r10
            Assert(r3)
            int r3 = r9.m_nMidPacketChannel
            r4 = 255(0xff, float:3.57E-43)
            r12 = 1
            r14 = 0
            if (r3 <= r4) goto L_0x004d
            long[] r3 = new long[r10]
            int r4 = com.valvesoftware.IStreamingBootStrap.StaticHelpers.DecodeUVarInt(r2, r0, r1, r3)
            r5 = -1
            if (r4 != 0) goto L_0x0033
            return r5
        L_0x0033:
            r7 = r3[r11]
            int r3 = (r7 > r14 ? 1 : (r7 == r14 ? 0 : -1))
            if (r3 != 0) goto L_0x003b
            long r0 = (long) r4
            return r0
        L_0x003b:
            long r7 = r7 - r12
            int r3 = r1 - r0
            if (r3 > r4) goto L_0x0041
            return r5
        L_0x0041:
            int r3 = r0 + r4
            byte r3 = r2[r3]
            int r4 = r4 + r10
            int r0 = r0 + r4
            r5 = r7
            r16 = r14
            r7 = r0
            r8 = r3
            goto L_0x0057
        L_0x004d:
            long r4 = r9.m_nMidPacketOffset
            long r6 = r9.m_nMidPacketSize
            r8 = r3
            r16 = r4
            r5 = r6
            r4 = 0
            r7 = r0
        L_0x0057:
            long r12 = (long) r1
            long r10 = (long) r7
            long r14 = r5 - r16
            long r10 = r10 + r14
            int r0 = (r12 > r10 ? 1 : (r12 == r10 ? 0 : -1))
            if (r0 <= 0) goto L_0x0064
            int r0 = (int) r14
            int r0 = r0 + r7
            r10 = r0
            goto L_0x0065
        L_0x0064:
            r10 = r1
        L_0x0065:
            r0 = r23
            r1 = r8
            r2 = r24
            r3 = r7
            r11 = r4
            r4 = r10
            r12 = r5
            r5 = r16
            r18 = r7
            r24 = r8
            r7 = r12
            long r0 = r0.CallChannelHandler(r1, r2, r3, r4, r5, r7)
            r2 = 0
            int r4 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r4 >= 0) goto L_0x00d1
            r19 = -9223372036854775808
            int r2 = (r0 > r19 ? 1 : (r0 == r19 ? 0 : -1))
            if (r2 == 0) goto L_0x00bd
            int r2 = r10 - r18
            long r3 = (long) r2
            long r3 = r16 + r3
            long r3 = r3 - r0
            int r5 = (r3 > r12 ? 1 : (r3 == r12 ? 0 : -1))
            if (r5 <= 0) goto L_0x0096
            r3 = 0
            Assert(r3)
        L_0x0093:
            r21 = r19
            goto L_0x009e
        L_0x0096:
            int r2 = r2 + r11
            r3 = 8388608(0x800000, float:1.17549435E-38)
            if (r2 != r3) goto L_0x009c
            goto L_0x0093
        L_0x009c:
            r21 = r0
        L_0x009e:
            int r0 = (r21 > r19 ? 1 : (r21 == r19 ? 0 : -1))
            if (r0 != 0) goto L_0x00bb
            r2 = 0
            r3 = 0
            r4 = 0
            r5 = 0
            r7 = 0
            r0 = r23
            r1 = r24
            long r0 = r0.CallChannelHandler(r1, r2, r3, r4, r5, r7)
            int r2 = (r0 > r19 ? 1 : (r0 == r19 ? 0 : -1))
            if (r2 != 0) goto L_0x00b7
            r0 = 1
            goto L_0x00b8
        L_0x00b7:
            r0 = 0
        L_0x00b8:
            Assert(r0)
        L_0x00bb:
            r0 = r21
        L_0x00bd:
            int r2 = (r0 > r19 ? 1 : (r0 == r19 ? 0 : -1))
            if (r2 != 0) goto L_0x00d0
            long[][] r0 = r9.m_ChannelPacketCounters
            r1 = 1
            r0 = r0[r1]
            r3 = r24
            r1 = r0[r3]
            r4 = 0
            r9.ReportDroppedPacket(r3, r1, r4)
            r0 = r14
            goto L_0x00d4
        L_0x00d0:
            return r0
        L_0x00d1:
            r3 = r24
            r4 = 0
        L_0x00d4:
            int r2 = (r0 > r14 ? 1 : (r0 == r14 ? 0 : -1))
            if (r2 <= 0) goto L_0x00dc
            Assert(r4)
            goto L_0x00ef
        L_0x00dc:
            r5 = 0
            int r2 = (r0 > r5 ? 1 : (r0 == r5 ? 0 : -1))
            if (r2 != 0) goto L_0x00ee
            int r10 = r10 - r18
            long r0 = (long) r10
            int r2 = (r0 > r14 ? 1 : (r0 == r14 ? 0 : -1))
            if (r2 != 0) goto L_0x00ea
            r4 = 1
        L_0x00ea:
            Assert(r4)
            goto L_0x00ef
        L_0x00ee:
            r14 = r0
        L_0x00ef:
            long r0 = r16 + r14
            int r2 = (r0 > r12 ? 1 : (r0 == r12 ? 0 : -1))
            if (r2 != 0) goto L_0x0106
            long[][] r0 = r9.m_ChannelPacketCounters
            r1 = 1
            r0 = r0[r1]
            r1 = r0[r3]
            r4 = 1
            long r1 = r1 + r4
            r0[r3] = r1
            r0 = 256(0x100, float:3.59E-43)
            r9.m_nMidPacketChannel = r0
            goto L_0x0118
        L_0x0106:
            r0 = 0
            int r2 = (r16 > r0 ? 1 : (r16 == r0 ? 0 : -1))
            if (r2 != 0) goto L_0x0113
            r9.m_nMidPacketChannel = r3
            r9.m_nMidPacketSize = r12
            r9.m_nMidPacketOffset = r14
            goto L_0x0118
        L_0x0113:
            long r0 = r9.m_nMidPacketOffset
            long r0 = r0 + r14
            r9.m_nMidPacketOffset = r0
        L_0x0118:
            long r0 = (long) r11
            long r14 = r14 + r0
            return r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.StreamingBootStrap_JavaImpl.TryHandlingWaitingMessage(byte[], int, int):long");
    }

    public void SetErrorState() {
        this.m_IOReadMutex.lock();
        this.m_bErrorState = true;
        this.m_nReadBufferPut = 0;
        this.m_ReadBuffer = null;
        this.m_IOReadMutex.unlock();
    }

    public void DumpBuffer() {
        this.m_IOReadMutex.lock();
        StringBuilder sb = new StringBuilder();
        sb.append("Dumping buffer [");
        sb.append(this.m_nReadBufferPut);
        sb.append("]: ");
        String str = "com.valvesoftware.StreamingBootStrap_JavaImpl";
        Log.i(str, sb.toString());
        for (int i = 0; i < this.m_nReadBufferPut; i++) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("\t");
            sb2.append(i);
            sb2.append(": ");
            sb2.append(this.m_ReadBuffer[i]);
            Log.i(str, sb2.toString());
        }
        this.m_IOReadMutex.unlock();
    }

    public static void Assert(boolean z) {
        if (!z) {
            throw new AssertionError("com.valvesoftware.StreamingBootStrap_JavaImpl.Assert() failed");
        }
    }
}
