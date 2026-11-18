#!/usr/bin/env python3
"""Aggregates CI step TSV files and job results into qa-report.md and failure-report.md."""

import json
import os
import sys
from pathlib import Path


def collect_statuses(root: Path):
    rows = []
    for tsv in sorted(root.rglob("*.tsv")):
        if "ci-status" not in tsv.parts:
            continue
        for line in tsv.read_text(encoding="utf-8", errors="replace").splitlines():
            parts = line.split("\t")
            if len(parts) >= 4:
                rows.append((parts[0], parts[1], parts[2], parts[3], str(tsv.parent.parent.name)))
    return rows


def main():
    artifacts_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("artifacts")
    out_dir = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("report-out")
    out_dir.mkdir(parents=True, exist_ok=True)

    version = os.environ.get("VERSION_NAME", "unknown")
    strict_lint = os.environ.get("STRICT_LINT", "false")
    job_results_raw = os.environ.get("JOB_RESULTS", "{}")
    try:
        job_results = json.loads(job_results_raw)
    except json.JSONDecodeError:
        job_results = {}

    rows = collect_statuses(artifacts_dir)
    failures = [r for r in rows if r[1] == "FAIL"]

    qa_lines = [
        f"# QA Report — VideoLiveWallpaper v{version}",
        "",
        "## Release-readiness gates",
        "",
        "| Job | Result |",
        "|---|---|",
    ]
    for job_name, info in job_results.items():
        res = info.get("result", "unknown") if isinstance(info, dict) else str(info)
        qa_lines.append(f"| `{job_name}` | **{res.upper()}** |")

    qa_lines += [
        "",
        f"Release rule: Gate 1 (`verify`) and `build-release` must PASS (`strict_lint={strict_lint}`).",
        "",
        "## Detailed Check Results",
        "",
        "| Check | Status | Exit | Description |",
        "|---|---|---|---|",
    ]
    for cid, status, rc, desc, _ in rows:
        qa_lines.append(f"| `{cid}` | **{status}** | `{rc}` | {desc} |")

    if not rows:
        qa_lines.append("| _(none)_ | NOT_RUN | `-` | No step TSV files were found in artifacts |")

    (out_dir / "qa-report.md").write_text("\n".join(qa_lines) + "\n", encoding="utf-8")

    fail_lines = [f"# Failure Report — VideoLiveWallpaper v{version}", ""]
    if not failures:
        fail_lines.append("All recorded checks passed (0 failures).")
    else:
        fail_lines.append(f"Found **{len(failures)}** failed check(s):")
        fail_lines.append("")
        for cid, status, rc, desc, source in failures:
            fail_lines.append(f"### `{cid}` (exit `{rc}`, from `{source}`)")
            fail_lines.append(f"- {desc}")
            log_matches = list(artifacts_dir.rglob(f"{cid}.log"))
            if log_matches:
                tail = log_matches[0].read_text(encoding="utf-8", errors="replace").splitlines()[-30:]
                fail_lines.append("```")
                fail_lines.extend(tail)
                fail_lines.append("```")
            fail_lines.append("")

    (out_dir / "failure-report.md").write_text("\n".join(fail_lines) + "\n", encoding="utf-8")
    print(f"Wrote qa-report.md ({len(rows)} checks) and failure-report.md ({len(failures)} failures).")


if __name__ == "__main__":
    main()
