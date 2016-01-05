package com.attensa.rubberband;

import com.attensa.rubberband.data.*;
import com.attensa.rubberband.data.internal.CountResponse;
import com.attensa.rubberband.data.internal.CreateResponse;
import com.attensa.rubberband.data.internal.GetResponse;
import com.attensa.rubberband.data.internal.SearchResponse;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.jooq.lambda.Seq.seq;

public class RubberbandClient {
    private static final Logger logger = LoggerFactory.getLogger(RubberbandClient.class);
    public static final MediaType APPLICATION_JSON = MediaType.parse("application/json");
    public static final MediaType TEXT_PLAIN = MediaType.parse("text/plain");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String elasticSearchUrl;

    public RubberbandClient(OkHttpClient httpClient, Gson gson, String elasticSearchUrl) {
        this.httpClient = httpClient;
        this.gson = gson;
        this.elasticSearchUrl = elasticSearchUrl;
    }

    private Request getRequest(String url) {
        return new Request.Builder().url(url).build();
    }

    private Request jsonPutRequest(String url, Object data) {
        return new Request.Builder().url(url).put(RequestBody.create(APPLICATION_JSON, gson.toJson(data))).build();
    }

    private Request jsonPostRequest(String url, Object data) {
        return new Request.Builder().url(url).post(RequestBody.create(APPLICATION_JSON, gson.toJson(data))).build();
    }

    private Request deleteRequest(String url) {
        return new Request.Builder().delete().url(url).build();
    }

    @SneakyThrows
    private void postJson(Object data, String url) {
        post(url, RequestBody.create(APPLICATION_JSON, gson.toJson(data)));
    }

    @SneakyThrows
    private void post(String url, RequestBody body) {
        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RubberbandException(response.code(), response.body().string());
        }
    }

    public void createIndex(String index, Object mappings) {
        postJson(mappings, indexUrl(index));
    }

    @SneakyThrows
    public void deleteIndex(String index) {
        Response response = httpClient.newCall(deleteRequest(indexUrl(index))).execute();
        if (response.code() == 404) {
            logger.info("No index with name \"" + index + "\" found to delete");
            return;
        }
        if (response.code() != 200) {
            throw new RubberbandException(response.code(), response.body().string());
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
        post(elasticSearchUrl + "/_bulk", RequestBody.create(TEXT_PLAIN, content.getBytes(UTF_8)));
    }

    public void create(String index, String type, List<Object> documents) {
        String actionAndMetadataFormat = "{\"create\":{\"_index\":\"" + index + "\", \"_type\":\"" + type + "\"}}\n";

        StringBuilder requestBody = new StringBuilder();
        for (Object document : documents) {
            requestBody.append(actionAndMetadataFormat);
            requestBody.append(gson.toJson(document)).append("\n");
        }

        String content = requestBody.toString();
        post(elasticSearchUrl + "/_bulk", RequestBody.create(TEXT_PLAIN, content.getBytes(UTF_8)));
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
        post(elasticSearchUrl + "/_bulk", RequestBody.create(TEXT_PLAIN, content.getBytes(UTF_8)));
    }

    @SneakyThrows
    public void save(String index, String type, String id, Object item) {
        Request put = jsonPutRequest(singleItemUri(index, type, id).toString(), item);
        checkResponseAggressive(httpClient.newCall(put).execute());
    }

    /**
     * @param index The index to create the item in.
     * @param type  The type of the document.
     * @param item  the document to create.
     * @return The elasticsearch _id of the newly created document.
     */
    @SneakyThrows
    public String create(String index, String type, Object item) {
        Request post = jsonPostRequest(indexTypeUrl(index, type), item);
        Response response = httpClient.newCall(post).execute();
        checkResponseAggressive(response);
        CreateResponse createResponse = gson.fromJson(response.body().string(), CreateResponse.class);
        return createResponse.get_id();
    }

    @SneakyThrows
    private void checkResponseAggressive(Response response) {
        if (200 > response.code() || response.code() > 204) {
            throw new RubberbandException(response.code(), response.body().string());
        }
    }

    public void update(String index, String type, DocumentUpdate update) {
        update(index, type, singletonList(update));
    }

    @SneakyThrows
    private String formatOne(Map.Entry<String, java.lang.Object> entry) {
        return String.format("%s:%s", entry.getKey(), gson.toJson(entry.getValue()));
    }

    @SneakyThrows
    public long count(String index, SearchRequest searchRequest) {
        logger.debug("Running count query on index: " + index + " : " + gson.toJson(searchRequest));
        String searchUrl = indexUrl(index) + "_count";

        Request request = jsonPostRequest(searchUrl, searchRequest);
        Response response = httpClient.newCall(request).execute();
        int status = checkResponse(response);
        if (status == 400) {
            throw new RuntimeException("Count request failed. Details should be in the logs.");
        }
        CountResponse countResponse = gson.fromJson(response.body().string(), CountResponse.class);
        return countResponse.getCount();
    }

    @SneakyThrows
    private int checkResponse(Response response) {
        if (400 == response.code()) {
            logger.warn("400 Bad Request: " + response.body().string());
            return response.code();
        }
        checkResponseAggressive(response);
        return response.code();
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

    @SneakyThrows
    public void delete(String index, String type, String id) {
        httpClient.newCall(deleteRequest(singleItemUri(index, type, id).toString())).execute();
    }

    @SneakyThrows
    public void deleteByFieldValue(String index, String type, String fieldName, String value) {
        httpClient.newCall(deleteRequest(String.format("%s?q=%s:%s", itemQueryUrl(index, type), fieldName, value))).execute();
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

    @SneakyThrows
    public <T> Optional<T> get(String index, String type, String id, Type documentType) {
        Request getRequest = getRequest(singleItemUri(index, type, id).toString());
        Response response = httpClient.newCall(getRequest).execute();
        if (response.code() == 404) {
            return Optional.empty();
        }
        GetResponse<T> getResponse = gson.fromJson(response.body().string(), parameterize(GetResponse.class, documentType));
        return Optional.ofNullable(getResponse.get_source());
    }

    private <T> List<ScoredItem<T>> makeScoredItems(SearchResponse<T> response) {
        return seq(response.getHits().getHits())
                .map(hit -> new ScoredItem<>(hit.get_source(), hit.get_score()))
                .toList();
    }

    @SneakyThrows
    private <T> SearchResponse<T> makeSearchRequest(String searchUrl, SearchRequest searchRequest, Type documentType) {
        Request request = jsonPostRequest(searchUrl, searchRequest);
        Response response = httpClient.newCall(request).execute();
        int status = checkResponse(response);
        if (status == 400) {
            throw new RuntimeException("Search request failed. Details should be in the logs.");
        }
        return gson.fromJson(response.body().string(), parameterize(SearchResponse.class, documentType));
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
     * @param <T>                 : The type of document (matches the documentType)
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