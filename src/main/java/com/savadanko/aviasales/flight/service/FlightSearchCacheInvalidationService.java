package com.savadanko.aviasales.flight.service;

import com.savadanko.aviasales.mongo.repository.FlightSearchCacheRepository;
import com.savadanko.aviasales.mongo.service.MongoFailoverGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlightSearchCacheInvalidationService {

    private final FlightSearchCacheRepository cacheRepository;
    private final MongoFailoverGuard mongoFailoverGuard;

    public void invalidateByOfferId(String offerId) {
        if (offerId == null || offerId.isBlank()) {
            return;
        }

        if (!mongoFailoverGuard.canUseMongo()) {
            return;
        }

        try {
            long deleted = cacheRepository.deleteByOfferId(offerId);
            log.info("Flight search cache invalidated by offerId={}, deletedEntries={}", offerId, deleted);
        } catch (Exception e) {
            mongoFailoverGuard.recordFailure("cache invalidation", e);
        }
    }
}
