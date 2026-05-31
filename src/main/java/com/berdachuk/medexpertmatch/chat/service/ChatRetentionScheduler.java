package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatRetentionScheduler {

    private final ChatRetentionService chatRetentionService;
    private final ChatRetentionProperties properties;

    public ChatRetentionScheduler(ChatRetentionService chatRetentionService, ChatRetentionProperties properties) {
        this.chatRetentionService = chatRetentionService;
        this.properties = properties;
    }

    @Scheduled(cron = "${chat.retention.cron:0 0 3 * * *}")
    public void purgeIdleChats() {
        if (!properties.enabled()) {
            return;
        }
        chatRetentionService.purgeIdleChats();
    }
}
