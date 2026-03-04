package com.savadanko.aviasales.mongo.projection;

public interface TopRouteProjection {
    String getOrigin();
    String getDestination();
    Long getSearches();
}
