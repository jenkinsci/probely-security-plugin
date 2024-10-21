package com.probely.api;

import org.apache.hc.client5.http.ClientProtocolException;

public class AuthenticationException extends ClientProtocolException {
  private static final long serialVersionUID = 1L;

  public AuthenticationException(String s) {
    super(s);
  }
}
