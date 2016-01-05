package com.attensa.rubberband;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;

public class TestUtilities {

    public static RubberbandClient buildClient() {
        return new RubberbandClient(new OkHttpClient(), new Gson(), "http://localhost:9200");
    }
}
