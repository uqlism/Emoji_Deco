指定バージョンでリリースを行う。

引数: `$ARGUMENTS` がバージョン番号（例: `0.4.0`）。引数がなければユーザーに確認する。

## 手順

### Step 1: バージョン確認

`$ARGUMENTS` がバージョン番号か確認する。空なら「リリースするバージョンを教えてください（例: 0.4.0）」と聞く。
以下 VERSION = `$ARGUMENTS` として進める。

### Step 2: gradle.properties を更新

`gradle.properties` の `mod_version=` を `mod_version=VERSION` に書き換える。
Read してから Edit で更新する。

### Step 3: ビルド確認

```bash
./gradlew :forge:build :neoforge:build
```

BUILD SUCCESSFUL でなければ中断してエラーを報告する。

ビルド成功後、生成された JAR ファイルのパスを確認する:
```bash
ls forge/build/libs/emoji_deco-forge-*[0-9].jar neoforge/build/libs/emoji_deco-neoforge-*[0-9].jar
```

### Step 4: コミット・タグ・プッシュ

```bash
git add gradle.properties
git commit -m "chore: bump version to VERSION"
git tag vVERSION
git push origin HEAD
git push origin vVERSION
```

### Step 5: GitHub Actions の完了を待機

Actions URL を表示する:
```
https://github.com/uqlism/Emoji_Deco/actions
```

gh コマンドでワークフローの完了を待つ:
```bash
gh run list --workflow=publish.yml --limit=1
gh run watch $(gh run list --workflow=publish.yml --limit=1 --json databaseId -q '.[0].databaseId')
```

失敗した場合はログを取得して報告:
```bash
gh run view $(gh run list --workflow=publish.yml --limit=1 --json databaseId -q '.[0].databaseId') --log-failed
```

### Step 6: Modrinth の更新確認

Modrinth API でバージョンが公開されたか確認する:
```bash
curl -s "https://api.modrinth.com/v2/project/MODRINTH_PROJECT_ID/version" \
  | python -c "import sys,json; vs=[v['version_number'] for v in json.load(sys.stdin)]; print('FOUND' if 'VERSION' in vs else 'NOT FOUND'); print('Latest:', vs[:3])"
```

`MODRINTH_PROJECT_ID` は `vars.MODRINTH_PROJECT_ID` の値（リポジトリの Variables に設定済み）。

### Step 7: CurseForge の更新確認

gh コマンドで Actions のサマリーを確認し、CurseForge へのアップロード成功を確認:
```bash
gh run view $(gh run list --workflow=publish.yml --limit=1 --json databaseId -q '.[0].databaseId') --log
```

ログに `curseforge` の成功メッセージがあれば PASS。

### Step 8: 結果報告

以下の形式で報告する:

```
## リリース完了 vVERSION

| 確認項目 | 結果 |
|---|---|
| ビルド (Forge) | ✅ / ❌ |
| ビルド (NeoForge) | ✅ / ❌ |
| GitHub Actions | ✅ 成功 / ❌ 失敗 |
| Modrinth | ✅ 公開確認 / ❌ 未確認 |
| CurseForge | ✅ 公開確認 / ❌ 未確認 |

Modrinth: https://modrinth.com/mod/MODRINTH_PROJECT_ID/version/VERSION
GitHub Release: https://github.com/uqlism/Emoji_Deco/releases/tag/vVERSION
```
