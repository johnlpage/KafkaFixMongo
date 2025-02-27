package com.mongodb.devrel.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

@Configuration
public class ObjectMapperConfig {

  @Bean
  public ObjectMapper objectMapper(JsonFactory jsonFactory) {
    return new ObjectMapper(jsonFactory)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(new JavaTimeModule());
  }

  @Bean
  public JsonFactory jsonFactory() {
    return new JsonFactory();
  }

  @Bean
  public DataBufferFactory dataBufferFactory() {
    return new DefaultDataBufferFactory();
  }
}
