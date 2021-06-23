package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;

public class JniApi {
    static final String TAG = "ArtiLibJni";

    static {
        try {
            System.loadLibrary("core");
            Log.d(TAG, "Arti Rust library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Cannot load Arti Rust library: " + e);
        }
    }

    public native String hello(String who);

    public native void initLogger();
    public native String tlsGet(String cacheDir, String domain);
    public native HttpResponse tlsPost(String cacheDir, String url, Map<String, List<String>> headers, byte[] body);
}
