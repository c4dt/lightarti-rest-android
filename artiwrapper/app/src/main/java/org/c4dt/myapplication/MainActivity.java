package org.c4dt.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.c4dt.artiwrapper.JniApi;
import org.c4dt.myapplication.databinding.ActivityMainBinding;

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
        Log.d(TAG, "greet() → " + jniApi.hello("world"));

        String cacheDir = getApplicationContext().getCacheDir().toString();
        Log.d(TAG, "cacheDir = " + cacheDir);

        jniApi.initLogger();
        Log.d(TAG, "initLogger() completed");

        String response = jniApi.tlsGet(cacheDir, "google.ch");
        Log.d(TAG, "Response: " + response);
        tv.setText(response);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}