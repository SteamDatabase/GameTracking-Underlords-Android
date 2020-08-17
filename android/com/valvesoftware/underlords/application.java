package com.valvesoftware.underlords;

public class application extends com.valvesoftware.source2launcher.application {
    /* access modifiers changed from: protected */
    public String GetManifestBaseURL(boolean z) {
        return z ? "https://beta.dota2.com/project7manifest/?platform=android&u=beta" : "https://www.dota2.com/project7manifest/?platform=android";
    }
}
