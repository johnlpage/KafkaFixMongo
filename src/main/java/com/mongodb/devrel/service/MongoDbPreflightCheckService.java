package com.mongodb.devrel.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

/**
 * This is an example of a class you can use to ensure the system is ready to start correctly. It
 * shows you how to check or indexes etc. And optionally create them - although spring has similar
 * lifecycle methods they aren't comprehensive enough. It uses ApplicationRunner to ensure this is
 * run on startup.
 */
@Service
public class MongoDbPreflightCheckService {
  private static final Logger LOG = LoggerFactory.getLogger(MongoDbPreflightCheckService.class);

  /**
   * Defining this a JSON makes it easy to read but still hard coded. todo - create Atlas Search
   * indexes todo - create Atlas Vector Indexes todo - extend this to include schema validation
   * check.
   */
  private static final String SCHEMA_AND_INDEXES =
      """
  {
      "collections" : [
        { "name" : "messages" ,
          "indexes": [ ],
         }
      ]
  }
  """;

  private final ApplicationContext context;
  private final MongoTemplate mongoTemplate;

  @Value("${mews.preflight.createRequiredIndexes:true}")
  private boolean createRequiredIndexes;

  @Value("${mews.preflight.createRequiredIndexes:true}")
  private boolean createCollections;

  public MongoDbPreflightCheckService(ApplicationContext context, MongoTemplate mongoTemplate) {
    this.context = context;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Ensure all Collections exist, create them or quit depending on flag.
   *
   * <p>todo - ensure that have required properties like timeseries or validation using
   * getCollectionInfos
   */
  List<Document> ensureCollectionsExist(Document schemaAndIndexes) {
    MongoDatabase database = mongoTemplate.getDb();

    List<String> existingCollections = database.listCollectionNames().into(new ArrayList<>());
    List<Document> requiredCollections = schemaAndIndexes.getList("collections", Document.class);

    for (Document requiredCollection : requiredCollections) {
      String collectionName = requiredCollection.getString("name");
      if (!existingCollections.contains(collectionName)) {
        if (createCollections) {
          LOG.warn("Collection '{}' does not exist, creating it.", collectionName);
          database.createCollection(collectionName);
        } else {
          LOG.error("Collection '{}' does not exist, cancelling startup", collectionName);
          int exitCode = SpringApplication.exit(context, () -> 0);
          System.exit(exitCode);
        }
      }
    }
    return requiredCollections;
  }

  /** Ensure all required indexes exist */
  void ensureRequiredIndexesExist(List<Document> requiredInfo) {
    for (Document requiredCollection : requiredInfo) {

      String collectionName = requiredCollection.getString("name");
      MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);

      List<Document> requiredIndexes =
          requiredCollection.getList("indexes", Document.class, List.of());
      if (requiredIndexes.isEmpty()) {
        continue;
      }
      List<String> existingIndexes =
          collection
              .listIndexes()
              .map(index -> index.get("key", Document.class).toJson())
              .into(new ArrayList<>());

      for (Document index : requiredIndexes) {
        if (existingIndexes.contains(index.toJson())) {
          continue;
        }
        if (createRequiredIndexes) {
          LOG.warn("Index '{}' does not exist, creating required index", index.toJson());
          collection.createIndex(index);
        } else {
          LOG.error(
              "Collection '{}' does not have index {}, cancelling startup",
              collectionName,
              index.toJson());
          SpringApplication.exit(context, () -> 0);
          return;
        }
      }
    }
  }


  @Bean
  public ApplicationRunner mongoPreflightCheck() {
    return args -> {
      LOG.info("PREFLIGHT CHECK");
      if (createRequiredIndexes) {
        LOG.warn(
            "THIS IS CONFIGURED TO AUTOMATICALLY CREATE INDEXES - THIS IS NOT RECOMMENDED IN PRODUCTION");
      }

      Document schemaAndIndexes = Document.parse(SCHEMA_AND_INDEXES);
      List<Document> requiredInfo = ensureCollectionsExist(schemaAndIndexes);
      ensureRequiredIndexesExist(requiredInfo);
      LOG.info("PREFLIGHT CHECK COMPLETE");
    };
  }

  /** Create an Atlas Search Index from a String definition */
  void createSearchIndex(String collection, String name, Document definition) {
    Document createSearchIndexCommand =
        new Document("createSearchIndexes", collection)
            .append(
                "indexes",
                Collections.singletonList(
                    new Document("name", name)
                        .append("type", "search")
                        .append("definition", definition)));

    // Run the command
    MongoDatabase database = mongoTemplate.getDb();
    database.runCommand(createSearchIndexCommand);
  }
}
