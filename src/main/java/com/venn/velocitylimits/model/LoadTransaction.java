package com.venn.velocitylimits.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "load_transactions")
@IdClass(LoadTransactionId.class)
public class LoadTransaction {

    @Id
    private String id;

    @Id
    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "load_amount", precision = 19, scale = 2)
    private BigDecimal loadAmount;

    @Column(name = "time")
    private Instant time;

    @Column(name = "accepted")
    private boolean accepted;

    public LoadTransaction() {}

    public LoadTransaction(String id, String customerId, BigDecimal loadAmount, Instant time, boolean accepted) {
        this.id = id;
        this.customerId = customerId;
        this.loadAmount = loadAmount;
        this.time = time;
        this.accepted = accepted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getLoadAmount() { return loadAmount; }
    public void setLoadAmount(BigDecimal loadAmount) { this.loadAmount = loadAmount; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
}
