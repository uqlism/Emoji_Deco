---
test: TC-NEO-GRAFFITI-02
feature: GraffitiEditScreen 入力枠表示品質（NeoForge）
result: FAIL
date: 2026-05-31
commit: 7061b8b
screenshot: screenshots/TC-NEO-GRAFFITI-02.png
---

PostMessage ベース right-click（WM_RBUTTONDOWN/UP、commit 7061b8b の新実装）も SendInput ベース実装と同様に、NeoForge 1.21.1 のインゲーム Raw Input マウスキャプチャと干渉する。right-click コマンド実行のたびにプレイヤー視点が地面方向にリセットされ、GraffitiEditScreen を開けない。PostMessage による WM_RBUTTONDOWN はゲームのマウスキャプチャ中にインタラクションではなく視点変動として処理される。エディタ画面自体の描画品質は未評価のため FAIL で継続記録。setblock + data merge で graffiti ブロックに直接テキストを設定すると block data は正常に保存されるが（/data get で確認済み）、テキストの描画が視認できなかった（レンダラーは正常登録されていることをコード審査で確認）。
