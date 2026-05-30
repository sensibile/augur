#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v ktlint >/dev/null 2>&1; then
  echo "ktlint is not installed. Run scripts/bootstrap-dev.sh first." >&2
  exit 127
fi

if [[ "$#" -gt 0 ]]; then
  ktlint -F "$@"
  exit 0
fi

files=()
while IFS= read -r file; do
  files+=("$file")
done < <(
  {
    git -C "$ROOT_DIR" diff --name-only --diff-filter=ACMR -- '*.kt' '*.kts'
    git -C "$ROOT_DIR" diff --cached --name-only --diff-filter=ACMR -- '*.kt' '*.kts'
    git -C "$ROOT_DIR" ls-files --others --exclude-standard -- '*.kt' '*.kts'
  } | sort -u
)

if [[ "${#files[@]}" -eq 0 ]]; then
  echo "No changed Kotlin files to format."
  exit 0
fi

cd "$ROOT_DIR"
ktlint -F "${files[@]}"
