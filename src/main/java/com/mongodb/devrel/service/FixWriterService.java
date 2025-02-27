package com.mongodb.devrel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.devrel.model.FixMessage;
import com.mongodb.devrel.repository.FixMessageRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FixWriterService {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FixWriterService.class);
  private final ObjectMapper objectMapper;
  private final FixMessageRepository repository;

  // If this Fails - IT needs to DEAD.LETTER or whatever is needed as we arlready told Kafaka we
  // have it

  @Async
  public void sendBatch(List<String> fixMessages) {

    List<FixMessage> batch = new ArrayList<>();
    for (String message : fixMessages) {
      try {
        FixMessage fixMessage = objectMapper.readValue(message, FixMessage.class);
        batch.add(fixMessage);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
    }
    List<FixMessage> rval = repository.insert(batch);
    return;
  }
}
