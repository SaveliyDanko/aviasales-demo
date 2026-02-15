package com.savadanko.aviasales.payment.validation;

import com.savadanko.aviasales.payment.dto.PaymentDataRequest;
import com.savadanko.aviasales.payment.dto.PaymentProcessRequest;
import com.savadanko.aviasales.payment.entity.PaymentMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PaymentProcessRequestValidator implements ConstraintValidator<ValidPaymentProcessRequest, PaymentProcessRequest> {

    @Override
    public boolean isValid(PaymentProcessRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getPayment() == null) {
            return true;
        }

        boolean valid = true;
        PaymentDataRequest payment = value.getPayment();

        context.disableDefaultConstraintViolation();
        if (Boolean.TRUE.equals(payment.getSaveCard()) && payment.getPaymentMethod() != PaymentMethod.BANK_CARD) {
            context.buildConstraintViolationWithTemplate("saveCard is allowed only for BANK_CARD")
                    .addPropertyNode("payment")
                    .addPropertyNode("saveCard")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
