package com.example.api;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

@Converter
public class AESConverter implements AttributeConverter<String, String> {

    @Value("${secret-key}")
    private String KEY;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}