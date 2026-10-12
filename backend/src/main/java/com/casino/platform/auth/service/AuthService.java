package com.casino.platform.auth.service;

import com.casino.platform.auth.dto.RegisterRequest;
import com.casino.platform.user.entity.User;
import com.casino.platform.user.entity.UserRole;
import com.casino.platform.user.repository.UserRepository;
import com.casino.platform.wallet.entity.CurrencyType;
import com.casino.platform.wallet.entity.Wallet;
import com.casino.platform.wallet.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                hashedPassword
        );
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        // Create DEMO wallet for the new user
        Wallet wallet = new Wallet(savedUser, CurrencyType.DEMO);
        walletRepository.save(wallet);

        return savedUser;
    }
}
