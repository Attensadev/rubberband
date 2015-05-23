package com.attensa.rubberband;

import com.attensa.rubberband.misc.*;
import com.attensa.rubberband.misc.SearchResponse.Hit;
import com.flightstats.http.HttpTemplate;
import com.google.gson.Gson;
import com.google.inject.util.Types;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Charsets.UTF_8;
import static org.jooq.lambda.Seq.seq;

public class RubberbandClient {

    private final HttpTemplate httpTemplate;
    private final Gson gson;
    private final String elasticSearchUrl;

    @Inject
    public RubberbandClient(HttpTemplate httpTemplate, Gson gson, @Named("elasticSearchUrl") String elasticSearchUrl) {
        this.httpTemplate = httpTemplate;
        this.gson = gson;
        this.elasticSearchUrl = elasticSearchUrl;
    }

    public void createIndex(String indexName, ElasticSearchMappings mappings) {
        httpTemplate.post(indexUrl(indexName), mappings);
    }

    public void deleteIndex(String indexName) {
        httpTemplate.delete(URI.create(indexUrl(indexName)));
    }

    public void save(String index, String type, Map<String, Object> documentsById) {
        String actionAndMetadataFormat = "{\"index\":{\"_index\":\"" + index + "\", \"_type\":\"" + type + "\", \"_id\": \"%s\"}}%n";

        StringBuilder requestBody = new StringBuilder();
        for (Entry<String, Object> idAndDocument : documentsById.entrySet()) {
            requestBody.append(String.format(actionAndMetadataFormat, idAndDocument.getKey()));
            requestBody.append(gson.toJson(idAndDocument.getValue())).append("\n");
        }

        String content = requestBody.toString();
        httpTemplate.post(URI.create(elasticSearchUrl + "/_bulk"), content.getBytes(UTF_8), "text/plain");
    }

    public void save(String indexName, String type, String id, Object item) {
        httpTemplate.put(URI.create(indexTypeUrl(indexName, type) + id), gson.toJson(item).getBytes(UTF_8), "application/json");
    }

    public long count(String index, SearchRequest searchRequest) {
        String searchUrl = indexUrl(index) + "_count";
        return httpTemplate.post(searchUrl, searchRequest, s -> {
            CountResponse countResponse = gson.fromJson(s, CountResponse.class);
            return countResponse.getCount();
        });
    }

    public <T> Page<T> query(String index, SearchRequest searchRequest, PageRequest pageRequest, Class<T> documentType) {
        String searchUrl = indexUrl(index) + "_search?from=" + (pageRequest.getPageNumber() * pageRequest.getSize()) + "&size=" + pageRequest.getSize();
        SearchResponse<T> response = makeSearchRequest(searchUrl, searchRequest, documentType);
        return new Page<>(makeItems(response), pageRequest, response.getHits().getTotal());
    }

    private <T> List<T> makeItems(SearchResponse<T> response) {
        return seq(response.getHits().getHits()).map(Hit::get_source).toList();
    }

    private <T> SearchResponse<T> makeSearchRequest(String searchUrl, SearchRequest searchRequest, Class<T> documentType) {
        return httpTemplate.post(searchUrl, searchRequest, s -> {
            ParameterizedType type = Types.newParameterizedType(SearchResponse.class, documentType);
            return gson.fromJson(s, type);
        });
    }

    private String indexTypeUrl(String indexName, String type) {
        return indexUrl(indexName) + type + "/";
    }

    private String indexUrl(String indexName) {
        return elasticSearchUrl + "/" + indexName + "/";
    }

}