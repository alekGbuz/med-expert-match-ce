"""
Shared synthetic data definitions for FunctionGemma tool-selection eval and training (M58).
No PHI — synthetic 24-char hex case IDs only.
"""

from __future__ import annotations

import json
import random
import secrets
from dataclasses import dataclass
from typing import Iterator

CASE_HINT = (
    "IMPORTANT — medical case ID for case-ID tools "
    "(match_doctors_to_case, analyze_case, etc.): {case_id}\n"
    "Use this exact 24-character ID only. Do NOT use ICD-10 codes, labels, or invented IDs."
)

NO_CASE_ID_HINT = (
    "No medical case ID in this message. Do NOT invent case IDs or pass ICD-10 codes to match_doctors_to_case.\n"
    "For specialist matching use match_doctors_from_text with the full anonymized case description.\n"
    "For analysis without matching use analyze_case_text with the case description."
)

TOOLS_WITH_CASE_ID = frozenset(
    {"analyze_case", "match_doctors_to_case", "match_facilities_for_case"}
)


@dataclass(frozen=True)
class ScenarioDef:
    id: str
    locale: str
    goal: str
    tool: str | None
    with_case_id: bool
    weight: float = 1.0


# Message banks — combinatorial templates expand to many unique phrasings.
MESSAGE_BANKS: dict[str, list[str]] = {
    "analyze_with_case_id_en": [
        "detail the clinical case",
        "elaborate on the case presentation",
        "clinical breakdown please",
        "what are the clinical findings",
        "provide more details",
        "tell me more about this case",
        "describe the clinical case",
        "patient case overview",
        "go on",
        "expand on the case",
        "walk me through the clinical case",
        "case summary please",
        "in-depth case analysis",
        "review the clinical case",
        "clinical assessment for this case",
        "explain the case presentation",
        "deeper clinical analysis",
        "what is the clinical picture",
        "break down the case",
        "summarize the patient case",
    ],
    "analyze_with_case_id_ru": [
        "детализируй клинический случай",
        "опиши случай подробнее",
        "расскажи подробнее о кейсе",
        "анализ случая",
        "разверни клинический случай",
        "что еще по случаю",
        "подробнее о пациенте",
        "клиническая картина случая",
        "опиши клинический случай",
        "детали случая",
    ],
    "match_with_case_id_en": [
        "find specialists for this case",
        "rank doctors for complex presentation",
        "suggest a specialist for referral",
        "locate a specialist",
        "match doctors to this case",
        "find expert for referral",
        "who should treat this case",
        "top doctors for this presentation",
        "specialist recommendation",
        "find the best clinician",
        "expert match please",
        "recommend a physician",
        "suitable specialist for case",
        "find doctors for this patient",
    ],
    "match_follow_up_ru": [
        "найди еще докторов",
        "подбери других специалистов",
        "еще варианты врачей",
        "другие кандидаты врачей",
        "покажи еще специалистов",
        "найди других экспертов",
    ],
    "match_follow_up_en": [
        "find other doctors",
        "show me more specialists",
        "any other experts",
        "different doctor options",
        "more physician matches",
    ],
    "route_with_case_id_en": [
        "route patient to appropriate facility",
        "where should the patient go",
        "send to hospital",
        "appropriate care center",
        "facility routing for case",
        "refer to treatment center",
    ],
    "match_from_text_no_id": [
        "55 year old with progressive dyspnea and orthopnea",
        "find specialist for anonymized chest pain case",
        "patient 42 with recurrent migraines needs neurologist",
        "best doctor for stroke rehabilitation",
        "62yo with new onset atrial fibrillation",
        "pediatric case with persistent wheeze",
        "подбери врача для случая с болью в груди",
        "young adult with inflammatory joint pain",
    ],
    "analyze_from_text_no_id": [
        "Patient is 45 with chest pain radiating to left arm",
        "Analyze this narrative: 30yo with fever and rash",
        "опиши клиническую картину по тексту случая",
        "case workup for abdominal pain",
        "48 year old with progressive weakness",
        "review this anonymized case text",
    ],
    "evidence_pubmed": [
        "search pubmed for diabetes treatment",
        "find literature on heart failure management",
        "systematic review for hypertension",
        "pubmed evidence for COPD",
        "research papers on sepsis protocols",
    ],
    "evidence_guidelines": [
        "clinical practice guidelines for hypertension",
        "treatment guidelines for asthma",
        "evidence for COPD management",
        "guideline recommendations for stroke",
    ],
    "negative_text_only": [
        "What is GraphRAG?",
        "How does specialist matching work?",
        "explain the harness architecture",
        "what tools do you have",
        "how does this application work",
        "describe the agent pipeline",
    ],
    "triage_with_case_id": [
        "how urgent is this presentation",
        "triage this case",
        "priority level assessment",
        "acuity for this patient",
    ],
}

SCENARIOS: list[ScenarioDef] = [
    ScenarioDef("analyze_with_case_id_en", "en", "ANALYZE_CASE", "analyze_case", True, 1.2),
    ScenarioDef("analyze_with_case_id_ru", "ru", "ANALYZE_CASE", "analyze_case", True, 1.0),
    ScenarioDef("match_with_case_id_en", "en", "MATCH_DOCTORS", "match_doctors_to_case", True, 1.2),
    ScenarioDef("match_follow_up_ru", "ru", "MATCH_DOCTORS", "match_doctors_to_case", True, 0.9),
    ScenarioDef("match_follow_up_en", "en", "MATCH_DOCTORS", "match_doctors_to_case", True, 0.8),
    ScenarioDef("route_with_case_id_en", "en", "ROUTE_CASE", "match_facilities_for_case", True, 0.6),
    ScenarioDef("match_from_text_no_id", "en", "MATCH_DOCTORS", "match_doctors_from_text", False, 0.9),
    ScenarioDef("analyze_from_text_no_id", "en", "ANALYZE_CASE", "analyze_case_text", False, 0.8),
    ScenarioDef("evidence_pubmed", "en", "SEARCH_EVIDENCE", "query_pubmed", False, 0.5),
    ScenarioDef("evidence_guidelines", "en", "SEARCH_EVIDENCE", "search_clinical_guidelines", False, 0.5),
    ScenarioDef("triage_with_case_id", "en", "TRIAGE_INTAKE", "analyze_case", True, 0.4),
    ScenarioDef("negative_text_only", "en", "GENERAL_QUESTION", None, False, 0.25),
]


