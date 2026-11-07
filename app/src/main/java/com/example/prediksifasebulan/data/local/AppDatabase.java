package com.example.prediksifasebulan.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.prediksifasebulan.data.local.dao.WeatherDao;
import com.example.prediksifasebulan.data.local.entity.WeatherData;

@Database(entities = {WeatherData.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract WeatherDao weatherDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "prediksi_fase_bulan.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
