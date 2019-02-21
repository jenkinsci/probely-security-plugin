package com.probely.api;

import com.probely.util.ApiUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

public class ScanRequest {
    private final String authToken;
    private final String baseUrl;
    private final String target;
    private final CloseableHttpClient httpClient;
    private Scan scan;

    public ScanRequest(String authToken, String url, String target, CloseableHttpClient httpClient) {
        this.authToken = authToken;
        this.baseUrl = url;
        this.target = target;
        this.httpClient = httpClient;
    }

    public Scan start() throws IOException {
        String url = String.format("%s/%s/scan_now/", baseUrl, target);
        HttpPost request = new HttpPost(url);
        ApiUtils.addRequiredHeaders(authToken, request);
        String response = ApiUtils.post(httpClient, request);
        scan = ScanDeserializer.deserialize(response);
        return scan;
    }

    public Scan refresh() throws IOException {
        String url = String.format("%s/%s/scans/%s/", baseUrl, target, scan.id);
        HttpGet request = new HttpGet(url);
        ApiUtils.addRequiredHeaders(authToken, request);
        String response = ApiUtils.get(httpClient, request);
        scan = ScanDeserializer.deserialize(response);
        return scan;
    }

    public Scan stop() {
        return null;
    }
}
