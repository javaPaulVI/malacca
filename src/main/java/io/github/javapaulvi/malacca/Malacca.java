package io.github.javapaulvi.malacca;

import io.github.javapaulvi.malacca.dispatch.Dispatcher;
import io.github.javapaulvi.malacca.openapi.ApiInfo;
import io.github.javapaulvi.malacca.openapi.OpenApiGenerator;
import io.github.javapaulvi.malacca.routing.Router;
import io.github.javapaulvi.malacca.server.MalaccaHttpServer;
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
        String openApiJson = docsUrl!=null ? OpenApiGenerator.generate(router, apiInfo) : null;
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

    public void stop(){
        httpServer.stop();
    }


}
