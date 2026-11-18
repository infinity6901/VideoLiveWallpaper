#!/usr/bin/env bash
# Helper functions to run a CI check, capture its log, and record PASS/FAIL/NOT_RUN in ci-status/*.tsv.

ci_record() {
  local id="$1"
  local status="$2"
  local desc="$3"
  local rc="${4:-0}"
  local status_dir="${CI_STATUS_DIR:-ci-status}"
  mkdir -p "$status_dir"
  printf '%s\t%s\t%s\t%s\n' "$id" "$status" "$rc" "$desc" > "${status_dir}/${id}.tsv"
}

ci_step() {
  local id="$1"
  local desc="$2"
  shift 2
  local log_dir="${CI_LOG_DIR:-ci-logs}"
  local status_dir="${CI_STATUS_DIR:-ci-status}"
  mkdir -p "$log_dir" "$status_dir"
  local log_file="${log_dir}/${id}.log"

  echo "=== [${id}] ${desc} ==="
  set +e
  "$@" 2>&1 | tee "$log_file"
  local rc="${PIPESTATUS[0]}"
  set -e

  if [ "$rc" -eq 0 ] && [ -n "${CI_FAIL_IF_LOG_MATCHES:-}" ]; then
    if grep -qE "$CI_FAIL_IF_LOG_MATCHES" "$log_file"; then
      echo "::error::Log for ${id} matched failure pattern: ${CI_FAIL_IF_LOG_MATCHES}"
      rc=1
    fi
  fi

  if [ "$rc" -eq 0 ]; then
    ci_record "$id" "PASS" "$desc" 0
  else
    ci_record "$id" "FAIL" "$desc" "$rc"
  fi
  return 0
}
