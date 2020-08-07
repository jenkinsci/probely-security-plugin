package com.probely.plugin;

public class Settings {
    public static final String API_URL = "https://api.probely.com";
    public static final String API_TARGET_URL = API_URL + "/targets";
    public static final String API_PROFILE_URL = API_URL + "/profile/";

    public static final String PLUGIN_DISPLAY_NAME = "Probely Security Scanner";

    public static final String MSG_CREDS_VALID = "Credentials verified successfully";

    public static final String ERR_CREDS_INVALID = "Invalid credentials";
    public static final String ERR_CREDS_NOT_FOUND = "Credentials not found";
    public static final String ERR_API_CONN = "Could not connect to the API";
}
