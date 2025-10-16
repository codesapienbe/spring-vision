package io.github.codesapienbe.springvision.core.djl.translator;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.DataType;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Custom translator for SFace face recognition model.
 *
 * <p>SFace is a face recognition model from OpenCV Zoo that generates
 * 128-dimensional face embeddings for face recognition tasks.</p>
 *
 * <p>Model details:</p>
 * <ul>
 *   <li>Input: RGB face image, resized to 112x112</li>
 *   <li>Output: 128-dimensional embedding vector</li>
 *   <li>Format: ONNX</li>
 *   <li>Use case: Face verification, identification</li>
 * </ul>
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public class SFaceFaceRecognitionTranslator implements Translator<Image, float[]> {

    private static final int INPUT_WIDTH = 112;
    private static final int INPUT_HEIGHT = 112;

    @Override
    public NDList processInput(TranslatorContext ctx, Image input) {
        // Resize face image to model input size
        Image resized = input.resize(INPUT_WIDTH, INPUT_HEIGHT, false);

        // Convert to NDArray
        NDArray array = resized.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);

        // Convert to float and normalize to [0, 1]
        array = array.toType(DataType.FLOAT32, false);
        array = array.div(255.0f);

        // Normalize with ImageNet mean and std
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};

        // Split channels
        NDList channelList = array.split(3, 2);
        NDArray[] channels = channelList.toArray(new NDArray[0]);

        // Normalize each channel
        for (int i = 0; i < 3; i++) {
            channels[i] = channels[i].sub(mean[i]).div(std[i]);
        }

        // Concatenate channels back
        array = NDArrays.concat(new NDList(channels), 2);

        // Transpose from HWC to CHW format
        array = array.transpose(2, 0, 1);

        // Add batch dimension
        array = array.expandDims(0);

        return new NDList(array);
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list) {
        // SFace output: [batch, 128] embedding vector
        NDArray embeddings = list.singletonOrThrow();

        // Remove batch dimension and convert to float array
        embeddings = embeddings.squeeze(0);

        // Normalize the embedding (L2 normalization)
        float[] embedding = embeddings.toFloatArray();
        float norm = 0.0f;

        for (float value : embedding) {
            norm += value * value;
        }

        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }

        return embedding;
    }

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }
}
