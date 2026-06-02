# M41: Fix Chat Follow-Up Context Loss

Chat assistant loses conversational context between turns, causing follow-up questions
like "provide more details about Dr. Young McGlynn" to receive irrelevant responses
referencing different doctors and cases.

## Root Causes

| # | Root cause | Location |
|---|-----------|----------|
| R1 | `ConversationGoalContext.clear()` called at end of **every** turn | `ChatAssistantServiceImpl.clearTurnContext():284` |
| R2 | `isFollowUpSignal()` rejects "provide more details about Dr. X" — length >20, contains domain keyword `doctor` | `GoalClassifier.isFollowUpSignal():169-173` |
| R3 | No conversation history injected into LLM prompt for follow-up turns | `ChatAssistantServiceImpl.buildUserPrompt():326-338` |
| R4 | `CaseIdExtractor` only scans current message, not prior turn context | `ChatCasePromptSupport.buildCaseToolHints():39` |
| R5 | `SessionMemoryAdvisor` has no events — `sessionService.appendMessage()` not called in stream path | `ChatAssistantServiceImpl.streamMessage():132-209` |

## Scope

| # | Deliverable | Effort |
|---|-------------|--------|
| 1 | Stop clearing `ConversationGoalContext` at end of turn; only clear on case-switch/explicit intent change | 2h |
| 2 | Expand `isFollowUpSignal()` to recognize "tell me more about X", "provide more details about X", "what about X" and similar follow-up phrasings | 2h |
| 3 | Inject recent conversation history into `buildUserPrompt` when a follow-up is detected or caseId is inherited from context | 4h |
| 4 | Fall back to `ConversationGoalContext` when `CaseIdExtractor` finds no ID in the current message | 1h |
| 5 | Persist assistant messages to `SessionService` in stream path for `SessionMemoryAdvisor` | 1h |
| 6 | Add/update tests | 4h |

**Total effort: ~14h**

## D1: Stop clearing ConversationGoalContext at end of turn

**Files:** `ChatAssistantServiceImpl.java`

**Problem:** `clearTurnContext()` at line 284 calls `ConversationGoalContext.clear(sessionId)` after every turn,
deleting the goal+caseId from both Caffeine cache and PostgreSQL. This means `detectFollowUp()` at
`GoalClassifier.java:139` always returns `null` for the next turn.

**Fix:** Remove `ConversationGoalContext.clear(sessionId)` from `clearTurnContext()`.
Only clear in `processMessage()` when `goal.isRoutableToEngine()` runs the harness engine
(path handles context separately). For the stream path, the `ConversationGoalContext.set()` at
line 172-173 now persists across turns.

Add a new `ConversationGoalContext.clear()` call in `GoalClassifier.detectFollowUp()` when a
`CASE_SWITCH_PATTERN` match is detected — this is the appropriate place to reset context.

```java
// ChatAssistantServiceImpl.clearTurnContext()
private void clearTurnContext(String sessionId) {
    ChatToolContextHolder.clear();
    OrchestrationContextHolder.clear();
    // REMOVED: ConversationGoalContext.clear(sessionId);
    logStreamService.clearCurrentSessionId();
}
```

Also add explicit clears:
- In `GoalClassifier.classifyByKeywords()` when a new routable goal is detected (user explicitly
  asks "find specialist for case X") — clear old context and start fresh.
- In `detectFollowUp()` when `CASE_SWITCH_PATTERN` matches — clear context.

## D2: Expand isFollowUpSignal() patterns

**Files:** `GoalClassifier.java`

**Problem:** Messages like "provide more details about Dr. Young McGlynn" fail all three
follow-up detection strategies: too long for short-message heuristic, no `FOLLOW_UP_PREFIX` match,
and contains domain keywords.

**Fix:** Add a new regex pattern `FOLLOW_UP_PHRASING` that captures natural follow-up expressions:

```java
private static final Pattern FOLLOW_UP_PHRASING = Pattern.compile(
    "\\b(tell me more|more (?:details|info|information)|provide more|what about|" +
    "how about|elaborate|expand|details? (?:about|on|for)|explain (?:more|further))\\b",
    Pattern.CASE_INSENSITIVE);
```

Update `isFollowUpSignal()` to check this pattern before the length/domain-keyword guards:

```java
private boolean isFollowUpSignal(String message) {
    // ... existing checks ...
    // NEW: match conversational follow-up phrasings (before length check)
    if (FOLLOW_UP_PHRASING.matcher(trimmed).find()) {
        return true;
    }
    // ... existing short-message heuristic ...
}
```

## D3: Inject conversation history into follow-up prompts

**Files:** `ChatAssistantServiceImpl.java`, `chat-user-message.st`

**Problem:** `buildUserPrompt()` only passes the current raw message to the LLM, with no
visibility into prior turns. The `SessionMemoryAdvisor` has no events either.

**Fix:** When the goal is classified as a follow-up (detected by `GoalClassifier`),
load the last N messages from `ChatService.getHistory()` and inject them into the prompt.

Two approaches — implement the simpler one:

### Approach A (simpler, preferred): Load last 4 messages into prompt

