package org.c4dt.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.c4dt.artiwrapper.HttpResponse;
import org.c4dt.artiwrapper.TorLibApi;
import org.c4dt.myapplication.databinding.ActivityMainBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;

/**
 * Simple example application showing how to use the Arti wrapper.
 */
public class MainActivity extends AppCompatActivity {
    static final String TAG = "ArtiApp";

    /**
     * Example function to downloaded the required cache files from a URL.
     * The resource at the URL is expected to be a gzipped tar archive containing
     * all the files within the root directory.
     *
     * @param urlString     the URL of the archive
     * @param destDirString the path where the contents of the archive are to be extracted
     * @return a Future wrapping the download execution
     */
    private Future<Void> downloadFiles(String urlString, String destDirString) {
        // Execute download in a thread
        ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            File destDir = new File(destDirString);

            try (InputStream uin = urlConnection.getInputStream();
                 InputStream buin = new BufferedInputStream(uin);
                 InputStream gzin = new GzipCompressorInputStream(buin);
                 ArchiveInputStream ain = new TarArchiveInputStream(gzin)) {
                byte[] buf = new byte[1024];

                ArchiveEntry entry;
                while ((entry = ain.getNextEntry()) != null) {
                    // Skip directories
                    if (entry.isDirectory()) continue;

                    File destFile = new File(destDir, entry.getName());
                    int nbRead;
                    try (FileOutputStream out = new FileOutputStream(destFile)) {
                        while ((nbRead = ain.read(buf)) != -1) {
                            out.write(buf, 0, nbRead);
                        }
                    }
                    Log.d(TAG, "Extracted file: " + destFile.getName());
                }
            }

            return null;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

        TextView tv = binding.sampleText;

        File cacheDir = getApplicationContext().getCacheDir();
        Log.d(TAG, "cacheDir = " + cacheDir.toString());

        tv.setText("Downloading cache files...");
        try {
            // Download cache files from the C4DT GitHub release
            downloadFiles("https://github.com/c4dt/lightarti-directory/releases/latest/download/directory-cache.tgz", cacheDir.getPath()).get();
            Log.d(TAG, "Files downloaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to download files: " + e);
        }

        TorLibApi torLibApi = new TorLibApi();

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        tv.setText("Starting request...");

        torLibApi.asyncTorRequest(cacheDir.getPath(),
                TorLibApi.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success) result).getResult();
                        Log.d(TAG, "Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        handler.post(() -> tv.setText(String.format(Locale.ENGLISH,
                                "Result received [status = %d]:\n\n%s", resp.getStatus(), new String(resp.getBody()))));
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);

                        handler.post(() -> tv.setText("Exception: " + e));
                    }
                }
        );
    }
}