import com.fasterxml.jackson.databind.ObjectMapper;
import dev.javapaul.malacca.Malacca;
import dev.javapaul.malacca.annotation.*;
import dev.javapaul.malacca.exception.http.*;
import dev.javapaul.malacca.http.Request;
import dev.javapaul.malacca.http.responses.Response;
import dev.javapaul.malacca.http.responses.PlainTextResponse;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full integration test suite for the Malacca framework.
 * Tests two independent Malacca instances on different ports to verify isolation.
 * All request and response models use records.
 */
class MalaccaIntegrationTest {

    // -------------------------------------------------------------------------
    // Records — request and response models
    // -------------------------------------------------------------------------

    record CreateUserRequest(String name, int age) {}
    record UserResponse(String id, String name, int age) {}
    record MessageResponse(String message) {}
    record EchoResponse(String body, String header, String queryParam, String pathParam) {}
    record ErrorResponse(String message) {}
    record ProductRequest(String title, double price) {}
    record ProductResponse(String id, String title, double price) {}

    // -------------------------------------------------------------------------
    // Controllers
    // -------------------------------------------------------------------------

    @Controller("/users")
    static class UserController {

        @Description("Create a new user")
        @POST("/")
        public Response<UserResponse> create(Request<CreateUserRequest> req) {
            CreateUserRequest body = req.body();
            if (body.name() == null || body.name().isBlank())
                throw new BadRequestException("Name is required");
            return new Response<UserResponse>(new UserResponse("123", body.name(), body.age())).status(201);
        }

        @Description("Get a user by ID")
        @GET("/{id}")
        public UserResponse getById(Request<?> req) {
            String id = req.pathParam("id");
            if (id.equals("999")) throw new NotFoundException("User not found");
            return new UserResponse(id, "John Doe", 30);
        }

        @Description("Delete a user")
        @DELETE("/{id}")
        public Response<Void> delete(Request<?> req) {
            String id = req.pathParam("id");
            if (id.equals("999")) throw new NotFoundException("User not found");
            return Response.unstructured().status(204);
        }
    }

    @Controller("/products")
    static class ProductController {

        @Description("Create a product")
        @POST("/")
        public Response<ProductResponse> create(Request<ProductRequest> req) {
            ProductRequest body = req.body();
            if (body.price() < 0) throw new UnprocessableEntityException("Price cannot be negative");
            return Response.of(new ProductResponse("p-1", body.title(), body.price())).status(201);
        }

        @Description("Get a product")
        @GET("/{id}")
        public ProductResponse getById(Request<?> req) {
            return new ProductResponse(req.pathParam("id"), "Test Product", 9.99);
        }
    }

    @Controller("/echo")
    static class EchoController {

        @Header(name = "X-Api-Key", required = true)
        @QueryParam(name = "q", required = true)
        @Description("Echo back params")
        @POST("/{param}")
        public EchoResponse echo(Request<CreateUserRequest> req) {
            CreateUserRequest body = req.body();
            return new EchoResponse(
                    body.name(),
                    req.firstHeader("X-Api-Key"),
                    req.queryParam("q"),
                    req.pathParam("param")
            );
        }
    }

    @Controller("/health")
    static class HealthController {

        @GET("/")
        public PlainTextResponse health(Request<?> req) {
            return new PlainTextResponse("OK");
        }
    }

    // Second app's controllers — completely separate
    @Controller("/admin")
    static class AdminController {

        @GET("/status")
        public MessageResponse status(Request<?> req) {
            return new MessageResponse("admin server running");
        }

        @GET("/secret")
        @Header(name = "X-Admin-Key", required = true)
        public MessageResponse secret(Request<?> req) {
            String key = req.firstHeader("X-Admin-Key");
            if (!("supersecret".equals(key))) throw new UnauthorizedException("Invalid admin key");
            return new MessageResponse("welcome admin");
        }
    }

    // -------------------------------------------------------------------------
    // Server setup — two independent Malacca instances
    // -------------------------------------------------------------------------

    private static Malacca app;        // main app on 8081
    private static Malacca adminApp;   // admin app on 8082

    private static final String BASE_URL       = "http://localhost:8081";
    private static final String ADMIN_BASE_URL = "http://localhost:8082";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServers() {
        app = new Malacca("test-app");
        app.register(new UserController(), new ProductController(), new EchoController(), new HealthController());
        app.listen(8081);

        adminApp = new Malacca("admin-app");
        adminApp.register(new AdminController());
        adminApp.listen(8082);
    }

