package com.smartretail.backend.repository;

import com.smartretail.backend.models.Bill;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends MongoRepository<Bill, String> {
    Optional<Bill> findByBillIdAndShopId(String billId, String shopId);
    boolean existsByBillIdAndShopId(String billId, String shopId);
    List<Bill> findByShopIdAndCreatedAtBetween(String shopId, Date startDate, Date endDate);
    List<Bill> findByShopId(String shopId);
}