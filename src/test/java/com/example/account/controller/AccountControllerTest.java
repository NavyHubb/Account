package com.example.account.controller;

import com.example.account.domain.Account;
import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.exception.AccountException;
import com.example.account.service.AccountService;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;

//    @MockBean
//    private RedisTestService redisTestService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("계좌 생성 성공")
    void successCreateAccount() throws Exception {
        //given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("1234567890")
                                .balance(10000L)
                                .registeredAt(LocalDateTime.now())
                                .unRegisteredAt(LocalDateTime.now())
                                .build());
        //when
        //then
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAccount.Request(1L, 100L)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value(1234567890))
                .andDo(print());
    }

    @Test
    @DisplayName("계좌 삭제 성공")
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("1234567890")
                                .balance(10000L)
                                .registeredAt(LocalDateTime.now())
                                .unRegisteredAt(LocalDateTime.now())
                                .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(333L, "1111111111")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andDo(print());
    }
    @Test
    @DisplayName("계좌 조회 성공 - 유저 아이디로 조회")
    void successGetAccountsByUserId() throws Exception {
        //given
        List<AccountDto> accountDtos = Arrays.asList(
                AccountDto.builder()
                        .accountNumber("1234567890")
                        .balance(1000L)
                        .build(),
                AccountDto.builder()
                        .accountNumber("2345678901")
                        .balance(2000L)
                        .build(),
                AccountDto.builder()
                        .accountNumber("3456789012")
                        .balance(3000L)
                        .build()
        );

        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);

        //when
        //then
        mockMvc.perform(get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                .andExpect(jsonPath("$[0].balance").value(1000))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("계좌 조회 성공")
    void successGetAccount() throws Exception {
        //given
        given(accountService.getAccount(anyLong()))
                .willReturn(Account.builder()
                        .accountNumber("1234")
                        .accountStatus(AccountStatus.IN_USE)
                        .build());

        //when
        //then
        mockMvc.perform(get("/account/785"))
                .andDo(print())
                .andExpect(jsonPath("$.accountNumber").value("1234"))
                .andExpect(jsonPath("$.accountStatus").value("IN_USE"));  // enum타입을 String으로 써야함에 유의
    }

    @Test
    @DisplayName("계좌 조회 실패 - 해당 계좌 없음")
    void failGetAccount() throws Exception {
        //given
        given(accountService.getAccount(anyLong()))
                .willThrow(new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        //when
        //then
        mockMvc.perform(get("/account/785"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."));
    }
}