def synthetic_case_id(rng: random.Random) -> str:
    return secrets.token_hex(12)


def pick_case_id(rng: random.Random, pool: list[str]) -> str:
    if pool:
        return rng.choice(pool)
    return synthetic_case_id(rng)


def build_user_prompt(case_id: str | None, goal: str, message: str) -> str:
    parts: list[str] = []
    if case_id:
        parts.append(CASE_HINT.format(case_id=case_id))
    else:
        parts.append(NO_CASE_ID_HINT)
    if goal and goal != "GENERAL_QUESTION":
        parts.append(f"Goal hint: {goal}")
    parts.append(f"User message:\n{message}")
    return "\n\n".join(parts)


def scenario_messages(scenario_id: str) -> list[str]:
    base = list(MESSAGE_BANKS.get(scenario_id, ["help with this case"]))
    # Light combinatorial expansion for large datasets.
    extras: list[str] = []
    prefixes_en = ["please", "can you", "I need you to", "quickly"]
    prefixes_ru = ["пожалуйста", "нужно", "можешь"]
    for msg in base[:8]:
        if scenario_id.endswith("_ru"):
            for p in prefixes_ru:
                extras.append(f"{p} {msg}")
        elif scenario_id.endswith("_en") or "en" in scenario_id:
            for p in prefixes_en:
                extras.append(f"{p} {msg}")
    combined = base + extras
    return list(dict.fromkeys(combined))


def to_eval_row(
    scenario: ScenarioDef,
    message: str,
    case_id: str | None,
    *,
    golden: bool = False,
) -> dict:
    expected_args = {"caseId": case_id} if scenario.tool in TOOLS_WITH_CASE_ID and case_id else None
    return {
        "scenario": scenario.id,
        "goalType": scenario.goal,
        "caseIdInHints": bool(case_id),
        "caseId": case_id,
        "userMessage": message,
        "expectedTool": scenario.tool,
        "expectedArgs": expected_args,
        "locale": scenario.locale,
        "golden": golden,
    }


def to_train_row(scenario: ScenarioDef, message: str, case_id: str | None) -> dict:
    tool = scenario.tool or ""
    args = {"caseId": case_id} if tool in TOOLS_WITH_CASE_ID and case_id else {}
    return {
        "user_prompt": build_user_prompt(case_id, scenario.goal, message),
        "tool_name": tool,
        "tool_args_json": json.dumps(args) if args else "{}",
        "locale": scenario.locale,
        "scenario": scenario.id,
        "goalType": scenario.goal,
        "userMessage": message,
        "caseId": case_id or "",
    }


def resolve_min_per_scenario(target_size: int, min_per_scenario: int) -> tuple[int, int]:
    """Return (effective_min_per_scenario, actual_target_size)."""
    num_scenarios = len(SCENARIOS)
    min_required = min_per_scenario * num_scenarios
    if target_size >= min_required:
        return min_per_scenario, target_size
    effective = max(1, target_size // num_scenarios)
    return effective, target_size


def iter_balanced_rows(
    rng: random.Random,
    target_size: int,
    *,
    min_per_scenario: int,
    case_id_pool: list[str],
) -> Iterator[dict]:
    """Yield eval-format rows until target_size, balanced by scenario weight."""
    effective_min, actual_size = resolve_min_per_scenario(target_size, min_per_scenario)
    per_scenario: dict[str, int] = {s.id: 0 for s in SCENARIOS}
    weighted = [(s, s.weight) for s in SCENARIOS]
    scenarios_cycle: list[ScenarioDef] = []
    while len(scenarios_cycle) < len(SCENARIOS) * max(effective_min, 1):
        pick = rng.choices([s for s, _ in weighted], weights=[w for _, w in weighted], k=1)[0]
        scenarios_cycle.append(pick)

    messages_cache: dict[str, list[str]] = {
        s.id: scenario_messages(s.id) for s in SCENARIOS
    }

    produced = 0
    idx = 0
    while produced < actual_size:
        scenario = scenarios_cycle[idx % len(scenarios_cycle)]
        idx += 1
        msgs = messages_cache[scenario.id]
        message = rng.choice(msgs)
        case_id = pick_case_id(rng, case_id_pool) if scenario.with_case_id else None
        per_scenario[scenario.id] += 1
        produced += 1
        yield to_eval_row(scenario, message, case_id)

    for scenario in SCENARIOS:
        while per_scenario[scenario.id] < effective_min and produced < actual_size:
            msgs = messages_cache[scenario.id]
            message = rng.choice(msgs)
            case_id = pick_case_id(rng, case_id_pool) if scenario.with_case_id else None
            per_scenario[scenario.id] += 1
            produced += 1
            yield to_eval_row(scenario, message, case_id)


def split_rows(
    rows: list[dict], train_ratio: float, val_ratio: float
) -> tuple[list[dict], list[dict], list[dict]]:
    n = len(rows)
    train_end = int(n * train_ratio)
    val_end = train_end + int(n * val_ratio)
    return rows[:train_end], rows[train_end:val_end], rows[val_end:]
