# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Emoji & Deco** is a Minecraft Forge mod (1.20.1 / Forge 47.4.10) that adds a rich text system to Minecraft's text rendering. Features include a decorator syntax (`#bold[...]`), a parameterized shortcode system (`:heart:`, `:player.Steve:`), player head glyphs, sprite glyphs, escape sequences, and a graffiti block.

Supported locations: chat, signs, item tooltips, entity name tags, written books, GUI titles/actionbar, graffiti blocks.

## Build & Run Commands

```bash
./gradlew build                  # Build the mod jar
./gradlew runClient              # Run Minecraft client with mod
./gradlew runServer              # Run Minecraft server with mod
./gradlew test                   # Run unit tests (JUnit 5)
./gradlew runData                # Data generation
./gradlew genIntellijRuns        # Generate IntelliJ run configs
./gradlew clean
./gradlew --refresh-dependencies
```

## Architecture

### Text Pipeline

```
Raw string
   │
   ▼ RichTextParser.parse(raw)
RichNode (IR)
   │
   ├─▶ .toComponent()       → Minecraft Component  (chat / tooltip / entity / book)
   └─▶ RichNode.toSequence(font, node)
                             → FormattedCharSequence (sign / graffiti — honours scale/glow)
```

**`RichTextParser`** parses raw strings into a `RichNode` tree:
- `:shortcode:` and `:shortcode.arg1,arg2:` → `ShortcodeManager.resolve()`
- `#decorator[content]` and `#decorator.arg1,arg2[content]` → `DecoratorManager.resolve()`
- `\#`, `\:`, `\[`, `\]`, `\.`, `\,`, `\\` — backslash escape sequences

**`ComponentTransformer`** walks existing `Component` trees (e.g. incoming chat messages) and applies `RichTextParser` to `LiteralContents` and `TranslatableContents` args.

### RichNode (IR)

`RichNode` is a sealed interface with five node types:

| Node | Meaning |
|---|---|
| `Text(literal, style, children)` | Text fragment with optional style delta and sub-nodes |
| `Sized(scale, children)` | Size multiplier (honoured in `toSequence`, ignored in `toComponent`) |
| `Glowing(lightMode, children)` | Light override — `LightMode.GLOW`, `AMBIENT`, or `BYPASS` |
| `Sprite(atlas, sprite)` | Sprite glyph via `SPRITE_FONT` |
| `Head(username)` | Player head glyph pair via `HEAD_FONT` + `HEAD_OVERLAY_FONT` |

### Shortcode & Decorator JSON Format

Both live in `src/main/resources/assets/emoji_deco/`:
- `shortcodes/<name>.json` — loaded by `ShortcodeManager` as a resourcepack
- `decorators/<name>.json` — loaded by `DecoratorManager` as a resourcepack

**Display element types:**

| JSON key | Meaning |
|---|---|
| `{"emoji_deco:slot":{}}` | Slot placeholder (decorator only) |
| `{"emoji_deco:arg":{"index":N,"type":"...","default":...,"suggestions":...}}` | Positional argument |
| `{"emoji_deco:size":{"size":..., "contents":...}}` | Size wrapper |
| `{"emoji_deco:glow":{"glow":..., "contents":...}}` | Glow wrapper |
| `{"emoji_deco:sprite":{"atlas":"...","sprite":"..."}}` | Sprite glyph |
| `{"emoji_deco:player_head":{"player":"..."}}` | Player head |
| `{"emoji_deco:player_names":{}}` | Dynamic list of online player names |
| Standard MC fields | `text`, `color`, `bold`, `italic`, `strikethrough`, `underlined`, `font`, `extra` |

**Argument types:** `"string"` (default), `"boolean"`, `"integer"`, `"float"`. Args expand to a JSON primitive via `EmojiDecoComponentParser.expandDynamicProviders()` before structural parsing.

**`display` arrays** produce a sequence of nodes (e.g. `stone.json` combines text + sprite).

### FormattedCharSequence Wrappers (render/sequence/)

Used by `RichNode.toSequence()` to carry rendering metadata:

| Class | Responsibility |
|---|---|
| `ScaledSequence(inner, scale)` | Applies matrix scale in `MixinFont.drawInBatch` |
| `LightSequence(inner, mode)` | Overrides `packedLight` — `LightMode.GLOW`=0xF000F0, `AMBIENT`=restore original, `BYPASS`=pass-through |
| `ConcatSequence(parts)` | Concatenates multiple sequences, advancing X between them |

`MixinFont` intercepts `Font.drawInBatch` and `Font.width` at HEAD+RETURN to handle these wrappers and track ambient light depth for `LightMode.AMBIENT`.

### Custom Glyph Rendering

**Sprite glyphs** (`render/registry/SpriteRegistry`):
- Codepoints 0xE000+, font `emoji_deco:sprite`
- `MixinFontSet` resolves codepoints → `SpriteGlyphInfo` → `BakedGlyph` with atlas UV

