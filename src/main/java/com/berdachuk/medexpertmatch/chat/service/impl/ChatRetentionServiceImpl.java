package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.ChatRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class ChatRetentionServiceImpl implements ChatRetentionService {

    private final ChatRetentionProperties properties;
    private final ChatRepository chatRepository;

    public ChatRetentionServiceImpl(ChatRetentionProperties properties, ChatRepository chatRepository) {
        this.properties = properties;
        this.chatRepository = chatRepository;
    }

    @Override
    @Transactional
    public int purgeIdleChats() {
        if (!properties.enabled()) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(properties.idleDays(), ChronoUnit.DAYS);
        List<Chat> idle = chatRepository.findIdleNonDefaultChatsBefore(cutoff, properties.batchSize());
        int purged = 0;
        for (Chat chat : idle) {
            if (chatRepository.deleteChat(chat.id())) {
                purged++;
            }
        }
        if (purged > 0) {
            log.info("Purged {} idle non-default chat(s) older than {} days", purged, properties.idleDays());
        }
        return purged;
    }
}
