package com.mongodb.devrel.model;

import com.fasterxml.jackson.annotation.*;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@Document(collection = "messages")
public class FixMessage {
  /**
   * Use this to capture any fields not captured explicitly As MongoDB's flexibility makes this easy
   */
  @JsonAnySetter
  @JsonAnyGetter
  @Singular("payload")
  Map<String, Object> payload;
  @Id private ObjectId msgid;
  private String msg;
}
