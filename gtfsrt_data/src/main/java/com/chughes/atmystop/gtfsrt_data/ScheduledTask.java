package com.chughes.atmystop.gtfsrt_data;

import com.chughes.atmystop.common.model.AgencyTripId;
import com.chughes.atmystop.common.model.BusUpdateData;
import com.chughes.atmystop.common.model.StopTimeData;
import com.chughes.atmystop.common.service.BusUpdateService;
import com.chughes.atmystop.common.service.StopTimeService;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

@Component
public class ScheduledTask {

    private StopTimeService stopTimeService;
    private BusUpdateService busUpdateService;

    public ScheduledTask(StopTimeService stopTimeService, BusUpdateService busUpdateService) {
        this.stopTimeService = stopTimeService;
        this.busUpdateService = busUpdateService;
    }

    private static final String AGENCY_UNIQUE = "actransit";

    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void updateRealtime() {
        String agencyId = "1" + "_" + AGENCY_UNIQUE;
        try {
            URL url = new URL("http://api.actransit.org/transit/gtfsrt/tripupdates?token=67BAECDF34E1BDCA36BC44499FDDF6F9");
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    //System.out.println("*****NEW*****");
                    GtfsRealtime.TripUpdate tripUpdate = entity.getTripUpdate();
                    String tripId = tripUpdate.getTrip().getTripId();
                    //System.out.println(tripId);
                    //System.out.println(tripUpdate);

                    if (tripUpdate.getStopTimeUpdateCount() > 0) {
                        GtfsRealtime.TripUpdate.StopTimeUpdate update = tripUpdate.getStopTimeUpdate(0);

                        String stopId = update.getStopId();

                        StopTimeData stopTimeData = stopTimeService.getByStopIdAgencyTripId(stopId,agencyId,tripId);
                        LocalTime updateTime = LocalTime.ofSecondOfDay(tripUpdate.getTimestamp() % (86400));

                        long delay = (update.getArrival().getTime() % (86400)) - stopTimeData.getArrivalTime().toSecondOfDay();

                        BusUpdateData busUpdateData = new BusUpdateData(new AgencyTripId(agencyId, tripId).hashCode(), updateTime, delay, null);

                        //System.out.println(delay);

                        busUpdateService.saveBusUpdate(busUpdateData);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
