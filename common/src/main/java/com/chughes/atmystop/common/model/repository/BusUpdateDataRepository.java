package com.chughes.atmystop.common.model.repository;

import com.chughes.atmystop.common.model.AgencyTripId;
import com.chughes.atmystop.common.model.BusUpdateData;
import org.springframework.data.repository.CrudRepository;

public interface BusUpdateDataRepository extends CrudRepository<BusUpdateData, Integer> {
}
