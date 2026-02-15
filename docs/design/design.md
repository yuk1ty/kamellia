# Kamellia - Kotlin HTTP Server Library Design

## 概要

KamelliaはNettyをベースとした、型安全で関数型プログラミングスタイルのKotlin HTTPサーバーライブラリです。
HonoやAxumのように、`Handler = Request -> Response`という単純な型付けを中心に設計します。

## 設計思想

### 1. 核となる型定義

```kotlin
typealias Handler = suspend (Request) -> Response
```

この単純な関数型を基本とし、すべてのHTTPハンドリングロジックを構築します。

### 2. 主要な設計原則

- **Simplicity**: シンプルで直感的なAPI
- **Type Safety**: コンパイル時の型安全性を最大限活用
- **Composability**: ミドルウェアやハンドラの合成可能性
- **Performance**: Nettyの非同期性能を活かす
- **Extensibility**: ミドルウェアによる柔軟な拡張性

## コア設計

### 1. Request/Response モデル

```kotlin
data class Request(
    val method: HttpMethod,
    val path: String,
    val headers: Headers,
    val queryParams: QueryParams, // 型安全なアクセスを提供するクラス
    val pathParams: PathParams,   // 型安全なアクセスを提供するクラス
    val body: Body,
    val context: Context
)

data class Response(
    val status: HttpStatus,
    val headers: Headers,
    val body: Body
) {
    companion object {
        fun ok(body: String = ""): Response
        fun json(data: Any): Response
        fun notFound(): Response
        fun badRequest(message: String = "Bad Request"): Response
        fun internalServerError(message: String): Response
    }
}
```

### 2. ルーティングシステム

Honoスタイルのチェーン可能なルーティングAPI:

```kotlin
class Kamellia {
    fun get(path: String, handler: Handler): Kamellia
    fun post(path: String, handler: Handler): Kamellia
    fun put(path: String, handler: Handler): Kamellia
    fun delete(path: String, handler: Handler): Kamellia
    fun patch(path: String, handler: Handler): Kamellia

    // ルートグループ化
    fun route(prefix: String, block: Kamellia.() -> Unit): Kamellia

    // サーバー起動
    suspend fun start(port: Int)
}
```

使用例:

```kotlin
val app = Kamellia()

app.get("/") { req ->
    Response.ok("Hello, Kamellia!")
}

app.get("/users/:id") { req ->
    val userId = req.pathParams.int("id")
        ?: return@get Response.badRequest("Invalid user ID")
    Response.json(mapOf("userId" to userId))
}

app.route("/api") {
    get("/health") { Response.ok("OK") }

    route("/v1") {
        get("/users") { Response.json(users) }
        post("/users") { req ->
            // create user
            Response.json(newUser)
        }
    }
}

app.start(3000)
```

### 3. ミドルウェアシステム

ミドルウェアもHandlerを変換する関数として表現:

```kotlin
typealias Middleware = (Handler) -> Handler
```

実装例:

```kotlin
// ロギングミドルウェア
fun loggingMiddleware(): Middleware = { next ->
    { req ->
        println("${req.method} ${req.path}")
        val response = next(req)
        println("Response: ${response.status}")
        response
    }
}

// CORSミドルウェア
fun corsMiddleware(config: CorsConfig): Middleware = { next ->
    { req ->
        val response = next(req)
        response.copy(
            headers = response.headers + mapOf(
                "Access-Control-Allow-Origin" to config.origin
            )
        )
    }
}

// JSONミドルウェア（kotlinx.serializationベース）
// Response.body が JsonElement の場合に自動的にシリアライズ
fun jsonMiddleware(json: Json = Json): Middleware = { next ->
    { req ->
        val response = next(req)
        // レスポンスボディがTextで、Content-Typeがapplication/jsonの場合は既に処理済み
        response
    }
}

// 使用例
app.use(loggingMiddleware())
app.use(corsMiddleware(CorsConfig(origin = "*")))
app.use(jsonMiddleware())
```

### 4. パスパラメータとルーティングマッチング

```kotlin
interface RouteMatcher {
    fun match(path: String): RouteMatch?
}

data class RouteMatch(
    val pathParams: Map<String, String>,
    val handler: Handler
)

class PathPatternMatcher(private val pattern: String) : RouteMatcher {
    // "/users/:id/posts/:postId" のようなパターンをパース
    // 正規表現または独自のパーサーで実装
}
```

