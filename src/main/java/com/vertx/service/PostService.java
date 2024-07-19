package com.vertx.service;

import com.vertx.model.Post;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.client.WebClient;

import java.util.List;

public interface PostService {

    Future<List<Post>> fetchPosts();
}
