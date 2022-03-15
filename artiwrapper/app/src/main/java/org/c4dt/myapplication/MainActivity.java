package org.c4dt.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import org.c4dt.artiwrapper.Client;
import org.c4dt.artiwrapper.HttpResponse;
import org.c4dt.artiwrapper.TorLibApi;
import org.c4dt.artiwrapper.TorLibException;
import org.c4dt.myapplication.databinding.ActivityMainBinding;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Simple example application showing how to use the Arti wrapper.
 */
public class MainActivity extends AppCompatActivity {
    static final String TAG = "ArtiApp";

    private TextView tv;
    private Handler handler;
    private File cacheDir;

    private void display(String text) {
        handler.post(() -> tv.setText(String.format(Locale.ENGLISH, "%s- %s\n", tv.getText(), text)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handler = HandlerCompat.createAsync(Looper.getMainLooper());
        tv = binding.sampleText;

        cacheDir = getApplicationContext().getCacheDir();
        Log.d(TAG, "cacheDir = " + cacheDir.toString());

        TorLibApi torLibApi = new TorLibApi();

        display("Updating cache...");
        torLibApi.updateCache(cacheDir.getPath(),
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        TorLibApi.CacheUpdateStatus status = ((TorLibApi.TorRequestResult.Success<TorLibApi.CacheUpdateStatus>) result).getResult();
                        switch (status) {
                            case CACHE_IS_UP_TO_DATE:
                                display("Cache is already up to date");
                                break;
                            case DOWNLOADED_CHURN_FILE:
                                display("Downloaded churn file");
                                break;
                            case DOWNLOADED_FULL_CACHE:
                                display("Downloaded full cache");
                                break;
                        }
                        try {
                            makeRequest();
                        } catch (TorLibException e) {
                            Log.e(TAG, "Failed to create client: " + e);
                            display("Failed to create client: " + e);
                        }
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<TorLibApi.CacheUpdateStatus>) result).getError();
                        Log.e(TAG, "Failed to update cache: " + e);
                        display("Failed to update cache: " + e);
                    }
                }
        );
    }

    private void makeRequest() throws TorLibException {
        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        display("Starting request...");

        Client client = new Client(cacheDir.getPath());
        client.asyncTorRequest(
                Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success<HttpResponse>) result).getResult();
                        Log.d(TAG, "Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        display(String.format(Locale.ENGLISH,
                                "Response received [status = %d]:\n\n%s", resp.getStatus(), new String(resp.getBody())));
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<HttpResponse>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);

                        display("Exception: " + e);
                    }
                    client.close();
                }
        );
    }
}