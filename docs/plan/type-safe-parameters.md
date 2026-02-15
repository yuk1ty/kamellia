# 型安全なパラメータアクセスの実装計画

## 概要

現在 `PathParams` と `QueryParams` は `typealias` で `Map<String, String>` と `Map<String, List<String>>` として定義されており、型安全ではありません。
この計画では、専用のクラスを作成して型安全なパラメータアクセスを実現します。

## 目標

1. `PathParams` と `QueryParams` を専用のクラスとして実装
2. 型安全なアクセスメソッドを提供（`int()`, `long()`, `string()` など）
3. inline reified を使った型推論版のメソッドも提供
4. 既存のコードを破壊せずに移行
5. 包括的なテストを追加

## 現状分析

### 影響を受けるファイル

1. **定義ファイル**
   - `src/main/kotlin/io/github/kamellia/core/Request.kt`
     - 現在: `typealias PathParams = Map<String, String>`
     - 現在: `typealias QueryParams = Map<String, List<String>>`

2. **使用箇所**
   - `src/main/kotlin/io/github/kamellia/netty/RequestConverter.kt`
     - `queryParams = queryParams` (line 46)
     - `pathParams = emptyMap()` (line 47)
   - `src/main/kotlin/io/github/kamellia/routing/RouteMatch.kt`
     - `val pathParams: PathParams` (line 7)
   - `src/main/kotlin/io/github/kamellia/routing/Router.kt`
     - `route.matcher.match()` の返り値が `Map<String, String>?` (line 19)
   - `src/main/kotlin/io/github/kamellia/routing/PathPatternMatcher.kt`
     - `match()` の返り値が `Map<String, String>?` (line 24)

3. **テストファイル**
   - `src/test/kotlin/io/github/kamellia/core/RequestTest.kt`
     - `queryParams = emptyMap()` (line 15, 32, 48, 63)
     - `pathParams = emptyMap()` (line 16, 33, 49, 64)
     - `request.queryParams["filter"]` (line 72)
     - `request.pathParams["id"]` (line 73)
   - `src/test/kotlin/io/github/kamellia/netty/RequestConverterTest.kt`
     - `request.queryParams["q"]` (line 67, 68, 102)

## 実装ステップ

### Step 1: 新しい value class の作成

**ファイル**: `src/main/kotlin/io/github/kamellia/core/PathParams.kt`

```kotlin
package io.github.kamellia.core

@JvmInline
value class PathParams internal constructor(
    private val params: Map<String, String>
) {
    // 基本的な型変換メソッド
    fun int(key: String): Int? = params[key]?.toIntOrNull()
    fun long(key: String): Long? = params[key]?.toLongOrNull()
    fun string(key: String): String? = params[key]
    fun boolean(key: String): Boolean? = params[key]?.toBooleanStrictOrNull()
    fun double(key: String): Double? = params[key]?.toDoubleOrNull()

    // inline reified を使った型推論版
    inline fun <reified T> get(key: String): T? {
        val value = params[key] ?: return null
        return when (T::class) {
            Int::class -> value.toIntOrNull() as? T
            Long::class -> value.toLongOrNull() as? T
            String::class -> value as? T
            Boolean::class -> value.toBooleanStrictOrNull() as? T
            Double::class -> value.toDoubleOrNull() as? T
            else -> null
        }
    }

    companion object {
        fun empty(): PathParams = PathParams(emptyMap())
        internal fun of(params: Map<String, String>): PathParams = PathParams(params)
    }
}
```

**ファイル**: `src/main/kotlin/io/github/kamellia/core/QueryParams.kt`

```kotlin
package io.github.kamellia.core

@JvmInline
value class QueryParams internal constructor(
    private val params: Map<String, List<String>>
) {
    // 単一値の取得（最初の値を返す）
    fun int(key: String): Int? = params[key]?.firstOrNull()?.toIntOrNull()
    fun long(key: String): Long? = params[key]?.firstOrNull()?.toLongOrNull()
    fun string(key: String): String? = params[key]?.firstOrNull()
    fun boolean(key: String): Boolean? = params[key]?.firstOrNull()?.toBooleanStrictOrNull()
    fun double(key: String): Double? = params[key]?.firstOrNull()?.toDoubleOrNull()

    // 複数値の取得
    fun list(key: String): List<String> = params[key] ?: emptyList()
    fun intList(key: String): List<Int> = params[key]?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    fun longList(key: String): List<Long> = params[key]?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    // inline reified を使った型推論版（単一値）
    inline fun <reified T> get(key: String): T? {
        val value = params[key]?.firstOrNull() ?: return null
        return when (T::class) {
            Int::class -> value.toIntOrNull() as? T
            Long::class -> value.toLongOrNull() as? T
            String::class -> value as? T
            Boolean::class -> value.toBooleanStrictOrNull() as? T
            Double::class -> value.toDoubleOrNull() as? T
            else -> null
        }
    }

    companion object {
        fun empty(): QueryParams = QueryParams(emptyMap())
        internal fun of(params: Map<String, List<String>>): QueryParams = QueryParams(params)
    }
}
```

### Step 2: Request.kt の更新

**ファイル**: `src/main/kotlin/io/github/kamellia/core/Request.kt`

