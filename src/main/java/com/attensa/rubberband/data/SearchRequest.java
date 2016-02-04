package com.attensa.rubberband.data;

import com.attensa.rubberband.query.QueryType;
import lombok.*;
import lombok.experimental.Wither;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Wither
@AllArgsConstructor
public class SearchRequest {
    QueryType query;
    List<ElasticSearchSort> sort;
    List<String> _source;

    Integer min_score;
    Integer size;

    /**
     * Placeholder for aggregations, until a decent API can be figured out.
     */
    Map<String, Object> aggs;

    public SearchRequest(QueryType query, List<ElasticSearchSort> sort, List<String> source) {
        this.query = query;
        this.sort = sort;
        this._source = source;
        this.min_score = null;
        this.size = null;
        this.aggs = null;
    }

    /**
     * Should consist of a field &amp; a direction.  Something that turns into json like { "fieldName": "asc" }
     */
    public interface ElasticSearchSort {}
}
