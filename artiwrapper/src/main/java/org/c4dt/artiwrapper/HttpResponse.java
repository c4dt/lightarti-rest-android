package org.c4dt.artiwrapper;

import java.util.List;
import java.util.Map;

/**
 * Request response returned by the library.
 */
public class HttpResponse {
    private int status;
    private String version;
    private Map<String, List<String>> headers;
    private byte[] body;

    private HttpResponse(int status, String version, Map<String, List<String>> headers, byte[] body) {
        this.status = status;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Get the response status code (e.g. 200 == OK).
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get the response HTTP version (e.g. "HTTP/1.1").
     *
     * @return the HTTP version of the response
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the response headers.
     *
     * @return the HTTP headers of the response
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Get the response body.
     *
     * @return the body of the response
     */
    public byte[] getBody() {
        return body;
    }
}
