package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class FilteredQuery implements QueryType {
    Filtered filtered;

    public FilteredQuery(QueryType query, QueryType filter) {
        this.filtered = new Filtered(query, new FilterQuery(filter));
    }

    @Value
    public static class Filtered {
        QueryType query;
        FilterQuery filter;
    }

    @Value
    public static class FilterQuery {
        QueryType query;
    }
}
