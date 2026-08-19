# SDK Observability

Phase 5 adds optional analytics events, diagnostics callbacks, and production-safe logging for Mtv Video Player SDK.

## Enable Analytics Events

```kotlin
val config = PlayerConfig(
    analyticsEnabled = true
)
```

Analytics events are delivered through:

```kotlin
override fun onAnalyticsEvent(event: PlayerAnalyticsEvent) {
    // Forward to your analytics backend.
}
```

Current event categories:

- `PLAYER_READY`
- `BUFFERING_STARTED`
- `BUFFERING_ENDED`
- `PLAY`
- `PAUSE`
- `PLAYBACK_COMPLETED`
- `SEEK_STARTED`
- `SEEK_COMPLETED`
- `QUALITY_CHANGED`
- `SUBTITLE_CHANGED`
- `PLAYBACK_SPEED_CHANGED`
- `FULLSCREEN_CHANGED`
- `PIP_CHANGED`
- `CAST_STATE_CHANGED`
- `CAST_ERROR`
- `AD_LOADED`
- `AD_STARTED`
- `AD_COMPLETED`
- `AD_ERROR`
- `MUTE_CHANGED`
- `ERROR`

## Enable Diagnostics

```kotlin
val config = PlayerConfig(
    diagnosticsEnabled = true
)
```

Diagnostics are delivered through:

```kotlin
override fun onDiagnosticEvent(event: PlayerDiagnosticEvent) {
    // Show in integration tooling or attach to support reports.
}
```

Diagnostics are intended for integration support. They include player errors, Cast failures, playback recovery actions, and track availability details.

## Enable SDK Logs

Logs are off by default:

```kotlin
PlayerConfig()
```

Enable logs only during integration or debugging:

```kotlin
val config = PlayerConfig(
    logging = SdkLoggingConfig(
        level = SdkLogLevel.DEBUG,
        tag = "MyAppVideoSdk"
    )
)
```

Available levels:

- `OFF`
- `ERROR`
- `INFO`
- `DEBUG`

## Redaction

SDK logs and event attributes are redacted before dispatch. The redaction layer protects:

- URLs and query strings.
- DRM/license URLs.
- DRM tokens.
- Ad tag URLs.
- Keys containing token, license, ad tag, URL, content ID, or content URL.

## Specific Callbacks

The SDK still exposes direct callbacks for common host-app behavior:

```kotlin
override fun onSeekStarted(positionMs: Long) {}
override fun onSeekCompleted(positionMs: Long) {}
override fun onQualityChanged(width: Int, height: Int, label: String?) {}
override fun onSubtitleChanged(language: String?, label: String?, enabled: Boolean) {}
override fun onPlaybackSpeedChanged(speed: Float) {}
```

These are additive and keep existing listener implementations source-compatible because every method has a default no-op implementation.
