package com.vertx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Post {

    private int userId;
    private int id ;
    private String title;
    private String body ;
}
