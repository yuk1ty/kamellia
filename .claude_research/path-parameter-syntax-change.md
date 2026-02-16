# パスパラメータ構文の変更: `{id}` → `:id`

## 概要

現在の実装では `{id}` 構文でパスパラメータを定義していますが、`:id` 構文（Hono、Express、Sinatraスタイル）に変更する必要があります。

## 調査日

2026-02-15

## 現在の実装

### PathPatternMatcher クラス

**ファイル**: `src/main/kotlin/io/github/kamellia/routing/PathPatternMatcher.kt`

現在の正規表現ベースの実装:

```kotlin
// Line 11: パラメータ名の抽出用正規表現
val paramPattern = """\{(\w+)\}""".toRegex()

// Line 12-14: パターンからパラメータ名を抽出
paramPattern.findAll(pattern).forEach { match ->
    paramNames.add(match.groupValues[1])
}

// Line 19-21: 正規表現への変換
val regexPattern =
    pattern
        .replace("{", "(?<")
        .replace("}", ">[^/]+)")
```

### 動作例

- 入力: `/users/{id}/posts/{postId}`
- 出力: `^/users/(?<id>[^/]+)/posts/(?<postId>[^/]+)$`

## 変更が必要なファイル

### 1. コア実装（必須）

#### `src/main/kotlin/io/github/kamellia/routing/PathPatternMatcher.kt`

**変更箇所**: Line 11, 19-21

**現在の実装**:
```kotlin
val paramPattern = """\{(\w+)\}""".toRegex()
paramPattern.findAll(pattern).forEach { match ->
    paramNames.add(match.groupValues[1])
}

val regexPattern =
    pattern
        .replace("{", "(?<")
        .replace("}", ">[^/]+)")
```

**変更後の実装**:
```kotlin
val paramPattern = """:(\w+)""".toRegex()
paramPattern.findAll(pattern).forEach { match ->
    paramNames.add(match.groupValues[1])
}

// :id を (?<id>[^/]+) に変換
val regexPattern = paramPattern.replace(pattern) { matchResult ->
    "(?<${matchResult.groupValues[1]}>[^/]+)"
}
```

### 2. テストファイル（必須）

#### `src/test/kotlin/io/github/kamellia/routing/PathPatternMatcherTest.kt`

変更箇所:
- Line 27: `"/users/{id}"` → `"/users/:id"`
- Line 35: `"/users/{userId}/posts/{postId}"` → `"/users/:userId/posts/:postId"`
- Line 44: `"/users/{id}"` → `"/users/:id"`
- Line 52: `"/users/{id}"` → `"/users/:id"`
- Line 59: `"/users/{id}/posts"` → `"/users/:id/posts"`
- Line 74: `"/users/{id}"` → `"/users/:id"`
- Line 81: `"/users/{id}/age/{age}"` → `"/users/:id/age/:age"`

**変更数**: 7箇所

#### `src/test/kotlin/io/github/kamellia/routing/RouterTest.kt`

変更箇所:
- Line 46: `"/users/{id}"` → `"/users/:id"`
- Line 151: `"/users/{id}"` → `"/users/:id"`
- Line 152: `"/users/{name}"` → `"/users/:name"`

**変更数**: 3箇所

#### `src/test/kotlin/io/github/kamellia/KamelliaTest.kt`

変更箇所:
- Line 62: `"/users/{id}"` → `"/users/:id"`
- Line 63: `"/users/{id}"` → `"/users/:id"`

**変更数**: 2箇所

### 3. サンプルコード（推奨）

#### `src/main/kotlin/io/github/kamellia/Main.kt`

変更箇所:
- Line 28: `"/users/{id}"` → `"/users/:id"`
- Line 34: `"/users/{userId}/posts/{postId}"` → `"/users/:userId/posts/:postId"`

**変更数**: 2箇所

### 4. ドキュメント（推奨）

#### `docs/design/design.md`

