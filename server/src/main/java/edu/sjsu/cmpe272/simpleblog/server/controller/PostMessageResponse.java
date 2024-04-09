package edu.sjsu.cmpe272.simpleblog.server.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PostMessageResponse{
    @JsonProperty("message_id")
    private final long message_id;
}

