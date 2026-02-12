package com.venn.velocitylimits.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testInvalidLoadAmountFormat() throws Exception {
        mockMvc.perform(post("/api/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"customer_id\":\"100\",\"load_amount\":\"abc\",\"time\":\"2000-01-03T10:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void testMalformedJson() throws Exception {
        mockMvc.perform(post("/api/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testMissingCustomerId() throws Exception {
        mockMvc.perform(post("/api/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"load_amount\":\"$100.00\",\"time\":\"2000-01-03T10:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void testMissingTime() throws Exception {
        mockMvc.perform(post("/api/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"customer_id\":\"100\",\"load_amount\":\"$100.00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }
}
