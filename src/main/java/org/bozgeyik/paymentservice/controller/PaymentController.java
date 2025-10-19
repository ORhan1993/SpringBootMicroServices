package org.bozgeyik.paymentservice.controller;

import jakarta.validation.Valid; // Bean Validation için
import org.bozgeyik.paymentservice.dto.CreateAccountRequest;
import org.bozgeyik.paymentservice.dto.DepositRequest; // Deposit için DTO
import org.bozgeyik.paymentservice.dto.WithdrawRequest; // Withdraw için DTO
import org.bozgeyik.paymentservice.exception.CustomAccountNotFoundException;
import org.bozgeyik.paymentservice.exception.InsufficientFundsException;
import org.bozgeyik.paymentservice.model.Account;
import org.bozgeyik.paymentservice.model.Transaction;
import org.bozgeyik.paymentservice.service.AccountService;
import org.bozgeyik.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        // @Valid anotasyonu ile CreateAccountRequest DTO'su üzerindeki validasyon kuralları
        // (örneğin @NotBlank, @NotNull, @Size) otomatik olarak çalıştırılır.
        // pom.xml'de spring-boot-starter-validation bağımlılığı olmalıdır.
        Account createdAccount = accountService.createAccount(
                request.getCustomerId(),
                request.getAccountNumber(),
                request.getOwnerName(),
                request.getCurrency(),
                request.getInitialBalance()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) throws CustomAccountNotFoundException {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(@Valid @RequestBody DepositRequest request) throws CustomAccountNotFoundException {
        // TransactionService.deposit metodu artık currency ve description da alıyor.
        // Bu parametreleri DepositRequest DTO'sundan alıyoruz.
        return ResponseEntity.ok(transactionService.deposit(
                request.getAccountNumber(),
                request.getAmount(),
                request.getCurrency(), // DTO'dan gelen para birimi
                request.getDescription() // DTO'dan gelen açıklama
        ));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) throws CustomAccountNotFoundException, InsufficientFundsException {
        // TransactionService.withdraw metodu artık currency ve description da alıyor.
        // Bu parametreleri WithdrawRequest DTO'sundan alıyoruz.
        return ResponseEntity.ok(transactionService.withdraw(
                request.getAccountNumber(),
                request.getAmount(),
                request.getCurrency(), // DTO'dan gelen para birimi
                request.getDescription() // DTO'dan gelen açıklama
        ));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            // Transfer işlemi için de bir DTO (örn: TransferRequest) kullanmak daha iyi bir pratik olabilir.
            // Ancak mevcut @RequestParam yapısı da TransactionService ile uyumlu.
            @RequestParam String fromAccount,
            @RequestParam String toAccount,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) throws CustomAccountNotFoundException, InsufficientFundsException {
        return ResponseEntity.ok(transactionService.transferMoney(fromAccount, toAccount, amount, description));
    }

    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String accountNumber) throws CustomAccountNotFoundException {
        return ResponseEntity.ok(transactionService.getTransactions(accountNumber));
    }
}