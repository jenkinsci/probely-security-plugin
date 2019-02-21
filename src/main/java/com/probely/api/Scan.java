package com.probely.api;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Scan {
    public String id;
    public String status;
    @SerializedName("scan_profile")
    public String scanProfile;
    public int lows;
    public int mediums;
    public int highs;
    public Date started;
    public Date completed;
    public Date created;
}
