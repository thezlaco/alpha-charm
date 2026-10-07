# Charm

Charm is an Android browser being reconstructed from Chromium with a different
goal: not to preserve Chromium's structure, but to make the browser smaller,
simpler, more deliberate, and fully under our control.

Chromium is the starting point. It is not the architecture we are required to
keep.

Charm exists because a browser should not need an ever-growing pile of
historical layers, duplicated systems, platform baggage, accidental
abstractions, and build-time machinery merely because they accumulated over
years.

The goal is not to make Chromium look cleaner.

**The goal is to build Charm.**

## The idea

Modern Chromium is an enormous project.

That size is understandable: it supports many platforms, products,
configurations, experiments, compatibility requirements, development tools,
testing systems, and years of accumulated design decisions.

But Charm does not need to remain a universal Chromium implementation.

It is an Android browser.

That changes the question completely.

Instead of asking:

> «How do we modify Chromium without breaking its existing structure?»

Charm asks:

> «What is actually necessary for this browser, and what should its architecture look like if we are willing to change the structure itself?»

That means Charm is not built around preserving every existing boundary.

- A directory is not sacred.
- A class is not sacred.
- A target is not sacred.
- An abstraction is not sacred.
- A subsystem is not sacred.

If two systems are actually one concept split across historical boundaries, they
should be candidates for becoming one system.

If a layer only forwards data between two other layers, it should justify its
existence.

If an entire subsystem exists only because another historical subsystem once
existed, it should be questioned.

If something is not part of the browser we are building, it should not
automatically remain merely because Chromium contains it.

## From Chromium to Charm

Charm is currently kept as a full Chromium source tree rather than as a small
patch set.

This is deliberate.

Chromium's source is preserved in the repository, and Charm's changes are
ordinary commits on top of that pinned revision. This gives the project a
complete, inspectable starting point while allowing the architecture itself to
change over time.

The important distinction is this:

- The repository may begin as Chromium.
- The resulting architecture does not have to remain Chromium.

Charm is therefore better understood as a reconstruction based on Chromium
source than as a conventional downstream fork.

## What we are trying to achieve

### Architectural simplification

Reduce unnecessary conceptual separation.

Merge systems that represent the same thing. Remove wrappers whose only purpose
is historical layering. Replace accidental complexity with explicit ownership
and smaller dependency surfaces.

The objective is not fewer files for its own sake.

**The objective is fewer independent things that need to exist.**

### Measurable efficiency

Charm should use fewer resources to build and run.

That includes:

- source-tree size
- dependency-graph size
- number of compiled inputs
- intermediate build size
- peak memory usage
- build time
- final native binary size
- final APK size
- runtime overhead

These are separate measurements. Improving one does not automatically improve
all of the others.

### Android focus

Charm is an Android browser.

Desktop-only implementations, alternative platform layers, unnecessary
configurations, redundant backends, and other parts of the universal Chromium
environment are not automatically justified merely because they exist upstream.

The target is a browser optimized for the platform it actually runs on.

### Deliberate control

Charm should not inherit behavior simply because it happens to be the Chromium
default.

Build configuration, experiments, feature switches, dependencies, and browser
behavior should be explicit and inspectable wherever practical.

The project favors deterministic source and build configuration over accidental
remote or inherited behavior.

### Long-term maintainability

Optimization that produces an unreadable mess is not considered success.

Charm should become simpler as a consequence of understanding the system better,
not by turning it into an unmaintainable pile of special cases.

## The 5 GB problem

A current Charm checkout is approximately:

| Metric | Current |
| --- | --- |
| Source tree | ~5.5 GB |
| Tracked files | ~509,570 |
| Typical build working set | ~40 GB |
| Recommended RAM | ~32 GB |

These numbers describe the current Chromium-derived tree, not the intended size
of Charm.

One of the project's long-term targets is to reduce the source tree from roughly
5.5 GB to around 300 MB.

That is a target, not a claim that Charm has already achieved it.

The purpose of this target is not to win a compression contest.

It is a way to force a much more fundamental question:

