package org.c4dt.artiwrapper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Client API to execute Tor requests.
 */
public class Client implements AutoCloseable {
    static final String TAG = "ArtiClient";

    private final Executor executor;
    private long client;

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

    static {
        try {
            System.loadLibrary("lightarti_rest");
            Log.d(TAG, "Arti Rust library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Cannot load Arti Rust library: " + e);
        }
    }

    /**
     * Create a new Tor client, which is then used to send requests to the library.
     * The default executor used for asynchronous requests is a single thread executor.
     * The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass several files to the library (see {@link TorLibApi#CACHE_FILENAMES})</li>
     * </ul>
     * These files must be copied to the given directory before creating a Client.
     *
     * @param cacheDir the cache directory path
     */
    public Client(String cacheDir)
            throws TorLibException {
        this(Executors.newSingleThreadExecutor(), cacheDir);
    }

    /**
     * Create a new Tor client, which is then used to send requests to the library.
     * The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass several files to the library (see {@link TorLibApi#CACHE_FILENAMES})</li>
     * </ul>
     * These files must be copied to the given directory before creating a Client.
     *
     * @param executor the executor used for asynchronous requests
     */
    public Client(Executor executor, String cacheDir)
            throws TorLibException {
        this.executor = executor;
        this.client = create(cacheDir);
    }

    /**
     * Perform an asynchronous request.
     *
     * @param method   the HTTP method for the request
     * @param url      the URL for the request
     * @param headers  the headers for the request
     * @param body     the body for the request
     * @param callback the callback which will receive the request result
     */
    public void asyncTorRequest(
            Client.TorRequestMethod method, String url, Map<String, List<String>> headers, byte[] body,
            final TorLibApi.TorLibCallback<HttpResponse> callback) {
        executor.execute(() -> {
            try {
                HttpResponse response = syncTorRequest(method, url, headers, body);
                callback.onComplete(new TorLibApi.TorRequestResult.Success<>(response));
            } catch (Exception e) {
                callback.onComplete(new TorLibApi.TorRequestResult.Error<>(e));
            }
        });
    }

    /**
     * Perform a synchronous (blocking) request.
     *
     * @param method  the HTTP method for the request
     * @param url     the URL for the request
     * @param headers the headers for the request
     * @param body    the body for the request
     * @return the request response
     * @throws TorLibException an error occurred during the request execution
     */
    public HttpResponse syncTorRequest(Client.TorRequestMethod method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException {
        if (this.client == 0) {
            throw new TorLibException("Client has already been closed");
        }
        if (method == null) {
            throw new TorLibException("Invalid method: Null pointer");
        }
        return send(this.client, method.name(), url, headers, body);
    }

    /**
     * Close this client and free the associated memory.
     * Subsequent request calls performed using this client will fail.
     */
    @Override
    public void close() {
        Log.d(TAG, "About to free client: " + this.client);
        free(this.client);
        this.client = 0;
    }

    // Native methods

    private native long create(String cacheDir)
            throws TorLibException;

    private native HttpResponse send(long client, String method, String url, Map<String, List<String>> headers, byte[] body)
            throws TorLibException;

    private native void free(long client);
}
