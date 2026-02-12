package com.venn.velocitylimits.repository;

import com.venn.velocitylimits.model.LoadTransaction;
import com.venn.velocitylimits.model.LoadTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface LoadTransactionRepository extends JpaRepository<LoadTransaction, LoadTransactionId> {

    @Query("SELECT COUNT(t) FROM LoadTransaction t WHERE t.customerId = :customerId AND t.accepted = true AND t.time >= :start AND t.time < :end")
    long countAcceptedByCustomerIdAndTimeBetween(
            @Param("customerId") String customerId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(t.loadAmount), 0) FROM LoadTransaction t WHERE t.customerId = :customerId AND t.accepted = true AND t.time >= :start AND t.time < :end")
    BigDecimal sumAcceptedAmountByCustomerIdAndTimeBetween(
            @Param("customerId") String customerId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
