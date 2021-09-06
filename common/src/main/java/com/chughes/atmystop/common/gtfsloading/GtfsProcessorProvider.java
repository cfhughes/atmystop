package com.chughes.atmystop.common.gtfsloading;

import com.chughes.atmystop.common.model.BusStopData;
import com.chughes.atmystop.common.model.StopTimeData;
import com.chughes.atmystop.common.model.TripHeadSign;
import com.chughes.atmystop.common.service.BusStopsService;
import com.chughes.atmystop.common.service.SerializationService;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;

@Service
public class GtfsProcessorProvider {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SerializationService<StopTimeData> stopTimeDataSerializationService;
    private final BusStopsService busStopsService;

    public GtfsProcessorProvider(RedisTemplate<String, Object> redisTemplate, SerializationService<StopTimeData> stopTimeDataSerializationService, BusStopsService busStopsService) {
        this.redisTemplate = redisTemplate;
        this.stopTimeDataSerializationService = stopTimeDataSerializationService;
        this.busStopsService = busStopsService;
    }

    public GtfsProcessor getGtfsProcessor(GtfsDaoImpl store, TimeZone timeZone, String agencyUniqueString) {
        return new GtfsProcessor(store, timeZone, agencyUniqueString);
    }

    public class GtfsProcessor {
        private final GtfsDaoImpl store;
        private TimeZone timeZone;
        private String agencyUniqueString;
        private HashMap<String, String> lastStopByTrip;
        private String agencyId;
        private String agencyGtfsId;

        public GtfsProcessor (GtfsDaoImpl store, TimeZone timeZone, String agencyUniqueString) {
            this.store = store;
            this.timeZone = timeZone;
            this.agencyUniqueString = agencyUniqueString;
        }

        HashMap<String, Set<String>> tripsByStop;

        public void processGtfs() {
            tripsByStop = getTripsByStop();
            insertAllStopTimes();
            insertAllStops();
        }

        private HashMap<String, Set<String>> getTripsByStop() {
            HashMap<String, Set<String>> tripsByStop = new HashMap<>();
            store.getAllStopTimes().stream().forEach((stopTime) -> {
                if (!tripsByStop.containsKey(stopTime.getStop().getId().getId())){
                    tripsByStop.put(stopTime.getStop().getId().getId(),new HashSet<>());
                }
                tripsByStop.get(stopTime.getStop().getId().getId()).add(stopTime.getTrip().getId().getId());
            });
            return tripsByStop;
        }

        private void insertAllStopTimes() {
            agencyGtfsId = store.getAllAgencies().stream().findFirst().get().getId();

            List<StopTimeData> allTimes = store.getAllStopTimes().stream().map((stopTime) -> {
                StopTimeData stopTimeData = new StopTimeData();
                stopTimeData.setTripId(stopTime.getTrip().getId().getId());
                stopTimeData.setRouteShortName(stopTime.getTrip().getRoute().getShortName());
                stopTimeData.setColor(stopTime.getTrip().getRoute().getColor());
                stopTimeData.setTextColor(stopTime.getTrip().getRoute().getTextColor());
                stopTimeData.setStopId(stopTime.getStop().getId().getId());
                stopTimeData.setArrivalTime(LocalTime.ofSecondOfDay(Math.floorMod(stopTime.getArrivalTime() - timeZone.getOffset(new Date().getTime()) / 1000, 86400)));
                stopTimeData.setStopSequence(stopTime.getStopSequence());
                stopTimeData.setServiceId(stopTime.getTrip().getServiceId().getId());

                return stopTimeData;
            }).collect(Collectors.toList());

            lastStopByTrip = new HashMap<>();

            agencyId = agencyGtfsId + "_" + agencyUniqueString;

            allTimes.stream().collect(groupingBy(StopTimeData::getTripId,
                    maxBy(Comparator.comparing(StopTimeData::getStopSequence))))
                    .forEach((s, stopTimeData) -> {
                        stopTimeData.ifPresent(timeData -> {
                            timeData.setLastStop(true);
                            lastStopByTrip.put(timeData.getTripId(),store.getStopForId(new AgencyAndId(agencyGtfsId,timeData.getStopId())).getName());
                        });
                    });

            redisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        // Remove previous stuff
                        Set<byte[]> members = redisTemplate.execute((RedisCallback<Set<byte[]>>) connectionS -> connectionS.sMembers((agencyId + ":timesSet").getBytes()));
                        if (members.size() > 0) {
                            connection.unlink(members.toArray(new byte[0][]));
                            connection.unlink((agencyId + ":timesSet").getBytes());
                        }
                        // Recreate data
                        allTimes.forEach((stopTimeData -> {
                            // Add stop time data
                            connection.set((agencyId+":"+stopTimeData.getTripId()+":"+stopTimeData.getStopId()).getBytes(), stopTimeDataSerializationService.serialize(stopTimeData, StopTimeData.class));
                            connection.sAdd((agencyId+":timesSet").getBytes(), (agencyId+":"+stopTimeData.getTripId()+":"+stopTimeData.getStopId()).getBytes());
                            // Add map from service -> trip
                            connection.sAdd((agencyId+":"+stopTimeData.getServiceId()).getBytes(),(agencyId+":"+stopTimeData.getTripId()).getBytes());
                            connection.sAdd((agencyId+":timesSet").getBytes(), (agencyId+":"+stopTimeData.getServiceId()).getBytes());
                            // Add map from stop id -> trip
                            connection.sAdd((agencyId+":"+stopTimeData.getStopId()).getBytes(),(agencyId+":"+stopTimeData.getTripId()).getBytes());
                            connection.sAdd((agencyId+":timesSet").getBytes(), (agencyId+":"+stopTimeData.getStopId()).getBytes());
                        }));
                        lastStopByTrip.forEach((key, value) -> {
                            connection.set(("tripInfo:"+agencyId+":"+key).getBytes(),value.getBytes());
                            connection.sAdd((agencyId+":timesSet").getBytes(),("tripInfo:"+agencyId+":"+key).getBytes());
                        });
                        return null;
                    }
            );

        }

        public void insertAllStops() {
            List<BusStopData> allStops = store.getAllStops().stream().map((stop -> {
                BusStopData busStopData = new BusStopData();
                busStopData.setAgency(agencyId);
                busStopData.setId(stop.getId().getId());
                busStopData.setCode(stop.getCode());
                busStopData.setTitle(stop.getName());
                busStopData.setLocation(new Point(stop.getLon(),stop.getLat()));
                HashSet<TripHeadSign> headSigns = new HashSet<>();
                for (String tripId:tripsByStop.get(busStopData.getId())){
                    Trip trip = store.getTripForId(new AgencyAndId(agencyGtfsId, tripId));
                    Route route = trip.getRoute();
                    String name = trip.getTripHeadsign();
                    if (name == null || name.trim().length() == 0) {
                        name = lastStopByTrip.get(tripId);
                    }
                    headSigns.add(new TripHeadSign(name, route.getShortName(), route.getColor(), route.getTextColor()));
                }
                busStopData.setTrips(headSigns);
                return busStopData;
            })).collect(Collectors.toList());

            busStopsService.addAllStops(allStops);
        }
    }

}
