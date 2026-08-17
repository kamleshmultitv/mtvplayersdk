#!/usr/bin/env sh
set -eu

ADB=${ADB:-adb}
LOG_TAG=${LOG_TAG:-MtvVideoPlayerSdk}
OUT_FILE=${1:-"/tmp/mtv_sdk_log_redaction_$(date +%Y%m%d_%H%M%S).log"}

adb_cmd() {
    if [ -n "${ANDROID_SERIAL:-}" ]; then
        "$ADB" -s "$ANDROID_SERIAL" "$@"
    else
        "$ADB" "$@"
    fi
}

fail() {
    echo "FAIL: $*" >&2
    echo "Captured log: $OUT_FILE" >&2
    exit 1
}

command -v rg >/dev/null 2>&1 || fail "ripgrep is required"
command -v "$ADB" >/dev/null 2>&1 || fail "adb is required"

echo "Capturing SDK logs for tag: $LOG_TAG"
adb_cmd logcat -d -v time "$LOG_TAG:D" "*:S" > "$OUT_FILE"

if [ ! -s "$OUT_FILE" ]; then
    fail "No SDK logs captured. Enable SdkLoggingConfig(level = SdkLogLevel.DEBUG), exercise playback, then rerun."
fi

echo "Checking for unredacted URLs with query strings"
if rg -q 'https?://[^[:space:],;)"]+\?[^<[:space:],;)"]' "$OUT_FILE"; then
    fail "Found URL query strings that are not redacted"
fi

echo "Checking for unredacted sensitive key/value pairs"
if rg -q -i '(^|[?&;[:space:],])([a-z0-9_.-]*(token|license|adtag|url|authorization|auth|payload|signature|secret|password|bearer|apikey|api_key|accesskey|access_key|session|cookie)[a-z0-9_.-]*)=[^<[:space:],;)"]+' "$OUT_FILE"; then
    fail "Found sensitive key/value fields that are not redacted"
fi

echo "Checking for common raw credential markers"
if rg -q -i 'Authorization:|Bearer [A-Za-z0-9._~+/=-]{12,}|X-Api-Key|X-Auth-Token' "$OUT_FILE"; then
    fail "Found credential-like values in SDK logs"
fi

echo "PASS: device SDK logs did not expose raw query strings, tokens, license URLs, ad tags, or credential markers"
echo "Captured log: $OUT_FILE"
