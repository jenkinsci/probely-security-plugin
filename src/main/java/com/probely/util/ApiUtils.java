package com.probely.util;

import com.probely.api.AuthenticationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApiUtils {
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int API_TIMEOUT_MS = 30000;

    public static CloseableHttpClient buildHttpClient(int timeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(config);
        return builder.build();
    }

    public static CloseableHttpClient buildHttpClient() {
        return buildHttpClient(API_TIMEOUT_MS);
    }

    public static void addRequiredHeaders(String authToken, HttpRequestBase request) {
        String token = "JWT " + authToken;
        request.addHeader(HttpHeaders.AUTHORIZATION, token);
        request.addHeader(HttpHeaders.ACCEPT, CONTENT_TYPE_JSON);
        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
    }

    public static String get(CloseableHttpClient httpClient,
                             HttpGet request) throws IOException {
        CloseableHttpResponse resp = httpClient.execute(request);
        String result = handleResponse(resp);
        request.releaseConnection();
        return result;
    }

    public static String post(CloseableHttpClient httpClient,
                              HttpPost request) throws IOException {
        CloseableHttpResponse resp = httpClient.execute(request);
        String result = handleResponse(resp);
        request.releaseConnection();
        return result;
    }

    private static String handleResponse(CloseableHttpResponse resp) throws IOException {
        try {
            int status = resp.getStatusLine().getStatusCode();
            if (status == 401) {
                throw new AuthenticationException(resp.getStatusLine().getReasonPhrase());
            }
            if (status < 200 || status >= 300) {
                throw new ClientProtocolException(resp.getStatusLine().getReasonPhrase());
            }
            HttpEntity entity = resp.getEntity();
            if (entity == null) {
                throw new ClientProtocolException("Empty server response.");
            }
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } finally {
            if (resp != null) resp.close();
        }
    }

    public static void closeHttpClient(CloseableHttpClient httpClient) {
        try {
            httpClient.close();
        } catch (IOException ignored) {
            // Ignore
        }
    }
}

