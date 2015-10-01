package com.attensa.rubberband.examples;

import com.attensa.rubberband.RubberbandClient;
import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CreateTest {

    private RubberbandClient client;

    @Before
    public void setup() {
        Gson gson = new Gson();
        Retryer<Response> retryer = new Retryer<>(StopStrategies.stopAfterAttempt(2), WaitStrategies.exponentialWait(), Attempt::hasException);
        HttpTemplate httpTemplate = new HttpTemplate(HttpClientBuilder.create().build(), gson, retryer);
        this.client = new RubberbandClient(httpTemplate, gson, "http://localhost:9200");
    }

    @Test
    public void testCreate() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        String id = client.create("animals", "cat", simon);
        simon = simon.withId(id);

        Cat modified = simon.withBreed("Main Coon");
        client.save("animals", "cat", modified.getId(), modified);

        Optional<Cat> retrieved = client.get("animals", "cat", id, Cat.class);
        assertTrue(retrieved.isPresent());
        assertEquals("Main Coon", retrieved.get().getBreed());
    }
}
