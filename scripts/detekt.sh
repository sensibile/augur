#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v detekt >/dev/null 2>&1; then
  echo "detekt is not installed. Run scripts/bootstrap-dev.sh first." >&2
  exit 127
fi

cd "$ROOT_DIR"

inputs=()
while IFS= read -r input; do
  inputs+=("$input")
done < <(
  find . \
    \( -path './.gradle' -o -path './build' -o -path '*/build' \) -prune \
    -o -type d \( -path '*/src/main/kotlin' -o -path '*/src/test/kotlin' \) \
    -print | sed 's#^\./##' | sort
)

if [[ "${#inputs[@]}" -eq 0 ]]; then
  echo "No Kotlin source sets found for detekt."
  exit 0
fi

input_paths="$(IFS=:; echo "${inputs[*]}")"
report_dir="$ROOT_DIR/build/reports/detekt"
mkdir -p "$report_dir"

args=(
  --input "$input_paths"
  --build-upon-default-config
  --report "html:$report_dir/detekt.html"
  --report "sarif:$report_dir/detekt.sarif"
)

if [[ -f "$ROOT_DIR/detekt.yml" ]]; then
  args+=(--config "$ROOT_DIR/detekt.yml")
fi

detekt "${args[@]}"
