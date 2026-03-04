package com.savadanko.aviasales.flight.repository;

import com.savadanko.aviasales.flight.FlightOffer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightOfferRepository extends JpaRepository<FlightOffer, String>, JpaSpecificationExecutor<FlightOffer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FlightOffer f where f.offerId = :offerId")
    Optional<FlightOffer> findByOfferIdForUpdate(@Param("offerId") String offerId);
}
