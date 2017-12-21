package com.theeralabs.follome.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Kuldeep on 01-Nov-17.
 */

public class GoogleDirectionApiClient {
    public static final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
