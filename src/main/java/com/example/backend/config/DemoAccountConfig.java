package com.example.backend.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "demo")
@Getter
@Setter
public class DemoAccountConfig {

    private List<DemoAccount> accounts = new ArrayList<>();

    @Data
    public static class DemoAccount {
        private String email;
        private String password;
        private String name;
    }
}
