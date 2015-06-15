package com.attensa.rubberband;

import com.attensa.rubberband.data.*;
import com.attensa.rubberband.data.internal.CountResponse;
import com.attensa.rubberband.data.internal.GetResponse;
import com.attensa.rubberband.data.internal.SearchResponse;
import com.attensa.rubberband.data.internal.SearchResponse.Hit;
import com.flightstats.http.HttpTemplate;
import com.google.gson.Gson;
import com.google.inject.util.Types;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static com.google.common.base.Charsets.UTF_8;
import static org.jooq.lambda.Seq.seq;

//todo: much better error handling and reporting!
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

    public void createIndex(String index, ElasticSearchMappings mappings) {
        httpTemplate.post(indexUrl(index), mappings);
    }

    public void deleteIndex(String index) {
        httpTemplate.delete(URI.create(indexUrl(index)));
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

    public void update(String index, String type, List<DocumentUpdate> updates) {
        String actionAndMetadataFormat = "{\"update\":{\"_index\":\"" + index + "\", \"_type\":\"" + type + "\", \"_id\": \"%s\"}}%n";
        StringBuilder requestBody = new StringBuilder();
        for (DocumentUpdate update : updates) {
            String bits = seq(update.getFieldUpdates().entrySet()).map(this::formatOne).join(",", "{", "}");
            requestBody.append(String.format(actionAndMetadataFormat, update.getDocumentId()));
            requestBody.append("{\"doc\": ").append(bits).append("}\n");
        }

        String content = requestBody.toString();
        httpTemplate.post(URI.create(elasticSearchUrl + "/_bulk"), content.getBytes(UTF_8), "text/plain");
    }

    public void update(String index, String type, DocumentUpdate update) {
        update(index, type, Collections.singletonList(update));
    }

    @SneakyThrows
    private String formatOne(Map.Entry<String, Object> entry) {
        return String.format("%s:%s", entry.getKey(), gson.toJson(entry.getValue()));
    }

    public void save(String index, String type, String id, Object item) {
        httpTemplate.put(singleItemUri(index, type, id), gson.toJson(item).getBytes(UTF_8), "application/json");
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

    public void delete(String index, String type, String id) {
        httpTemplate.delete(singleItemUri(index, type, id));
    }

    public void deleteByFieldValue(String index, String type, String fieldName, String value) {
        httpTemplate.delete(URI.create(String.format("%s?q=%s:%s", itemQueryUrl(index, type), fieldName, value)));
    }

    private String itemQueryUrl(String index, String type) {
        return indexTypeUrl(index, type) + "_query";
    }

    private URI singleItemUri(String index, String type, String id) {
        return URI.create(indexTypeUrl(index, type) + id);
    }

    public <T> Optional<T> get(String index, String type, String id, Class<T> documentType) {
        GetResponse<T> getResponse = httpTemplate.get(singleItemUri(index, type, id), Types.newParameterizedType(GetResponse.class, documentType));
        return Optional.ofNullable(getResponse.get_source());
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

    private String indexTypeUrl(String index, String type) {
        return indexUrl(index) + type + "/";
    }

    private String indexUrl(String index) {
        return elasticSearchUrl + "/" + index + "/";
    }

}