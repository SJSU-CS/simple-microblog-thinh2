package edu.sjsu.cmpe272.simpleblog.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.message.Message;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import org.ini4j.Ini;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import reactor.core.publisher.Mono;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@Command
public class ClientApplication implements CommandLineRunner, ExitCodeGenerator {

  private static String remoteServer = "https://thinhproject.hopto.org/";
  private static String localServer = "http://127.0.0.1:8080";
  private static WebClient webClient = WebClient.create(remoteServer);

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");

  @Autowired CommandLine.IFactory iFactory;

  @Autowired private ConfigurableApplicationContext context;

  static String KeyFilePath = "mb.ini";
  static Logger logger = LoggerFactory.getLogger(ClientApplication.class);

  @Command
  public int post(
      @Parameters String message, @Parameters(defaultValue = "null") String attachment) {

    String encodedAttachment = "null";
    if (!attachment.equals("null")) {
      try {
        encodedAttachment = Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(attachment)));
      } catch (Exception e) {
        System.out.println(e.getCause());
        logger.error(e.toString());
        return 2;
      }
    }

    try {
      PrivateKey privateKey = readPrivateKeyFromIni();
      String author = readAuthorFromIni();

      String date = dateFormat.format(new Date());
      logger.info(date);
      SignatureMessage signatureMessage = new SignatureMessage(date, author, message, encodedAttachment);
      String signature = signMessage(signatureMessage, privateKey);

      PostMessageRequest req = new PostMessageRequest(author, encodedAttachment, signature, message, date);

      Mono<ResponseEntity<String>> mono = webClient.post().uri("/messages/create").bodyValue(req).retrieve().toEntity(String.class);
      String response = mono.block().getBody();
      System.out.println(response);
      return 0;
    } catch (WebClientResponseException e) {
      System.out.println(e.getResponseBodyAsString());
      return 2;
    } catch (Exception e) {
      System.err.println(e);
      return 2;
    }
  }

  private String signMessage(SignatureMessage message, PrivateKey privateKey) throws Exception {
    String serializedMsg = new ObjectMapper().writeValueAsString(message);
    logger.debug(serializedMsg);

    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initSign(privateKey);
    signature.update(serializedMsg.getBytes());
    String signatureStr = new String(Base64.getEncoder().encode(signature.sign()));
    logger.debug(signatureStr);

    return signatureStr;
  }

  private static int DEFAULT_START = -1;
  private static int DEFAULT_COUNT = 10;
  private static int LIST_BATCH = 10;
  @Command
  public int list(
          @CommandLine.Option(names = "--starting") int start,
          @CommandLine.Option(names = "--count") int count,
          @CommandLine.Option(names = "--save-attachment") boolean saveAttachment) {

    if (start == 0) start = DEFAULT_START;
    if (count == 0) count = DEFAULT_COUNT;

    ListMessageRequest req = new ListMessageRequest();
    req.setNext(start);
    req.setLimit(count);

    logger.debug(String.format("list request: start %d, limit %d", start, count));

    Mono<List<MessageResponse>> mono = webClient.post().uri("messages/list").bodyValue(req).retrieve().bodyToMono(new ParameterizedTypeReference<List<MessageResponse>>() {});
    List<MessageResponse> resp = mono.block();

    for (MessageResponse msg: resp) {
      if (!saveAttachment || (saveAttachment && msg.getAttachment().equals("null"))) {
        System.out.println(String.format("%s: %s %s says \"%s\"", msg.getId(), msg.getDate(), msg.getAuthor(), msg.getMessage()));
        continue;
      }
      if (saveAttachment && !msg.getAttachment().equals("null")) {
        String decodedString = new String(Base64.getDecoder().decode(msg.getAttachment()));
        try {
          FileWriter writer = new FileWriter(String.format("%d.out", msg.getId()));
          writer.write(decodedString);
          writer.close();
        } catch (IOException e) {
          System.out.printf("failed to create attachment file %s", e.getCause());
          return 2;
        }

        System.out.println(String.format("%s: %s %s says \"%s\" \uD83D\uDCCE ", msg.getId(), msg.getDate(), msg.getAuthor(), msg.getMessage()));
      }
    }
    return 0;
  }

  @Command
  int create(@Parameters String id) {
    KeyPairGenerator generator = null;
    KeyPair keyPair;
    try {
      generator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    generator.initialize(2048);
    keyPair = generator.generateKeyPair();
    String pemPublicKey = null;

    try {
      pemPublicKey = keyToPEM(keyPair.getPublic());
      String pemPrivateKey = keyToPEM(keyPair.getPrivate());
      writeKeyToIni(id, pemPrivateKey, pemPublicKey);

    } catch (Exception e) {
      System.err.println(e);
      return 2;
    }

    CreateUserRequest req = new CreateUserRequest();
    req.setPublickey(pemPublicKey);
    req.setUsername(id);

    logger.info(String.format("create user %s", id));
    try {
      Mono<ResponseEntity<String>> mono = webClient.post().uri("/user/create").bodyValue(req).retrieve().toEntity(String.class);
      String response = mono.block().getBody();
      System.out.println(response);
      return 0;
    } catch (WebClientResponseException e) {
      System.out.println(e.getResponseBodyAsString());
      return 2;
    }
  }

  private String keyToPEM(Key obj) throws Exception {
    StringWriter stringWriter = new StringWriter();
    try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter)) {
      jcaPEMWriter.writeObject(obj);
    }
    return stringWriter.toString();
  }

  private PrivateKey pemToKey(String pemEncoded) throws IOException {
    PEMParser pemParser = new PEMParser(new StringReader(pemEncoded));
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
    Object object = pemParser.readObject();
    if (object instanceof PEMKeyPair) {
      KeyPair keyPair = converter.getKeyPair((PEMKeyPair) object);
      return keyPair.getPrivate();
    }
    throw new IllegalArgumentException("Invalid PEM encoded private key");
  }

  private void writeKeyToIni(String id, String privateKey, String publicKey) throws Exception {
    Path path = Paths.get(KeyFilePath);
    if (Files.notExists(path)) {
      Files.createFile(path);
    }

    Ini ini = new Ini(new File(KeyFilePath));
    ini.put(id, "private-key", privateKey);
    ini.put(id, "public-key", publicKey);
    ini.store();
  }

  private PrivateKey readPrivateKeyFromIni() throws IOException {
    Ini ini = new Ini(new File(KeyFilePath));
    for (Profile.Section section : ini.values()) {
      logger.info(section.getName());
      return pemToKey(section.get("private-key"));
    }

    return null;
  }

  private String readAuthorFromIni() throws IOException {
    File fileToParse = new File(KeyFilePath);

    Ini ini = new Ini(fileToParse);
    for (Profile.Section section : ini.values()) {
      return section.getName();
    }

    return null;
  }
  public static void main(String[] args) {
    SpringApplication.run(ClientApplication.class, args);
  }

  int exitCode;

  @Override
  public void run(String... args) throws Exception {
    exitCode = new CommandLine(this, iFactory).execute(args);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}
