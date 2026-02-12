package com.venn.velocitylimits.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venn.velocitylimits.model.LoadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseFileWriterTest {

    private ObjectMapper objectMapper;
    private ResponseFileWriter writer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        writer = new ResponseFileWriter(objectMapper, tempDir.toString());
    }

    @Test
    void testWritesSingleResponse() throws IOException {
        writer.write(new LoadResponse("1", "100", true));

        Path outputFile = tempDir.resolve("output.txt");
        assertTrue(Files.exists(outputFile));

        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(1, lines.size());

        JsonNode node = objectMapper.readTree(lines.get(0));
        assertEquals("1", node.get("id").asText());
        assertEquals("100", node.get("customer_id").asText());
        assertTrue(node.get("accepted").asBoolean());
    }

    @Test
    void testAppendsMultipleResponses() throws IOException {
        writer.write(new LoadResponse("1", "100", true));
        writer.write(new LoadResponse("2", "100", false));

        Path outputFile = tempDir.resolve("output.txt");
        List<String> lines = Files.readAllLines(outputFile);
        assertEquals(2, lines.size());

        JsonNode first = objectMapper.readTree(lines.get(0));
        assertTrue(first.get("accepted").asBoolean());

        JsonNode second = objectMapper.readTree(lines.get(1));
        assertFalse(second.get("accepted").asBoolean());
    }

    @Test
    void testCreatesOutputDirectory() throws IOException {
        Path nestedDir = tempDir.resolve("nested/deep");
        ResponseFileWriter nestedWriter = new ResponseFileWriter(objectMapper, nestedDir.toString());

        nestedWriter.write(new LoadResponse("1", "100", true));

        Path outputFile = nestedDir.resolve("output.txt");
        assertTrue(Files.exists(outputFile));
        assertEquals(1, Files.readAllLines(outputFile).size());
    }
}