#### 型安全なパラメータアクセス

パスパラメータとクエリパラメータは、型安全性を最優先に設計します。
内部的には `Map<String, String>` を保持しますが、アクセスは専用のクラスを通じて行います。

##### パスパラメータの型安全アクセス

```kotlin
class PathParams internal constructor(
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

    // デバッグ用
    override fun toString(): String = params.toString()
}
```

##### クエリパラメータの型安全アクセス

```kotlin
class QueryParams internal constructor(
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

    // デバッグ用
    override fun toString(): String = params.toString()
}
```

##### Request型の定義

```kotlin
data class Request(
    val method: HttpMethod,
    val path: String,
    val headers: Headers,
    val queryParams: QueryParams,
    val pathParams: PathParams,
    val body: Body,
    val context: Context,
)
```

##### 使用例

```kotlin
// パスパラメータの取得（型名で明示的）
app.get("/users/:id") { req ->
    val userId = req.pathParams.int("id")
        ?: return@get Response.badRequest("Invalid user ID")
    Response.json(getUser(userId))
}

// inline reified 版（型推論が効く）
app.get("/posts/:postId") { req ->
    val postId: Long = req.pathParams.get("postId")
        ?: return@get Response.badRequest("Invalid post ID")
    Response.json(getPost(postId))
}

// クエリパラメータ（単一値）
app.get("/search") { req ->
    val page = req.queryParams.int("page") ?: 1
    val limit = req.queryParams.int("limit") ?: 10
    val sortBy = req.queryParams.string("sort") ?: "created_at"

    Response.json(search(page, limit, sortBy))
}

// クエリパラメータ（複数値）
app.get("/filter") { req ->
    val tags = req.queryParams.list("tags") // ["kotlin", "server"]
    val ids = req.queryParams.intList("ids") // [1, 2, 3]

    Response.json(filterItems(tags, ids))
}

// より複雑な例
app.get("/articles/:articleId/comments/:commentId") { req ->
    val articleId: Long = req.pathParams.get("articleId")
        ?: return@get Response.badRequest("Invalid article ID")
    val commentId: Int = req.pathParams.get("commentId")
        ?: return@get Response.badRequest("Invalid comment ID")

    val includeReplies = req.queryParams.boolean("replies") ?: false

    Response.json(getComment(articleId, commentId, includeReplies))
}
```

##### 設計のメリット

1. **コンパイル時の型安全性**: メソッド名で型が明確になる（`int()`, `string()` など）
2. **型推論の活用**: `get<T>()` を使えば、変数の型アノテーションから自動推論される
3. **null安全**: すべてのメソッドが nullable を返すため、パースエラーを明示的に処理できる
4. **複数値のサポート**: クエリパラメータの複数値を型安全に扱える
5. **拡張性**: 新しい型のサポートを簡単に追加できる
6. **デバッグしやすい**: `toString()` で内部状態を確認できる

##### 内部実装の隠蔽

`PathParams` と `QueryParams` のコンストラクタを `internal` にすることで、
フレームワーク内部でのみインスタンス化でき、ユーザーは型安全なAPIだけを使用することになります。

### 5. Context とステート管理

リクエストスコープのコンテキスト:

```kotlin
class Context {
    private val state = mutableMapOf<String, Any>()

    fun <T> get(key: String): T?
    fun <T> set(key: String, value: T)

    // ミドルウェア間でのデータ共有に使用
}

// 使用例（認証ミドルウェア）
fun authMiddleware(): Middleware = { next ->
    { req ->
        val token = req.headers["Authorization"]
        val user = authenticateToken(token)
        req.context.set("user", user)
        next(req)
    }
}

app.get("/profile") { req ->
    val user = req.context.get<User>("user")
    Response.json(user)
}
```

### 6. ボディパーシング (kotlinx.serialization)

