package com.example.account.controller;

import com.example.account.dto.*;
import com.example.account.service.AccountService;
import com.example.account.service.TransactionService;
import com.example.account.type.TransactionResultType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.example.account.dto.TransactionType.*;
import static com.example.account.type.TransactionResultType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void useBalance_Success() throws Exception {
        //given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .amount(12345L)
                        .transactedAt(LocalDateTime.now())
                        .build()
                );
        //when
        //then
        mockMvc.perform(post("/transaction/use")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UseBalance.Request(1L, "2000000000", 3000L)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResultType").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345))
                .andDo(print());

    }

    @Test
    void cancelBalance_Success() throws Exception {
        //given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactionResultType(S)
                        .transactionId("transactionIdForCancel")
                        .amount(12345L)
                        .transactedAt(LocalDateTime.now())
                        .build()
                );
        //when
        //then
        mockMvc.perform(post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request("transactionId", "2000000000", 3000L)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value(12345))
                .andDo(print());

    }

    @Test
    void queryTransaction_success() throws Exception {
        //given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1000000000")
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .amount(12345L)
                        .transactedAt(LocalDateTime.now())
                        .build());

        //when
        //then
        mockMvc.perform(get("/transaction/12345"))  // transactionId로 12345든 뭐든 given에서 설정한대로 나올것
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1000000000"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value(12345))
                .andDo(print());
    }


}