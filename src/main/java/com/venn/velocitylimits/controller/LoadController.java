package com.venn.velocitylimits.controller;

import com.venn.velocitylimits.model.LoadRequest;
import com.venn.velocitylimits.model.LoadResponse;
import com.venn.velocitylimits.service.LoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LoadController {

    private final LoadService loadService;

    public LoadController(LoadService loadService) {
        this.loadService = loadService;
    }

    @PostMapping("/loads")
    public ResponseEntity<LoadResponse> load(@RequestBody LoadRequest request) {
        Optional<LoadResponse> response = loadService.processLoad(request);

        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response.get());
    }
}
