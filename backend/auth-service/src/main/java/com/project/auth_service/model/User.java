package com.project.auth_service.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;   // خزّنه مشفّر (BCrypt)

  @Builder.Default
  @Column(nullable = false)
  private boolean enabled = true;

  // كل مستخدم له دور واحد
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_users_role"))
  private Role role;

}