```kotlin
sealed interface Body {
    data object Empty : Body
    data class Text(val content: String) : Body
    data class Binary(val bytes: ByteArray) : Body
    data class Stream(val channel: ReceiveChannel<ByteArray>) : Body
}

// Request拡張関数（kotlinx.serialization使用）
suspend fun Request.text(): String {
    return when (val body = this.body) {
        is Body.Text -> body.content
        is Body.Binary -> body.bytes.decodeToString()
        else -> ""
    }
}

// JSONデシリアライズ
suspend inline fun <reified T> Request.json(): T {
    val text = text()
    return Json.decodeFromString<T>(text)
}

// Response helper（kotlinx.serialization使用）
inline fun <reified T> Response.Companion.json(data: T): Response {
    return Response(
        status = HttpStatus.OK,
        headers = mapOf("Content-Type" to "application/json"),
        body = Body.Text(Json.encodeToString(data))
    )
}
```

### 7. エラーハンドリング

```kotlin
class HttpException(
    val status: HttpStatus,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// グローバルエラーハンドラ
app.onError { error, req ->
    when (error) {
        is HttpException -> Response(error.status, emptyMap(), Body.Text(error.message))
        else -> Response.internalServerError(error.message ?: "Internal Server Error")
    }
}
```

## Netty統合

### 1. アーキテクチャ

```
Netty Handler Pipeline
    ↓
HttpRequestDecoder → HttpObjectAggregator → KamelliaHandler
                                                    ↓
                                            Request変換
                                                    ↓
                                            Router/Middleware実行
                                                    ↓
                                            Response変換
                                                    ↓
                                            HttpResponseEncoder
```

### 2. KamelliaHandler実装

```kotlin
class KamelliaHandler(
    private val router: Router,
    private val middlewares: List<Middleware>,
    private val errorHandler: ErrorHandler
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        // コルーチンスコープでハンドラを実行
        ctx.executor().execute {
            runBlocking {
                try {
                    val request = convertToRequest(msg)
                    val handler = composeHandlers(router, middlewares)
                    val response = handler(request)
                    val nettyResponse = convertToNettyResponse(response)
                    ctx.writeAndFlush(nettyResponse)
                } catch (e: Exception) {
                    handleError(ctx, e)
                }
            }
        }
    }
}
```

## 高度な機能

### 1. WebSocketサポート

```kotlin
app.ws("/chat") { ws ->
    ws.onMessage { message ->
        ws.send("Echo: $message")
    }

    ws.onClose {
        println("WebSocket closed")
    }
}
```

### 2. ストリーミングレスポンス

```kotlin
app.get("/stream") { req ->
    Response(
        status = HttpStatus.OK,
        headers = mapOf("Content-Type" to "text/event-stream"),
        body = Body.Stream(flow {
            repeat(10) {
                emit("data: Event $it\n\n".toByteArray())
                delay(1000)
            }
        }.asChannel())
    )
}
```

### 3. ファイルアップロード

```kotlin
data class MultipartBody(
    val fields: Map<String, String>,
    val files: List<UploadedFile>
) : Body

data class UploadedFile(
    val name: String,
    val filename: String,
    val contentType: String,
    val content: ByteArray
)

app.post("/upload") { req ->
    val multipart = req.body as? MultipartBody
    val file = multipart?.files?.firstOrNull()
    Response.json(mapOf("uploaded" to file?.filename))
}
```

### 4. バリデーション

kotlinx.serializationと統合したバリデーション:

```kotlin
@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String
)

// バリデーション関数
fun CreateUserRequest.validate(): ValidationResult {
    val errors = mutableListOf<String>()

    if (name.length !in 3..50) {
        errors.add("Name must be between 3 and 50 characters")
    }

    if (!email.contains("@")) {
        errors.add("Invalid email format")
    }

    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}

// バリデーションミドルウェア
inline fun <reified T> validatingJsonMiddleware(
    crossinline validator: (T) -> ValidationResult
): Middleware = { next ->
    { req ->
        if (req.method == HttpMethod.POST || req.method == HttpMethod.PUT) {
            val body = req.json<T>()
            when (val result = validator(body)) {
                is ValidationResult.Valid -> {
                    req.context.set("validatedBody", body)
                    next(req)
                }
                is ValidationResult.Invalid -> {
                    Response(
                        status = HttpStatus.BAD_REQUEST,
                        headers = mapOf("Content-Type" to "application/json"),
                        body = Body.Text(Json.encodeToString(result.errors))
                    )
                }
            }
        } else {
            next(req)
        }
    }
}

// 使用例
app.post("/users") { req ->
    val body = req.context.get<CreateUserRequest>("validatedBody")!!
    // バリデーション済みのデータを使用
    Response.json(createUser(body))
}
```

