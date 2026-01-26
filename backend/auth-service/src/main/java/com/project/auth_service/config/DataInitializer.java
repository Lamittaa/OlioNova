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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class DataInitializer {

    private final RoleRepository roles;
    private final AuthorityRepository auths;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    void seedData() {

        // ===================== 1) CREATE ROLES =====================
        Role admin        = getOrCreateRole("ADMIN");
        Role accountant   = getOrCreateRole("ACCOUNTANT");
        Role receptionist = getOrCreateRole("RECEPTIONIST");
        Role technician   = getOrCreateRole("TECHNICIAN");

        // ===================== 2) DEFINE ALL AUTHORITIES =====================
        List<String> authorityNames = List.of(
                // Role markers
                "ROLE_ADMIN",
                "ROLE_ACCOUNTANT",
                "ROLE_RECEPTIONIST",
                "ROLE_TECHNICIAN",

                // Users & Roles management
                "ADD_USER",
                "DELETE_USER",
                "UPDATE_USER",
                "VIEW_USERS",

                "ADD_ROLE",
                "DELETE_ROLE",
                "VIEW_ROLES",

                "ASSIGN_ROLE",
                "ASSIGN_AUTHORITY",

                // Profile / Reports
                "VIEW_PROFILE",
                "VIEW_REPORTS",

                // Cities
                "CITY_CREATE",
                "CITY_READ",
                "CITY_UPDATE",
                "CITY_DELETE",

                // Customers
                "CUSTOMER_CREATE",
                "CUSTOMER_READ",
                "CUSTOMER_UPDATE",
                "CUSTOMER_DELETE",
                "CUSTOMER_SEARCH",
                "CUSTOMER_UPDATE_NATIONAL_ID",

                // Products
                "PRODUCT_CREATE",
                "PRODUCT_READ",
                "PRODUCT_UPDATE",
                "PRODUCT_UPDATE_INVENTORY",
                "PRODUCT_DELETE",

                // Orders
                "ORDER_CREATE",
                "ORDER_READ",
                "ORDER_UPDATE_STATUS",
                "ORDER_CANCEL",

                // Order Items
                "ORDER_ITEM_READ",
                "ORDER_ITEM_ADD",
                "ORDER_ITEM_UPDATE",
                "ORDER_ITEM_DELETE",

                // ✅ Order Status (Lookup)
                "ORDER_STATUS_READ"
        );

        // Create authorities
        Set<Authority> allAuthorities = new LinkedHashSet<>();
        for (String name : authorityNames) {
            allAuthorities.add(getOrCreateAuthority(name));
        }

        // Convenience
        Authority ROLE_ADMIN        = getOrCreateAuthority("ROLE_ADMIN");
        Authority ROLE_ACCOUNTANT   = getOrCreateAuthority("ROLE_ACCOUNTANT");
        Authority ROLE_RECEPTIONIST = getOrCreateAuthority("ROLE_RECEPTIONIST");
        Authority ROLE_TECHNICIAN   = getOrCreateAuthority("ROLE_TECHNICIAN");

        Authority ORDER_STATUS_READ = getOrCreateAuthority("ORDER_STATUS_READ");

        // ===================== 3) ASSIGN AUTHORITIES TO ROLES =====================

        // ADMIN => كلشي
        setAuthorities(admin, allAuthorities);

        // ACCOUNTANT
        setAuthorities(accountant, Set.of(
                ROLE_ACCOUNTANT,
                getOrCreateAuthority("VIEW_PROFILE"),
                getOrCreateAuthority("VIEW_REPORTS"),

                getOrCreateAuthority("CUSTOMER_READ"),
                getOrCreateAuthority("CITY_READ"),

                getOrCreateAuthority("PRODUCT_READ"),
                getOrCreateAuthority("PRODUCT_UPDATE_INVENTORY"),

                getOrCreateAuthority("ORDER_READ"),
                getOrCreateAuthority("ORDER_UPDATE_STATUS"),
                getOrCreateAuthority("ORDER_ITEM_READ"),

                // ✅ Order Status lookup
                ORDER_STATUS_READ
        ));

        // RECEPTIONIST
        setAuthorities(receptionist, Set.of(
                ROLE_RECEPTIONIST,
                getOrCreateAuthority("VIEW_PROFILE"),

                getOrCreateAuthority("CITY_CREATE"),
                getOrCreateAuthority("CITY_READ"),
                getOrCreateAuthority("CITY_UPDATE"),

                getOrCreateAuthority("CUSTOMER_CREATE"),
                getOrCreateAuthority("CUSTOMER_READ"),
                getOrCreateAuthority("CUSTOMER_UPDATE"),
                getOrCreateAuthority("CUSTOMER_SEARCH"),

                getOrCreateAuthority("PRODUCT_READ"),

                getOrCreateAuthority("ORDER_CREATE"),
                getOrCreateAuthority("ORDER_READ"),
                getOrCreateAuthority("ORDER_CANCEL"),

                getOrCreateAuthority("ORDER_ITEM_READ"),
                getOrCreateAuthority("ORDER_ITEM_ADD"),
                getOrCreateAuthority("ORDER_ITEM_UPDATE"),
                getOrCreateAuthority("ORDER_ITEM_DELETE"),

                // ✅ Order Status lookup
                ORDER_STATUS_READ
        ));

        // TECHNICIAN
        setAuthorities(technician, Set.of(
                ROLE_TECHNICIAN,
                getOrCreateAuthority("VIEW_PROFILE"),

                getOrCreateAuthority("ORDER_READ"),
                getOrCreateAuthority("ORDER_UPDATE_STATUS"),
                getOrCreateAuthority("ORDER_ITEM_READ"),

                // ✅ Order Status lookup
                ORDER_STATUS_READ
        ));

        roles.save(admin);
        roles.save(accountant);
        roles.save(receptionist);
        roles.save(technician);

        // ===================== 4) CREATE USERS =====================
        getOrCreateUser("admin", encoder.encode("admin"), admin);
        getOrCreateUser("acc", encoder.encode("acc"), accountant);
        getOrCreateUser("reception", encoder.encode("reception"), receptionist);
        getOrCreateUser("tech", encoder.encode("tech"), technician);
    }

    // ---------------- helpers ----------------

    private Role getOrCreateRole(String name) {
        return roles.findByName(name).orElseGet(() -> {
            Role r = Role.builder().name(name).build();
            if (r.getAuthorities() == null) r.setAuthorities(new HashSet<>());
            return roles.save(r);
        });
    }

    private Authority getOrCreateAuthority(String name) {
        return auths.findByNameIgnoreCase(name)
                .orElseGet(() -> auths.save(Authority.builder().name(name).build()));
    }

    private void setAuthorities(Role role, Set<Authority> authorities) {
        if (role.getAuthorities() == null) role.setAuthorities(new HashSet<>());
        role.getAuthorities().clear();
        role.getAuthorities().addAll(authorities);
    }

    private User getOrCreateUser(String username, String encodedPassword, Role role) {
        return users.findByUsername(username).orElseGet(() ->
                users.save(User.builder()
                        .username(username)
                        .password(encodedPassword)
                        .enabled(true)
                        .role(role)
                        .build())
        );
    }
}
