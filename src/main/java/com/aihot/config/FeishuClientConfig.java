package com.aihot.config;

import com.aihot.config.properties.FeishuProperties;
import com.lark.oapi.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeishuClientConfig {

    @Bean
    public Client feishuClient(FeishuProperties properties) {
        return Client.newBuilder(properties.getAppId(), properties.getAppSecret())
                .build();
    }
}
