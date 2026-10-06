# Charm

An Android browser built from Chromium source, kept under our own control.

This repository is a full copy of Chromium rather than a patch set applied
before each build. Chromium's history is here, and Charm's changes are ordinary
commits on top of it. Building this tree produces Charm.

## Why we build it ourselves

The browser we install is not ours. It is assembled elsewhere, updated on someone
else's schedule, and it can be made to change its behaviour remotely without
anything appearing in an update note. The same goes for the source: we can read
a patch, but we cannot change the thing the patch produces.

Building from source removes that. Everything the browser does is in this
repository, as code we can read, change and refuse to change. Nothing reaches a
server that decides what it will do tomorrow.

That is the project. The individual commits are just what we have decided to
change so far.

## What we have changed

| Commit | Effect |
| --- | --- |
| `Show the extensions toolbar on phone layouts` | Makes the extensions popup, pinning and per-site permissions reachable at phone widths |

One change so far, and it is not the interesting part of the project. It is
recorded here because it is what currently differs from upstream, not because it
defines what Charm is.

## How the extensions change works

Recorded because it is the one change made so far, and because understanding it
is the pattern to follow for the rest.

Two separate things kept the extensions toolbar out of the phone layout, and
both had to go.

`ToolbarManager` constructs the extensions toolbar coordinator only when it
finds `R.id.extensions_toolbar_container_stub` among the children of the control
container. That stub was declared in `toolbar_tablet.xml` only, so on a phone the
coordinator was never created at all.

Declaring the stub in `toolbar_phone.xml` is not sufficient on its own. The
coordinator was handed the toolbar after a hard cast to `ToolbarTablet`, which
threw `ClassCastException` during startup on a phone, because a phone toolbar is
not a tablet toolbar. The constructor only requires a `ViewGroup`, and
`ToolbarLayout` — the common base of `ToolbarPhone` and `ToolbarTablet` — already
is one, so the cast was never load-bearing.

## What we will not accept from upstream

Chromium's experimental desktop-Android build is what makes extension support
possible here at all, and it is not something we can rebuild ourselves. Two
things about it are worth knowing:

**Field trials.** A Chromium build that is not Google-branded compiles in
`fieldtrial_testing_config.json` and follows whichever experiment arm that file
names, instead of the defaults in the code. That is a server-supplied behaviour
switch. `disable_fieldtrial_testing_config = true` in `config/args.gn` turns it
off, which keeps behaviour in the binary rather than on a server.

**The build flags that look free.** `enable_arcore`, `enable_cardboard` and
`enable_openxr` appear to be free size savings. They are not: `enable_vr` is
derived from them, and an Android build needs `ENABLE_VR` for reasons that have
nothing to do with VR, because the omnibox declares `GetVectorIcon` behind it.
Switching the backends off breaks omnibox compilation rather than removing
anything.

## Building

`config/args.gn` is the whole build configuration. It carries no device-specific
values and no version numbers, so a build from it installs on whatever Android
supports and keeps working as Android moves on.

```bash
git clone --depth=1 https://github.com/thezlaco/alpha-charm.git
cd alpha-charm

./build/install-build-deps.sh --android
tools/build.sh
```

`tools/build.sh` compiles `chrome_public_apk` and prints the path to the APK.

### What a build costs

Chromium is large. Measured, not quoted:

| | |
| --- | --- |
| Source in this repository | 5.5 GB, 509 570 files |
| Disk while compiling | about 40 GB, the rest being intermediate objects and generated sources |
| RAM | 32 GB recommended; linking is what needs it |
| CPU time | several hours |

GitHub's free runners give 4 vCPU, 16 GB of RAM and 14 GB of disk, short of all
three, so the included workflow needs a larger runner to finish.

### Host dependencies

Chromium ships `build/install-build-deps.sh`, which knows the package list for
the host it is on. Run it rather than installing packages by hand — a
hand-written list goes stale the moment a distribution renames a package, and
did.

## Identity

The application id is `zlaco.charm`, set in `config/args.gn`. Android treats the
package name as the app's identity, so changing it later produces a second
install rather than an update.

## Licensing

This repository is Chromium, which is BSD-3-Clause, licensed by The Chromium
Authors. `LICENSE` is theirs and is unmodified. `NOTICE` records that Charm is a
modification of it.

A Charm build also bundles Chromium's third-party components under their own
licenses, enumerated in the browser itself under `about:credits`.

## Upstream

Chromium is at https://www.chromium.org. Its own README is preserved as
[`README.chromium.md`](README.chromium.md), and its documentation is under
`docs/`.

## Updating Chromium

Charm tracks a pinned revision rather than a branch, so that a given commit
always yields the same source. To move forward: fetch the new upstream revision
into this tree, read what changed underneath our commits, and fix our commits so
they apply to the new code.

Current revision: `ac9b84a0b3`, Chromium 153, 2 September 2026. The reasoning
for pinning rather than tracking a branch is in `docs/decisions.md`.