package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class FilteredQuery implements QueryType {
    Filtered filtered;

    public FilteredQuery(QueryType query, QueryType filter) {
        this.filtered = new Filtered(query, new FilterQuery(filter, null));
    }

    public FilteredQuery(QueryType query, Map<String, List<String>> termsFilter) {
        this.filtered = new Filtered(query, new FilterQuery(null, termsFilter));
    }

    @Value
    public static class Filtered {
        QueryType query;
        FilterQuery filter;
    }

    @Value
    public static class FilterQuery {
        QueryType query;
        Map<String, List<String>> terms;
    }
}
