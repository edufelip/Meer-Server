package com.edufelip.meer.security;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizingStringDeserializerTest {

    private ObjectMapper mapper(int maxLen) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new SanitizingStringDeserializer(maxLen));
        return new ObjectMapper().rebuild().addModule(module).build();
    }

    record Payload(String value) {}

    @Test
    void stripsHtmlTagsAndScripts() throws Exception {
        ObjectMapper m = mapper(2048);
        String json = "{\"value\":\"<script>alert(1)</script> hello\"}";
        Payload p = m.readValue(json, Payload.class);
        assertThat(p.value()).isEqualTo("hello");
    }

    @Test
    void enforcesMaxLength() throws Exception {
        ObjectMapper m = mapper(5);
        String json = "{\"value\":\"0123456789\"}";
        Payload p = m.readValue(json, Payload.class);
        assertThat(p.value()).isEqualTo("01234");
    }
}
