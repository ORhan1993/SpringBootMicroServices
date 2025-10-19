package org.bozgeyik.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.dto.*;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.service.PaymentOrchestratorService;
import org.bozgeyik.paymentservice.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrchestratorService paymentOrchestratorService;
    private final WalletService walletService;

    // --- Cüzdan (Wallet) Yönetimi ---

    @PostMapping("/wallets")
    public ResponseEntity<WalletBalanceResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapWalletToResponse(wallet));
    }

    @GetMapping("/wallets/balances/{customerId}")
    public ResponseEntity<WalletBalanceResponse> getWalletBalances(@PathVariable String customerId) {
        Wallet wallet = walletService.getWalletByCustomerId(customerId);
        return ResponseEntity.ok(mapWalletToResponse(wallet));
    }

    @DeleteMapping("/wallets/{customerId}")
    public ResponseEntity<Void> closeWallet(@PathVariable String customerId) {
        walletService.closeWallet(customerId);
        return ResponseEntity.noContent().build();
    }

    // --- Parasal İşlemler ---

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> depositOnRamp(@Valid @RequestBody DepositRequest request) {
        Transaction transaction = paymentOrchestratorService.depositOnRamp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdrawOffRamp(@Valid @RequestBody WithdrawRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.withdrawOffRamp(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.transferMoney(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/fx/execute")
    public ResponseEntity<Transaction> executeFxTrade(@Valid @RequestBody FxRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.executeFx(request);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/transactions/{customerId}")
    public ResponseEntity<Page<Transaction>> getTransactions(
            @PathVariable String customerId,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {
        Page<Transaction> transactions = paymentOrchestratorService.getTransactionsForCustomer(customerId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // --- Helper Metot ---
    private WalletBalanceResponse mapWalletToResponse(Wallet wallet) {
        return new WalletBalanceResponse(
                wallet.getCustomerId(),
                wallet.getOwnerName(),
                wallet.getStatus(),
                wallet.getBalances().stream()
                        .map(b -> new WalletBalanceDto(b.getCurrency(), b.getBalance()))
                        .collect(Collectors.toSet())
        );
    }
}