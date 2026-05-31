package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "A2A Bridge", description = "JSON-RPC 2.0 A2A message endpoint")
@RestController
@RequestMapping("/a2a/v1")
public class A2aJsonRpcController {

    private final A2AMessageService a2aMessageService;

    public A2aJsonRpcController(A2AMessageService a2aMessageService) {
        this.a2aMessageService = a2aMessageService;
    }

    @Operation(summary = "JSON-RPC 2.0 sendMessage (PHI-safe, routes to domain skills)")
    @PostMapping("/jsonrpc")
    public Map<String, Object> jsonRpc(@RequestBody Map<String, Object> body) {
        return a2aMessageService.handleJsonRpc(body);
    }

    @Operation(summary = "Stream skill result with chat-compatible SSE token envelope")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        return a2aMessageService.streamMessage(body);
    }
}
