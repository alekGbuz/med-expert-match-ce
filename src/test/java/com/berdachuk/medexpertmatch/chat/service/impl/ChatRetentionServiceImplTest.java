package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.config.ChatRetentionProperties;
import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRetentionServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    private ChatRetentionProperties properties;
    private ChatRetentionServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new ChatRetentionProperties();
        properties.setIdleDays(30);
        properties.setBatchSize(10);
        service = new ChatRetentionServiceImpl(properties, chatRepository);
    }

    @Test
    @DisplayName("Skips purge when retention disabled")
    void disabledRetention() {
        properties.setIdleDays(0);
        assertEquals(0, service.purgeIdleChats());
        verify(chatRepository, never()).findIdleNonDefaultChatsBefore(any(), eq(10));
    }

    @Test
    @DisplayName("Purges idle non-default chats")
    void purgesIdleChats() {
        Chat idle = new Chat("c1", "u1", "Old", "auto", false,
                Instant.now(), Instant.now(), Instant.now().minusSeconds(86400L * 40), 0);
        when(chatRepository.findIdleNonDefaultChatsBefore(any(), eq(10))).thenReturn(List.of(idle));
        when(chatRepository.deleteChat("c1")).thenReturn(true);

        assertEquals(1, service.purgeIdleChats());
        verify(chatRepository).deleteChat("c1");
    }
}
