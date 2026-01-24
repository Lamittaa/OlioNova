package com.project.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "order_status",
    schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "status_name")
    }
)
public class OrderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_name", length = 50, nullable = false)
    private String statusName;
}
