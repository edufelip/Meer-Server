package com.edufelip.meer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edufelip.meer.config.TestClockConfig;
import com.edufelip.meer.core.support.SupportContact;
import com.edufelip.meer.domain.port.RateLimitPort;
import com.edufelip.meer.domain.repo.SupportContactRepository;
import com.edufelip.meer.dto.SupportContactRequest;
import com.edufelip.meer.security.SanitizingJacksonModuleConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupportController.class)
@Import({SanitizingJacksonModuleConfig.class, TestClockConfig.class})
class SupportControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private SupportContactRepository repository;
  @MockitoBean private RateLimitPort rateLimitService;

  @Test
  void contactReturnsNoContent() throws Exception {
    var request = new SupportContactRequest("Jane Doe", "jane@example.com", "Hello there");

    org.mockito.Mockito.when(
            rateLimitService.allowSupportContact(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);

    mockMvc
        .perform(
            post("/support/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isNoContent());

    verify(repository).save(any());
  }

  @Test
  void contactMissingFieldsReturnsBadRequestWithMessage() throws Exception {
    var request = new SupportContactRequest("", "jane@example.com", "");

    org.mockito.Mockito.when(
            rateLimitService.allowSupportContact(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);

    mockMvc
        .perform(
            post("/support/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Name is required"));

    verify(repository, never()).save(any());
  }

  @Test
  void contactStripsHtmlAndSavesCleanedValues() throws Exception {
    var request =
        new SupportContactRequest(
            "<b>Jane Doe</b>", "bad@example.com", "<b>Hello</b> <img src=x onerror=1>");

    org.mockito.Mockito.when(
            rateLimitService.allowSupportContact(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(true);

    mockMvc
        .perform(
            post("/support/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isNoContent());

    ArgumentCaptor<SupportContact> captor = ArgumentCaptor.forClass(SupportContact.class);
    verify(repository).save(captor.capture());
    SupportContact saved = captor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals("Jane Doe", saved.getName());
    org.junit.jupiter.api.Assertions.assertEquals("bad@example.com", saved.getEmail());
    org.junit.jupiter.api.Assertions.assertEquals("Hello", saved.getMessage());
  }

  @Test
  void contactRateLimitedReturnsTooManyRequests() throws Exception {
    var request = new SupportContactRequest("Jane Doe", "jane@example.com", "Help");
    org.mockito.Mockito.when(rateLimitService.allowSupportContact(any())).thenReturn(false);

    mockMvc
        .perform(
            post("/support/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isTooManyRequests());

    verify(repository, never()).save(any());
  }
}
