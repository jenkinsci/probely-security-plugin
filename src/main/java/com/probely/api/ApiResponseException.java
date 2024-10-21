package com.probely.api;

import org.apache.hc.client5.http.ClientProtocolException;

public class ApiResponseException extends ClientProtocolException {
  private static final long serialVersionUID = 1L;

  public ApiResponseException(String s) {
    super(s);
  }
}
