package com.chughes.atmystop.appserver;

import com.chughes.atmystop.appserver.model.Bus;
import com.chughes.atmystop.appserver.model.RealtimeTripInfo;
import com.chughes.atmystop.common.model.Agency;
import com.chughes.atmystop.common.model.AgencyTripId;
import com.chughes.atmystop.common.model.BusStopData;
import com.chughes.atmystop.common.model.BusUpdateData;
import com.chughes.atmystop.common.model.repository.AgencyRepository;
import com.chughes.atmystop.common.model.repository.StopTimeDataRepository;
import com.chughes.atmystop.common.service.BusStopsService;
import com.chughes.atmystop.common.service.BusUpdateService;
import com.chughes.atmystop.common.service.StopTimeService;
import com.chughes.atmystop.common.service.TripsService;
import org.springframework.data.geo.Point;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class StopController {

    private BusUpdateService busUpdateService;
    private AgencyRepository agencyRepository;
    private StopTimeDataRepository stopTimeDataRepository;
    private BusStopsService busStopsService;
    private StopTimeService stopTimeService;
    private TripsService tripsService;

    public StopController(AgencyRepository agencyRepository, StopTimeDataRepository stopTimeDataRepository, BusUpdateService busUpdateService, BusStopsService busStopsService, StopTimeService stopTimeService, TripsService tripsService) {
        this.busUpdateService = busUpdateService;
        this.agencyRepository = agencyRepository;
        this.stopTimeDataRepository = stopTimeDataRepository;
        this.busStopsService = busStopsService;
        this.stopTimeService = stopTimeService;
        this.tripsService = tripsService;
    }

//    @GetMapping("/trips")
//    public List<VehicleOnTrip> getTrips(){
//        List<VehicleOnTrip> trips = new ArrayList<>();
//        VehicleOnTrip trip = new VehicleOnTrip();
//        trip.setVehicleId(1234);
//        trips.add(trip);
//        return trips;
//    }

    @GetMapping("/stops")
    public List<BusStopData> getStops(@RequestParam double lat,@RequestParam double lon){
        return busStopsService.nearestStops(new Point(lon, lat));
    }

    @GetMapping("/route/{routeId}/stops")
    public List<BusStopData> getStopsByRoute(@PathVariable String routeId) {
        return busStopsService.stopsByRoute(routeId);
    }

    @GetMapping("/stops/search")
    public List<BusStopData> searchStops(@RequestParam(required = false) String name, @RequestParam(required = false) String route, @RequestParam(required = false) String id) {
        return busStopsService.stopsBySearch(name, route, id);
    }

    @GetMapping("/agency/{agencyId}/stops/{stopId}")
    public List<RealtimeTripInfo> getTrips(@PathVariable String agencyId, @PathVariable String stopId){
        Optional<Agency> currentServiceIds = agencyRepository.findById(agencyId);
        if (!currentServiceIds.isPresent()){
            throw new IllegalArgumentException("No Such Agency");
        }

        List<String> serviceIds = currentServiceIds.get().getServiceIds();
//        if (currentServiceIds.get().getYesterdayServiceIds() != null) {
//            serviceIds.addAll(currentServiceIds.get().getYesterdayServiceIds());
//        }
        return stopTimeService.getAllByStopIdAndAgency(stopId,agencyId, serviceIds).stream()
                .flatMap((stopTimeData -> {
                    if (stopTimeData.isLastStop()) {
                        return null;
                    }
                    //System.out.println(stopTimeData.toString());
                    RealtimeTripInfo realtimeTripInfo = new RealtimeTripInfo();
                    BusUpdateData busUpdateData = busUpdateService.findById(new AgencyTripId(agencyId,stopTimeData.getTripId()).hashCode());
                    //Only include recent enough updates
                    if (busUpdateData!= null && Duration.between(busUpdateData.getUpdateTime(),LocalTime.now(currentServiceIds.get().getTimeZone().toZoneId())).getSeconds() < 1800) {
                        realtimeTripInfo.setSecondsLate(busUpdateData.getSecondsLate());
                    }
                    Duration arrivesIn = Duration.between(LocalTime.now(TimeZone.getTimeZone("UTC").toZoneId()), stopTimeData.getArrivalTime());
                    arrivesIn = arrivesIn.plusSeconds(realtimeTripInfo.getSecondsLate());
                    arrivesIn = Duration.ofSeconds(Math.floorMod(arrivesIn.getSeconds(), 60 * 60 * 24));
                    //System.out.println(String.format("%s : %s : %d",stopTimeData.getTripId(),stopTimeData.getArrivalTime(),arrivesIn.getSeconds()));
                    if (arrivesIn.getSeconds() < 3600 && arrivesIn.getSeconds() > -2 * 60){
                        realtimeTripInfo.setScheduledTime(stopTimeData.getArrivalTime());
                        realtimeTripInfo.setTripId(stopTimeData.getTripId());
                        realtimeTripInfo.setService(stopTimeData.getServiceId());
                        realtimeTripInfo.setRoute(stopTimeData.getRouteShortName());
                        realtimeTripInfo.setDisplayTime(stopTimeData.getArrivalTime().toString());
                        realtimeTripInfo.setColor(stopTimeData.getColor());
                        realtimeTripInfo.setTextColor(stopTimeData.getTextColor());
                        realtimeTripInfo.setHeadsign(tripsService.getHeadsignByTrip(agencyId, stopTimeData.getTripId()));
                        return Stream.of(realtimeTripInfo);
                    }
                    return Stream.empty();
                })).sorted().collect(Collectors.toList());

    }

    @GetMapping("/agency/{agencyId}/stops/{stopId}/proto")
    public Bus.Stop getTripsProto(@PathVariable String agencyId, @PathVariable String stopId){
        List<RealtimeTripInfo> trips = getTrips(agencyId, stopId);

        Bus.Stop.Builder stopBuilder = Bus.Stop.newBuilder();

        for (RealtimeTripInfo trip: trips){
            Bus.BusTime bus = Bus.BusTime.newBuilder()
                    .setSecondsLate((int) trip.getSecondsLate())
                    .setTripId(trip.getTripId())
                    .setScheduledTime(trip.getDisplayTime())
                    .build();
            stopBuilder.addBusses(bus);
        }

        return stopBuilder.build();
    }

}
