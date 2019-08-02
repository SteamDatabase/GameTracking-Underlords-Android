package com.valvesoftware.source2launcher;

import android.util.Log;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/* compiled from: VulkanWhitelist */
class DeviceInfo {
    private static final int MAX_OS_VERSION = 999999;
    private ArrayList<DeviceInfo> m_exclusionList = null;
    public int m_nMaxOS = MAX_OS_VERSION;
    public int m_nMinOS = 0;
    public String m_sDeviceName = null;
    public String m_sMaxDriverVersion = null;
    public String m_sMinDriverVersion = null;
    public String m_sRenderer = null;

    DeviceInfo() {
    }

    public static DeviceInfo PopulateFromJSON(JSONObject jSONObject) {
        String str = "exclusions";
        try {
            DeviceInfo PopulateDeviceInfo = PopulateDeviceInfo(jSONObject);
            if (!jSONObject.has(str)) {
                return PopulateDeviceInfo;
            }
            PopulateDeviceInfo.m_exclusionList = new ArrayList<>();
            JSONArray jSONArray = jSONObject.getJSONArray(str);
            for (int i = 0; i < jSONArray.length(); i++) {
                DeviceInfo PopulateDeviceInfo2 = PopulateDeviceInfo(jSONArray.getJSONObject(i));
                if (PopulateDeviceInfo2 != null) {
                    PopulateDeviceInfo.m_exclusionList.add(PopulateDeviceInfo2);
                }
            }
            return PopulateDeviceInfo;
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception populating DeviceInfo: ");
            sb.append(e.toString());
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", sb.toString());
            return null;
        }
    }

    public static DeviceInfo PopulateDeviceInfo(JSONObject jSONObject) {
        String str = "device_name";
        String str2 = "max_driver_version";
        String str3 = "min_driver_version";
        String str4 = "renderer";
        String str5 = "max_os";
        String str6 = "min_os";
        try {
            DeviceInfo deviceInfo = new DeviceInfo();
            if (jSONObject.has(str6)) {
                deviceInfo.m_nMinOS = jSONObject.getInt(str6);
            }
            if (jSONObject.has(str5)) {
                deviceInfo.m_nMaxOS = jSONObject.getInt(str5);
            }
            if (jSONObject.has(str4)) {
                deviceInfo.m_sRenderer = jSONObject.getString(str4);
            }
            if (jSONObject.has(str3)) {
                deviceInfo.m_sMinDriverVersion = jSONObject.getString(str3);
            }
            if (jSONObject.has(str2)) {
                deviceInfo.m_sMaxDriverVersion = jSONObject.getString(str2);
            }
            if (!jSONObject.has(str)) {
                return deviceInfo;
            }
            deviceInfo.m_sDeviceName = jSONObject.getString(str);
            return deviceInfo;
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception parsing DeviceInfo JSON: ");
            sb.append(e.toString());
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", sb.toString());
            return null;
        }
    }

