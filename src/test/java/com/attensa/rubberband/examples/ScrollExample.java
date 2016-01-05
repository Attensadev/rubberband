package com.attensa.rubberband.examples;

import com.attensa.rubberband.Cat;
import com.attensa.rubberband.RubberbandClient;
import com.attensa.rubberband.data.ScrollContext;
import com.attensa.rubberband.data.ScrollResult;
import com.attensa.rubberband.data.SearchRequest;
import com.attensa.rubberband.query.MatchAllQuery;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;

import java.util.List;

public class ScrollExample {

    public static void main(String[] args) {
        RubberbandClient client = new RubberbandClient(new OkHttpClient(), new Gson(), "http://localhost:9200");

        long totalSeen = 0;
        ScrollResult<Cat> page = client.beginScroll("animals", "cat", new SearchRequest(new MatchAllQuery(), null, null), 500, "1m", Cat.class);
        ScrollContext<Cat> context = page.getScrollContext();
        System.out.println("total to scroll through = " + context.getTotal());
        while (context.hasMore()) {
            System.out.print(".");
            List<Cat> data = page.getData();
            totalSeen += data.size();
            page = client.continueScroll(context);
            context = page.getScrollContext();
        }
        System.out.println("\ntotalSeen = " + totalSeen);
    }

}
