package com.project.customer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "city_lookup", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = "city_name")
})
public class CityLookup{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", length = 100, nullable = false)
    private String cityName;

}
