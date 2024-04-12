package edu.sjsu.cmpe272.simpleblog.server.controller;

import lombok.Data;

import java.util.Date;

@Data
public class PostMessageRequest {
    private String author;
    private String attachment;
    private String signature;
    private String message;
    private String date;
}
