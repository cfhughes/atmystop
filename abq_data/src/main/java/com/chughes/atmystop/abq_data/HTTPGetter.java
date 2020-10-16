package com.chughes.atmystop.abq_data;

import com.chughes.atmystop.abq_data.model.Vehicle;
import com.chughes.atmystop.abq_data.service.AbqDataService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public class HTTPGetter {

    private Logger logger = LoggerFactory.getLogger(HTTPGetter.class);

    private final AbqDataService service;

    public HTTPGetter(){
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class,
                        (JsonDeserializer<LocalTime>) (json, type, jsonDeserializationContext)
                                -> {
                            String timeString = json.getAsString();
                            String hour = timeString.split(":")[0];
                            int intHour = Integer.parseInt(hour);
                            if (intHour > 23){
                                intHour = intHour - 24;
                                StringBuilder time = new StringBuilder(timeString);
                                time.replace(0,2, String.format("%02d", intHour));
                                timeString = time.toString();
                            }
                            return LocalTime.parse(timeString);
                        })
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://data.cabq.gov/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        service = retrofit.create(AbqDataService.class);
    }

    public List<Vehicle> get() {
        try {
            return service.listVehicles().execute().body().getAllRoutes();
        } catch (IOException e) {
            logger.error("Error Getting Live Bus Data", e);
        }
        return new ArrayList<>();
    }

}
