package com.attensa.rubberband;

import com.attensa.rubberband.data.*;
import com.attensa.rubberband.data.internal.CountResponse;
import com.attensa.rubberband.data.internal.CreateResponse;
import com.attensa.rubberband.data.internal.GetResponse;
import com.attensa.rubberband.data.internal.SearchResponse;
import com.flightstats.http.HttpException;
import com.flightstats.http.HttpTemplate;
import com.flightstats.http.Response;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.inject.util.Types.newParameterizedType;
import static org.jooq.lambda.Seq.seq;

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

    public void create(String index, String type, List<Object> documents) {
        String actionAndMetadataFormat = "{\"create\":{\"_index\":\"" + index + "\", \"_type\":\"" + type + "\"}}\n";

        StringBuilder requestBody = new StringBuilder();
        for (Object document : documents) {
            requestBody.append(actionAndMetadataFormat);
            requestBody.append(gson.toJson(document)).append("\n");
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
        checkResponseAggressive(response);
    }

    public void save(String index, String type, String id, Object item) {
        Response response = httpTemplate.put(singleItemUri(index, type, id), gson.toJson(item).getBytes(UTF_8), "application/json");
        checkResponseAggressive(response);
    }

    /**
     * @param index The index to create the item in.
     * @param type  The type of the document.
     * @param item  the document to create.
     * @return The elasticsearch _id of the newly created document.
     */
    public String create(String index, String type, Object item) {
        Response response = httpTemplate.post(URI.create(indexTypeUrl(index, type)), gson.toJson(item).getBytes(UTF_8), "application/json");
        checkResponseAggressive(response);
        CreateResponse createResponse = gson.fromJson(response.getBodyString(), CreateResponse.class);
        return createResponse.get_id();
    }

    public void update(String index, String type, DocumentUpdate update) {
        update(index, type, Collections.singletonList(update));
    }

    @SneakyThrows
    private String formatOne(Map.Entry<String, Object> entry) {
        return String.format("%s:%s", entry.getKey(), gson.toJson(entry.getValue()));
    }

    private void checkResponseAggressive(Response response) {
        if (HttpStatus.SC_OK > response.getCode() || response.getCode() > HttpStatus.SC_NO_CONTENT) {
            throw new HttpException(new HttpException.Details(response.getCode(), response.getBodyString(UTF_8)));
        }
    }

    public long count(String index, SearchRequest searchRequest) {
        logger.debug("Running count query on index: " + index + " : " + gson.toJson(searchRequest));
        String searchUrl = indexUrl(index) + "_count";
        AtomicLong result = new AtomicLong(-1L);
        httpTemplate.postWithNoResponseCodeValidation(searchUrl, searchRequest, response -> {
            int status = checkResponse(response);
            if (status == HttpStatus.SC_BAD_REQUEST) {
                return;
            }
            CountResponse countResponse = gson.fromJson(response.getBodyString(UTF_8), CountResponse.class);
            result.set(countResponse.getCount());
        });
        if (result.get() == -1L) {
            throw new RuntimeException("Count request failed. Details should be in the logs.");
        }
        return result.get();
    }

    private int checkResponse(Response response) {
        if (HttpStatus.SC_BAD_REQUEST == response.getCode()) {
            logger.warn("400 Bad Request: " + response.getBodyString());
            return response.getCode();
        }
        checkResponseAggressive(response);
        return response.getCode();
    }

    public <T> Page<T> query(String index, SearchRequest searchRequest, PageRequest pageRequest, Class<T> documentType) {
        return query(index, searchRequest, pageRequest, (Type) documentType);
    }

    public <T> Page<T> query(String index, SearchRequest searchRequest, PageRequest pageRequest, Type documentType) {
        Page<ScoredItem<T>> scoredItems = queryWithScores(index, searchRequest, pageRequest, documentType);
        return scoredItems.map(ScoredItem::getItem);
    }

    public <T> Page<ScoredItem<T>> queryWithScores(String index, SearchRequest searchRequest, PageRequest pageRequest, Class<T> type) {
        return queryWithScores(index, searchRequest, pageRequest, (Type) type);
    }

    public <T> Page<ScoredItem<T>> queryWithScores(String index, SearchRequest searchRequest, PageRequest pageRequest, Type type) {
        logger.debug("Running search query on index: " + index + " : " + gson.toJson(searchRequest));
        String searchUrl = indexUrl(index) + "_search?from=" + (pageRequest.getPageNumber() * pageRequest.getSize()) + "&size=" + pageRequest.getSize();
        SearchResponse<T> response = makeSearchRequest(searchUrl, searchRequest, type);
        return new Page<>(makeScoredItems(response), pageRequest, response.getHits().getTotal());
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
        return get(index, type, id, (Type) documentType);
    }

    public <T> Optional<T> get(String index, String type, String id, Type documentType) {
        GetResponse<T> getResponse;
        try {
            getResponse = httpTemplate.get(singleItemUri(index, type, id), newParameterizedType(GetResponse.class, documentType));
        } catch (HttpException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
        return Optional.ofNullable(getResponse.get_source());
    }

    private <T> List<ScoredItem<T>> makeScoredItems(SearchResponse<T> response) {
        return seq(response.getHits().getHits())
                .map(hit -> new ScoredItem<>(hit.get_source(), hit.get_score()))
                .toList();
    }

    private <T> SearchResponse<T> makeSearchRequest(String searchUrl, SearchRequest searchRequest, Type documentType) {
        ParameterizedType type = newParameterizedType(SearchResponse.class, documentType);
        AtomicReference<SearchResponse<T>> result = new AtomicReference<>();
        httpTemplate.postWithNoResponseCodeValidation(searchUrl, searchRequest, response -> {
            int status = checkResponse(response);
            if (status == HttpStatus.SC_BAD_REQUEST) {
                return;
            }
            SearchResponse<T> searchResponse = gson.fromJson(response.getBodyString(UTF_8), type);
            result.set(searchResponse);
        });
        if (result.get() == null) {
            throw new RuntimeException("Search request failed. Details should be in the logs.");
        }
        return result.get();
    }

    private String indexTypeUrl(String index, String type) {
        return indexUrl(index) + type + "/";
    }

    private String indexUrl(String index) {
        return elasticSearchUrl + "/" + index + "/";
    }

    /**
     * Uses the "scan" type, which disables sorting for optimal retrieval of data.
     * <br>
     * Returns the the scroll context to be used to initiate the scroll.
     *
     * @param <T>               : The type of document (matches the documentType)
     * @param index             : The index to scan
     * @param type              : The type to scan
     * @param searchRequest     : The search request to use for the scan
     * @param documentsPerShard : The number of documents to return, per shard, per request.
     * @param timeToKeepAlive   : How long to keep the scroll alive.  This should be something like "1m". See ES docs for options.
     * @param docType           : The type of document to be returned
     * @return The ScrollContext to use for scrolling through the results.
     * @see #continueScroll(ScrollContext)
     */
    public <T> ScrollContext<T> beginScanAndScroll(String index, String type, SearchRequest searchRequest, int documentsPerShard, String timeToKeepAlive, Type docType) {
        String searchUrl = indexTypeUrl(index, type) + "_search/?search_type=scan&scroll=" + timeToKeepAlive;
        SearchResponse<T> response = makeSearchRequest(searchUrl, searchRequest.withSize(documentsPerShard), docType);
        return new ScrollContext<>(response.get_scroll_id(), timeToKeepAlive, docType, true, response.getTotal());
    }

    /**
     * @param <T>               : The type of document (matches the documentType)
     * @param index               : The index to scan
     * @param type                : The type to scan
     * @param searchRequest       : The search request to use for the scan
     * @param documentsPerRequest : The number of documents to return, per request.
     * @param timeToKeepAlive     : How long to keep the scroll alive.  This should be something like "1m". See ES docs for options.
     * @param documentType        : The type of the documents.
     * @return The initial page of results, with the context to use for the next page.
     * @see #continueScroll(ScrollContext)
     */
    public <T> ScrollResult<T> beginScroll(String index, String type, SearchRequest searchRequest, int documentsPerRequest, String timeToKeepAlive, Type documentType) {
        String searchUrl = indexTypeUrl(index, type) + "_search/?scroll=" + timeToKeepAlive;
        SearchResponse<T> response = makeSearchRequest(searchUrl, searchRequest.withSize(documentsPerRequest), documentType);
        List<ScoredItem<T>> scoredResults = makeScoredItems(response);
        List<T> data = seq(scoredResults).map(ScoredItem::getItem).toList();
        return new ScrollResult<>(new ScrollContext<>(response.get_scroll_id(), timeToKeepAlive, documentType, true, response.getTotal()), data);
    }

    /**
     * Be sure to always pass in the context from the previous ScrollResult that was received.
     *
     * @param <T>     : The type of document being scrolled over.
     * @param context : The ScrollContext returned from the previous batch of results.
     * @return The next ScrollContext.
     *
     * @see #beginScanAndScroll(String, String, SearchRequest, int, String, Type)
     * @see #beginScroll(String, String, SearchRequest, int, String, Type)
     */
    public <T> ScrollResult<T> continueScroll(ScrollContext<T> context) {
        String searchUrl = elasticSearchUrl + "/_search/scroll?scroll=" + context.getKeepAliveTime() + "&scroll_id=" + context.getScrollId();
        SearchResponse<T> response = makeSearchRequest(searchUrl, null, context.getDocumentType());
        List<ScoredItem<T>> scoredResults = makeScoredItems(response);
        List<T> data = seq(scoredResults).map(ScoredItem::getItem).toList();
        ScrollContext<T> updatedContext = context
                .withScrollId(response.get_scroll_id())
                .withHasMore(data.size() > 0);
        return new ScrollResult<>(updatedContext, data);
    }

}