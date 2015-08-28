package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class NestedQuery implements QueryType {
    Nested nested;

    public NestedQuery(String path, QueryType query) {
        this.nested = new Nested(path, query);
    }

    @Value
    public static class Nested {
        String path;
        QueryType query;
    }
}
