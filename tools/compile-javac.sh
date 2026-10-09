#!/usr/bin/env bash
#
# Compiles the Java half of the browser, and nothing else.
#
# tools/closure.sh answers which files the build needs, by asking gn and by
# walking the graph ninja was given. Neither of those compiles anything, so a
# change that leaves the graph perfectly intact can still fail to compile: a
# method removed from a class because a subclass defined it, an override whose
# interface still declares it, a type used before it is declared. None of that
# shows up in a count of files.
#
# That matters here more than it would in an ordinary checkout, because this
# tree is edited without a compiler anywhere in the loop. A whole browser takes
# hours and tens of gigabytes to build, which makes it a poor instrument for
# deciding whether the last edit is sound. The Java half takes minutes.
#
# It asks for chrome_java__compile_java rather than the APK. That target is an
# android_library, so gn has already resolved every dependency it needs, and the
# suffix narrows the work to the compile step: no dexing, no packaging, no
# resource crunching, and none of the C++ the browser is mostly made of. The
# classes it produces are the ones dex would consume, so a Java error here is
# the same error that a full build would report.
#
# Usage:
#   tools/compile-javac.sh
#
# Environment:
#   CHARM_OUT_DIR       build output, relative to the repository (default out/Closure)
#   CHARM_DEPOT_TOOLS   depot_tools to use, if not already on PATH

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${CHARM_OUT_DIR:-out/Closure}"

# Not //chrome/android:chrome_public_apk. The APK pulls in dex, packaging and
# every C++ target in the browser, which is the six hour problem this script
# exists to avoid. This target is Java only, and its label is what gn emits, so
# ninja and gn agree on the spelling.
TARGET="chrome/android:chrome_java__compile_java"

say() { printf '\n=== %s ===\n' "$1"; }
die() { printf '%s\n' "$1" >&2; exit 1; }

cd "$REPO_DIR"

[ -f .gn ] && [ -f BUILD.gn ] ||
  die "$REPO_DIR does not look like a Chromium checkout."

# Same resolution as tools/closure.sh, and for the same two reasons: depot_tools
# ships a wrapper called gn that shadows the real binary and refuses to run in an
# unbootstrapped checkout, and ninja and gn come from DEPS rather than from the
# machine.
export PATH="$PATH:$REPO_DIR/buildtools/linux64/gn:$REPO_DIR/third_party/ninja"
[ -n "${CHARM_DEPOT_TOOLS:-}" ] && export PATH="$PATH:$CHARM_DEPOT_TOOLS"

GN="$REPO_DIR/buildtools/linux64/gn/gn"
[ -x "$GN" ] || GN="$(command -v gn || true)"
[ -n "$GN" ] && [ -x "$GN" ] ||
  die "gn not found at $REPO_DIR/buildtools/linux64/gn/gn, nor on PATH. Run tools/fetch-deps.sh first."

NINJA="$REPO_DIR/third_party/ninja/ninja"
[ -x "$NINJA" ] || NINJA="$(command -v ninja || true)"
[ -n "$NINJA" ] ||
  die "ninja not found. It comes from third_party/ninja via tools/fetch-deps.sh."

say "gn gen ${OUT_DIR}"
# Regenerated like in closure.sh: gn is incremental, and a graph describing the
# previous commit would answer about the wrong tree. The same output directory is
# shared so that one gn gen serves both tools.
mkdir -p "$OUT_DIR"
cp config/args.gn "$OUT_DIR/args.gn"
"$GN" gen "$OUT_DIR"

say "the target exists"
# Checked rather than assumed, because the failure this guards against is
# silent: a target that does not exist matches nothing, and ninja answers that
# nothing matched by exiting successfully. Without this, a rename of the target
# would look exactly like a tree that compiles.
"$GN" ls "$OUT_DIR" "$TARGET" > /dev/null ||
  die "gn does not know $TARGET, so there is nothing to compile."

say "compile ${TARGET}"
# -k 0, so that one run reports every failure rather than the first. The
# opposite was correct once, when the tree was believed sound and a hundred
# errors were assumed to be one mistake wearing a hundred hats: then the first
# was all there was to read. It is wrong now, because the tree had files taken
# out of it, and one absent file fails an edge while every edge that would have
# reached it is never attempted. Stopping at the first turns one run into one
# finding; keeping going turns it into the whole list, which is the difference
# between repairing the tree in a few passes and in a few hundred.
#
# The status is left to matter. ninja still exits nonzero when any edge failed,
# which is what should fail this workflow: a Java error is a mistake to be fixed
# before it lands, not a number for a human to interpret.
#
# Not a dry run either. `ninja -n` would enumerate the same edges and prove
# nothing about Java, which is the whole reason this script exists.
"$NINJA" -C "$OUT_DIR" -k 0 "$TARGET"

say "the Java half compiles"