package com.example.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Value("${ai.server.timeout:30000}")
    private Integer defaultTimeout;

    @Bean("defaultRestTemplate")
    @Primary
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(defaultTimeout);
        factory.setReadTimeout(defaultTimeout);

        log.info("기본 RestTemplate 생성: timeout {}ms", defaultTimeout);
        return new RestTemplate(factory);
    }

    @Bean("aiServerRestTemplate")
    public RestTemplate aiServerRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        int connectTimeoutMs = 120000;
        int readTimeoutMs = 15 * 60 * 1000;

        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        log.info("AI 서버 RestTemplate 생성");
        log.info("연결 타임아웃: {}ms ({}분)", connectTimeoutMs, connectTimeoutMs / 60000);
        log.info("읽기 타임아웃: {}ms ({}분)", readTimeoutMs, readTimeoutMs / 60000);

        return new RestTemplate(factory);
    }
}