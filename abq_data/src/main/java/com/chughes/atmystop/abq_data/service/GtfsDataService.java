package com.chughes.atmystop.abq_data.service;


import com.chughes.atmystop.common.gtfsloading.GtfsProcessorProvider;
import com.chughes.atmystop.common.model.Agency;
import com.chughes.atmystop.common.model.BusStopData;
import com.chughes.atmystop.common.model.StopTimeData;
import com.chughes.atmystop.common.model.TripHeadSign;
import com.chughes.atmystop.common.model.repository.AgencyRepository;
import com.chughes.atmystop.common.service.BusStopsService;
import com.chughes.atmystop.common.service.SerializationService;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Calendar.*;
import static java.util.stream.Collectors.groupingBy;

@Service
public class GtfsDataService {

    private AgencyRepository agencyRepository;
    private BusStopsService busStopsService;
    private SerializationService<StopTimeData> stopTimeDataSerializationService;
    private RedisTemplate<String, Object> redisTemplate;
    private GtfsProcessorProvider gtfsProcessorProvider;

    public GtfsDataService(AgencyRepository agencyRepository, RedisTemplate<String, Object> redisTemplate, BusStopsService busStopsService, SerializationService<StopTimeData> stopTimeDataSerializationService, GtfsProcessorProvider gtfsProcessorProvider) {
        this.agencyRepository = agencyRepository;
        this.redisTemplate = redisTemplate;
        this.busStopsService = busStopsService;
        this.stopTimeDataSerializationService = stopTimeDataSerializationService;
        this.gtfsProcessorProvider = gtfsProcessorProvider;
    }

    private static final String AGENCY_UNIQUE = "abqride";

    public static final int HOURS_AFTER_MIDNIGHT = 3;
    private GtfsDaoImpl store;

    //private Map<AgencyAndId, List<StopTime>> timesByStop;

    private Map<AgencyAndId, StopTime> lastStopByTrip;

    private String agencyId = null;

    @PostConstruct
    public void init() {

        //TODO: Don't remove everything
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            //This deletes everything
            connection.flushAll();
            return null;
        });
        busStopsService.defineStopNameIndex();

        GtfsReader reader = new GtfsReader();
        try {
            reader.setInputLocation(new File("/Users/chughes/code/atmystop/abq_data/src/main/resources/google_transit.zip"));
            store = new GtfsDaoImpl();
            reader.setEntityStore(store);
            reader.run();

            String agencyGtfsId = store.getAllAgencies().stream().findFirst().get().getId();

            TimeZone timeZone = TimeZone.getTimeZone("America/Denver");

            agencyId = agencyGtfsId + "_" + AGENCY_UNIQUE;

            GtfsProcessorProvider.GtfsProcessor processor = gtfsProcessorProvider.getGtfsProcessor(store, timeZone, AGENCY_UNIQUE);

            processor.processGtfs();
        } catch (IOException e) {
            throw new RuntimeException("Can't read gtfs file",e);
        }

        populateServiceIds();

    }

    public GtfsDaoImpl getStore() {
        return store;
    }

    public void populateServiceIds(){
        Agency agency = new Agency();
        agency.setId(agencyId);
        agency.setTimeZone(TimeZone.getTimeZone("America/Denver"));
        agency.setServiceIds(getCurrentServiceIds());
        agencyRepository.save(agency);
    }

    private ArrayList<String> getCurrentServiceIds(){
        ArrayList<String> service = new ArrayList<>();
        ServiceDate serviceDate = getServiceDate();
        for (ServiceCalendar calendar:store.getAllCalendars()){
            if (calendar.getStartDate().compareTo(serviceDate) > 0){
                continue;
            }
            if (calendar.getEndDate().compareTo(serviceDate) < 0){
                continue;
            }
            switch (getDayOfWeek()){
                case SUNDAY:
                    if (calendar.getSunday() == 0) continue;
                    break;
                case MONDAY:
                    if (calendar.getMonday() == 0) continue;
                    break;
                case TUESDAY:
                    if (calendar.getTuesday() == 0) continue;
                    break;
                case WEDNESDAY:
                    if (calendar.getWednesday() == 0) continue;
                    break;
                case THURSDAY:
                    if (calendar.getThursday() == 0) continue;
                    break;
                case FRIDAY:
                    if (calendar.getFriday() == 0) continue;
                    break;
                case SATURDAY:
                    if (calendar.getSaturday() == 0) continue;
                    break;
            }
            service.add(calendar.getServiceId().getId());
        }
        for (ServiceCalendarDate serviceCalendarDate:store.getAllCalendarDates()){
            if (serviceCalendarDate.getDate().equals(serviceDate)){
                if (serviceCalendarDate.getExceptionType() == 1) {
                    service.add(serviceCalendarDate.getServiceId().getId());
                }else if (serviceCalendarDate.getExceptionType() == 2){
                    service.remove(serviceCalendarDate.getServiceId().getId());
                }
            }
        }
        return service;
    }

    public boolean isLastStop(StopTime stopTime){
        if (lastStopByTrip == null) {
            lastStopByTrip = new HashMap<>();
            for (StopTime stopT : store.getAllStopTimes()) {
                if (!getCurrentServiceIds().contains(stopT.getTrip().getServiceId().getId())){
                    //No service today
                    continue;
                }
                StopTime currentLastStop = lastStopByTrip.get(stopT.getTrip().getId());
                if (currentLastStop == null || currentLastStop.getStopSequence() < stopT.getStopSequence()) {
                    lastStopByTrip.put(stopT.getTrip().getId(), stopT);
                }
            }
        }
        return stopTime.getId().equals(lastStopByTrip.get(stopTime.getTrip().getId()).getId());

    }

/*    //TODO Make sure this works for all cases
    public boolean isServiceActive(LocalTime time){
        LocalTime latestUpdateTime = latestTime.plusMinutes(60);
        if (seconds > earliestTime && seconds < latestUpdateTime){
            return true;
        }
        if (latestUpdateTime > 24 * 60 * 60 && seconds < latestUpdateTime % 24 * 60 * 60){
            return true;
        }
        return false;
    }*/

    private int getDayOfWeek(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        //Before 3am, it's still yesterday
        if (calendar.get(Calendar.HOUR_OF_DAY) < HOURS_AFTER_MIDNIGHT){
            dayOfWeek--;
        }
        return dayOfWeek;
    }

    private ServiceDate getServiceDate(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR,-HOURS_AFTER_MIDNIGHT);
        return new ServiceDate(calendar);
    }

    //Invalidate serviceIds at 3:05am
    @Scheduled(cron="0 "+HOURS_AFTER_MIDNIGHT+" 5 * * ?")
    private void invalidateServiceIds(){
        agencyRepository.deleteById(agencyId);
        populateServiceIds();
    }

    public String getAgencyId() {
        return agencyId;
    }
}

