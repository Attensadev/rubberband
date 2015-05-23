package com.attensa.rubberband.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
public class QueryStringQuery implements QueryType {
    QueryString query_string;

    public QueryStringQuery(String query, String[] fields, String defaultField, Boolean analyzeWildcard, String defaultOperator, String minimumShouldMatch) {
        this.query_string = new QueryString(query, fields, defaultField, analyzeWildcard, defaultOperator, minimumShouldMatch);
    }

    public QueryStringQuery(String query, String[] fields, String defaultField, Boolean analyzeWildcard, String defaultOperator) {
        this(query, fields, defaultField, analyzeWildcard, defaultOperator, null);
    }

    public QueryStringQuery(String query, String[] fields, String defaultField) {
        this(query, fields, defaultField, null, null);
    }

    @Value
    @Builder
    public static class QueryString {
        String query;
        String[] fields;
        String default_field;
        Boolean analyze_wildcard;
        String default_operator;
        String minimum_should_match;
    }

}
