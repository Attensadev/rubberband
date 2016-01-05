package com.attensa.rubberband;

import com.attensa.rubberband.data.SearchRequest;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;

public class TestUtilities {

    public static RubberbandClient buildClient() {
        return new RubberbandClient(new OkHttpClient(), new Gson(), "http://localhost:9200");
    }

    public static long waitForResultsToShowUp(RubberbandClient client, SearchRequest searchRequest) throws InterruptedException {
        long count;
        int tries = 0;
        do {
            count = client.count("animals", searchRequest);
            Thread.sleep(200);
        } while (count != 0 && tries++ < 10);
        return count;
    }

}
