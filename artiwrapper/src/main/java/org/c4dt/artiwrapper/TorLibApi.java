package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;

public class TorLibApi {
    static final String TAG = "ArtiLibApi";

    static {
        try {
            System.loadLibrary("core");
            Log.d(TAG, "Arti Rust library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Cannot load Arti Rust library: " + e);
        }

        TorLibApi.initLogger();
        Log.d(TAG, "initLogger() completed");
    }

    public native String hello(String who);

    public static native void initLogger();
    public native HttpResponse torRequest(String cacheDir, String method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException;
}
