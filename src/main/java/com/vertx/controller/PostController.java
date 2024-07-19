package com.vertx.controller;

import com.vertx.model.Post;
import com.vertx.service.PostService;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    public void setupRoutes(Router router) {
        router.get("/fetch").handler(this::fetchPosts);
    }

    private void fetchPosts(RoutingContext routingContext) {
        System.out.println("Fetching posts...");
        postService.fetchPosts().onComplete(ar -> {
            if (ar.succeeded()) {
                List<Post> posts = ar.result();
                System.out.println("Fetched posts: " + posts);
                routingContext.response()
                        .putHeader("content-type", "application/json")
                        .end(Json.encodePrettily(posts));
            } else {
                System.err.println("Failed to fetch posts: " + ar.cause().getMessage());
                routingContext.response()
                        .setStatusCode(500)
                        .end("Failed to fetch posts: " + ar.cause().getMessage());
            }
        });
    }
}
