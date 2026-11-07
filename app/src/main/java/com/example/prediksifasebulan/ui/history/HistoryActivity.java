package com.example.prediksifasebulan.ui.history;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.prediksifasebulan.databinding.ActivityHistoryBinding;
import com.example.prediksifasebulan.ui.main.MainViewModel;
import com.example.prediksifasebulan.utils.DateTimeUtils;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getLatestWeather().observe(this, weather -> {
            if (weather == null) {
                binding.tvHistoryContent.setText("Belum ada data cuaca tersimpan.");
                return;
            }
            String content = "Lokasi: " + weather.getLocationName()
                    + "\nGelombang: " + weather.getWaveHeight() + " m"
                    + "\nAngin: " + weather.getWindSpeed() + " km/jam"
                    + "\nVisibilitas: " + weather.getVisibility() + " km"
                    + "\nTerakhir: " + DateTimeUtils.getRelativeHours(this, weather.getFetchedAt());
            binding.tvHistoryContent.setText(content);
        });
    }
}
