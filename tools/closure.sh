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
# The result is four numbers, because they are not the same question:
#
#   targets     how many build targets the browser is made of
#   inputs      how many files those targets name, generated ones included
#   in tree     how many of those files the repository actually contains
#   closure     the last number as a share of the committed tree
#
# The third is the one that answers the question removal decisions rest on. An
# input that ninja reports may be a source file in this repository, a file a
# build step generates into the output directory, or part of the toolchain; the
# last two are not in the repository and cannot be deleted from it, so counting
# them would inflate the figure being used to decide what to cut.
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

# gn is named outright rather than looked up on PATH, because depot_tools also
# ships something called gn: a shell wrapper that runs gn.py under depot_tools'
# own bootstrap python. Any depot_tools directory on PATH ahead of the real
# binary shadows it with that wrapper, and the wrapper refuses to run in a
# checkout that has not been bootstrapped:
#
#     python3_bin_reldir.txt not found. need to initialize depot_tools
#
# which is what happened here, since Chromium's tree carries its own copy of
# depot_tools at third_party/depot_tools. The binary is the one DEPS puts in
# buildtools/linux64/gn, guarded by host_os == "linux".
#
# Appended rather than prepended, so that whatever is already on PATH cannot
# shadow the binary. And set before gn is resolved rather than after, because the
# fallback below is a lookup on PATH, and a lookup made before these two
# directories were added cannot see them.
export PATH="$PATH:$REPO_DIR/buildtools/linux64/gn:$REPO_DIR/third_party/ninja"
[ -n "${CHARM_DEPOT_TOOLS:-}" ] && export PATH="$PATH:$CHARM_DEPOT_TOOLS"

GN="$REPO_DIR/buildtools/linux64/gn/gn"
[ -x "$GN" ] || GN="$(command -v gn || true)"
[ -n "$GN" ] && [ -x "$GN" ] ||
  die "gn not found at $REPO_DIR/buildtools/linux64/gn/gn, nor on PATH. Run tools/fetch-deps.sh first."

# The tree's own python, which .gn already names as script_executable. Used
# rather than the system one so that the counting agrees with gn on what the
# build scripts see.
PYTHON="$REPO_DIR/third_party/cpython3/host/bin/python3"
[ -x "$PYTHON" ] ||
  die "third_party/cpython3 is absent. Run tools/fetch-deps.sh first."

say "gn gen ${OUT_DIR}"
# Regenerated every run: gn is incremental, and a stale graph would answer the
# question below about the wrong tree.
mkdir -p "$OUT_DIR"
cp config/args.gn "$OUT_DIR/args.gn"
"$GN" gen "$OUT_DIR"

say "targets reachable from ${TARGET}"
# --all walks transitively, which is the whole point: the question is what the
# browser needs, not what the entry target names directly.
"$GN" desc "$OUT_DIR" "$TARGET" deps --all > "$OUT_DIR/deps.txt"
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

say "which of those the repository actually holds"
# Counting and reporting are both done here, rather than counting in python and
# printing from the shell. The previous version printed four bare numbers and the
# shell read them back by position, so a change in the order they were printed
# in silently attributed each figure to the wrong label, and there was nothing
# to notice that the figures did not add up.
#
# The two lists are a partition of the inputs, so they are reported together with
# the size of the partition they came from. If they do not account for all of it,
# that is said outright rather than left as two numbers that quietly disagree.
#
# Denominator is the committed tree rather than the index. `git ls-files`
# answers zero for a checkout made with --no-checkout, and a share against zero
# is not a share.
"$PYTHON" - "$REPO_DIR" "$OUT_DIR" <<'PYTHON'
import os
import subprocess
import sys

repo_dir, out_dir = os.path.abspath(sys.argv[1]), os.path.abspath(sys.argv[2])

with open(os.path.join(out_dir, "inputs.txt"), encoding="utf-8", errors="replace") as handle:
    inputs = [line for line in handle.read().splitlines() if line]

tracked = set(
    subprocess.run(
        ["git", "ls-tree", "-r", "--name-only", "HEAD"],
        cwd=repo_dir,
        capture_output=True,
        text=True,
        check=True,
    ).stdout.splitlines()
)

# The first few, unchanged, because what ninja actually prints is the thing being
# reasoned about here and it should not have to be guessed at from a count.
with open(os.path.join(out_dir, "inputs-sample.txt"), "w", encoding="utf-8") as handle:
    for line in inputs[:20]:
        handle.write(line + "\n")

in_tree, elsewhere = set(), set()
for path in inputs:
    # os.path.join returns the second argument outright when it is absolute, so an
    # input ninja already gave as an absolute path is handled by the same two
    # readings without a special case. Exactly one reading can name a tracked
    # file, so a file that matches is found whichever form ninja used, and a
    # generated file matches neither.
    for base in (out_dir, repo_dir):
        relative = os.path.relpath(os.path.normpath(os.path.join(base, path)), repo_dir)
        if relative in tracked:
            in_tree.add(relative)
            break
    else:
        elsewhere.add(path)

for name, paths in (("repo-inputs.txt", in_tree), ("non-repo-inputs.txt", elsewhere)):
    with open(os.path.join(out_dir, name), "w", encoding="utf-8") as handle:
        # No trailing newline for an empty set, so that `wc -l` on these files is
        # the count whether or not there is anything in them.
        handle.write("".join(path + "\n" for path in sorted(paths)))

print("%d build inputs read from ninja" % len(inputs))
print("%d of them files in this repository" % len(in_tree))
print("%d of them generated, or in the output directory or the toolchain" % len(elsewhere))
print("%d distinct inputs, since ninja can print one file twice" % len(set(inputs)))
print("%d tracked files in the repository" % len(tracked))

accounted = len(in_tree) + len(elsewhere)
if accounted != len(set(inputs)):
    print("these do not add up to the inputs: %d against %d" % (accounted, len(set(inputs))))

if tracked:
    print("%d of %d tracked files, %.2f%%" % (len(in_tree), len(tracked), 100 * len(in_tree) / len(tracked)))
else:
    print("no share: the checkout reported no tracked files to compare against")
PYTHON

printf '\nWrote %s/deps.txt, %s/inputs.txt, %s/repo-inputs.txt\n' "$OUT_DIR" "$OUT_DIR" "$OUT_DIR"