> «Why does the browser need the rest?»

A 94% reduction cannot realistically come from formatting changes, isolated
micro-optimizations, or thousands of tiny cleanups.

It requires understanding the structure of the system itself.

## Source size is not APK size

Charm treats these as different problems:

```text
Chromium source
      ↓
relevant source
      ↓
GN target graph
      ↓
Android dependency closure
      ↓
compiled objects
      ↓
native binaries/resources
      ↓
APK
```

A file can exist in the repository without being part of the final browser.

A target can exist without being required by the Android application.

A small source file can introduce a very large dependency.

A large collection of source files can be irrelevant to the final APK.

For this reason, Charm does not use source-tree size as a substitute for
understanding the build graph.

The project aims to measure all of these layers independently.

## Meta-refactoring

Most refactoring operates locally:

```text
class A
class B
class C
    ↓
better class relationships
```

Charm also works at a higher level:

```text
folder
subsystem
target
dependency cluster
platform layer
        ↓
understand whether they should exist separately at all
```

This is meta-refactoring.

A major Charm refactor may therefore involve an entire directory hierarchy,
several GN targets, multiple classes, or a complete subsystem boundary.

The questions are:

**Duplicate** — Are two parts of the tree implementing substantially the same concept?

**Shadow** — Does one system reproduce another system's behavior with a slightly different interface or configuration?

**Wrapper** — Is a layer primarily forwarding calls, state, or data without owning meaningful behavior?

**Historical residue** — Does this code still exist mainly because the architecture used to look different?

**False separateness** — Are several independently named components actually one conceptual system?

**Unnecessary dependency** — Does a component depend on something only because of an accidental implementation detail?

**Unnecessary generality** — Is a universal abstraction being maintained even though Charm only needs one concrete case?

The aim is not to delete everything.

**The aim is to make every remaining boundary intentional.**

## Engineering principles

### Understand before deleting

Large-scale removal should be based on dependency evidence, not on the
appearance of a directory.

### Delete before abstracting

If two systems exist only because of unnecessary complexity, creating a new
abstraction on top of both does not solve the problem.

Prefer removing the distinction where the distinction itself is not valuable.

### Merge before wrapping

Do not preserve unnecessary architecture by introducing another compatibility
layer.

### Measure before optimizing

Source size, dependency count, compile inputs, build memory, build time, binary
size, and APK size are different metrics and should be measured separately.

### Abstractions must earn their existence

An abstraction should communicate a real invariant, ownership boundary, or
reusable concept.

An abstraction created only to make code look symmetrical is not automatically
an improvement.

### Shared does not mean universal

Not every dependency shared by two components belongs in a giant parameter
object or common base class.

Shared structure should be extracted when it represents a real relationship.

### Platform differences should be real

Android-specific behavior should not be hidden behind layers whose only purpose
is to imitate another platform.

### Preserve behavior deliberately

Refactoring is not allowed to silently change browser behavior merely because
the new structure looks cleaner.

When behavior changes are intended, they should be explicit.

### Keep the architecture visible

A system should not require following six layers of delegation to discover which
component actually owns a piece of behavior.

Ownership should be easy to find.

## Current state

Charm is still early in development.

The current tree is Chromium-derived, and the project has already begun testing
the methodology on the extensions toolbar and related Android UI infrastructure.

Current changes include:

- extensions toolbar availability on phone layouts
- removal of an unnecessary `ToolbarTablet` dependency from common toolbar construction
- centralized width arbitration
- phone-side participation in extension width allocation
- consolidation of duplicated toolbar lifecycle handling
- replacement of fragile positional dependency passing with explicit parameter objects
- stronger state modeling around extension actions
- shared factories for repeated menu-button construction
- cleanup of unnecessary mediator/coordinator dependencies
- dependency on interfaces rather than concrete implementations where the boundary is real
- a repository-local build entry point
- build workflow groundwork
- cleanup of repository/build assumptions inherited from earlier iterations

These changes are not presented as the final architecture.

They are evidence that the architecture can be changed.

