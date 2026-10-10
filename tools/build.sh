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

# A build that dies halfway leaves behind a number nobody wrote down: the disk it
# needed is only visible while it is running, since the object files it has
# already produced are exactly what it still has to keep. So it is sampled while
# it runs rather than looked up afterwards.
#
# The figure matters more than it looks. Disk, not time and not memory, is what
# a Chromium build runs out of first, and it is what decides which machine the
# build can run on at all. A build measured at 38 GB is a build that cannot run
# on a 14 GB runner; the same build measured at 9 GB could. Reporting the peak
# turns "the build did not fit" into "the build needed N", which is the number
# that has to come down.
#
# Held in a file rather than a shell variable because the sampler is a background
# loop, and a subshell cannot widen the scope of its parent's variables. Writing
# to a file is also what lets the number survive the build being killed.
PEAK_FILE="$(mktemp)"
# The file is seeded with a number, not left empty. mktemp creates a zero byte
# file, `cat` on it succeeds with no output, so a `|| echo 0` fallback never
# fires and the comparison below is handed an empty string, which bash rejects
# rather than treating as zero.
echo 0 > "$PEAK_FILE"
trap 'rm -f "$PEAK_FILE"' EXIT
sample_disk() {
  local used peak
  used=$(df -Pk "$REPO_DIR" | awk 'NR==2 {print $3}')
  peak=$(cat "$PEAK_FILE" 2>/dev/null || echo 0)
  [ -n "$peak" ] || peak=0
  [ -n "$used" ] || used=0
  [ "$used" -gt "$peak" ] && echo "$used" > "$PEAK_FILE"
}
sample_disk
(
  while true; do
    used=$(df -Pk "$REPO_DIR" | awk 'NR==2 {print $3}')
    peak=$(cat "$PEAK_FILE" 2>/dev/null || echo 0)
    [ -n "$peak" ] || peak=0
    [ -n "$used" ] || used=0
    [ "$used" -gt "$peak" ] && echo "$used" > "$PEAK_FILE"
    sleep 30
  done
) &
SAMPLER=$!

say "chrome_public_apk"
autoninja -C "$OUT_DIR" chrome_public_apk

kill "$SAMPLER" 2>/dev/null || true
sample_disk
PEAK_KB=$(cat "$PEAK_FILE" 2>/dev/null || echo 0)

APK="$REPO_DIR/$OUT_DIR/apks/ChromePublic.apk"
[ -f "$APK" ] || die "autoninja finished but $APK is not there."

printf '\nAPK: %s\n' "$APK"
ls -la "$APK"

say "what the build cost"
printf 'peak disk used on the volume holding the tree: %s\n' "$(awk -v k="$PEAK_KB" 'BEGIN {printf "%.1f GB", k/1048576}')"
printf 'that is the figure a machine has to have free before it starts\n'
