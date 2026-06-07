package com.aihot.integration.english;

import com.aihot.common.exception.EnglishWordFetchException;
import com.aihot.config.properties.EnglishHotspotProperties;
import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.integration.english.response.RandomEnglishWordResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultEnglishWordClient implements EnglishWordClient {

    private final HttpClient httpClient;
    private final EnglishHotspotProperties properties;
    private final ObjectMapper objectMapper;
    private final EnglishWordResponseMapper responseMapper;

    public DefaultEnglishWordClient(
            HttpClient httpClient,
            EnglishHotspotProperties properties,
            ObjectMapper objectMapper,
            EnglishWordResponseMapper responseMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.responseMapper = responseMapper;
    }

    @Override
    public EnglishWordRecord fetchRandomWord() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getApiUrl()))
                .timeout(properties.getReadTimeout())
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new EnglishWordFetchException(
                        "英语热点 API HTTP 错误: status=%d".formatted(response.statusCode()));
            }
            RandomEnglishWordResponse body = objectMapper.readValue(response.body(), RandomEnglishWordResponse.class);
            if (body.code() != 200) {
                throw new EnglishWordFetchException(
                        "英语热点 API 业务错误: code=%d msg=%s".formatted(body.code(), body.msg()));
            }
            return responseMapper.toRecord(body.data());
        } catch (EnglishWordFetchException e) {
            throw e;
        } catch (Exception e) {
            throw new EnglishWordFetchException("拉取随机英语单词失败", e);
        }
    }
}
