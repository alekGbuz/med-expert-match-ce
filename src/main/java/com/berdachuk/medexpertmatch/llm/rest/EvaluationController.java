package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.evaluation.EvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runEvaluation(
            @RequestParam String datasetName,
            @RequestParam(defaultValue = "0.80") double semanticThreshold) {
        try {
            String result = evaluationService.run(datasetName, semanticThreshold);
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "dataset", datasetName,
                    "result", result));
        } catch (Exception e) {
            log.warn("Evaluation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "failed",
                    "error", e.getMessage()));
        }
    }
}
