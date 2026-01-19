package com.edufelip.meer.service.moderation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

@Service
public class ImagePreprocessor {

  // Typical ImageNet normalization - adjust if model expects different values
  private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
  private static final float[] STD = {0.229f, 0.224f, 0.225f};

  /**
   * Preprocesses an image to NCHW format (batch, channels, height, width) Expected by most vision
   * transformers: - Resize to target dimensions - Convert to RGB - Normalize with mean/std - Return
   * as float array in channel-first order
   *
   * @param input Source image
   * @param width Target width
   * @param height Target height
   * @return Float array in NCHW format [1, 3, height, width]
   */
  public float[] toNchwFloatRgb(BufferedImage input, int width, int height) throws IOException {
    // Resize image to model input size
    BufferedImage resized = Thumbnails.of(input).size(width, height).asBufferedImage();

    int hw = width * height;
    float[] chw = new float[3 * hw];

    // Convert to NCHW: separate channels (R, G, B) in contiguous blocks
    int idx = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = resized.getRGB(x, y);

        // Extract RGB channels (0-255) and normalize to [0, 1]
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        int i = idx++;

        // Apply ImageNet normalization: (value - mean) / std
        chw[i] = (r - MEAN[0]) / STD[0]; // Red channel
        chw[hw + i] = (g - MEAN[1]) / STD[1]; // Green channel
        chw[2 * hw + i] = (b - MEAN[2]) / STD[2]; // Blue channel
      }
    }
    return chw;
  }

  /**
   * Wraps the float array in a FloatBuffer for ONNX Runtime
   *
   * @param chw Float array in NCHW format
   * @return FloatBuffer ready for OnnxTensor creation
   */
  public FloatBuffer wrapAsBuffer(float[] chw) {
    return FloatBuffer.wrap(chw);
  }
}
