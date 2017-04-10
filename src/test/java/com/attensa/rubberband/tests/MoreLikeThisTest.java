package com.attensa.rubberband.tests;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.TestUtilities;
import com.attensa.rubberband.data.Page;
import com.attensa.rubberband.data.PageRequest;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.MatchAllQuery;
import com.attensa.rubberband.query.MoreLikeThisQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

public class MoreLikeThisTest {
    private RubberbandClient client;

    @Before
    public void setUp() throws Exception {
        client = TestUtilities.buildClient();
        client.deleteIndex("animals");
    }

    @Test
    public void testMoreLikeThis() throws Exception {
        //note: it looks like unless you have some minimum number of documents, mlt doesn't return anything.
        // This totally may chnage with ES versions, though. This worked with ES 1.7.6
        for (int i = 1; i <= 20; i++) {
            Cat tigger = new Cat("" + i, "Tigger " + i, "male", "American Shorthair", null);
            client.save("animals", "cat", tigger.getId(), tigger);
        }

        //wait for the docs to be indexed and available
        int tries = 0;
        while (tries++ < 10 && client.count("animals", SearchRequest.builder().query(new MatchAllQuery()).build()) < 9) {
            Thread.sleep(500);
        }

        SearchRequest mlt = SearchRequest.builder()
                .query(new MoreLikeThisQuery(null, singletonList("2"), null, 1))
                ._source(Arrays.asList("name", "gender", "breed", "description"))
                .build();
        PageRequest pageRequest = new PageRequest(5, 0);
        Page<Cat> result = client.query("animals", mlt, pageRequest, Cat.class);

        assertTrue(result.getTotal() >= 5);
    }
}
