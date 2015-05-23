package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class HasChildQuery implements QueryType {
    HasChild has_child;

    public HasChildQuery(String childType, QueryType query) {
        this.has_child = new HasChild(childType, query);
    }

    @Value
    public static class HasChild {
        String type;
        QueryType query;
    }
}
