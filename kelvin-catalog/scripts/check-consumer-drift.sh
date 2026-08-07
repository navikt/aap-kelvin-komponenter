#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <path-to-consumer-libs.versions.toml>" >&2
  exit 2
fi

managed_file="$(cd "$(dirname "$0")/.." && pwd)/gradle/libs.versions.toml"
consumer_file="$1"

if [[ ! -f "$consumer_file" ]]; then
  echo "Consumer catalog not found: $consumer_file" >&2
  exit 2
fi

extract_aliases() {
  local file="$1"
  awk '
    /^\[versions\]/ { section="versions"; next }
    /^\[libraries\]/ { section="libraries"; next }
    /^\[bundles\]/ { section="bundles"; next }
    /^\[plugins\]/ { section="plugins"; next }
    /^\[/ { section="other"; next }
    section != "other" && $0 ~ /^[a-zA-Z0-9._-]+[[:space:]]*=/ {
      key=$0
      sub(/[[:space:]]*=.*/, "", key)
      print section ":" key
    }
  ' "$file" | sort -u
}

managed_aliases="$(mktemp)"
consumer_aliases="$(mktemp)"
common_aliases="$(mktemp)"
trap 'rm -f "$managed_aliases" "$consumer_aliases" "$common_aliases"' EXIT

extract_aliases "$managed_file" > "$managed_aliases"
extract_aliases "$consumer_file" > "$consumer_aliases"
comm -12 "$managed_aliases" "$consumer_aliases" > "$common_aliases" || true

if [[ ! -s "$common_aliases" ]]; then
  echo "No drift detected."
  exit 0
fi

echo "Drift detected: consumer redefines aliases already managed in kelvin-catalog:"
cat "$common_aliases"
echo
echo "Recommended action: remove overlapping aliases from consumer catalog and use kelvinLibs aliases instead."
exit 1
