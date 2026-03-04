package com.savadanko.aviasales.mongo.repository;

import com.savadanko.aviasales.mongo.document.FlightSearchEventDocument;
import com.savadanko.aviasales.mongo.projection.TopAirlineProjection;
import com.savadanko.aviasales.mongo.projection.TopRouteProjection;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface FlightSearchEventRepository extends MongoRepository<FlightSearchEventDocument, String> {

    @Aggregation(pipeline = {
            "{ '$match': { 'searchedAt': { '$gte': ?0 }, 'origin': { '$ne': null }, 'destination': { '$ne': null } } }",
            "{ '$group': { '_id': { 'origin': '$origin', 'destination': '$destination' }, 'searches': { '$sum': 1 } } }",
            "{ '$sort': { 'searches': -1 } }",
            "{ '$limit': ?1 }",
            "{ '$project': { '_id': 0, 'origin': '$_id.origin', 'destination': '$_id.destination', 'searches': 1 } }"
    })
    List<TopRouteProjection> findTopRoutes(Instant from, int limit);

    @Aggregation(pipeline = {
            "{ '$match': { 'searchedAt': { '$gte': ?0 }, 'airline': { '$nin': [null, ''] } } }",
            "{ '$group': { '_id': '$airline', 'searches': { '$sum': 1 } } }",
            "{ '$sort': { 'searches': -1 } }",
            "{ '$limit': ?1 }",
            "{ '$project': { '_id': 0, 'airline': '$_id', 'searches': 1 } }"
    })
    List<TopAirlineProjection> findTopAirlines(Instant from, int limit);

    long countBySearchedAtGreaterThanEqual(Instant from);

    long countBySearchedAtGreaterThanEqualAndCacheHitTrue(Instant from);
}
