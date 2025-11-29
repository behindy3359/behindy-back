package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${seoul.metro.api.timeout:10000}")
    private int timeoutMs;

    @Value("${ai.server.url:http://llmserver:8000}")
    private String aiServerUrl;

    @Value("${ai.server.timeout:900000}")
    private int aiServerTimeout;

    @Value("${behindy.internal.api-key:behindy-internal-2025-secret-key}")
    private String internalApiKey;

    @Bean("metroWebClient")
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs))
                .followRedirect(true);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024);
                })
                .build();

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();

        log.info("Metro WebClient 초기화 완료: 타임아웃 {}ms, 최대 메모리 2MB", timeoutMs);
        return client;
    }

    @Bean("llmWebClient")
    public WebClient llmWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000)
                .responseTimeout(Duration.ofMillis(aiServerTimeout))
                .followRedirect(true);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();

        WebClient client = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader("X-Internal-API-Key", internalApiKey)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();

        log.info("LLM WebClient 초기화 완료: snake_case 변환 활성화");

        return client;
    }
}