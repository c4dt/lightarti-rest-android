package org.c4dt.artiwrapper;

import android.util.Log;

public class JniApi {
    static final String TAG = "ArtiLib";

    static {
        try {
            System.loadLibrary("arti_android");
            Log.d(TAG, "Arti Rust library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Cannot load Arti Rust library: " + e);
        }
    }

    public native String greet(String who);

    public native void initLogger();
    public native String getGoogle(String cacheDir);
}
