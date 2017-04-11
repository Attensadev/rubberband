package com.attensa.rubberband;

import com.attensa.rubberband.data.SearchRequest;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import org.junit.Ignore;

@Ignore
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
        } while (count == 0L && tries++ < 100);
        if (count == 0) {
            throw new IllegalStateException("nothing ever showed up");
        }
        return count;
    }

}