- `typealias Headers = Map<String, String>` は残す
- `typealias QueryParams` と `typealias PathParams` を削除
- `Request` データクラスはそのまま（プロパティの型が変わるだけ）

### Step 3: PathPatternMatcher の更新

**ファイル**: `src/main/kotlin/io/github/kamellia/routing/PathPatternMatcher.kt`

- `match()` メソッドの返り値を `Map<String, String>?` から `PathParams?` に変更
- 内部実装: `PathParams.of(params)` を返す

### Step 4: RequestConverter の更新

**ファイル**: `src/main/kotlin/io/github/kamellia/netty/RequestConverter.kt`

- `queryParams` の生成を `QueryParams.of(decoder.parameters().mapValues { it.value })` に変更
- `pathParams` を `PathParams.empty()` に変更

### Step 5: Router の更新

**ファイル**: `src/main/kotlin/io/github/kamellia/routing/Router.kt`

- `match()` メソッド内で `route.matcher.match()` の返り値を受け取る部分は変更不要
  （`PathPatternMatcher.match()` が既に `PathParams?` を返すため）

### Step 6: テストの更新

#### `src/test/kotlin/io/github/kamellia/core/RequestTest.kt`

- `queryParams = emptyMap()` → `queryParams = QueryParams.empty()`
- `pathParams = emptyMap()` → `pathParams = PathParams.empty()`
- `request.queryParams["filter"]` → `request.queryParams.list("filter")`
- `request.pathParams["id"]` → `request.pathParams.string("id")`

#### `src/test/kotlin/io/github/kamellia/netty/RequestConverterTest.kt`

- `request.queryParams["q"]` → `request.queryParams.list("q")`
- `request.queryParams["page"]` → `request.queryParams.list("page")`
- `request.queryParams["tag"]` → `request.queryParams.list("tag")`

### Step 7: 新しいテストの追加

#### `src/test/kotlin/io/github/kamellia/core/PathParamsTest.kt`

- `int()`, `long()`, `string()`, `boolean()`, `double()` のテスト
- `get<T>()` の型推論テスト
- パースエラー時の `null` 返却テスト
- `empty()` のテスト
- `equals()`, `hashCode()` のテスト（value classで自動生成されるため、基本的な動作確認のみ）

#### `src/test/kotlin/io/github/kamellia/core/QueryParamsTest.kt`

- 単一値取得のテスト（`int()`, `long()`, `string()` など）
- 複数値取得のテスト（`list()`, `intList()`, `longList()`）
- `get<T>()` の型推論テスト
- 存在しないキーへのアクセステスト
- `empty()` のテスト
- `equals()`, `hashCode()` のテスト（value classで自動生成されるため、基本的な動作確認のみ）

#### `src/test/kotlin/io/github/kamellia/routing/PathPatternMatcherTest.kt` の更新

- 既存テストで返り値の型が `PathParams?` になることを確認
- 型安全なアクセスのテストを追加

## 実装順序

1. **Phase 1**: 新しいクラスの作成
   - `PathParams.kt` を作成
   - `QueryParams.kt` を作成
   - 新しいクラスのテストを作成（`PathParamsTest.kt`, `QueryParamsTest.kt`）

2. **Phase 2**: Request.kt の更新
   - `typealias` を削除
   - コンパイルエラーを確認

3. **Phase 3**: コア実装の更新
   - `PathPatternMatcher.kt` を更新
   - `RequestConverter.kt` を更新
   - `Router.kt` は変更不要のはず

4. **Phase 4**: テストの更新
   - `RequestTest.kt` を更新
   - `RequestConverterTest.kt` を更新
   - `PathPatternMatcherTest.kt` を更新
   - すべてのテストが通ることを確認

5. **Phase 5**: 検証
   - `./gradlew compileTestKotlin` でコンパイルが通ることを確認
   - `test-runner` でテストが通ることを確認
   - `code-quality-checker` で品質チェックが通ることを確認

## 破壊的変更の影響

この変更は以下の点で破壊的です：

1. **型の変更**: `PathParams` と `QueryParams` が `Map` から専用クラスに変更
2. **アクセス方法の変更**: `params["key"]` から `params.string("key")` などに変更

ただし、これはまだ初期段階のライブラリであり、外部ユーザーがいないため問題ありません。

## 利点

1. **型安全性**: コンパイル時に型エラーを検出できる
2. **明示的な型変換**: `int()`, `long()` などで意図が明確
3. **null安全**: パースエラーを `null` で表現し、明示的に処理できる
4. **拡張性**: 新しい型のサポートを簡単に追加できる
5. **パフォーマンス**: value class により実行時のオーバーヘッドがない
6. **シンプルさ**: `equals()`, `hashCode()`, `toString()` が自動生成される

## リスク

1. **コンパイルエラー**: 多くのファイルで型の変更が必要
2. **テストの更新**: すべてのテストケースを更新する必要がある
3. **見落とし**: 一部のファイルで更新を忘れる可能性

## 成功基準

- [ ] すべてのコンパイルエラーが解決される
- [ ] すべての既存テストが通る
- [ ] 新しいテスト（`PathParamsTest.kt`, `QueryParamsTest.kt`）が追加され、通る
- [ ] `./gradlew compileTestKotlin` が成功する
- [ ] `test-runner` が成功する
- [ ] `code-quality-checker` が成功する
- [ ] docs/design/design.md の内容と実装が一致している
