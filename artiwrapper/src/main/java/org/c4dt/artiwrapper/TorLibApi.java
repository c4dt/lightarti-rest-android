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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * API for Arti utility functions.
 */
public class TorLibApi {
    static final String TAG = "ArtiLibApi";

    private final Executor executor;

    public static final String CONSENSUS_FILENAME = "consensus.txt";
    public static final String MICRODESCRIPTORS_FILENAME = "microdescriptors.txt";
    public static final String AUTHORITY_FILENAME = "authority.json";
    public static final String CERTIFICATE_FILENAME = "certificate.txt";
    public static final String CHURN_FILENAME = "churn.txt";

    public static final String DIRECTORY_CACHE_C4DT = "https://github.com/c4dt/lightarti-directory/releases/latest/download/directory-cache.tgz";
    public static final String CHURN_CACHE_C4DT = "https://github.com/c4dt/lightarti-directory/releases/latest/download/churn.txt";

    /**
     * Files to pass via the directory cache when calling
     * {@link Client#Client(String)} or {@link Client#Client(Executor, String)}.
     * See <a href=">https://github.com/c4dt/lightarti-directory/blob/main/tools/README.md">
     * <code>lightarti-directory</code> tools</a> for details.
     */
    public static final String[] CACHE_FILENAMES = new String[]{
            CONSENSUS_FILENAME,
            MICRODESCRIPTORS_FILENAME,
            AUTHORITY_FILENAME,
            CERTIFICATE_FILENAME,
            CHURN_FILENAME,
    };

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
            System.loadLibrary("lightarti_rest");
            Log.d(TAG, "Arti Rust library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Cannot load Arti Rust library: " + e);
        }

        TorLibApi.initLogger();
        Log.d(TAG, "initLogger() completed");
    }

    /**
     * Create a new instance of the API. The new object is then used to call utility functions.
     * The default executor used for asynchronous requests is a single thread executor.
     */
    public TorLibApi() {
        this(Executors.newSingleThreadExecutor());
    }

    /**
     * Create a new instance of the API. The new object is then used to call utility functions.
     *
     * @param executor the executor used for asynchronous requests
     */
    public TorLibApi(Executor executor) {
        this.executor = executor;
    }

    private static class CacheState {
        public final boolean udescIsCurrent;
        public final boolean churnFileIsCurrent;

        public CacheState(boolean udescIsCurrent, boolean churnFileIsCurrent) {
            this.udescIsCurrent = udescIsCurrent;
            this.churnFileIsCurrent = churnFileIsCurrent;
        }
    }

    private CacheState getCacheState(String destDirString) {
        // Use UK locale to have Monday as the first day of the week
        Calendar now = Calendar.getInstance(Locale.UK);

        int currentYear = now.get(Calendar.YEAR);
        int currentDayOfYear = now.get(Calendar.DAY_OF_YEAR);
        int currentWeekOfYear = now.get(Calendar.WEEK_OF_YEAR);

        boolean udescIsCurrent = false;
        boolean churnFileIsCurrent = false;

        boolean missingFiles = false;
        for (String fileName : CACHE_FILENAMES) {
            // Churn file is optional
            if (fileName.equals(CHURN_FILENAME)) continue;

            if (!new File(destDirString, fileName).exists()) {
                Log.d(TAG, String.format("Cache is missing file \"%s\"", fileName));
                missingFiles = true;
            }
        }

        if (!missingFiles) {
            File udescFile = new File(destDirString, MICRODESCRIPTORS_FILENAME);
            Calendar udescTime = Calendar.getInstance(Locale.UK);
            udescTime.setTimeInMillis(udescFile.lastModified());
            if ((currentYear == udescTime.get(Calendar.YEAR)) &&
                    (currentWeekOfYear == udescTime.get(Calendar.WEEK_OF_YEAR))) {
                udescIsCurrent = true;

                File churnFile = new File(destDirString, CHURN_FILENAME);
                if (churnFile.exists()) {
                    Calendar churnTime = Calendar.getInstance(Locale.UK);
                    churnTime.setTimeInMillis(churnFile.lastModified());
                    if ((currentYear == churnTime.get(Calendar.YEAR)) &&
                            (currentDayOfYear == churnTime.get(Calendar.DAY_OF_YEAR))) {
                        churnFileIsCurrent = true;
                    }
                } else {
                    Log.d(TAG, "Churn file does not exist");
                }
            }
        }

        return new CacheState(udescIsCurrent, churnFileIsCurrent);
    }

    /**
     * Status of the cache update process, indicating what actions were taken.
     */
    public enum CacheUpdateStatus {
        CACHE_IS_UP_TO_DATE,
        DOWNLOADED_CHURN_FILE,
        DOWNLOADED_FULL_CACHE,
    }

    /**
     * Update the cache files using the C4DT releases.
     * Examine the current files, and determine whether the full archive, only the churn file,
     * or nothing needs to be downloaded.
     *
     * @param destDirString the path where the contents of the archive are to be extracted
     * @param callback      the callback which will be called when the update is done
     */
    public void updateCache(String destDirString, final TorLibCallback<CacheUpdateStatus> callback) {
        Log.d(TAG, "Updating cache");

        CacheState cacheState = getCacheState(destDirString);

        if (cacheState.udescIsCurrent) {
            if (cacheState.churnFileIsCurrent) {
                Log.d(TAG, "Churn file is current -- cache is already up to date");
                callback.onComplete(new TorRequestResult.Success<>(CacheUpdateStatus.CACHE_IS_UP_TO_DATE));
            } else {
                Log.d(TAG, "Microdescriptors file is current -- download churn file only");
                downloadChurnFile(CHURN_CACHE_C4DT, destDirString, callback);
            }
        } else {
            Log.d(TAG, "Microdescriptors file is not current -- download full cache");
            downloadFullCache(DIRECTORY_CACHE_C4DT, destDirString, callback);
        }
    }

    /**
     * Download the churn cache file from a URL.
     *
     * @param urlString     the URL of the file
     * @param destDirString the path where the contents of the archive are to be extracted
     * @param callback      the callback which will be called when the download is complete
     */
    private void downloadChurnFile(String urlString, String destDirString,
                                   final TorLibCallback<CacheUpdateStatus> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                try (InputStream uin = urlConnection.getInputStream()) {
                    Files.copy(uin, Paths.get(destDirString, CHURN_FILENAME));
                }

                callback.onComplete(new TorRequestResult.Success<>(CacheUpdateStatus.DOWNLOADED_CHURN_FILE));
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
    private void downloadFullCache(String urlString, String destDirString,
                                   final TorLibCallback<CacheUpdateStatus> callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, String.format("Reading from %s", urlString));
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                try (InputStream uin = urlConnection.getInputStream();
                     InputStream buin = new BufferedInputStream(uin);
                     InputStream gzin = new GzipCompressorInputStream(buin);
                     ArchiveInputStream ain = new TarArchiveInputStream(gzin)) {
                    ArchiveEntry entry;
                    while ((entry = ain.getNextEntry()) != null) {
                        // Skip directories
                        if (entry.isDirectory()) continue;

                        Files.copy(ain, Paths.get(destDirString, entry.getName()));
                        Log.d(TAG, "Extracted file: " + entry.getName());
                    }
                }

                callback.onComplete(new TorRequestResult.Success<>(CacheUpdateStatus.DOWNLOADED_FULL_CACHE));
            } catch (Exception e) {
                callback.onComplete(new TorRequestResult.Error<>(e));
            }
        });
    }

    // Native methods

    /**
     * Simple test method to verify access to the native library.
     */
    public native String hello(String who);

    private static native void initLogger();
}
