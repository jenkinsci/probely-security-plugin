package com.probely.api;

import org.apache.http.client.ClientProtocolException;

public class AuthenticationException extends ClientProtocolException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String s) {
        super(s);
    }
}
