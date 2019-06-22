package com.valvesoftware.underlords;

import com.valvesoftware.Resources;

public class application extends com.valvesoftware.source2launcher.application {
    public String GetManifestURL() {
        int[] GetInteger = Resources.GetInteger("VPC_AppID");
        if (GetInteger == null) {
            return null;
        }
        int intValue = Integer.valueOf(GetInteger[0]).intValue();
        String str = intValue != 1024290 ? intValue != 1046930 ? null : "https://www.dota2.com/project7manifest/?platform=android&appid=1046930" : "https://beta.dota2.com/project7manifest/?platform=android&appid=1024290&u=beta";
        if (str == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append("&password=");
        String sb2 = sb.toString();
        String GetString = Resources.GetString("VPC_ManifestPasswordString");
        if (GetString != null) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(sb2);
            sb3.append(GetString);
            sb2 = sb3.toString();
        }
        StringBuilder sb4 = new StringBuilder();
        sb4.append(sb2);
        sb4.append("&version=");
        sb4.append(String.valueOf(GetEffectiveApplicationVersion()));
        return sb4.toString();
    }
}
