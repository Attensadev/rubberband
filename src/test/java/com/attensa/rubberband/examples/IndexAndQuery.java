package com.attensa.rubberband.examples;

import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.misc.Page;
import com.attensa.rubberband.misc.PageRequest;
import com.attensa.rubberband.misc.SearchRequest;
import com.attensa.rubberband.query.QueryStringQuery;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.jooq.lambda.Seq.seq;

public class IndexAndQuery {
    public static void main(String[] args) throws InterruptedException {
        Gson gson = new Gson();
        Retryer<Response> retryer = new Retryer<>(StopStrategies.stopAfterAttempt(2), WaitStrategies.exponentialWait(), Attempt::hasException);
        HttpTemplate httpTemplate = new HttpTemplate(HttpClientBuilder.create().build(), gson, retryer);
        RubberbandClient client = new RubberbandClient(httpTemplate, gson, "http://localhost:9200");

        Cat winston = new Cat("1", "Winston", "male", "Turkish Van");
        Cat templeton = new Cat("2", "Templeton", "male", "American Shorthair");
        Cat zipper = new Cat("3", "Zipper", "female", "Longhair");
        Cat brownie = new Cat("4", "Brownie", "female", "Longhair");
        List<Cat> cats = newArrayList(templeton, zipper, brownie);

        client.deleteIndex("animals");
        //save a single one
        client.save("animals", "cat", winston.getId(), winston);
        //save a few using the bulk API
        client.save("animals", "cat", seq(cats).toMap(Cat::getId, c -> c));

        SearchRequest searchRequest = new SearchRequest(new QueryStringQuery("gender: male", null, "_all"), null, null);

        //loop for a bit because ES takes a bit to make saved items searchable, after indexing.
        waitForResultsToShowUp(client, searchRequest);

        Page<Cat> result = client.query("animals", searchRequest, new PageRequest(20, 0), Cat.class);
        result.getContents().forEach(System.out::println);
    }

    private static void waitForResultsToShowUp(RubberbandClient client, SearchRequest searchRequest) throws InterruptedException {
        long count;
        int tries = 0;
        do {
            count = client.count("animals", searchRequest);
            Thread.sleep(200);
        } while (count < 2 && tries++ < 10);
        System.out.println("number of results = " + count);
    }

}
