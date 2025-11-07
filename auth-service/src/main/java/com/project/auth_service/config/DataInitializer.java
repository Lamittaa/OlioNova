package com.project.auth_service.config;

import com.project.auth_service.model.Authority;
import com.project.auth_service.model.Role;
import com.project.auth_service.model.User;
import com.project.auth_service.repository.AuthorityRepository;
import com.project.auth_service.repository.RoleRepository;
import com.project.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roles;
    private final AuthorityRepository auths;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Bean
    CommandLineRunner seed() {
        return args -> seedData();
    }

    @Transactional
    void seedData() {
        Role admin = getOrCreateRole("ADMIN");
        Role accountant = getOrCreateRole("ACCOUNTANT");

        Authority addUser     = getOrCreateAuthority("ADD_USER");
        Authority deleteUser  = getOrCreateAuthority("DELETE_USER");
        Authority addRole     = getOrCreateAuthority("ADD_ROLE");
        Authority assignRole  = getOrCreateAuthority("ASSIGN_ROLE");// 🟢 ضروري لهذا الاندبوينت
        Authority viewReports = getOrCreateAuthority("VIEW_REPORTS");
           Authority viewRoles   = getOrCreateAuthority("VIEW_ROLES");    
        Authority viewProfile = getOrCreateAuthority("VIEW_PROFILE");

        attachAuthorities(admin,    Set.of(addUser, deleteUser, addRole, assignRole,    viewRoles, viewProfile));
        attachAuthorities(accountant, Set.of(viewReports));

        roles.save(admin);
        roles.save(accountant);

        getOrCreateUser("admin", encoder.encode("admin"), admin);
        getOrCreateUser("acc",   encoder.encode("acc"),   accountant);
    }


    private Role getOrCreateRole(String name) {
        return roles.findByName(name)
                .orElseGet(() -> roles.save(Role.builder().name(name).build()));
    }

    private Authority getOrCreateAuthority(String name) {
        return auths.findByNameIgnoreCase(name)
                .orElseGet(() -> auths.save(Authority.builder().name(name).build()));
    }

    private void attachAuthorities(Role role, Set<Authority> authorities) {
        role.getAuthorities().addAll(authorities);
    }

    private User getOrCreateUser(String username, String rawEncodedPassword, Role role) {
        return users.findByUsername(username).orElseGet(() ->
                users.save(User.builder()
                        .username(username)
                        .password(rawEncodedPassword)  // password مُشفّر مسبقًا
                        .enabled(true)
                        .role(role)                    // لكل مستخدم دور واحد
                        .build())
        );
    }
}
