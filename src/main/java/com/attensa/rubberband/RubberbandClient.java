package com.attensa.rubberband;

import com.attensa.rubberband.data.*;
import com.attensa.rubberband.data.internal.CountResponse;
import com.attensa.rubberband.data.internal.GetResponse;
import com.attensa.rubberband.data.internal.SearchResponse;
import com.attensa.rubberband.data.internal.SearchResponse.Hit;
import com.flightstats.http.HttpException;
import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.google.gson.Gson;
import com.google.inject.util.Types;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Charsets.UTF_8;
import static org.jooq.lambda.Seq.seq;

//todo: currently, fava's HttpTemplate.get does not utilize the retryer. That needs to be fixed. Until then, gets here won't get automatically retried.
public class RubberbandClient {
    private static final Logger logger = LoggerFactory.getLogger(RubberbandClient.class);

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
        Response response = httpTemplate.delete(URI.create(indexUrl(index)));
        if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
            logger.info("No index with name \"" + index + "\" found to delete");
            return;
        }
        if (response.getCode() != HttpStatus.SC_OK) {
            throw new HttpException(new HttpException.Details(response.getCode(), response.getBodyString(UTF_8)));
        }
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
        Response response = httpTemplate.post(URI.create(elasticSearchUrl + "/_bulk"), content.getBytes(UTF_8), "text/plain");
        checkResponse(response);
    }

    public void update(String index, String type, DocumentUpdate update) {
        update(index, type, Collections.singletonList(update));
    }

    @SneakyThrows
    private String formatOne(Map.Entry<String, Object> entry) {
        return String.format("%s:%s", entry.getKey(), gson.toJson(entry.getValue()));
    }

    public void save(String index, String type, String id, Object item) {
        Response response = httpTemplate.put(singleItemUri(index, type, id), gson.toJson(item).getBytes(UTF_8), "application/json");
        checkResponse(response);
    }

    public long count(String index, SearchRequest searchRequest) {
        logger.debug("Running count query on index: " + index + " : " + gson.toJson(searchRequest));
        String searchUrl = indexUrl(index) + "_count";
        AtomicLong result = new AtomicLong();
        httpTemplate.postWithNoResponseCodeValidation(searchUrl, searchRequest, response -> {
            checkResponse(response);
            CountResponse countResponse = gson.fromJson(response.getBodyString(UTF_8), CountResponse.class);
            result.set(countResponse.getCount());
        });
        return result.get();
    }

    private void checkResponse(Response response) {
        if (HttpStatus.SC_OK > response.getCode() || response.getCode() > HttpStatus.SC_NO_CONTENT) {
            throw new HttpException(new HttpException.Details(response.getCode(), response.getBodyString(UTF_8)));
        }
    }

    public <T> Page<T> query(String index, SearchRequest searchRequest, PageRequest pageRequest, Class<T> documentType) {
        logger.debug("Running search query on index: " + index + " : " + gson.toJson(searchRequest));
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
        GetResponse<T> getResponse = null;
        try {
            getResponse = httpTemplate.get(singleItemUri(index, type, id), Types.newParameterizedType(GetResponse.class, documentType));
        } catch (HttpException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
        return Optional.ofNullable(getResponse.get_source());
    }

    private <T> List<T> makeItems(SearchResponse<T> response) {
        return seq(response.getHits().getHits()).map(Hit::get_source).toList();
    }

    private <T> SearchResponse<T> makeSearchRequest(String searchUrl, SearchRequest searchRequest, Class<T> documentType) {
        ParameterizedType type = Types.newParameterizedType(SearchResponse.class, documentType);
        AtomicReference<SearchResponse<T>> result = new AtomicReference<>();
        httpTemplate.postWithNoResponseCodeValidation(searchUrl, searchRequest, response -> {
            checkResponse(response);
            SearchResponse<T> searchResponse = gson.fromJson(response.getBodyString(UTF_8), type);
            result.set(searchResponse);
        });
        return result.get();
    }

    private String indexTypeUrl(String index, String type) {
        return indexUrl(index) + type + "/";
    }

    private String indexUrl(String index) {
        return elasticSearchUrl + "/" + index + "/";
    }

}