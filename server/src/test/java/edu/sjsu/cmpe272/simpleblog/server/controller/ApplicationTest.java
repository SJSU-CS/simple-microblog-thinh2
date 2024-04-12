package edu.sjsu.cmpe272.simpleblog.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.security.*;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SignatureTest {

  private KeyPair keyPair;

  @BeforeEach
  void setup() {
    KeyPairGenerator generator = null;
    try {
      generator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    generator.initialize(2048);
    keyPair = generator.generateKeyPair();
  }

  @Test
  public void testSignatureCheck() throws Exception {
    String dateStr = (new Date()).toString();
    VerifyMessage message =
        new VerifyMessage(dateStr, "test-author", "test-message", "test-attachment");
    String serializedMsg = null;
    try {
      serializedMsg = new ObjectMapper().writeValueAsString(message);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    // sign message
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(keyPair.getPrivate());
    signature.update(serializedMsg.getBytes());
    String signatureStr = new String(Base64.getEncoder().encode(signature.sign()));

    StringWriter stringWriter = new StringWriter();
    try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter)) {
      jcaPEMWriter.writeObject(keyPair.getPublic());
    }
    String pemStr = stringWriter.toString();

    PostMessageRequest req = new PostMessageRequest();
    req.setSignature(signatureStr);
    req.setAuthor(message.getAuthor());
    req.setAttachment(message.getAttachment());
    req.setMessage(message.getMessage());
    req.setDate(dateStr);
    assertEquals(Application.signatureMatched(req, pemStr), true);
  }
}