```java
private String buildUserPrompt(ChatAgentProfile profile, String content, GoalClassification goal,
                                String chatId, String userId) {
    // ... existing code ...

    String historyBlock = "";
    if (goal.summary() != null && goal.summary().startsWith("follow-up:")) {
        var history = chatService.getHistory(chatId, userId, 6, 0);
        if (!history.isEmpty()) {
            StringBuilder sb = new StringBuilder("Previous conversation:\n");
            for (var msg : history) {
                if (!msg.content().equals(content)) { // skip current message
                    sb.append(msg.role()).append(": ").append(msg.content()).append("\n\n");
                }
            }
            historyBlock = sb.toString();
        }
    }
    // add historyBlock as a new template variable
}
```

Update `chat-user-message.st`:

```
<historyBlock><caseToolHints><routingHint>User message:
<userMessage>
```

### Approach B (alternative): Append messages to SessionService
Call `sessionService.appendMessage()` for both user and assistant messages in the stream path
so the `SessionMemoryAdvisor` can inject context automatically. More robust but requires
testing the Spring AI session memory behavior with tool-calling models.

**Decision:** Use Approach A for this milestone. Approach B can be a separate follow-up (M42).

## D4: CaseIdExtractor fallback to ConversationGoalContext

**Files:** `ChatCasePromptSupport.java`

**Problem:** `buildCaseToolHints()` at line 39 calls `CaseIdExtractor.extractFromText(content)` which
only searches the current message text. When a follow-up has no case ID, the "no case ID" hint
is rendered, telling the LLM not to invent one.

**Fix:** Add a fallback that reads the caseId from `ConversationGoalContext` when extraction fails:

```java
public String buildCaseToolHints(String content, GoalClassification goal) {
    CaseContextIntent intent = resolveIntent(goal);
    return CaseIdExtractor.extractFromText(content)
            .or(() -> goal != null ? goal.caseId() : Optional.empty())
            .or(() -> {
                String sid = OrchestrationContextHolder.sessionIdOrNull();
                if (sid != null) {
                    ConversationGoalContext.Entry ctx = ConversationGoalContext.get(sid);
                    if (ctx != null && ctx.lastCaseId() != null) {
                        return Optional.of(ctx.lastCaseId());
                    }
                }
                return Optional.empty();
            })
            .map(caseId -> {
                String hint = caseIdHintTemplate.render(Map.of("caseId", caseId));
                CaseContextBundle bundle = caseContextBundleService.build(caseId, intent);
                return hint + "\n\nContext bundle: " + bundle.summary();
            })
            .orElseGet(() -> noCaseIdHintTemplate.render(Collections.emptyMap()));
}
```

## D5: Persist messages to SessionService in stream path

**Files:** `ChatAssistantServiceImpl.java`

**Problem:** `SessionMemoryAdvisor` at line 155 has no conversation events because
`sessionService.appendMessage()` is only called in the harness engine path (line 392).

**Fix:** Append messages to `SessionService` when they are persisted to `chat_message` table:

In `streamMessage()` doOnComplete callback:
```java
// After chatService.appendAssistantMessage(chatId, userId, reply)
try {
    sessionService.appendMessage(sessionId, new AssistantMessage(reply));
} catch (Exception ignored) { }
```

In `prepareTurn()`:
```java
// After chatService.appendUserMessage(chatId, userId, content.trim())
try {
    sessionService.appendMessage(sessionId, new UserMessage(content.trim()));
} catch (Exception ignored) { }
```

## D6: Tests

| Test | Type | File | Covers |
|------|------|------|--------|
| `shouldDetectFollowUpWithProvideMoreDetailsPhrasing` | Unit | `GoalClassifierFollowUpTest.java` | D2: "provide more details about Dr. X" detected as follow-up |
| `shouldDetectFollowUpWithTellMeMoreAbout` | Unit | `GoalClassifierFollowUpTest.java` | D2: "tell me more about..." detected |
| `shouldDetectFollowUpWithWhatAboutPhrasing` | Unit | `GoalClassifierFollowUpTest.java` | D2: "what about..." detected |
| `shouldNotClearConversationGoalContextOnTurnEnd` | Unit | `ChatAssistantServiceImplTest.java` | D1: verify context survives across turn boundaries |
| `shouldInjectHistoryIntoFollowUpPrompt` | Unit | `ChatAssistantServiceImplTest.java` | D3: history appears in prompt when follow-up detected |
| `shouldFallbackToGoalContextForCaseId` | Unit | `ChatCasePromptSupportTest.java` | D4: caseId inherited from ConversationGoalContext |
| `shouldClearContextOnCaseSwitchInFollowUp` | Unit | `GoalClassifierFollowUpTest.java` | D1: explicit case switch clears context |
| `followUpContextSurvivalEndToEnd` | IT | New `ChatFollowUpContextIT.java` | D1-D5: full turn 1 → turn 2 flow |

## TDD

1. Write tests first (D6)
2. Implement D1 (don't clear)
3. Implement D2 (expand patterns)
4. Implement D4 (caseId fallback)
5. Implement D3 (history injection)
6. Implement D5 (session persistence)
7. Run `mvn verify` and fix until green

## Out of Scope

- Approach B for D3 (SessionMemoryAdvisor-based history) — separate M42
- Eval harness run for prompt changes — post-implementation
- Playwright E2E tests for chat multi-turn — post-implementation
