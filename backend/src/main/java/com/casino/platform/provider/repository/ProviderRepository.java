package com.casino.platform.provider.repository;

import com.casino.platform.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByCode(String code);

    Optional<Provider> findByCodeAndIsActiveTrue(String code);

    Optional<Provider> findByKeyId(String keyId);

    boolean existsByCode(String code);
}
