# Malacca

A lightweight Java API framework inspired by FastAPI. Annotation-based routing, typed request/response models, built-in Swagger UI documentation, and zero magic — everything is explicit.

---

## Installation

### Gradle
```kotlin
implementation("io.github.javapaulvi:malacca:malacca-latest:0.1.0")
```

### Maven
```xml
<dependency>
    <groupId>io.github.javapaulvi</groupId>
    <artifactId>malacca</artifactId>
    <version>malacca-latest:0.1.0</version>
</dependency>
```

**Stable release:**
```kotlin
implementation("io.github.javapaulvi:malacca:malacca-stable:0.1.0")
```

Requires Java 21 or higher. Find more information on [Maven Central]()

---

## Quick Start

```java
// 1. Define a controller
@Controller("/hello")
public class HelloController {

    @GET("/{name}")
    public MessageResponse hello(Request<?> req) {
        return new MessageResponse("Hello, " + req.pathParam("name") + "!");
    }
}

record MessageResponse(String message) {}

// 2. Start the server
public class Main {
    public static void main(String[] args) {
        var app = new Malacca("my-app");
        app.register(new HelloController());
        app.listen(8080);
    }
}
```

Hit `GET http://localhost:8080/hello/World` and you get:
```json
{ "message": "Hello, World!" }
```

---

## Controllers & Routing

Controllers are plain Java classes annotated with `@Controller`. Each handler method is annotated with an HTTP method annotation and a path.

```java
@Controller("/users")
public class UserController {

    @GET("/{id}")
    public UserResponse getUser(Request<?> req) {
        String id = req.pathParam("id");
        return new UserResponse(id, "John Doe");
    }

    @POST("/")
    public Response<UserResponse> createUser(Request<CreateUserRequest> req) {
        CreateUserRequest body = req.body();
        return Response.of(new UserResponse("123", body.name())).status(201);
    }

    @DELETE("/{id}")
    public Response<Void> deleteUser(Request<?> req) {
        return Response.of(null).status(204);
    }
}
```

**Supported annotations:** `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE`

**Path parameters** use `{name}` syntax and are extracted via `req.pathParam("name")`.

**Registering controllers:**
```java
app.register(new UserController(), new ProductController());
```
or
```java
app.register(new UserController());
app.register(new ProductController());
```

---

## Request

Every handler receives a `Request<T>` object. `T` is the expected body type — use `Request<?>` when there is no body, **although it's strongly recommended to use a defined model record**.

```java
req.pathParam("id")           // path parameter — /users/{id}
req.queryParam("page")        // query parameter — ?page=2
req.firstHeader("X-Api-Key")  // first value of a header
req.header("Accept")          // all values of a header as List<String>, for headers with multiple values
req.allHeaders()              // all headers as Map<String, List<String>>
req.rawBody()                 // raw body string
req.body()                    // typed body — ONLY on Request<T>, throws on Request<?>
req.bodyMap()                 // body as Map<String, Object> — use on Request<?>
req.method()                  // HTTP method
req.path()                    // request path
```

**Typed body — declare a model record:**
```java
record CreateUserRequest(String name, int age) {}

@POST("/")
public UserResponse create(Request<CreateUserRequest> req) {
    CreateUserRequest body = req.body();
    String name = body.name();
    int age = body.age();}
```

**Untyped body — use bodyMap():**
```java
@POST("/ingest")
public MessageResponse ingest(Request<?> req) {
    Map<String, Object> fields = req.bodyMap();
    String name = (String) fields.get("name");
}
```

> Note: Header names are case-insensitive. `req.firstHeader("X-Api-Key")` and `req.firstHeader("x-api-key")` both work.

---

## Response

Handler methods can return either a plain object (automatically wrapped as a 200 response) or a `Response<T>` for full control, **again a typed response is strongly recommended**.

**Plain return — 200 with JSON body:**
```java
@GET("/{id}")
public UserResponse getUser(Request<?> req) {
    return new UserResponse("123", "John");
}
```

**Full control — custom status and headers:**
```java
@POST("/")
public Response<UserResponse> createUser(Request<CreateUserRequest> req) {
    return Response.of(new UserResponse("123", "John"))
                   .status(201)
                   .header("X-Request-Id", "abc123");
}
```

**No body — 204:**
```java
@DELETE("/{id}")
public Response<Void> delete(Request<?> req) {
    return Response.of(null).status(204);
}
```

**Unstructured response — no model needed:**
```java
@GET("/info")
public Response<Void> info(Request<?> req) {
    return Response.unstructured()
                   .field("version", "1.0.0")
                   .field("status", "running");
}
```

**Plain text:**
```java
@GET("/health")
public PlainTextResponse health(Request<?> req) {
    return new PlainTextResponse("OK");
}
```

**Redirect:**
```java
@GET("/old")
public RedirectResponse redirect(Request<?> req) {
    return new RedirectResponse("/new");
}
```

---

## Documentation (Swagger UI)

Enable Swagger UI with one line:

```java
var app = new Malacca("my-app");
app.register(new UserController());
app.docs("/docs", new ApiInfo("My API", "1.0.0", "API description"));
app.listen(8080);
```

Then open `http://localhost:8080/docs` in your browser.

**Annotate your endpoints for richer docs:**

```java
@Description("Get a user by their ID")
@QueryParam(name = "verbose", required = false, type = "boolean")
@Header(name = "X-Api-Key", required = true)
@GET("/{id}")
public UserResponse getUser(Request<?> req) {
    // ...
}
```

For now, those Annotations are supported, potentially more in the future (if a request doesn't fulfill the annotation, no exception is thrown at the moment, coming in the next release):

- `@Description` — summary shown in Swagger UI
- `@QueryParam` / `@QueryParams` — documents query parameters
- `@Header` / `@Headers` — documents expected request headers


The OpenAPI spec is available at `/openapi.json`.

---

## Exception Handling

Throw built-in exceptions from any handler — Malacca catches them and returns the correct HTTP status automatically.

```java
@GET("/{id}")
public UserResponse getUser(Request<?> req) {
    String id = req.pathParam("id");
    if (id == null) throw new BadRequestException("ID is required");
    if (!exists(id)) throw new NotFoundException("User not found");
    return findUser(id);
}
```

| Exception | Status |
|---|---|
| `BadRequestException` | 400 |
| `UnauthorizedException` | 401 |
| `ForbiddenException` | 403 |
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `UnprocessableEntityException` | 422 |
| `InternalServerException` | 500 |

Any unhandled exception is caught by the framework and returned as a 500.

---

## Multiple Instances

Multiple independent Malacca instances can run on different ports in the same JVM — each has its own router and dispatcher:

```java
var api = new Malacca("api");
api.register(new UserController());
api.listen(8080);

var admin = new Malacca("admin");
admin.register(new AdminController());
admin.listen(9090);
```

Routes registered on one instance are completely invisible to the other.

---

## Contributing

Issues and pull requests are welcome at [github.com/javaPaulVI/malacca](https://github.com/javaPaulVI/malacca).

---

## License

MIT License — see [LICENSE](LICENSE) for details.