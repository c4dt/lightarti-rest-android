package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TorLibApi {
    static final String TAG = "ArtiLibApi";

    private final Executor executor;

    public static class TorRequestResult {
        private TorRequestResult(){}

        public static class Success extends TorRequestResult {
            private final HttpResponse result;

            public Success(HttpResponse response) {
                result = response;
            }

            public HttpResponse getResult() {
                return result;
            }
        }

        public static class Error extends TorRequestResult {
            private final Exception error;

            public Error(Exception e) {
                error = e;
            }

            public Exception getError() {
                return error;
            }
        }
    }

    public interface TorLibCallback {
        void onComplete(TorRequestResult result);
    }

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

    public TorLibApi() {
        this(Executors.newSingleThreadExecutor());
    }

    public TorLibApi(Executor executor) {
        this.executor = executor;
    }

    public void submitTorRequest(
            String cacheDir, String method, String url, Map<String, List<String>> headers, byte[] body,
            final TorLibCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpResponse response = torRequest(
                            cacheDir, method, url, headers, body);
                    callback.onComplete(new TorRequestResult.Success(response));
                } catch (Exception e) {
                    callback.onComplete(new TorRequestResult.Error(e));
                }
            }
        });
    }

    // Native methods

    public native String hello(String who);

    public static native void initLogger();
    public native HttpResponse torRequest(String cacheDir, String method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException;
}
