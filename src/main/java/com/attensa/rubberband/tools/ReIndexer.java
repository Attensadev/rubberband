package com.attensa.rubberband.tools;

import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.data.ScrollContext;
import com.attensa.rubberband.data.ScrollResult;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.MatchAllQuery;
import org.jooq.lambda.Seq;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class ReIndexer {

    private final RubberbandClient sourceClient;
    private final RubberbandClient targetClient;

    @Inject
    public ReIndexer(RubberbandClient sourceClient, RubberbandClient targetClient) {
        this.sourceClient = sourceClient;
        this.targetClient = targetClient;
    }

    /**
     * Re-index all items in an index with the given type into another index, possible on another cluster.  This method works with documents that
     * contain their unique id, the field which must be specified as the idField parameter.
     *
     * @param sourceIndex       - The index from which to fetch documents.
     * @param type              - The type of document to re-index.
     * @param targetIndex       - The index (possibly on another cluster, based on the source & target clients) to re-index into.
     * @param documentsPerShard - The number of items to re-index per batch, per cluster shard.
     * @param idField           - the document field containing the unique id of the document.
     */
    public void reindex(String sourceIndex, String type, String targetIndex, int documentsPerShard, String idField) {
        SearchRequest searchRequest = new SearchRequest(new MatchAllQuery(), null, null);
        ScrollContext<Map> context = sourceClient.beginScanAndScroll(sourceIndex, type, searchRequest, documentsPerShard, "2m", Map.class);
        while (context.hasMore()) {
            ScrollResult<Map> data = sourceClient.continueScroll(context);
            List<Map> items = data.getData();
            Map<String, Object> indexedData = Seq.seq(items)
                    .toMap(item -> (String) item.get(idField), item -> item);

            targetClient.save(targetIndex, type, indexedData);
            context = data.getScrollContext();
        }
    }

    /**
     * Re-index all items in an index with the given type into another index, possible on another cluster.  This method works with documents that
     * do not contain their unique id and the document in the new index will have different elasticsearch _id values than the source index.
     *
     * @param sourceIndex       - The index from which to fetch documents.
     * @param type              - The type of document to re-index.
     * @param targetIndex       - The index (possibly on another cluster, based on the source and target clients) to re-index into.
     * @param documentsPerShard - The number of items to re-index per batch, per cluster shard.
     */
    public void reindexWithCreate(String sourceIndex, String type, String targetIndex, int documentsPerShard) {
        SearchRequest searchRequest = new SearchRequest(new MatchAllQuery(), null, null);
        ScrollContext<Object> context = sourceClient.beginScanAndScroll(sourceIndex, type, searchRequest, documentsPerShard, "2m", Object.class);
        while (context.hasMore()) {
            ScrollResult<Object> data = sourceClient.continueScroll(context);
            List<Object> items = data.getData();
            targetClient.create(targetIndex, type, items);
            context = data.getScrollContext();
        }
    }

}
