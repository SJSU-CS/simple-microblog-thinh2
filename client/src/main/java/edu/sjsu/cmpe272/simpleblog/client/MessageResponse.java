package edu.sjsu.cmpe272.simpleblog.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MessageResponse {
  @JsonProperty("message-id")
  private int id;

  private String author;
  private String message;
  private String attachment;
  private String signature;
  private String date;
}
