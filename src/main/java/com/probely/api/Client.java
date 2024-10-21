package com.probely.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

public class Client implements HttpClientResponseHandler<String> {
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64; +https://probely.com/sos) AppleWebKit/537.36"
          + " (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36 ProbelyJK/1.0.0";
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final int API_TIMEOUT = 30;

  private final CloseableHttpClient httpClient;
  private final String baseUrl;
  private final String token;

  public Client(String baseUrl, String token, int timeout) {
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    this.baseUrl = baseUrl;
    this.token = token;
    Timeout t = Timeout.ofSeconds(timeout);

    RequestConfig config =
        RequestConfig.custom().setConnectionRequestTimeout(t).setResponseTimeout(t).build();
    this.httpClient =
        HttpClients.custom().setDefaultRequestConfig(config).setUserAgent(USER_AGENT).build();
  }

  public Client(String baseUrl, String token) {
    this(baseUrl, token, API_TIMEOUT);
  }

  // Adds required headers to the request
  private void addRequiredHeaders(HttpUriRequest request) {
    request.addHeader(HttpHeaders.AUTHORIZATION, "JWT " + token);
    request.addHeader(HttpHeaders.ACCEPT, CONTENT_TYPE_JSON);
    request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
  }

  public String execute(HttpUriRequest request, HttpClientResponseHandler<String> handler)
      throws IOException {
    try {
      // Make sure that the URI path does not start with a slash.
      String reqPath = request.getUri().getPath();
      if (reqPath.startsWith("/")) {
        reqPath = reqPath.substring(1);
      }

      URI uri = new URI(baseUrl + reqPath);
      request.setUri(uri);
    } catch (URISyntaxException | InvalidParameterException e) {
      throw new InvalidParameterException("Invalid URI: " + e);
    }
    addRequiredHeaders(request);
    return httpClient.execute(request, handler != null ? handler : this);
  }

  // Closes the HTTP client safely
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      // Ignore
    }
  }

  public String execute(HttpUriRequest request) throws IOException {
    return execute(request, null);
  }

  // Default response handler
  @Override
  public String handleResponse(ClassicHttpResponse response)
      throws AuthenticationException, ApiResponseException {
    int status = response.getCode();
    if (status >= 200 && status < 300) {
      HttpEntity entity = response.getEntity();
      try {
        return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : null;
      } catch (ParseException | IOException e) {
        throw new ApiResponseException("Failed to fetch response entity" + e);
      }
    } else if (status == 401) {
      throw new AuthenticationException(response.getReasonPhrase());
    } else {
      throw new ApiResponseException("Unexpected response status: " + status);
    }
  }

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }
}
