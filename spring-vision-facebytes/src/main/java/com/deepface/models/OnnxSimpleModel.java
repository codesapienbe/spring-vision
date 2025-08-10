package com.deepface.models;

import ai.onnxruntime.*;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Collections;

public final class OnnxSimpleModel implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    public OnnxSimpleModel(String modelPath) throws OrtException {
        if (modelPath == null || modelPath.isBlank() || !new File(modelPath).isFile()) {
            throw new IllegalArgumentException("Invalid ONNX model path: " + modelPath);
        }
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
    }

    public float[] run(float[] nchw, long[] shape) throws OrtException {
        try (OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape)) {
            try (OrtSession.Result res = session.run(Collections.singletonMap(inputName, input))) {
                for (var e : res) {
                    OnnxValue ov = e.getValue();
                    if (ov instanceof OnnxTensor t) {
                        Object v = t.getValue();
                        if (v instanceof float[] a) return a;
                        if (v instanceof float[][] a2) return a2[0];
                        if (v instanceof float[][][] a3) return a3[0][0];
                    }
                }
            }
        }
        throw new OrtException("Unsupported ONNX output type");
    }

    @Override
    public void close() {
        try { session.close(); } catch (Throwable ignored) {}
        try { env.close(); } catch (Throwable ignored) {}
    }
}