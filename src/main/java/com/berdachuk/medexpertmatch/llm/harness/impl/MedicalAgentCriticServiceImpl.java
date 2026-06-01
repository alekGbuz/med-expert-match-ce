package com.berdachuk.medexpertmatch.llm.harness.impl;

import com.berdachuk.medexpertmatch.llm.harness.HarnessFailureReason;
import com.berdachuk.medexpertmatch.llm.harness.MedicalAgentCriticService;
import com.berdachuk.medexpertmatch.llm.config.HarnessProperties;
import com.berdachuk.medexpertmatch.llm.harness.HarnessMetrics;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class MedicalAgentCriticServiceImpl implements MedicalAgentCriticService {

    private static final String SAFE_FALLBACK = """
            No validated clinical response is available for this request.
            This system is for research and educational purposes only and is not a substitute \
            for professional medical advice, diagnosis, or treatment. Always consult qualified \
            healthcare professionals for medical decisions.""";

    private final HarnessProperties harnessProperties;
    private final HarnessMetrics harnessMetrics;

    public MedicalAgentCriticServiceImpl(HarnessProperties harnessProperties, HarnessMetrics harnessMetrics) {
        this.harnessProperties = harnessProperties;
        this.harnessMetrics = harnessMetrics;
    }

    @Override
    public CriticResult review(String responseText, Map<String, Object> metadata) {
        if (!harnessProperties.criticEnabled()) {
            return new CriticResult(true, responseText, null, null);
        }
        if (responseText == null || responseText.isBlank()) {
            harnessMetrics.recordCriticFailure(HarnessFailureReason.CRITIC_REJECTED.name());
            return new CriticResult(false, SAFE_FALLBACK, HarnessFailureReason.CRITIC_REJECTED, "empty response");
        }
        String lower = responseText.toLowerCase(Locale.ROOT);
        if (containsObviousPhi(lower)) {
            harnessMetrics.recordCriticFailure(HarnessFailureReason.POLICY_VIOLATION.name());
            return new CriticResult(false, SAFE_FALLBACK, HarnessFailureReason.POLICY_VIOLATION, "phi pattern");
        }
        String finalText = responseText;
        if (!containsDisclaimer(lower)) {
            finalText = responseText + "\n\n"
                    + "This output is for research and educational purposes only and is not a substitute "
                    + "for professional medical advice, diagnosis, or treatment.";
        }
        return new CriticResult(true, finalText, null, null);
    }

    private static boolean containsDisclaimer(String lower) {
        return lower.contains("not a substitute")
                || lower.contains("research and educational")
                || lower.contains("professional medical advice");
    }

    private static boolean mentionsGrounding(String lower) {
        return lower.contains("match")
                || lower.contains("doctor")
                || lower.contains("specialist")
                || lower.contains("score")
                || lower.contains("retrieval");
    }

    private static boolean containsObviousPhi(String lower) {
        return lower.contains("ssn")
                || lower.contains("social security")
                || lower.matches(".*\\b\\d{3}-\\d{2}-\\d{4}\\b.*");
    }
}