## ミドルウェアライブラリ

プラグインシステムの代わりに、よく使われる機能はミドルウェアとして提供します。

### 提供するミドルウェア

```kotlin
// JSON処理（kotlinx.serialization）
fun jsonMiddleware(json: Json = Json): Middleware

// CORS
fun corsMiddleware(config: CorsConfig): Middleware

// ロギング
fun loggingMiddleware(logger: (String) -> Unit = ::println): Middleware

// 認証
fun bearerAuthMiddleware(validator: suspend (String) -> User?): Middleware

// レート制限
fun rateLimitMiddleware(
    windowMs: Long,
    max: Int,
    keyExtractor: (Request) -> String = { it.headers["X-Forwarded-For"] ?: "unknown" }
): Middleware

// 圧縮
fun compressionMiddleware(threshold: Int = 1024): Middleware

// 静的ファイル
fun staticMiddleware(root: String, prefix: String = "/"): Middleware
```

### 使用例

```kotlin
val app = Kamellia()

// ミドルウェアの適用
app.use(loggingMiddleware())
app.use(corsMiddleware(CorsConfig(
    origin = "*",
    methods = listOf("GET", "POST", "PUT", "DELETE"),
    headers = listOf("Content-Type", "Authorization")
)))
app.use(jsonMiddleware(Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}))
app.use(compressionMiddleware())

// 特定のルートにのみ適用
app.route("/api") {
    use(bearerAuthMiddleware { token ->
        // トークン検証ロジック
        validateToken(token)
    })
    use(rateLimitMiddleware(windowMs = 60_000, max = 100))

    get("/users") { req ->
        val user = req.context.get<User>("user")
        Response.json(user)
    }
}

app.start(3000)
```

## パフォーマンス最適化

### 1. 非同期処理

- すべてのハンドラをsuspend関数として実装
- Kotlin Coroutinesを活用した効率的な並行処理

### 2. ゼロコピー

- Nettyの ByteBuf を直接活用
- 不要なバッファコピーを最小化

### 3. 接続プーリング

- Nettyのチャネルプールを活用
- Keep-Aliveのサポート

### 4. コンパイル時ルーティング最適化

- 可能な限りルーティングマッチングをコンパイル時に解決
- Trie構造によるルックアップの高速化

## テスト戦略

```kotlin
class KamelliaTest {
    @Test
    fun testGetEndpoint() = runBlocking {
        val app = Kamellia()
        app.get("/hello") { Response.ok("Hello") }

        val testClient = app.testClient()
        val response = testClient.get("/hello")

        assertEquals(HttpStatus.OK, response.status)
        assertEquals("Hello", response.body.text())
    }
}
```

## 実装フェーズ

### Phase 1: Core

- [ ] Request/Response データクラス
- [ ] Handler型定義
- [ ] 基本的なルーティング
- [ ] Netty統合

### Phase 2: Middleware

- [ ] Middlewareシステム
- [ ] Context実装
- [ ] 基本的なミドルウェア（logging, CORS）

### Phase 3: Advanced Routing

- [ ] パスパラメータ
- [ ] クエリパラメータ
- [ ] ルートグループ化

### Phase 4: Body Handling

- [ ] JSONパーシング
- [ ] フォームデータ
- [ ] マルチパート

### Phase 5: Advanced Features

- [ ] WebSocketサポート
- [ ] ストリーミング
- [ ] エラーハンドリング

### Phase 6: Ecosystem

- [ ] 標準ミドルウェアライブラリ
- [ ] テストユーティリティ
- [ ] ドキュメント

## 参考実装

- **Hono** (TypeScript): シンプルなAPI設計
- **Axum** (Rust): 型安全性とextractor pattern
- **http4k** (Kotlin): 関数型アプローチ

## 依存関係

```kotlin
dependencies {
    // Netty
    implementation("io.netty:netty-all:4.1.100.Final")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
```

## まとめ

Kamelliaは、関数型の単純さとNettyのパフォーマンスを組み合わせた、モダンなKotlin HTTPサーバーライブラリを目指します。
`Handler = Request -> Response`という核となる型定義を中心に、kotlinx.serializationによる型安全なJSON処理と、
ミドルウェアによる柔軟な拡張性を持つエコシステムを構築します。
