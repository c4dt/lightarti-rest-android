package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * API to execute Tor requests.
 */
public class TorLibApi {
    static final String TAG = "ArtiLibApi";

    private final Executor executor;

    public static final String CONSENSUS_FILENAME = "consensus.txt";
    public static final String MICRODESCRIPTORS_FILENAME = "microdescriptors.txt";
    public static final String AUTHORITY_FILENAME = "authority.txt";
    public static final String CERTIFICATE_FILENAME = "certificate.txt";

    public static final String[] CACHE_FILENAMES = new String[]{
            CONSENSUS_FILENAME,
            MICRODESCRIPTORS_FILENAME,
            AUTHORITY_FILENAME,
            CERTIFICATE_FILENAME,
    };

    /**
     * Enumeration type for an HTTP method.
     */
    public enum TorRequestMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
    }

    /**
     * Result of am asynchronous request, passed to the callback.
     */
    public static class TorRequestResult {
        private TorRequestResult(){}

        /**
         * Success result.
         */
        public static class Success extends TorRequestResult {
            private final HttpResponse result;

            private Success(HttpResponse response) {
                result = response;
            }

            public HttpResponse getResult() {
                return result;
            }
        }

        /**
         * Error result.
         */
        public static class Error extends TorRequestResult {
            private final Exception error;

            private Error(Exception e) {
                error = e;
            }

            public Exception getError() {
                return error;
            }
        }
    }

    /**
     * Callback for an asynchronous request.
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

    /**
     * Create a new instance of the API. The new object is then used to send requests to the library.
     * The default executor used for asynchronous requests is a single thread executor.
     */
    public TorLibApi() {
        this(Executors.newSingleThreadExecutor());
    }

    /**
     * Create a new instance of the API. The new object is then used to send requests to the library.
     *
     * @param executor the executor used for asynchronous requests
     */
    public TorLibApi(Executor executor) {
        this.executor = executor;
    }

    /**
     * Perform an asynchronous request. The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass the <code>consensus.txt</code> and <code>microdescriptors.txt</code> files to the library</li>
     * </ul>
     * These files must be copied to the given directory before calling this method.
     *
     * @param cacheDir the cache directory path
     * @param method the HTTP method for the request
     * @param url the URL for the request
     * @param headers the headers for the request
     * @param body the body for the request
     * @param callback the callback which will receive the request result
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
     * Perform a synchronous (blocking) request. The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass the <code>consensus.txt</code> and <code>microdescriptors.txt</code> files to the library</li>
     * </ul>
     * These files must be copied to the given directory before calling this method.
     *
     * @param cacheDir the cache directory path
     * @param method the HTTP method for the request
     * @param url the URL for the request
     * @param headers the headers for the request
     * @param body the body for the request
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
