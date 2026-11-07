package com.example.prediksifasebulan.utils;

import android.content.Context;
import com.example.prediksifasebulan.R;

import java.util.concurrent.TimeUnit;

public class DateTimeUtils {
    public static String getRelativeHours(Context context, long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - timestampMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours <= 0) {
            return context.getString(R.string.just_now);
        }
        return context.getString(R.string.hours_ago, hours);
    }

    public static boolean isOlderThanHours(long timestampMillis, int hours) {
        long diff = Math.abs(System.currentTimeMillis() - timestampMillis);
        return diff >= TimeUnit.HOURS.toMillis(hours);
    }
}
