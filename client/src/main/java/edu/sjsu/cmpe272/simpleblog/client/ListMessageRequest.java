package edu.sjsu.cmpe272.simpleblog.client;

import lombok.Data;

@Data
public class ListMessageRequest {
    private int next;
    private int limit; // raise error if limit greater than 20;
}
