package com.example.prediksifasebulan.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weather_data")
public class WeatherData {
    @PrimaryKey
    private int id;
    private double latitude;
    private double longitude;
    private double waveHeight;
    private double windSpeed;
    private double visibility;
    private long fetchedAt;
    private String locationName;

    public WeatherData(int id, double latitude, double longitude, double waveHeight, double windSpeed,
                       double visibility, long fetchedAt, String locationName) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.waveHeight = waveHeight;
        this.windSpeed = windSpeed;
        this.visibility = visibility;
        this.fetchedAt = fetchedAt;
        this.locationName = locationName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getWaveHeight() {
        return waveHeight;
    }

    public void setWaveHeight(double waveHeight) {
        this.waveHeight = waveHeight;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getVisibility() {
        return visibility;
    }

    public void setVisibility(double visibility) {
        this.visibility = visibility;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
