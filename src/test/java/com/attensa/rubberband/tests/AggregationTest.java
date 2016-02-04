package com.attensa.rubberband.tests;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.TestUtilities;
import com.attensa.rubberband.data.Page;
import com.attensa.rubberband.data.PageRequest;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.MatchAllQuery;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class AggregationTest {
    private RubberbandClient client;

    @Before
    public void setUp() throws Exception {
        client = TestUtilities.buildClient();
        client.deleteIndex("animals");
    }

    @Test
    public void testSimpleAggregation() throws Exception {
        client.create("animals", "cat", Arrays.asList(
                new Cat("1", "Simon", "male", "Maine Coon", "Fluffy and gone"),
                new Cat("2", "Zipper", "female", "Longhair", "Fluffy"),
                new Cat("3", "Templeton", "male", "Shorthair", "old")
        ));

        int tries = 0;
        while (tries++ < 10 && client.count("animals", SearchRequest.builder().query(new MatchAllQuery()).build()) < 3) {
            Thread.sleep(500);
        }

        List<Map<String, Object>> expected = Arrays.asList(ImmutableMap.of("key", "male", "doc_count", 2.0), ImmutableMap.of("key", "female", "doc_count", 1.0));

        SearchRequest searchRequest = SearchRequest.builder()
                .aggs(singletonMap("genders", singletonMap("terms", singletonMap("field", "gender"))))
                .build();
        Page<Cat> results = client.query("animals", searchRequest, new PageRequest(10, 0), Cat.class);
        Map<String, Object> aggregations = results.getAggregations();
        assertEquals(expected, ((Map) aggregations.get("genders")).get("buckets"));
    }sup

}
