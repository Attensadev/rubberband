package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

@Value
public class FunctionScoreQuery implements QueryType {
    FunctionScore function_score;

    @Value
    public static class FunctionScore {
        QueryType query;
        List<ScoreFunction> functions;
        String score_mode;
    }

    public interface ScoreFunction {
    }

    @Value
    public static class FilterFunction implements ScoreFunction {
        InnerFilter filter;
        int weight;

        public FilterFunction(QueryType query, int weight) {
            this.filter = new InnerFilter(query);
            this.weight = weight;
        }
    }

    @Value
    static class InnerFilter {
        QueryType query;
    }

    @Value
    public static class GaussFunction implements ScoreFunction {
        Map<String, Gauss> gauss;
        int weight;

        public GaussFunction(String fieldName, int offsetDays, float scaleDays, float decay, int weight) {
            gauss = singletonMap(fieldName, new Gauss("now", offsetDays + "d", scaleDays + "d", decay));
            this.weight = weight;
        }
    }

    @Value
    static class Gauss {
        String origin;
        String offset;
        String scale;
        Float decay;
    }

    @Value
    public class FieldValueFactor implements FunctionScoreQuery.ScoreFunction {
        InnerFactor field_value_factor;

        public FieldValueFactor(String field, int factor, FieldValueFactorModifier modifier) {
            this.field_value_factor = new InnerFactor(field, factor, modifier);
        }
    }

    @Value
    static class InnerFactor {
        String field;
        int factor;
        FieldValueFactorModifier modifier;
    }

    public enum FieldValueFactorModifier {
        none, log, log1p, log2p, ln, ln1p, ln2p, square, sqrt, reciprocal;
    }

}
