# freemem

An IntelliJ Platform plugin that reclaims IDE heap memory without a restart.

freemem watches the IDE's heap usage in the background. When usage crosses a
threshold it safely flushes open documents, drops PSI caches across open
projects, closes idle editor tabs, and runs garbage collection. It also includes
a live status bar indicator and a settings UI.

## Features

- **Automatic reclaim** — checks heap usage on a fixed schedule (every 2 minutes
  by default) and frees memory once usage reaches the threshold (85% of max
  heap by default).
- **Emergency LowMemoryWatcher** — hooks into IntelliJ's platform memory watcher
  to trigger immediate reclamation when the IDE detects native JVM low-memory events.
- **Manual reclaim** — **Tools → Free IDE Memory (freemem)** or `Ctrl+Alt+Shift+M`
  runs memory reclamation immediately with detailed balloon feedback.
- **Status bar widget** — shows live heap % and MB in the bottom status bar
  (`freemem: 68% (1400/2048MB)`). Clicking it opens a menu to reclaim memory
  now or open freemem settings.
- **Settings UI** — set the IDE's maximum heap (`-Xmx`, applied after restart)
  and configure thresholds, intervals, cache drops, tab closing, and notification
  preferences under **Settings → Tools → freemem** (also **Tools → freemem Settings…**).
- **Smart idle-tab closing** — tracks true editor tab access times (LRU) via
  `FileEditorManagerListener` and safely closes unmodified tabs that have been
  inactive beyond the configured cutoff.

## Compatibility

| | |
|---|---|
| IDEs | All IntelliJ Platform IDEs (depends only on `com.intellij.modules.platform`) |
| Builds | `241` – `262.*` (2024.1 – 2026.2) |
| Built against | IntelliJ IDEA Community 2024.1 |
| JDK | 17 |

`runPluginVerifier` reports the plugin as compatible with 2024.1 through 2025.3.

## Installation

### From disk

1. Build the plugin (see below), or use a released zip.
2. In the IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. Select `build/distributions/freemem-1.2.0.zip` (the zip, not the project folder).
4. Restart the IDE.

## Configuration

Settings can be managed directly in the IDE via **Settings / Preferences → Tools → freemem**.
Preferences are persisted to `freemem.xml` in the IDE's options directory.

| Setting | Default | Meaning |
|---|---|---|
| `enableAutoReclaim` | `true` | Enable background heap monitoring and automatic reclamation |
| `thresholdPercent` | `85` | Heap usage (% of max heap) that triggers a reclaim |
| `checkIntervalMinutes` | `2` | How often heap usage is checked (minimum 1 minute) |
| `dropPsiCaches` | `true` | Drop PSI syntax tree and indexing caches across open projects |
| `closeIdleTabs` | `false` | Close inactive, unmodified editor tabs during a reclaim |
| `idleTabMinutes` | `30` | Inactivity cutoff in minutes for idle tab closing |
| `notifyOnAutoReclaim` | `false` | Show balloon notification when automatic reclaim runs |

The background check starts the first time a project is opened.

## Building

Requirements: JDK 17 and network access to Maven Central, the Gradle Plugin
Portal, and the JetBrains repositories. The Gradle wrapper (Gradle 8.13) is
included. Gradle 9 is not supported by `org.jetbrains.intellij` 1.17.x.

```bash
./gradlew buildPlugin
```

Output: `build/distributions/freemem-1.2.0.zip`

Other useful tasks:

| Task | Purpose |
|---|---|
| `./gradlew runIde` | Launch a sandbox IDE with the plugin installed |
| `./gradlew runPluginVerifier` | Check binary compatibility against recent IDE releases |
| `./gradlew signPlugin` | Produce `freemem-1.2.0-signed.zip` (needs signing variables) |
| `./gradlew publishPlugin` | Upload to JetBrains Marketplace (needs `PUBLISH_TOKEN`) |

Sandbox IDE logs are written to `build/idea-sandbox/system/log/idea.log`.

## Signing and publishing

`signPlugin` reads these environment variables. It is skipped silently if they
are not set.

| Variable | Contents |
|---|---|
| `CERTIFICATE_CHAIN` | Contents of the certificate chain (`chain.crt`) |
| `PRIVATE_KEY` | Contents of the private key (`private_encrypted.pem`) |
| `PRIVATE_KEY_PASSWORD` | Passphrase for the private key |

Keep the key files outside the repository. The first Marketplace release must
be uploaded by hand at [plugins.jetbrains.com](https://plugins.jetbrains.com);
later releases can use `publishPlugin` with a `PUBLISH_TOKEN` generated on your
Marketplace profile. Increase `pluginVersion` in `gradle.properties` for every
release.

## Project layout

```
src/main/kotlin/com/edigitalhub/freemem/
  FreeMemAction.kt                 Tools menu & shortcut action for manual reclaim
  FreeMemConfigurable.kt           Settings UI (Tools -> freemem) incl. -Xmx editor, via Kotlin UI DSL
  FreeMemSettingsAction.kt         Tools menu action that opens the settings page
  FreeMemService.kt                App service: heap check, LowMemoryWatcher, and reclaim
  FreeMemSettingsState.kt          Persisted configuration and settings listener topic
  FreeMemStartup.kt                Starts FreeMemService when a project opens
  FreeMemStatusBarWidget.kt        Real-time status bar memory indicator & click menu
  FreeMemStatusBarWidgetFactory.kt Status bar widget factory
  FreeMemTabTracker.kt             LRU tab access tracker via FileEditorManagerListener
src/main/resources/META-INF/plugin.xml
```

## Known limitations

- The plugin ID and vendor (`com.yourco.freemem`, *YourCo*) are placeholders and
  must be replaced before publishing to Marketplace.
