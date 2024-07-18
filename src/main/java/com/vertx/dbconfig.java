package com.vertx;

import io.ebean.datasource.DataSourceConfig;

public class dbconfig {

    public DataSourceConfig createDataSourceConfig() {
        DataSourceConfig dsConfig = new DataSourceConfig();
        dsConfig.setUsername("root");
        dsConfig.setPassword("root");
        dsConfig.setUrl("jdbc:mysql://localhost:3306/testdb");
        dsConfig.setDriver("com.mysql.cj.jdbc.Driver");
        return dsConfig;
    }

}
