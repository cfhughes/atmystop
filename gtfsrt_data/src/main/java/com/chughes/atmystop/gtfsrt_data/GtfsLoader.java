package com.chughes.atmystop.gtfsrt_data;

import com.chughes.atmystop.common.gtfsloading.GtfsProcessorProvider;
import com.chughes.atmystop.common.model.Agency;
import com.chughes.atmystop.common.model.repository.AgencyRepository;
import com.chughes.atmystop.common.service.BusStopsService;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.onebusaway.gtfs.serialization.GtfsReader;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static java.util.Calendar.*;

@Service
public class GtfsLoader {

    private AgencyRepository agencyRepository;
    private RedisTemplate<String, Object> redisTemplate;
    private BusStopsService busStopsService;
    private GtfsProcessorProvider gtfsProcessorProvider;

    private GtfsDaoImpl store;
    private String agencyId = null;

    private static final String AGENCY_UNIQUE = "actransit";

    public static final int HOURS_AFTER_MIDNIGHT = 5;
    private TimeZone timeZone;

    public GtfsLoader(AgencyRepository agencyRepository, RedisTemplate<String, Object> redisTemplate, BusStopsService busStopsService, GtfsProcessorProvider gtfsProcessorProvider) {
        this.agencyRepository = agencyRepository;
        this.redisTemplate = redisTemplate;
        this.busStopsService = busStopsService;
        this.gtfsProcessorProvider = gtfsProcessorProvider;
    }

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
            reader.setInputLocation(new File("/Users/chughes/code/atmystop/gtfs_atmystop.zip"));
            store = new GtfsDaoImpl();
            reader.setEntityStore(store);
            reader.run();

            String agencyGtfsId = store.getAllAgencies().stream().findFirst().get().getId();

            timeZone = TimeZone.getTimeZone(store.getAllAgencies().stream().findFirst().get().getTimezone());

            agencyId = agencyGtfsId + "_" + AGENCY_UNIQUE;

            GtfsProcessorProvider.GtfsProcessor processor = gtfsProcessorProvider.getGtfsProcessor(store, timeZone, AGENCY_UNIQUE);

            processor.processGtfs();
        } catch (IOException e) {
            throw new RuntimeException("Can't read gtfs file",e);
        }

        populateServiceIds();

    }

    public void populateServiceIds(){
        Agency agency = new Agency();
        agency.setId(agencyId);
        agency.setTimeZone(timeZone);
        agency.setServiceIds(getCurrentServiceIds());
        agency.setYesterdayServiceIds(getYesterdayServiceIds());
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

    private ArrayList<String> getYesterdayServiceIds(){
        ArrayList<String> service = new ArrayList<>();
        ServiceDate serviceDate = getServiceDate().previous();
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

    private int getDayOfWeek(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek;
    }

    private ServiceDate getServiceDate(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return new ServiceDate(calendar);
    }
}
