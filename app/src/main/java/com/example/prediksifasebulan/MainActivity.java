package com.example.prediksifasebulan;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


/**
 * Legacy entry point retained for compatibility. Redirects to the new MVVM MainActivity.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, com.example.prediksifasebulan.ui.main.MainActivity.class));
        finish();
    }
}

