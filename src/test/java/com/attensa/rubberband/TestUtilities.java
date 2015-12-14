package com.attensa.rubberband;

import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.flightstats.util.UUIDGenerator;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import org.apache.http.impl.client.HttpClientBuilder;

public class TestUtilities {

    public static RubberbandClient buildClient() {
        Gson gson = new Gson();
        Retryer<Response> retryer = new Retryer<>(StopStrategies.stopAfterAttempt(2), WaitStrategies.exponentialWait(), Attempt::hasException);
        HttpTemplate httpTemplate = new HttpTemplate(HttpClientBuilder.create().build(), gson, retryer, new UUIDGenerator());
        return new RubberbandClient(httpTemplate, gson, "http://localhost:9200");
    }
}
