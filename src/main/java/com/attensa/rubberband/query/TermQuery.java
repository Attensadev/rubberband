package com.attensa.rubberband.query;

import lombok.Value;

import java.util.Map;

import static java.util.Collections.singletonMap;

@Value
public class TermQuery implements QueryType {
    Map<String, String> term;

    public TermQuery(String field, String value) {
        this.term = singletonMap(field, value);
    }

}
