package org.bozgeyik.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WalletBalanceDto {
    private String currency;
    private BigDecimal balance;
}
