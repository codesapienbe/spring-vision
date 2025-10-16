/**
 * Model training, export, and custom ModelZoo implementation for Spring Vision.
 *
 * <p>This package provides capabilities for:</p>
 * <ul>
 *   <li>Training custom models using DJL</li>
 *   <li>Fine-tuning pre-trained models</li>
 *   <li>Exporting models to production formats</li>
 *   <li>Managing custom ModelZoo repositories</li>
 * </ul>
 *
 * <h2>Training Example</h2>
 * <pre>{@code
 * TrainingConfig config = TrainingConfig.builder()
 *     .setEpochs(50)
 *     .setBatchSize(32)
 *     .setLearningRate(0.001f)
 *     .build();
 * }</pre>
 *
 * @since 1.0.5
 */
package io.github.codesapienbe.springvision.model.training;

