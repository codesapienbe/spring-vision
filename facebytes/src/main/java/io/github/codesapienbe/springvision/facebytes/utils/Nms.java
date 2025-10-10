package io.github.codesapienbe.springvision.facebytes.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class Nms {

    private Nms() {
    }

    public static class Box {
        public final float x1, y1, x2, y2, score;

        public Box(float x1, float y1, float x2, float y2, float score) {
            this.x1 = Math.min(x1, x2);
            this.y1 = Math.min(y1, y2);
            this.x2 = Math.max(x1, x2);
            this.y2 = Math.max(y1, y2);
            this.score = score;
        }
    }

    public static List<Integer> nms(List<Box> boxes, double iouThreshold, int topK) {
        if (boxes == null || boxes.isEmpty()) return List.of();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) order.add(i);
        order.sort(Comparator.comparingDouble((Integer i) -> boxes.get(i).score).reversed());
        List<Integer> keep = new ArrayList<>();
        boolean[] removed = new boolean[boxes.size()];
        for (int i = 0; i < order.size(); i++) {
            int idx = order.get(i);
            if (removed[idx]) continue;
            keep.add(idx);
            if (topK > 0 && keep.size() >= topK) break;
            for (int j = i + 1; j < order.size(); j++) {
                int idx2 = order.get(j);
                if (removed[idx2]) continue;
                if (iou(boxes.get(idx), boxes.get(idx2)) > iouThreshold) removed[idx2] = true;
            }
        }
        return keep;
    }

    private static double iou(Box a, Box b) {
        float interX1 = Math.max(a.x1, b.x1);
        float interY1 = Math.max(a.y1, b.y1);
        float interX2 = Math.min(a.x2, b.x2);
        float interY2 = Math.min(a.y2, b.y2);
        float iw = Math.max(0f, interX2 - interX1);
        float ih = Math.max(0f, interY2 - interY1);
        float inter = iw * ih;
        float areaA = Math.max(0f, a.x2 - a.x1) * Math.max(0f, a.y2 - a.y1);
        float areaB = Math.max(0f, b.x2 - b.x1) * Math.max(0f, b.y2 - b.y1);
        float union = areaA + areaB - inter;
        if (union <= 0f) return 0.0;
        return inter / union;
    }
}
