package com.example.backend.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {
    private String fieldSecretKey;
    private String tableSecretKey;

    public void setFieldSecretKey(String key) { this.fieldSecretKey = key; }
    public void setTableSecretKey(String key) { this.tableSecretKey = key; }
}