package org.bozgeyik.paymentservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.bozgeyik.paymentservice.dto.*;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.model.User;
import org.bozgeyik.paymentservice.model.Wallet;
import org.bozgeyik.paymentservice.service.PaymentOrchestratorService;
import org.bozgeyik.paymentservice.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrchestratorService paymentOrchestratorService;
    private final WalletService walletService;

    @PostMapping("/wallets")
    public ResponseEntity<WalletBalanceResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapWalletToResponse(wallet));
    }

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getWalletById(@PathVariable Long walletId) {
        Wallet wallet = walletService.getWalletById(walletId);
        return ResponseEntity.ok(mapWalletToResponse(wallet));
    }

    // YENİ EKLENEN: Kullanıcının tüm cüzdanlarını getir
    @GetMapping("/wallets")
    public ResponseEntity<List<WalletBalanceResponse>> getWalletsByUser(@RequestParam String customerId) {
        List<Wallet> wallets = walletService.getWalletsByUserEmail(customerId);
        List<WalletBalanceResponse> response = wallets.stream()
                .map(this::mapWalletToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/wallets/{walletId}")
    public ResponseEntity<Void> closeWallet(@PathVariable Long walletId) {
        walletService.closeWallet(walletId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/payments/deposit")
    public ResponseEntity<Transaction> depositOnRamp(@Valid @RequestBody DepositRequest request) {
        Transaction transaction = paymentOrchestratorService.depositOnRamp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @PostMapping("/payments/withdraw")
    public ResponseEntity<Transaction> withdrawOffRamp(@Valid @RequestBody WithdrawRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.withdrawOffRamp(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/payments/transfer")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.transferMoney(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/payments/fx")
    public ResponseEntity<Transaction> executeFxTrade(@Valid @RequestBody FxRequest request)
            throws InsufficientFundsException {
        Transaction transaction = paymentOrchestratorService.executeFx(request);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/payments/transactions")
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestParam @NotNull String customerId,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {
        Page<Transaction> transactions = paymentOrchestratorService.getTransactionsForUser(customerId, pageable);
        return ResponseEntity.ok(transactions);
    }

    private WalletBalanceResponse mapWalletToResponse(@NotNull Wallet wallet) {
        User user = wallet.getUser();
        return new WalletBalanceResponse(
                wallet.getId(),
                user.getId().toString(),
                user.getName(),
                wallet.getStatus(),
                wallet.getBalances().stream()
                        .map(b -> new WalletBalanceDto(b.getCurrency(), b.getBalance()))
                        .collect(Collectors.toSet())
        );
    }

    @PostMapping("/payments/external-transfer")
    public ResponseEntity<Transaction> externalTransfer(@Valid @RequestBody ExternalTransferRequest request) {
        Transaction transaction = paymentOrchestratorService.externalTransfer(request);
        return ResponseEntity.ok(transaction);
    }
}