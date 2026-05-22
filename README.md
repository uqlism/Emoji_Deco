# Emoji & Deco

**チャット・看板・エンティティ名タグ・本など、あらゆる場所のテキストをリッチテキストで装飾する Minecraft Mod**

[![Modrinth](https://img.shields.io/modrinth/v/Ukxc5O8g?label=Modrinth&logo=modrinth)](https://modrinth.com/mod/emoji-deco)
[![CurseForge](https://img.shields.io/curseforge/v/1527446?label=CurseForge&logo=curseforge)](https://www.curseforge.com/minecraft/mc-mods/emoji-deco)
[![GitHub Release](https://img.shields.io/github/v/release/uqlism/Emoji_Deco?label=GitHub)](https://github.com/uqlism/Emoji_Deco/releases)

**Minecraft 1.21.1 / Forge 52.1.0 · NeoForge 21.1.x**

---

## ✨ できること

`#bold[太字]`、`#color.red[赤い文字]`、`:heart:` など専用の記法を入力するだけで、
チャットや看板などあらゆる場所のテキストを自由に装飾できます。

- `#` から始まる**デコレーター**でスタイル・色・サイズを変更
- `:` で囲む**ショートコード**で絵文字やアイテムアイコンを挿入
- チャット・看板・エンティティ名タグ・本・タイトル・ホットバー・ツールチップなど幅広く対応
- `#` や `:` 入力時に**オートコンプリート**が表示される

---

## 📝 デコレーター記法

`#デコレーター名[テキスト]` の形で記述します。

### スタイル

| 記法 | 効果 |
|---|---|
| `#bold[テキスト]` | **太字** |
| `#italic[テキスト]` | *斜体* |
| `#underline[テキスト]` | 下線 |
| `#strike[テキスト]` | 取り消し線 |
| `#glow[テキスト]` | 周囲の明るさに関わらず常に明るく光る（3Dワールド描画のみ） |
| `#rainbow[テキスト]` | 時間とともに色が変化するレインボーアニメーション |

### 色

`#color.色名[テキスト]` または `#color.#RRGGBB[テキスト]` で色を指定します。

```
#color.red[赤い文字]
#color.#00ffcc[カスタムカラー]
#bold[#color.gold[ゴールドの太字]]
```

使える色名: `aqua` `black` `blue` `dark_aqua` `dark_blue` `dark_gray` `dark_green`
`dark_purple` `dark_red` `gold` `gray` `green` `light_purple` `red` `white` `yellow`

### サイズ変更

`#size.倍率[テキスト]` で文字を拡大・縮小できます。

```
#size.2[2倍の大きさ]
#size.0.5[半分の大きさ]
```

> **注意**: 本（Written Book）では `font.split()` 経路のためサイズ変更は無効です。

### デコレーターのネスト

デコレーターは自由に入れ子にできます。

```
#bold[#color.gold[ゴールドの太字]]
#size.2[#rainbow[大きくレインボー]]
```

---

## 😊 ショートコード記法

`:ショートコード名:` の形で絵文字やアイコンを挿入できます。

### 絵文字

emoji_deco_starter リソースパックを有効にすると、1800 種類以上の Twemoji 絵文字が使えます。

```
:heart:
:smile:
:thumbsup:
:fire:
```

`#` または `:` を入力するとオートコンプリートで候補が表示されます。

### アイテム・ブロックアイコン

```
:item.diamond:
:item.diamond_sword:
:block.stone:
:block.grass_block:
```

### プレイヤーヘッドアイコン

```
:player.Steve:
:player.プレイヤー名:
```

> スキンテクスチャの取得にはネット接続が必要です。

---

## ⌨️ オートコンプリート

チャット・看板・本の編集中に `#` や `:` を入力すると候補が自動表示されます。

- **↑ / ↓** で候補を選択
- **Tab** で確定

引数ありのデコレーター・ショートコードも、`.` の後に候補が表示されます。

```
#color.   →  色名の候補が出る
:player.  →  オンラインプレイヤー名の候補が出る
```

---

## 🚫 エスケープ記法

`\` を前に付けると特殊文字をそのまま表示できます。

| 記法 | 表示される文字 |
|---|---|
| `\#bold[普通]` | `#bold[普通]`（装飾されない） |
| `\:heart\:` | `:heart:`（変換されない） |
| `\\` | `\` |

対応文字: `\#` `\:` `\[` `\]` `\.` `\,` `\\`

---

## 🖊️ 落書きブロック (Graffiti)

**落書きインク**アイテムで壁・床・天井に落書きブロックを設置できます。
右クリックで編集画面が開き、最大10行のリッチテキストを書き込めます。
サイズ変更・グロー効果・レインボーアニメーションが完全に反映されます。

- 耐久値 8（修繕・耐久力エンチャント対応）
- 創造モードでは耐久値を消費しません

---

## 📍 対応箇所

| 場所 | ✅ | #size | #glow |
|---|---|---|---|
| チャット | ✅ | ❌ | ⚠️ 限定的 |
| 看板 | ✅ | ✅ | ✅ |
| エンティティ名タグ | ✅ | ✅ | ✅ |
| 本・羊皮紙 | ✅ | ❌ | ❌ |
| タイトル・アクションバー | ✅ | ✅ | ❌ |
| ホットバー・ツールチップ | ✅ | ✅ | ❌ |
| 落書きブロック | ✅ | ✅ | ✅ |

---

## 📦 インストール

### Forge (1.21.1)

1. [Forge 52.x](https://files.minecraftforge.net/) をインストール
2. `emoji_deco-forge-1.21.1-x.x.x.jar` を `.minecraft/mods/` に配置
3. Minecraft を起動

### NeoForge (1.21.1)

1. [NeoForge 21.1.x](https://neoforged.net/) をインストール
2. `emoji_deco-neoforge-1.21.1-x.x.x.jar` を `.minecraft/mods/` に配置
3. Minecraft を起動

### 絵文字ショートコードを使う場合

初回起動後、リソースパック画面で **emoji_deco_starter** を有効にしてください。
1800 種類以上の Twemoji 絵文字ショートコード（`:smile:` `:heart:` など）が使えるようになります。

---

## ⚙️ 設定

`config/emoji_deco-common.toml` で各場所の機能を個別に ON/OFF できます。

| キー | 説明 | デフォルト |
|---|---|---|
| `enableChat` | チャット | `true` |
| `enableSigns` | 看板 | `true` |
| `enableItemNames` | アイテム名（ホットバー・ツールチップ） | `true` |
| `enableEntityNames` | エンティティ名タグ | `true` |
| `enableBooks` | 本・羊皮紙 | `true` |
| `enableGui` | タイトル・アクションバー | `true` |

---

## 🔧 カスタムショートコード・デコレーターの追加

リソースパックに対応しています。
`assets/emoji_deco/shortcodes/` または `assets/emoji_deco/decorators/` に JSON を追加するだけで
独自のショートコード・デコレーターを定義できます。
サーバーリソースパックを使えば参加者全員に配布できます。

---

## ⚠️ 既知の制限・互換性

- **Supplementaries 道標 (Sign Post)**: ショートコード・デコレーターが適用されません（次バージョンで対応予定）
- **#glow**: 看板・エンティティ名タグ・落書きブロックなど3D描画コンテキストのみ有効です（ホットバー・タイトル・アクションバーでは非対応）
- **本 × #size / #glow / #rainbow**: `font.split()` 経路のため無効です

---

## 🛠️ ビルド方法

```bash
git clone https://github.com/uqlism/Emoji_Deco.git
cd Emoji_Deco

# Forge
./gradlew :forge:jarJar

# NeoForge
./gradlew :neoforge:build
```

---

## 📄 ライセンス

MIT License — 詳細は [LICENSE](LICENSE) を参照してください。
