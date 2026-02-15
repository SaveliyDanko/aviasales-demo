package com.savadanko.aviasales.booking.dto;

import com.savadanko.aviasales.booking.entity.InsuranceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceRequest {
    private InsuranceType type;
    private Boolean accepted;
}

