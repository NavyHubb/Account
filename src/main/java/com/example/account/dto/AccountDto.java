package com.example.account.dto;

import com.example.account.domain.Account;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private Long userId;
    private String accountNumber;
    private Long balance;
    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    // AccountDto 생성자를 통해 할 수도 있지만 이 방식이 더 가독성도 좋고 안전함
    public static AccountDto fromEntity(Account account) {
        return AccountDto.builder()
                .userId(account.getAccountUser().getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .registeredAt(account.getRegisteredAt())
                .unRegisteredAt(account.getUnRegisteredAt())
                .build();
    }
}
