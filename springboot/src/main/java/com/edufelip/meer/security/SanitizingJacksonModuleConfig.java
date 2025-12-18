package com.edufelip.meer.security;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class SanitizingJacksonModuleConfig {

    private static final int MAX_INCOMING_STRING_LENGTH = 2048;

    @Bean
    public JsonMapperBuilderCustomizer sanitizingStringModule() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(String.class, new SanitizingStringDeserializer(MAX_INCOMING_STRING_LENGTH));
            builder.addModule(module);
        };
    }
}
