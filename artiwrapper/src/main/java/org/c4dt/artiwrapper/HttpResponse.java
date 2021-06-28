package org.c4dt.artiwrapper;

import java.util.List;
import java.util.Map;

public class HttpResponse {
    private int status;
    private String version;
    private Map<String, List<String>> headers;
    private byte[] body;

    private String data;

    public HttpResponse(int status, String version, Map<String, List<String>> headers, byte[] body) {
        this.status = status;
        this.version = version;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}
