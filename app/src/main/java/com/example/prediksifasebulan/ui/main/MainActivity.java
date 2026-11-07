package com.example.prediksifasebulan.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.prediksifasebulan.databinding.ActivityMainBinding;
import com.example.prediksifasebulan.MoonApplication;
import com.example.prediksifasebulan.databinding.BottomSheetClassifyBinding;
import com.example.prediksifasebulan.ui.result.ResultActivity;
import com.example.prediksifasebulan.ui.settings.SettingsActivity;
import com.example.prediksifasebulan.utils.DateTimeUtils;
import com.example.prediksifasebulan.utils.LocationHelper;
import com.example.prediksifasebulan.utils.NetworkUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private Uri cameraImageUri;
    private File currentPhotoFile;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    private CancellationTokenSource gpsCancellationTokenSource;
    private final Handler gpsTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable gpsTimeoutRunnable;
    private boolean gpsRequestInProgress;
    private boolean weatherLoading;

    private static final int GPS_PERMISSION_REQUEST = 1001;
    private static final long GPS_TIMEOUT_MS = 15_000L;
    private static final String PREFS_NAME = "manual_location_prefs";
    private static final String KEY_MANUAL_NAME = "manual_name";
    private static final String KEY_MANUAL_LAT = "manual_lat";
    private static final String KEY_MANUAL_LNG = "manual_lng";
    private static final String KEY_USE_MANUAL = "use_manual";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupActivityResults();
        setupListeners();
        observeViewModel();

        updateLocationLabel();
    }

    private void setupActivityResults() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                openResult(uri);
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                openResult(cameraImageUri);
            } else {
                Toast.makeText(this, "Gagal mengambil foto", Toast.LENGTH_SHORT).show();
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
            boolean locationGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                    || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

            if (cameraGranted) {
                startCamera();
            }
            if (locationGranted) {
                updateLocationLabel();
            }
        });
    }

    private void setupListeners() {
        Button btnGps = findViewById(com.example.prediksifasebulan.R.id.btn_gps);
        btnGps.setOnClickListener(v -> startGpsLocationFlow());

        binding.btnRefreshWeather.setOnClickListener(v -> {
            if (!NetworkUtils.isConnected(this)) {
                Toast.makeText(this, "Tidak ada koneksi. Data terakhir tetap digunakan.", Toast.LENGTH_SHORT).show();
                return;
            }
            ManualLocation manualLocation = getManualLocation();
            if (isManualModeEnabled() && manualLocation != null) {
                fetchWeatherForLocation(manualLocation.latitude, manualLocation.longitude, manualLocation.name);
                return;
            }
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestLocationPermissionIfNeeded();
                return;
            }
            fetchWeather();
        });

        binding.btnClassify.setOnClickListener(v -> showClassifySheet());

        binding.btnManualLocation.setOnClickListener(v -> showManualLocationDialog());
        binding.btnSettings.setOnClickListener(v -> toggleLanguage());
    }

    private void observeViewModel() {
        viewModel.getLatestWeather().observe(this, weatherData -> {
            if (weatherData == null) {
                binding.tvLocation.setText("-");
            binding.tvWeatherTime.setText(getString(com.example.prediksifasebulan.R.string.last_updated, "-"));
                binding.tvWeatherWarning.setText("");
                binding.tvWaveHeight.setText("-");
                binding.tvWindSpeed.setText("-");
                binding.tvVisibility.setText("-");
                return;
            }
            binding.tvLocation.setText(weatherData.getLocationName());
            String relative = DateTimeUtils.getRelativeHours(this, weatherData.getFetchedAt());
            binding.tvWeatherTime.setText(getString(com.example.prediksifasebulan.R.string.last_updated, relative));
            binding.tvWaveHeight.setText(getString(com.example.prediksifasebulan.R.string.wave_value, weatherData.getWaveHeight()));
            binding.tvWindSpeed.setText(getString(com.example.prediksifasebulan.R.string.wind_value, weatherData.getWindSpeed()));
            binding.tvVisibility.setText(getString(com.example.prediksifasebulan.R.string.visibility_value, weatherData.getVisibility()));
            if (DateTimeUtils.isOlderThanHours(weatherData.getFetchedAt(), 6)) {
                binding.tvWeatherWarning.setText(com.example.prediksifasebulan.R.string.weather_stale);
            } else {
                binding.tvWeatherWarning.setText("");
            }
        });

        viewModel.getLoading().observe(this, isLoading -> {
            weatherLoading = Boolean.TRUE.equals(isLoading);
            updateProgressVisibility();
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startGpsLocationFlow() {
        setGpsLoading(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchGPSLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSION_REQUEST
            );
        }
    }

    private void fetchGPSLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            setGpsLoading(false);
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    GPS_PERMISSION_REQUEST
            );
            return;
        }

        if (!isGpsEnabled()) {
            setGpsLoading(false);
            showGpsDisabledDialog();
            return;
        }

        setGpsLoading(true);
        cancelGpsTimeout();
        if (gpsCancellationTokenSource != null) {
            gpsCancellationTokenSource.cancel();
        }

        gpsCancellationTokenSource = new CancellationTokenSource();
        gpsTimeoutRunnable = () -> {
            if (!gpsRequestInProgress) return;
            if (gpsCancellationTokenSource != null) {
                gpsCancellationTokenSource.cancel();
            }
            Toast.makeText(this, "GPS tidak menemukan sinyal. Coba lagi atau gunakan lokasi manual.", Toast.LENGTH_SHORT).show();
            finishGpsRequest();
        };
        gpsTimeoutHandler.postDelayed(gpsTimeoutRunnable, GPS_TIMEOUT_MS);

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                gpsCancellationTokenSource.getToken()
        ).addOnSuccessListener(this, location -> {
            if (!gpsRequestInProgress) return;
            if (location == null) {
                Toast.makeText(this, "GPS tidak menemukan sinyal. Coba lagi atau gunakan lokasi manual.", Toast.LENGTH_SHORT).show();
                finishGpsRequest();
                return;
            }
            handleGpsLocation(location);
        }).addOnFailureListener(this, e -> {
            if (!gpsRequestInProgress) return;
            Toast.makeText(this, "GPS tidak menemukan sinyal. Coba lagi atau gunakan lokasi manual.", Toast.LENGTH_SHORT).show();
            finishGpsRequest();
        });
    }

    private void handleGpsLocation(Location location) {
        new Thread(() -> {
            String locationName = resolveLocationName(location);
            runOnUiThread(() -> {
                if (!gpsRequestInProgress) return;
                setManualModeEnabled(false);
                binding.tvLocation.setText(locationName);
                binding.tvWeatherTime.setText("GPS • Baru saja");
                fetchWeatherForLocation(location.getLatitude(), location.getLongitude(), locationName);
                finishGpsRequest();
            });
        }).start();
    }

    private void fetchWeather() {
        ManualLocation manualLocation = getManualLocation();
        if (isManualModeEnabled() && manualLocation != null) {
            fetchWeatherForLocation(manualLocation.latitude, manualLocation.longitude, manualLocation.name);
            return;
        }
        LocationHelper locationHelper = new LocationHelper(this);
        locationHelper.getLastLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocation(Location location, String locationName) {
                fetchWeatherForLocation(location.getLatitude(), location.getLongitude(), locationName);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                if (message.toLowerCase(Locale.US).contains("nonaktif")) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            }
        });
    }

    private void fetchWeatherForLocation(double latitude, double longitude, String locationName) {
        viewModel.fetchWeather(latitude, longitude, locationName);
    }

    private void updateLocationLabel() {
        ManualLocation manualLocation = getManualLocation();
        if (isManualModeEnabled() && manualLocation != null) {
            binding.tvLocation.setText("Manual: " + manualLocation.name);
            return;
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            binding.tvLocation.setText("-");
            return;
        }
        LocationHelper locationHelper = new LocationHelper(this);
        locationHelper.getLastLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocation(Location location, String locationName) {
                binding.tvLocation.setText(locationName);
            }

            @Override
            public void onError(String message) {
                binding.tvLocation.setText(message);
            }
        });
    }

    private void startCamera() {
        try {
            currentPhotoFile = createImageFile();
            String authority = getPackageName() + ".fileprovider";
            cameraImageUri = FileProvider.getUriForFile(this, authority, currentPhotoFile);
            takePictureLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(this, "Gagal membuat file foto", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openResult(Uri uri) {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString());
        startActivity(intent);
    }

    private void toggleLanguage() {
        String current = getSharedPreferences(MoonApplication.PREFS, MODE_PRIVATE)
                .getString(MoonApplication.LANGUAGE_KEY, "in");
        MoonApplication.setLanguage(this, "en".equals(current) ? "in" : "en");
        recreate();
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissionIfNeeded() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                || hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return;
        }
        permissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void showManualLocationDialog() {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("Nama lokasi (contoh: Jakarta)");

        android.widget.EditText latInput = new android.widget.EditText(this);
        latInput.setHint("Latitude, contoh -6.2000");
        latInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);

        android.widget.EditText lngInput = new android.widget.EditText(this);
        lngInput.setHint("Longitude, contoh 106.8166");
        lngInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(nameInput);
        layout.addView(latInput);
        layout.addView(lngInput);

        String[] presetNames = new String[] {
                "Jakarta (Ancol)",
                "Surabaya (Kenjeran)",
                "Semarang (Marina)",
                "Makassar (Losari)",
                "Bali (Sanur)",
                "Lombok (Mataram)",
                "Manado (Malalayang)",
                "Padang (Bungus)",
                "Palembang (Musi)",
                "Banjarmasin (Siring)",
                "Batam (Nongsa)"
        };
        double[][] presetCoords = new double[][] {
                {-6.1175, 106.8539},
                {-7.2247, 112.7616},
                {-6.9480, 110.4086},
                {-5.1477, 119.4158},
                {-8.6930, 115.2625},
                {-8.5887, 116.1210},
                {1.4570, 124.8230},
                {-1.0306, 100.3964},
                {-2.9896, 104.7565},
                {-3.3186, 114.5900},
                {1.1189, 104.0977}
        };

        new AlertDialog.Builder(this)
                .setTitle("Pilih Lokasi Manual")
                .setItems(presetNames, (dialog, which) -> {
                    double[] coord = presetCoords[which];
                    saveManualLocation(presetNames[which], coord[0], coord[1]);
                    setManualModeEnabled(true);
                    updateLocationLabel();
                    Toast.makeText(this, "Lokasi preset dipilih", Toast.LENGTH_SHORT).show();
                })
                .setView(layout)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String latText = latInput.getText().toString().trim();
                    String lngText = lngInput.getText().toString().trim();
                    if (name.isEmpty() || latText.isEmpty() || lngText.isEmpty()) {
                        Toast.makeText(this, "Lengkapi semua field lokasi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double lat = Double.parseDouble(latText);
                        double lng = Double.parseDouble(lngText);
                        saveManualLocation(name, lat, lng);
                        setManualModeEnabled(true);
                        updateLocationLabel();
                        Toast.makeText(this, "Lokasi manual disimpan", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Format koordinat tidak valid", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Gunakan GPS", (dialog, which) -> startGpsLocationFlow())
                .setNeutralButton("Batal", null)
                .show();
    }

    private void saveManualLocation(String name, double lat, double lng) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_MANUAL_NAME, name)
                .putFloat(KEY_MANUAL_LAT, (float) lat)
                .putFloat(KEY_MANUAL_LNG, (float) lng)
                .apply();
    }

    private void clearManualLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private ManualLocation getManualLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String name = prefs.getString(KEY_MANUAL_NAME, null);
        if (name == null) {
            return null;
        }
        double lat = prefs.getFloat(KEY_MANUAL_LAT, 0f);
        double lng = prefs.getFloat(KEY_MANUAL_LNG, 0f);
        return new ManualLocation(name, lat, lng);
    }

    private void setManualModeEnabled(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_USE_MANUAL, enabled).apply();
    }

    private boolean isManualModeEnabled() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_USE_MANUAL, false);
    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private String resolveLocationName(Location location) {
        String fallback = formatCoordinateLabel(location);
        if (!Geocoder.isPresent()) {
            return fallback;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locality = address.getLocality();
                String subAdmin = address.getSubAdminArea();
                String feature = address.getFeatureName();
                if (locality != null && !locality.isEmpty()) return locality;
                if (subAdmin != null && !subAdmin.isEmpty()) return subAdmin;
                if (feature != null && !feature.isEmpty()) return feature;
            }
        } catch (IOException ignored) {
        }
        return fallback;
    }

    private String formatCoordinateLabel(Location location) {
        return String.format(Locale.US, "Lat: %.2f, Lon: %.2f", location.getLatitude(), location.getLongitude());
    }

    private void showGpsDisabledDialog() {
        new AlertDialog.Builder(this)
                .setMessage("GPS tidak aktif. Aktifkan lokasi di pengaturan?")
                .setPositiveButton("Buka Pengaturan", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Izin lokasi diperlukan agar aplikasi dapat mengambil posisi GPS. Buka pengaturan aplikasi?")
                .setPositiveButton("Buka Pengaturan", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setGpsLoading(boolean loading) {
        gpsRequestInProgress = loading;
        binding.btnGps.setEnabled(!loading);
        binding.btnManualLocation.setEnabled(!loading);
        updateProgressVisibility();
    }

    private void updateProgressVisibility() {
        binding.progressWeather.setVisibility((gpsRequestInProgress || weatherLoading)
                ? android.view.View.VISIBLE
                : android.view.View.GONE);
    }

    private void finishGpsRequest() {
        cancelGpsTimeout();
        setGpsLoading(false);
    }

    private void cancelGpsTimeout() {
        if (gpsTimeoutRunnable != null) {
            gpsTimeoutHandler.removeCallbacks(gpsTimeoutRunnable);
            gpsTimeoutRunnable = null;
        }
    }

    private void cancelGpsRequest() {
        cancelGpsTimeout();
        if (gpsCancellationTokenSource != null) {
            gpsCancellationTokenSource.cancel();
            gpsCancellationTokenSource = null;
        }
        if (gpsRequestInProgress) {
            setGpsLoading(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != GPS_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchGPSLocation();
            return;
        }

        setGpsLoading(false);
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionSettingsDialog();
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan. Gunakan Pilih Lokasi Manual sebagai alternatif.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelGpsRequest();
    }

    @Override
    protected void onDestroy() {
        cancelGpsRequest();
        super.onDestroy();
    }

    private static class ManualLocation {
        final String name;
        final double latitude;
        final double longitude;

        ManualLocation(String name, double latitude, double longitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private void showClassifySheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        BottomSheetClassifyBinding sheetBinding = BottomSheetClassifyBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        sheetBinding.btnBsCamera.setOnClickListener(v -> {
            dialog.dismiss();
            if (hasPermission(Manifest.permission.CAMERA)) {
                startCamera();
            } else {
                permissionLauncher.launch(new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_IMAGES
                });
            }
        });

        sheetBinding.btnBsGallery.setOnClickListener(v -> {
            dialog.dismiss();
            pickImageLauncher.launch("image/*");
        });

        dialog.show();
    }
}
