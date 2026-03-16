package dev.javapaul.malacca;

import dev.javapaul.malacca.dispatch.Dispatcher;
import dev.javapaul.malacca.openapi.ApiInfo;
import dev.javapaul.malacca.openapi.OpenApiGenerator;
import dev.javapaul.malacca.routing.Router;
import dev.javapaul.malacca.server.MalaccaHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Malacca {
    private Router router;
    private Dispatcher dispatcher;
    private Logger logger;
    private String docsUrl = null;
    private ApiInfo apiInfo = null;
    private String name;
    private final MalaccaHttpServer httpServer = new MalaccaHttpServer();


    public Malacca(String name) {
        initFields(name);

    }


    public Malacca register(Object... controllers){
        router.registerControllers(
                controllers
        );        return this;
    }

    public void listen(int port){
        String openApiJson = OpenApiGenerator.generate(router, apiInfo);
        httpServer.start(port, name, dispatcher, docsUrl, openApiJson);
    }

    public Malacca docs(String docsUrl) {
        this.docsUrl = docsUrl;
        router.enableDocs(docsUrl);
        this.apiInfo = new ApiInfo(this.name, "0.0.1", "");
        return this; // fluent chaining
    }
    public Malacca docs(String docsUrl, ApiInfo info) {
        this.docsUrl = docsUrl;
        this.apiInfo = info;
        router.enableDocs(docsUrl);
        return this;
    }

    private void initFields(String name){
        this.name = name;
        router = new Router(name);
        dispatcher = new Dispatcher(router, name);
        logger = LoggerFactory.getLogger("Malacca:"+ name);}



}
