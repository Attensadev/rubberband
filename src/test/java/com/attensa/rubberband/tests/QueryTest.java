package com.attensa.rubberband.tests;

import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.Cat;
import com.attensa.rubberband.TestUtilities;
import com.attensa.rubberband.data.Page;
import com.attensa.rubberband.data.PageRequest;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.FunctionScoreQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class QueryTest {

    private RubberbandClient client;

    @Before
    public void setup() {
        client = TestUtilities.buildClient();
    }

    @Test
    public void testCreateAndGet() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        String id = client.create("animals", "cat", simon);
        simon = simon.withId(id);

        Cat modified = simon.withBreed("Maine Coon");
        client.save("animals", "cat", modified.getId(), modified);

        Optional<Cat> retrieved = client.get("animals", "cat", id, Cat.class);
        assertTrue(retrieved.isPresent());
        assertEquals("Maine Coon", retrieved.get().getBreed());
    }

    @Test
    public void testQueryRandom() throws Exception {
        Cat simon = new Cat(null, "Simon", "mail", "unknown", "Well loved and now deceased.");
        client.create("animals", "cat", simon);

        FunctionScoreQuery functionScoreQuery = FunctionScoreQuery.builder().functions(singletonList(new FunctionScoreQuery.RandomScoreFunction(UUID.randomUUID().toString()))).build();
        SearchRequest searchRequest = SearchRequest.builder().query(functionScoreQuery).build();
        Page<Cat> cats = client.query("animals", searchRequest, new PageRequest(1, 0), Cat.class);
        assertNotNull(cats);
        assertEquals(1, cats.getContents().size());
    }

}
