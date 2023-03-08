package com.example.account.repository;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Account 엔티티를 DB에 저장하기 위해 필요한 repository.
    // JpaRepository<해당 레포가 활용할 엔티티, 그 엔티티의 PK 자료형>

    Optional<Account> findFirstByOrderByIdDesc();
    Integer countByAccountUser(AccountUser accountUser);


    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountUser(AccountUser accountUser);
}
