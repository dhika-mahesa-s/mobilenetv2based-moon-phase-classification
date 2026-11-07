package com.example.prediksifasebulan.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.prediksifasebulan.data.local.AppDatabase;
import com.example.prediksifasebulan.data.local.dao.WeatherDao;
import com.example.prediksifasebulan.data.local.entity.WeatherData;
import com.example.prediksifasebulan.data.remote.RetrofitClient;
import com.example.prediksifasebulan.data.remote.model.StormglassResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {
    private static final int SINGLE_ROW_ID = 1;
    private final WeatherDao weatherDao;
    private final ExecutorService executorService;

    public interface FetchCallback {
        void onSuccess(WeatherData weatherData);

        void onError(String message);
    }

    public WeatherRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        weatherDao = db.weatherDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<WeatherData> getLatestWeather() {
        return weatherDao.getLatestWeather();
    }

    public void fetchAndStoreWeather(double lat, double lng, String locationName, FetchCallback callback) {
        long now = System.currentTimeMillis() / 1000L;
        long end = now + (6 * 60 * 60);

        RetrofitClient.getInstance()
                .getStormglassApi()
                .getWeather(lat, lng, "waveHeight,windSpeed,visibility", "sg", now, end)
                .enqueue(new Callback<StormglassResponse>() {
                    @Override
                    public void onResponse(Call<StormglassResponse> call, Response<StormglassResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("Gagal mengambil data cuaca");
                            return;
                        }

                        StormglassResponse body = response.body();
                        List<StormglassResponse.HourData> hours = body.getHours();
                        if (hours == null || hours.isEmpty()) {
                            callback.onError("Data cuaca kosong");
                            return;
                        }

                        StormglassResponse.HourData firstHour = hours.get(0);
                        double waveHeight = safeValue(firstHour.getWaveHeight());
                        double windSpeed = safeValue(firstHour.getWindSpeed());
                        double visibility = safeValue(firstHour.getVisibility());

                        WeatherData weatherData = new WeatherData(
                                SINGLE_ROW_ID,
                                lat,
                                lng,
                                waveHeight,
                                windSpeed,
                                visibility,
                                System.currentTimeMillis(),
                                locationName
                        );

                        executorService.execute(() -> {
                            weatherDao.insertWeather(weatherData);
                            callback.onSuccess(weatherData);
                        });
                    }

                    @Override
                    public void onFailure(Call<StormglassResponse> call, Throwable t) {
                        callback.onError("Error jaringan: " + t.getMessage());
                    }
                });
    }

    private double safeValue(StormglassResponse.Parameter parameter) {
        if (parameter == null || parameter.getSg() == null) {
            return -1d;
        }
        return parameter.getSg();
    }
}
