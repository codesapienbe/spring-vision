package com.springvision.core.util;

import com.springvision.core.BoundingBox;
import com.springvision.core.Detection;
import com.springvision.core.ImageData;
import com.springvision.core.exception.VisionProcessingException;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight ZXing-based barcode scanner utility.
 */
public final class ZxingBarcodeScanner {

    private ZxingBarcodeScanner() {}

    /**
     * Detects barcodes in the provided ImageData using ZXing's multi-reader.
     * Returns an empty list if no barcodes are found.
     */
    public static List<Detection> detectBarcodes(ImageData imageData) throws VisionProcessingException {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData.data()));
            if (img == null) {
                throw new VisionProcessingException("Failed to decode image data for barcode detection", "detectBarcodes", "barcode");
            }

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);

            Map<DecodeHintType,Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result[] results;
            try {
                results = multiReader.decodeMultiple(bitmap, hints);
            } catch (NotFoundException nf) {
                return List.of(); // No barcodes found
            }

            List<Detection> detections = new ArrayList<>();
            for (Result r : results) {
                String text = r.getText();
                String format = r.getBarcodeFormat().name();

                // Compute bounding box from result points
                java.awt.geom.Rectangle2D.Float rect = pointsToRect(r.getResultPoints());
                int imgW = img.getWidth();
                int imgH = img.getHeight();

                int x = Math.max(0, Math.round(rect.x));
                int y = Math.max(0, Math.round(rect.y));
                int w = Math.max(1, Math.round(rect.width));
                int h = Math.max(1, Math.round(rect.height));

                BoundingBox bbox = BoundingBox.fromPixels(x, y, w, h, imgW, imgH);

                // Attributes: include raw text and format
                Detection d = Detection.of(format, 1.0, bbox, "text", text);
                detections.add(d);
            }

            return detections;

        } catch (VisionProcessingException vpe) {
            throw vpe;
        } catch (NoClassDefFoundError ncd) {
            // ZXing not present on classpath
            throw new VisionProcessingException("ZXing library not available for barcode detection", "detectBarcodes", "barcode", ncd);
        } catch (Exception e) {
            throw new VisionProcessingException("Barcode detection failed", "detectBarcodes", "barcode", e);
        }
    }

    private static java.awt.geom.Rectangle2D.Float pointsToRect(com.google.zxing.ResultPoint[] pts) {
        if (pts == null || pts.length == 0) return new java.awt.geom.Rectangle2D.Float(0,0,0,0);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        for (com.google.zxing.ResultPoint p : pts) {
            if (p == null) continue;
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }
        if (minX == Float.MAX_VALUE) return new java.awt.geom.Rectangle2D.Float(0,0,0,0);
        return new java.awt.geom.Rectangle2D.Float(minX, minY, Math.max(1.0f, maxX - minX), Math.max(1.0f, maxY - minY));
    }
} 