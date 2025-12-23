package com.edufelip.meer.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class TestClockConfig {

  public static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

  @Bean
  @Primary
  public Clock testClock() {
    return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
  }
}
