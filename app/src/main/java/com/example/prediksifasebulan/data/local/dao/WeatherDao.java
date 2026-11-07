package com.example.prediksifasebulan.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.prediksifasebulan.data.local.entity.WeatherData;

@Dao
public interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWeather(WeatherData weatherData);

    @Query("SELECT * FROM weather_data ORDER BY fetchedAt DESC LIMIT 1")
    LiveData<WeatherData> getLatestWeather();

    @Query("SELECT * FROM weather_data WHERE latitude = :lat AND longitude = :lng ORDER BY fetchedAt DESC LIMIT 1")
    WeatherData getWeatherByLocation(double lat, double lng);

}
