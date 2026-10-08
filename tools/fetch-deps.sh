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

# The checkout has to be in a directory named src. Every path in DEPS is
# written against a solution called src, and gclient resolves a DEPS path
# against the solution whose name prefixes it. A solution named after the
# repository matches none of those paths, so every dependency lands in a
# sibling directory called src that no BUILD file refers to, and the hooks fail
# on a file they expect inside this checkout. It is a naming convention the
# whole tree depends on rather than a preference.
[ "$(basename "$REPO_DIR")" = "src" ] || {
  echo "This checkout is in $(basename "$REPO_DIR"), and it has to be in src." >&2
  echo "Move it, or clone it into a directory of that name:" >&2
  echo "  git clone https://github.com/thezlaco/alpha-charm.git /some/where/src" >&2
  exit 1
}

# gclient locates a workspace by finding .gclient, which therefore belongs to the
# directory containing src rather than to src itself. The solution is named src
# and is left unmanaged, because it is this repository and it is already checked
# out: gclient is here for the other several hundred of them.
CLIENT="$WORKSPACE_DIR/.gclient"
if [ ! -f "$CLIENT" ]; then
  cat > "$CLIENT" <<EOF
solutions = [
  {
    "name": "src",
    "url": "https://github.com/thezlaco/alpha-charm.git",
    "managed": False,
    "deps_file": "DEPS",
    "custom_vars": {},
  },
]
EOF
  echo "wrote $CLIENT"
fi

# Android is a target rather than a default. gclient turns target_os into
# build/config/gclient_args.gni, and DEPS reads checkout_android from it to
# decide what to fetch: 73 packages, among them the NDK, the Android SDK and
# android_deps. Without it gclient syncs happily and leaves a tree that cannot
# resolve its toolchain, which is a worse failure than not running at all.
#
# Top level, because that is where gclient reads it from: it selects the keys
# of a DEPS file's deps_os and hooks_os dicts, so it describes the workspace
# rather than any one solution. There is no gclient command that sets it;
# `gclient config --add-target_os` does not exist, and asking for it stops the
# run with "no such option".
if ! grep -q '^target_os' "$CLIENT"; then
  cat >> "$CLIENT" <<'EOF'
target_os = [ "android" ]
EOF
  echo "added target_os to $CLIENT"
fi

export PATH="$DEPOT_TOOLS:$PATH"
cd "$WORKSPACE_DIR"

# --no-history because none of these packages' histories are wanted: gclient is
# here for their contents, and their histories are most of what they cost.
#
# --nohooks because the hooks are Chromium's tooling for their own review and
# build infrastructure: landmines, clobber, tast, reclient, lastchange. They are
# Python scripts that import each other, and keeping a tree buildable with them
# means keeping their whole transitive import tail, which is a set nobody wrote
# down and which grows by import. Charm builds an APK and does not run Chromium's
# bots, so the scripts are not run.
#
# What that costs is knowable rather than guessed. Nothing fetched by a hook is
# fetched, so if one of them turned out to produce something the build needs, the
# first gn gen or ninja run will say so, and that hook can be re-enabled on
# purpose. CHARM_DEPS_ARGS cannot switch them back on, since this flag is
# unconditional; removing it from here is the way to do that.
gclient sync --no-history --nohooks ${CHARM_DEPS_ARGS:-}

echo
echo "Dependencies fetched into src/third_party."
