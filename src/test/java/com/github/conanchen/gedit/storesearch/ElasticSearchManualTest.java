package com.github.conanchen.gedit.storesearch;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElasticSearchManualTest {
    private List<Person> listOfPersons = new ArrayList<>();
    private RestHighLevelClient client = null;
    private String POST_ID_1 = "1";
    private String POST_ID_2 = "2";

    @Before
    public void setUp() {
        Person person1 = new Person(10, "John Doe", new Date());
        Person person2 = new Person(25, "Janette Doe", new Date());
        listOfPersons.add(person1);
        listOfPersons.add(person2);
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")
                ));
//                ,
//                        new HttpHost("localhost", 9201, "http")));
    }

    @After
    public void close() {
        try {
            Thread.sleep(5000);
            client.deleteAsync(new DeleteRequest("posts", "doc", POST_ID_1), new ActionListener<DeleteResponse>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {

                }

                @Override
                public void onFailure(Exception e) {

                }
            });
            client.deleteAsync(new DeleteRequest("posts", "doc", POST_ID_2), new ActionListener<DeleteResponse>() {
                @Override
                public void onResponse(DeleteResponse deleteResponse) {

                }

                @Override
                public void onFailure(Exception e) {

                }
            });
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void givenJsonString_whenJavaObject_thenIndexDocument() {
        IndexRequest request = new IndexRequest(
                "posts",
                "doc",
                POST_ID_1);
        String jsonString = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2013-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        request.source(jsonString, XContentType.JSON);

        try {
            IndexResponse indexResponse = client.index(request);
            String index = indexResponse.getIndex();
            String type = indexResponse.getType();
            String id = indexResponse.getId();
            assertEquals(RestStatus.OK, indexResponse.status());
            assertEquals(index, "posts");
            assertEquals(type, "doc");
            assertEquals(id, POST_ID_1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void givenContentBuilder_whenHelpers_thanIndexJson() throws IOException {
        XContentBuilder builder = XContentFactory
                .jsonBuilder()
                .startObject()
                .field("fullName", "Test")
                .field("salary", "11500")
                .field("age", "10")
                .endObject();
        IndexRequest indexRequest = new IndexRequest("posts", "doc", POST_ID_2)
                .source(builder);
        try {
            IndexResponse indexResponse = client.index(indexRequest);
            assertEquals(RestStatus.OK, indexResponse.status());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void givenDocumentId_whenJavaObject_thenDeleteDocument() {
        DeleteRequest request = new DeleteRequest(
                "posts",
                "doc",
                POST_ID_1);

        client.deleteAsync(request, new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
                String index = deleteResponse.getIndex();
                String type = deleteResponse.getType();
                String id = deleteResponse.getId();
                long version = deleteResponse.getVersion();
                assertEquals(POST_ID_1, id);
                ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                        String reason = failure.reason();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }

    @Test
    public void givenSearchRequest_whenMatchAll_thenReturnAllResults() {
        SearchRequest searchRequest = new SearchRequest("posts");
        searchRequest.types("doc");
        client.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                SearchHit[] searchHits = searchResponse
                        .getHits()
                        .getHits();
//                List<Person> results = new ArrayList<>();
                for (SearchHit hit : searchHits) {
                    String sourceAsString = hit.getSourceAsString();
                    System.out.println(String.format("hit: %s", sourceAsString));
//                    Person person = JSON.parseObject(sourceAsString, Person.class);
//                    results.add(person);
                }
            }

            @Override
            public void onFailure(Exception e) {

            }
        });


    }

//    @Test
//    public void givenSearchParameters_thenReturnResults() {
//        SearchResponse response = client
//                .prepareSearch()
//                .setTypes()
//                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
//                .setPostFilter(QueryBuilders
//                        .rangeQuery("age")
//                        .from(5)
//                        .to(15))
//                .setFrom(0)
//                .setSize(60)
//                .setExplain(true)
//                .execute()
//                .actionGet();
//
//        SearchResponse response2 = client
//                .prepareSearch()
//                .setTypes()
//                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
//                .setPostFilter(QueryBuilders.simpleQueryStringQuery("+John -Doe OR Janette"))
//                .setFrom(0)
//                .setSize(60)
//                .setExplain(true)
//                .execute()
//                .actionGet();
//
//        SearchResponse response3 = client
//                .prepareSearch()
//                .setTypes()
//                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
//                .setPostFilter(QueryBuilders.matchQuery("John", "Name*"))
//                .setFrom(0)
//                .setSize(60)
//                .setExplain(true)
//                .execute()
//                .actionGet();
//        response2.getHits();
//        response3.getHits();
//        List<SearchHit> searchHits = Arrays.asList(response
//                .getHits()
//                .getHits());
//        final List<Person> results = new ArrayList<>();
//        searchHits.forEach(hit -> results.add(JSON.parseObject(hit.getSourceAsString(), Person.class)));
//    }
//
}