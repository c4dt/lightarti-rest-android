package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TorLibApi {
    static final String TAG = "ArtiLibApi";

    private final Executor executor;

    /**
     * Enumeration type for an HTTP method
     */
    public enum TorRequestMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
    }

    /**
     * Result of a asynchronous request, passed to the callback
     */
    public static class TorRequestResult {
        private TorRequestResult(){}

        /**
         * Success result
         */
        public static class Success extends TorRequestResult {
            private final HttpResponse result;

            public Success(HttpResponse response) {
                result = response;
            }

            public HttpResponse getResult() {
                return result;
            }
        }

        /**
         * Error result
         */
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

    /**
     * Callback for an asynchronous request
     */
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

    /**
     * Perform an asynchronous request
     *
     * @param cacheDir cache directory, used:
     *                 - by the library for the creation of temporary files
     *                 - to pass the `consensus.txt` and `microdescriptors.txt` files to the library
     *                 These files must be copied to the given directory before calling this method.
     *
     * @param method request HTTP method
     * @param url request URL
     * @param headers request headers
     * @param body request body
     * @param callback callback to provide the request result
     */
    public void asyncTorRequest(
            String cacheDir, TorRequestMethod method, String url, Map<String, List<String>> headers, byte[] body,
            final TorLibCallback callback) {
        executor.execute(() -> {
            try {
                HttpResponse response = syncTorRequest(cacheDir, method, url, headers, body);
                callback.onComplete(new TorRequestResult.Success(response));
            } catch (Exception e) {
                callback.onComplete(new TorRequestResult.Error(e));
            }
        });
    }

    /**
     * Perform a synchronous (blocking) request
     *
     * @param cacheDir cache directory, used:
     *                 - by the library for the creation of temporary files
     *                 - to pass the `consensus.txt` and `microdescriptors.txt` files to the library
     *                 These files must be copied to the given directory before calling this method.
     *
     * @param method request HTTP method
     * @param url request URL
     * @param headers request headers
     * @param body request body
     * @return the request response
     * @throws TorLibException
     */
    public HttpResponse syncTorRequest(String cacheDir, TorRequestMethod method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException {
        if (method == null) {
            throw new TorLibException("Invalid method: Null pointer");
        }
        return torRequest(cacheDir, method.name(), url, headers, body);
    }

    // Native methods

    /**
     * Simple test method to verify access to the native library.
     */
    public native String hello(String who);

    private static native void initLogger();
    private native HttpResponse torRequest(String cacheDir, String method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException;
}
