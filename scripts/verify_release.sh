#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

export JAVA_HOME="${JAVA_HOME:-/opt/android-studio/jbr}"

./gradlew :app:lintDebug --console=plain --no-daemon
./gradlew :app:assembleDebug :app:assembleRelease --console=plain --no-daemon
