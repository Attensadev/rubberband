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

    public interface ScoreFunction {}

    @Value
    public static class FilterFunction implements ScoreFunction {
        InnerFilter filter;
        int weight;
    }

    @Value
    public static class InnerFilter {
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
    public static class Gauss {
        String origin;
        String offset;
        String scale;
        Float decay;
    }

}
