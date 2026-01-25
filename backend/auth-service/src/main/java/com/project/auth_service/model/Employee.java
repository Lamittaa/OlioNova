package com.project.auth_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "employee",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "national_id"),
        @UniqueConstraint(columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // رقم الهوية
    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = true, length = 100)
    private String email;

    // MALE / FEMALE
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    // SINGLE / MARRIED
    @Enumerated(EnumType.STRING)
    @Column(name = "martial_status", nullable = false, length = 20)
    private MaritalStatus martialStatus;

    // ربط الموظف مع اليوزر (auth-service)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_employee_user")
    )
    private User user;
}
