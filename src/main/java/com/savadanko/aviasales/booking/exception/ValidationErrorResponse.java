package com.savadanko.aviasales.booking.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private String field;
    private String message;
}

