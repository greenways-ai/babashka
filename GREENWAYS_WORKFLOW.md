# Greenways Babashka Automated Workflow

This document describes the automated workflow for syncing upstream `babashka.core` changes and releasing a custom build with IFn and IPersistentMap support.

## Overview

The workflow automatically:
1. Monitors `https://github.com/babashka/babashka.core.git` for changes to the `main` branch
2. When changes are detected, syncs them to this repository
3. Applies the `greenways/fn` branch changes (IFn and IPersistentMap support for `deftype`)
4. Bumps the version and creates a new release

## Repository Structure

- **Upstream**: `babashka/babashka` - Main babashka repository
- **Submodule**: `babashka/babashka.core` - Core protocols and interfaces
- **This repo**: `greenways-ai/babashka` - Fork with IFn/IPersistentMap support
- **Feature branch**: `greenways/fn` - Contains the custom modifications

## Custom Modifications (greenways/fn branch)

The following files add IFn and IPersistentMap support for `deftype`:

| File | Description |
|------|-------------|
| `impl-java/src-java/babashka/impl/SciFn.java` | IFn implementation for deftype |
| `impl-java/src-java/babashka/impl/SciMapFn.java` | IPersistentMap implementation for deftype |
| `src/babashka/impl/deftype.clj` | Updated deftype to support IFn and IPersistentMap |
| `src/babashka/impl/classes.clj` | Additional classes for native image |
| `src/babashka/main.clj` | Main entry point updates |
| `test/babashka/deftype_ifn_test.clj` | Tests for IFn/IPersistentMap support |

## Workflows

### 1. Sync Upstream Workflow (`.github/workflows/sync-upstream.yml`)

Runs every hour to check for upstream changes.

**Triggers:**
- Schedule: Every hour (`0 * * * *`)
- Manual: Via GitHub Actions UI
- Optional: Repository dispatch from upstream

**Process:**
1. Check if `babashka.core` submodule has new commits on `main`
2. If new commits exist (or `--force` is used):
   - Update the submodule to latest `main`
   - Rebase `greenways/fn` branch on `master`
   - Merge `greenways/fn` into `master`
   - Bump version (patch/minor/major)
   - Create git tag
   - Push changes
   - Trigger build workflow

### 2. Build Workflow (`.github/workflows/build.yml`)

Builds native binaries for macOS (Intel and Apple Silicon).

**Triggers:**
- Push to `master`
- Push of version tags (`v*`)
- Pull requests to `master`
- Manual trigger

**Jobs:**
- **native**: Builds native binaries on macOS (Intel + ARM)
- **create-release**: Creates GitHub Release with artifacts (on tag push)

## Usage

### Manual Sync

To manually trigger a sync:

```bash
# Using the script locally
script/sync-upstream

# With options
script/sync-upstream --version-bump minor --force
```

### Via GitHub Actions

1. Go to **Actions** → **Sync Upstream and Release**
2. Click **Run workflow**
3. Optional: Set `force` to `true` to sync even without upstream changes
4. Optional: Set `version_bump` (patch/minor/major)

## Versioning

Versions follow the pattern: `{upstream-version}-greenways.{build}`

Examples:
- `1.12.218-greenways` - Patch bump
- `1.13.0-greenways` - Minor bump  
- `2.0.0-greenways` - Major bump

## Required Secrets

The following secrets should be configured in GitHub:

| Secret | Description |
|--------|-------------|
| `GITHUB_TOKEN` | Automatically provided, used for releases |
| `GH_PAT` | (Optional) Personal Access Token with repo scope for pushing changes |

## Local Development

### Setup

```bash
# Clone with submodules
git clone --recursive git@github.com:greenways-ai/babashka.git
cd babashka

# Or if already cloned
git submodule update --init --recursive
```

### Build Locally

```bash
# Build uberjar
script/uberjar

# Build native image (requires GRAALVM_HOME)
export GRAALVM_HOME=/path/to/graalvm
script/compile

# Run tests
script/test
```

### Update Submodule Manually

```bash
cd babashka.core
git checkout main
git pull origin main
cd ..
git add babashka.core
git commit -m "chore: update babashka.core submodule"
```

## Troubleshooting

### Merge Conflicts

If the sync workflow fails due to merge conflicts:

1. Clone the repo locally
2. Checkout the feature branch: `git checkout greenways/fn`
3. Rebase on master: `git rebase master`
4. Resolve conflicts manually
5. Force push: `git push origin greenways/fn --force`
6. Re-run the sync workflow

### Failed Build

Check the build workflow logs in GitHub Actions. Common issues:
- GraalVM version mismatch
- Missing native dependencies
- Test failures

## Architecture Diagram

```
┌─────────────────────────────────┐
│  babashka/babashka.core (main)  │
│     (Upstream Repository)       │
└───────────────┬─────────────────┘
                │ Push to main
                ▼
┌─────────────────────────────────┐
│  Scheduled Check (every hour)   │
│  .github/workflows/sync-        │
│       upstream.yml              │
└───────────────┬─────────────────┘
                │ New commits detected
                ▼
┌─────────────────────────────────┐
│   Update babashka.core          │
│   submodule to latest main      │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│   Rebase greenways/fn on        │
│   master, merge to master       │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│   Bump version & create tag     │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│   Trigger Build Workflow        │
│   .github/workflows/build.yml   │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│   Build Native Binaries         │
│   (macOS Intel + ARM)           │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│   Create GitHub Release         │
│   with Artifacts                │
└─────────────────────────────────┘
```

## Support

For issues with this workflow, please open an issue in the `greenways-ai/babashka` repository.
