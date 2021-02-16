package com.chughes.atmystop.abq_data;

import com.chughes.atmystop.abq_data.model.Vehicle;
import com.chughes.atmystop.abq_data.service.GtfsDataService;
import com.chughes.atmystop.abq_data.service.TempDataService;
import com.chughes.atmystop.common.model.AgencyTripId;
import com.chughes.atmystop.common.service.BusUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.chughes.atmystop.common.model.BusUpdateData;

@Component
public class ScheduledQuery {

    private static final Logger log = LoggerFactory.getLogger(ScheduledQuery.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private HTTPGetter httpGetter;
    private TempDataService tempDataService;
    private GtfsDataService gtfsDataService;
    private BusUpdateService busUpdateService;

    public ScheduledQuery(HTTPGetter httpGetter,
                          TempDataService tempDataService,
                          GtfsDataService gtfsDataService, BusUpdateService busUpdateService){
        this.httpGetter = httpGetter;
        this.tempDataService = tempDataService;
        this.gtfsDataService = gtfsDataService;
        this.busUpdateService = busUpdateService;
    }

    @Scheduled(fixedRate = 15 * 1000)
    public void reportCurrentTime() {
        int seconds = LocalTime.now(ZoneId.of("America/Denver")).toSecondOfDay();
/*        if (!gtfsDataService.isServiceActive(seconds)){
            return;
        }*/
        List<Vehicle> vehicles = httpGetter.get();
        log.info("The time is now {} and There are {} buses", dateFormat.format(new Date()), vehicles.size());
        for (Vehicle vehicle:vehicles){
            //Trip t = gtfsDataService.getStore().getTripForId(new AgencyAndId("1",vehicle.getTripId().trim()));
            //Stop s = gtfsDataService.getStore().getStopForId(new AgencyAndId("1",vehicle.getNextStopId()));

            Duration d = Duration.between(vehicle.getNextStopSchedTime(),vehicle.getMsgTime());
            if (vehicle.getTripId().equals("471203")) {
                log.info("Between {} and {} is {}", vehicle.getNextStopSchedTime(), vehicle.getMsgTime(), d.getSeconds());
            }
            //TODO Is this really necessary? Fixes past midnight trips
            if (d.compareTo(Duration.ofHours(20)) > 0){
                d = d.minus(Duration.ofHours(24));
            }
            if (d.compareTo(Duration.ofHours(-20)) < 0){
                d = d.plus(Duration.ofHours(24));
            }
            //log.info("Trip "+vehicle.getTripId() + " Duration "+d);

            AgencyTripId agencyTripId = new AgencyTripId(gtfsDataService.getAgencyId(),vehicle.getTripId());
            BusUpdateData updateDataCurrent = new BusUpdateData(agencyTripId.hashCode(), vehicle.getMsgTime(), d.getSeconds(), vehicle.getNextStopId());
            BusUpdateData updateDataPrevious = tempDataService.getSecondsLateTemp(vehicle.getTripId());

            if (updateDataPrevious != null) {
                //If next stop id is different, accept time update
                if (!updateDataPrevious.getNextStop().equals(vehicle.getNextStopId())) {
                    busUpdateService.saveBusUpdate(updateDataCurrent);
                    if (vehicle.getTripId().equals("471203")) {
                        log.info("Updated");
                    }
                    if (d.getSeconds() > 5 * 60){
                        log.info("Trip id {} is more than 5 minutes late on route {}",vehicle.getTripId(), vehicle.getRouteShortName());
                    }
                }

                //If already later to next stop, use current lateness
                if (d.getSeconds() > updateDataPrevious.getSecondsLate()) {
                    busUpdateService.saveBusUpdate(updateDataCurrent);
                }
            }

            //Put current update for next time
            tempDataService.putSecondsLateTemp(vehicle.getTripId(), updateDataCurrent);

            //System.out.println(vehicle.getTripId() + " " + d);
        }
    }
}

