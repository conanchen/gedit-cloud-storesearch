package com.github.conanchen.gedit.storesearch.grpc;

import com.github.conanchen.gedit.common.grpc.Location;
import com.github.conanchen.gedit.common.grpc.Status;
import com.github.conanchen.gedit.store.search.grpc.*;
import com.github.conanchen.gedit.storesearch.StoreIndex;
import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@GRpcService(interceptors = {LogInterceptor.class})
public class SearchService extends StoreSearchApiGrpc.StoreSearchApiImplBase {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final Gson gson = new Gson();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void index(IndexStoreRequest request, StreamObserver<IndexStoreResponse> responseObserver) {
        try {
            XContentBuilder builder = XContentFactory
                    .jsonBuilder()
                    .startObject()
                    .field(StoreIndex.FIELD_uuid, request.getUuid())
                    .field(StoreIndex.FIELD_name, request.getName())
                    .field(StoreIndex.FIELD_geoPoint, new GeoPoint(request.getLocation().getLat(), request.getLocation().getLon()))
                    .field(StoreIndex.FIELD_type, request.getType())
                    .field(StoreIndex.FIELD_desc, request.getDesc())
                    .field(StoreIndex.FIELD_pointsRate, request.getPointsRate())

                    .field(StoreIndex.FIELD_amapAdCode, request.getAmapAdCode())
                    .field(StoreIndex.FIELD_amapAoiName, request.getAmapAoiName())
                    .field(StoreIndex.FIELD_amapBuildingId, request.getAmapBuildingId())
                    .field(StoreIndex.FIELD_amapStreet, request.getAmapStreet())
                    .field(StoreIndex.FIELD_amapStreetNum, request.getAmapStreetNum())
                    .field(StoreIndex.FIELD_amapDistrict, request.getAmapDistrict())
                    .field(StoreIndex.FIELD_amapCityCode, request.getAmapCityCode())
                    .field(StoreIndex.FIELD_amapCity, request.getAmapCity())
                    .field(StoreIndex.FIELD_amapProvince, request.getAmapProvince())
                    .field(StoreIndex.FIELD_amapCountry, request.getAmapCountry())
                    .endObject();

            org.elasticsearch.action.index.IndexRequest indexRequest = new org.elasticsearch.action.index.
                    IndexRequest(StoreIndex.INDEX, StoreIndex.TYPE, request.getUuid())
                    .source(builder);
            restHighLevelClient.indexAsync(indexRequest, new ActionListener<org.elasticsearch.action.index.IndexResponse>() {
                @Override
                public void onResponse(org.elasticsearch.action.index.IndexResponse indexResponse) {
                    IndexStoreResponse result = IndexStoreResponse.newBuilder()
                            .setUuid(indexResponse.getId())
                            .setStatus(Status.newBuilder().setCode(Status.Code.OK).setDetails("index ok...").build())
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
                ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
                if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

                }
                if (shardInfo.getFailed() > 0) {
                    for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                        String reason = failure.reason();
                    }
                }

                DeleteStoreResponse result = DeleteStoreResponse
                        .newBuilder()
                        .setUuid(id)
                        .setStatus(Status.newBuilder().setCode(Status.Code.OK).setDetails(String.format("%s", deleteResponse.status().name())).build())
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
    public void search(SearchStoreRequest request, StreamObserver<com.github.conanchen.gedit.store.search.grpc.SearchStoreResponse> responseObserver) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.query(QueryBuilders.geoDistanceQuery(StoreIndex.FIELD_geoPoint)
                .point(request.getLocation().getLat(), request.getLocation().getLon())
                .distance(100, DistanceUnit.KILOMETERS))
                .from(0)
                .size(20)
                .timeout(new TimeValue(60, TimeUnit.SECONDS));

        org.elasticsearch.action.search.SearchRequest searchRequest = new org.elasticsearch.action.search
                .SearchRequest(StoreIndex.INDEX)
                .types(StoreIndex.TYPE)
                .source(sourceBuilder);

        restHighLevelClient.searchAsync(searchRequest, new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                SearchHit[] searchHits = searchResponse.getHits().getHits();
                for (int i = 0; i < searchHits.length; i++) {
                    SearchHit hit = searchHits[i];
                    String sourceAsString = hit.getSourceAsString();
                    System.out.println(String.format("hit: %s", sourceAsString));
                    com.github.conanchen.gedit.store.search.grpc.SearchStoreResponse storeResponse = buildStoreSearchResponse(request.getFrom() + i, hit);
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


    private com.github.conanchen.gedit.store.search.grpc.SearchStoreResponse buildStoreSearchResponse(int from, SearchHit hit) {
        Map<String, Object> sourceAsMap = hit.getSourceAsMap();
        String uuid = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_uuid, "NO_UUID");
        String name = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_name, "NO_NAMAE");
        String logo = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_logo, "NO_LOGO");
        GeoPoint geoPoint = (GeoPoint) sourceAsMap.getOrDefault(StoreIndex.FIELD_geoPoint, new GeoPoint(0, 0));
        String type = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_type, "NO_TYPE");
        String desc = (String) sourceAsMap.getOrDefault(StoreIndex.FIELD_desc, "NO_DESC");
        Double pointsRate = (Double) sourceAsMap.getOrDefault(StoreIndex.FIELD_pointsRate, 0);
        com.github.conanchen.gedit.store.search.grpc.SearchStoreResponse storeResponse = com.github.conanchen.gedit.store.search.grpc.SearchStoreResponse
                .newBuilder()
                .setUuid(uuid)
                .setName(name)
                .setLogo(logo)
                .setLocation(Location.newBuilder().setLat(geoPoint.getLat()).setLon(geoPoint.getLon()).build())
                .setType(type)
                .setDesc(desc)
                .setPointsRate(pointsRate)

                .setFrom(from)
                .build();
        return storeResponse;
    }

}