package com.probely.api;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class ScanController implements HttpClientResponseHandler<String> {
  private final String target;
  private Client api;
  private Scan scan;

  public ScanController(Client api, String target) {
    this.api = api;
    this.target = target;
  }

  public Scan start() throws IOException {
    String url = String.format("/%s/scan_now/", target);
    HttpPost request = new HttpPost(url);
    String response = api.execute(request, this);
    scan = ScanDeserializer.deserialize(response);
    return scan;
  }

  public Scan refresh() throws IOException {
    if (scan == null) {
      throw new RuntimeException("Scan is not available.");
    }
    String url = String.format("/%s/scans/%s/", target, scan.id);
    HttpGet request = new HttpGet(url);
    String response = api.execute(request, this);
    return ScanDeserializer.deserialize(response);
  }

  public Scan stop() throws IOException {
    if (scan == null) {
      throw new RuntimeException("Scan is not available.");
    }
    String url = String.format("/%s/scans/%s/cancel/", target, scan.id);
    HttpPost request = new HttpPost(url);
    String response = api.execute(request, this);
    scan = ScanDeserializer.deserialize(response);
    return scan;
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
      } catch (IOException ignored) {
        // Ignored
      }
      if (current != null && !current.status.equals(scan.status)) {
        scan = current;
        break;
      }

      try {
        TimeUnit.SECONDS.sleep(60);
      } catch (InterruptedException ignored) {
      }
    }
    return scan;
  }

  public Scan getScan() {
    return scan;
  }

  public void close() {
    if (api == null) {
      return;
    }
    api.close();
    this.api = null;
  }

  public String handleResponse(ClassicHttpResponse response)
      throws AuthenticationException, ApiResponseException {

    int status = response.getCode();
    if (status == 404) {
      throw new ApiResponseException("Target does not exist.");
    }

    // Use the generic response handler as fallback.
    return api.handleResponse(response);
  }
}
