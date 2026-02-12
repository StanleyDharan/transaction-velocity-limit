package com.venn.velocitylimits.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public class VelocityLimitsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAllLoadsAgainstExpectedOutput() throws Exception {
        List<String> inputLines = readResourceLines("input.txt");
        List<String> expectedOutputLines = readResourceLines("output.txt");

        List<String> actualOutputLines = new ArrayList<>();

        for (String inputLine : inputLines) {
            MvcResult result = mockMvc.perform(
                    post("/api/loads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(inputLine))
                    .andReturn();

            int status = result.getResponse().getStatus();

            if (status == 200) {
                String responseBody = result.getResponse().getContentAsString();
                // Normalize JSON to match expected format
                JsonNode node = objectMapper.readTree(responseBody);
                String normalized = objectMapper.writeValueAsString(node);
                actualOutputLines.add(normalized);
            }
            // 204 = duplicate, skip (no output expected)
        }

        assertEquals(expectedOutputLines.size(), actualOutputLines.size(),
                "Number of output lines should match");

        for (int i = 0; i < expectedOutputLines.size(); i++) {
            JsonNode expected = objectMapper.readTree(expectedOutputLines.get(i));
            JsonNode actual = objectMapper.readTree(actualOutputLines.get(i));
            assertEquals(expected, actual,
                    "Mismatch at output line " + (i + 1) +
                    "\nExpected: " + expectedOutputLines.get(i) +
                    "\nActual:   " + actualOutputLines.get(i));
        }
    }

    private List<String> readResourceLines(String resourceName) throws Exception {
        List<String> lines = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }
}
