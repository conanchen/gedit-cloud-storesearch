package com.github.conanchen.gedit.storesearch.controller;

import com.google.gson.Gson;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

@RestController
@EnableAutoConfiguration
public class HelloController {
    private final static Gson gson = new Gson();
    private static final String POST_ID = "100019";
    //
//    @Autowired
//    private WordRepository wordRepository;
    @Value("${gedit.docker.enabled}")
    Boolean insideDocker = false;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @RequestMapping(value = "/hello")
    public String hello() {
        IndexResponse indexResponse = index();
        return
                String.format("hello@%s \n indexResponse=%s, \nHelloController Spring Boot insideDocker=%b",
                        DateFormat.getInstance().format(new Date()),
                        gson.toJson(indexResponse, IndexResponse.class),
                        insideDocker);
    }

    private IndexResponse index() {
        IndexResponse indexResponse = null;
        try {
            XContentBuilder builder = XContentFactory
                    .jsonBuilder()
                    .startObject()
                    .field("fullName", "Test")
                    .field("salary", "11500")
                    .field("age", "10")
                    .endObject();
            IndexRequest indexRequest = new IndexRequest("posts", "doc", POST_ID)
                    .source(builder);
            indexResponse = restHighLevelClient.index(indexRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return indexResponse;
    }
}