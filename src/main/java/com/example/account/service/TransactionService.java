package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.TransactionType;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.dto.TransactionType.*;
import static com.example.account.type.TransactionResultType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    /**
     * 정책 : 사용자 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
     * 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
     * 거래금액이 너무 작거나 큰 경우 실패 응답
     */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);  // 여기서 예외가 발생한다면? 컨트롤러로 보내

        account.useBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(USE, S, account, amount));
    }

    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERD) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseBalance(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, F, account, amount);
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(amount, transaction, account);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, S, account, amount));
    }

    private static void validateCancelBalance(Long amount, Transaction transaction, Account account) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }

        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResultType resultType,
            Account account,
            Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(resultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public void saveFailedCancelBalance(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, F, account, amount);
    }


    public TransactionDto queryTransaction(String transactionId) {

        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
