package com.example.prediksifasebulan.ui.settings;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.prediksifasebulan.MoonApplication;
import com.example.prediksifasebulan.R;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density); root.setPadding(pad,pad,pad,pad);
        TextView title = new TextView(this); title.setText(R.string.settings); title.setTextSize(22); root.addView(title);
        TextView label = new TextView(this); label.setText(R.string.language_selection); label.setTextSize(16); root.addView(label);
        RadioGroup group = new RadioGroup(this); RadioButton indonesian = new RadioButton(this); indonesian.setText(R.string.language_indonesian); indonesian.setId(1);
        RadioButton english = new RadioButton(this); english.setText(R.string.language_english); english.setId(2); group.addView(indonesian); group.addView(english); root.addView(group); setContentView(root);
        String current = getSharedPreferences(MoonApplication.PREFS, MODE_PRIVATE).getString(MoonApplication.LANGUAGE_KEY, "in"); group.check("en".equals(current) ? 2 : 1);
        group.setOnCheckedChangeListener((g, id) -> { String selected = id == 2 ? "en" : "in"; if (!selected.equals(current)) { MoonApplication.setLanguage(this, selected); recreate(); } });
    }
}
