package com.probely.api;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Scan {
    private static final String[] runningStatuses = {"queued", "started"};
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

    public String toString() {
        return String.format(
                "id: %s | status: %s | vulnerabilities: high: %d, medium: %d, low: %d",
                id, status, highs, mediums, lows);
    }

    public boolean isRunning() {
        for (String s : runningStatuses) {
            if (s.equals(status)) {
                return true;
            }
        }
        return false;
    }
}