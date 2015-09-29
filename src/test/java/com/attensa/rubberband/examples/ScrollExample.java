package com.attensa.rubberband.examples;

import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.data.ScrollContext;
import com.attensa.rubberband.data.ScrollResult;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.MatchAllQuery;
import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.List;

public class ScrollExample {

    public static void main(String[] args) {
        Gson gson = new Gson();
        Retryer<Response> retryer = new Retryer<>(StopStrategies.stopAfterAttempt(2), WaitStrategies.exponentialWait(), Attempt::hasException);
        HttpTemplate httpTemplate = new HttpTemplate(HttpClientBuilder.create().build(), gson, retryer);
        RubberbandClient client = new RubberbandClient(httpTemplate, gson, "http://localhost:9200");

        long totalSeen = 0;
        ScrollResult<Cat> page = client.beginScroll("animals", "cat", new SearchRequest(new MatchAllQuery(), null, null), 500, "1m", Cat.class);
        ScrollContext<Cat> context = page.getScrollContext();
        System.out.println("total to scroll through = " + context.getTotal());
        while (context.hasMore()) {
            System.out.print(".");
            List<Cat> data = page.getData();
            totalSeen += data.size();
            page = client.continueScroll(context);
            context = page.getScrollContext();
        }
        System.out.println("\ntotalSeen = " + totalSeen);
    }

}
