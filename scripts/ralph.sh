#!/usr/bin/env bash
# Ralph-style autonomous iteration loop for MedExpertMatch.
#
# Usage: ./scripts/ralph.sh M{NN} [--max N] [--dry-run]
#
# Reads .agents/plans/M{NN}-stories.json, picks the highest-priority unpassed
# story (lowest priority int where passes == false), invokes the agent (stub
# for now) in a fresh subprocess, runs the story's test_target, on green
# commits with no Co-authored-by trailer, marks passes:true in the JSON, writes
# commit_sha, and appends a block to .agents/plans/progress.txt.
#
# This is the M79 build of the loop. Real agent invocation is a TODO stub
# (see invoke_agent below). The M80 pilot will wire the LLM API in.

set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") M{NN} [--max N] [--dry-run]

Arguments:
  M{NN}          Milestone id (e.g. M77). Reads .agents/plans/M{NN}-stories.json.

Options:
  --max N        Run at most N stories (default: 999999 = run until done).
  --dry-run      Print the chosen story and exit without invoking agent/tests.
  -h, --help     Show this help.

Exit codes:
  0   All stories passed (or --max reached without error).
  1   Bad usage.
  2   Missing stories.json or repo state.
  3   Test failure on the chosen story.
EOF
}

# ---- arg parsing ----

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac

MILESTONE="$1"
shift

MAX=999999
DRY_RUN=0
while [ $# -gt 0 ]; do
    case "$1" in
        --max)
            MAX="${2:-}"
            shift 2
            ;;
        --max=*)
            MAX="${1#--max=}"
            shift
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if ! [[ "$MAX" =~ ^[0-9]+$ ]] || [ "$MAX" -eq 0 ]; then
    echo "ERROR: --max must be a positive integer (got: '$MAX')" >&2
    exit 1
fi

# ---- locate repo and stories ----

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
STORIES_FILE="$REPO_ROOT/.agents/plans/${MILESTONE}-stories.json"
PROGRESS_FILE="$REPO_ROOT/.agents/plans/progress.txt"

if [ ! -f "$STORIES_FILE" ]; then
    echo "ERROR: $STORIES_FILE not found" >&2
    exit 2
fi

if ! jq -e . "$STORIES_FILE" >/dev/null 2>&1; then
    echo "ERROR: $STORIES_FILE is not valid JSON" >&2
    exit 2
fi

# ---- helpers ----

log() {
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

pick_next_story() {
    jq -r '
        .stories
        | map(select(.passes == false))
        | sort_by(.priority // 9999)
        | .[0].id // empty
    ' "$STORIES_FILE"
}

get_field() {
    local story_id="$1"
    local field="$2"
    jq -r --arg id "$story_id" --arg f "$field" '
        .stories[] | select(.id == $id) | .[$f] // ""
    ' "$STORIES_FILE"
}

append_progress() {
    local block="$1"
    printf '\n%s\n' "$block" >> "$PROGRESS_FILE"
}

mark_passed() {
    local story_id="$1"
    local commit_sha="$2"
    local tmp
    tmp="$(mktemp)"
    jq --arg id "$story_id" --arg sha "$commit_sha" --arg now "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '
        (.stories[] | select(.id == $id)) |= (.passes = true | .commit_sha = $sha | .finished_at = $now)
    ' "$STORIES_FILE" > "$tmp" && mv "$tmp" "$STORIES_FILE"
}

invoke_agent() {
    # TODO(M80): wire real agent invocation (e.g. opencode / claude-code subprocess).
    # For M79, the agent is a no-op: the story is considered "implemented" if its
    # test_target passes against the current working tree. Human-driven runs
    # implement the story in their working tree first, then invoke ralph.sh to
    # mark the test pass + commit.
    log "agent stub: assuming $1 is already implemented in working tree"
}

run_test() {
    local test_target="$1"
    if [ -z "$test_target" ]; then
        log "no test_target set; skipping test run"
        return 0
    fi
    if [[ "$test_target" == *.IT ]] || [[ "$test_target" == *IT.java ]]; then
        log "running mvn verify -Dit.test='$test_target'"
        (cd "$REPO_ROOT" && mvn -q verify -Dit.test="$test_target")
    else
        log "running mvn test -Dtest='$test_target'"
        (cd "$REPO_ROOT" && mvn -q test -Dtest="$test_target")
    fi
}

# ---- main loop ----

log "ralph.sh start: milestone=$MILESTONE max=$MAX dry_run=$DRY_RUN"

iter=0
while [ "$iter" -lt "$MAX" ]; do
    iter=$((iter + 1))

    story_id="$(pick_next_story)"
    if [ -z "$story_id" ] || [ "$story_id" = "null" ]; then
        log "no unpassed stories remaining; exiting"
        exit 0
    fi

    title="$(get_field "$story_id" title)"
    test_target="$(get_field "$story_id" test_target)"

    log "iter=$iter picked=$story_id title='$title' test_target='$test_target'"

    if [ "$DRY_RUN" -eq 1 ]; then
        log "dry-run: would invoke agent, run '$test_target', commit, mark passes"
        exit 0
    fi

    invoke_agent "$story_id"

    set +e
    run_test "$test_target"
    test_rc=$?
    set -e

    if [ "$test_rc" -ne 0 ]; then
        append_progress "## $(date -u +%Y-%m-%d) ${MILESTONE}-${story_id} (${title}) [RED]
- test_target '$test_target' failed (rc=$test_rc)
- Discovered: $(cd "$REPO_ROOT" && mvn test -Dtest="$test_target" 2>&1 | tail -20 || true)
- Next: fix and re-run ralph.sh"
        log "test failed; appended red block to progress.txt"
        exit 3
    fi

    # green: commit + mark
    commit_msg="${MILESTONE}-${story_id}: ${title}"
    (cd "$REPO_ROOT" && git add -A && git -c trailer.co-authored-by= commit -m "$commit_msg" -m "no Co-authored-by trailer per AGENTS.md")
    commit_sha="$(cd "$REPO_ROOT" && git rev-parse HEAD)"

    mark_passed "$story_id" "$commit_sha"

    append_progress "## $(date -u +%Y-%m-%d) ${MILESTONE}-${story_id} (${title}) [GREEN]
- commit: $commit_sha
- test_target '$test_target' green
- Next: $(pick_next_story || echo 'none')"

    log "story $story_id passed; commit=$commit_sha"
done

log "max iterations ($MAX) reached; exiting"
exit 0
