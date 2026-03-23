package com.personalproject.user_service.repository;

import com.personalproject.user_service.models.Account;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AccountRepo extends CrudRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT rt FROM Account rt WHERE rt.userId = ?1 AND rt.enabled = true AND rt.deleted = false")
    Optional<Account> findByUserId(Long userId);

    @Query("SELECT rt FROM Account rt WHERE rt.email = ?1 AND rt.enabled = true AND rt.deleted = false")
    Optional<Account> findByEmail(String email);
}
