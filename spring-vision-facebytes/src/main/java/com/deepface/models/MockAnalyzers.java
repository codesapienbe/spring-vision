package com.deepface.models;

import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;

public final class MockAnalyzers {

    private MockAnalyzers() {}

    public static int predictAge(BufferedImage img) {
        SplittableRandom rnd = rng(img);
        return 20 + rnd.nextInt(41); // 20-60
    }

    public static String predictGender(BufferedImage img) {
        return rng(img).nextBoolean() ? "male" : "female";
    }

    public static Map<String, Double> predictEmotionDistribution(BufferedImage img) {
        String[] emotions = {"angry","disgust","fear","happy","sad","surprise","neutral"};
        return softmax(emotions, rng(img));
    }

    public static Map<String, Double> predictRaceDistribution(BufferedImage img) {
        String[] races = {"asian","indian","black","white","middle_eastern","latino_hispanic"};
        return softmax(races, rng(img));
    }

    private static Map<String, Double> softmax(String[] keys, SplittableRandom rnd) {
        double[] logits = new double[keys.length];
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < keys.length; i++) {
            logits[i] = rnd.nextDouble(-1.0, 1.0);
            if (logits[i] > max) max = logits[i];
        }
        double sum = 0.0;
        for (int i = 0; i < logits.length; i++) {
            logits[i] = Math.exp(logits[i] - max);
            sum += logits[i];
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], logits[i] / sum);
        }
        return map;
    }

    private static SplittableRandom rng(BufferedImage img) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int w = img.getWidth();
            int h = img.getHeight();
            for (int y = 0; y < h; y += Math.max(1, h / 16)) {
                for (int x = 0; x < w; x += Math.max(1, w / 16)) {
                    int rgb = img.getRGB(x, y);
                    md.update((byte) (rgb >> 16));
                    md.update((byte) (rgb >> 8));
                    md.update((byte) (rgb));
                }
            }
            byte[] dig = md.digest();
            long seed = 0L;
            for (int i = 0; i < Math.min(8, dig.length); i++) {
                seed = (seed << 8) ^ (dig[i] & 0xFF);
            }
            return new SplittableRandom(seed == 0 ? 1L : seed);
        } catch (Exception e) {
            return new SplittableRandom(1L);
        }
    }
}
