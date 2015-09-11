package com.attensa.rubberband.query;

import lombok.Value;
import org.jooq.lambda.Seq;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@Value
public class RangeQuery implements QueryType {
    Map<String, Map<Comparator, String>> range;

    public RangeQuery(String field, Option... options) {
        this.range = Collections.singletonMap(field, Seq.seq(Arrays.stream(options)).toMap(Option::getComparator, Option::getValue));
    }

    @Value
    public static class Option {
        Comparator comparator;
        String value;

        public static Option gte(String value) {
            return new Option(Comparator.gte, value);
        }

        public static Option gt(String value) {
            return new Option(Comparator.gt, value);
        }

        public static Option lte(String value) {
            return new Option(Comparator.lte, value);
        }

        public static Option lt(String value) {
            return new Option(Comparator.lt, value);
        }
    }

    public enum Comparator {gte, gt, lt, lte}
}
