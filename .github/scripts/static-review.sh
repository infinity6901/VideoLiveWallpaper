#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-static-review}"
mkdir -p "$OUT_DIR"
REPORT="${OUT_DIR}/static-review.txt"

{
  echo "# Static Review Hints (Informational)"
  echo
  echo "## 1. Forbidden legacy APIs (lockCanvas on wallpaper surface, com.example namespace)"
  grep -rnE 'lockCanvas\(|com\.example\.' app/src/main/ || echo "None found (clean)."
  echo
  echo "## 2. Hardcoded TODO / FIXME markers"
  grep -rnE 'TODO|FIXME' app/src/main/ || echo "None found (clean)."
  echo
  echo "## 3. Broad exception swallows (empty catch blocks)"
  grep -rnE 'catch\s*\([^)]*\)\s*\{\s*\}' app/src/main/ || echo "None found (clean)."
  echo
  echo "## 4. Source file line counts"
  find app/src -type f -name '*.kt' -exec wc -l {} + | sort -n
} > "$REPORT"

cat "$REPORT"
