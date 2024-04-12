package edu.sjsu.cmpe272.simpleblog.server.controller;

import lombok.Data;

import java.util.Date;

@Data
class VerifyMessage {
    private final String date;
    private final String author;
    private final String message;
    private final String attachment;
}
