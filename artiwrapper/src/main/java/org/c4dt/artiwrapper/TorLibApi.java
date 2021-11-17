package org.c4dt.artiwrapper;

import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

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
    public static final String CHURN_FILENAME = "churn.txt";

    public static final String DIRECTORY_CACHE_C4DT = "https://github.com/c4dt/lightarti-directory/releases/latest/download/directory-cache.tgz";
    public static final String CHURN_CACHE_C4DT = "https://github.com/c4dt/lightarti-directory/releases/latest/download/churn.txt";

    /**
     * Files to pass via the directory cache when calling
     * {@link #asyncTorRequest(String, TorRequestMethod, String, Map, byte[], TorLibCallback)}
     * or {@link #syncTorRequest(String, TorRequestMethod, String, Map, byte[])}.
     * See <a href=">https://github.com/c4dt/lightarti-rest/blob/main/tools/README.md">
     * <code>lightarti-rest</code> tools</a> for details.
     */
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
    public static class TorRequestResult<T> {
        TorRequestResult() {
        }

        /**
         * Success result.
         */
        public static class Success<T> extends TorRequestResult<T> {
            private final T result;

            Success(T response) {
                result = response;
            }

            public T getResult() {
                return result;
            }
        }

        /**
         * Error result.
         */
        public static class Error<T> extends TorRequestResult<T> {
            private final Exception error;

            Error(Exception e) {
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
    public interface TorLibCallback<T> {
        void onComplete(TorRequestResult<T> result);
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
     * Update the cache files using the C4DT releases.
     * Examine the current files, and determine whether the full archive, only the churn file,
     * or nothing needs to be downloaded.
     *
     * @param destDirString the path where the contents of the archive are to be extracted
     * @param callback      the callback which will be called when the update is done
     */
    public void updateCache(String destDirString, final TorLibCallback<Void> callback) {
        Log.d(TAG, "Updating cache");

        Calendar now = Calendar.getInstance();

        int currentYear = now.get(Calendar.YEAR);
        int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        int currentWeekOfYear = now.get(Calendar.WEEK_OF_YEAR);

        boolean udescIsCurrent = false;
        boolean churnFileIsCurrent = false;

        File udescFile = new File(destDirString, MICRODESCRIPTORS_FILENAME);
        if (udescFile.exists()) {
            Calendar udescTime = Calendar.getInstance();
            udescTime.setTimeInMillis(udescFile.lastModified());
            if ((currentYear == udescTime.get(Calendar.YEAR)) &&
                    (currentWeekOfYear == udescTime.get(Calendar.WEEK_OF_YEAR))) {
                udescIsCurrent = true;

                File churnFile = new File(destDirString, CHURN_FILENAME);
                if (churnFile.exists()) {
                    Calendar churnTime = Calendar.getInstance();
                    churnTime.setTimeInMillis(churnFile.lastModified());
                    if ((currentYear == churnTime.get(Calendar.YEAR)) &&
                            (currentDayOfYear == churnTime.get(Calendar.DAY_OF_YEAR))) {
                        churnFileIsCurrent = true;
                    }
                } else {
                    Log.d(TAG, "Churn file does not exist");
                }
            }
        } else {
            Log.d(TAG, "Microdescriptors file does not exist");
        }

        if (udescIsCurrent) {
            if (churnFileIsCurrent) {
                Log.d(TAG, "Churn file is current -- cache is up to date");
                callback.onComplete(new TorRequestResult.Success<>(null));
            } else {
                Log.d(TAG, "Microdescriptors file is current -- download churn file only");
                downloadChurnFile(CHURN_CACHE_C4DT, destDirString, callback);
            }
        } else {
            Log.d(TAG, "Microdescriptors file is not current -- download full cache");
            downloadFullCache(DIRECTORY_CACHE_C4DT, destDirString, callback);
        }
    }

    private void copyFile(InputStream is, File destFile) throws IOException {
        byte[] buf = new byte[1024];

        int nbRead;
        try (FileOutputStream out = new FileOutputStream(destFile)) {
            while ((nbRead = is.read(buf)) != -1) {
                out.write(buf, 0, nbRead);
            }
        }
    }

    /**
     * Download the churn cache file from a URL.
     *
     * @param urlString     the URL of the file
     * @param destDirString the path where the contents of the archive are to be extracted
     * @param callback      the callback which will be called when the download is complete
     */
    public void downloadChurnFile(String urlString, String destDirString,
                                  final TorLibCallback<Void> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                File destDir = new File(destDirString);

                try (InputStream uin = urlConnection.getInputStream()) {
                    copyFile(uin, new File(destDir, CHURN_FILENAME));
                }

                callback.onComplete(new TorRequestResult.Success<>(null));
            } catch (Exception e) {
                callback.onComplete(new TorRequestResult.Error<>(e));
            }
        });
    }

    /**
     * Download the full cache files from a URL.
     * The resource at the URL is expected to be a gzipped tar archive containing
     * all the files within the root directory.
     *
     * @param urlString     the URL of the archive
     * @param destDirString the path where the contents of the archive are to be extracted
     * @param callback      the callback which will be called when the download is complete
     */
    public void downloadFullCache(String urlString, String destDirString,
                                  final TorLibCallback<Void> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                File destDir = new File(destDirString);

                try (InputStream uin = urlConnection.getInputStream();
                     InputStream buin = new BufferedInputStream(uin);
                     InputStream gzin = new GzipCompressorInputStream(buin);
                     ArchiveInputStream ain = new TarArchiveInputStream(gzin)) {
                    ArchiveEntry entry;
                    while ((entry = ain.getNextEntry()) != null) {
                        // Skip directories
                        if (entry.isDirectory()) continue;

                        copyFile(ain, new File(destDir, entry.getName()));
                        Log.d(TAG, "Extracted file: " + entry.getName());
                    }
                }

                callback.onComplete(new TorRequestResult.Success<>(null));
            } catch (Exception e) {
                callback.onComplete(new TorRequestResult.Error<>(e));
            }
        });
    }

    /**
     * Perform an asynchronous request. The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass several files to the library (see {@link #CACHE_FILENAMES})</li>
     * </ul>
     * These files must be copied to the given directory before calling this method.
     *
     * @param cacheDir the cache directory path
     * @param method   the HTTP method for the request
     * @param url      the URL for the request
     * @param headers  the headers for the request
     * @param body     the body for the request
     * @param callback the callback which will receive the request result
     */
    public void asyncTorRequest(
            String cacheDir, TorRequestMethod method, String url, Map<String, List<String>> headers, byte[] body,
            final TorLibCallback<HttpResponse> callback) {
        executor.execute(() -> {
            try {
                HttpResponse response = syncTorRequest(cacheDir, method, url, headers, body);
                callback.onComplete(new TorRequestResult.Success<>(response));
            } catch (Exception e) {
                callback.onComplete(new TorRequestResult.Error<>(e));
            }
        });
    }

    /**
     * Perform a synchronous (blocking) request. The <code>cacheDir</code> argument is used in two ways:
     * <ul>
     *     <li>by the library for the creation of temporary files</li>
     *     <li>to pass several files to the library (see {@link #CACHE_FILENAMES})</li>
     * </ul>
     * These files must be copied to the given directory before calling this method.
     *
     * @param cacheDir the cache directory path
     * @param method   the HTTP method for the request
     * @param url      the URL for the request
     * @param headers  the headers for the request
     * @param body     the body for the request
     * @return the request response
     * @throws TorLibException an error occurred during the request execution
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
