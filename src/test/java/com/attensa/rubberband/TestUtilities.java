package com.attensa.rubberband;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.junit.Ignore;

@Ignore
public class TestUtilities {

    public static RubberbandClient buildClient() {
        return new RubberbandClient(new OkHttpClient(), new Gson(), "http://localhost:9200");
    }
}
