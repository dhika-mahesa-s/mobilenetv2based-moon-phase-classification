package com.example.prediksifasebulan.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.prediksifasebulan.data.local.entity.WeatherData;
import com.example.prediksifasebulan.data.repository.WeatherRepository;

public class MainViewModel extends AndroidViewModel {
    private final WeatherRepository weatherRepository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        weatherRepository = new WeatherRepository(application);
    }

    public LiveData<WeatherData> getLatestWeather() {
        return weatherRepository.getLatestWeather();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void fetchWeather(double lat, double lng, String locationName) {
        loading.postValue(true);
        weatherRepository.fetchAndStoreWeather(lat, lng, locationName, new WeatherRepository.FetchCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                loading.postValue(false);
                message.postValue("Data cuaca berhasil diperbarui");
            }

            @Override
            public void onError(String msg) {
                loading.postValue(false);
                message.postValue(msg);
            }
        });
    }
}
