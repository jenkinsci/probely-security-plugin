package com.probely.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ScanDeserializer {
  public static Scan deserialize(String data) {
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(data, Scan.class);
  }
}
