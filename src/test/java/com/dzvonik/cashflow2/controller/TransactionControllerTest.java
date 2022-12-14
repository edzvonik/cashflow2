package com.dzvonik.cashflow2.controller;

import com.dzvonik.cashflow2.exception.ResourceNotFoundException;
import com.dzvonik.cashflow2.model.TransactionType;
import com.dzvonik.cashflow2.model.dto.TransactionDto;
import com.dzvonik.cashflow2.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean(name = "defaultTransactionService")
    private TransactionService transactionService;

    @Test
    void create_WhenCreate_ThenReturnLocationAndCreatedStatus() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .amount(new BigDecimal("1032.52"))
                .type(TransactionType.INCOME)
                .date(LocalDate.of(2022, 10, 25))
                .comment("Test dto")
                .accountId(5L)
                .categoryId(3L)
                .build();

        when(transactionService.create(any(TransactionDto.class))).thenReturn(1L);

        mockMvc.perform(post("/transactions/new")
                .contentType("application/json")
                .content(mapper.writeValueAsString(dto)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.path").value("/transactions/1")
                );
    }

    @Test
    void create_WhenInvalidIdentifier_ThenResourceNotFoundException() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .amount(new BigDecimal("1032.52"))
                .type(TransactionType.INCOME)
                .date(LocalDate.of(2022, 10, 25))
                .comment("Test dto")
                .accountId(5L)
                .categoryId(3L)
                .build();

        when(transactionService.create(any(TransactionDto.class))).thenThrow(new ResourceNotFoundException("Account with id=5 not found"));

        mockMvc.perform(post("/transactions/new")
                .contentType("application/json")
                .content(mapper.writeValueAsString(dto)))
                .andExpectAll(
                        status().isNotFound(),
                        jsonPath("$.message").value("Account with id=5 not found"),
                        jsonPath("$.uri").value("/transactions/new")
                );
    }

    @Test
    void create_WhenFieldsEmpty_ThenCorrectResponse() throws Exception {
        TransactionDto dto = TransactionDto.builder().build();

        mockMvc.perform(post("/transactions/new")
                .contentType("application/json")
                .content(mapper.writeValueAsString(dto)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Required fields must be filled"),
                        jsonPath("$.errors.amount").value("Amount may not be null"),
                        jsonPath("$.errors.date").value("Date may not be null"),
                        jsonPath("$.errors.type").value("Type may not be null"),
                        jsonPath("$.errors.accountId").value("Account may not be null"),
                        jsonPath("$.errors.categoryId").value("Category may not be null")
                );
    }

    @Test
    void getById_WhenCall_ThenReturnTransaction() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .id(22L)
                .amount(new BigDecimal("55512125.51"))
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2022, 11, 2))
                .comment("Get transaction test")
                .accountId(5L)
                .categoryId(3L)
                .build();

        when(transactionService.getById(22L, 5L)).thenReturn(dto);

        mockMvc.perform(get("/transactions/22?accountId=5")
                .contentType("application/json"))
                .andExpectAll(
                        status().isOk(),
                        header().string("Cache-control", "no-store, no-cache, must-revalidate"),
                        jsonPath("$.transaction.id").value(22),
                        jsonPath("$.transaction.amount").value(55512125.51),
                        jsonPath("$.transaction.type").value("EXPENSE"),
                        jsonPath("$.transaction.date").value("2022-11-02"),
                        jsonPath("$.transaction.comment").value("Get transaction test"),
                        jsonPath("$.transaction.accountId").value(5),
                        jsonPath("$.transaction.categoryId").value(3)
                );
    }

    @Test
    void getAll_WhenCall_ThenReturnPageWithDto() throws Exception {
        TransactionDto dto = TransactionDto.builder()
                .id(22L)
                .amount(new BigDecimal("55512125.51"))
                .type(TransactionType.EXPENSE)
                .date(LocalDate.of(2022, 11, 2))
                .comment("Get transaction test")
                .accountId(5L)
                .categoryId(3L)
                .build();
        String jsonDto = mapper.writeValueAsString(dto);
        List<TransactionDto> content = new ArrayList<>(List.of(dto));
        Pageable pageable = PageRequest.of(0, 3);
        Page<TransactionDto> pageDto = new PageImpl<>(content, pageable, 1);


        when(transactionService.getAll(any())).thenReturn(pageDto);

        MvcResult result = mockMvc.perform(get("/transactions?page=0&size=3")
                .contentType("application/json"))
                .andExpectAll(
                        status().isOk(),
                        header().string("Cache-control", "no-store, no-cache, must-revalidate")
                )
                .andReturn();
        String resultJson = result.getResponse().getContentAsString();
        assertThat(resultJson).contains(
                "\"totalPages\":1",
                "\"totalElements\":1",
                jsonDto
        );
    }

    @Test
    void deleteById_WhenExist_ThenReturnNoContentStatus() throws Exception {
        when(transactionService.deleteById(any(), any(), any())).thenReturn(true);

        mockMvc.perform(delete("/transactions/1?accountId=1&categoryId=2")
                .contentType("application/json"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_WhenBadRequest_ThenReturnBadRequestStatus() throws Exception {
        when(transactionService.deleteById(any(), any(), any())).thenReturn(false);

        mockMvc.perform(delete("/transactions/1")
                .contentType("application/json"))
                .andExpect(status().isBadRequest());
    }

}