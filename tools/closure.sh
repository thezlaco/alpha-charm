#!/usr/bin/env bash
#
# Measures how much of the tree the Android browser actually needs.
#
# This asks gn for the answer rather than guessing it. `gn gen` reads every
# BUILD file reachable from a target, expands the templates with the arguments
# each call site passes, and writes a build graph. It compiles nothing, so it
# costs seconds and a fraction of the disk a build costs; the two questions it
# answers are the ones a static reader cannot, namely which targets are reached
# at all and which files each of them contributes.
#
# The result is three numbers, because they are not the same question:
#
#   targets    how many build targets the browser is made of
#   inputs     how many files those targets name
#   closure    how much of the repository that is, as a share of the tree
#
# Usage:
#   tools/closure.sh [output-directory]
#
# Environment:
#   CHARM_OUT_DIR       build output, relative to the repository (default out/Closure)
#   CHARM_DEPOT_TOOLS   depot_tools to use, if not already on PATH

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${CHARM_OUT_DIR:-out/Closure}"
TARGET="//chrome/android:chrome_public_apk"

say() { printf '\n=== %s ===\n' "$1"; }
die() { printf '%s\n' "$1" >&2; exit 1; }

cd "$REPO_DIR"

[ -f .gn ] && [ -f BUILD.gn ] ||
  die "$REPO_DIR does not look like a Chromium checkout."

command -v gn >/dev/null 2>&1 ||
  die "gn not found. Run tools/fetch-deps.sh, which places it in the tree."

[ -x third_party/cpython3/host/bin/python3 ] ||
  die "third_party/cpython3 is absent. Run tools/fetch-deps.sh first."

say "gn gen ${OUT_DIR}"
# Regenerated every run: gn is incremental, and a stale graph would answer the
# question below about the wrong tree.
mkdir -p "$OUT_DIR"
cp config/args.gn "$OUT_DIR/args.gn"
gn gen "$OUT_DIR"

say "targets reachable from ${TARGET}"
# --all walks transitively, which is the whole point: the question is what the
# browser needs, not what the entry target names directly.
gn desc "$OUT_DIR" "$TARGET" deps --all > "$OUT_DIR/deps.txt"
grep -c . "$OUT_DIR/deps.txt" | xargs printf '%d targets\n'

say "files those targets name"
# `inputs` is the honest list: it is what the build would read, generated files
# included, rather than what a directory listing happens to contain.
gn ls "$OUT_DIR" --type=source --recurse > "$OUT_DIR/sources.txt" 2>/dev/null ||
  die "gn ls failed; the graph was generated but could not be walked."
grep -c . "$OUT_DIR/sources.txt" | xargs printf '%d source files\n'

say "share of the repository"
TOTAL=$(git ls-files | wc -l)
INPUTS=$(grep -c . "$OUT_DIR/sources.txt")
awk -v in="$INPUTS" -v tot="$TOTAL" \
  'BEGIN { printf "%d of %d tracked files, %.2f%%\n", in, tot, 100 * in / tot }'

printf '\nWrote %s/deps.txt and %s/sources.txt\n' "$OUT_DIR" "$OUT_DIR"
