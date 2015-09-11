package com.attensa.rubberband.data;

import lombok.Value;
import org.jooq.lambda.Seq;

import java.util.List;
import java.util.function.Function;

import static org.jooq.lambda.Seq.seq;

@Value
public class Page<T> {
    List<T> contents;
    PageRequest request;
    long total;

    public <S> Page<S> map(Function<T, S> conversion) {
        return new Page<>(seq(contents).map(conversion).toList(), request, total);
    }
}
