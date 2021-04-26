package com.chughes.atmystop.common.service;

import com.chughes.atmystop.common.model.BusStopData;
import io.redisearch.Query;
import io.redisearch.Schema;
import io.redisearch.SearchResult;
import io.redisearch.client.Client;
import io.redisearch.client.IndexDefinition;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusStopsService {

    private static final byte[] REDIS_KEY_STOPS = "STOPS".getBytes();

    private RedisTemplate<String, Object> redisTemplate;
    private Client redisearchClient;

    public BusStopsService(RedisTemplate<String, Object> redisTemplate, Client redisearchClient) {
        this.redisTemplate = redisTemplate;
        this.redisearchClient = redisearchClient;
    }

    public void addAllStops(List<BusStopData> stops){
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.del(REDIS_KEY_STOPS);
            stops.forEach(busStopData -> {
                byte[] key = String.format("stopObj:%s:%s",busStopData.getAgency(),busStopData.getId()).getBytes();
                connection.set(key, SerializationUtils.serialize(busStopData));
                connection.geoCommands().geoAdd(REDIS_KEY_STOPS, busStopData.getLocation(), key);
                busStopData.getTrips().forEach(tripHeadSign -> connection.sAdd(("route:"+tripHeadSign.getRoute()).getBytes(),key));
                connection.sAdd(("stopId:"+busStopData.getId()).getBytes(),key);

                if (!busStopData.getCode().equals(busStopData.getId())) {
                    connection.sAdd(("stopId:"+busStopData.getCode()).getBytes(),key);
                }

                Map<byte[], byte[]> fields = new HashMap<>();
                fields.put("name".getBytes(), busStopData.getTitle().getBytes());
                fields.put("key".getBytes(), key);

                connection.hashCommands().hMSet(String.format("stop:%s:%s",busStopData.getAgency(),busStopData.getId()).getBytes(),fields);
            });
            return null;
        });
    }

    public List<BusStopData> nearestStops(Point point){
        return redisTemplate.execute((RedisCallback<List<BusStopData>>) connection -> {
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .sortAscending().limit(2);
            return connection.geoRadius(REDIS_KEY_STOPS,new Circle(point, new Distance(1000, Metrics.MILES)),args)
                    .getContent().stream().map((geoLocationGeoResult -> {
                        return (BusStopData) SerializationUtils.deserialize(connection.get(geoLocationGeoResult.getContent().getName()));
                    })).collect(Collectors.toList());
        });
    }

    public List<BusStopData> stopsByRoute(String route) {
        return redisTemplate.execute((RedisCallback<List<BusStopData>>) connection -> {
            return connection.setCommands().sMembers(route.getBytes()).stream()
                    .map(bytes -> (BusStopData) SerializationUtils.deserialize(connection.get(bytes))).collect(Collectors.toList());
        });
    }

    public List<BusStopData> stopsBySearch(String name, String route, String id) {
        if (route == null)route = "";
        if (id == null)id = "";
        if (name == null)name = "";

        route = route.trim();
        id = id.trim();

        List<byte[]> keys = new ArrayList<>();

        if (route.length() > 0) {
            keys.add(("route:"+route).getBytes());
        }
        if (id.length() > 0) {
            keys.add(("stopId:"+id).getBytes());
        }
        Set<byte[]> stopsFound = null;
        if (id.length() > 0 || route.length() > 0) {
            stopsFound = redisTemplate.execute((RedisCallback<Set<byte[]>>) connection -> {
                return connection.setCommands().sInter(keys.toArray(new byte[0][]));
            });
        }

        Set<byte[]> nameFound = null;
        name = name.trim();
        if (name.length() > 0) {

            Query q = new Query(name)
                    .limit(0, 100);

            SearchResult res = redisearchClient.search(q);

            nameFound = redisTemplate.execute((RedisCallback<Set<byte[]>>) connection -> {
                return res.docs.stream()
                        .map(result -> ((String) result.get("key")).getBytes()).collect(Collectors.toSet());
            });
        }

        Set<byte[]> allTogether = new HashSet<>();
        if (name.length() > 0) {
            allTogether.addAll(nameFound);
            if (id.length() > 0 || route.length() > 0){
                Set<byte[]> finalStopsFound = stopsFound;
                allTogether = allTogether.stream().filter(aItem -> finalStopsFound.stream().anyMatch(bItem -> Arrays.equals(aItem, bItem))).collect(Collectors.toSet());
            }
        }else if(id.length() > 0 || route.length() > 0) {
            allTogether.addAll(stopsFound);
        }

        Set<byte[]> finalAllTogether = allTogether;
        return redisTemplate.execute((RedisCallback<List<BusStopData>>) connection -> {
            return finalAllTogether.stream()
                    .map(bytes -> (BusStopData) SerializationUtils.deserialize(connection.get(bytes))).collect(Collectors.toList());
        });
    }

    public void defineStopNameIndex() {
        Schema sc = new Schema()
                .addTextField("name", 5.0);

        IndexDefinition def = new IndexDefinition()
                .setPrefixes(new String[] {"stop:"});

        redisearchClient.createIndex(sc, Client.IndexOptions.defaultOptions().setDefinition(def));
    }

}
