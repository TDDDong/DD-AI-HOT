package com.aihot.config;

import com.aihot.config.properties.EnglishHotspotProperties;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnglishHotspotClientConfig {

    @Bean
    public HttpClient httpClient(EnglishHotspotProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }
}
