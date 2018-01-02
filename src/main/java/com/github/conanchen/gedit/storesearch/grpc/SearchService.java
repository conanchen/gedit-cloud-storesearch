package com.github.conanchen.gedit.storesearch.grpc;

import com.github.conanchen.gedit.storesearch.StoreIndex;
import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

@GRpcService(interceptors = {LogInterceptor.class})
public class SearchService extends StoresearchGrpc.StoresearchImplBase {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void index(IndexRequest request, StreamObserver<IndexResponse> responseObserver) {
        try {
            XContentBuilder builder = XContentFactory
                    .jsonBuilder()
                    .startObject()
                    .field(StoreIndex.FIELD_NAME, request.getName())
                    .field(StoreIndex.FIELD_LAT, request.getLat())
                    .field(StoreIndex.FIELD_LON, request.getLon())
                    .field(StoreIndex.FIELD_TYPE, request.getType())
                    .field(StoreIndex.FIELD_DESC, request.getDesc())
                    .field(StoreIndex.FIELD_BONUSRATE, request.getBonusRate())
                    .endObject();
            //    string id = 1; //store id
            //    string name = 2;
            //    string logo = 3;
            //    int64 lat = 4;
            //    int64 lon = 5;
            //    string type = 6;
            //    string desc = 7;
            //    int32 bonusRate = 8;

            org.elasticsearch.action.index.IndexRequest indexRequest =
                    new org.elasticsearch.action.index.
                            IndexRequest(StoreIndex.INDEX, StoreIndex.TYPE, request.getId())
                            .source(builder);
            restHighLevelClient.indexAsync(indexRequest, new ActionListener<org.elasticsearch.action.index.IndexResponse>() {
                @Override
                public void onResponse(org.elasticsearch.action.index.IndexResponse indexResponse) {
                    IndexResponse result = IndexResponse.newBuilder()
                            .setId(indexResponse.getId())
                            .setStatus(String.format("%s", indexResponse.status().name()))
                            .build();
                    responseObserver.onNext(result);
                    responseObserver.onCompleted();
                }

                @Override
                public void onFailure(Exception e) {
                    responseObserver.onCompleted();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        org.elasticsearch.action.delete.DeleteRequest deleteRequest = new org.elasticsearch.action.delete.DeleteRequest(
                StoreIndex.INDEX,
                StoreIndex.TYPE,
                request.getId());

        restHighLevelClient.deleteAsync(deleteRequest, new ActionListener<org.elasticsearch.action.delete.DeleteResponse>() {
            @Override
            public void onResponse(org.elasticsearch.action.delete.DeleteResponse deleteResponse) {
                String index = deleteResponse.getIndex();
                String type = deleteResponse.getType();
                String id = deleteResponse.getId();
                long version = deleteResponse.getVersion();
                ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                        String reason = failure.reason();
                    }
                }

                DeleteResponse result = DeleteResponse
                        .newBuilder()
                        .setId(id)
                        .setStatus(String.format("%s", deleteResponse.status().name()))
                        .build();
                responseObserver.onNext(result);
                responseObserver.onCompleted();
            }

            @Override
            public void onFailure(Exception e) {
                responseObserver.onCompleted();
            }
        });
    }

    @Override
    public void search(SearchRequest request, StreamObserver<StoreResponse> responseObserver) {
        org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search
                .SearchRequest(StoreIndex.INDEX)
                .types(StoreIndex.TYPE);
        restHighLevelClient.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                SearchHit[] searchHits = searchResponse.getHits().getHits();
                for (int i = 0; i < searchHits.length; i++) {
                    SearchHit hit = searchHits[i];
                    String sourceAsString = hit.getSourceAsString();
                    System.out.println(String.format("hit: %s", sourceAsString));
                    StoreResponse storeResponse = buildStoreResponse(request.getFrom() + i, hit);
                    responseObserver.onNext(storeResponse);
                }
                responseObserver.onCompleted();
            }

            @Override
            public void onFailure(Exception e) {
                responseObserver.onCompleted();
            }
        });

    }

    private StoreResponse buildStoreResponse(int from, SearchHit hit) {
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        String name = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_NAME, "NO_NAMAE");
        String logo = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_LOGO, "NO_LOGO");
        Long lat = (Long) sourceAsMap.getOrDefault(StoreIndex.FIELD_LAT, 0l);
        Long lon = (Long) sourceAsMap.getOrDefault(StoreIndex.FIELD_LON, 0l);
        String type = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_TYPE, "FOOD");
        String desc = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_DESC, "NO_DESC");
        Integer bonusRate = (Integer) sourceAsMap.getOrDefault(StoreIndex.FIELD_BONUSRATE, 0);
        StoreResponse storeResponse = StoreResponse
                .newBuilder()
                .setId(hit.getId())
                .setName(name)
                .setLogo(logo)
                .setLat(lat)
                .setLon(lon)
                .setType(type)
                .setDesc(desc)
                .setBonusRate(bonusRate)
                .setFrom(from)
                .build();
        return storeResponse;
    }

}