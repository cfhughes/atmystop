package com.chughes.atmystop.gtfsrt_data;

import com.google.transit.realtime.GtfsRealtime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

@Component
public class ScheduledTask {

    @Scheduled(fixedRate = 30 * 1000)
    public void updateRealtime() {
        try {
            URL url = new URL("https://api.actransit.org/transit/gtfsrt/tripupdates?token=67BAECDF34E1BDCA36BC44499FDDF6F9");
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (entity.hasTripUpdate()) {
                    System.out.println(entity.getTripUpdate());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
