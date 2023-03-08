package com.example.account.dto;

import com.example.account.domain.Transaction;
import com.example.account.type.TransactionResultType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private String accountNumber;
    private TransactionType transactionType;
    private TransactionResultType transactionResultType;
    private String transactionId;
    private Long amount;
    private Long balanceSnapshot;
    private LocalDateTime transactedAt;

    public static TransactionDto fromEntity(Transaction transaction) {
        return TransactionDto.builder()
                .accountNumber(transaction.getAccount().getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .transactionResultType(transaction.getTransactionResultType())
                .transactionId(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .balanceSnapshot(transaction.getBalanceSnapshot())
                .transactedAt(transaction.getTransactedAt())
                .build();
    }
}