The project is now moving from local refactoring toward larger-scale
architectural analysis.

## What Charm is not

Charm is not intended to be:

- Chromium with a different icon
- a collection of cosmetic patches
- a build wrapper around an untouched Chromium tree
- a fork that blindly follows every upstream architectural decision
- an optimization project that counts lines changed instead of measuring outcomes
- a rewrite performed for the sake of rewriting
- a random collection of deletions justified by source-tree size alone

Charm is also not committed to preserving an abstraction merely because it is
established upstream.

Upstream correctness and Charm architecture are different concerns.

## What we will not inherit blindly

Chromium contains many systems that are valuable in its own environment but not
automatically necessary for Charm.

That includes, depending on the subsystem:

- platform-specific implementations Charm does not use
- alternative backends
- experimental features
- historical compatibility layers
- duplicated implementations
- development-only infrastructure
- test-only dependencies
- generated or auxiliary material not required by the browser
- generic abstractions whose cost is disproportionate to their value

This does not mean that Charm will remove components simply because they look
large.

Every removal must answer:

1. What does this provide?
2. Who depends on it?
3. Does Charm actually need that behavior?
4. Can the behavior be provided by something simpler?
5. What new dependency does removing it create?
6. Does the removal improve the resulting system, rather than merely moving complexity somewhere else?

## Build configuration

`config/args.gn` is the project's primary build configuration.

The configuration is kept separate from device-specific values and
version-specific release metadata so that a valid Charm build remains portable
across supported Android environments.

The project prefers explicit configuration over accidental inherited defaults.

Some Chromium flags are deceptive optimization targets: disabling a feature that
appears unrelated to Android can break compilation because the underlying
feature gate is also used elsewhere.

Such cases are treated as dependency-graph problems, not as opportunities for
blind flag deletion.

Build-size optimizations must therefore be verified against the actual target
graph.

## Building

Charm currently follows Chromium's Android build system.

A basic checkout looks like:

```bash
git clone --depth=1 https://github.com/thezlaco/alpha-charm.git
cd alpha-charm

./build/install-build-deps.sh --android
tools/build.sh
```

`tools/build.sh` is intended to build the repository's configured Android
browser target and report the resulting APK.

Chromium's complete source/dependency setup is substantially larger than a
normal Android project, so a successful Charm build requires the appropriate
Chromium dependency synchronization and host resources.

Build infrastructure is still being developed alongside the browser itself.

### Build resource goals

Build resource usage is treated as a first-class engineering problem.

The project tracks:

- checkout size
- build directory size
- peak RAM
- CPU time
- compiled input count
- linker memory
- final native binary size
- final APK size

A change is not considered a build optimization merely because one command
becomes shorter or a configuration file becomes smaller.

The result must be measured.

## Android package identity

Charm uses:

```text
zlaco.charm
```

as its Android application id.

The package name is part of the application's identity and is therefore treated
as a deliberate project-level decision rather than as a temporary build setting.

## Upstream Chromium

Charm is based on a pinned Chromium revision rather than a moving branch.

The current revision recorded by the project is:

```text
ac9b84a0b3
Chromium 153
2 September 2026
```

A pinned revision ensures that a Charm commit refers to a stable source tree.

Updating Chromium is therefore an explicit operation:

```text
new Chromium revision
        ↓
inspect upstream changes
        ↓
re-evaluate Charm architecture
        ↓
repair or replace affected Charm changes
        ↓
validate the resulting tree
```

Charm does not treat upstream architectural evolution as something that must
automatically become Charm architecture.

Upstream is a source of improvements, fixes, and implementations.

**It is not the project's design authority.**

## Development workflow

Charm favors changes that produce a visible architectural result.

A useful change should ideally make one or more of these things better:

- fewer concepts
- fewer dependencies
- clearer ownership
- fewer duplicated implementations
- smaller dependency closure
- less generated/build machinery
- less platform baggage
- smaller compilation surface
- better runtime behavior
- lower build resource usage
- smaller output

The repository should not accumulate refactors that exist only because
refactoring feels productive.

