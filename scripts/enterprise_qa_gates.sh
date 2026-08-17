#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

fail() {
    echo "FAIL: $*" >&2
    exit 1
}

run() {
    echo "==> $*"
    "$@"
}

command -v rg >/dev/null 2>&1 || fail "ripgrep is required for enterprise static gates"

run sh gradlew :videosdk:compileDebugKotlin
run sh gradlew :app:assembleDebug
run git diff --check

echo "==> Checking SDK manifest for sensitive permissions"
if rg -n 'SYSTEM_ALERT_WINDOW|WRITE_SETTINGS' videosdk/src/main/AndroidManifest.xml; then
    fail "Sensitive permissions must not be shipped from the SDK manifest"
fi

echo "==> Checking SDK source for force unwraps"
if rg -n '!!' videosdk/src/main/java/com/app/videosdk; then
    fail "Force unwraps are not allowed in enterprise SDK source"
fi

echo "==> Checking SDK source for direct Android Log usage"
if rg --glob '!**/SdkLogger.kt' -n 'android\.util\.Log|Log\.[diewv]\(' videosdk/src/main/java/com/app/videosdk; then
    fail "Direct Android Log usage must route through SdkLogger"
fi

echo "==> Checking enterprise release readiness status is explicit"
if ! rg -n 'Status: `(NOT_READY_FOR_ENTERPRISE_RELEASE|READY_FOR_ENTERPRISE_RELEASE)`' SDK_ENTERPRISE_RELEASE_READINESS.md >/dev/null; then
    fail "Enterprise readiness doc must declare a release decision"
fi

echo "PASS: enterprise automated gates completed"
