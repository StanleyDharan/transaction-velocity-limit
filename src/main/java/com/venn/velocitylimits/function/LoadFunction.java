package com.venn.velocitylimits.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.venn.velocitylimits.VelocityLimitsApplication;
import com.venn.velocitylimits.model.LoadRequest;
import com.venn.velocitylimits.model.LoadResponse;
import com.venn.velocitylimits.service.LoadService;
import com.venn.velocitylimits.service.ResponseFileWriter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LoadFunction {

    private static final ConfigurableApplicationContext context;
    private static final LoadService loadService;
    private static final ResponseFileWriter responseFileWriter;
    private static final Validator validator;
    private static final ObjectMapper objectMapper;

    static {
        // On Azure, persist SQLite DB to durable storage
        String home = System.getenv("HOME");
        if (home != null && !home.isBlank()) {
            String dataDir = home + "/data";
            System.setProperty("spring.datasource.url", "jdbc:sqlite:" + dataDir + "/velocity-limits.db");
            System.setProperty("velocity-limits.output-dir", dataDir);
        }

        context = new SpringApplicationBuilder(VelocityLimitsApplication.class)
                .web(WebApplicationType.NONE)
                .run();

        loadService = context.getBean(LoadService.class);
        responseFileWriter = context.getBean(ResponseFileWriter.class);
        validator = context.getBean(Validator.class);
        objectMapper = context.getBean(ObjectMapper.class);
    }

    @FunctionName("loads")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "loads",
                    authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<Optional<String>> request,
            ExecutionContext executionContext) {

        executionContext.getLogger().info("Azure Function: POST /api/loads invoked");

        try {
            // Parse body
            String body = request.getBody().orElse(null);
            if (body == null || body.isBlank()) {
                return errorResponse(request, HttpStatus.BAD_REQUEST,
                        "invalid_request", "Request body is required");
            }

            LoadRequest loadRequest;
            try {
                loadRequest = objectMapper.readValue(body, LoadRequest.class);
            } catch (Exception e) {
                return errorResponse(request, HttpStatus.BAD_REQUEST,
                        "invalid_request", "Malformed JSON or unrecognized field format");
            }

            // Validate
            Set<ConstraintViolation<LoadRequest>> violations = validator.validate(loadRequest);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return errorResponse(request, HttpStatus.BAD_REQUEST,
                        "invalid_request", errors);
            }

            // Process
            Optional<LoadResponse> result = loadService.processLoad(loadRequest);

            if (result.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
            }

            LoadResponse response = result.get();
            responseFileWriter.write(response);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(response))
                    .build();

        } catch (Exception e) {
            executionContext.getLogger().severe("Unexpected error: " + e.getMessage());
            return errorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR,
                    "internal_error", "An unexpected error occurred. Please retry.");
        }
    }

    private HttpResponseMessage errorResponse(HttpRequestMessage<?> request,
                                               HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        try {
            return request.createResponseBuilder(status)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .build();
        } catch (Exception e) {
            return request.createResponseBuilder(status)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}")
                    .build();
        }
    }
}
