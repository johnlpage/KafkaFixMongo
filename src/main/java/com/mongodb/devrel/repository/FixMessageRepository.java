package com.mongodb.devrel.repository;

import com.mongodb.devrel.model.FixMessage;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FixMessageRepository
    extends MongoRepository<FixMessage, ObjectId> {
}