**Player head glyphs** (`render/registry/PlayerHeadRegistry`):
- Codepoints 0xF000+, ONE codepoint per player used in TWO fonts
- `HEAD_FONT` (base face 8×8, advance=0) + `HEAD_OVERLAY_FONT` (hat 9×9, advance=9)
- Both fonts share the codepoint space; font identity determines base vs overlay
- `MixinBakedGlyph` shifts overlay glyphs to z=0.01f via `OverlayGlyphs.OVERLAYS`

### Graffiti Block

A placeable block that stores up to 10 lines of rich text. Placed with `GraffitiInkItem`. Rendered by `GraffitiRenderer` using `RichNode.toSequence()`.

### Autocomplete (SuggestionState)

`SuggestionState` is **per-screen** — instantiated via `SuggestionState.of(screen)` using a `WeakHashMap`. Each screen holds its own state. Trigger detection (`#` and `:`) respects backslash escapes. Arg completions are data-driven via the `"suggestions"` field in `emoji_deco:arg` specs.

### Configuration

`Config.java` exposes: `enableChat`, `enableSigns`, `enableItemNames`, `enableEntityNames`, `enableBooks`, `enableGui` (all default `true`).

## Package Structure

```
com.uqlism.emoji_deco/
├── text/              RichNode, RichTextParser, EmojiDecoComponentParser,
│                      ComponentTransformer, DecoratorManager, ShortcodeManager
├── render/
│   ├── glyphinfo/     SpriteGlyphInfo, HeadGlyphInfo
│   ├── registry/      SpriteRegistry, PlayerHeadRegistry
│   ├── sequence/      ScaledSequence, LightSequence, ConcatSequence, LightMode
│   └── OverlayGlyphs
├── client/            ClientEvents, SuggestionState, CompletionRenderer, …
│   ├── screen/        GraffitiEditScreen
│   └── renderer/      GraffitiRenderer
├── mixin/             All @Mixin classes
├── block/             GraffitiBlock, GraffitiBlockEntity, GraffitiAlignment
├── item/              GraffitiInkItem
└── network/           GraffitiUpdatePacket
```

## Key Files

| File | Purpose |
|---|---|
| `EmojiDeco.java` | `@Mod` entry point |
| `text/RichNode.java` | IR sealed interface + `toComponent()` + `toSequence()` |
| `text/RichTextParser.java` | Raw string → RichNode parser (decorators, shortcodes, escapes) |
| `text/EmojiDecoComponentParser.java` | JSON display element → RichNode converter |
| `text/DecoratorManager.java` | Decorator JSON loader / resolver |
| `text/ShortcodeManager.java` | Shortcode JSON loader / resolver |
| `render/registry/SpriteRegistry.java` | Sprite codepoint allocation |
| `render/registry/PlayerHeadRegistry.java` | Player head codepoint allocation (2 fonts) |
| `client/SuggestionState.java` | Per-screen autocomplete state |
| `emoji_deco.mixins.json` | Mixin class list (update when adding mixins) |
| `gradle.properties` | Mod/MC/Forge versions |

## Mixin Call Paths

各描画コンテキストがどの MC メソッドを経由し、どの Mixin で変換されるかの一覧。
新しい描画場所に対応する際や、バグの原因を追う際の起点として使う。

### GUI テキスト全般（ホットバーアイテム名・タイトル・サブタイトル・アクションバー・ツールチップ等）

```
GuiGraphics.drawString(Font, Component, ...)
GuiGraphics.drawCenteredString(Font, Component, ...)
Font.width(Component)          ← センタリング計算もここを通る
    └─ component.getVisualOrderText()
        └─ Language.getVisualOrder(FormattedText)   [SRG: m_5536_]
            └─ ★ MixinLanguage (@Inject HEAD, cancellable)
                └─ ComponentSequenceConverter.toSequence(font, component)
                    → 以降は FormattedCharSequence として描画
```

**補足**: `GuiGraphics.drawString(Component)` は Language を経由するため、
`Font.drawInBatch(Component, ...)` は呼ばれない。MixinFont のキャッチオールは効かない。

---

### チャットメッセージ

```
ChatComponent.addMessage() / rescaleChat()
    └─ ComponentRenderUtils.wrapComponents(FormattedText, width, Font)   [SRG: m_94005_]
        └─ ★ MixinChatComponent (@Redirect, method=m_240465_ / m_93795_)
            └─ ComponentSequenceConverter.toSequence(font, component)
                → DynamicFormattedCharSequence (動的コンテンツの場合、毎フレーム再評価)
```

**補足**: チャットは Language.getVisualOrder ではなく ComponentRenderUtils を使う独自経路。

---

### エンティティ名前タグ（3D 空間）

```
EntityRenderer.renderNameTag()   [SRG: m_7392_]
    └─ Font.drawInBatch(Component, x, y, ...)   [SRG: m_272077_]  ← GuiGraphics を使わず直呼び
        └─ ★ MixinFont.runicink$transformDrawBatchComponent (@Inject HEAD, cancellable)
            └─ ComponentSequenceConverter.computeNow(font, component)
                → Font.drawInBatch(FormattedCharSequence, ...)
```

