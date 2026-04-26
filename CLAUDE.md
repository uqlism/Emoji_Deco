# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RunicInk** is a Minecraft Forge mod (1.20.1 / Forge 47.4.10) that adds Markdown-style rich text formatting and a custom shortcode/emoji system to Minecraft's text rendering.

Supported locations: chat, signs, item tooltips, entity name tags, and written books.

## Build & Run Commands

```bash
# Build the mod jar
./gradlew build

# Run Minecraft client with the mod loaded
./gradlew runClient

# Run Minecraft server with the mod loaded
./gradlew runServer

# Run data generation (outputs to src/generated/resources/)
./gradlew runData

# Generate IDE run configurations
./gradlew genIntellijRuns   # IntelliJ IDEA
./gradlew genEclipseRuns    # Eclipse

# Clean build artifacts
./gradlew clean

# Refresh Gradle dependency cache
./gradlew --refresh-dependencies
```

There are no unit tests; functional testing requires running `runClient` or `runGameTestServer`.

## Architecture

### Core Text Pipeline

Text flows through three stages:

1. **`RichTextParser`** — Parses raw strings into Minecraft `Component` trees, handling inline Markdown syntax (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`) and resolving `:shortcode:` tokens via `ShortcodeManager`.

2. **`ComponentTransformer`** — Walks existing `Component` trees recursively and applies the parser to leaf nodes. Used when the text is already a structured Component (e.g., item tooltips).

3. **Mixin injection points** — Six mixins in `src/main/java/com/uqlism/runicink/mixin/` intercept Minecraft internals to run the pipeline at the right rendering moments:
   - `MixinChatScreen` — chat input and live preview
   - `MixinSignRenderer` — sign block text
   - `MixinEntityRenderer` — entity name tags
   - `MixinWrittenBookAccess` — written books
   - `MixinGui` / `MixinFontSet` — GUI-level font patches

### Shortcode System

Shortcodes are defined as JSON files under `src/main/resources/assets/runicink/shortcodes/`. Each file maps a name (`:name:`) to a Minecraft Component definition (text, font, color, or sprite reference). `ShortcodeManager` loads these at startup and on resource reload (`PreparableReloadListener`).

### Sprite Rendering

Custom sprites are registered in `SpriteRegistry` and referenced from shortcode JSON. Glyph metadata is in `SpriteGlyphInfo`. The custom font is declared in `assets/runicink/font/sprite.json`.

### Configuration

`Config.java` (Forge config) exposes per-location toggles: `enableChat`, `enableSigns`, `enableItemNames`, `enableEntityNames`, `enableBooks` (all default `true`).

## Key Files

| File | Purpose |
|---|---|
| `RunicInk.java` | `@Mod` entry point, registers config and event buses |
| `text/RichTextParser.java` | Markdown + shortcode parser |
| `text/ComponentTransformer.java` | Recursive Component tree transformer |
| `text/ShortcodeManager.java` | JSON shortcode loader / resolver |
| `client/ClientEvents.java` | Item tooltip event handler |
| `runicink.mixins.json` | Mixin class list (must be updated when adding new mixins) |
| `gradle.properties` | Mod version, MC version, Forge version |

## Adding a New Shortcode

Create `src/main/resources/assets/runicink/shortcodes/<name>.json` following the pattern of `heart.json` or `stone.json`. No Java changes required — `ShortcodeManager` discovers all JSON files in that directory automatically.

## Adding a New Mixin

1. Create the mixin class in `src/main/java/com/uqlism/runicink/mixin/`.
2. Register it in `src/main/resources/runicink.mixins.json` under the appropriate `"mixins"` or `"client"` array.
