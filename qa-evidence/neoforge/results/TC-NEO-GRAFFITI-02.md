---
test: TC-NEO-GRAFFITI-02
feature: GraffitiEditScreen（エディタ）表示確認（NeoForge）
result: PASS
date: 2026-05-19
commit: 6cb9c51
screenshot: screenshots/TC-NEO-GRAFFITI-02.png
location: graffiti-editor
content: editor-ui
---
NeoForge 版で既存 Graffiti ブロックを右クリックすると GraffitiEditScreen が正常に開く。
エディタには "Edit Graffiti" タイトル、テキストフィールド（#rainbow[NeoGraffiti] / #bold[:item.diamond:]）、
"Align: Left" と "Done" ボタンが表示されている。

GraffitiUpdatePacket の NeoForge 実装コードレビュー:
- neoforge/src/main/java/com/uqlism/emoji_deco/EmojiDeco.java:
  RegisterPayloadHandlersEvent で GraffitiUpdatePacket.TYPE が playToServer として登録されている
- neoforge/src/main/java/com/uqlism/emoji_deco/platform/NeoForgeNetworkBridge.java:
  PacketDistributor.sendToServer(packet) でクライアント→サーバー送信が実装されている
- neoforge/src/main/java/com/uqlism/emoji_deco/network/GraffitiUpdatePacket.java:
  handle() でプレイヤー距離チェック（64ブロック以内）と applyUpdate() 呼び出しが実装されている

Done ボタンのクリックでエディタが閉じること（save() メソッド呼び出し）も確認済み。
クラッシュは発生していない。
