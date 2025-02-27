package com.mongodb.devrel.model;

import com.fasterxml.jackson.annotation.*;
import java.util.Map;
import lombok.Data;
import lombok.Singular;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/* Replace @Data with this to make an Immutable model
 * which is a little more efficient but no setters just a builder
 * This also impact the controller and fuzzer and JsonLoaderService -
 * changes there are commented
 *
 *  @Builder(toBuilder = true)
 *  @Jacksonized
 *  @Value
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "messages")
public class FixMessage {
  @Id ObjectId msgid;
  String msg;
  /**
   * Use this to capture any fields not captured explicitly As MongoDB's flexibility makes this easy
   */
  @JsonAnySetter
  @JsonAnyGetter
  @Singular("payload")
  Map<String, Object> payload;
}
