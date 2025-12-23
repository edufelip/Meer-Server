package com.edufelip.meer.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.core.auth.AuthUser;
import com.edufelip.meer.domain.repo.AuthUserRepository;
import com.edufelip.meer.security.token.TokenProvider;
import com.edufelip.meer.support.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StoreRatingsWorkflowTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthUserRepository authUserRepository;
  @Autowired private TokenProvider tokenProvider;

  @Test
  void signupCreateStoreFeedbackListAndDeleteFlow() throws Exception {
    AuthUser user = authUserRepository.save(TestFixtures.user("flow@example.com", "Flow User"));

    String token = tokenProvider.generateAccessToken(user);

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    Map<String, Object> storeRequest =
        Map.of(
            "name",
            "Flow Store",
            "description",
            "Nice place",
            "addressLine",
            "123 Road",
            "phone",
            "555-123",
            "latitude",
            -23.0,
            "longitude",
            -46.0);

    var createResult =
        mockMvc
            .perform(
                post("/stores")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(storeRequest)))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
    UUID storeId = UUID.fromString(createJson.get("id").asText());
    assertThat(storeId).isNotNull();

    mockMvc
        .perform(
            post("/stores/{storeId}/feedback", storeId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":5,\"body\":\"Great\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(5));

    mockMvc
        .perform(
            get("/stores/{storeId}/ratings", storeId)
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].storeId").value(storeId.toString()))
        .andExpect(jsonPath("$.items[0].score").value(5))
        .andExpect(jsonPath("$.items[0].body").value("Great"));

    mockMvc
        .perform(
            delete("/stores/{storeId}/feedback", storeId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/stores/{storeId}/ratings", storeId)
                .header("Authorization", "Bearer " + token)
                .param("page", "1")
                .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items").isEmpty());
  }
}
