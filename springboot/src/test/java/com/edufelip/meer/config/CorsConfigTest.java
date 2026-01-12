package com.edufelip.meer.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "meer.cors.allowed-origins=https://*.guiabrecho.com.br")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CorsConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void preflightAllowsWildcardSubdomainOrigins() throws Exception {
    mockMvc
        .perform(
            options("/dashboard/login")
                .header("Origin", "https://dev.guiabrecho.com.br")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://dev.guiabrecho.com.br"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
  }
}
