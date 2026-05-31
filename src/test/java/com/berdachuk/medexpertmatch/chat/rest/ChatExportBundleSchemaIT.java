package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatExportBundleSchemaIT extends BaseIntegrationTest {

    private static final Set<String> BUNDLE_TOP_LEVEL =
            Set.of("userIdHash", "exportedAt", "phiRedacted", "chatCount", "messageCount", "chats", "auditReferenceHash");

    private static final Set<String> CHAT_EXPORT_FIELDS =
            Set.of("chatId", "name", "agentId", "isDefault", "messages");

    private static final Set<String> MESSAGE_FIELDS =
            Set.of("id", "role", "content", "sequenceNumber", "createdAt");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Export bundle JSON matches OpenAPI ChatExportBundleResponse contract")
    void exportBundleMatchesOpenApiContract() throws Exception {
        String userId = "export-schema-user";
        createChatWithMessage(userId, "Schema chat");

        String body = mockMvc.perform(get("/api/v1/chats/export-bundle")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertTrue(hasExactKeys(root, BUNDLE_TOP_LEVEL));
        assertTrue(root.get("phiRedacted").asBoolean());
        assertTrue(root.get("chatCount").isInt());
        assertTrue(root.get("messageCount").isInt());
        assertTrue(root.get("chats").isArray());
        assertTrue(root.get("chats").size() >= 1);

        JsonNode chat = root.get("chats").get(0);
        assertTrue(hasExactKeys(chat, CHAT_EXPORT_FIELDS));
        assertTrue(chat.get("messages").isArray());
        assertTrue(chat.get("messages").size() >= 1);

        JsonNode message = chat.get("messages").get(0);
        assertTrue(hasExactKeys(message, MESSAGE_FIELDS));
        assertTrue(message.get("role").isTextual());
        assertTrue(message.get("content").isTextual());
    }

    private static boolean hasExactKeys(JsonNode node, Set<String> expected) {
        if (node.size() != expected.size()) {
            return false;
        }
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            if (!expected.contains(names.next())) {
                return false;
            }
        }
        return true;
    }

    private void createChatWithMessage(String userId, String name) throws Exception {
        var createResult = mockMvc.perform(post("/api/v1/chats")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"agentId\":\"auto\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String chatId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/chats/" + chatId + "/messages")
                        .header(HeaderBasedUserContext.USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Anonymized symptom summary for schema validation\"}"))
                .andExpect(status().isOk());
    }
}
