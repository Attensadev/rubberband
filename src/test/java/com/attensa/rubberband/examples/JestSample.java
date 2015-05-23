package com.attensa.rubberband.examples;

import com.attensa.rubberband.misc.SearchRequest;
import com.attensa.rubberband.query.QueryStringQuery;
import com.google.gson.Gson;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import io.searchbox.core.Search.Builder;
import io.searchbox.core.SearchResult;
import io.searchbox.core.SearchResult.Hit;
import io.searchbox.indices.DeleteIndex;

import java.util.Arrays;
import java.util.List;

public class JestSample {
    public static void main(String[] args) throws Exception {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder("http://localhost:9200").build());
        JestClient jestClient = jestClientFactory.getObject();

        jestClient.execute(new DeleteIndex.Builder("animals").build());

        Cat winston = new Cat("1", "Winston", "male", "Turkish Van");
        Cat templeton = new Cat("2", "Templeton", "male", "American Shorthair");
        Cat zipper = new Cat("3", "Zipper", "female", "Longhair");
        Cat brownie = new Cat("4", "Brownie", "female", "Longhair");

        List<Index> indexings = Arrays.asList(
                new Index.Builder(winston).id(winston.getId()).build(),
                new Index.Builder(templeton).id(templeton.getId()).build(),
                new Index.Builder(zipper).id(zipper.getId()).build(),
                new Index.Builder(brownie).id(brownie.getId()).build());
        jestClient.execute(new Bulk.Builder().addAction(indexings).defaultIndex("animals").defaultType("cat").build());
        Thread.sleep(2000L);

        Gson gson = new Gson();

        SearchRequest searchRequest = new SearchRequest(new QueryStringQuery("gender: male", null, "_all"), null, null);
        String query = gson.toJson(searchRequest);
        SearchResult result = jestClient.execute(new Builder(query).addIndex("animals").addType("cat").build());
        List<Hit<Cat, Void>> hits = result.getHits(Cat.class);
        hits.forEach(hit -> System.out.println("hit.source = " + hit.source));

        jestClient.shutdownClient();
    }
}
