package com.vertx;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.List;

public class App extends AbstractVerticle {

    private Database database;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new App());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // Configure Ebean programmatically
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(createDataSourceConfig());
        config.setDdlGenerate(true);
        config.setDdlRun(true);
        database = DatabaseFactory.create(config);

        // Create a Router
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Define the GET endpoint
        router.get("/api/items").handler(ctx -> {
            List<Item> items = database.find(Item.class).findList();
            ctx.json(items);
        });


        router.post("/api/items").handler(ctx -> {
            // Extract the 'name' from the request body
            JsonObject requestBody = ctx.getBodyAsJson();
            String itemName = requestBody.getString("name");

            // Create a new Item object and save it to the database
            Item newItem = new Item(itemName);
            database.save(newItem);

            // Return a success message or the created item
            ctx.json(newItem);
        });


        router.get("/api/items/:id").handler(ctx -> {
            String itemId = ctx.pathParam("id");
            Item item = database.find(Item.class, Long.parseLong(itemId));
            if (item != null) {
//                ctx.json(item);


                JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions()
                                .setAlgorithm("HS256")
                                .setBuffer("keyboard cat")));

                String token = provider.generateToken(new JsonObject());

                ctx.json(item+" token is "+ token);


            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });


        router.delete("/api/items/:id").handler(ctx -> {
            String itemId = ctx.pathParam("id");
            Item item = database.find(Item.class, Long.parseLong(itemId));
            if (item != null) {
                database.delete(item);
                ctx.response().setStatusCode(204).end("Item deleted");
            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });


        router.put("/api/items/:id").handler(ctx -> {
            String itemId = ctx.pathParam("id");
            Item existingItem = database.find(Item.class, Long.parseLong(itemId));
            if (existingItem != null) {
                JsonObject requestBody = ctx.getBodyAsJson();
                String newName = requestBody.getString("name");
                existingItem.setName(newName);
                database.update(existingItem);
                ctx.json(existingItem);
            } else {
                ctx.response().setStatusCode(404).end("Item not found");
            }
        });





        // Start the HTTP server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onSuccess(server -> {
                    System.out.println("HTTP server started on port 8080");
                    startPromise.complete();
                })
                .onFailure(err -> {
                    System.err.println("Failed to start HTTP server: " + err.getMessage());
                    startPromise.fail(err);
                });
    }

    private DataSourceConfig createDataSourceConfig() {
        DataSourceConfig dsConfig = new DataSourceConfig();
        dsConfig.setUsername("root");
        dsConfig.setPassword("root");
        dsConfig.setUrl("jdbc:mysql://localhost:3306/testdb");
        dsConfig.setDriver("com.mysql.cj.jdbc.Driver");
        return dsConfig;
    }

    @Entity
    public static class Item {
        @Id
        private Long id;
        private String name;

        public Item(String name) {
            this.name = name;
        }

        public Item() {

        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
