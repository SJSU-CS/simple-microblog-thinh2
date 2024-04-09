package edu.sjsu.cmpe272.simpleblog.server.controller;

import lombok.Data;

@Data
public class PostMessageRequest {
    private String author;
    private String attachment;
    private String signature;
    private String message;
    //TODO: add date
}
