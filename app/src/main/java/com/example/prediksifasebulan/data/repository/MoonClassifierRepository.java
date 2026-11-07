package com.example.prediksifasebulan.data.repository;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.prediksifasebulan.ml.MoonPhaseClassifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoonClassifierRepository {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MoonPhaseClassifier classifier;

    public interface ClassificationCallback {
        void onSuccess(MoonPhaseClassifier.ClassificationResult result);

        void onError(String message);
    }

    public MoonClassifierRepository(Context context) {
        classifier = new MoonPhaseClassifier(context);
    }

    public void classify(Bitmap bitmap, ClassificationCallback callback) {
        executorService.execute(() -> {
            try {
                MoonPhaseClassifier.ClassificationResult result = classifier.classify(bitmap);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onError("Gagal klasifikasi: " + e.getMessage());
            }
        });
    }

    public void close() {
        classifier.close();
    }
}
