package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class MultiMatchQuery implements QueryType {
    MultiMatch multi_match;

    public MultiMatchQuery(String query, String[] fields) {
        this.multi_match = new MultiMatch(query, fields);
    }

    @Value
    public static class MultiMatch {
        String query;
        String[] fields;
    }
}
