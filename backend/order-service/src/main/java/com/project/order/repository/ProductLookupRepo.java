package com.project.order.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.order.model.ProductLookup;

public interface ProductLookupRepo extends JpaRepository<ProductLookup, Long> {

    Optional<ProductLookup> findByProductNameIgnoreCase(String productName);

    boolean existsByProductNameIgnoreCase(String productName);

    Optional<ProductLookup> findByIdAndActiveTrue(Long id);

    List<ProductLookup> findByActiveTrueOrderByProductNameAsc();

    List<ProductLookup> findByProductTypeAndActiveTrue(String productType);

    @Query("select p.productName from ProductLookup p where p.active = true order by p.productName")
    List<String> findAllActiveProductNames();

}
