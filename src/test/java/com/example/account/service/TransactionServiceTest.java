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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.dto.TransactionType.*;
import static com.example.account.type.AccountStatus.*;
import static com.example.account.type.TransactionResultType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void useBalance_Success() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(10000L)
                                .accountNumber("1000000012")
                                .build()));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(S)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        //when
        TransactionDto transactionDto = transactionService.useBalance(
                        1L, "1000000000", 200L);  // 숫자는 의미 없음

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void userBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void useBalance_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Green")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 100L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌 소유주 다름")
    void useBalance_userUnMatch() {
        //given
        AccountUser Green = AccountUser.builder()
                .id(12L)
                .name("Green")
                .build();
        AccountUser Red = AccountUser.builder()
                .id(13L)
                .name("Red")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(Green));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(Red)  // 위에서 찾은 유저는 Green인데 계좌 소유주는 Red
                                .accountNumber("1000000012")
                                .balance(0L)
                                .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 해지된 계좌")
    void useBalance_accountAlreadyUnregistered() {
        //given
        AccountUser user = AccountUser.builder()
                .id(12L)
                .name("Green")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .accountStatus(AccountStatus.UNREGISTERD)
                                .build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 잔액 부족")
    void useBalance_amountExcceedBalance() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(100L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(
                        1L, "1234567890", 500L));
        verify(transactionRepository, times(0)).save(any());
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction_Success() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(
                        Account.builder()
                                .accountUser(user)
                                .balance(10000L)
                                .accountNumber("1000000012")
                                .build()));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(S)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseBalance(
                "1000000000", USE_AMOUNT);  // 숫자는 의미 없음

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());  // 잔액을 사용하지 못했으므로 account 초기값과 같음
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(USE, captor.getValue().getTransactionType());
    }

    @Test
    @DisplayName("거래 취소 성공")
    void cancelTransaction_Success() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .account(account)
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", CANCEL_AMOUNT);  // 숫자는 의미 없음

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("거래 취소 실패 - 해당 계좌 없음")
    void cancelTransaction_AccountNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 100L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소 실패 - 해당 거래 없음")
    void cancelTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", 100L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소 실패 - 거래와 계좌 불일치")
    void cancelTransaction_TransactionAccountUnMatch() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013")
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소 실패 - 거래 금액과 취소 금액이 다름")
    void cancelTransaction_cancelMustFully() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(CANCEL_AMOUNT - 100)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 취소 실패 - 거래 취소로부터 1년 이상이 경과함")
    void cancelTransaction_tooOldTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(2))
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 조회 성공")
    void queryTransaction_success() {
        //given
        AccountUser user = AccountUser.builder()
                .id(13L)
                .name("Green")
                .build();

        Account account = Account.builder()
                .id(1L)
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(USE)
                .transactionResultType(S)
                .account(account)
                .amount(USE_AMOUNT)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(2))
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals("1000000012", transactionDto.getAccountNumber());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals("transactionId", transactionDto.getTransactionId());
        assertEquals(USE_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("거래 조회 실패 - 해당 거래 없음")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

}