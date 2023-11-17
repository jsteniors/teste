package br.com.teste;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import static br.com.teste.EncodeUtil.encodeJwt;

@Component
public class TokenUtil {

    private ObjectMapper mapper;

    public TokenUtil(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ByteBuffer encodeData(String header, String payload) throws IOException {
        var headerBytes = Base64.getDecoder().decode(header.getBytes());
        var payloadBytes = Base64.getDecoder().decode(payload.getBytes());
        var h = mapper.readTree(headerBytes);
        var user = mapper.readTree(payloadBytes);
//        var bytesHeader = Base64.getDecoder().decode(header.getBytes());
        var bytesHeader = mapper.writeValueAsBytes(h);
        var bytesPayload = mapper.writeValueAsBytes(user);
//        var bytesPayload = Base64.getDecoder().decode(header.getBytes());
        return decodeBytes(bytesHeader, bytesPayload);
    }
    public ByteBuffer encodeData(Object header, Object payload) throws JsonProcessingException {
        var headersBystes =  this.mapper.writeValueAsBytes(header);
        var payloadBytes =  this.mapper.writeValueAsBytes(payload);
        var messageByteBuffer = decodeBytes(payloadBytes, headersBystes);
        return messageByteBuffer;
    }

    private static ByteBuffer decodeBytes(byte[] payloadBytes, byte[] headersBystes) {
        var payloadBase64Bytes = encodeJwt(payloadBytes);
        var headerBase64Bytes = encodeJwt(headersBystes);
        var messageByteBuffer = BufferUtil.concat(headerBase64Bytes, DOT_BASE64_BYTES, payloadBase64Bytes);
        return messageByteBuffer;
    }

    private static final String DOT_BASE64_BYTES = encodeJwt(".".getBytes());
}
