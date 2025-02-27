package com.mongodb.devrel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.devrel.model.FixMessage;
import com.mongodb.devrel.repository.FixMessageRepository;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/* This is unusual in that we want to read from the Topic but if we run out of
items on it then we want to send the batch we are working on , this code is simply to show how
a Kafka consumer can be used instead of a web service - it's the same code as the JSONLoader*/

@RequiredArgsConstructor
@Service
public class FixKafkaConsumerService {
  private static final org.slf4j.Logger LOG =
      LoggerFactory.getLogger(FixKafkaConsumerService.class);
  final int REPORT_AT = 10000;
  final int BATCH_SIZE = 2000;
  private final FixMessageRepository repository;
  private final ObjectMapper objectMapper;
  private final AtomicLong lastMessageTime = new AtomicLong(System.currentTimeMillis());
  List<FixMessage> toSave = new ArrayList<>();
  List<CompletableFuture<BulkWriteResult>> futures = new ArrayList<>();
  int processedCount = 0;

  @KafkaListener(topics = "fixdata", groupId = "my-group-id")
  public void listen(String message) {
    processedCount++;
    lastMessageTime.set(System.currentTimeMillis());
    if (processedCount % REPORT_AT == 0) {
      LOG.info("KAFKA Read: {}", processedCount);
    }

    try {
      FixMessage document = objectMapper.readValue(message, FixMessage.class);
      toSave.add(document);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    if (toSave.size() >= BATCH_SIZE) {
      sendBatch();
    }
  }

  @Async
  public CompletableFuture<List<FixMessage>> sendBatch() {
    List<FixMessage> copyOfToSave = List.copyOf(toSave);
    toSave.clear();
    List<FixMessage> rval = repository.insert(copyOfToSave);
    return CompletableFuture.completedFuture(rval);
  }

  @Scheduled(fixedRate = 50) // Run every 50
  public void checkForIdle() {
    long now = System.currentTimeMillis();
    long lastReceived = lastMessageTime.get();
    long idleTime = now - lastReceived;
    if (idleTime > 50) { // No messages for 50ms
      sendBatch();
    }
  }

  @PreDestroy
  public void onShutdown() {
    sendBatch();
    System.out.println("Kafka Listener is shutting down.");
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    // Wait for all futures to complete
    allFutures.join();
    LOG.info("Processed {} docs.", processedCount);
  }
}
