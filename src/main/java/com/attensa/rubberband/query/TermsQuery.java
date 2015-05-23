package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

@Value
public class TermsQuery implements QueryType {
    Map<String, List<String>> terms;

    public TermsQuery(String field, List<String> values) {
        this.terms = singletonMap(field, values);
    }
}
