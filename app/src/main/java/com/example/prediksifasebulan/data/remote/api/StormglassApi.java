package com.example.prediksifasebulan.data.remote.api;

import com.example.prediksifasebulan.data.remote.model.StormglassResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface StormglassApi {
    @GET("weather/point")
    Call<StormglassResponse> getWeather(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("params") String params,
            @Query("source") String source,
            @Query("start") long start,
            @Query("end") long end
    );
}
