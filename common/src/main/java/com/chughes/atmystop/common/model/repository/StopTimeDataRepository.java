package com.chughes.atmystop.common.model.repository;

import com.chughes.atmystop.common.model.StopTimeData;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StopTimeDataRepository extends CrudRepository<StopTimeData, String> {

    List<StopTimeData> findAllByStopIdAndLastStop(String stopId, boolean lastStop);

}
