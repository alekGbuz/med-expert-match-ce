package com.berdachuk.medexpertmatch.llm.harness;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Business rules for doctor-match tool output verification.
 */
public final class DoctorMatchVerificationRules {

    public static final int DEFAULT_MIN_MATCHES = 1;

    private DoctorMatchVerificationRules() {}

    public static List<String> validateMatches(List<DoctorMatch> matches, int minMatches) {
        List<String> violations = new ArrayList<>();
        if (matches == null) {
            violations.add("match list is null");
            return violations;
        }
        if (matches.size() < minMatches) {
            violations.add("match count " + matches.size() + " below minimum " + minMatches);
        }
        for (int i = 0; i < matches.size(); i++) {
            DoctorMatch match = matches.get(i);
            if (match == null) {
                violations.add("match at index " + i + " is null");
                continue;
            }
            if (match.doctor() == null) {
                violations.add("match at index " + i + " has no doctor");
                continue;
            }
            Doctor doctor = match.doctor();
            if (doctor.name() == null || doctor.name().isBlank()) {
                violations.add("match at index " + i + " missing doctor name");
            }
            if (match.matchScore() < 0 || match.matchScore() > 100) {
                violations.add("match at index " + i + " score out of range");
            }
        }
        return violations;
    }
}
