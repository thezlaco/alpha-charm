#!/usr/bin/env bash
#
# Fetches the packages that DEPS names but git does not carry.
#
# Chromium's tree is one git repository plus several hundred neighbouring ones.
# DEPS lists them, and gclient is what checks them out, because they are
# binaries, generated code, or whole subprojects with their own histories. This
# repository is the main repository, so its checkout is complete as a git tree
# and incomplete as a build tree: third_party/cpython3, the Android NDK,
# android_deps, ninja and the rest are absent by design, not by mistake.
#
# Reading config/args.gn or editing BUILD files does not need them. `gn gen`
# does, because it resolves the toolchain and script_executable through those
# paths. Nothing here compiles anything.
#
# Usage:
#   tools/fetch-deps.sh
#
# Environment:
#   CHARM_DEPS_ARGS   extra arguments for gclient sync

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_NAME="$(basename "$REPO_DIR")"
WORKSPACE_DIR="$(dirname "$REPO_DIR")"
DEPOT_TOOLS="${CHARM_DEPOT_TOOLS:-$WORKSPACE_DIR/depot_tools}"

if [ ! -d "$DEPOT_TOOLS" ]; then
  echo "depot_tools missing at $DEPOT_TOOLS." >&2
  echo "Clone it first:" >&2
  echo "  git clone --depth=1 https://chromium.googlesource.com/chromium/tools/depot_tools.git" >&2
  exit 1
fi

[ -f "$REPO_DIR/DEPS" ] || {
  echo "No DEPS in $REPO_DIR: this does not look like the Chromium checkout." >&2
  exit 1
}

# gclient locates a workspace by finding .gclient, and each DEPS path is written
# relative to the solution root, which gclient calls "src". Naming the solution
# after the directory it actually occupies keeps those paths landing inside this
# repository, where the BUILD files expect them, instead of in a sibling named
# src.
CLIENT="$WORKSPACE_DIR/.gclient"
if [ ! -f "$CLIENT" ]; then
  cat > "$CLIENT" <<EOF
solutions = [
  {
    "name": "$REPO_NAME",
    "url": "https://github.com/thezlaco/alpha-charm.git",
    "managed": False,
    "deps_file": "DEPS",
    "custom_vars": {},
  },
]
EOF
  echo "wrote $CLIENT"
fi

export PATH="$DEPOT_TOOLS:$PATH"
cd "$WORKSPACE_DIR"

# Android is a target rather than a default. gclient turns target_os into
# build/config/gclient_args.gni, and DEPS reads checkout_android from it to
# decide what to fetch: 73 packages, among them the NDK, the Android SDK and
# android_deps. Without it gclient syncs happily and leaves a tree that cannot
# resolve its toolchain, which is a worse failure than not running at all.
#
# `gclient config` rather than a line in the file above, because this is what
# tools/fetch android does, and because it updates an existing .gclient instead
# of requiring the caller to know whether there was one.
gclient config --name="$REPO_NAME" --add-target_os=android

# --no-history because none of these packages' histories are wanted: gclient is
# here for their contents, and their histories are most of what they cost.
gclient sync --no-history ${CHARM_DEPS_ARGS:-}

echo
echo "Dependencies fetched into $REPO_NAME/third_party."
