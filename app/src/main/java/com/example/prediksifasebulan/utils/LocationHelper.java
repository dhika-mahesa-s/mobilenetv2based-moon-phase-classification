package com.example.prediksifasebulan.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    public interface LocationCallback {
        void onLocation(Location location, String locationName);

        void onError(String message);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getLastLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Izin lokasi belum diberikan");
            return;
        }

        if (!isLocationEnabled()) {
            callback.onError("Layanan lokasi nonaktif");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        requestCurrentLocation(callback);
                        return;
                    }
                    String locationName = reverseGeocode(location);
                    callback.onLocation(location, locationName);
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil lokasi: " + e.getMessage()));
    }

    private void requestCurrentLocation(LocationCallback callback) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Lokasi tidak tersedia");
                        return;
                    }
                    String locationName = reverseGeocode(location);
                    callback.onLocation(location, locationName);
                })
                .addOnFailureListener(e -> callback.onError("Gagal mengambil lokasi: " + e.getMessage()));
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private String reverseGeocode(Location location) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String subAdmin = address.getSubAdminArea();
                if (city != null && !city.isEmpty()) {
                    return city;
                }
                if (subAdmin != null && !subAdmin.isEmpty()) {
                    return subAdmin;
                }
                return address.getFeatureName();
            }
        } catch (IOException ignored) {
        }
        return "Lokasi tidak diketahui";
    }
}
