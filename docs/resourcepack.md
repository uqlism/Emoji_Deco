# Emoji & Deco リソースパック開発ガイド

このガイドは、Emoji & Deco mod 用のショートコード・デコレーターを  
**自分で追加・カスタマイズしたい人**向けの解説書です。  
「JSON が読める」程度の知識があれば十分です。

---

## 目次

1. [ファイルの配置](#1-ファイルの配置)
2. [ショートコードを作る](#2-ショートコードを作る)
   - [最小構成：テキストのみ](#最小構成テキストのみ)
   - [画像を表示する](#画像を表示する)
   - [引数を受け取る](#引数を受け取る)
   - [ショートコード トップレベルフィールド一覧](#ショートコード-トップレベルフィールド一覧)
3. [デコレーターを作る](#3-デコレーターを作る)
   - [最小構成：スタイル適用](#最小構成スタイル適用)
   - [引数付きデコレーター](#引数付きデコレーター)
   - [デコレーター トップレベルフィールド一覧](#デコレーター-トップレベルフィールド一覧)
4. [display ディレクティブ一覧](#4-display-ディレクティブ一覧)
   - [Minecraft 標準テキストフィールド](#minecraft-標準テキストフィールド)
   - [emoji_deco:slot](#emoji_decoslot--デコレーター専用)
   - [emoji_deco:glow](#emoji_decoglow)
   - [emoji_deco:image_to_glyph](#emoji_decoimage_to_glyph)
   - [emoji_deco:offset](#emoji_decooffset)
   - [emoji_deco:scale](#emoji_decoscale)
   - [emoji_deco:rotate](#emoji_decorotate)
   - [emoji_deco:apply_shortcode](#emoji_decoapply_shortcode)
   - [emoji_deco:apply_decorator](#emoji_decoapply_decorator)
5. [画像ソース (image)](#5-画像ソース-image)
   - [emoji_deco:fetch_atlas](#emoji_decofetch_atlas)
   - [emoji_deco:fetch_skin](#emoji_decofetch_skin)
   - [emoji_deco:decode_image + emoji_deco:fetch_url](#emoji_decodecode_image--emoji_decofetch_url)
   - [emoji_deco:decode_image + emoji_deco:fetch_resource](#emoji_decodecode_image--emoji_decofetch_resource)
6. [動的プロバイダー](#6-動的プロバイダー)
   - [emoji_deco:arg](#emoji_decoarg)
   - [emoji_deco:join](#emoji_decojoin)
   - [emoji_deco:player_names](#emoji_decoplayer_names)
7. [args フィールド詳細](#7-args-フィールド詳細)
8. [display を配列にする](#8-display-を配列にする)
9. [実践サンプル集](#9-実践サンプル集)

---

## 1. ファイルの配置

リソースパックの構造は以下のとおりです。

```
assets/
└── emoji_deco/
    ├── shortcodes/
    │   └── <名前>.json      ← ショートコード定義
    └── decorators/
        └── <名前>.json      ← デコレーター定義
```

- **ショートコード** `:名前:` または `:名前.引数1,引数2:` で呼び出します。
- **デコレーター** `#名前[内容]` または `#名前.引数1,引数2[内容]` で呼び出します。

ファイル名がそのまま呼び出し名になります（拡張子 `.json` を除く）。  
例: `shortcodes/heart.json` → `:heart:`

---

## 2. ショートコードを作る

### 最小構成：テキストのみ

```json
{
  "enable": true,
  "display": {
    "text": "♥",
    "color": "red"
  }
}
```

これだけで `:heart:` と打つと赤いハートが表示されます。  
`"enable": false` にするとそのファイルは読み込まれません。

---

### 画像を表示する

Minecraft のブロックテクスチャアトラスから画像を表示する例です。

```json
{
  "enable": true,
  "display": {
    "text": "",
    "extra": [
      {
        "type": "emoji_deco:image_to_glyph",
        "width": 8,
        "height": 8,
        "image": {
          "type": "emoji_deco:fetch_atlas",
          "atlas": "minecraft:textures/atlas/blocks.png",
          "sprite": "block/diamond_block"
        }
      }
    ]
  }
}
```

`emoji_deco:image_to_glyph` は画像をグリフ（1文字）として描画します。  
`width`/`height` はゲーム内での表示サイズ（ピクセル単位）です。

---

### 引数を受け取る

`:color.Steve:` のように呼び出し側から値を渡せます。

```json
{
  "enable": true,
  "args": [
    { "value_type": "string", "default": "Steve", "label": "<player>" }
  ],
  "display": {
    "text": "",
    "extra": [
      {
        "type": "emoji_deco:image_to_glyph",
        "width": 8,
        "height": 8,
        "image": {
          "type": "emoji_deco:fetch_skin",
          "player": { "type": "emoji_deco:arg", "index": 0 }
        }
      }
    ]
  }
}
```

`args` 配列に引数の仕様を書き、`display` 内で `{"type":"emoji_deco:arg","index":N}` で参照します（0始まり）。

---

### ショートコード トップレベルフィールド一覧

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `enable` | boolean | ○ | `false` で読み込みスキップ |
| `display` | object / array / string | ○ | 表示内容（後述） |
| `args` | array | - | 引数定義（[7章](#7-args-フィールド詳細)参照） |
| `preview` | object / array / string | - | オートコンプリートの候補表示に使うプレビュー。省略時は `display` を空引数で展開したものが使われる |
| `label` | string | - | オートコンプリート候補の説明テキスト。省略時は引数ラベルから自動生成 |
| `aliases` | array of string | - | 別名リスト。例: `["hrt"]` にすると `:hrt:` でも呼べる |

---

## 3. デコレーターを作る

### 最小構成：スタイル適用

```json
{
  "enable": true,
  "display": {
    "text": "",
    "bold": true,
    "extra": [
      { "type": "emoji_deco:slot" }
    ]
  }
}
```

`emoji_deco:slot` は `#bold[ここのテキスト]` の **「ここのテキスト」** が入る場所です。  
デコレーターには必ず1つ `slot` を置いてください。

---

### 引数付きデコレーター

`#color.red[テキスト]` のように色名を引数で受け取る例です。

```json
{
  "enable": true,
  "args": [
    {
      "value_type": "string",
      "default": "white",
      "label": "<color>",
      "suggestions": ["red", "green", "blue", "yellow", "gold", "aqua"]
    }
  ],
  "display": {
    "text": "",
    "color": { "type": "emoji_deco:arg", "index": 0 },
    "extra": [
      { "type": "emoji_deco:slot" }
    ]
  }
}
```

---

### デコレーター トップレベルフィールド一覧

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `enable` | boolean | ○ | `false` で読み込みスキップ |
| `display` | object / array / string | ○ | 表示内容。`emoji_deco:slot` を含める必要がある |
| `args` | array | - | 引数定義（[7章](#7-args-フィールド詳細)参照） |
| `preview` | object / array / string | - | オートコンプリートのプレビュー |

---

## 4. display ディレクティブ一覧

`display` の値はオブジェクト・配列・文字列のいずれかです。

- **オブジェクト** → 1つのコンポーネント/ディレクティブ
- **配列** → 複数の要素を並べて連結（[8章](#8-display-を配列にする)参照）
- **文字列** → そのままのテキスト（`{"text":"..."}` の省略形）

---

### Minecraft 標準テキストフィールド

`"type"` を指定しない通常のオブジェクトは Minecraft の **テキストコンポーネント** として扱われます。

```json
{
  "text": "Hello",
  "color": "#ff4040",
  "bold": true,
  "italic": false,
  "underlined": false,
  "strikethrough": false,
  "obfuscated": false,
  "font": "minecraft:default",
  "extra": [ ... ],
  "hoverEvent": {
    "action": "show_text",
    "contents": { "text": "ホバーテキスト" }
  }
}
```

`color` には `"red"` などの名前色のほか、`"#rrggbb"` 形式の16進数も使えます。  
これらのフィールドには `{"type":"emoji_deco:arg","index":N}` も渡せます（動的引数）。

---

### `emoji_deco:slot` — デコレーター専用

```json
{ "type": "emoji_deco:slot" }
```

`#decorator[ここ]` の内容が展開される場所。デコレーターに1つ必要。

---

### `emoji_deco:glow`

```json
{
  "type": "emoji_deco:glow",
  "glow": true,
  "contents": { ... }
}
```

`contents` の中のテキスト・グリフを最大輝度（`packedLight=0xF000F0`）で描画します。  
看板やグラフィティブロックで暗い場所でも光って見えます。

| フィールド | 型 | 説明 |
|---|---|---|
| `glow` | boolean / arg | `true` でグロー有効 |
| `contents` | display要素 | グロー対象 |

---

### `emoji_deco:image_to_glyph`

```json
{
  "type": "emoji_deco:image_to_glyph",
  "width": 8,
  "height": 8,
  "advance": 8,
  "image": { ... }
}
```

画像を1グリフとしてインライン描画します。`image` フィールドには[画像ソース](#5-画像ソース-image)を指定します。

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `width` | number | ○ | 表示幅（px） |
| `height` | number | ○ | 表示高さ（px） |
| `advance` | number | - | 次の文字との間隔。省略時は `width` と同じ |
| `image` | image source | ○ | 画像ソース（[5章](#5-画像ソース-image)参照） |

`advance: 0` にすると次のグリフと重ねて描画できます（プレイヤーヘッドの顔+帽子のような重ね合わせに使う）。

---

### `emoji_deco:offset`

```json
{
  "type": "emoji_deco:offset",
  "x": -0.5,
  "y": -0.5,
  "z": 0.01,
  "contents": { ... }
}
```

`contents` の描画位置をずらします。

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `x` | number / arg | - | 水平オフセット（px）。正で右 |
| `y` | number / arg | - | 垂直オフセット（px）。正で下 |
| `z` | number | - | 奥行きオフセット。重なり順の調整に使う |
| `contents` | display要素 | ○ | オフセット対象 |

---

### `emoji_deco:scale`

```json
{
  "type": "emoji_deco:scale",
  "x": 2.0,
  "y": 2.0,
  "contents": { ... }
}
```

`contents` を拡大縮小します。`x: -1` で左右反転（`#flip` デコレーターがこれを使っています）。

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `x` | number / arg | - | 横方向倍率（省略時 1.0） |
| `y` | number / arg | - | 縦方向倍率（省略時 1.0） |
| `contents` | display要素 | ○ | スケール対象 |

---

### `emoji_deco:rotate`

```json
{
  "type": "emoji_deco:rotate",
  "angle": 45.0,
  "contents": { ... }
}
```

`contents` を回転します（度数法）。

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `angle` | number / arg | ○ | 回転角度（度）。正で時計回り |
| `contents` | display要素 | ○ | 回転対象 |

---

### `emoji_deco:apply_shortcode`

```json
{
  "type": "emoji_deco:apply_shortcode",
  "shortcode": "player",
  "args": [
    { "type": "emoji_deco:arg", "index": 0 }
  ]
}
```

別のショートコードをインラインで呼び出します。  
`args` には文字列リテラルや `emoji_deco:arg` などの動的プロバイダーを渡せます。

---

### `emoji_deco:apply_decorator`

```json
{
  "type": "emoji_deco:apply_decorator",
  "decorator": "color",
  "args": ["yellow"],
  "slot": { "type": "emoji_deco:arg", "index": 0 }
}
```

別のデコレーターをインラインで呼び出します。

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `decorator` | string | ○ | デコレーター名 |
| `args` | array | - | デコレーターへの引数 |
| `slot` | display要素 | ○ | スロットに入れる内容 |

---

## 5. 画像ソース (image)

`emoji_deco:image_to_glyph` の `image` フィールドに指定します。

---

### `emoji_deco:fetch_atlas`

```json
{
  "type": "emoji_deco:fetch_atlas",
  "atlas": "minecraft:textures/atlas/blocks.png",
  "sprite": "block/stone"
}
```

Minecraft のテクスチャアトラスからスプライトを取得します。  
`sprite` は ResourceLocation 形式（`namespace:path` または単に `path`）です。

`sprite` には文字列リテラルだけでなく `emoji_deco:join` を使った動的な値も渡せます:

```json
"sprite": {
  "type": "emoji_deco:join",
  "parts": [
    { "type": "emoji_deco:arg", "index": 1 },
    ":block/",
    { "type": "emoji_deco:arg", "index": 0 }
  ]
}
```

---

### `emoji_deco:fetch_skin`

```json
{
  "type": "emoji_deco:fetch_skin",
  "player": "Steve",
  "uv": [8, 8, 16, 16]
}
```

プレイヤーのスキンテクスチャを取得します。

| フィールド | 型 | 説明 |
|---|---|---|
| `player` | string / arg | プレイヤー名 |
| `uv` | [x0, y0, x1, y1] | スキンテクスチャ上のUV範囲（省略時はスキン全体） |

スキンテクスチャの主な UV 座標：

| 部位 | UV |
|---|---|
| 顔（face） | `[8, 8, 16, 16]` |
| 帽子（hat overlay） | `[40, 8, 48, 16]` |
| 全体 | なし（省略） |

---

### `emoji_deco:decode_image` + `emoji_deco:fetch_url`

```json
{
  "type": "emoji_deco:decode_image",
  "source": {
    "type": "emoji_deco:fetch_url",
    "url": "https://example.com/icon.png",
    "disk_cache": true,
    "ttl": 3600
  }
}
```

HTTP/HTTPS から画像を取得してデコードします。

`fetch_url` フィールド：

| フィールド | 型 | 説明 |
|---|---|---|
| `url` | string | 取得先 URL |
| `disk_cache` | boolean | `true` でローカルにキャッシュ（省略時 false） |
| `ttl` | number | キャッシュ有効期間（秒）。省略時は永続 |

`decode_image` の `format` フィールドで `"png"` / `"webp"` などを明示指定できますが、省略時は自動検出されます。

---

### `emoji_deco:decode_image` + `emoji_deco:fetch_resource`

```json
{
  "type": "emoji_deco:decode_image",
  "source": {
    "type": "emoji_deco:fetch_resource",
    "path": "emoji_deco:textures/emoji/fire.png"
  }
}
```

リソースパック内のファイルを読み込みます。  
`path` は `namespace:path` 形式の ResourceLocation です。

---

## 6. 動的プロバイダー

`display` の**値の位置**（文字列・数値・真偽値が入る場所）にオブジェクトとして記述し、実行時に展開されます。

---

### `emoji_deco:arg`

```json
{ "type": "emoji_deco:arg", "index": 0 }
```

ショートコード/デコレーターに渡された引数を参照します。インデックスは0始まりです。

省略可能なフィールド:

| フィールド | 型 | 説明 |
|---|---|---|
| `index` | number | 引数番号（0始まり） |
| `value_type` | string | ここで型変換する場合に指定（`args` 側の `value_type` が優先） |
| `default` | any | この参照でのフォールバック値 |

---

### `emoji_deco:join`

```json
{
  "type": "emoji_deco:join",
  "parts": [
    { "type": "emoji_deco:arg", "index": 1 },
    ":item/",
    { "type": "emoji_deco:arg", "index": 0 }
  ],
  "separator": ""
}
```

複数の値を文字列として連結します。スプライトIDなどを動的に組み立てるときに使います。

| フィールド | 型 | 説明 |
|---|---|---|
| `parts` | array | 連結する要素（文字列リテラル・arg など） |
| `separator` | string | 要素間の区切り文字（省略時は `""`） |

---

### `emoji_deco:player_names`

```json
{ "type": "emoji_deco:player_names" }
```

現在オンラインのプレイヤー名一覧に展開されます。  
`args` の `suggestions` フィールドに指定することでオートコンプリート候補にできます（[7章](#7-args-フィールド詳細)参照）。

---

## 7. args フィールド詳細

`args` は引数仕様のオブジェクト配列です。インデックスが `emoji_deco:arg` の `index` に対応します。

```json
"args": [
  {
    "value_type": "string",
    "default": "Steve",
    "label": "<player>",
    "hidden": false,
    "suggestions": { "type": "emoji_deco:player_names" }
  }
]
```

| フィールド | 型 | 説明 |
|---|---|---|
| `value_type` | string | 型。`"string"` / `"boolean"` / `"integer"` / `"float"` |
| `default` | any | 引数が省略されたときの値 |
| `label` | string | オートコンプリートに表示するラベル（例: `"<color>"`） |
| `hidden` | boolean | `true` で通常オートコンプリートに表示しない（`#bold` のような省略可能引数に使用） |
| `suggestions` | array / object | 補完候補。文字列配列か `{"type":"emoji_deco:player_names"}` |

### value_type の挙動

| 型 | 変換ルール |
|---|---|
| `"string"` | そのまま |
| `"boolean"` | `"true"` / `"1"` / `"yes"`（大小文字無視）→ `true`、それ以外 → `false` |
| `"integer"` | 整数にパース。失敗時は `0` |
| `"float"` | 浮動小数点にパース。失敗時は `0.0` |

---

## 8. display を配列にする

`display` の値を配列にすると複数の要素を並べて表示できます。

```json
{
  "enable": true,
  "display": [
    { "text": "[" },
    {
      "type": "emoji_deco:image_to_glyph",
      "width": 8,
      "height": 8,
      "image": {
        "type": "emoji_deco:fetch_atlas",
        "atlas": "minecraft:textures/atlas/blocks.png",
        "sprite": "block/diamond_block"
      }
    },
    { "text": "]" }
  ]
}
```

配列の要素には以下が使えます:
- オブジェクト（ディレクティブまたは Minecraft テキストコンポーネント）
- 文字列（`"Hello"` は `{"text":"Hello"}` と同等）

---

## 9. 実践サンプル集

### テキストに絵文字を追加する（シンプル版）

`shortcodes/star.json`:
```json
{
  "enable": true,
  "display": { "text": "★", "color": "gold" }
}
```

使用例: `:star:` → 金色の ★

---

### バニラブロックをアイコン表示する

`shortcodes/grass.json`:
```json
{
  "enable": true,
  "display": {
    "text": "",
    "extra": [
      {
        "type": "emoji_deco:image_to_glyph",
        "width": 8,
        "height": 8,
        "image": {
          "type": "emoji_deco:fetch_atlas",
          "atlas": "minecraft:textures/atlas/blocks.png",
          "sprite": "block/grass_block_side"
        }
      }
    ]
  }
}
```

使用例: `:grass:`

---

### 任意ブロックをアイコン表示する（引数付き）

`shortcodes/block.json`:
```json
{
  "enable": true,
  "args": [
    { "value_type": "string", "default": "",          "label": "<block>" },
    { "value_type": "string", "default": "minecraft", "label": "<namespace=minecraft>" }
  ],
  "display": {
    "text": "",
    "extra": [
      {
        "type": "emoji_deco:image_to_glyph",
        "width": 8,
        "height": 8,
        "image": {
          "type": "emoji_deco:fetch_atlas",
          "atlas": "minecraft:textures/atlas/blocks.png",
          "sprite": {
            "type": "emoji_deco:join",
            "parts": [
              { "type": "emoji_deco:arg", "index": 1 },
              ":block/",
              { "type": "emoji_deco:arg", "index": 0 }
            ]
          }
        }
      }
    ]
  }
}
```

使用例: `:block.stone:` → 石、`:block.oak_log,minecraft:` → 樫の木

---

### プレイヤーヘッドを表示する

`shortcodes/head.json`:
```json
{
  "enable": true,
  "args": [
    {
      "value_type": "string",
      "default": "",
      "label": "<player>",
      "suggestions": { "type": "emoji_deco:player_names" }
    }
  ],
  "display": {
    "text": "",
    "extra": [
      {
        "type": "emoji_deco:image_to_glyph",
        "width": 8,
        "height": 8,
        "advance": 0,
        "image": {
          "type": "emoji_deco:fetch_skin",
          "player": { "type": "emoji_deco:arg", "index": 0 },
          "uv": [8, 8, 16, 16]
        }
      },
      {
        "type": "emoji_deco:offset",
        "x": -0.5,
        "y": -0.5,
        "z": 0.01,
        "contents": {
          "type": "emoji_deco:image_to_glyph",
          "width": 9,
          "height": 9,
          "advance": 9,
          "image": {
            "type": "emoji_deco:fetch_skin",
            "player": { "type": "emoji_deco:arg", "index": 0 },
            "uv": [40, 8, 48, 16]
          }
        }
      }
    ]
  }
}
```

顔（8×8、advance=0 で後続と重ねる）と帽子レイヤー（9×9、-0.5px オフセットで顔を包む）を重ねてリアルなヘッドを再現しています。

使用例: `:head.Steve:`

---

### 光るデコレーターを作る

`decorators/glow.json`:
```json
{
  "enable": true,
  "display": {
    "type": "emoji_deco:glow",
    "glow": true,
    "contents": { "type": "emoji_deco:slot" }
  }
}
```

使用例: `#glow[光るテキスト]`

---

### 回転デコレーターを作る

`decorators/spin.json`:
```json
{
  "enable": true,
  "args": [
    { "value_type": "float", "default": 45.0, "label": "<angle>" }
  ],
  "display": {
    "type": "emoji_deco:rotate",
    "angle": { "type": "emoji_deco:arg", "index": 0 },
    "contents": { "type": "emoji_deco:slot" }
  }
}
```

使用例: `#spin.90[⇧]` → 90度回転した矢印

---

### ネット上の画像を表示する

`shortcodes/wiki.json`:
```json
{
  "enable": true,
  "display": [
    {
      "type": "emoji_deco:image_to_glyph",
      "width": 16,
      "height": 16,
      "image": {
        "type": "emoji_deco:decode_image",
        "source": {
          "type": "emoji_deco:fetch_url",
          "url": "https://example.com/icon.png",
          "disk_cache": true,
          "ttl": 86400
        }
      }
    }
  ]
}
```

`disk_cache: true` + `ttl: 86400`（24時間）でキャッシュされ、2回目以降は高速に表示されます。

---

### 既存デコレーターを組み合わせたショートコード

`shortcodes/shout.json`:
```json
{
  "enable": true,
  "args": [
    { "value_type": "string", "default": "", "label": "<text>" }
  ],
  "display": [
    {
      "type": "emoji_deco:apply_decorator",
      "decorator": "bold",
      "args": [],
      "slot": {
        "type": "emoji_deco:apply_decorator",
        "decorator": "color",
        "args": ["red"],
        "slot": { "type": "emoji_deco:arg", "index": 0 }
      }
    },
    { "text": "!", "color": "red", "bold": true }
  ]
}
```

使用例: `:shout.Warning:` → **Warning**! （太字・赤）

---

## よくある注意点

- `emoji_deco:slot` はデコレーター専用。ショートコードには使えません。
- `args` の `index` は 0 始まりです。
- `display` 配列の中に `"文字列"` を直接書けます（`{"text":"文字列"}` の省略形）。
- `value_type: "boolean"` の引数は `#bold.false[...]` のように明示的に `false` を渡せます。
- 画像グリフはフォントキャッシュに登録されます。ゲームセッション中に初めて表示されるときだけ若干の処理が走ります。