    public static boolean DevicesCompatible(DeviceInfo deviceInfo, DeviceInfo deviceInfo2) {
        if (deviceInfo.m_nMinOS < deviceInfo2.m_nMinOS || deviceInfo.m_nMaxOS > deviceInfo2.m_nMaxOS) {
            return false;
        }
        String str = deviceInfo.m_sRenderer;
        if (str != null) {
            String str2 = deviceInfo2.m_sRenderer;
            if (str2 != null && !str.equalsIgnoreCase(str2)) {
                return false;
            }
        }
        String str3 = deviceInfo.m_sDeviceName;
        if (str3 != null) {
            String str4 = deviceInfo2.m_sDeviceName;
            if (str4 != null && !str3.equalsIgnoreCase(str4)) {
                return false;
            }
        }
        if (!(deviceInfo.m_sMinDriverVersion == null || (deviceInfo2.m_sMinDriverVersion == null && deviceInfo2.m_sMaxDriverVersion == null))) {
            int[] ParseDriverVersion = ParseDriverVersion(deviceInfo.m_sMinDriverVersion);
            if (ParseDriverVersion[0] == 0) {
                return false;
            }
            String str5 = deviceInfo2.m_sMinDriverVersion;
            if (str5 != null) {
                int[] ParseDriverVersion2 = ParseDriverVersion(str5);
                if (ParseDriverVersion2[0] == 0 || ParseDriverVersion[0] < ParseDriverVersion2[0] || (ParseDriverVersion[0] == ParseDriverVersion2[0] && ParseDriverVersion[1] < ParseDriverVersion2[1])) {
                    return false;
                }
            }
            String str6 = deviceInfo2.m_sMaxDriverVersion;
            if (str6 != null) {
                int[] ParseDriverVersion3 = ParseDriverVersion(str6);
                if (ParseDriverVersion3[0] == 0 || ParseDriverVersion[0] > ParseDriverVersion3[0] || (ParseDriverVersion[0] == ParseDriverVersion3[0] && ParseDriverVersion[1] > ParseDriverVersion3[1])) {
                    return false;
                }
            }
        }
        if (deviceInfo2.m_exclusionList != null) {
            for (int i = 0; i < deviceInfo2.m_exclusionList.size(); i++) {
                if (DeviceExcluded(deviceInfo, (DeviceInfo) deviceInfo2.m_exclusionList.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:21:0x002b  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x003a  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x005c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean DeviceExcluded(com.valvesoftware.source2launcher.DeviceInfo r5, com.valvesoftware.source2launcher.DeviceInfo r6) {
        /*
            int r0 = r6.m_nMinOS
            r1 = 999999(0xf423f, float:1.401297E-39)
            r2 = 1
            if (r0 <= 0) goto L_0x0015
            int r3 = r6.m_nMaxOS
            if (r3 >= r1) goto L_0x0015
            int r1 = r5.m_nMinOS
            if (r1 < r0) goto L_0x0027
            int r0 = r5.m_nMaxOS
            if (r0 > r3) goto L_0x0027
            return r2
        L_0x0015:
            int r0 = r6.m_nMinOS
            if (r0 <= 0) goto L_0x001e
            int r1 = r5.m_nMinOS
            if (r1 < r0) goto L_0x0027
            return r2
        L_0x001e:
            int r3 = r6.m_nMaxOS
            if (r3 >= r1) goto L_0x0027
            int r1 = r5.m_nMaxOS
            if (r1 > r0) goto L_0x0027
            return r2
        L_0x0027:
            java.lang.String r0 = r5.m_sRenderer
            if (r0 == 0) goto L_0x0036
            java.lang.String r1 = r6.m_sRenderer
            if (r1 == 0) goto L_0x0036
            boolean r0 = r0.equalsIgnoreCase(r1)
            if (r0 == 0) goto L_0x0036
            return r2
        L_0x0036:
            java.lang.String r0 = r5.m_sDeviceName
            if (r0 == 0) goto L_0x0045
            java.lang.String r1 = r6.m_sDeviceName
            if (r1 == 0) goto L_0x0045
            boolean r0 = r0.equalsIgnoreCase(r1)
            if (r0 == 0) goto L_0x0045
            return r2
        L_0x0045:
            java.lang.String r0 = r5.m_sMinDriverVersion
            r1 = 0
            if (r0 == 0) goto L_0x0098
            java.lang.String r0 = r6.m_sMinDriverVersion
            if (r0 != 0) goto L_0x0052
            java.lang.String r0 = r6.m_sMaxDriverVersion
            if (r0 == 0) goto L_0x0098
        L_0x0052:
            java.lang.String r5 = r5.m_sMinDriverVersion
            int[] r5 = ParseDriverVersion(r5)
            java.lang.String r0 = r6.m_sMinDriverVersion
            if (r0 == 0) goto L_0x0098
            int[] r0 = ParseDriverVersion(r0)
            r3 = r0[r1]
            if (r3 != 0) goto L_0x0065
            return r2
        L_0x0065:
            r3 = r5[r1]
            r4 = r0[r1]
            if (r3 >= r4) goto L_0x0077
            r3 = r5[r1]
            r4 = r0[r1]
            if (r3 != r4) goto L_0x0098
            r3 = r5[r2]
            r0 = r0[r2]
            if (r3 < r0) goto L_0x0098
        L_0x0077:
            java.lang.String r6 = r6.m_sMaxDriverVersion
            if (r6 != 0) goto L_0x007c
            return r2
        L_0x007c:
            int[] r6 = ParseDriverVersion(r6)
            r0 = r6[r1]
            if (r0 != 0) goto L_0x0085
            return r2
        L_0x0085:
            r0 = r5[r1]
            r3 = r6[r1]
            if (r0 <= r3) goto L_0x0097
            r0 = r5[r2]
            r3 = r6[r2]
            if (r0 != r3) goto L_0x0098
            r5 = r5[r2]
            r6 = r6[r2]
            if (r5 > r6) goto L_0x0098
        L_0x0097:
            return r2
        L_0x0098:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.source2launcher.DeviceInfo.DeviceExcluded(com.valvesoftware.source2launcher.DeviceInfo, com.valvesoftware.source2launcher.DeviceInfo):boolean");
    }

    private static int[] ParseDriverVersion(String str) {
        int[] iArr = {0, 0};
        Matcher matcher = Pattern.compile(".*V@([0-9]+)[.]([0-9]+).*").matcher(str);
        Matcher matcher2 = Pattern.compile(".*[.][r]([0-9]+)p([0-9])+[-].*").matcher(str);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            iArr[0] = Integer.parseInt(matcher.group(1));
            iArr[1] = Integer.parseInt(matcher.group(2));
        } else if (!matcher2.matches() || matcher2.groupCount() < 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Could not parse driver version string: ");
            sb.append(str);
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", sb.toString());
        } else {
            iArr[0] = Integer.parseInt(matcher2.group(1));
            iArr[1] = Integer.parseInt(matcher2.group(2));
        }
        return iArr;
    }
}
