package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static java.util.Collections.min;
import static java.util.Collections.singletonMap;

@Value
public class FunctionScoreQuery implements QueryType {
    FunctionScore function_score;

    private FunctionScoreQuery(QueryType query, List<ScoreFunction> functions, String scoreMode, String boostMode, Float minScore, Float maxBoost, Float boost) {
        function_score = new FunctionScore(query, functions, scoreMode, boostMode, minScore, maxBoost, boost);
    }

    public static FunctionScoreQueryBuilder build() {
        return new FunctionScoreQueryBuilder();
    }

    @Value
    public static class FunctionScore {
        QueryType query;
        List<ScoreFunction> functions;
        String score_mode;
        String boost_mode;
        Float min_score;
        Float max_boost;
        Float boost;
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
    public static class FieldValueFactor implements FunctionScoreQuery.ScoreFunction {
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

    public static class FunctionScoreQueryBuilder {
        private QueryType query;
        private List<ScoreFunction> functions;
        private String scoreMode;
        private String boostMode;
        private Float minScore;
        private Float maxBoost;
        private Float boost;

        private FunctionScoreQueryBuilder() {
        }

        public FunctionScoreQueryBuilder query(QueryType query) {
            this.query = query;
            return this;
        }

        public FunctionScoreQueryBuilder functions(List<ScoreFunction> functions) {
            this.functions = functions;
            return this;
        }

        public FunctionScoreQueryBuilder scoreMode(String scoreMode) {
            this.scoreMode = scoreMode;
            return this;
        }

        public FunctionScoreQueryBuilder boostMode(String boostMode) {
            this.boostMode = boostMode;
            return this;
        }

        public FunctionScoreQueryBuilder minScore(Float minScore) {
            this.minScore = minScore;
            return this;
        }

        public FunctionScoreQueryBuilder maxBoost(Float maxBoost) {
            this.maxBoost = maxBoost;
            return this;
        }

        public FunctionScoreQueryBuilder boost(Float boost) {
            this.boost = boost;
            return this;
        }

        public FunctionScoreQuery createFunctionScoreQuery() {
            return new FunctionScoreQuery(query, functions, scoreMode, boostMode, minScore, maxBoost, boost);
        }
    }
}
