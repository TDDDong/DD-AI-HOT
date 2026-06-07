package com.aihot.service.english;

import com.aihot.domain.english.EnglishWordRecord;
import com.aihot.domain.storage.SaveResult;
import com.aihot.integration.english.EnglishWordClient;
import org.springframework.stereotype.Service;

@Service
public class EnglishWordFetchService {

    private final EnglishWordClient englishWordClient;
    private final EnglishWordPersistenceService persistenceService;

    public EnglishWordFetchService(
            EnglishWordClient englishWordClient, EnglishWordPersistenceService persistenceService) {
        this.englishWordClient = englishWordClient;
        this.persistenceService = persistenceService;
    }

    public EnglishWordRecord fetchRandom() {
        return englishWordClient.fetchRandomWord();
    }

    public SaveResult fetchAndPersist() {
        EnglishWordRecord word = fetchRandom();
        return persistenceService.saveNewWords(java.util.List.of(word));
    }
}
