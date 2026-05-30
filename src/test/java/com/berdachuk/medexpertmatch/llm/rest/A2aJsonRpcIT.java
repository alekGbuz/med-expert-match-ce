package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class A2aJsonRpcIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonRpcSendMessageRoutesEvidenceSkill() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "jsonrpc", "2.0",
                "id", "it-1",
                "method", "sendMessage",
                "params", java.util.Map.of(
                        "skill", "evidence_search",
                        "message", "Anonymized COPD exacerbation management literature")));

        mockMvc.perform(post("/a2a/v1/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("completed"))
                .andExpect(jsonPath("$.result.skill").value("evidence_search"));
    }
}
