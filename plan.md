# 概要
チャット欄, アイテム名, 看板, 本, 名札 等でリッチテキストが使えるようになるMod

# 機能

## Md記法に一部対応

下記のマークダウン記法に対応
```
# ヘッダー1-6
**太字**
~~打ち消し線~~
__アンダースコア__
*斜体*
``` 

## ショートコードによるテキストコンポーネントエイリアスに対応

data/runicink/shortcodes/mycode.json

```
{
    "enable":true,
    "type":"text_component",
    "text_component": { "type":"text", "text":"hello", "font":"myfont" }
}
```

と設定したうえで`:mycode:`と入力すると、`hello`が`myfont`で見れるようになる。

data/runicink/shortcodes/diamond.json
```
{
    "enable":true,
    "type":"splite",
    "atlas": "minecraft:block",
    "splite": "item/diamond"
}
```
と設定したうえで`:diamond:`と入力すると、ダイヤモンドのアイコンが見れるようになる。

# 実装

描画時に看板のプレーンテキストを解析して、TextComponentを構築し描画するのがよさそう
ヘッダーについてはTextComponentでは実現できないので別途対応が必要
