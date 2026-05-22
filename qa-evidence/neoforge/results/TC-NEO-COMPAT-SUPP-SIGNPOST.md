---
test: TC-NEO-COMPAT-SUPP-SIGNPOST
feature: Supplementaries 道標（Sign Post）互換性
result: FAIL
date: 2026-05-23
commit: 45df155
location: compat
content: sprite
---

Supplementaries Sign Post にショートコード・デコレータを入力しても変換されず、リテラル文字列がそのまま表示される。
MixinSignText は vanilla SignText.getRenderMessages() をフックするが、Supplementaries の Sign Post は独自 BlockEntityRenderer を持つため未対応。
