package com.valvesoftware;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SelfInstall {
    private static int counter;

    private static boolean HasGameZip() {
        return false;
    }

    public static boolean OnStartup() {
        return CheckZippedAssets();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00f2, code lost:
        if (r1 != 0) goto L_0x00f7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.io.File GetContentDirectory() {
        /*
            android.app.Application r0 = com.valvesoftware.JNI_Environment.m_application
            android.content.Context r1 = r0.getApplicationContext()
            java.lang.String r2 = "PatchSystemEnabled"
            boolean[] r2 = com.valvesoftware.Resources.GetBoolean(r2)
            r3 = 1
            r4 = 0
            if (r2 == 0) goto L_0x0016
            boolean r2 = r2[r4]
            if (r2 == 0) goto L_0x0016
            r2 = 1
            goto L_0x0017
        L_0x0016:
            r2 = 0
        L_0x0017:
            boolean r5 = HasGameZip()
            if (r5 != 0) goto L_0x0022
            if (r2 == 0) goto L_0x0020
            goto L_0x0022
        L_0x0020:
            r2 = 0
            goto L_0x0023
        L_0x0022:
            r2 = 1
        L_0x0023:
            java.lang.String r5 = "SelfInstall/Directories"
            org.json.JSONObject r3 = com.valvesoftware.Configuration.GetGlobalKey(r5, r3)
            java.lang.String r5 = "ZipInstall"
            java.lang.String r6 = "StreamingDev"
            r7 = 0
            if (r2 == 0) goto L_0x0032
            r8 = r5
            goto L_0x0033
        L_0x0032:
            r8 = r6
        L_0x0033:
            java.lang.String r8 = r3.getString(r8)     // Catch:{ Throwable -> 0x0038 }
            goto L_0x0039
        L_0x0038:
            r8 = r7
        L_0x0039:
            if (r8 == 0) goto L_0x0041
            java.io.File r9 = new java.io.File
            r9.<init>(r8)
            goto L_0x0042
        L_0x0041:
            r9 = r7
        L_0x0042:
            java.lang.String r8 = "com.valvesoftware.SelfInstall"
            if (r9 == 0) goto L_0x0074
            boolean r10 = r9.exists()
            if (r10 == 0) goto L_0x0074
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "Re-using previous "
            r0.append(r1)
            if (r2 == 0) goto L_0x005b
            java.lang.String r1 = "install"
            goto L_0x005d
        L_0x005b:
            java.lang.String r1 = "streaming dev"
        L_0x005d:
            r0.append(r1)
            java.lang.String r1 = " directory "
            r0.append(r1)
            java.lang.String r1 = r9.getAbsolutePath()
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r8, r0)
            return r9
        L_0x0074:
            if (r2 == 0) goto L_0x009e
            java.io.File r7 = r1.getExternalFilesDir(r7)
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r10 = "Havezip wants to use "
            r9.append(r10)
            java.lang.String r10 = r7.getAbsolutePath()
            r9.append(r10)
            java.lang.String r9 = r9.toString()
            android.util.Log.i(r8, r9)
            boolean r9 = android.os.Environment.isExternalStorageEmulated(r7)
            if (r9 != 0) goto L_0x009e
            java.lang.String r0 = "Havezip favored directory is not emulated"
            android.util.Log.i(r8, r0)
            return r7
        L_0x009e:
            java.io.File r9 = android.os.Environment.getExternalStorageDirectory()
            boolean r10 = android.os.Environment.isExternalStorageEmulated(r9)
            boolean r11 = DeviceCanInstallToSD()
            if (r11 == 0) goto L_0x00f5
            java.lang.String r11 = "bTrySD"
            android.util.Log.i(r8, r11)
            if (r10 == 0) goto L_0x00f5
            java.lang.String r11 = "bPublicBaseIsEmulated"
            android.util.Log.i(r8, r11)
            java.lang.String r11 = "checking getExternalFilesDirs"
            android.util.Log.i(r8, r11)
            java.lang.String r11 = "external"
            java.io.File[] r11 = r1.getExternalFilesDirs(r11)
            java.io.File r11 = FindNonEmulatedDirectoryInList(r11)
            if (r11 != 0) goto L_0x00d7
            java.lang.String r11 = "checking media dirs"
            android.util.Log.i(r8, r11)
            java.io.File[] r1 = r1.getExternalMediaDirs()
            java.io.File r1 = FindNonEmulatedDirectoryInList(r1)
            goto L_0x00d8
        L_0x00d7:
            r1 = r11
        L_0x00d8:
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r12 = "non emulated dir result "
            r11.append(r12)
            if (r1 == 0) goto L_0x00e6
            r12 = r1
            goto L_0x00e8
        L_0x00e6:
            java.lang.String r12 = "[null]"
        L_0x00e8:
            r11.append(r12)
            java.lang.String r11 = r11.toString()
            android.util.Log.i(r8, r11)
            if (r1 == 0) goto L_0x00f5
            goto L_0x00f7
        L_0x00f5:
            r1 = r9
            r4 = r10
        L_0x00f7:
            if (r2 == 0) goto L_0x012d
            if (r4 == 0) goto L_0x0112
            if (r7 == 0) goto L_0x0112
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "everything is emulated, using the favored zip directory "
            r0.append(r1)
            r0.append(r7)
            java.lang.String r0 = r0.toString()
            android.util.Log.i(r8, r0)
            goto L_0x0134
        L_0x0112:
            java.io.File r7 = new java.io.File
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r8 = "com.valvesoftware/"
            r4.append(r8)
            java.lang.String r0 = r0.getPackageName()
            r4.append(r0)
            java.lang.String r0 = r4.toString()
            r7.<init>(r1, r0)
            goto L_0x0134
        L_0x012d:
            java.io.File r7 = new java.io.File
            java.lang.String r0 = "com.valvesoftware/dev/source2/main"
            r7.<init>(r1, r0)
        L_0x0134:
            if (r2 == 0) goto L_0x0137
            goto L_0x0138
        L_0x0137:
            r5 = r6
        L_0x0138:
            java.io.File r0 = r7.getAbsoluteFile()     // Catch:{ Throwable -> 0x0142 }
            r3.put(r5, r0)     // Catch:{ Throwable -> 0x0142 }
            com.valvesoftware.Configuration.MarkGlobalConfigurationDirty()     // Catch:{ Throwable -> 0x0142 }
        L_0x0142:
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.SelfInstall.GetContentDirectory():java.io.File");
    }

    public static boolean ShouldSyncContentFromBootstrap(IStreamingBootStrap iStreamingBootStrap) {
        if (iStreamingBootStrap == null) {
            return false;
        }
        return !HasGameZip();
    }

    private static boolean testUnzip(InputStream inputStream, String str) throws IOException {
        boolean z = false;
        try {
            if (!new File(str).exists()) {
                return false;
            }
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            boolean z2 = false;
            boolean z3 = false;
            while (true) {
                if (nextEntry == null) {
                    break;
                }
                String replace = nextEntry.getName().replace("\\", "/");
                StringBuilder sb = new StringBuilder();
                sb.append(str);
                sb.append(File.separator);
                sb.append(replace);
                String sb2 = sb.toString();
                boolean isDirectory = nextEntry.isDirectory();
                if (nextEntry.getName().charAt(nextEntry.getName().length() - 1) == '\\') {
                    isDirectory = true;
                }
                if (!isDirectory) {
                    if (new File(sb2).exists()) {
                        z2 = true;
                    }
                    z3 = true;
                }
                zipInputStream.closeEntry();
                if (z2 || z3) {
                    break;
                }
                nextEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
            z = z2;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("testunzip ");
            sb3.append(z);
            Log.e("com.valvesoftware.SelfInstall", sb3.toString());
            return z;
        } catch (Throwable unused) {
        }
    }

    private static boolean isAlreadyUnpacked(InputStream inputStream, String str) throws IOException {
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() == 0) {
            return testUnzip(inputStream, str);
        }
        boolean z = false;
        try {
            if (!new File(str).exists()) {
                return false;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(File.separator);
            sb.append(GetString);
            if (new File(sb.toString()).exists()) {
                z = true;
            }
            return z;
        } catch (Throwable unused) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("isAlreadyUnpacked ");
            sb2.append(false);
            Log.e("com.valvesoftware.SelfInstall", sb2.toString());
            return false;
        }
    }

    private static void writeVersionFile(String str) throws IOException {
        String str2 = "com.valvesoftware.SelfInstall";
        String GetString = Resources.GetString("VersionCodeString");
        if (!(GetString == null || GetString.length() == 0)) {
            String str3 = "nofile";
            try {
                if (!new File(str).exists()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Could not find ");
                    sb.append(str);
                    Log.e(str2, sb.toString());
                    return;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(File.separator);
                sb2.append(GetString);
                String sb3 = sb2.toString();
                StringBuilder sb4 = new StringBuilder();
                sb4.append("DAC installed version: ");
                sb4.append(GetString);
                String sb5 = sb4.toString();
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(sb3));
                bufferedWriter.write(sb5);
                bufferedWriter.close();
                StringBuilder sb6 = new StringBuilder();
                sb6.append("Wrote version file ");
                sb6.append(sb3);
                Log.e(str2, sb6.toString());
            } catch (Throwable unused) {
                StringBuilder sb7 = new StringBuilder();
                sb7.append("Could not write ");
                sb7.append(str3);
                Log.e(str2, sb7.toString());
            }
        }
    }

    private static void unzip(InputStream inputStream, String str) throws IOException {
        File file = new File(str);
        if (!file.exists()) {
            file.mkdirs();
            file.mkdir();
        }
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        for (ZipEntry nextEntry = zipInputStream.getNextEntry(); nextEntry != null; nextEntry = zipInputStream.getNextEntry()) {
            String replace = nextEntry.getName().replace("\\", File.separator);
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(File.separator);
            sb.append(replace);
            String sb2 = sb.toString();
            boolean isDirectory = nextEntry.isDirectory();
            if (nextEntry.getName().charAt(nextEntry.getName().length() - 1) == '\\') {
                isDirectory = true;
            }
            if (!isDirectory) {
                new File(sb2.substring(0, sb2.lastIndexOf(File.separator))).mkdirs();
                extractFile(zipInputStream, sb2);
            } else {
                File file2 = new File(sb2);
                if (!file2.exists()) {
                    file2.mkdirs();
                    file2.mkdir();
                } else if (!file2.isDirectory()) {
                    file2.delete();
                    file2.mkdir();
                }
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
    }

    private static void extractFile(ZipInputStream zipInputStream, String str) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(str));
        byte[] bArr = new byte[4096];
        while (true) {
            int read = zipInputStream.read(bArr);
            if (read == -1) {
                break;
            }
            bufferedOutputStream.write(bArr, 0, read);
        }
        bufferedOutputStream.close();
        counter++;
        if (counter > 10) {
            StringBuilder sb = new StringBuilder();
            sb.append("extracting ");
            sb.append(str);
            Log.i("com.valvesoftware.SelfInstall", sb.toString());
            counter = 0;
        }
    }

    private static File FindExpansionFile() {
        Application application = JNI_Environment.m_application;
        String packageName = application.getApplicationContext().getPackageName();
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() == 0) {
            GetString = "0000001";
        }
        String str = "com.valvesoftware.SelfInstall";
        if (Environment.getExternalStorageState().equals("mounted")) {
            File obbDir = application.getApplicationContext().getObbDir();
            StringBuilder sb = new StringBuilder();
            sb.append("Looking for ObbFilePath: ");
            sb.append(obbDir.toString());
            Log.i(str, sb.toString());
            if (obbDir.exists()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(obbDir.toString());
                sb2.append(File.separator);
                sb2.append("main.");
                sb2.append(GetString);
                sb2.append(".");
                sb2.append(packageName);
                sb2.append(".obb");
                File file = new File(sb2.toString());
                if (file.canRead()) {
                    return file;
                }
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Can't read expansion file: ");
                sb3.append(file.toString());
                Log.i(str, sb3.toString());
                return null;
            }
        } else {
            Log.e(str, "ExternalStorage not mounted!");
        }
        return null;
    }

    private static InputStream GetAssetZipFile() {
        File FindExpansionFile = FindExpansionFile();
        String str = "com.valvesoftware.SelfInstall";
        if (FindExpansionFile == null) {
            return JNI_Environment.m_application.getApplicationContext().getAssets().open("game01.zip");
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(FindExpansionFile);
            StringBuilder sb = new StringBuilder();
            sb.append("Found OBB file ");
            sb.append(FindExpansionFile.toString());
            Log.e(str, sb.toString());
            return fileInputStream;
        } catch (FileNotFoundException unused) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("game01.zip or ");
            sb2.append(FindExpansionFile);
            sb2.append(" not found, ok. ");
            Log.e(str, sb2.toString());
            return null;
        } catch (Throwable th) {
            th.printStackTrace();
            StringBuilder sb3 = new StringBuilder();
            sb3.append("game01.zip exception");
            sb3.append(th.getMessage());
            Log.e(str, sb3.toString());
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x00b0  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00e1 A[Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }] */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00e0 A[EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00e0 A[EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00e0 A[EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  , SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00e0 A[EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  
    EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  , SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean CheckZippedAssets() {
        /*
            java.lang.String r0 = " files to "
            java.lang.String r1 = " exception"
            java.io.File r2 = com.valvesoftware.JNI_Environment.GetPublicPath()
            java.lang.String r2 = r2.getAbsolutePath()
            android.app.Application r3 = com.valvesoftware.JNI_Environment.m_application
            java.lang.String r4 = "com.valvesoftware.SelfInstall"
            java.lang.String r5 = "Checking for game01.zip asset."
            android.util.Log.e(r4, r5)
            android.content.Context r3 = r3.getApplicationContext()
            android.content.res.AssetManager r3 = r3.getAssets()
            r5 = 0
            java.io.InputStream r6 = GetAssetZipFile()     // Catch:{ FileNotFoundException -> 0x0094, Throwable -> 0x0076 }
            boolean r7 = isAlreadyUnpacked(r6, r2)     // Catch:{ FileNotFoundException -> 0x0094, Throwable -> 0x0076 }
            if (r7 == 0) goto L_0x003d
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r8.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r9 = "Already unzipped files "
            r8.append(r9)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r8.append(r2)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r8 = r8.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r4, r8)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            goto L_0x006f
        L_0x003d:
            r6.close()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r6.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r8 = "Starting unzip files to "
            r6.append(r8)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r6.append(r2)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r6 = r6.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r4, r6)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.io.InputStream r6 = GetAssetZipFile()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            unzip(r6, r2)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r8.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r9 = "Unzipped files to "
            r8.append(r9)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r8.append(r2)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r8 = r8.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r4, r8)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
        L_0x006f:
            r6.close()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r5 = 1
            goto L_0x009a
        L_0x0074:
            r6 = move-exception
            goto L_0x0078
        L_0x0076:
            r6 = move-exception
            r7 = 0
        L_0x0078:
            r6.printStackTrace()
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = "game01.zip exception"
            r8.append(r9)
            java.lang.String r6 = r6.getMessage()
            r8.append(r6)
            java.lang.String r6 = r8.toString()
            android.util.Log.e(r4, r6)
            goto L_0x009a
        L_0x0094:
            r7 = 0
        L_0x0095:
            java.lang.String r6 = "game01.zip not found, ok. "
            android.util.Log.e(r4, r6)
        L_0x009a:
            if (r5 == 0) goto L_0x0171
            if (r7 != 0) goto L_0x0171
            r6 = 2
        L_0x009f:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "game"
            r7.append(r8)
            r8 = 10
            if (r6 >= r8) goto L_0x00b0
            java.lang.String r8 = "0"
            goto L_0x00b2
        L_0x00b0:
            java.lang.String r8 = ""
        L_0x00b2:
            r7.append(r8)
            r7.append(r6)
            java.lang.String r8 = ".zip"
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = "Checking for "
            r8.append(r9)
            r8.append(r7)
            java.lang.String r9 = " asset."
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            android.util.Log.e(r4, r8)
            java.io.InputStream r8 = r3.open(r7)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            if (r8 != 0) goto L_0x00e1
            goto L_0x0150
        L_0x00e1:
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.<init>()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r10 = "Starting unzip "
            r9.append(r10)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r7)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r0)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r2)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r9 = r9.toString()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            android.util.Log.e(r4, r9)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            unzip(r8, r2)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.<init>()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r10 = "Unzipped "
            r9.append(r10)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r7)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r0)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r9.append(r2)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r9 = r9.toString()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            android.util.Log.e(r4, r9)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.close()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            int r6 = r6 + 1
            goto L_0x009f
        L_0x011e:
            r0 = move-exception
            r0.printStackTrace()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r7)
            r3.append(r1)
            java.lang.String r0 = r0.getMessage()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.e(r4, r0)
            goto L_0x0150
        L_0x013c:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            r0.append(r7)
            java.lang.String r3 = " not found, ok. "
            r0.append(r3)
            java.lang.String r0 = r0.toString()
            android.util.Log.e(r4, r0)
        L_0x0150:
            writeVersionFile(r2)     // Catch:{ Throwable -> 0x0154 }
            goto L_0x0171
        L_0x0154:
            r0 = move-exception
            r0.printStackTrace()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            r3.append(r2)
            r3.append(r1)
            java.lang.String r0 = r0.getMessage()
            r3.append(r0)
            java.lang.String r0 = r3.toString()
            android.util.Log.e(r4, r0)
        L_0x0171:
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.SelfInstall.CheckZippedAssets():boolean");
    }

    private static File FindNonEmulatedDirectoryInList(File[] fileArr) {
        for (int i = 0; i < fileArr.length; i++) {
            if (fileArr[i] != null && !Environment.isExternalStorageEmulated(fileArr[i]) && fileArr[i].exists()) {
                return fileArr[i];
            }
        }
        return null;
    }

    private static boolean DeviceCanInstallToSD() {
        return !Build.MANUFACTURER.equals("NVIDIA") || !Build.DEVICE.equals("shieldtablet");
    }
}
