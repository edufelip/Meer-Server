package com.edufelip.meer.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.core.store.StoreFeedback;
import com.edufelip.meer.core.store.ThriftStore;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.domain.repo.StoreFeedbackRepository;
import com.edufelip.meer.domain.repo.ThriftStoreRepository;
import com.edufelip.meer.security.token.TokenProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("perf")
class PerformanceGuardrailsTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthUserRepository authUserRepository;
  @Autowired private ThriftStoreRepository thriftStoreRepository;
  @Autowired private StoreFeedbackRepository storeFeedbackRepository;
  @Autowired private TokenProvider tokenProvider;

  private String authHeader;
  private UUID storeId;

  @BeforeEach
  void setup() {
    storeFeedbackRepository.deleteAll();
    thriftStoreRepository.deleteAll();
    authUserRepository.deleteAll();

    AuthUser user = new AuthUser();
    user.setEmail("perf@example.com");
    user.setDisplayName("Perf User");
    user.setPasswordHash("hash");
    user = authUserRepository.save(user);
    authHeader = "Bearer " + tokenProvider.generateAccessToken(user);

    List<ThriftStore> stores = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      ThriftStore store = new ThriftStore();
      store.setName("Store " + i);
      store.setAddressLine("Road " + i);
      store.setLatitude(-23.0 + (i * 0.001));
      store.setLongitude(-46.0 + (i * 0.001));
      stores.add(store);
    }
    stores = thriftStoreRepository.saveAll(stores);
    storeId = stores.get(0).getId();

    List<AuthUser> raters = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      AuthUser rater = new AuthUser();
      rater.setEmail("rater" + i + "@example.com");
      rater.setDisplayName("Rater " + i);
      rater.setPasswordHash("hash");
      raters.add(rater);
    }
    raters = authUserRepository.saveAll(raters);

    List<StoreFeedback> feedbacks = new ArrayList<>();
    for (int i = 0; i < raters.size(); i++) {
      StoreFeedback feedback = new StoreFeedback(raters.get(i), stores.get(0), 4, "Nice " + i);
      feedbacks.add(feedback);
    }
    storeFeedbackRepository.saveAll(feedbacks);
  }

  @Test
  void ratingsListAverageLatencyUnderGuardrail() throws Exception {
    warmupRatings();
    int iterations = 50;
    long totalMs = 0;
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      mockMvc
          .perform(
              get("/stores/{storeId}/ratings", storeId)
                  .header("Authorization", authHeader)
                  .param("page", "1")
                  .param("pageSize", "10"))
          .andExpect(status().isOk());
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      totalMs += elapsedMs;
    }
    long avgMs = totalMs / iterations;
    assertThat(avgMs).isLessThan(500);
  }

  @Test
  void storeSearchAverageLatencyUnderGuardrail() throws Exception {
    warmupSearch();
    int iterations = 50;
    long totalMs = 0;
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      mockMvc
          .perform(
              get("/stores")
                  .header("Authorization", authHeader)
                  .param("type", "nearby")
                  .param("page", "1")
                  .param("pageSize", "10")
                  .param("lat", "-23.0")
                  .param("lng", "-46.0"))
          .andExpect(status().isOk());
      long elapsedMs = (System.nanoTime() - start) / 1_000_000;
      totalMs += elapsedMs;
    }
    long avgMs = totalMs / iterations;
    assertThat(avgMs).isLessThan(700);
  }

  private void warmupRatings() throws Exception {
    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              get("/stores/{storeId}/ratings", storeId)
                  .header("Authorization", authHeader)
                  .param("page", "1")
                  .param("pageSize", "10"))
          .andExpect(status().isOk());
    }
  }

  private void warmupSearch() throws Exception {
    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              get("/stores")
                  .header("Authorization", authHeader)
                  .param("type", "nearby")
                  .param("page", "1")
                  .param("pageSize", "10")
                  .param("lat", "-23.0")
                  .param("lng", "-46.0"))
          .andExpect(status().isOk());
    }
  }
}
