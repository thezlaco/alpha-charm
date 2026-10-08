#!/usr/bin/env bash
#
# Compiles Charm. Safe to run any number of times.
#
# This repository is the source tree, so there is nothing to materialise first:
# the script builds the checkout it lives in, using config/args.gn as the whole
# of the build configuration.
#
# Usage:
#   tools/build.sh
#
# Environment:
#   CHARM_OUT_DIR       build output, relative to the repository (default out/Charm)
#   CHARM_DEPOT_TOOLS   depot_tools to use, if not already on PATH

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${CHARM_OUT_DIR:-out/Charm}"

say() { printf '\n=== %s ===\n' "$1"; }
die() { printf '%s\n' "$1" >&2; exit 1; }

# A Chromium checkout is told apart from any other repository by the two files
# gn itself needs. Checking here means a wrong directory fails with an
# explanation instead of gn's own.
[ -f "$REPO_DIR/.gn" ] && [ -f "$REPO_DIR/BUILD.gn" ] ||
    die "$REPO_DIR does not look like a Chromium checkout: .gn or BUILD.gn is missing."

# depot_tools carries gn and autoninja. Anything already on PATH is trusted
# first, then the usual locations, so a machine that has it installed needs no
# configuration and one that does not is told exactly what to do.
[ -n "${CHARM_DEPOT_TOOLS:-}" ] && export PATH="$CHARM_DEPOT_TOOLS:$PATH"

if ! command -v gn >/dev/null 2>&1 || ! command -v autoninja >/dev/null 2>&1; then
    for candidate in "$REPO_DIR/depot_tools" "$REPO_DIR/../depot_tools" "$HOME/depot_tools"; do
        if [ -d "$candidate" ]; then
            export PATH="$candidate:$PATH"
            break
        fi
    done
fi

command -v gn >/dev/null 2>&1 && command -v autoninja >/dev/null 2>&1 ||
    die "gn and autoninja not found. Install depot_tools and either put it on PATH or
point CHARM_DEPOT_TOOLS at it: https://chromium.googlesource.com/chromium/tools/depot_tools.git"

cd "$REPO_DIR"

# Regenerated every run rather than stamped, because it depends on a file that
# changes whenever the configuration is edited. gn is incremental and costs
# seconds, so a stale graph is not worth the special case.
say "gn gen ${OUT_DIR}"
mkdir -p "$OUT_DIR"
cp "$REPO_DIR/config/args.gn" "$OUT_DIR/args.gn"
gn gen "$OUT_DIR"

say "chrome_public_apk"
autoninja -C "$OUT_DIR" chrome_public_apk

APK="$REPO_DIR/$OUT_DIR/apks/ChromePublic.apk"
[ -f "$APK" ] || die "autoninja finished but $APK is not there."

printf '\nAPK: %s\n' "$APK"
ls -la "$APK"
