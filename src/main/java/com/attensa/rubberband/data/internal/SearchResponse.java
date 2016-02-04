package com.attensa.rubberband.data.internal;

import lombok.Getter;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Getter
@Value
public class SearchResponse<T> {
    String _scroll_id;
    Hits<T> hits;
    Map<String, Object> aggregations;

    public long getTotal() {
        return hits.getTotal();
    }

    @Value
    public static class Hits<T> {
        long total;
        List<Hit<T>> hits;
    }

    @Value
    public static class Hit<T> {
        float _score;
        String _type;
        T _source;
    }

}
