package com.project.customer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "customer", schema = "public")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "national_id", length = 50, nullable = false, unique = true)
    private String nationalId;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100, nullable = false)
    private String lastName;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "city_id", nullable = false)
    private CityLookup city;

    @Column(name = "is_member")
    private Boolean isMember;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;


}
