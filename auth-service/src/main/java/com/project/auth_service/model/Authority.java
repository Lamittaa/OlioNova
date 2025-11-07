package com.project.auth_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "authorities", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Authority {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name; // ADD_USER, DELETE_USER, ...

  // مرجع عكسي (اختياري)
  @ManyToMany(mappedBy = "authorities")
  @Builder.Default
  private Set<Role> roles = new HashSet<>();
}