**補足**: 3D レンダラーは GuiGraphics を使わないため Language 経路でなく直接 `m_272077_`。
エンティティ名前タグだけが `Font.drawInBatch(Component)` キャッチオールの実質的な対象。

---

### 看板（Sign）

```
SignRenderer → SignText.getRenderMessages(filtered)   [SRG: m_277130_]
    └─ ★ MixinSignText (@Inject HEAD, cancellable)
        └─ ComponentSequenceConverter.toSequence(font, line) × 4行
            → FormattedCharSequence[] として返却、以降は FCS 描画経路
```

Config: `Config.enableSigns`

---

### 本（Written Book）

```
BookViewScreen.WrittenBookAccess.getPage(index)   [SRG: m_7303_]
    └─ ★ MixinWrittenBookAccess (@Inject RETURN, cancellable)
        └─ ComponentTransformer.transform(component)
            → Component ツリーを変換して返す（font.split() 経路なので scale/glow 非対応）
```

Config: `Config.enableBooks` / 制限: `Sized`・`Glowing` は本では無効。

---

### ホバーツールチップ（emoji_deco:hover/text）

```
マウスホバー時
    └─ GuiGraphics.renderComponentHoverEffect(Font, Style, x, y)   [SRG: m_280304_]
        └─ ★ MixinGuiGraphics (@Inject HEAD, cancellable)
            └─ Style の HoverEvent から HoverRichContents を取得
                └─ RichNode.toSequence(font, node)
                    → GuiGraphics.renderTooltip(font, lines, x, y)
```

---

### 落書きブロック（Graffiti）

```
GraffitiRenderer.render()
    └─ RichNode.toSequence(font, node) を直接呼び出し（Mixin 不要）
        → ScaledSequence / LightSequence などを MixinFont で処理
```

---

### FormattedCharSequence レンダリング共通（MixinFont）

上記いずれの経路でも、最終的に `Font.drawInBatch(FormattedCharSequence, ...)` [SRG: `m_272191_`] が
呼ばれる。MixinFont はここで各カスタム FCS 型をハンドリングする:

| FCS 型 | 処理内容 |
|---|---|
| `DynamicFormattedCharSequence` | 毎フレーム `computeNow()` で再評価（アニメ対応） |
| `LightSequence` | `packedLight` を上書き（GLOW / AMBIENT / BYPASS） |
| `AffineSequence` | Matrix4f 変換を適用（scale・rotation・flip 等） |
| `ConcatSequence` | 各 part を順番に描画し X を進める |

`drawInBatch8xOutline` [SRG: `m_168645_`]（エンティティ名アウトライン）も同様にハンドリング。

---

### SRG 名 早見表（MC 1.20.1）

| SRG 名 | クラス | Mojang 名 |
|---|---|---|
| `m_5536_` | `Language` | `getVisualOrder(FormattedText)` |
| `m_272077_` | `Font` | `drawInBatch(Component, ...)` |
| `m_272191_` | `Font` | `drawInBatch(FormattedCharSequence, ...)` |
| `m_168645_` | `Font` | `drawInBatch8xOutline(...)` |
| `m_92724_` | `Font` | `width(FormattedCharSequence)` |
| `m_94005_` | `ComponentRenderUtils` | `wrapComponents(FormattedText, int, Font)` |
| `m_277130_` | `SignText` | `getRenderMessages(boolean, Function)` |
| `m_7303_` | `BookViewScreen.WrittenBookAccess` | `getPage(int)` |
| `m_280304_` | `GuiGraphics` | `renderComponentHoverEffect(Font, Style, int, int)` |
| `m_7392_` | `EntityRenderer` | `renderNameTag(Entity, Component, PoseStack, ...)` |
| `m_41786_` | `ItemStack` | `getHoverName()` |
| `m_240465_` | `ChatComponent` | `addMessage(...)` |
| `m_93795_` | `ChatComponent` | `rescaleChat()` |

## Adding a New Shortcode

Create `src/main/resources/assets/emoji_deco/shortcodes/<name>.json`. `ShortcodeManager` discovers all JSON files automatically. For parameterized shortcodes include `emoji_deco:arg` in the display; these are stored in `PARAM_REGISTRY` and re-parsed on each call.

## Adding a New Decorator

Create `src/main/resources/assets/emoji_deco/decorators/<name>.json` with `{"enable":true,"display":{...}}`. The display must include `{"emoji_deco:slot":{}}` where the wrapped content should appear.

## Adding a New Mixin

1. Create the mixin class in `src/main/java/com/uqlism/emoji_deco/mixin/`.
2. Register it in `src/main/resources/emoji_deco.mixins.json` under `"mixins"` or `"client"`.

## Running Tests

```bash
./gradlew test
```

Tests live in `src/test/java/com/uqlism/emoji_deco/`. `MinecraftTestBase` calls `SharedConstants.tryDetectVersion()` (no full Bootstrap needed). `ParsingIntegrationTest` injects test data into manager registries via reflection.
