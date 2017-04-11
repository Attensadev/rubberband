package com.attensa.rubberband.tests;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.TestUtilities;
import com.attensa.rubberband.data.Page;
import com.attensa.rubberband.data.PageRequest;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.FunctionScoreQuery;
import com.attensa.rubberband.query.FunctionScoreQuery.RandomScoreFunction;
import com.attensa.rubberband.query.TermQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static com.attensa.rubberband.TestUtilities.waitForResultsToShowUp;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class QueryTest {

    private RubberbandClient client;

    @Before
    public void setup() {
        client = TestUtilities.buildClient();
        client.deleteIndex("animals");
    }

    @Test
    public void testQueryRandom() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        for (int i = 0; i < 10; i++) {
            client.create("animals", "cat", simon);
        }
        long results = waitForResultsToShowUp(client, SearchRequest.builder().query(new TermQuery("name", "simon")).build());
        assertTrue("found results: " + results, results > 0);

        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.builder().functions(singletonList(new RandomScoreFunction(UUID.randomUUID().toString()))).build();
        SearchRequest searchRequest = SearchRequest.builder().query(functionScoreQuery).build();
        Page<Cat> cats = client.query("animals", searchRequest, new PageRequest(1, 0), Cat.class);
        assertNotNull(cats);
        assertEquals(1, cats.getContents().size());
    }

}
