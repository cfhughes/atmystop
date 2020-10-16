package com.chughes.atmystop.abq_data.service;

import com.chughes.atmystop.common.model.BusUpdateData;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class TempDataService {

    private Map<String, BusUpdateData> secondsLateTempDataStore;

    @PostConstruct
    public void init(){
        secondsLateTempDataStore = new HashMap<>();
    }

    public void putSecondsLateTemp(String tripId, BusUpdateData update){
        secondsLateTempDataStore.put(tripId, update);
    }

    public BusUpdateData getSecondsLateTemp(String tripId){
        return secondsLateTempDataStore.get(tripId);
    }

}
