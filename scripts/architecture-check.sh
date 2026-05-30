#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() {
  echo "architecture-check: $*" >&2
  exit 1
}

dependencies_for() {
  local module="$1"
  local configuration="$2"
  local build_file="$ROOT_DIR/$module/build.gradle.kts"

  [[ -f "$build_file" ]] || fail "missing build file for $module"

  sed -n "s/^[[:space:]]*$configuration(project(\":\\([^\"]*\\)\"))$/\\1/p" "$build_file"
}

assert_production_dependencies() {
  local module="$1"
  shift
  local allowed=("$@")
  local dependencies=()

  while IFS= read -r dependency; do
    dependencies+=("$dependency")
  done < <({
    dependencies_for "$module" "api"
    dependencies_for "$module" "implementation"
  } | sort -u)

  for dependency in "${dependencies[@]:-}"; do
    local permitted=false
    for allowed_dependency in "${allowed[@]:-}"; do
      if [[ "$dependency" == "$allowed_dependency" ]]; then
        permitted=true
        break
      fi
    done

    if [[ "$permitted" != "true" ]]; then
      fail "$module has forbidden production dependency on $dependency"
    fi
  done
}

assert_no_module_imports() {
  local module="$1"
  shift
  local forbidden_imports=("$@")
  local source_dir="$ROOT_DIR/$module/src/main/kotlin"

  [[ -d "$source_dir" ]] || return 0

  for forbidden_import in "${forbidden_imports[@]}"; do
    if grep -R --line-number --fixed-strings "import $forbidden_import" "$source_dir" >/dev/null; then
      fail "$module main source imports forbidden package $forbidden_import"
    fi
  done
}

assert_production_dependencies "augur-rule-core"
assert_production_dependencies "augur-rule-json" "augur-rule-core"
assert_production_dependencies "augur-rule-sdk" "augur-rule-core"

assert_no_module_imports "augur-rule-core" \
  "me.sensibile.augur.rule.json" \
  "me.sensibile.augur.rule.sdk"
assert_no_module_imports "augur-rule-json" \
  "me.sensibile.augur.rule.sdk"
assert_no_module_imports "augur-rule-sdk" \
  "me.sensibile.augur.rule.json"

echo "architecture-check: ok"
