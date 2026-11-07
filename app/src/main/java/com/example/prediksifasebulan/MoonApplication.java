package com.example.prediksifasebulan;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class MoonApplication extends Application {
    public static final String PREFS = "app_preferences";
    public static final String LANGUAGE_KEY = "app_language";
    @Override public void onCreate() {
        super.onCreate();
        String language = getSharedPreferences(PREFS, MODE_PRIVATE).getString(LANGUAGE_KEY, "in");
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }
    public static void setLanguage(android.content.Context context, String language) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        preferences.edit().putString(LANGUAGE_KEY, language).apply();
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
    }
}
