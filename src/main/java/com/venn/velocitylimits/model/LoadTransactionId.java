package com.venn.velocitylimits.model;

import java.io.Serializable;
import java.util.Objects;

public class LoadTransactionId implements Serializable {

    private String id;
    private String customerId;

    public LoadTransactionId() {}

    public LoadTransactionId(String id, String customerId) {
        this.id = id;
        this.customerId = customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadTransactionId that = (LoadTransactionId) o;
        return Objects.equals(id, that.id) && Objects.equals(customerId, that.customerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId);
    }
}
