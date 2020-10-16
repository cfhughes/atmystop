package com.chughes.atmystop.abq_data.service;


import com.chughes.atmystop.common.model.Agency;
import com.chughes.atmystop.common.model.StopTimeData;
import com.chughes.atmystop.common.model.repository.AgencyRepository;
import com.chughes.atmystop.common.model.repository.StopTimeDataRepository;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Calendar.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;

@Service
public class GtfsDataService {

    AgencyRepository agencyRepository;
    StopTimeDataRepository stopTimeDataRepository;
    RedisTemplate<String, Object> redisTemplate;

    public GtfsDataService(AgencyRepository agencyRepository, StopTimeDataRepository stopTimeDataRepository, RedisTemplate<String, Object> redisTemplate) {
        this.agencyRepository = agencyRepository;
        this.stopTimeDataRepository = stopTimeDataRepository;
        this.redisTemplate = redisTemplate;
    }

    private static final String AGENCY_UNIQUE = "abqride";

    public static final int HOURS_AFTER_MIDNIGHT = 3;
    private GtfsDaoImpl store;

    //private Map<AgencyAndId, List<StopTime>> timesByStop;

    private Map<AgencyAndId, StopTime> lastStopByTrip;

    private LocalTime latestTime;
    private LocalTime earliestTime;

    private String agencyId = null;

    @PostConstruct
    public void init() {
        GtfsReader reader = new GtfsReader();
        try {
            reader.setInputLocation(new File("abq_data/src/main/resources/google_transit.zip"));
            store = new GtfsDaoImpl();
            reader.setEntityStore(store);
            reader.run();

            List<StopTimeData> allTimes = store.getAllStopTimes().stream().map((stopTime) -> {
                StopTimeData stopTimeData = new StopTimeData();
                stopTimeData.setTripId(stopTime.getTrip().getId().getId());
                stopTimeData.setRouteId(stopTime.getTrip().getRoute().getId().getId());
                stopTimeData.setStopId(stopTime.getStop().getId().getId());
                stopTimeData.setArrivalTime(LocalTime.ofSecondOfDay(stopTime.getArrivalTime() % 86400));
                stopTimeData.setServiceId(stopTime.getTrip().getServiceId().getId());
                return stopTimeData;
            }).collect(Collectors.toList());

            allTimes.stream().collect(groupingBy(StopTimeData::getTripId,
                    maxBy(Comparator.comparing(StopTimeData::getArrivalTime))))
                    .forEach(((s, stopTimeData) -> {
                        stopTimeData.ifPresent(timeData -> timeData.setLastStop(true));
                    }));

            earliestTime = allTimes.stream().min(Comparator.comparing(StopTimeData::getArrivalTime)).get().getArrivalTime();

            latestTime = allTimes.stream().max(Comparator.comparing(StopTimeData::getArrivalTime)).get().getArrivalTime();

            agencyId = store.getAllAgencies().stream().findFirst().get().getId() + "_" + AGENCY_UNIQUE;

            redisTemplate.executePipelined(
                    (RedisCallback<Object>) connection -> {
                        allTimes.forEach((stopTimeData -> {
                            connection.set((agencyId+":"+stopTimeData.getTripId()+":"+stopTimeData.getStopId()).getBytes(), SerializationUtils.serialize(stopTimeData));
                            connection.sAdd(stopTimeData.getServiceId().getBytes(),stopTimeData.getTripId().getBytes());
                            connection.sAdd(stopTimeData.getStopId().getBytes(),stopTimeData.getTripId().getBytes());
                        }));
                        return null;
                    }
            );

            //stopTimeDataRepository.saveAll(allTimes);
            System.out.println("Latest: "+latestTime+" Earliest: "+earliestTime);
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

    public LocalTime getLatestTime() {
        return latestTime;
    }

    public LocalTime getEarliestTime() {
        return earliestTime;
    }

    public String getAgencyId() {
        return agencyId;
    }
}

