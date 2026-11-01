# freemem

An IntelliJ Platform plugin that reclaims IDE heap memory without a restart.

freemem watches the IDE's heap usage in the background. When usage crosses a
threshold it runs garbage collection and drops PSI caches for every open
project. It can also be triggered by hand from the **Tools** menu.

## Features

- **Automatic reclaim** — checks heap usage on a fixed schedule (every 2 minutes
  by default) and frees memory once usage reaches the threshold (85% of max
  heap by default).
- **Manual reclaim** — **Tools → Free IDE Memory (freemem)** runs GC, drops the
  current project's PSI caches, and shows a balloon with how much was freed and
  the current heap usage.
- **Optional stale-tab closing** — off by default. See [Known limitations](#known-limitations).

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
3. Select `build/distributions/freemem-1.0.0.zip` (the zip, not the project folder).
4. Restart the IDE.

## Configuration

Settings are stored in `freemem.xml` in the IDE's config `options` directory.
There is no settings UI yet; to change a value, close the IDE and edit that file.

| Setting | Default | Meaning |
|---|---|---|
| `thresholdPercent` | `85` | Heap usage (% of max heap) that triggers a reclaim |
| `checkIntervalMinutes` | `2` | How often heap usage is checked. Must be greater than 0 |
| `dropPsiCaches` | `true` | Drop PSI caches for open projects during an automatic reclaim |
| `closeIdleTabs` | `false` | Close stale editor tabs during an automatic reclaim |
| `idleTabMinutes` | `30` | Age cutoff used by `closeIdleTabs` |

The background check starts the first time a project is opened.

## Building

Requirements: JDK 17 and network access to Maven Central, the Gradle Plugin
Portal, and the JetBrains repositories. The Gradle wrapper (Gradle 8.13) is
included. Gradle 9 is not supported by `org.jetbrains.intellij` 1.17.x.

```bash
./gradlew buildPlugin
```

Output: `build/distributions/freemem-1.0.0.zip`

Other useful tasks:

| Task | Purpose |
|---|---|
| `./gradlew runIde` | Launch a sandbox IDE with the plugin installed |
| `./gradlew runPluginVerifier` | Check binary compatibility against recent IDE releases |
| `./gradlew signPlugin` | Produce `freemem-1.0.0-signed.zip` (needs signing variables) |
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
src/main/kotlin/com/yourco/freemem/
  FreeMemService.kt        App service: scheduled heap check and reclaim
  FreeMemAction.kt         Tools menu action for a manual reclaim
  FreeMemSettingsState.kt  Persisted settings (freemem.xml)
  FreeMemStartup.kt        Starts FreeMemService when a project opens
src/main/resources/META-INF/plugin.xml
```

## Known limitations

- **`closeIdleTabs` does not track tab activity.** It closes open files that are
  read-only and whose file modification time is older than `idleTabMinutes`,
  not tabs that have gone unused.
- **Keyboard shortcut conflict.** The action's default shortcut,
  Ctrl+Shift+F12, is also IntelliJ's *Hide All Tool Windows*. Use the Tools
  menu or remap it under **Settings → Keymap**.
- **No settings UI.** Values must be edited in `freemem.xml`.
- **`checkIntervalMinutes` of 0 fails.** The scheduler rejects a non-positive
  interval.
- The plugin ID and vendor (`com.yourco.freemem`, *YourCo*) are placeholders and
  must be replaced before publishing to Marketplace.
