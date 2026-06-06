package com.berdachuk.medexpertmatch.llm.eval;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Lightweight tool stubs for FunctionGemma live eval (schema only; no side effects).
 */
public class EvalToolStubs {

    @Tool(description = "Analyze a medical case by ID and extract key clinical information.")
    public String analyze_case(
            @ToolParam(description = "Medical case ID - 24-character hex string") String caseId) {
        return "eval-stub";
    }

    @Tool(description = "Analyze unstructured medical case text and extract key clinical information.")
    public String analyze_case_text(
            @ToolParam(description = "Unstructured case description text") String caseText) {
        return "eval-stub";
    }

    @Tool(description = "Match doctors to a medical case by case ID using GraphRAG.")
    public String match_doctors_to_case(
            @ToolParam(description = "Medical case ID - 24-character hex string") String caseId) {
        return "eval-stub";
    }

    @Tool(description = "Match doctors from anonymized case text when NO existing case ID is available.")
    public String match_doctors_from_text(
            @ToolParam(description = "Full anonymized clinical case description") String caseText) {
        return "eval-stub";
    }

    @Tool(description = "Match facilities for a medical case by case ID.")
    public String match_facilities_for_case(
            @ToolParam(description = "Medical case ID - 24-character hex string") String caseId) {
        return "eval-stub";
    }

    @Tool(description = "Search clinical practice guidelines for a medical condition.")
    public String search_clinical_guidelines(
            @ToolParam(description = "Medical condition or diagnosis") String condition) {
        return "eval-stub";
    }

    @Tool(description = "Query PubMed for clinical literature and research articles.")
    public String query_pubmed(
            @ToolParam(description = "Search query for PubMed") String query) {
        return "eval-stub";
    }
}
