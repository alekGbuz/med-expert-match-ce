package com.berdachuk.medexpertmatch.retrieval.domain.dto;

import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatchOutcomeRecordRequest(
        @NotBlank String caseId,
        @NotBlank String doctorId,
        @NotNull MatchOutcomeLabel label
) {
}
