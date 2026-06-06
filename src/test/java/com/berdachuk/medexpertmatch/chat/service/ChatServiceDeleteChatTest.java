package com.berdachuk.medexpertmatch.chat.service;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.repository.ChatMessageRepository;
import com.berdachuk.medexpertmatch.chat.repository.ChatRepository;
import com.berdachuk.medexpertmatch.chat.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceDeleteChatTest {

    private final ChatRepository chatRepository = org.mockito.Mockito.mock(ChatRepository.class);
    private final ChatMessageRepository chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
    private final ChatService chatService = new ChatServiceImpl(chatRepository, chatMessageRepository);

    private static Chat chat(String id, String userId, boolean isDefault) {
        return new Chat(id, userId, "Chat", "auto", isDefault, Instant.now(), Instant.now(), Instant.now(), 0);
    }

    @Test
    void deleteDefaultChatWhenLastRemainingCreatesNewDefault() {
        Chat defaultChat = chat("default-1", "user-1", true);
        when(chatRepository.findById("default-1")).thenReturn(Optional.of(defaultChat));
        when(chatRepository.deleteChat("default-1")).thenReturn(true);
        when(chatRepository.findAllByUserId("user-1")).thenReturn(List.of());
        when(chatRepository.findDefaultChat("user-1")).thenReturn(Optional.empty());
        when(chatRepository.createChat(eq("user-1"), eq("Default Chat"), eq("auto"), eq(true)))
                .thenReturn(chat("default-2", "user-1", true));

        assertTrue(chatService.deleteChat("default-1", "user-1"));

        verify(chatRepository).deleteChat("default-1");
        verify(chatRepository).createChat("user-1", "Default Chat", "auto", true);
    }

    @Test
    void deleteNonDefaultChatWhenOthersRemainDoesNotCreateDefault() {
        Chat other = chat("other-1", "user-1", false);
        when(chatRepository.findById("delete-me")).thenReturn(Optional.of(chat("delete-me", "user-1", false)));
        when(chatRepository.deleteChat("delete-me")).thenReturn(true);
        when(chatRepository.findAllByUserId("user-1")).thenReturn(List.of(other));

        assertTrue(chatService.deleteChat("delete-me", "user-1"));

        verify(chatRepository).deleteChat("delete-me");
        org.mockito.Mockito.verify(chatRepository, org.mockito.Mockito.never())
                .createChat(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        anyBoolean());
    }
}
