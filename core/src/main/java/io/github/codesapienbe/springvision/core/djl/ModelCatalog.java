package io.github.codesapienbe.springvision.core.djl;

import java.util.List;

/**
 * The model files {@link DjlVisionBackend} loads from {@code models/}, with the sources and
 * checksums the {@code download-models} Maven profile uses. The {@link ModelStore} fetches them
 * at runtime when they are not on the classpath.
 */
public final class ModelCatalog {

    private ModelCatalog() {
    }

    private static final String RELEASE = "https://github.com/codesapienbe/spring-vision/releases/download/v0.0.4/";
    private static final String TESSDATA_FAST =
        "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/87416418657359cb625c412a48b6e1d6d41c29bd/";

    public static final String TESSDATA_ENG = "tessdata/eng.traineddata";
    public static final String TESSDATA_NLD = "tessdata/nld.traineddata";
    public static final String TESSDATA_FRA = "tessdata/fra.traineddata";
    public static final String TESSDATA_DEU = "tessdata/deu.traineddata";
    public static final String EMOTION = "emotion-ferplus/emotion-ferplus-8.onnx";
    public static final String AGE = "age-gender/age_googlenet.onnx";
    public static final String GENDER = "age-gender/gender_googlenet.onnx";
    public static final String PALM = "palm-detection/palm_detection_mediapipe.onnx";
    public static final String NSFW = "nsfw-detection/nsfw-detection-vit-fp16.onnx";
    public static final String DEEPFAKE = "deepfake-detector/deepfake-detector-fp16.onnx";
    public static final String ACTION = "action-recognition/action-recognition-vit-fp16.onnx";
    public static final String FACE_FEATURE = "face_feature/face_feature.pt";

    /** Every Tesseract language the OCR, document and plate readers may ask for. */
    public static final List<String> TESSDATA = List.of(TESSDATA_ENG, TESSDATA_NLD, TESSDATA_FRA, TESSDATA_DEU);

    private static final List<ModelSpec> ALL = List.of(
        ModelSpec.file(TESSDATA_ENG, TESSDATA_FAST + "eng.traineddata",
            "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2", 4_113_088L),
        ModelSpec.file(TESSDATA_NLD, TESSDATA_FAST + "nld.traineddata",
            "ced0e5e046a84c908a6aa7accbef9a232c4a5d9a8276691b81c6ee64d02963f6", 6_050_296L),
        ModelSpec.file(TESSDATA_FRA, TESSDATA_FAST + "fra.traineddata",
            "ced037562e8c80c13122dece28dd477d399af80911a28791a66a63ac1e3445ca", 1_130_365L),
        ModelSpec.file(TESSDATA_DEU, TESSDATA_FAST + "deu.traineddata",
            "19d219bbb6672c869d20a9636c6816a81eb9a71796cb93ebe0cb1530e2cdb22d", 1_525_436L),
        ModelSpec.file(EMOTION, RELEASE + "emotion-ferplus-8.onnx",
            "a2a2ba6a335a3b29c21acb6272f962bd3d47f84952aaffa03b60986e04efa61c", 35_040_571L),
        ModelSpec.file(AGE, RELEASE + "age_googlenet.onnx",
            "fa2a3228e425056aa2b080b3afd3cf607327c86616e952602ed67b5fc16ab356", 23_960_165L),
        ModelSpec.file(GENDER, RELEASE + "gender_googlenet.onnx",
            "af24a4eaa9eaf70913cc9a337a0387c86f11549cbd9bbc16bffeefcdcf88cbf4", 23_935_566L),
        ModelSpec.file(PALM, RELEASE + "palm_detection_mediapipe.onnx",
            "78ff51c38496b7fc8b8ebdb6cc8c1abb02fa6c38427c6848254cdaba57fcce7c", 3_905_734L),
        ModelSpec.file(NSFW, RELEASE + "nsfw-detection-vit-fp16.onnx",
            "edf335fa01a597a65e339ca953609b3741477470e20aa3e683a0bdde104d8471", 171_801_382L),
        ModelSpec.file(DEEPFAKE, RELEASE + "deepfake-detector-fp16.onnx",
            "07eee7514086ff66fdec3e3f55155e76cdb8e00a788c185e4c95d58040811191", 171_801_382L),
        ModelSpec.file(ACTION, RELEASE + "action-recognition-vit-fp16.onnx",
            "2ec25738f4571291bc0ce6623ef4816c70d6ca3837f8cfd89b3754b39388c323", 171_833_699L),
        ModelSpec.zipEntry(FACE_FEATURE, RELEASE + "face_feature.zip",
            "07e467dbade951e1df0a2784b09acdf4a25becfb75b15ce3de6e1b2c5e98be05", 103_963_429L, 112_605_830L,
            "face_feature.pt"));

    /** Every downloadable model. */
    public static List<ModelSpec> all() {
        return ALL;
    }
}
