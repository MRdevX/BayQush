#!/usr/bin/env bash
set -euo pipefail

version="${1:?usage: bump-android-version.sh <semver>}"

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version: $version" >&2
  exit 1
fi

IFS=. read -r major minor patch <<< "$version"
code=$((major * 10000 + minor * 100 + patch))
if (( code < 1 )); then
  code=1
fi

properties="gradle.properties"
tmp="$(mktemp)"
awk -v ver="$version" -v code="$code" '
  /^VERSION_NAME=/ { print "VERSION_NAME=" ver; next }
  /^VERSION_CODE=/ { print "VERSION_CODE=" code; next }
  { print }
' "$properties" > "$tmp"
mv "$tmp" "$properties"
