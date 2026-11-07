package com.example.prediksifasebulan.ui.result;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.prediksifasebulan.data.local.entity.WeatherData;
import com.example.prediksifasebulan.data.repository.MoonClassifierRepository;
import com.example.prediksifasebulan.data.repository.WeatherRepository;
import com.example.prediksifasebulan.domain.RecommendationEngine;
import com.example.prediksifasebulan.domain.RecommendationResult;
import com.example.prediksifasebulan.ml.MoonPhaseClassifier;

public class ResultViewModel extends AndroidViewModel {
    private final WeatherRepository weatherRepository;
    private final MoonClassifierRepository classifierRepository;

    private final MutableLiveData<MoonPhaseClassifier.ClassificationResult> classification = new MutableLiveData<>();
    private final MutableLiveData<RecommendationResult> recommendation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ResultViewModel(@NonNull Application application) {
        super(application);
        weatherRepository = new WeatherRepository(application);
        classifierRepository = new MoonClassifierRepository(application);
    }

    public LiveData<MoonPhaseClassifier.ClassificationResult> getClassification() {
        return classification;
    }

    public LiveData<RecommendationResult> getRecommendation() {
        return recommendation;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<WeatherData> getLatestWeather() {
        return weatherRepository.getLatestWeather();
    }

    public void classifyAndRecommend(Bitmap bitmap, WeatherData weatherData) {
        loading.postValue(true);
        classifierRepository.classify(bitmap, new MoonClassifierRepository.ClassificationCallback() {
            @Override
            public void onSuccess(MoonPhaseClassifier.ClassificationResult result) {
                classification.postValue(result);
                recommendation.postValue(RecommendationEngine.getRecommendation(result.getClassName(), weatherData));
                loading.postValue(false);
            }

            @Override
            public void onError(String message) {
                error.postValue(message);
                loading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        classifierRepository.close();
    }
}
