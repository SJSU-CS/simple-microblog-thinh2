package edu.sjsu.cmpe272.simpleblog.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateUserRequest {
    @JsonProperty("user")
    private String username;

    @JsonProperty("public-key")
    private String publickey;
}
