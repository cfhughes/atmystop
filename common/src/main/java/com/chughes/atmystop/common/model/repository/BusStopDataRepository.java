package com.chughes.atmystop.common.model.repository;

import com.chughes.atmystop.common.model.BusStopData;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface BusStopDataRepository extends CrudRepository<BusStopData, String> {

    List<BusStopData> findByLocationNear(Point point, Distance distance);

}
