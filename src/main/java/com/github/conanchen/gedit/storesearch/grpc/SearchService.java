package com.github.conanchen.gedit.storesearch.grpc;

import com.github.conanchen.gedit.common.grpc.Location;
import com.github.conanchen.gedit.common.grpc.Status;
import com.github.conanchen.gedit.store.search.grpc.*;
import com.github.conanchen.gedit.storesearch.StoreIndex;
import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;

import static io.grpc.Status.Code.OK;
@Slf4j
@GRpcService(interceptors = {LogInterceptor.class})
public class SearchService extends StoreSearchApiGrpc.StoreSearchApiImplBase {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    private static final Gson gson = new Gson();

    @Override
    public void index(IndexStoreRequest request, StreamObserver<IndexStoreResponse> responseObserver) {
        try {
            XContentBuilder builder = XContentFactory
                    .jsonBuilder()
                    .startObject()
                    .field(StoreIndex.FIELD_NAME, request.getName())
                    .field(StoreIndex.FIELD_LAT, request.getLocation().getLat())
                    .field(StoreIndex.FIELD_LON, request.getLocation().getLon())
                    .field(StoreIndex.FIELD_TYPE, request.getType())
                    .field(StoreIndex.FIELD_DESC, request.getDesc())
                    .field(StoreIndex.FIELD_BONUSRATE, request.getBonusRate())
                    .endObject();

            org.elasticsearch.action.index.IndexRequest indexRequest =
                    new org.elasticsearch.action.index.
                            IndexRequest(StoreIndex.INDEX, StoreIndex.TYPE, request.getUuid())
                            .source(builder);
            restHighLevelClient.indexAsync(indexRequest, new ActionListener<org.elasticsearch.action.index.IndexResponse>() {
                @Override
                public void onResponse(org.elasticsearch.action.index.IndexResponse indexResponse) {
                    IndexStoreResponse result = IndexStoreResponse.newBuilder()
                            .setUuid(indexResponse.getId())
                            .setStatus(Status.newBuilder()
                                    .setCode(String.valueOf(OK.value()))
                                    .setDetails("success")
                                    .build())
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
    public void delete(DeleteStoreRequest request, StreamObserver<DeleteStoreResponse> responseObserver) {
        org.elasticsearch.action.delete.DeleteRequest deleteRequest = new org.elasticsearch.action.delete.DeleteRequest(
                StoreIndex.INDEX,
                StoreIndex.TYPE,
                request.getUuid());

        restHighLevelClient.deleteAsync(deleteRequest, new ActionListener<org.elasticsearch.action.delete.DeleteResponse>() {
            @Override
            public void onResponse(org.elasticsearch.action.delete.DeleteResponse deleteResponse) {
                String index = deleteResponse.getIndex();
                String type = deleteResponse.getType();
                String id = deleteResponse.getId();
                long version = deleteResponse.getVersion();
                log.info("async delete request,uuid[{}],index[{}],type[{}],version[{}]",id,index,type,version);
                ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
                    log.info("shard delete fail ,shard total[{}],actually success[{}]",shardInfo.getTotal(),shardInfo.getSuccessful());
                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                        String reason = failure.reason();
                        log.info("shard delete fail reason :[{}]",reason);
                    }
                }

                DeleteStoreResponse result = DeleteStoreResponse
                        .newBuilder()
                        .setUuid(id)
                        .setStatus(Status.newBuilder()
                                .setCode(String.valueOf(OK.value()))
                                .setDetails("success")
                                .build())
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
    public void search(SearchStoreRequest request, StreamObserver<SearchStoreResponse> responseObserver) {
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
                    log.info(String.format("hit: %s", sourceAsString));
                    SearchStoreResponse storeResponse = buildStoreResponse(request.getFrom() + i, hit);
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

    private SearchStoreResponse buildStoreResponse(int from, SearchHit hit) {
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        String name = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_NAME, "NO_NAMAE");
        String logo = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_LOGO, "NO_LOGO");
        Long lat = (Long) sourceAsMap.getOrDefault(StoreIndex.FIELD_LAT, 0L);
        Long lon = (Long) sourceAsMap.getOrDefault(StoreIndex.FIELD_LON, 0L);
        String type = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_TYPE, "FOOD");
        String desc = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_DESC, "NO_DESC");
        Integer bonusRate = (Integer) sourceAsMap.getOrDefault(StoreIndex.FIELD_BONUSRATE, 0);
        SearchStoreResponse storeResponse = SearchStoreResponse
                .newBuilder()
                .setUuid(hit.getId())
                .setName(name)
                .setLogo(logo)
                .setLocation(Location.newBuilder().setLat(lat).setLon(lon).build())
                .setType(type)
                .setDesc(desc)
                .setBonusRate(bonusRate)
                .setFrom(from)
                .build();
        return storeResponse;
    }

}