package com.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.http.HttpServerResponse;

public class App {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

//        router.route("/hello").handler(App::handleRequest);

        Route handler1 = router
                .get("/hello/:name")
                .handler(routingContext -> {
                    String name = routingContext.request().getParam("name");
                    System.out.println("came to hello: get"+name );
                    HttpServerResponse response = routingContext.response();
                    response.setChunked(true);
                    response.write("Hi "+name);
                    response.end();

                });

        Route handler2 = router
                .post("/hello")
                .handler(routingContext -> {
                    System.out.println("came to hello: post" );
                    HttpServerResponse response = routingContext.response();
                    response.setChunked(true);
                    response.write("Hi from post");
                    response.end();

                });






        httpServer
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("Server is now listening on port 8080");
                    } else {
                        System.out.println("Failed to bind on port 8080");
                    }
                });
    }

    private static void handleRequest(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "text/plain");
        response.end("Hi, I am Naman");
    }
}
