package edu.sjsu.cmpe272.simpleblog.server.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.List;

@Data
public class ListMessageResponse {
  @Data
  public static class MessageResponse {
    @JsonProperty("message-id")
    private final int id;

    private final String author;
    private final String message;
    private final String attachment;
    private final String signature;
    private final String date;
  }

  @JsonValue private final List<MessageResponse> responseList;
}
