package com.sjdscxz.ecommerce;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
class EcommerceApplicationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void productListSeededWithDataSql() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        mvc.perform(get("/api/products/99999")).andExpect(status().isNotFound());
    }

    @Test
    void checkoutHappyPath() throws Exception {
        var req = new OrderController.CheckoutRequest(
                "buyer@example.com",
                List.of(new OrderController.CheckoutItem(1L, 2),
                        new OrderController.CheckoutItem(2L, 1)));
        mvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerEmail").value("buyer@example.com"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void checkoutInsufficientStockReturns409() throws Exception {
        var req = new OrderController.CheckoutRequest(
                "greedy@example.com",
                List.of(new OrderController.CheckoutItem(1L, 99999)));
        mvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createProduct() throws Exception {
        Product p = new Product("Test Product", "x", new BigDecimal("9.99"), 5, null);
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(p)))
                .andExpect(status().isCreated());
    }

    @Test
    void ordersByEmailReturnsHistory() throws Exception {
        // place an order first
        var req = new OrderController.CheckoutRequest(
                "history@example.com",
                List.of(new OrderController.CheckoutItem(2L, 1)));
        mvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/orders/by-email?email=history@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
