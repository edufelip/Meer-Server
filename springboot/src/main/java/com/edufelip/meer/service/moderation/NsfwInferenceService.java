package com.edufelip.meer.service.moderation;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.edufelip.meer.config.ModerationProperties;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for running NSFW content detection inference using ONNX Runtime. This service is
 * thread-safe as it reuses a singleton OrtSession.
 */
@Service
@ConditionalOnProperty(
    name = "moderation.nsfw.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class NsfwInferenceService {

  private static final Logger log = LoggerFactory.getLogger(NsfwInferenceService.class);

  private final OrtEnvironment env;
  private final OrtSession session;
  private final ImagePreprocessor preprocessor;
  private final ModerationProperties properties;

  public NsfwInferenceService(
      OrtEnvironment env,
      OrtSession session,
      ImagePreprocessor preprocessor,
      ModerationProperties properties) {
    this.env = env;
    this.session = session;
    this.preprocessor = preprocessor;
    this.properties = properties;
  }

  /**
   * Runs NSFW inference on a BufferedImage and returns the probability score.
   *
   * @param image The image to analyze
   * @return Probability score between 0.0 (safe) and 1.0 (NSFW)
   * @throws Exception if inference fails
   */
  public double inferNsfwProbability(BufferedImage image) throws Exception {
    int width = properties.getModel().getInputWidth();
    int height = properties.getModel().getInputHeight();

    // Preprocess image to model input format
    float[] chw = preprocessor.toNchwFloatRgb(image, width, height);
    FloatBuffer buffer = preprocessor.wrapAsBuffer(chw);

    // Get input tensor name from config or use first available
    String inputName = properties.getModel().getInputName();
    if (inputName == null || inputName.isEmpty()) {
      inputName = session.getInputNames().iterator().next();
    }

    long[] shape = new long[] {1, 3, height, width};

    try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape)) {
      Map<String, OnnxTensor> inputs = Map.of(inputName, inputTensor);

      try (OrtSession.Result result = session.run(inputs)) {
        // Extract probability from output
        return extractNsfwProbability(result);
      }
    } catch (OrtException e) {
      log.error("ONNX Runtime inference failed", e);
      throw new RuntimeException("Failed to run NSFW inference", e);
    }
  }

  /**
   * Extracts the NSFW probability from the model output. This method handles different output
   * formats: - Single logit (apply sigmoid) - Two-class logits (apply softmax and take index 1) -
   * Pre-computed probability
   */
  private double extractNsfwProbability(OrtSession.Result result) throws OrtException {
    // Get first output (most models have single output)
    String outputName = session.getOutputNames().iterator().next();
    OnnxTensor outputTensor = (OnnxTensor) result.get(outputName).orElseThrow();

    long[] shape = outputTensor.getInfo().getShape();
    double[] values = extractValues(outputTensor.getValue());

    if (values.length == 0) {
      throw new IllegalStateException("Empty output from NSFW model");
    }
    if (values.length == 1) {
      // Single logit - apply sigmoid: σ(x) = 1 / (1 + e^(-x))
      return sigmoid(values[0]);
    } else if (values.length == 2) {
      // Two-class output [safe, nsfw] - apply softmax and return NSFW probability
      double[] probabilities = softmax(values);
      return probabilities[1]; // Index 1 is NSFW class
    } else {
      // Assume first value is the probability (already normalized)
      log.warn("Unexpected output shape: {}. Using first value as probability.", shape);
      return Math.min(1.0, Math.max(0.0, values[0]));
    }
  }

  private double sigmoid(double logit) {
    return 1.0 / (1.0 + Math.exp(-logit));
  }

  private double[] softmax(double[] logits) {
    double[] exp = new double[logits.length];
    double sum = 0.0;
    double max = Double.NEGATIVE_INFINITY;

    for (double logit : logits) {
      if (logit > max) {
        max = logit;
      }
    }

    for (int i = 0; i < logits.length; i++) {
      exp[i] = Math.exp(logits[i] - max);
      sum += exp[i];
    }

    for (int i = 0; i < exp.length; i++) {
      exp[i] = sum == 0.0 ? 0.0 : exp[i] / sum;
    }

    return exp;
  }

  private double[] extractValues(Object value) {
    if (value instanceof float[] floats) {
      double[] out = new double[floats.length];
      for (int i = 0; i < floats.length; i++) {
        out[i] = floats[i];
      }
      return out;
    }
    if (value instanceof float[][] floats) {
      int total = 0;
      for (float[] row : floats) {
        total += row.length;
      }
      double[] out = new double[total];
      int idx = 0;
      for (float[] row : floats) {
        for (float v : row) {
          out[idx++] = v;
        }
      }
      return out;
    }
    if (value instanceof float[][][] floats) {
      int total = 0;
      for (float[][] mat : floats) {
        for (float[] row : mat) {
          total += row.length;
        }
      }
      double[] out = new double[total];
      int idx = 0;
      for (float[][] mat : floats) {
        for (float[] row : mat) {
          for (float v : row) {
            out[idx++] = v;
          }
        }
      }
      return out;
    }
    if (value instanceof double[] doubles) {
      return doubles;
    }
    if (value instanceof double[][] doubles) {
      int total = 0;
      for (double[] row : doubles) {
        total += row.length;
      }
      double[] out = new double[total];
      int idx = 0;
      for (double[] row : doubles) {
        for (double v : row) {
          out[idx++] = v;
        }
      }
      return out;
    }
    if (value instanceof double[][][] doubles) {
      int total = 0;
      for (double[][] mat : doubles) {
        for (double[] row : mat) {
          total += row.length;
        }
      }
      double[] out = new double[total];
      int idx = 0;
      for (double[][] mat : doubles) {
        for (double[] row : mat) {
          for (double v : row) {
            out[idx++] = v;
          }
        }
      }
      return out;
    }
    throw new IllegalStateException("Unsupported ONNX output type: " + value.getClass());
  }
}
