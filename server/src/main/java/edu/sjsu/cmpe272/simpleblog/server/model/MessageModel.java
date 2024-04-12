package edu.sjsu.cmpe272.simpleblog.server.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "MESSAGE_TEST")
@Getter
@Setter
public class MessageModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String date;
    private String message;
    private String attachment;
    private String signature;

    private String username;
}
