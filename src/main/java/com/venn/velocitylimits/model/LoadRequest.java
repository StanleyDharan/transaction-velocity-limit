package com.venn.velocitylimits.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class LoadRequest {

    private String id;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("load_amount")
    private String loadAmount;

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
