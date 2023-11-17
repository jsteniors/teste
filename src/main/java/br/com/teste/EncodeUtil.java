package br.com.teste;

import java.util.Base64;

public class EncodeUtil {

    public static String encodeJwt(byte[] data){
        String encoded = Base64.getEncoder().encodeToString(data);
//        encoded = encoded.replaceAll("\\=*$", "");
        return encoded;
    }
}
