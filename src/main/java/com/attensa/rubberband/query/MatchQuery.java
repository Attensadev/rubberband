package com.attensa.rubberband.query;

import lombok.Value;

@Value
public class MatchQuery implements QueryType {
    AllMatch match;

    public MatchQuery(String query, String fuzziness) {
        this.match = new AllMatch(new Match(query, fuzziness));
    }

    /**
     * Query on the _all field.
     */
    @Value
    public static class AllMatch {
        Match _all;
    }

    @Value
    public static class Match {
        String query;
        String operator = null;
        String fuzziness;
        Integer prefix_length = null;
        String analyzer = "english";
    }
}
