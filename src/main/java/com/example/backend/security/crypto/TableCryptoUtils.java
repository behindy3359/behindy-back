package com.example.backend.security.crypto;

import com.example.backend.config.CryptoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TableCryptoUtils {
    private final CryptoProperties properties;
    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        keySpec = new SecretKeySpec(properties.getTableSecretKey().getBytes(), "AES");
    }

    public String encrypt(String raw) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(raw.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String enc) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(enc)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}