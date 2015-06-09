package com.attensa.rubberband.data;

import com.attensa.rubberband.query.QueryType;
import lombok.Value;

import java.util.List;

@Value
public class SearchRequest {
    QueryType query;
    List<ElasticSearchSort> sort;
    List<String> _source;

    /**
     * Should consist of a field & a direction.  Something that turns into json like { "fieldName": "asc" }
     */
    public interface ElasticSearchSort {}
}
