package edu.sjsu.cmpe272.simpleblog.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sjsu.cmpe272.simpleblog.server.model.MessageModel;
import edu.sjsu.cmpe272.simpleblog.server.model.UserModel;
import edu.sjsu.cmpe272.simpleblog.server.repository.MessageRepository;
import edu.sjsu.cmpe272.simpleblog.server.repository.UserRepository;
import lombok.Data;

import java.io.StringReader;
import java.security.Signature;
import java.util.Base64;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

@Controller
public class Application {

  @Autowired private MessageRepository messageRepository;

  @Autowired private UserRepository userRepository;

  static Logger logger = LoggerFactory.getLogger(Application.class);

  @PostMapping("/messages/create")
  public ResponseEntity postMessage(@RequestBody PostMessageRequest req) {
    Optional<UserModel> user = userRepository.findByUsername(req.getAuthor());
    if (!user.isPresent()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ErrorMessage("user does not existed"));
    }

    // Signature verification
    if (!signatureMatched(req, user.get().getPublickey())) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ErrorMessage("signature didn't match"));
    }

    MessageModel messageModel = new MessageModel();
    messageModel.setDate(req.getDate());
    messageModel.setAttachment(req.getAttachment());
    messageModel.setUsername(req.getAuthor());
    messageModel.setMessage(req.getMessage());
    messageModel.setSignature(req.getSignature());
    messageModel = messageRepository.save(messageModel);
    return ResponseEntity.ok(new PostMessageResponse(messageModel.getId()));
  }

  static boolean signatureMatched(PostMessageRequest req, String publicKeyStr) {
    VerifyMessage msg = new VerifyMessage(req.getDate(), req.getAuthor(), req.getMessage(), req.getAttachment());
    String serializedMsg;
    try {
      serializedMsg = new ObjectMapper().writeValueAsString(msg);
      logger.debug(String.format("[verify-signature] serialized object %s", serializedMsg));
    } catch (JsonProcessingException e) {
      logger.error(e.toString());
      return false;
    }

    try {
      // Signature check
      PublicKey publicKey = pemToPublicKey(publicKeyStr);
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initVerify(publicKey);
      signature.update(serializedMsg.getBytes());
      return signature.verify(Base64.getDecoder().decode(req.getSignature()));
    } catch (Exception e) {
      logger.error(e.toString());
      return false;
    }
  }

  private static PublicKey pemToPublicKey(String pemStr)
      throws Exception {
    PEMParser parser = new PEMParser(new StringReader(pemStr));
    Object obj = parser.readObject();

    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
    return converter.getPublicKey(SubjectPublicKeyInfo.getInstance(obj));
  }

  @PostMapping("/messages/list")
  public ResponseEntity listMessage(@RequestBody ListMessageRequest req) {
    List<ListMessageResponse.MessageResponse> result = new ArrayList<>();

    // validate request
    if (req.getLimit() > 20) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage("limit exceed"));
    }

    int start = req.getNext();
    int limit = req.getLimit();
    if (start == -1) {
      start = Math.toIntExact(messageRepository.findFirstByOrderByIdDesc().getId());
    }
    logger.info(String.format("[message-list] from %d, limit %d", start, limit));

    LongStream.rangeClosed(start - limit + 1, start)
        .forEach(
            id -> {
              messageRepository
                  .findById(id)
                  .ifPresent(
                      message -> {
                        result.add(
                            new ListMessageResponse.MessageResponse(
                                (int) message.getId(),
                                message.getUsername(),
                                message.getMessage(),
                                message.getAttachment(),
                                message.getSignature(), message.getDate()));
                      });
            });

    return ResponseEntity.ok(new ListMessageResponse(result));
  }

  @PostMapping("/user/create")
  public ResponseEntity createUser(@RequestBody CreateUserRequest req) {
    if (userRepository.findByUsername(req.getUsername()).isPresent()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage("user existed"));
    }

    logger.debug("create user ", req.getUsername());
    UserModel record = new UserModel();
    record.setPublickey(req.getPublickey());
    record.setUsername(req.getUsername());
    try {
      userRepository.save(record);
      return ResponseEntity.ok(new CreateUserResponse("welcome"));
    } catch (DataAccessException e) {
      return ResponseEntity.internalServerError().body(new ErrorMessage("failed to create user"));
    }
  }

  @GetMapping("/user/{username}/public-key")
  public ResponseEntity getPublicKey(@PathVariable("username") String username) {
    Optional<UserModel> tmp = userRepository.findByUsername(username);
    if (tmp.isPresent()) {
      return ResponseEntity.ok(tmp.get().getPublickey());
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorMessage("user does not exist"));
  }
}
