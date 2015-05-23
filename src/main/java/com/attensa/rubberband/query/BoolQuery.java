package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;

@Value
public class BoolQuery implements QueryType {
    Bool bool;

    public BoolQuery(List<QueryType> must, List<QueryType> should, List<QueryType> mustNot) {
        this.bool = new Bool(must, should, mustNot);
    }

    @Value
    public static class Bool {
        List<QueryType> must;
        List<QueryType> should;
        List<QueryType> must_not;
    }
}
