package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class NestedQuery implements QueryType {
    Nested nested;

    @Value
    public static class Nested {
        String path;
        QueryType query;
    }
}