The intended direction is:

```text
observe
  ↓
measure
  ↓
understand
  ↓
identify the real boundary
  ↓
remove / merge / simplify
  ↓
measure again
```

## Architecture over patch count

Charm does not measure progress by the number of commits or lines deleted.

A 2-line change that removes an unnecessary architectural dependency can be more
valuable than a 2,000-line cleanup.

Likewise, deleting 50 files is not progress if the same complexity simply
reappears elsewhere.

**The real unit of progress is reduced system complexity.**

## Long-term direction

The project is moving toward a progressively smaller and more deliberate browser
architecture.

A simplified view of the intended process is:

```text
Chromium
   ↓
inventory
   ↓
dependency graph
   ↓
Android browser closure
   ↓
remove irrelevant systems
   ↓
merge duplicate concepts
   ↓
remove historical layers
   ↓
simplify build graph
   ↓
measure
   ↓
repeat
```

The long-term goal is not merely a smaller Chromium checkout.

**It is a browser whose size and architecture can be explained.**

For every major subsystem, Charm should eventually be able to answer:

- «Why does this exist?»
- «What does it own?»
- «What depends on it?»
- «Why is it separate?»
- «Can it be simpler?»

That standard is more important than any particular number.

The ~300 MB source-tree target is a forcing function for reaching that standard.

## Philosophy

The browser should be understandable as a system.

Not because every line must be simple.

Because the relationships between the important parts should make sense.

Charm therefore prefers:

```text
one concept        → one clear owner
one responsibility → one meaningful boundary
one dependency     → one justified reason
```

rather than:

```text
historical layer
    ↓
compatibility layer
    ↓
adapter
    ↓
delegate
    ↓
mediator
    ↓
wrapper
    ↓
actual implementation
```

when those layers do not represent distinct concepts.

The objective is not minimal code.

**It is minimal unnecessary architecture.**

## Privacy and control

Charm is built from source that the project controls.

That gives the project the ability to inspect, modify, remove, or retain
behavior intentionally rather than depending on an opaque downstream packaging
process.

This does not mean the browser magically becomes offline or server-free.

Normal browser functionality can still require network services.

The distinction is that Charm aims to keep what the browser is built to do under
explicit project control, rather than treating remote configuration or inherited
defaults as unquestionable parts of the product.

Experimental behavior that can otherwise be injected through Chromium's testing
configuration is disabled where appropriate so that the resulting behavior is
determined by the source and build configuration.

## Licensing

Charm is based on Chromium.

The Chromium source is licensed under the BSD 3-Clause license, and Chromium's
original licensing and attribution files are retained in the repository.

Charm also includes Chromium's third-party dependencies, each of which retains
its own applicable license.

See:

- [`LICENSE`](LICENSE)
- [`NOTICE`](NOTICE)
- Chromium's original documentation
- `about:credits` in the built browser

## Repository structure

This repository intentionally retains Chromium's source layout while Charm's
architecture is being reconstructed.

Over time, that structure is expected to change.

Directories should therefore be considered implementation history and evidence,
not permanent architectural boundaries.

The intended future state is a tree whose organization reflects the architecture
Charm actually has, rather than the historical architecture it started from.

## Status

Charm is experimental and under active architectural development.

It should currently be viewed as:

```text
Chromium-derived
        +
early Charm changes
        +
ongoing architectural reconstruction
```

The project is deliberately not pretending that the final architecture already
exists.

The current code is part of the process of discovering it.

## The goal

The final objective can be stated simply:

> «Take a browser that became enormous through years of accumulation, understand what is actually necessary, and build a smaller system whose complexity is deliberate rather than inherited.»

Charm is not trying to win by having the most patches.

**It is trying to win by having less unnecessary system.**

## Upstream

Chromium: <https://www.chromium.org>

The original Chromium README is preserved as
[`README.chromium.md`](README.chromium.md).

Chromium documentation is available under [`docs/`](docs/).

## License

BSD 3-Clause

Charm is a modification of Chromium and includes third-party components under
their respective licenses.