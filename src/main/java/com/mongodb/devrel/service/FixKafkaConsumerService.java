package com.mongodb.devrel.service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
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
  final int BATCH_SIZE = 1000;

  private final AtomicLong lastMessageTime = new AtomicLong(System.currentTimeMillis());
  private final FixWriterService fixWriterService;
  List<String> toSave = new ArrayList<>();
  int processedCount = 0;

  @KafkaListener(topics = "fixdata", groupId = "my-group-id")
  public void listen(String message) {
    processedCount++;
    lastMessageTime.set(System.currentTimeMillis());
    if (processedCount % REPORT_AT == 0) {
      LOG.info("KAFKA Read: {}", processedCount);
    }
    try {
      toSave.add(message);
    } catch (Exception e) {
      LOG.error(e.getMessage());
    }
    if (toSave.size() >= BATCH_SIZE) {
      fixWriterService.sendBatch(List.copyOf(toSave));
      toSave.clear();
    }
  }

  @Scheduled(fixedRate = 50) // Run every 50
  public void checkForIdle() {
    long now = System.currentTimeMillis();
    long lastReceived = lastMessageTime.get();
    long idleTime = now - lastReceived;
    if (idleTime > 50) { // No messages for 50ms
      fixWriterService.sendBatch(List.copyOf(toSave));
      toSave.clear();
    }
  }

  @PreDestroy
  public void onShutdown() {
    fixWriterService.sendBatch(List.copyOf(toSave));
    toSave.clear();
    System.out.println("Kafka Listener is shutting down.");
    // Should use a Completeable future here
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    LOG.info("Processed {} docs.", processedCount);
  }
}
