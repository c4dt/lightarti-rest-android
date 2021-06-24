package org.c4dt.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.c4dt.artiwrapper.HttpResponse;
import org.c4dt.artiwrapper.TorLibApi;
import org.c4dt.myapplication.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "ArtiApp";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextView tv = binding.sampleText;

        TorLibApi torLibApi = new TorLibApi();

        String cacheDir = getApplicationContext().getCacheDir().toString();
        Log.d(TAG, "cacheDir = " + cacheDir);

        Handler handler = HandlerCompat.createAsync(Looper.getMainLooper());

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        tv.setText("Starting request...");

        torLibApi.submitTorRequest(cacheDir,
                "POST", "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success) result).getResult();
                        Log.d(TAG, "Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        handler.post(() -> tv.setText("Result received: " + resp.getStatus()));
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);

                        handler.post(() -> tv.setText("Exception: " + e));
                    }
                }
        );
    }
}