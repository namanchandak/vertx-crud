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
            router.get("/users/:username").handler(this::handleGetByIdResource);
            // Define POST endpoint
            router.post("/users").handler(this::handlePostResource);

            router.delete("/users/:username").handler(this::handleDeleteByIdResource);
            router.put("/users/:username").handler(this::handleUpdateResource);


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



        private void handleGetByIdResource(RoutingContext ctx) {
            String username = ctx.request().getParam("username");

            // Check if username parameter is provided


            // Prepare SQL query with parameter
            String sql = "SELECT * FROM userT WHERE username = ?";
            JsonArray params = new JsonArray().add(username);

            // Execute the SQL query
            client.queryWithParams(sql, params, res -> {
                if (res.succeeded()) {
                    // Retrieve the query result
                    if (res.result().getNumRows() > 0) {
                        JsonObject user = res.result().getRows().get(0);
                        ctx.response()
                                .putHeader("content-type", "application/json")
                                .end(user.encode());
                    } else {
                        // Handle case where no user is found with the given username
                        ctx.response().setStatusCode(404).end("User not found");
                    }
                } else {
                    // Handle query execution failure
                    ctx.fail(500);
                }
            });
        }

        private void handleDeleteByIdResource(RoutingContext ctx) {
            String username = ctx.request().getParam("username");

            // Check if username parameter is provided
            if (username == null || username.isEmpty()) {
                ctx.response().setStatusCode(400).end("Username parameter is required");
                return;
            }

            // Prepare SQL query with parameter
            String sql = "DELETE FROM userT WHERE username = ?";
            JsonArray params = new JsonArray().add(username);

            // Execute the SQL DELETE query
            client.updateWithParams(sql, params, res -> {
                if (res.succeeded()) {
                    if (res.result().getUpdated() > 0) {
                        ctx.response().setStatusCode(204).end();
                    } else {
                        ctx.response().setStatusCode(404).end("User not found");
                    }
                } else {
                    // Handle query execution failure
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

        private void handleUpdateResource(RoutingContext ctx) {
            JsonObject body = ctx.getBodyAsJson();

            // Logging to inspect the JSON body
            System.out.println("Received JSON body: " + body.encodePrettily());

            // Extract the username from the URL parameter
            String username = ctx.request().getParam("username");

            // Initialize the SQL update query
            StringBuilder sql = new StringBuilder("UPDATE userT SET ");
            JsonArray params = new JsonArray();
            boolean first = true;

            // Dynamically add fields to be updated
            if (body.containsKey("email")) {
                if (!first) sql.append(", ");
                sql.append("email = ?");
                params.add(body.getString("email"));
                first = false;
            }

            // Add more fields as necessary in a similar manner
            // Example:

            // Ensure at least one field is being updated
            if (params.isEmpty()) {
                ctx.response().setStatusCode(400).end("No fields to update");
                return;
            }

            // Append the WHERE clause
            sql.append(" WHERE username = ?");
            params.add(username);

            System.out.println(sql);

            // Execute the SQL update query
            client.updateWithParams(sql.toString(), params, res -> {
                if (res.succeeded()) {
                    if (res.result().getUpdated() == 0) {
                        // Handle case where no user is found with the given username
                        ctx.response().setStatusCode(404).end("User not found");
                    } else {
                        ctx.response().setStatusCode(200).end("User updated successfully");
                    }
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
