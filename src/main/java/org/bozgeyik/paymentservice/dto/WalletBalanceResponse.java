package org.bozgeyik.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bozgeyik.paymentservice.model.AccountStatus;

import java.util.Set;

@Data
@AllArgsConstructor
public class WalletBalanceResponse {
    private Long walletId; // Yeni eklenen alan
    private String customerId;
    private String ownerName;
    private AccountStatus status;
    private Set<WalletBalanceDto> balances;
}