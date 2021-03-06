package com.attensa.rubberband.examples;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.data.ElasticSearchMappings;
import com.attensa.rubberband.data.Page;
import com.attensa.rubberband.data.PageRequest;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.QueryStringQuery;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.attensa.rubberband.data.ElasticSearchMappings.Config;
import static com.attensa.rubberband.data.ElasticSearchMappings.PropertyContainer;
import static java.util.Collections.singletonMap;
import static org.jooq.lambda.Seq.seq;

//TODO: it would be great to turn this into an automated integration test.
public class IndexAndQuery {
    public static void main(String[] args) throws InterruptedException {
        RubberbandClient client = new RubberbandClient(new OkHttpClient(), new Gson(), "http://localhost:9200");

        Cat winston = new Cat("1", "Winston", "male", "Turkish Van", "A very fluffy, friendly cat.");
        Cat templeton = new Cat("2", "Templeton", "male", "American Shorthair", "The old man of the family. Black, and always hungry.");
        Cat zipper = new Cat("3", "Zipper", "female", "Longhair", "Skittish mama of Brownie.");
        Cat brownie = new Cat("4", "Brownie", "female", "Longhair", "Zipper's daughter. The least friendly of the crew. Hates the boys and yowls at them often.");
        List<Cat> cats = Arrays.asList(templeton, zipper, brownie);

        client.deleteIndex("animals");

        client.createIndex("animals", createCatMappings());

        //save a single one
        client.save("animals", "cat", winston.getId(), winston);
        //save a few using the bulk API
        client.save("animals", "cat", seq(cats).toMap(Cat::getId, c -> c));

        SearchRequest searchRequest = new SearchRequest(new QueryStringQuery("gender: male", null, "_all"), null, null);

        //loop for a bit because ES takes a bit to make saved items searchable, after indexing.
        waitForResultsToShowUp(client, searchRequest, 2);

        Page<Cat> result = client.query("animals", searchRequest, new PageRequest(20, 0), Cat.class);
        result.getContents().forEach(System.out::println);

        client.delete("animals", "cat", winston.getId());
        waitForResultsToShowUp(client, searchRequest, 1);
        System.out.println("After deletion:");
        result = client.query("animals", searchRequest, new PageRequest(20, 0), Cat.class);
        result.getContents().forEach(System.out::println);

        Optional<Cat> savedZipper = client.get("animals", "cat", zipper.getId(), Cat.class);
        System.out.println("savedZipper = " + savedZipper);

        Optional<Cat> deleted = client.get("animals", "cat", winston.getId(), Cat.class);
        System.out.println("deleted = " + deleted);
    }

    private static ElasticSearchMappings createCatMappings() {
        Config descriptionOptions = new Config("string", null, true, "english", null, null);
        Map<String, Config> properties = ImmutableMap.of("description", descriptionOptions);
        Map<String, PropertyContainer> typeMapping = singletonMap("cat", new PropertyContainer(properties));
        return new ElasticSearchMappings(typeMapping, new ElasticSearchMappings.Settings(null, 3, 1));
    }

    private static void waitForResultsToShowUp(RubberbandClient client, SearchRequest searchRequest, int expected) throws InterruptedException {
        long count;
        int tries = 0;
        do {
            count = client.count("animals", searchRequest);
            Thread.sleep(200);
        } while (count != expected && tries++ < 10);
        System.out.println("number of results = " + count);
    }

}
