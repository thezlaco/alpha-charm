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
#   tools/closure.sh
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

# gclient puts the toolchain into the tree rather than onto PATH, because a
# Chromium build must use the clang and ninja of its own pinned revision. gn
# lands in buildtools/linux64/gn/gn per DEPS, and ninja in
# third_party/ninja/ninja, so both directories are added rather than assumed.
[ -n "${CHARM_DEPOT_TOOLS:-}" ] && export PATH="$CHARM_DEPOT_TOOLS:$PATH"
export PATH="$REPO_DIR/third_party/depot_tools:$PATH"
export PATH="$REPO_DIR/buildtools/linux64/gn:$PATH"
export PATH="$REPO_DIR/third_party/ninja:$PATH"

command -v gn >/dev/null 2>&1 ||
  die "gn not found. Run tools/fetch-deps.sh, which puts it in the tree."

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
printf '%d targets\n' "$(grep -c . "$OUT_DIR/deps.txt")"

say "files the build would read"
# Asked of ninja rather than of gn, because gn describes targets and ninja
# describes the graph it was given. `ninja -t inputs` walks the graph and prints
# every file that reaches the target, generated files included, without
# compiling anything. Listing the graph with `gn ls` would name the targets, not
# their files, and it has no recursive walk over sources at all.
NINJA="$REPO_DIR/third_party/ninja/ninja"
[ -x "$NINJA" ] || NINJA="$(command -v ninja || true)"
[ -n "$NINJA" ] ||
  die "ninja not found. It comes from third_party/ninja via tools/fetch-deps.sh."

# gn identifies a target by `//path:name`; ninja knows the same target by
# `path:name`, the form gn itself prints and the form every gn-generated
# build.ninja uses. Passing the `//` form to ninja matches nothing at all.
NINJA_TARGET="${TARGET#//}"
"$NINJA" -C "$OUT_DIR" -t inputs "$NINJA_TARGET" > "$OUT_DIR/inputs.txt"
printf '%d input files\n' "$(grep -c . "$OUT_DIR/inputs.txt")"

say "share of the repository"
# Counted from the committed tree rather than from the index. `git ls-files`
# answers zero when the checkout was made without one, and this repository is
# checked out with actions/checkout, which does create an index, but the count
# has to mean the same thing either way. The tree is the thing being measured.
TOTAL=$(git ls-tree -r --name-only HEAD | wc -l)
INPUTS=$(grep -c . "$OUT_DIR/inputs.txt")
# Not `in`: that is a gawk keyword, and awk exits before printing anything.
awk -v inputs="$INPUTS" -v total="$TOTAL" \
  'BEGIN { printf "%d of %d tracked files, %.2f%%\n", inputs, total, 100 * inputs / total }'

printf '\nWrote %s/deps.txt and %s/inputs.txt\n' "$OUT_DIR" "$OUT_DIR"
