# Spring Vision Model Module

This module provides custom model training, fine-tuning, and export capabilities using DJL (Deep Java Library).

## Overview

The model module enables you to:

- **Train custom models** from scratch using DJL's training API
- **Fine-tune pre-trained models** for specific use cases
- **Export models** to multiple formats (ONNX, TorchScript, SavedModel)
- **Create custom ModelZoo** for versioned model distribution
- **Optimize models** for production deployment

## Architecture

```
model/
├── training/          # Model training implementations
├── export/            # Model export utilities
├── zoo/              # Custom ModelZoo implementation
├── datasets/         # Dataset loaders and augmentation
└── evaluation/       # Model evaluation and metrics
```

## Quick Start

### 1. Training a Face Detection Model

```java

@Autowired
private FaceDetectionTrainer trainer;

// Configure training
TrainingConfig config = TrainingConfig.builder()
        .setEpochs(50)
        .setBatchSize(32)
        .setLearningRate(0.001f)
        .setDatasetPath("/path/to/dataset")
        .build();

// Train model
Model trainedModel = trainer.train(config);

// Export to ONNX
ModelExporter.

exportToOnnx(trainedModel, "face_detection_v1.onnx");
```

### 2. Fine-tuning a Pre-trained Model

```java
@Autowired
private ModelFineTuner fineTuner;

// Load pre-trained model from DJL ModelZoo
ZooModel<Image, DetectedObjects> pretrainedModel = 
    ModelZoo.loadModel(criteria);

// Fine-tune on custom dataset
Model fineTunedModel = fineTuner.fineTune(
    pretrainedModel,
    customDataset,
    fineTuningConfig
);
```

### 3. Creating Custom ModelZoo

```java
// Register custom models in ModelZoo
SpringVisionModelZoo.registerModel(
    "spring-vision-face-detection-v1",
            "file:///var/models/face_detection_v1.onnx",
    ModelMetadata.builder()
        .

setVersion("1.0.0")
        .

setDescription("Custom face detection model")
        .

build()
);

// Load from custom zoo
Criteria<Image, DetectedObjects> criteria = Criteria.builder()
        .setTypes(Image.class, DetectedObjects.class)
        .optArtifactId("spring-vision-face-detection-v1")
        .build();

ZooModel<Image, DetectedObjects> model = criteria.loadModel();
```

## Features

### Training

- **Transfer Learning**: Fine-tune pre-trained models
- **Custom Architectures**: Build models from scratch
- **Data Augmentation**: Built-in augmentation pipeline
- **Multi-GPU Training**: Distributed training support
- **Checkpointing**: Automatic model checkpointing
- **TensorBoard Integration**: Training visualization

### Export

- **Multiple Formats**: ONNX, TorchScript, SavedModel, CoreML
- **Optimization**: Quantization, pruning, distillation
- **Validation**: Automatic export validation
- **Metadata**: Include preprocessing info in exports

### Custom ModelZoo

- **Version Management**: Track model versions
- **Metadata Storage**: Store training info, metrics
- **Distribution**: Share models via HTTP, S3, HDFS
- **Dependency Management**: Track model dependencies

## Configuration

```properties
# Model training configuration
spring.vision.model.training.enabled=true
spring.vision.model.training.device=gpu
spring.vision.model.training.checkpoint-dir=/var/models/checkpoints

# Export configuration
spring.vision.model.export.format=onnx
spring.vision.model.export.optimize=true
spring.vision.model.export.output-dir=/var/models/exported

# Custom ModelZoo
spring.vision.model.zoo.location=file:///var/models/zoo
spring.vision.model.zoo.metadata=/var/models/zoo/metadata.json
```

## Examples

### Train a Custom Face Recognition Model

```java
import io.github.codesapienbe.springvision.model.training.FaceRecognitionTrainer;
import io.github.codesapienbe.springvision.model.datasets.FaceDataset;

// Prepare dataset
FaceDataset dataset = FaceDataset.builder()
        .setDataPath("/path/to/faces")
        .setTrainTestSplit(0.8)
        .enableAugmentation(true)
        .build();

        // Configure training
        TrainingConfig config = TrainingConfig.builder()
                .setEpochs(100)
                .setBatchSize(64)
                .setLearningRate(0.0001f)
                .setOptimizer("adam")
                .setLossFunction("triplet")
                .build();

        // Train
        FaceRecognitionTrainer trainer = new FaceRecognitionTrainer();
        Model model = trainer.train(dataset, config);

        // Evaluate
        EvaluationMetrics metrics = trainer.evaluate(model, dataset.testSet());
System.out.

        println("Accuracy: "+metrics.getAccuracy());

// Export
        ModelExporter.

        exportToOnnx(model, "face_recognition_v1.onnx");
```

### Export Optimized Model

```java
import io.github.codesapienbe.springvision.model.export.ModelExporter;
import io.github.codesapienbe.springvision.model.export.OptimizationConfig;

OptimizationConfig optConfig = OptimizationConfig.builder()
        .enableQuantization(true)
        .quantizationType(QuantizationType.INT8)
        .enablePruning(true)
        .pruningRatio(0.3f)
        .build();

ModelExporter.

exportOptimized(
        model,
    "optimized_model.onnx",
        optConfig
        );
```

## Roadmap

- [ ] Face detection model training
- [ ] Face recognition model training
- [ ] Object detection model training
- [ ] YOLO integration for training
- [ ] Model compression techniques
- [ ] AutoML for hyperparameter tuning
- [ ] Federated learning support
- [ ] Model versioning and registry
- [ ] Production deployment tools

## Requirements

- Java 21+
- DJL 0.33.0+
- CUDA 11.x+ (for GPU training)
- Minimum 8GB RAM (16GB recommended for training)
- GPU with 4GB+ VRAM (for GPU training)

## Contributing

See [CONTRIBUTING.md](../docs/contributing.md) for guidelines on adding new model architectures and training capabilities.

## License

Same as Spring Vision parent project.

