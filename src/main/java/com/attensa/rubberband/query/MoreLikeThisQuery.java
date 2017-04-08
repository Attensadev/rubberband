package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;

@Value
public class MoreLikeThisQuery implements QueryType {
    MoreLikeThis more_like_this;

    public MoreLikeThisQuery(String[] fields, List<String> ids, String likeText, int minimumTermFrequency) {
        this.more_like_this = new MoreLikeThis(fields, ids, likeText, minimumTermFrequency);
    }

    @Value
    public static class MoreLikeThis {
        String[] fields;
        List<String> ids;
        String like_text;
        int min_term_freq;
    }

}
