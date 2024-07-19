package com.vertx.service.impl;

import com.vertx.model.Post;
import com.vertx.service.PostService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.core.json.Json;

import java.util.List;
import java.util.stream.Collectors;

public class PostServiceImpl implements PostService {

    private final WebClient webClient;

    public PostServiceImpl(Vertx vertx) {
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost("jsonplaceholder.typicode.com")
                .setDefaultPort(443)  // HTTPS port
                .setSsl(true);       // Enable SSL for HTTPS
        this.webClient = WebClient.create(vertx, options);
    }

    @Override
    public Future<List<Post>> fetchPosts() {
        return webClient.get("/posts").send().map(response -> {
            if (response.statusCode() == 200) {
                return response.bodyAsJsonArray().stream()
                        .map(json -> Json.decodeValue(json.toString(), Post.class))
                        .collect(Collectors.toList());
            } else {
                throw new RuntimeException("Failed to fetch posts, status code: " + response.statusCode());
            }
        });
    }
}
