package org.c4dt.artiwrapper;

import android.content.res.AssetManager;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.hamcrest.CoreMatchers.containsString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class JniTest {
    private TorLibApi api;
    private String cacheDir;

    private static final String TAG = "ArtiTest";

    private final String dummyCacheDir = "dummy cache dir";
    private final TorLibApi.TorRequestMethod dummyMethod = TorLibApi.TorRequestMethod.GET;
    private final String dummyUrl = "https://example.com";
    private final Map<String, List<String>> dummyHeaders = new HashMap<>();
    private final byte[] dummyBody = "dummy body".getBytes();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private void copyFiles(AssetManager am, File cacheDir) throws IOException {
        Log.d(TAG, "assets: " + Arrays.toString(am.list("")));

        for (String filename: TorLibApi.CACHE_FILENAMES) {
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


    @Before
    public void setUp() throws IOException {
        api = new TorLibApi();
        cacheDir = folder.getRoot().toString();

        AssetManager am = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        copyFiles(am, folder.getRoot());
    }

    @Test
    public void helloRust() {
        assertEquals("Hello world!", api.hello("world"));
    }

    @Test
    public void helloRustException() {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("create rust string"));

        api.hello(null);
    }

    @Test
    public void nullCacheDir() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("cache_dir"));
        thrown.expectMessage(containsString("Null pointer"));

        api.syncTorRequest(null, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void nonexistentCacheDir() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("No such file or directory"));

        api.syncTorRequest(dummyCacheDir, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void missingConsensus() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("Failed to read the consensus"));

        File f = new File(folder.getRoot(), TorLibApi.CONSENSUS_FILENAME);
        f.delete();

        api.syncTorRequest(cacheDir, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void missingMicroDescriptors() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("Failed to read microdescriptors"));

        File f = new File(folder.getRoot(), TorLibApi.MICRODESCRIPTORS_FILENAME);
        f.delete();

        api.syncTorRequest(cacheDir, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void missingAuthority() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("Failed to read authority"));

        File f = new File(folder.getRoot(), TorLibApi.AUTHORITY_FILENAME);
        f.delete();

        api.syncTorRequest(cacheDir, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void missingCertificate() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("Failed to read the certificate"));

        File f = new File(folder.getRoot(), TorLibApi.CERTIFICATE_FILENAME);
        f.delete();

        api.syncTorRequest(cacheDir, dummyMethod, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void nullMethod() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("method"));
        thrown.expectMessage(containsString("Null pointer"));

        api.syncTorRequest(dummyCacheDir, null, dummyUrl, dummyHeaders, dummyBody);
    }

    @Test
    public void nullUrl() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("url"));
        thrown.expectMessage(containsString("Null pointer"));

        api.syncTorRequest(dummyCacheDir, dummyMethod, null, dummyHeaders, dummyBody);
    }

    @Test
    public void invalidUrl() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("invalid"));

        api.syncTorRequest(dummyCacheDir, dummyMethod, "not:/valid", dummyHeaders, dummyBody);
    }

    @Test
    public void nullHeaders() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("JMap"));
        thrown.expectMessage(containsString("Null pointer"));

        api.syncTorRequest(dummyCacheDir, dummyMethod, dummyUrl, null, dummyBody);
    }

    @Test
    public void nullBody() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("byte array"));
        thrown.expectMessage(containsString("Null pointer"));

        api.syncTorRequest(dummyCacheDir, dummyMethod, dummyUrl, dummyHeaders, null);
    }

    @Test
    public void syncPost() {
        try {
            byte[] body = "key1=val1&key2=val2".getBytes();
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("header-one", Collections.singletonList("hello"));
            headers.put("header-two", Arrays.asList("how", "are", "you"));
            headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
            headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

            HttpResponse resp = api.syncTorRequest(cacheDir,
                    TorLibApi.TorRequestMethod.POST, "https://httpbin.org/post", headers, body);

            Log.d(TAG, "Response from POST: ");
            Log.d(TAG, "   status: " + resp.getStatus());
            Log.d(TAG, "   version: " + resp.getVersion());
            Log.d(TAG, "   headers: " + resp.getHeaders());
            Log.d(TAG, "   body: " + new String(resp.getBody()));

            assertEquals(200, resp.getStatus());
        } catch (TorLibException e) {
            Log.d(TAG, "!!! Exception: " + e);
            fail();
        }
    }

    @Test
    public void asyncPost() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        AtomicReference<HttpResponse> response = new AtomicReference<>();

        api.asyncTorRequest(cacheDir,
                TorLibApi.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success) result).getResult();

                        Log.d(TAG, "Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        response.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    signal.countDown();
                }
        );

        signal.await(120, TimeUnit.SECONDS);

        assertEquals(200, response.get().getStatus());
    }

    @Test
    public void syncGet() {
        try {
            HttpResponse resp = api.syncTorRequest(cacheDir,
                    TorLibApi.TorRequestMethod.GET, "https://www.c4dt.org", new HashMap<>(), new byte[]{});

            Log.d(TAG, "Response from GET: ");
            Log.d(TAG, "   status: " + resp.getStatus());
            Log.d(TAG, "   version: " + resp.getVersion());
            Log.d(TAG, "   headers: " + resp.getHeaders());
            Log.d(TAG, "   body: " + new String(resp.getBody()));

            assertEquals(200, resp.getStatus());
        } catch (TorLibException e) {
            Log.d(TAG, "!!! Exception: " + e);
            fail();
        }
    }
}
