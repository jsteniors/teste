package br.com.teste;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.*;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static br.com.teste.EncodeUtil.encodeJwt;

@SpringBootApplication
@RestController
public class App {

    private final ObjectMapper mapper;
    private final TokenUtil tokenUtil;

    public App(ObjectMapper mapper, TokenUtil tokenUtil){
        this.mapper = mapper;
        this.tokenUtil = tokenUtil;
    }

    public static void main(String[] args) throws IOException {
        SpringApplication.run(App.class, args);

    }

    @PostMapping("/generate")
    public ResponseEntity<String> foo(@RequestBody User user) throws JsonProcessingException {

        var payloadBytes = mapper.writeValueAsBytes(user);
        var headersBystes = mapper.writeValueAsBytes(new Header(SIGNING_ALGORITHM.toString()));

        var payloadBase64Bytes = encodeJwt(payloadBytes);
        var headerBase64Bytes = encodeJwt(headersBystes);

        ByteBuffer messageByteBuffer = this.tokenUtil.encodeData(user, new Header(SIGNING_ALGORITHM.toString()));

        var kmsClient = getKmsClient();

        SignRequest request = new SignRequest()
                .withKeyId(KMS_KEY_ID)
                .withSigningAlgorithm(SIGNING_ALGORITHM)
                .withMessage(messageByteBuffer);

        var response = kmsClient.sign(request);
        var signature = encodeJwt(response.getSignature().array());

        var token = headerBase64Bytes
                    .concat(".")
                .concat(payloadBase64Bytes)
                .concat(".")
                .concat(signature);


        return ResponseEntity.ok(token);
    }

    private static AWSKMS getKmsClient() {
        var credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials("AKIARQ3BJQFZSLNGJD7L", "8alu+giKBjR4LqzZyRzoG5KOVuL+O0c76rwA6Dua"));

        var kmsClient = AWSKMSClient.builder()
                .withCredentials(credentialsProvider)
                .build();
        return kmsClient;
    }

    @GetMapping("/verify")
    public ResponseEntity<String> validate(@RequestHeader String authorization) throws IOException {
        var jwt = authorization.replace("Bearer ", "");
        var decodedJwt = JWT.decode(jwt);

        var message = this.tokenUtil.encodeData(decodedJwt.getHeader(), decodedJwt.getPayload());
        var signature = Base64.getDecoder().decode(decodedJwt.getSignature());

        var verifyRequest = new VerifyRequest()
                .withKeyId(KMS_KEY_ID)
                .withSigningAlgorithm(SIGNING_ALGORITHM)
                .withSignature(ByteBuffer.wrap(signature))
                .withMessage(message);

        var kmsClient = getKmsClient();

        try {
            VerifyResult verifyResult = kmsClient.verify(verifyRequest);
            return ResponseEntity.ok("validado");
        } catch (KMSInvalidSignatureException e) {
            return ResponseEntity.badRequest().body("Token invalido");
        }

    }

    public static record User(String username){}
    public static record Header(String alg){}

    private static final SigningAlgorithmSpec SIGNING_ALGORITHM = SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256;
    private static final String KMS_KEY_ID = "3ed544f9-af83-4589-ac04-59af3d3cee8e";
}
