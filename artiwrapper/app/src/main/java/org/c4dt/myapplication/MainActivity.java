package org.c4dt.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.c4dt.artiwrapper.HttpResponse;
import org.c4dt.artiwrapper.TorLibApi;
import org.c4dt.myapplication.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple example application showing how to use the Arti wrapper.
 */
public class MainActivity extends AppCompatActivity {
    static final String TAG = "ArtiApp";

    private ActivityMainBinding binding;

    private void copyFiles(AssetManager am, File cacheDir) throws IOException {
        for (String filename: new String[]{"consensus.txt", "microdescriptors.txt"}) {
            File dest = new File(cacheDir, filename);
            InputStream is = am.open(filename);
            FileOutputStream fos = new FileOutputStream(dest);

            byte[] buf = new byte[1024];
            int nbRead;

            while ((nbRead = is.read(buf)) != -1) {
                fos.write(buf, 0, nbRead);
            }

            is.close();
            fos.close();

            Log.d(TAG, "Copied \"" + filename + "\"");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

        TextView tv = binding.sampleText;

        File cacheDir = getApplicationContext().getCacheDir();
        Log.d(TAG, "cacheDir = " + cacheDir.toString());

        try {
            copyFiles(getApplicationContext().getAssets(), cacheDir);
        } catch (IOException e) {
            Log.d(TAG, "Failed to copy files: " + e);
        }

        TorLibApi torLibApi = new TorLibApi();

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        tv.setText("Starting request...");

        torLibApi.asyncTorRequest(cacheDir.toString(),
                TorLibApi.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success) result).getResult();
                        Log.d(TAG, "Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        handler.post(() -> tv.setText(String.format(
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