    @AfterAll
    static void stopServers() {
        app.stop();
        adminApp.stop();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> get(String url) throws Exception {
        return client.send(
                HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(String url, Object body) throws Exception {
        return client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> postWithHeaders(String url, Object body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String url) throws Exception {
        return client.send(
                HttpRequest.newBuilder().uri(URI.create(url)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private <T> T parse(HttpResponse<String> response, Class<T> type) throws Exception {
        return objectMapper.readValue(response.body(), type);
    }

    // -------------------------------------------------------------------------
    // User controller tests
    // -------------------------------------------------------------------------

    @Test
    void createUser_shouldReturn201WithBody() throws Exception {
        var response = post(BASE_URL + "/users/", new CreateUserRequest("Alice", 28));
        var body = parse(response, UserResponse.class);


        assertEquals(201, response.statusCode());
        assertEquals("Alice", body.name());
        assertEquals(28, body.age());
        assertNotNull(body.id());
    }

    @Test
    void createUser_shouldReturn400WhenNameIsBlank() throws Exception {
        var response = post(BASE_URL + "/users/", new CreateUserRequest("", 28));
        var body = parse(response, ErrorResponse.class);

        assertEquals(400, response.statusCode());
        assertNotNull(body.message());
    }

    @Test
    void getUser_shouldReturn200() throws Exception {
        var response = get(BASE_URL + "/users/42");
        var body = parse(response, UserResponse.class);

        assertEquals(200, response.statusCode());
        assertEquals("42", body.id());
        assertEquals("John Doe", body.name());
    }

    @Test
    void getUser_shouldReturn404ForUnknownId() throws Exception {
        var response = get(BASE_URL + "/users/999");

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteUser_shouldReturn204() throws Exception {
        var response = delete(BASE_URL + "/users/42");

        assertEquals(204, response.statusCode());
    }

    @Test
    void deleteUser_shouldReturn404ForUnknownId() throws Exception {
        var response = delete(BASE_URL + "/users/999");

        assertEquals(404, response.statusCode());
    }

    // -------------------------------------------------------------------------
    // Product controller tests
    // -------------------------------------------------------------------------

    @Test
    void createProduct_shouldReturn201() throws Exception {
        var response = post(BASE_URL + "/products/", new ProductRequest("Widget", 19.99));
        var body = parse(response, ProductResponse.class);

        assertEquals(201, response.statusCode());
        assertEquals("Widget", body.title());
        assertEquals(19.99, body.price());
    }

    @Test
    void createProduct_shouldReturn422ForNegativePrice() throws Exception {
        var response = post(BASE_URL + "/products/", new ProductRequest("Widget", -5.00));

        assertEquals(422, response.statusCode());
    }

    @Test
    void getProduct_shouldReturn200() throws Exception {
        var response = get(BASE_URL + "/products/p-99");
        var body = parse(response, ProductResponse.class);

        assertEquals(200, response.statusCode());
        assertEquals("p-99", body.id());
    }

    // -------------------------------------------------------------------------
    // Echo controller tests — headers, query params, path params, body together
    // -------------------------------------------------------------------------

    @Test
    void echo_shouldReturnAllParamsCorrectly() throws Exception {
        var response = postWithHeaders(
                BASE_URL + "/echo/myparam?q=hello",
                new CreateUserRequest("Bob", 22),
                Map.of("X-Api-Key", "key123")
        );
        var body = parse(response, EchoResponse.class);

        assertEquals(200, response.statusCode());
        assertEquals("Bob", body.body());
        assertEquals("key123", body.header());
        assertEquals("hello", body.queryParam());
        assertEquals("myparam", body.pathParam());
    }

    // -------------------------------------------------------------------------
    // Health check — plain text response
    // -------------------------------------------------------------------------

    @Test
    void health_shouldReturnOk() throws Exception {
        var response = get(BASE_URL + "/health/");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    // -------------------------------------------------------------------------
    // 404 for completely unknown routes
    // -------------------------------------------------------------------------

    @Test
    void unknownRoute_shouldReturn404() throws Exception {
        var response = get(BASE_URL + "/doesnotexist");

        assertEquals(404, response.statusCode());
    }

    // -------------------------------------------------------------------------
    // Two Malacca instances are fully isolated
    // -------------------------------------------------------------------------

    @Test
    void adminApp_shouldRunIndependentlyOnDifferentPort() throws Exception {
        var response = get(ADMIN_BASE_URL + "/admin/status");
        var body = parse(response, MessageResponse.class);

        assertEquals(200, response.statusCode());
        assertEquals("admin server running", body.message());
    }

    @Test
    void adminApp_routesShouldNotBeVisibleOnMainApp() throws Exception {
        // admin routes should 404 on the main app
        var response = get(BASE_URL + "/admin/status");

        assertEquals(404, response.statusCode());
    }

    @Test
    void mainApp_routesShouldNotBeVisibleOnAdminApp() throws Exception {
        // main app routes should 404 on the admin app
        var response = get(ADMIN_BASE_URL + "/users/1");

        assertEquals(404, response.statusCode());
    }

    @Test
    void adminSecret_shouldReturn200WithCorrectKey() throws Exception {
        var response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(ADMIN_BASE_URL + "/admin/secret"))
                        .header("x-admin-key", "supersecret")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        var body = parse(response, MessageResponse.class);

        assertEquals(200, response.statusCode());
        assertEquals("welcome admin", body.message());
    }

    @Test
    void adminSecret_shouldReturn401WithWrongKey() throws Exception {
        var response = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(ADMIN_BASE_URL + "/admin/secret"))
                        .header("X-Admin-Key", "wrongkey")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(401, response.statusCode());
    }

    @Test
    void adminSecret_shouldReturn401WithNoKey() throws Exception {
        var response = get(ADMIN_BASE_URL + "/admin/secret");

        assertEquals(401, response.statusCode());
    }
}