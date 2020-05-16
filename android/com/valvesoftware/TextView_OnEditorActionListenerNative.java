package com.valvesoftware;

import android.view.KeyEvent;
import android.widget.TextView;

public class TextView_OnEditorActionListenerNative implements TextView.OnEditorActionListener {
    public long m_nativePointer;

    public static native boolean onEditorActionNative(long j, TextView textView, int i, KeyEvent keyEvent);

    public TextView_OnEditorActionListenerNative(long j) {
        this.m_nativePointer = j;
    }

    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        return onEditorActionNative(this.m_nativePointer, textView, i, keyEvent);
    }
}
