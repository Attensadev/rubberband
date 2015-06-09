package com.attensa.rubberband.data.internal;

import lombok.Getter;
import lombok.Value;

import java.util.List;

@Getter
@Value
public class SearchResponse<T> {
    Hits<T> hits;

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
