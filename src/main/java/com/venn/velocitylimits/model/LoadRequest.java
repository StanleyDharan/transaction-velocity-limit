package com.venn.velocitylimits.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class LoadRequest {

    @NotBlank
    private String id;

    @NotBlank
    @JsonProperty("customer_id")
    private String customerId;

    @NotBlank
    @Pattern(regexp = "^\\$\\d+(\\.\\d{1,2})?$", message = "must be in $0.00 format")
    @JsonProperty("load_amount")
    private String loadAmount;

    @NotNull
    private Instant time;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getLoadAmount() { return loadAmount; }
    public void setLoadAmount(String loadAmount) { this.loadAmount = loadAmount; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
}
