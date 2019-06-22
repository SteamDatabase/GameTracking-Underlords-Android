package com.valvesoftware;

import android.util.AttributeSet;

public class AttributeSetNative implements AttributeSet {
    public long m_nativePointer;

    private native boolean getAttributeBooleanValue_Native(long j, int i, boolean z);

    private native boolean getAttributeBooleanValue_Native(long j, String str, String str2, boolean z);

    private native int getAttributeCount_Native(long j);

    private native float getAttributeFloatValue_Native(long j, int i, float f);

    private native float getAttributeFloatValue_Native(long j, String str, String str2, float f);

    private native int getAttributeIntValue_Native(long j, int i, int i2);

    private native int getAttributeIntValue_Native(long j, String str, String str2, int i);

    private native int getAttributeListValue_Native(long j, int i, String[] strArr, int i2);

    private native int getAttributeListValue_Native(long j, String str, String str2, String[] strArr, int i);

    private native int getAttributeNameResource_Native(long j, int i);

    private native String getAttributeName_Native(long j, int i);

    private native int getAttributeResourceValue_Native(long j, int i, int i2);

    private native int getAttributeResourceValue_Native(long j, String str, String str2, int i);

    private native int getAttributeUnsignedIntValue_Native(long j, int i, int i2);

    private native int getAttributeUnsignedIntValue_Native(long j, String str, String str2, int i);

    private native String getAttributeValue_Native(long j, int i);

    private native String getAttributeValue_Native(long j, String str, String str2);

    private native String getPositionDescription_Native(long j);

    public AttributeSetNative(long j) {
        this.m_nativePointer = j;
    }

    public String getAttributeValue(String str, String str2) {
        return getAttributeValue_Native(this.m_nativePointer, str, str2);
    }

    public String getAttributeValue(int i) {
        return getAttributeValue_Native(this.m_nativePointer, i);
    }

    public boolean getAttributeBooleanValue(String str, String str2, boolean z) {
        return getAttributeBooleanValue_Native(this.m_nativePointer, str, str2, z);
    }

    public boolean getAttributeBooleanValue(int i, boolean z) {
        return getAttributeBooleanValue_Native(this.m_nativePointer, i, z);
    }

    public float getAttributeFloatValue(String str, String str2, float f) {
        return getAttributeFloatValue_Native(this.m_nativePointer, str, str2, f);
    }

    public float getAttributeFloatValue(int i, float f) {
        return getAttributeFloatValue_Native(this.m_nativePointer, i, f);
    }

    public int getAttributeIntValue(String str, String str2, int i) {
        return getAttributeIntValue_Native(this.m_nativePointer, str, str2, i);
    }

    public int getAttributeIntValue(int i, int i2) {
        return getAttributeIntValue_Native(this.m_nativePointer, i, i2);
    }

    public int getAttributeUnsignedIntValue(String str, String str2, int i) {
        return getAttributeUnsignedIntValue_Native(this.m_nativePointer, str, str2, i);
    }

    public int getAttributeUnsignedIntValue(int i, int i2) {
        return getAttributeUnsignedIntValue_Native(this.m_nativePointer, i, i2);
    }

    public int getAttributeListValue(String str, String str2, String[] strArr, int i) {
        return getAttributeListValue_Native(this.m_nativePointer, str, str2, strArr, i);
    }

    public int getAttributeListValue(int i, String[] strArr, int i2) {
        return getAttributeListValue_Native(this.m_nativePointer, i, strArr, i2);
    }

    public int getAttributeResourceValue(String str, String str2, int i) {
        return getAttributeResourceValue_Native(this.m_nativePointer, str, str2, i);
    }

    public int getAttributeResourceValue(int i, int i2) {
        return getAttributeResourceValue_Native(this.m_nativePointer, i, i2);
    }

    public int getAttributeCount() {
        return getAttributeCount_Native(this.m_nativePointer);
    }

    public String getAttributeName(int i) {
        return getAttributeName_Native(this.m_nativePointer, i);
    }

    public int getAttributeNameResource(int i) {
        return getAttributeNameResource_Native(this.m_nativePointer, i);
    }

    public String getPositionDescription() {
        return getPositionDescription_Native(this.m_nativePointer);
    }

    public String getClassAttribute() {
        return getAttributeValue(null, "class");
    }

    public String getIdAttribute() {
        return getAttributeValue(null, "id");
    }

    public int getIdAttributeResourceValue(int i) {
        return getAttributeResourceValue(null, "id", i);
    }

    public int getStyleAttribute() {
        return getAttributeResourceValue(null, "style", 0);
    }
}
