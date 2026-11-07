package com.example.prediksifasebulan.data.remote;

import com.example.prediksifasebulan.BuildConfig;
import com.example.prediksifasebulan.data.remote.api.StormglassApi;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://api.stormglass.io/v2/";
    private static RetrofitClient instance;
    private final StormglassApi stormglassApi;

    private RetrofitClient() {
        Interceptor apiKeyInterceptor = chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("Authorization", BuildConfig.STORMGLASS_API_KEY)
                    .build();
            return chain.proceed(request);
        };

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(apiKeyInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        stormglassApi = retrofit.create(StormglassApi.class);
    }

    public static RetrofitClient getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient();
                }
            }
        }
        return instance;
    }

    public StormglassApi getStormglassApi() {
        return stormglassApi;
    }
}
