package com.valvesoftware.underlords;

import com.valvesoftware.Resources;

public class application extends com.valvesoftware.source2launcher.application {
    public String GetManifestURL() {
        int[] GetInteger = Resources.GetInteger("APP_ID");
        if (GetInteger == null) {
            return null;
        }
        int intValue = Integer.valueOf(GetInteger[0]).intValue();
        String str = intValue != 1024290 ? intValue != 1046930 ? intValue != 1105690 ? null : "https://www.dota2.com/project7manifest/?platform=android&appid=1105690" : "https://www.dota2.com/project7manifest/?platform=android&appid=1046930" : "https://beta.dota2.com/project7manifest/?platform=android&appid=1024290&u=beta";
        if (str == null) {
            return null;
        }
        String str2 = str + "&password=";
        String GetString = Resources.GetString("ManifestPasswordString");
        if (GetString != null) {
            str2 = str2 + GetString;
        }
        return str2 + "&version=" + String.valueOf(GetEffectiveApplicationVersion());
    }
}
