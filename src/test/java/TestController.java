import io.github.javapaulvi.malacca.annotation.*;
import io.github.javapaulvi.malacca.http.Request;

@Controller("/hello")
public class TestController {

    @Header(name = "X-test")
    @Description("Hello World")
    @QueryParam(name = "name", required = true)
    @POST("/world/{param}")
    public TestControllerResponse hello(Request<TestControllerRequest> req) {
        TestControllerRequest body = req.body();
        String retStr = "Hi my name is "+body.name()+" and i am "+body.age()+" years old";
        return new TestControllerResponse("PathParameter is "+req.pathParam("param")+" and X-test = "+req.firstHeader("X-test")+" and QueryParam name = "+req.queryParam("name"), retStr);
    }
}