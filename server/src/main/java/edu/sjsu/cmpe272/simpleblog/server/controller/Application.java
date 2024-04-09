package edu.sjsu.cmpe272.simpleblog.server.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import edu.sjsu.cmpe272.simpleblog.server.model.MessageModel;
import edu.sjsu.cmpe272.simpleblog.server.model.UserModel;
import edu.sjsu.cmpe272.simpleblog.server.repository.MessageRepository;
import edu.sjsu.cmpe272.simpleblog.server.repository.UserRepository;
import lombok.Data;
import org.apache.catalina.User;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@Controller
public class Application {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;
    @PostMapping("/messages/create")
    public ResponseEntity<PostMessageResponse> postMessage(@RequestBody PostMessageRequest req)  {
        PostMessageResponse resp = new PostMessageResponse(1);
        MessageModel tmp = new MessageModel();
        tmp.setAttachment(req.getAttachment());
        tmp.setUsername(req.getAuthor());
        tmp.setMessage(req.getMessage());
        tmp.setSignature(req.getSignature());
        tmp = messageRepository.save(tmp);

        //TODO: implement signature verification
        return ResponseEntity.ok(new PostMessageResponse(tmp.getId()));
    }

    private boolean verifySignature(PostMessageRequest req, String publicKeyStr) {
        @Data
        class VerifyMessage {
            //private final Data date;
            private final String author;
            private final String message;
            private final String attachment;
        }

        VerifyMessage msg = new VerifyMessage(req.getAuthor(), req.getMessage(), req.getAttachment());
        String serializedMsg;
        try {
            serializedMsg = new ObjectMapper().writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            System.err.println(e);
            return false;
        }

        // SHA256 hash
        String sha256hex = Hashing.sha256().hashString(serializedMsg, StandardCharsets.UTF_8).toString();

        try {
            // Signature check
            Cipher encryptCipher = Cipher.getInstance("RSA");
            PublicKey pKey = strToPublicKey(publicKeyStr);
            encryptCipher.init(Cipher.ENCRYPT_MODE, pKey);
            byte[] secretMessageBytes = req.getSignature().getBytes(StandardCharsets.UTF_8);)
            byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);
            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);

            return sha256hex.equals(encodedMessage);
        } catch (Exception e) {
            System.err.println(e);
            return false;
        }

    }

    private PublicKey strToPublicKey(String publicKeyStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        return pubKey;
    }

    @PostMapping("/messages/list")
    public ResponseEntity<ListMessageResponse> listMessage(@RequestBody ListMessageRequest req) {
        List<ListMessageResponse.MessageResponse> result = new ArrayList<>();

        // validate request
        /*if (req.getLimit() > 10) {

        }*/

        if (req.getNext() == -1) {
            //req.getNext
        }

        LongStream.rangeClosed(req.getNext() - req.getLimit() + 1, req.getNext()).forEach(id -> {
            messageRepository.findById(id).ifPresent(message -> {
                result.add(new ListMessageResponse.MessageResponse(
                        (int) message.getId(),
                        message.getUsername(),
                        message.getMessage(),
                        message.getAttachment(),
                        message.getSignature()
                ));

            });
        });

        return ResponseEntity.ok(new ListMessageResponse(result));
    }

    @PostMapping("/user/create")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest req) {
        UserModel record = new UserModel();
        record.setPublickey(req.getPublickey());
        record.setUsername(req.getUsername());
        userRepository.save(record);

        return ResponseEntity.ok(new CreateUserResponse("welcome"));
    }

    @GetMapping("/user/{username}/public-key")
    public ResponseEntity<String> getPublicKey(@PathVariable("username") String username) {
        ResponseEntity<String> resp = ResponseEntity.ok("not found");
        Optional<UserModel> tmp = userRepository.findByUsername(username);
        if (tmp.isPresent()) {
            return ResponseEntity.ok(tmp.get().getPublickey());
        }
        return ResponseEntity.ok("not found");
    }
}
