package org.c4dt.artiwrapper;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class JniTest {
    private TorLibApi api;
    private String cacheDir;

    private static final String TAG = "ArtiTest";

    private final String dummyCacheDir = "dummy cache dir";
    private final Client.TorRequestMethod dummyMethod = Client.TorRequestMethod.GET;
    private final String dummyUrl = "https://example.com";
    private final Map<String, List<String>> dummyHeaders = new HashMap<>();
    private final byte[] dummyBody = "dummy body".getBytes();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException, InterruptedException {
        api = new TorLibApi();
        cacheDir = folder.getRoot().toString();

        execUpdateCache();
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
        thrown.expectMessage(containsString("create rust string for `cache_dir_j`: Null pointer in get_string obj argument"));

        try (Client client = new Client(null)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void nonexistentCacheDir() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("directory cache does not exist"));

        try (Client client = new Client(dummyCacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void missingConsensus() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(
            containsString("Corrupt cache: required file(s) missing in cache")
        );

        File f = new File(folder.getRoot(), TorLibApi.CONSENSUS_FILENAME);
        assertTrue(f.delete());

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void missingMicroDescriptors() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(
            containsString("Corrupt cache: required file(s) missing in cache")
        );

        File f = new File(folder.getRoot(), TorLibApi.MICRODESCRIPTORS_FILENAME);
        assertTrue(f.delete());

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void missingAuthority() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(
            containsString("Corrupt cache: required file(s) missing in cache")
        );

        File f = new File(folder.getRoot(), TorLibApi.AUTHORITY_FILENAME);
        assertTrue(f.delete());

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void missingCertificate() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(
            containsString("Corrupt cache: required file(s) missing in cache")
        );

        File f = new File(folder.getRoot(), TorLibApi.CERTIFICATE_FILENAME);
        assertTrue(f.delete());

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void nullMethod() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("method"));
        thrown.expectMessage(containsString("Null pointer"));

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(null, dummyUrl, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void nullUrl() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("url"));
        thrown.expectMessage(containsString("Null pointer"));

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, null, dummyHeaders, dummyBody);
        }
    }

    @Test
    public void invalidUrl() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("invalid"));

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, "not:/valid", dummyHeaders, dummyBody);
        }
    }

    @Test
    public void nullHeaders() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("JMap"));
        thrown.expectMessage(containsString("Null pointer"));

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, null, dummyBody);
        }
    }

    @Test
    public void nullBody() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("byte array"));
        thrown.expectMessage(containsString("Null pointer"));

        try (Client client = new Client(cacheDir)) {
            client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, null);
        }
    }

    @Test
    public void callAfterClose() throws TorLibException {
        thrown.expect(TorLibException.class);
        thrown.expectMessage(containsString("already been closed"));

        Client client = new Client(cacheDir);
        client.close();
        client.syncTorRequest(dummyMethod, dummyUrl, dummyHeaders, dummyBody);
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

            try (Client client = new Client(cacheDir)) {
                HttpResponse resp = client.syncTorRequest(
                        Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body);

                Log.d(TAG, "Response from POST: ");
                Log.d(TAG, "   status: " + resp.getStatus());
                Log.d(TAG, "   version: " + resp.getVersion());
                Log.d(TAG, "   headers: " + resp.getHeaders());
                Log.d(TAG, "   body: " + new String(resp.getBody()));

                assertEquals(200, resp.getStatus());
            }
        } catch (TorLibException e) {
            Log.d(TAG, "!!! Exception: " + e);
            fail();
        }
    }

    @Test
    public void syncPostSequential() {
        try {
            byte[] body = "key1=val1&key2=val2".getBytes();
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("header-one", Collections.singletonList("hello"));
            headers.put("header-two", Arrays.asList("how", "are", "you"));
            headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
            headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

            try (Client client = new Client(cacheDir)) {
                HttpResponse resp = client.syncTorRequest(
                        Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body);

                assertEquals(200, resp.getStatus());
                Log.w(TAG, "First request completed");

                resp = client.syncTorRequest(
                        Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body);

                assertEquals(200, resp.getStatus());
                Log.w(TAG, "Second request completed");
            }
        } catch (TorLibException e) {
            Log.d(TAG, "!!! Exception: " + e);
            fail();
        }
    }

    @Test
    public void asyncPost() throws InterruptedException, TorLibException {
        final CountDownLatch signal = new CountDownLatch(1);

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        AtomicReference<HttpResponse> response = new AtomicReference<>();

        Client client = new Client(cacheDir);
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

                        response.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<HttpResponse>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    client.close();
                    signal.countDown();
                }
        );

        assertTrue(signal.await(120, TimeUnit.SECONDS));

        assertEquals(200, response.get().getStatus());
    }

    @Test
    public void asyncPostParallel() throws InterruptedException, TorLibException {
        // Launch 3 requests, two of them on the same client
        final CountDownLatch signal = new CountDownLatch(3);

        byte[] body = "key1=val1&key2=val2".getBytes();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("header-one", Collections.singletonList("hello"));
        headers.put("header-two", Arrays.asList("how", "are", "you"));
        headers.put("Content-Length", Collections.singletonList(String.valueOf(body.length)));
        headers.put("Content-Type", Collections.singletonList("application/x-www-form-urlencoded"));

        Client client1 = new Client(cacheDir);
        Client client2 = new Client(cacheDir);

        AtomicReference<HttpResponse> response1 = new AtomicReference<>();
        client1.asyncTorRequest(
                Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success<HttpResponse>) result).getResult();

                        Log.d(TAG, "[1] Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        response1.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<HttpResponse>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    signal.countDown();
                }
        );
        Log.d(TAG, "First request sent");

        AtomicReference<HttpResponse> response2 = new AtomicReference<>();
        client2.asyncTorRequest(
                Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success<HttpResponse>) result).getResult();

                        Log.d(TAG, "[2] Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        response2.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<HttpResponse>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    signal.countDown();
                }
        );
        Log.d(TAG, "Second request sent");

        AtomicReference<HttpResponse> response3 = new AtomicReference<>();
        client1.asyncTorRequest(
                Client.TorRequestMethod.POST, "https://httpbin.org/post", headers, body,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        HttpResponse resp = ((TorLibApi.TorRequestResult.Success<HttpResponse>) result).getResult();

                        Log.d(TAG, "[3] Response from POST: ");
                        Log.d(TAG, "   status: " + resp.getStatus());
                        Log.d(TAG, "   version: " + resp.getVersion());
                        Log.d(TAG, "   headers: " + resp.getHeaders());
                        Log.d(TAG, "   body: " + new String(resp.getBody()));

                        response3.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<HttpResponse>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    signal.countDown();
                }
        );
        Log.d(TAG, "Third request sent");

        assertTrue(signal.await(120, TimeUnit.SECONDS));

        client1.close();
        client2.close();

        assertEquals(200, response1.get().getStatus());
        assertEquals(200, response2.get().getStatus());
        assertEquals(200, response3.get().getStatus());
    }

    @Test
    public void syncGet() {
        try {
            try (Client client = new Client(cacheDir)) {
                HttpResponse resp = client.syncTorRequest(
                        Client.TorRequestMethod.GET, "https://example.com", new HashMap<>(), new byte[]{});

                Log.d(TAG, "Response from GET: ");
                Log.d(TAG, "   status: " + resp.getStatus());
                Log.d(TAG, "   version: " + resp.getVersion());
                Log.d(TAG, "   headers: " + resp.getHeaders());
                Log.d(TAG, "   body: " + new String(resp.getBody()));

                assertEquals(200, resp.getStatus());
            }
        } catch (TorLibException e) {
            Log.d(TAG, "!!! Exception: " + e);
            fail();
        }
    }

    private TorLibApi.CacheUpdateStatus execUpdateCache() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        AtomicReference<TorLibApi.CacheUpdateStatus> status = new AtomicReference<>();

        api.updateCache(cacheDir,
                result -> {
                    if (result instanceof TorLibApi.TorRequestResult.Success) {
                        TorLibApi.CacheUpdateStatus resp = ((TorLibApi.TorRequestResult.Success<TorLibApi.CacheUpdateStatus>) result).getResult();
                        status.set(resp);
                    } else {
                        Exception e = ((TorLibApi.TorRequestResult.Error<TorLibApi.CacheUpdateStatus>) result).getError();
                        Log.d(TAG, "!!! Exception: " + e);
                        fail();
                    }

                    signal.countDown();
                }
        );

        signal.await(120, TimeUnit.SECONDS);

        return status.get();
    }

    @Test
    public void cacheIsUpToDate() throws InterruptedException {
        assertEquals(TorLibApi.CacheUpdateStatus.CACHE_IS_UP_TO_DATE, execUpdateCache());
    }

    @Test
    public void cacheChurnIsObsolete() throws InterruptedException {
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.DAY_OF_MONTH, -1);

        File f = new File(cacheDir, TorLibApi.CHURN_FILENAME);

        assertTrue(f.setLastModified(cal.getTimeInMillis()));
        assertEquals(TorLibApi.CacheUpdateStatus.DOWNLOADED_CHURN_FILE, execUpdateCache());

        assertTrue(f.delete());
        assertEquals(TorLibApi.CacheUpdateStatus.DOWNLOADED_CHURN_FILE, execUpdateCache());
    }

    @Test
    public void cacheMicroDescIsObsolete() throws InterruptedException {
        // Use UK locale to have Monday as the first day of the week
        Calendar cal = Calendar.getInstance(Locale.UK);

        // Roll to last day of previous week
        int currentWeek = cal.get(Calendar.WEEK_OF_MONTH);
        while (cal.get(Calendar.WEEK_OF_MONTH) == currentWeek) {
            cal.roll(Calendar.DAY_OF_MONTH, -1);
        }

        File f = new File(cacheDir, TorLibApi.MICRODESCRIPTORS_FILENAME);

        assertTrue(f.setLastModified(cal.getTimeInMillis()));
        assertEquals(TorLibApi.CacheUpdateStatus.DOWNLOADED_FULL_CACHE, execUpdateCache());

        assertTrue(f.delete());
        assertEquals(TorLibApi.CacheUpdateStatus.DOWNLOADED_FULL_CACHE, execUpdateCache());
    }

    @Test
    public void cacheIsMissingFiles() throws InterruptedException {
        File f = new File(cacheDir, TorLibApi.CERTIFICATE_FILENAME);
        assertTrue(f.delete());
        assertEquals(TorLibApi.CacheUpdateStatus.DOWNLOADED_FULL_CACHE, execUpdateCache());
    }
}
