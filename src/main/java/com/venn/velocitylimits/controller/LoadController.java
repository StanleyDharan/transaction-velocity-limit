package com.venn.velocitylimits.controller;

import com.venn.velocitylimits.model.LoadRequest;
import com.venn.velocitylimits.model.LoadResponse;
import com.venn.velocitylimits.service.LoadService;
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

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
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
        log.info("Response: status=200, id={}, customer_id={}, accepted={}, duration={}ms",
                body.getId(), body.getCustomerId(), body.isAccepted(), duration);
        return ResponseEntity.ok(body);
    }
}
