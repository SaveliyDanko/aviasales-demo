package com.savadanko.aviasales.booking.validation;

import com.savadanko.aviasales.booking.dto.AncillariesRequest;
import com.savadanko.aviasales.booking.dto.BaggageRequest;
import com.savadanko.aviasales.booking.dto.CreateBookingRequest;
import com.savadanko.aviasales.booking.dto.InsuranceRequest;
import com.savadanko.aviasales.booking.dto.PassengerRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BookingRequestValidator implements ConstraintValidator<ValidBookingRequest, CreateBookingRequest> {

    @Override
    public boolean isValid(CreateBookingRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        List<PassengerRequest> passengers = value.getPassengers();
        if (passengers != null) {
            Set<Integer> passengerIds = new HashSet<>();
            for (PassengerRequest passenger : passengers) {
                if (passenger != null && passenger.getId() != null && !passengerIds.add(passenger.getId())) {
                    addViolation(context, "passengers", "passenger id must be unique within request");
                    valid = false;
                    break;
                }
            }
        }

        AncillariesRequest ancillaries = value.getAncillaries();
        if (ancillaries != null) {
            List<BaggageRequest> baggageItems = ancillaries.getBaggage();
            if (baggageItems != null && passengers != null) {
                Set<Integer> existingPassengerIds = passengers.stream()
                        .filter(p -> p != null && p.getId() != null)
                        .map(PassengerRequest::getId)
                        .collect(Collectors.toSet());

                for (BaggageRequest baggage : baggageItems) {
                    if (baggage == null) {
                        continue;
                    }
                    if (baggage.getPassengerId() != null && !existingPassengerIds.contains(baggage.getPassengerId())) {
                        addViolation(context, "ancillaries.baggage", "baggage passengerId must reference existing passenger");
                        valid = false;
                    }
                    if (baggage.getCount() != null && baggage.getCount() > 0) {
                        if (baggage.getWeight() == null || baggage.getWeight() <= 0) {
                            addViolation(context, "ancillaries.baggage", "weight must be > 0 when baggage count > 0");
                            valid = false;
                        }
                    }
                }
            }

            InsuranceRequest insurance = ancillaries.getInsurance();
            if (insurance != null && Boolean.TRUE.equals(insurance.getAccepted()) && insurance.getType() == null) {
                addViolation(context, "ancillaries.insurance.type", "insurance type is required when accepted is true");
                valid = false;
            }
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
