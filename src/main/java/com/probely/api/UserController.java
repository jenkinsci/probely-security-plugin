package com.probely.api;

import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpGet;

public class UserController {
  private final Client api;

  public UserController(Client api) {
    this.api = api;
  }

  public User get() throws IOException {
    HttpGet request = new HttpGet("");
    String response = api.execute(request);
    return UserDeserializer.deserialize(response);
  }
}
