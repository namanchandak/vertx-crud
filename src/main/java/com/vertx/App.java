package com.vertx;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
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

        dbconfig dbConfig = new dbconfig();
        DataSourceConfig dsConfig = dbConfig.createDataSourceConfig();

        // Configure Ebean programmatically
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(dbConfig.createDataSourceConfig());
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
            String name = requestBody.getString("name");
            String pass = requestBody.getString("pass");

            // Create a new Item object and save it to the database
            Item newItem = new Item();
            newItem.setName(name);
            newItem.setPass(pass);

            // Save the item to the database using Ebean
            try {
                database.save(newItem);
                ctx.response().setStatusCode(201).end("Item saved successfully");
            } catch (Exception e) {
                ctx.response().setStatusCode(500).end("Failed to save item: " + e.getMessage());
                e.printStackTrace();  // Log the exception for debugging purposes
            }
        });


        router.get("/api/items/:id").handler(ctx -> {
            String itemId = ctx.pathParam("id");
            Item item = database.find(Item.class, Long.parseLong(itemId));
            if (item != null) {
//                ctx.json(item);



                ctx.json(item);


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


        //login

        router.get("/api/login").handler(ctx -> {
            JsonObject requestBody = ctx.getBodyAsJson();
            String id = requestBody.getString("id");
            String pass = requestBody.getString("pass");

            Item item = database.find(Item.class, Long.parseLong(id));
            System.out.println(item.getName()+ " " + pass);


            if (item != null  && item.getPass() != null && item.getPass().equals(pass)) {

                JWTAuthOptions jwtAuthOptions = new JWTAuthOptions()
                        .addPubSecKey(new PubSecKeyOptions()
                                .setAlgorithm("HS256")
                                .setBuffer("keyboard cat"));
                JWTAuth provider = JWTAuth.create(vertx, jwtAuthOptions);

                JsonObject claims = new JsonObject().put("sub", id);
                JWTOptions options = new JWTOptions().setExpiresInMinutes(30);

                String token = provider.generateToken(claims, options);
                ctx.response().setStatusCode(200).end(token);
            } else {
                ctx.response().setStatusCode(401).end("Invalid credentials");
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




}
