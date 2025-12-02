package com.edufelip.meer.web;

import com.edufelip.meer.domain.repo.SupportContactRepository;
import com.edufelip.meer.dto.SupportContactRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupportController.class)
class SupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SupportContactRepository repository;

    @Test
    void contactReturnsNoContent() throws Exception {
        var request = new SupportContactRequest("Jane Doe", "jane@example.com", "Hello there");

        mockMvc.perform(post("/support/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNoContent());

        verify(repository).save(any());
    }

    @Test
    void contactMissingFieldsReturnsBadRequestWithMessage() throws Exception {
        var request = new SupportContactRequest("", "jane@example.com", "");

        mockMvc.perform(post("/support/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Name is required"));

        verify(repository, never()).save(any());
    }
}
