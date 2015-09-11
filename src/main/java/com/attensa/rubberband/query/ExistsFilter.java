package com.attensa.rubberband.query;

import lombok.Value;

import java.util.Map;

import static java.util.Collections.singletonMap;

@Value
public class ExistsFilter implements QueryType {
    Map<String, String> exists;

    public ExistsFilter(String field) {
        this.exists = singletonMap("field", field);
    }

}