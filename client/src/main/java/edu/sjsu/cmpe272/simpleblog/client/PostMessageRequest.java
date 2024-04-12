package edu.sjsu.cmpe272.simpleblog.client;

import lombok.Data;

import java.util.Date;

@Data
public class PostMessageRequest {
  private final String author;
  private final String attachment;
  private final String signature;
  private final String message;
  private final String date;
}