変更箇所:
- Line 85: `app.get("/users/{id}")` → `app.get("/users/:id")`
- Line 168: コメント `"/users/{id}/posts/{postId}"` → `"/users/:id/posts/:postId"`
- Line 268: `app.get("/users/{id}")` → `app.get("/users/:id")`
- Line 275: `app.get("/posts/{postId}")` → `app.get("/posts/:postId")`
- Line 299: `app.get("/articles/{articleId}/comments/{commentId}")` → `app.get("/articles/:articleId/comments/:commentId")`

**変更数**: 5箇所

## 変更の影響範囲

| カテゴリ | ファイル数 | 変更箇所数 |
|---------|----------|----------|
| コア実装 | 1 | 1箇所（ロジック変更） |
| テストファイル | 3 | 12箇所 |
| サンプルコード | 1 | 2箇所 |
| ドキュメント | 1 | 5箇所 |
| **合計** | **6** | **20箇所** |

## 実装の詳細

### 変更のポイント

1. **正規表現パターンの変更**
   - `"""\{(\w+)\}"""` → `""":(\w+)"""`
   - `{name}` 形式から `:name` 形式への変更

2. **置換ロジックの変更**
   - 現在: 単純な文字列置換 (`replace("{", "(?<").replace("}", ">[^/]+)")`)
   - 変更後: 正規表現の `replace()` メソッドを使用
   - `:paramName` を `(?<paramName>[^/]+)` に変換

### 変更例

#### 入力パターンの変換

**変更前**:
```
/users/{id}                    → ^/users/(?<id>[^/]+)$
/users/{userId}/posts/{postId} → ^/users/(?<userId>[^/]+)/posts/(?<postId>[^/]+)$
```

**変更後**:
```
/users/:id                    → ^/users/(?<id>[^/]+)$
/users/:userId/posts/:postId → ^/users/(?<userId>[^/]+)/posts/(?<postId>[^/]+)$
```

## 推奨実装手順

1. **PathPatternMatcher.kt の実装変更**
   - 正規表現パターンを変更
   - 置換ロジックを変更

2. **テストファイルの更新**
   - PathPatternMatcherTest.kt (7箇所)
   - RouterTest.kt (3箇所)
   - KamelliaTest.kt (2箇所)

3. **テスト実行**
   ```bash
   ./gradlew test
   ```

4. **サンプルコードの更新**
   - Main.kt (2箇所)

5. **ドキュメントの更新**
   - design.md (5箇所)

6. **最終検証**
   ```bash
   ./gradlew compileTestKotlin
   ./gradlew test
   ./gradlew detekt
   ```

## 注意事項

### 破壊的変更

この変更は破壊的変更です。既存のすべてのルート定義を更新する必要があります。

### 利点

- より一般的なルーティング構文（Hono、Express、Sinatra など）
- タイプ数が少ない（`{id}` より `:id` の方が短い）
- 多くの開発者に馴染みのある構文

### 欠点

- 既存コードの変更が必要
- URL の一部として `:` を使う場合に曖昧さが生じる可能性（ただし実用上問題なし）

## 参考実装

### Hono (TypeScript)
```typescript
app.get('/users/:id', (c) => {
  const userId = c.req.param('id')
  return c.json({ userId })
})
```

### Express (Node.js)
```javascript
app.get('/users/:id', (req, res) => {
  const userId = req.params.id
  res.json({ userId })
})
```

### Sinatra (Ruby)
```ruby
get '/users/:id' do
  user_id = params[:id]
  { userId: user_id }.to_json
end
```

## 実装完了の確認

以下をすべて確認してください：

- [ ] PathPatternMatcher.kt の実装変更完了
- [ ] すべてのテストファイルの更新完了
- [ ] `./gradlew test` が成功
- [ ] Main.kt のサンプルコード更新完了
- [ ] design.md のドキュメント更新完了
- [ ] `./gradlew compileTestKotlin` が成功
- [ ] `./gradlew detekt` が成功
- [ ] 動作確認（手動テスト）完了

## 関連ファイル

- コア実装: `src/main/kotlin/io/github/kamellia/routing/PathPatternMatcher.kt`
- ルーティング: `src/main/kotlin/io/github/kamellia/routing/Router.kt`
- ルート定義: `src/main/kotlin/io/github/kamellia/routing/Route.kt`
