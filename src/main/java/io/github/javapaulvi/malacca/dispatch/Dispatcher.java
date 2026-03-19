package io.github.javapaulvi.malacca.dispatch;

import io.github.javapaulvi.malacca.exception.MalaccaException;
import io.github.javapaulvi.malacca.exception.http.InternalServerException;
import io.github.javapaulvi.malacca.http.HttpMethod;
import io.github.javapaulvi.malacca.http.Request;
import io.github.javapaulvi.malacca.http.responses.Response;
import io.github.javapaulvi.malacca.routing.RouteEntry;
import io.github.javapaulvi.malacca.routing.Router;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher {
    private final Router router;
    private final RequestBuilder requestBuilder = new RequestBuilder();
    private final HandlerInvoker invoker = new HandlerInvoker();
    private final ResponseWriter responseWriter = new ResponseWriter();
    private final ErrorHandler errorHandler;
    private final String malaccaName;
    private final Logger logger;

    public Dispatcher(Router router, String malaccaName){
        this.router = router;
        this.malaccaName = malaccaName;
        logger = LoggerFactory.getLogger("dispatch.Dispatcher:"+this.malaccaName);
        errorHandler = new ErrorHandler(responseWriter, malaccaName);
    }

    public void dispatch(HttpExchange exchange)  {
        try {
            RouteEntry entry = router.getRoute(HttpMethod.valueOf(exchange.getRequestMethod()), exchange.getRequestURI().getPath());
            Request<?> request = requestBuilder.build(exchange, entry);
            Response<?> response = invoker.invokeHandler(entry, request);
            responseWriter.writeResponse(exchange, response);
            logger.info("Endpoint called {} {} {}",exchange.getRequestMethod() , exchange.getRequestURI().getPath(), response.statusCode());
        } catch (MalaccaException e) {
            errorHandler.writeError(exchange, e);
        } catch (Exception e) {
            errorHandler.writeError(exchange, new InternalServerException("Unexpected Error: "+e.getMessage()));
        }




    }

}
