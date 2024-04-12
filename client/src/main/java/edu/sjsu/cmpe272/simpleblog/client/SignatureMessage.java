package edu.sjsu.cmpe272.simpleblog.client;

import lombok.Data;

@Data
class SignatureMessage {
    private final String date;
    private final String author;
    private final String message;
    private final String attachment;
}
