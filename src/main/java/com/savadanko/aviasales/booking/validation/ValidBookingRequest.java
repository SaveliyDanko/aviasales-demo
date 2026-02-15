package com.savadanko.aviasales.booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingRequestValidator.class)
public @interface ValidBookingRequest {
    String message() default "Invalid booking request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

