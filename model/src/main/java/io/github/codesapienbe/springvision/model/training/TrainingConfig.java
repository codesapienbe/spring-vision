package io.github.codesapienbe.springvision.model.training;

/**
 * Configuration for model training.
 *
 * @author Spring Vision Team
 * @since 1.0.5
 */
public class TrainingConfig {

    private int epochs = 50;
    private int batchSize = 32;
    private float learningRate = 0.001f;
    private String optimizer = "adam";
    private String lossFunction = "cross_entropy";
    private String datasetPath;
    private boolean useGpu = false;
    private int numWorkers = 4;
    private String checkpointDir;
    private int saveCheckpointEvery = 10;

    private TrainingConfig() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getEpochs() {
        return epochs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public float getLearningRate() {
        return learningRate;
    }

    public String getOptimizer() {
        return optimizer;
    }

    public String getLossFunction() {
        return lossFunction;
    }

    public String getDatasetPath() {
        return datasetPath;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    public int getNumWorkers() {
        return numWorkers;
    }

    public String getCheckpointDir() {
        return checkpointDir;
    }

    public int getSaveCheckpointEvery() {
        return saveCheckpointEvery;
    }

    public static class Builder {
        private final TrainingConfig config = new TrainingConfig();

        public Builder setEpochs(int epochs) {
            config.epochs = epochs;
            return this;
        }

        public Builder setBatchSize(int batchSize) {
            config.batchSize = batchSize;
            return this;
        }

        public Builder setLearningRate(float learningRate) {
            config.learningRate = learningRate;
            return this;
        }

        public Builder setOptimizer(String optimizer) {
            config.optimizer = optimizer;
            return this;
        }

        public Builder setLossFunction(String lossFunction) {
            config.lossFunction = lossFunction;
            return this;
        }

        public Builder setDatasetPath(String datasetPath) {
            config.datasetPath = datasetPath;
            return this;
        }

        public Builder setUseGpu(boolean useGpu) {
            config.useGpu = useGpu;
            return this;
        }

        public Builder setNumWorkers(int numWorkers) {
            config.numWorkers = numWorkers;
            return this;
        }

        public Builder setCheckpointDir(String checkpointDir) {
            config.checkpointDir = checkpointDir;
            return this;
        }

        public Builder setSaveCheckpointEvery(int saveCheckpointEvery) {
            config.saveCheckpointEvery = saveCheckpointEvery;
            return this;
        }

        public TrainingConfig build() {
            return config;
        }
    }
}

