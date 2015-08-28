package com.attensa.rubberband.query;

import lombok.Value;

import java.util.Map;

import static java.util.Collections.singletonMap;

/**
 * A match query, that works on a specific field.
 */
@Value
public class MatchFieldQuery implements QueryType {
    Map<String, String> match;

    public MatchFieldQuery(String field, String value) {
        this.match = singletonMap(field, value);
    }
}
