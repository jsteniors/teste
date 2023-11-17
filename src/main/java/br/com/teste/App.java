package br.com.teste;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.KmsInvalidSignatureException;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;
import software.amazon.awssdk.services.kms.model.VerifyRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import static br.com.teste.EncodeUtil.encodeJwt;

@SpringBootApplication
@RestController
public class App {

    private final ObjectMapper mapper;
    private final TokenUtil tokenUtil;
    private final KmsClient kmsClient;

    public App(ObjectMapper mapper, TokenUtil tokenUtil, KmsClient kmsClient){
        this.mapper = mapper;
        this.tokenUtil = tokenUtil;
        this.kmsClient = kmsClient;
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

        var request = SignRequest.builder()
                .keyId(KMS_KEY_ID)
                .signingAlgorithm(SIGNING_ALGORITHM)
                .message(SdkBytes.fromByteBuffer(messageByteBuffer))
                .build();

        var response = kmsClient.sign(request);

        var signature = encodeJwt(response.signature().asByteArray());

        var token = headerBase64Bytes
                    .concat(".")
                .concat(payloadBase64Bytes)
                .concat(".")
                .concat(signature);


        return ResponseEntity.ok(token);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> validate(@RequestHeader String authorization) throws IOException {
        var jwt = authorization.replace("Bearer ", "");
        var decodedJwt = JWT.decode(jwt);

        var message = this.tokenUtil.encodeData(decodedJwt.getHeader(), decodedJwt.getPayload());
        var signature = Base64.getDecoder().decode(decodedJwt.getSignature());

        var verifyRequest = VerifyRequest.builder()
                .keyId(KMS_KEY_ID)
                .signingAlgorithm(SIGNING_ALGORITHM)
                .signature(SdkBytes.fromByteArray(signature))
                .message(SdkBytes.fromByteBuffer(message))
                .build();

        try {
            var verifyResult = kmsClient.verify(verifyRequest);
            return ResponseEntity.ok("validado");
        } catch (KmsInvalidSignatureException e) {
            return ResponseEntity.badRequest().body("Token invalido");
        }

    }

    public static record User(String username){}
    public static record Header(String alg){}

    private static final SigningAlgorithmSpec SIGNING_ALGORITHM = SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256;
    private static final String KMS_KEY_ID = "3ed544f9-af83-4589-ac04-59af3d3cee8e";
}
