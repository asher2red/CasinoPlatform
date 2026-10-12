package com.casino.platform.wallet.controller;

import com.casino.platform.ledger.dto.LedgerResponse;
import com.casino.platform.ledger.entity.Ledger;
import com.casino.platform.ledger.repository.LedgerRepository;
import com.casino.platform.wallet.dto.DepositRequest;
import com.casino.platform.wallet.dto.WalletResponse;
import com.casino.platform.wallet.entity.Wallet;
import com.casino.platform.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final LedgerRepository ledgerRepository;

    public WalletController(WalletService walletService, LedgerRepository ledgerRepository) {
        this.walletService = walletService;
        this.ledgerRepository = ledgerRepository;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getMyWallet() {
        String username = getCurrentUsername();
        Wallet wallet = walletService.getWalletByUsername(username);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<LedgerResponse>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String username = getCurrentUsername();
        Wallet wallet = walletService.getWalletByUsername(username);

        Pageable pageable = PageRequest.of(page, size);
        Page<Ledger> ledgerPage = ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);

        return ResponseEntity.ok(ledgerPage.map(LedgerResponse::from));
    }

    @PostMapping("/dev/deposit")
    public ResponseEntity<?> devDeposit(@Valid @RequestBody DepositRequest request) {
        try {
            String username = getCurrentUsername();
            Wallet wallet = walletService.getWalletByUsername(username);

            String referenceId = "DEV-DEPOSIT-" + UUID.randomUUID().toString();
            String description = request.getDescription() != null
                    ? request.getDescription()
                    : "Development virtual money grant";

            Ledger ledger = walletService.deposit(
                    wallet.getId(),
                    request.getAmount(),
                    description,
                    referenceId
            );

            return ResponseEntity.ok(LedgerResponse.from(ledger));
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process deposit");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        return authentication.getName();
    }
}
