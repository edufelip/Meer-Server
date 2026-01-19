package com.edufelip.meer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "moderation.nsfw")
public class ModerationProperties {

  private String modelPath;
  private boolean enabled = true;
  private Threshold threshold = new Threshold();
  private Model model = new Model();
  private Worker worker = new Worker();

  public String getModelPath() {
    return modelPath;
  }

  public void setModelPath(String modelPath) {
    this.modelPath = modelPath;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Threshold getThreshold() {
    return threshold;
  }

  public void setThreshold(Threshold threshold) {
    this.threshold = threshold;
  }

  public Model getModel() {
    return model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public Worker getWorker() {
    return worker;
  }

  public void setWorker(Worker worker) {
    this.worker = worker;
  }

  public static class Threshold {
    private double allow = 0.30;
    private double review = 0.70;

    public double getAllow() {
      return allow;
    }

    public void setAllow(double allow) {
      this.allow = allow;
    }

    public double getReview() {
      return review;
    }

    public void setReview(double review) {
      this.review = review;
    }
  }

  public static class Model {
    private int inputWidth = 224;
    private int inputHeight = 224;
    private String inputName = "pixel_values";

    public int getInputWidth() {
      return inputWidth;
    }

    public void setInputWidth(int inputWidth) {
      this.inputWidth = inputWidth;
    }

    public int getInputHeight() {
      return inputHeight;
    }

    public void setInputHeight(int inputHeight) {
      this.inputHeight = inputHeight;
    }

    public String getInputName() {
      return inputName;
    }

    public void setInputName(String inputName) {
      this.inputName = inputName;
    }
  }

  public static class Worker {
    private int queueCapacity = 1000;
    private int threadPoolSize = 2;

    public int getQueueCapacity() {
      return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
      this.queueCapacity = queueCapacity;
    }

    public int getThreadPoolSize() {
      return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
      this.threadPoolSize = threadPoolSize;
    }
  }
}
