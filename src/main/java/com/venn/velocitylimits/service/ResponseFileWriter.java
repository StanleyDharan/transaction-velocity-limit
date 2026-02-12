package com.venn.velocitylimits.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.venn.velocitylimits.model.LoadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class ResponseFileWriter {

    private static final Logger log = LoggerFactory.getLogger(ResponseFileWriter.class);

    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public ResponseFileWriter(ObjectMapper objectMapper,
                              @Value("${velocity-limits.output-dir:data}") String outputDir) {
        this.objectMapper = objectMapper;
        this.outputPath = Paths.get(outputDir, "output.txt");
    }

    public void write(LoadResponse response) {
        try {
            Files.createDirectories(outputPath.getParent());
            String line = objectMapper.writeValueAsString(response) + System.lineSeparator();
            Files.writeString(outputPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write response to {}: {}", outputPath, e.getMessage());
        }
    }
}
