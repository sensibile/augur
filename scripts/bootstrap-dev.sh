#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
YES=false
VERIFY=true
INSTALL_HOOKS=false

usage() {
  cat <<'EOF'
Usage: scripts/bootstrap-dev.sh [--yes] [--skip-verify] [--install-hooks]

Installs local development prerequisites:
  - Homebrew, when missing and explicitly confirmed
  - mise, through Homebrew
  - ktlint, through Homebrew
  - Java and detekt, through mise
  - Git hooks, when --install-hooks is passed

Options:
  --yes            answer yes to install prompts
  --skip-verify   skip final version checks
  --install-hooks configure Git to use .githooks for this repository
EOF
}

for arg in "$@"; do
  case "$arg" in
    --yes|-y)
      YES=true
      ;;
    --skip-verify)
      VERIFY=false
      ;;
    --install-hooks)
      INSTALL_HOOKS=true
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

confirm() {
  local prompt="$1"

  if [[ "$YES" == "true" ]]; then
    return 0
  fi

  read -r -p "$prompt [y/N] " reply
  [[ "$reply" == "y" || "$reply" == "Y" || "$reply" == "yes" || "$reply" == "YES" ]]
}

load_homebrew_env() {
  if [[ -x /opt/homebrew/bin/brew ]]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  elif [[ -x /usr/local/bin/brew ]]; then
    eval "$(/usr/local/bin/brew shellenv)"
  fi
}

ensure_homebrew() {
  load_homebrew_env

  if command -v brew >/dev/null 2>&1; then
    return 0
  fi

  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "Homebrew is not installed. Install it manually for this platform, then rerun this script." >&2
    exit 1
  fi

  if ! confirm "Homebrew is required for ktlint. Install Homebrew now?"; then
    echo "Aborted. Install Homebrew and rerun this script." >&2
    exit 1
  fi

  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  load_homebrew_env
}

ensure_mise() {
  if command -v mise >/dev/null 2>&1; then
    return 0
  fi

  ensure_homebrew

  if ! confirm "mise is required for Java and detekt. Install mise with Homebrew now?"; then
    echo "Aborted. Install mise and rerun this script." >&2
    exit 1
  fi

  brew install mise
}

ensure_ktlint() {
  ensure_homebrew

  if brew list ktlint >/dev/null 2>&1; then
    return 0
  fi

  brew install ktlint
}

ensure_mise
ensure_ktlint

cd "$ROOT_DIR"
if [[ "$YES" == "true" ]]; then
  mise trust .mise.toml --yes
else
  mise trust .mise.toml
fi
mise install

if [[ "$INSTALL_HOOKS" == "true" ]]; then
  git config core.hooksPath .githooks
  chmod +x .githooks/pre-commit
fi

if [[ "$VERIFY" == "true" ]]; then
  java -version
  ktlint --version
  detekt --version
  ./gradlew help --no-daemon >/dev/null
fi
