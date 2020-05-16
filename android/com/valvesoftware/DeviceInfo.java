package com.valvesoftware;

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
        try {
            DeviceInfo PopulateDeviceInfo = PopulateDeviceInfo(jSONObject);
            if (!jSONObject.has("exclusions")) {
                return PopulateDeviceInfo;
            }
            PopulateDeviceInfo.m_exclusionList = new ArrayList<>();
            JSONArray jSONArray = jSONObject.getJSONArray("exclusions");
            for (int i = 0; i < jSONArray.length(); i++) {
                DeviceInfo PopulateDeviceInfo2 = PopulateDeviceInfo(jSONArray.getJSONObject(i));
                if (PopulateDeviceInfo2 != null) {
                    PopulateDeviceInfo.m_exclusionList.add(PopulateDeviceInfo2);
                }
            }
            return PopulateDeviceInfo;
        } catch (Exception e) {
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", "Exception populating DeviceInfo: " + e.toString());
            return null;
        }
    }

    public static DeviceInfo PopulateDeviceInfo(JSONObject jSONObject) {
        try {
            DeviceInfo deviceInfo = new DeviceInfo();
            if (jSONObject.has("min_os")) {
                deviceInfo.m_nMinOS = jSONObject.getInt("min_os");
            }
            if (jSONObject.has("max_os")) {
                deviceInfo.m_nMaxOS = jSONObject.getInt("max_os");
            }
            if (jSONObject.has("renderer")) {
                deviceInfo.m_sRenderer = jSONObject.getString("renderer");
            }
            if (jSONObject.has("min_driver_version")) {
                deviceInfo.m_sMinDriverVersion = jSONObject.getString("min_driver_version");
            }
            if (jSONObject.has("max_driver_version")) {
                deviceInfo.m_sMaxDriverVersion = jSONObject.getString("max_driver_version");
            }
            if (!jSONObject.has("device_name")) {
                return deviceInfo;
            }
            deviceInfo.m_sDeviceName = jSONObject.getString("device_name");
            return deviceInfo;
        } catch (Exception e) {
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", "Exception parsing DeviceInfo JSON: " + e.toString());
            return null;
        }
    }

    public static boolean DevicesCompatible(DeviceInfo deviceInfo, DeviceInfo deviceInfo2) {
        String str;
        String str2;
        if (deviceInfo.m_nMinOS < deviceInfo2.m_nMinOS || deviceInfo.m_nMaxOS > deviceInfo2.m_nMaxOS) {
            return false;
        }
        String str3 = deviceInfo.m_sRenderer;
        if (str3 != null && (str2 = deviceInfo2.m_sRenderer) != null && !str3.equalsIgnoreCase(str2)) {
            return false;
        }
        String str4 = deviceInfo.m_sDeviceName;
        if (str4 != null && (str = deviceInfo2.m_sDeviceName) != null && !str4.equalsIgnoreCase(str)) {
            return false;
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
                if (DeviceExcluded(deviceInfo, deviceInfo2.m_exclusionList.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean DeviceExcluded(DeviceInfo deviceInfo, DeviceInfo deviceInfo2) {
        String str;
        String str2;
        int i;
        int i2 = deviceInfo2.m_nMinOS;
        if (i2 <= 0 || (i = deviceInfo2.m_nMaxOS) >= MAX_OS_VERSION) {
            int i3 = deviceInfo2.m_nMinOS;
            if (i3 > 0) {
                if (deviceInfo.m_nMinOS >= i3) {
                    return true;
                }
            } else if (deviceInfo2.m_nMaxOS < MAX_OS_VERSION && deviceInfo.m_nMaxOS <= i3) {
                return true;
            }
        } else if (deviceInfo.m_nMinOS >= i2 && deviceInfo.m_nMaxOS <= i) {
            return true;
        }
        String str3 = deviceInfo.m_sRenderer;
        if (str3 != null && (str2 = deviceInfo2.m_sRenderer) != null && str3.equalsIgnoreCase(str2)) {
            return true;
        }
        String str4 = deviceInfo.m_sDeviceName;
        if (str4 != null && (str = deviceInfo2.m_sDeviceName) != null && str4.equalsIgnoreCase(str)) {
            return true;
        }
        if (!(deviceInfo.m_sMinDriverVersion == null || (deviceInfo2.m_sMinDriverVersion == null && deviceInfo2.m_sMaxDriverVersion == null))) {
            int[] ParseDriverVersion = ParseDriverVersion(deviceInfo.m_sMinDriverVersion);
            String str5 = deviceInfo2.m_sMinDriverVersion;
            if (str5 != null) {
                int[] ParseDriverVersion2 = ParseDriverVersion(str5);
                if (ParseDriverVersion2[0] == 0) {
                    return true;
                }
                if (ParseDriverVersion[0] >= ParseDriverVersion2[0] || (ParseDriverVersion[0] == ParseDriverVersion2[0] && ParseDriverVersion[1] >= ParseDriverVersion2[1])) {
                    String str6 = deviceInfo2.m_sMaxDriverVersion;
                    if (str6 == null) {
                        return true;
                    }
                    int[] ParseDriverVersion3 = ParseDriverVersion(str6);
                    if (ParseDriverVersion3[0] != 0 && ParseDriverVersion[0] > ParseDriverVersion3[0] && (ParseDriverVersion[1] != ParseDriverVersion3[1] || ParseDriverVersion[1] > ParseDriverVersion3[1])) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] ParseDriverVersion(String str) {
        int[] iArr = {0, 0};
        Matcher matcher = Pattern.compile(".*V@([0-9]+)[.]([0-9]+).*").matcher(str);
        Matcher matcher2 = Pattern.compile(".*[.][r]([0-9]+)p([0-9])+[-].*").matcher(str);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            iArr[0] = Integer.parseInt(matcher.group(1));
            iArr[1] = Integer.parseInt(matcher.group(2));
        } else if (!matcher2.matches() || matcher2.groupCount() < 2) {
            Log.e("com.valvesoftware.source2launcher.DeviceInfo", "Could not parse driver version string: " + str);
        } else {
            iArr[0] = Integer.parseInt(matcher2.group(1));
            iArr[1] = Integer.parseInt(matcher2.group(2));
        }
        return iArr;
    }
}
