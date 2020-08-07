package com.probely.api;

import com.probely.util.ApiUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

public class UserController {
    private final String authToken;
    private final String baseUrl;
    private final CloseableHttpClient httpClient;

    public UserController(String authToken, String url, CloseableHttpClient httpClient) {
        this.authToken = authToken;
        this.baseUrl = url;
        this.httpClient = httpClient;
    }

    public User get() throws IOException {
        HttpGet request = new HttpGet(baseUrl);
        ApiUtils.addRequiredHeaders(authToken, request);
        String response = ApiUtils.get(httpClient, request);
        return UserDeserializer.deserialize(response);
    }
}
