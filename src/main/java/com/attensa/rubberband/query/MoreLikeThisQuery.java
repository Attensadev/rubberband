package com.attensa.rubberband.query;

import lombok.Value;

import java.util.List;

@Value
public class MoreLikeThisQuery implements QueryType {
    MoreLikeThis more_like_this;

    public MoreLikeThisQuery(String[] fields, List<Document> documents, String likeText, int minimumTermFrequency) {
        this.more_like_this = new MoreLikeThis(fields, documents, likeText, minimumTermFrequency);
    }

    @Value
    public static class MoreLikeThis {
        String[] fields;
        List<Document> docs;
        String like_text;
        int min_term_freq;
    }

    @Value
    public static class Document {
        String _index;
        String _type;
        String _id;
    }
}
