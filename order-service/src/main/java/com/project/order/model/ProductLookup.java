package com.project.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "product_lookup", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = "product_name")
})
public class ProductLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    @Column(name = "product_type", length = 50, nullable = false)
    private String productType;

    @Column(name = "inventory")
    private Integer inventory;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "member_discount", precision = 5, scale = 2)
    private BigDecimal memberDiscount;

    @Column(name = "unit", length = 20, nullable = false)
    private String unit;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;

}
