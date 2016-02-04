package com.attensa.rubberband.data;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.jooq.lambda.Seq.seq;

@Value
@AllArgsConstructor
public class Page<T> {
    List<T> contents;
    PageRequest request;
    long total;

    //here until a better api can be figured out.
    Map<String, Object> aggregations;

    public Page(List<T> contents, PageRequest request, long total) {
        this(contents, request, total, null);
    }

    public <S> Page<S> map(Function<T, S> conversion) {
        return new Page<>(seq(contents).map(conversion).toList(), request, total, aggregations);
    }
}
