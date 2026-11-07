package com.example.prediksifasebulan.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MoonPhaseClassifier {
    private static final int INPUT_SIZE = 224;
    private static final int NUM_CLASSES = 8;

    private final Context context;
    private Interpreter interpreter;
    private List<String> classNames;

    public MoonPhaseClassifier(Context context) {
        this.context = context.getApplicationContext();
        loadModel();
    }

    public void loadModel() {
        try {
            interpreter = new Interpreter(loadModelFile("model_moon_phase_3phase.tflite"));
            classNames = loadClassNames("class_names.txt");
        } catch (IOException e) {
            throw new RuntimeException("Gagal memuat model: " + e.getMessage());
        }
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (interpreter == null) {
            throw new IllegalStateException("Interpreter belum diinisialisasi");
        }

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            float r = ((pixel >> 16) & 0xFF) / 127.5f - 1.0f;
            float g = ((pixel >> 8)  & 0xFF) / 127.5f - 1.0f;
            float b = (pixel         & 0xFF) / 127.5f - 1.0f;
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        inputBuffer.rewind();
        float[][] output = new float[1][NUM_CLASSES];
        interpreter.run(inputBuffer, output);

        float[] probs = output[0];
        int maxIndex = 0;
        float maxScore = probs[0];
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > maxScore) {
                maxScore = probs[i];
                maxIndex = i;
            }
        }
        String className = (classNames != null && classNames.size() > maxIndex)
                ? classNames.get(maxIndex)
                : "Unknown";

        return new ClassificationResult(className, maxScore, probs);
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    private MappedByteBuffer loadModelFile(String assetName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadClassNames(String assetName) throws IOException {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(assetName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    names.add(line.trim());
                }
            }
        }
        return names;
    }

    public static class ClassificationResult {
        private final String className;
        private final float confidence;
        private final float[] allProbabilities;

        public ClassificationResult(String className, float confidence, float[] allProbabilities) {
            this.className = className;
            this.confidence = confidence;
            this.allProbabilities = allProbabilities;
        }

        public String getClassName() {
            return className;
        }

        public float getConfidence() {
            return confidence;
        }

        public float[] getAllProbabilities() {
            return allProbabilities;
        }
    }
}
