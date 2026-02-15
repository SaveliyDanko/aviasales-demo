package com.savadanko.aviasales.flight.repository;

import com.savadanko.aviasales.flight.FlightOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightOfferRepository extends JpaRepository<FlightOffer, String>, JpaSpecificationExecutor<FlightOffer> {
}
