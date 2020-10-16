package com.chughes.atmystop.abq_data.service;

import com.chughes.atmystop.abq_data.model.AbqDataPayload;
import retrofit2.Call;
import retrofit2.http.GET;

public interface AbqDataService {

    @GET("transit/realtime/route/allroutes.json")
    Call<AbqDataPayload> listVehicles();


}
