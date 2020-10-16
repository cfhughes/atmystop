package com.chughes.atmystop.appserver;

import com.chughes.atmystop.appserver.model.Bus;
import com.chughes.atmystop.appserver.model.RealtimeTripInfo;
import com.chughes.atmystop.common.model.Agency;
import com.chughes.atmystop.common.model.AgencyTripId;
import com.chughes.atmystop.common.model.BusUpdateData;
import com.chughes.atmystop.common.model.repository.AgencyRepository;
import com.chughes.atmystop.common.model.repository.BusUpdateDataRepository;
import com.chughes.atmystop.common.model.repository.StopTimeDataRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class StopController {

    private BusUpdateDataRepository busUpdateDataRepository;
    private AgencyRepository agencyRepository;
    private StopTimeDataRepository stopTimeDataRepository;
    private StopTimeService stopTimeService;

    public StopController(BusUpdateDataRepository busUpdateDataRepository, AgencyRepository agencyRepository, StopTimeDataRepository stopTimeDataRepository, StopTimeService stopTimeService) {
        this.busUpdateDataRepository = busUpdateDataRepository;
        this.agencyRepository = agencyRepository;
        this.stopTimeDataRepository = stopTimeDataRepository;
        this.stopTimeService = stopTimeService;
    }

//    @GetMapping("/trips")
//    public List<VehicleOnTrip> getTrips(){
//        List<VehicleOnTrip> trips = new ArrayList<>();
//        VehicleOnTrip trip = new VehicleOnTrip();
//        trip.setVehicleId(1234);
//        trips.add(trip);
//        return trips;
//    }

    @GetMapping("/agency/{agencyId}/stops/{stopId}")
    public List<RealtimeTripInfo> getTrips(@PathVariable String agencyId, @PathVariable String stopId){
        Optional<Agency> currentServiceIds = agencyRepository.findById(agencyId);
        if (currentServiceIds.isEmpty()){
            throw new IllegalArgumentException("No Such Agency");
        }

        return stopTimeService.getAllByStopIdAndAgency(stopId,agencyId,currentServiceIds.get().getServiceIds()).stream()
                .flatMap((stopTimeData -> {
                    //System.out.println(stopTimeData.toString());
                    RealtimeTripInfo realtimeTripInfo = new RealtimeTripInfo();
                    Optional<BusUpdateData> busUpdateData = busUpdateDataRepository.findById(new AgencyTripId(agencyId,stopTimeData.getTripId()).hashCode());
                    if (busUpdateData.isPresent() && Duration.between(busUpdateData.get().getUpdateTime(),LocalTime.now(currentServiceIds.get().getTimeZone().toZoneId())).getSeconds() < 1800) {
                        realtimeTripInfo.setSecondsLate(busUpdateData.get().getSecondsLate());
                    }
                    realtimeTripInfo.setScheduledTime(stopTimeData.getArrivalTime());
                    realtimeTripInfo.setTripId(stopTimeData.getTripId());
                    realtimeTripInfo.setService(stopTimeData.getServiceId());
                    realtimeTripInfo.setDisplayTime(stopTimeData.getArrivalTime().toString());
                    Duration duration = Duration.between(LocalTime.now(currentServiceIds.get().getTimeZone().toZoneId()), stopTimeData.getArrivalTime());
                    if (duration.getSeconds() < 3600 && duration.getSeconds() > -1800){
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
