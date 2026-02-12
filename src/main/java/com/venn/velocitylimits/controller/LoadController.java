package com.venn.velocitylimits.controller;

import com.venn.velocitylimits.model.LoadRequest;
import com.venn.velocitylimits.model.LoadResponse;
import com.venn.velocitylimits.service.LoadService;
import com.venn.velocitylimits.service.ResponseFileWriter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LoadController {

    private static final Logger log = LoggerFactory.getLogger(LoadController.class);

    private final LoadService loadService;
    private final ResponseFileWriter responseFileWriter;

    public LoadController(LoadService loadService, ResponseFileWriter responseFileWriter) {
        this.loadService = loadService;
        this.responseFileWriter = responseFileWriter;
    }

    @PostMapping("/loads")
    public ResponseEntity<LoadResponse> load(@Valid @RequestBody LoadRequest request) {
        log.info("POST /api/loads id={}, customer_id={}", request.getId(), request.getCustomerId());

        long start = System.currentTimeMillis();
        Optional<LoadResponse> response = loadService.processLoad(request);
        long duration = System.currentTimeMillis() - start;

        if (response.isEmpty()) {
            log.info("Response: status=204, id={}, customer_id={}, duration={}ms",
                    request.getId(), request.getCustomerId(), duration);
            return ResponseEntity.noContent().build();
        }

        LoadResponse body = response.get();
        responseFileWriter.write(body);
        log.info("Response: status=200, id={}, customer_id={}, accepted={}, duration={}ms",
                body.getId(), body.getCustomerId(), body.isAccepted(), duration);
        return ResponseEntity.ok(body);
    }
}
