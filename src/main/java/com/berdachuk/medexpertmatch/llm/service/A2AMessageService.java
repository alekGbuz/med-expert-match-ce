package com.berdachuk.medexpertmatch.llm.service;

import java.util.Map;

/**
 * A2A-style message bridge with JSON-RPC 2.0 support and domain skill routing.
 */
public interface A2AMessageService {

    Map<String, Object> sendMessage(Map<String, Object> request);

    Map<String, Object> handleJsonRpc(Map<String, Object> request);
}
