package com.savadanko.aviasales.mongo.repository;

import com.savadanko.aviasales.mongo.document.FlightSearchCacheDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface FlightSearchCacheRepository extends MongoRepository<FlightSearchCacheDocument, String> {
    Optional<FlightSearchCacheDocument> findByCacheKey(String cacheKey);

    @Query(value = "{ 'offerIds': ?0 }", delete = true)
    long deleteByOfferId(String offerId);
}
