package com.vertx; // Replace with your actual package name

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class App {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MyVerticle());
    }

    public static class MyVerticle extends AbstractVerticle {

        private JDBCClient client;

        @Override
        public void start(Promise<Void> startPromise) {
            // Configure MySQL connection
            client = JDBCClient.create(vertx, new JsonObject()
                    .put("url", "jdbc:mysql://localhost:3306/testdb")
                    .put("driver_class", "com.mysql.cj.jdbc.Driver")
                    .put("user", "root")
                    .put("password", "root")
                    .put("max_pool_size", 30));

            // Create a router object
            Router router = Router.router(vertx);

            // Enable parsing of request bodies
            router.route().handler(BodyHandler.create());

            // Define GET endpoint
            router.get("/users").handler(this::handleGetResource);

            // Define POST endpoint
            router.post("/users").handler(this::handlePostResource);

            // Start the HTTP server
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8080, ar -> {
                        if (ar.succeeded()) {
                            System.out.println("Server started on port 8080");
                            startPromise.complete();
                        } else {
                            System.out.println("Server start failed: " + ar.cause());
                            startPromise.fail(ar.cause());
                        }
                    });
        }

        // Handler for GET /api/resource
        private void handleGetResource(RoutingContext ctx) {
            client.query("SELECT * FROM userT", res -> {
                if (res.succeeded()) {
                    ctx.response()
                            .putHeader("content-type", "application/json")
                            .end(res.result().toJson().encode());
                } else {
                    ctx.fail(500);
                }
            });
        }

        // Handler for POST /api/resource
        // Handler for POST /users
        private void handlePostResource(RoutingContext ctx) {
            JsonObject body = ctx.getBodyAsJson();

            // Logging to inspect the JSON body
            System.out.println("Received JSON body: " + body.encodePrettily());

            // Extract data from the JSON body
            String username = body.getString("username");
            String email = body.getString("email");

            // Validate incoming data
            if (username == null || username.isEmpty() || email == null || email.isEmpty()) {
                ctx.response().setStatusCode(400).end("Username and email are required");
                return;
            }

            // Prepare parameters for the SQL query
            JsonArray params = new JsonArray().add(username).add(email);

            // Execute the SQL INSERT query
            client.updateWithParams("INSERT INTO userT (username, email) VALUES (?, ?)", params, res -> {
                if (res.succeeded()) {
                    ctx.response().setStatusCode(201).end();
                } else {
                    ctx.fail(500);
                }
            });
        }


        @Override
        public void stop() {
            // Close the database client when stopping the verticle
            client.close();
        }
    }
}
