package com.probely.api;

import com.probely.util.ApiUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ScanController {
    private final String authToken;
    private final String baseUrl;
    private final String target;
    private final CloseableHttpClient httpClient;
    private Scan scan;

    public ScanController(String authToken, String url, String target, CloseableHttpClient httpClient) {
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
        if (scan == null) {
            throw new RuntimeException("Scan is not available.");
        }
        String url = String.format("%s/%s/scans/%s/", baseUrl, target, scan.id);
        HttpGet request = new HttpGet(url);
        ApiUtils.addRequiredHeaders(authToken, request);
        String response = ApiUtils.get(httpClient, request);
        return ScanDeserializer.deserialize(response);
    }

    public Scan stop() {
        return null;
    }

    public Scan waitForChanges(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (scan == null) {
           throw new RuntimeException("Scan is not available.");
        }

        long start = Instant.now().getEpochSecond();
        while (Instant.now().getEpochSecond() - start < timeout) {
            Scan current = null;
            try {
                current = refresh();
            } catch (IOException ignored) {}
            if (current != null && !current.status.equals(scan.status)) {
                scan = current;
                break;
            }

            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException ignored) {}
        }
        return scan;
    }

    public Scan getScan() {
        return scan;
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException ignored) {
            // Ignored
        }
    }
}