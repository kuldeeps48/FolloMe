package com.theeralabs.follome.api;


import com.theeralabs.follome.model.directionMatrix.direction.DirectionMatrix;
import com.theeralabs.follome.model.directionMatrix.distanceMatrix.DistanceMatrix;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Kuldeep on 01-Nov-17.
 */

public interface ApiInterface {

    @GET("json?")
    Call<DistanceMatrix> getDistance(@Query("origins") String originLatLng, @Query("destinations") String destLatLng, @Query("key") String APIKEY);

    @GET("json?")
    Call<DirectionMatrix> getDirection(@Query("origin") String originLatLng, @Query("destination") String destLatLng, @Query("key") String APIKEY);


}
