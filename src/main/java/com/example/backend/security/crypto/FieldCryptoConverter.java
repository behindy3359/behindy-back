package com.example.backend.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter
public class FieldCryptoConverter implements AttributeConverter<String, String> {

    @Autowired
    private FieldCryptoUtils fieldCrypto;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return fieldCrypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return fieldCrypto.decrypt(dbData);
    }
}
