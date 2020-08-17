package com.valvesoftware;

import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.valvesoftware.Application;
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

    public static boolean OnStartup(Application.InstallTask installTask) {
        return CheckZippedAssets(installTask);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:81:0x014e, code lost:
        if (r1 != null) goto L_0x0153;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing exception handler attribute for start block: B:99:0x01cd */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.io.File GetContentDirectory(java.lang.String r14, java.lang.String r15) {
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
            org.json.JSONObject r5 = com.valvesoftware.Configuration.GetGlobalKey(r5, r3)
            java.lang.String r6 = "ConfigVersion"
            int r7 = r5.optInt(r6, r4)
            java.lang.String r8 = "Install"
            r9 = 0
            if (r7 >= r3) goto L_0x005b
            java.lang.String r7 = "ZipInstall"
            java.lang.String r10 = r5.optString(r7, r9)
            if (r10 == 0) goto L_0x0042
            r5.put((java.lang.String) r8, (java.lang.Object) r10)     // Catch:{ Throwable -> 0x003f }
        L_0x003f:
            r5.remove(r7)
        L_0x0042:
            java.lang.String r7 = "StreamingDev"
            java.lang.String r10 = r5.optString(r7, r9)
            if (r10 == 0) goto L_0x005b
            java.io.File r11 = new java.io.File
            r11.<init>(r10)
            boolean r10 = r11.exists()
            if (r10 == 0) goto L_0x0058
            r11.delete()
        L_0x0058:
            r5.remove(r7)
        L_0x005b:
            r5.put((java.lang.String) r6, (int) r3)     // Catch:{ Throwable -> 0x005e }
        L_0x005e:
            com.valvesoftware.Configuration.MarkGlobalConfigurationDirty()
            if (r2 != 0) goto L_0x007b
            if (r14 == 0) goto L_0x007a
            if (r15 != 0) goto L_0x0068
            goto L_0x007a
        L_0x0068:
            org.json.JSONObject r3 = r5.optJSONObject(r15)
            if (r3 != 0) goto L_0x007c
            org.json.JSONObject r3 = new org.json.JSONObject
            r3.<init>()
            r5.put((java.lang.String) r15, (java.lang.Object) r3)     // Catch:{ Throwable -> 0x0076 }
        L_0x0076:
            com.valvesoftware.Configuration.MarkGlobalConfigurationDirty()
            goto L_0x007c
        L_0x007a:
            return r9
        L_0x007b:
            r3 = r9
        L_0x007c:
            java.lang.String r6 = "Branches"
            if (r2 == 0) goto L_0x0085
            java.lang.String r7 = r5.optString(r8, r9)
            goto L_0x0095
        L_0x0085:
            if (r3 == 0) goto L_0x0094
            if (r14 == 0) goto L_0x0094
            org.json.JSONObject r7 = r3.optJSONObject(r6)
            if (r7 == 0) goto L_0x0094
            java.lang.String r7 = r7.optString(r14, r9)
            goto L_0x0095
        L_0x0094:
            r7 = r9
        L_0x0095:
            if (r7 == 0) goto L_0x009d
            java.io.File r10 = new java.io.File
            r10.<init>(r7)
            goto L_0x009e
        L_0x009d:
            r10 = r9
        L_0x009e:
            java.lang.String r7 = "com.valvesoftware.SelfInstall"
            if (r10 == 0) goto L_0x00d0
            boolean r11 = r10.exists()
            if (r11 == 0) goto L_0x00d0
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            java.lang.String r15 = "Re-using previous "
            r14.append(r15)
            if (r2 == 0) goto L_0x00b7
            java.lang.String r15 = "install"
            goto L_0x00b9
        L_0x00b7:
            java.lang.String r15 = "streaming dev"
        L_0x00b9:
            r14.append(r15)
            java.lang.String r15 = " directory "
            r14.append(r15)
            java.lang.String r15 = r10.getAbsolutePath()
            r14.append(r15)
            java.lang.String r14 = r14.toString()
            android.util.Log.i(r7, r14)
            return r10
        L_0x00d0:
            if (r2 == 0) goto L_0x00fa
            java.io.File r9 = r1.getExternalFilesDir(r9)
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = "Install wants to use "
            r10.append(r11)
            java.lang.String r11 = r9.getAbsolutePath()
            r10.append(r11)
            java.lang.String r10 = r10.toString()
            android.util.Log.i(r7, r10)
            boolean r10 = android.os.Environment.isExternalStorageEmulated(r9)
            if (r10 != 0) goto L_0x00fa
            java.lang.String r14 = "Install favored directory is not emulated"
            android.util.Log.i(r7, r14)
            return r9
        L_0x00fa:
            java.io.File r10 = android.os.Environment.getExternalStorageDirectory()
            boolean r11 = android.os.Environment.isExternalStorageEmulated(r10)
            boolean r12 = DeviceCanInstallToSD()
            if (r12 == 0) goto L_0x0151
            java.lang.String r12 = "bTrySD"
            android.util.Log.i(r7, r12)
            if (r11 == 0) goto L_0x0151
            java.lang.String r12 = "bPublicBaseIsEmulated"
            android.util.Log.i(r7, r12)
            java.lang.String r12 = "checking getExternalFilesDirs"
            android.util.Log.i(r7, r12)
            java.lang.String r12 = "external"
            java.io.File[] r12 = r1.getExternalFilesDirs(r12)
            java.io.File r12 = FindNonEmulatedDirectoryInList(r12)
            if (r12 != 0) goto L_0x0133
            java.lang.String r12 = "checking media dirs"
            android.util.Log.i(r7, r12)
            java.io.File[] r1 = r1.getExternalMediaDirs()
            java.io.File r1 = FindNonEmulatedDirectoryInList(r1)
            goto L_0x0134
        L_0x0133:
            r1 = r12
        L_0x0134:
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "non emulated dir result "
            r12.append(r13)
            if (r1 == 0) goto L_0x0142
            r13 = r1
            goto L_0x0144
        L_0x0142:
            java.lang.String r13 = "[null]"
        L_0x0144:
            r12.append(r13)
            java.lang.String r12 = r12.toString()
            android.util.Log.i(r7, r12)
            if (r1 == 0) goto L_0x0151
            goto L_0x0153
        L_0x0151:
            r1 = r10
            r4 = r11
        L_0x0153:
            if (r2 == 0) goto L_0x0193
            if (r4 == 0) goto L_0x016e
            if (r9 == 0) goto L_0x016e
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            java.lang.String r15 = "everything is emulated, using the favored zip directory "
            r14.append(r15)
            r14.append(r9)
            java.lang.String r14 = r14.toString()
            android.util.Log.i(r7, r14)
            goto L_0x0188
        L_0x016e:
            java.io.File r9 = new java.io.File
            java.lang.StringBuilder r14 = new java.lang.StringBuilder
            r14.<init>()
            java.lang.String r15 = "com.valvesoftware/"
            r14.append(r15)
            java.lang.String r15 = r0.getPackageName()
            r14.append(r15)
            java.lang.String r14 = r14.toString()
            r9.<init>(r1, r14)
        L_0x0188:
            java.io.File r14 = r9.getAbsoluteFile()     // Catch:{ Throwable -> 0x018f }
            r5.put((java.lang.String) r8, (java.lang.Object) r14)     // Catch:{ Throwable -> 0x018f }
        L_0x018f:
            com.valvesoftware.Configuration.MarkGlobalConfigurationDirty()
            goto L_0x01d7
        L_0x0193:
            java.lang.String r0 = "_"
            java.lang.String r2 = "[^a-zA-Z0-9.-]"
            java.lang.String r15 = r15.replaceAll(r2, r0)
            java.lang.String r0 = r14.replaceAll(r2, r0)
            java.io.File r9 = new java.io.File
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r4 = "com.valvesoftware/dev/"
            r2.append(r4)
            r2.append(r15)
            java.lang.String r15 = "/"
            r2.append(r15)
            r2.append(r0)
            java.lang.String r15 = r2.toString()
            r9.<init>(r1, r15)
            if (r3 == 0) goto L_0x01d4
            org.json.JSONObject r15 = r3.optJSONObject(r6)
            if (r15 != 0) goto L_0x01cd
            org.json.JSONObject r15 = new org.json.JSONObject
            r15.<init>()
            r3.put((java.lang.String) r6, (java.lang.Object) r15)     // Catch:{ Throwable -> 0x01cd }
        L_0x01cd:
            java.lang.String r0 = r9.toString()     // Catch:{ Throwable -> 0x01d4 }
            r15.put((java.lang.String) r14, (java.lang.Object) r0)     // Catch:{ Throwable -> 0x01d4 }
        L_0x01d4:
            com.valvesoftware.Configuration.MarkGlobalConfigurationDirty()
        L_0x01d7:
            return r9
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.SelfInstall.GetContentDirectory(java.lang.String, java.lang.String):java.io.File");
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
                String str2 = str + File.separator + nextEntry.getName().replace("\\", "/");
                boolean isDirectory = nextEntry.isDirectory();
                if (nextEntry.getName().charAt(nextEntry.getName().length() - 1) == '\\') {
                    isDirectory = true;
                }
                if (!isDirectory) {
                    if (new File(str2).exists()) {
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
            Log.e("com.valvesoftware.SelfInstall", "testunzip " + z);
            return z;
        } catch (Throwable unused) {
        }
    }

    private static boolean isAlreadyUnpacked(InputStream inputStream, String str) throws IOException {
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() == 0) {
            return testUnzip(inputStream, str);
        }
        try {
            if (!new File(str).exists()) {
                return false;
            }
            if (new File(str + File.separator + GetString).exists()) {
                return true;
            }
            return false;
        } catch (Throwable unused) {
            Log.e("com.valvesoftware.SelfInstall", "isAlreadyUnpacked " + false);
            return false;
        }
    }

    private static void writeVersionFile(String str) throws IOException {
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString != null && GetString.length() != 0) {
            try {
                if (!new File(str).exists()) {
                    Log.e("com.valvesoftware.SelfInstall", "Could not find " + str);
                    return;
                }
                String str2 = str + File.separator + GetString;
                String str3 = "DAC installed version: " + GetString;
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(str2));
                bufferedWriter.write(str3);
                bufferedWriter.close();
                Log.e("com.valvesoftware.SelfInstall", "Wrote version file " + str2);
            } catch (Throwable unused) {
                Log.e("com.valvesoftware.SelfInstall", "Could not write " + "nofile");
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
            String str2 = str + File.separator + nextEntry.getName().replace("\\", File.separator);
            boolean isDirectory = nextEntry.isDirectory();
            if (nextEntry.getName().charAt(nextEntry.getName().length() - 1) == '\\') {
                isDirectory = true;
            }
            if (!isDirectory) {
                new File(str2.substring(0, str2.lastIndexOf(File.separator))).mkdirs();
                extractFile(zipInputStream, str2);
            } else {
                File file2 = new File(str2);
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
            Log.i("com.valvesoftware.SelfInstall", "extracting " + str);
            counter = 0;
        }
    }

    private static File FindExpansionFile() {
        android.app.Application application = JNI_Environment.m_application;
        String packageName = application.getApplicationContext().getPackageName();
        String GetString = Resources.GetString("VersionCodeString");
        if (GetString == null || GetString.length() == 0) {
            GetString = "0000001";
        }
        if (Environment.getExternalStorageState().equals("mounted")) {
            File obbDir = application.getApplicationContext().getObbDir();
            Log.i("com.valvesoftware.SelfInstall", "Looking for ObbFilePath: " + obbDir.toString());
            if (obbDir.exists()) {
                File file = new File(obbDir.toString() + File.separator + "main." + GetString + "." + packageName + ".obb");
                if (file.canRead()) {
                    return file;
                }
                Log.i("com.valvesoftware.SelfInstall", "Can't read expansion file: " + file.toString());
                return null;
            }
        } else {
            Log.e("com.valvesoftware.SelfInstall", "ExternalStorage not mounted!");
        }
        return null;
    }

    private static InputStream GetAssetZipFile() {
        File FindExpansionFile = FindExpansionFile();
        if (FindExpansionFile == null) {
            return JNI_Environment.m_application.getApplicationContext().getAssets().open("game01.zip");
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(FindExpansionFile);
            Log.e("com.valvesoftware.SelfInstall", "Found OBB file " + FindExpansionFile.toString());
            return fileInputStream;
        } catch (FileNotFoundException unused) {
            Log.e("com.valvesoftware.SelfInstall", "game01.zip or " + FindExpansionFile + " not found, ok. ");
            return null;
        } catch (Throwable th) {
            th.printStackTrace();
            Log.e("com.valvesoftware.SelfInstall", "game01.zip exception" + th.getMessage());
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x00b0  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00e1 A[Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }] */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00e0 A[EDGE_INSN: B:40:0x00e0->B:28:0x00e0 ?: BREAK  , SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean CheckZippedAssets(com.valvesoftware.Application.InstallTask r10) {
        /*
            java.lang.String r10 = " files to "
            java.lang.String r0 = " exception"
            java.io.File r1 = com.valvesoftware.JNI_Environment.GetPublicPath()
            java.lang.String r1 = r1.getAbsolutePath()
            android.app.Application r2 = com.valvesoftware.JNI_Environment.m_application
            java.lang.String r3 = "com.valvesoftware.SelfInstall"
            java.lang.String r4 = "Checking for game01.zip asset."
            android.util.Log.e(r3, r4)
            android.content.Context r2 = r2.getApplicationContext()
            android.content.res.AssetManager r2 = r2.getAssets()
            r4 = 0
            java.io.InputStream r5 = GetAssetZipFile()     // Catch:{ FileNotFoundException -> 0x0094, Throwable -> 0x0076 }
            boolean r6 = isAlreadyUnpacked(r5, r1)     // Catch:{ FileNotFoundException -> 0x0094, Throwable -> 0x0076 }
            if (r6 == 0) goto L_0x003d
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r7.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r8 = "Already unzipped files "
            r7.append(r8)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r7.append(r1)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r7 = r7.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r3, r7)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            goto L_0x006f
        L_0x003d:
            r5.close()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r5.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r7 = "Starting unzip files to "
            r5.append(r7)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r5.append(r1)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r5 = r5.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r3, r5)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.io.InputStream r5 = GetAssetZipFile()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            unzip(r5, r1)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r7.<init>()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r8 = "Unzipped files to "
            r7.append(r8)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r7.append(r1)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            java.lang.String r7 = r7.toString()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            android.util.Log.e(r3, r7)     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
        L_0x006f:
            r5.close()     // Catch:{ FileNotFoundException -> 0x0095, Throwable -> 0x0074 }
            r4 = 1
            goto L_0x009a
        L_0x0074:
            r5 = move-exception
            goto L_0x0078
        L_0x0076:
            r5 = move-exception
            r6 = 0
        L_0x0078:
            r5.printStackTrace()
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "game01.zip exception"
            r7.append(r8)
            java.lang.String r5 = r5.getMessage()
            r7.append(r5)
            java.lang.String r5 = r7.toString()
            android.util.Log.e(r3, r5)
            goto L_0x009a
        L_0x0094:
            r6 = 0
        L_0x0095:
            java.lang.String r5 = "game01.zip not found, ok. "
            android.util.Log.e(r3, r5)
        L_0x009a:
            if (r4 == 0) goto L_0x0171
            if (r6 != 0) goto L_0x0171
            r5 = 2
        L_0x009f:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "game"
            r6.append(r7)
            r7 = 10
            if (r5 >= r7) goto L_0x00b0
            java.lang.String r7 = "0"
            goto L_0x00b2
        L_0x00b0:
            java.lang.String r7 = ""
        L_0x00b2:
            r6.append(r7)
            r6.append(r5)
            java.lang.String r7 = ".zip"
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "Checking for "
            r7.append(r8)
            r7.append(r6)
            java.lang.String r8 = " asset."
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Log.e(r3, r7)
            java.io.InputStream r7 = r2.open(r6)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            if (r7 != 0) goto L_0x00e1
            goto L_0x0150
        L_0x00e1:
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.<init>()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r9 = "Starting unzip "
            r8.append(r9)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r6)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r10)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r1)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r8 = r8.toString()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            android.util.Log.e(r3, r8)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            unzip(r7, r1)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.<init>()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r9 = "Unzipped "
            r8.append(r9)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r6)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r10)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r8.append(r1)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            java.lang.String r8 = r8.toString()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            android.util.Log.e(r3, r8)     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            r7.close()     // Catch:{ FileNotFoundException -> 0x013c, Throwable -> 0x011e }
            int r5 = r5 + 1
            goto L_0x009f
        L_0x011e:
            r10 = move-exception
            r10.printStackTrace()
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r6)
            r2.append(r0)
            java.lang.String r10 = r10.getMessage()
            r2.append(r10)
            java.lang.String r10 = r2.toString()
            android.util.Log.e(r3, r10)
            goto L_0x0150
        L_0x013c:
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            r10.append(r6)
            java.lang.String r2 = " not found, ok. "
            r10.append(r2)
            java.lang.String r10 = r10.toString()
            android.util.Log.e(r3, r10)
        L_0x0150:
            writeVersionFile(r1)     // Catch:{ Throwable -> 0x0154 }
            goto L_0x0171
        L_0x0154:
            r10 = move-exception
            r10.printStackTrace()
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r2.append(r1)
            r2.append(r0)
            java.lang.String r10 = r10.getMessage()
            r2.append(r10)
            java.lang.String r10 = r2.toString()
            android.util.Log.e(r3, r10)
        L_0x0171:
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.SelfInstall.CheckZippedAssets(com.valvesoftware.Application$InstallTask):boolean");
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
