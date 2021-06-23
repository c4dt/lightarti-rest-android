package org.c4dt.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.c4dt.artiwrapper.HttpResponse;
import org.c4dt.artiwrapper.JniApi;
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

        // Example of a call to a native method
        TextView tv = binding.sampleText;

        JniApi jniApi = new JniApi();

        String cacheDir = getApplicationContext().getCacheDir().toString();
        Log.d(TAG, "cacheDir = " + cacheDir);

        jniApi.initLogger();
        Log.d(TAG, "initLogger() completed");

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));
        try {
            HttpResponse resp = jniApi.tlsPost(cacheDir, "https://httpbin.org/post", headers, body);
            Log.d(TAG, "Response from POST: ");
            Log.d(TAG, "   status: " + resp.getStatus());
            Log.d(TAG, "   version: " + resp.getVersion());
            Log.d(TAG, "   headers: " + resp.getHeaders());
            Log.d(TAG, "   body: " + new String(resp.getBody()));
        } catch (Exception e) {
            Log.d(TAG, "!!! Exception: " + e);
        }

        String response = jniApi.tlsGet(cacheDir, "google.ch");
        Log.d(TAG, "Response: " + response);
        tv.setText(response);
    }
}