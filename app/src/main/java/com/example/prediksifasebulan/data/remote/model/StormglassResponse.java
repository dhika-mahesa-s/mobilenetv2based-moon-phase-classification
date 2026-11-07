package com.example.prediksifasebulan.data.remote.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class StormglassResponse {
    @SerializedName("hours")
    private List<HourData> hours;

    public List<HourData> getHours() {
        return hours;
    }

    public static class HourData {
        @SerializedName("waveHeight")
        private Parameter waveHeight;

        @SerializedName("windSpeed")
        private Parameter windSpeed;

        @SerializedName("visibility")
        private Parameter visibility;

        public Parameter getWaveHeight() {
            return waveHeight;
        }

        public Parameter getWindSpeed() {
            return windSpeed;
        }

        public Parameter getVisibility() {
            return visibility;
        }
    }

    public static class Parameter {
        @SerializedName("sg")
        private Double sg;

        public Double getSg() {
            return sg;
        }
    }
}
