package com.project.auth_service.config;

import com.project.auth_service.model.*;
import com.project.auth_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Component
public class DataInitializer {

        private final RoleRepository roles;
        private final AuthorityRepository auths;
        private final UserRepository users;
        private final EmployeeRepository employeeRepo;
        private final PasswordEncoder encoder;

        @Transactional
        void seedData() {

                Role admin = getOrCreateRole("ADMIN");
                Role accountant = getOrCreateRole("ACCOUNTANT");
                Role receptionist = getOrCreateRole("RECEPTIONIST");
                Role technician = getOrCreateRole("TECHNICIAN");

                List<String> authorityNames = List.of(
                                "ROLE_ADMIN", "ROLE_ACCOUNTANT", "ROLE_RECEPTIONIST", "ROLE_TECHNICIAN",

                                "ADD_USER", "DELETE_USER", "UPDATE_USER", "VIEW_USERS",
                                "ADD_ROLE", "DELETE_ROLE", "VIEW_ROLES",
                                "ASSIGN_ROLE", "ASSIGN_AUTHORITY",

                                "VIEW_PROFILE", "VIEW_REPORTS",

                                "CITY_CREATE", "CITY_READ", "CITY_UPDATE", "CITY_DELETE",

                                "CUSTOMER_CREATE", "CUSTOMER_READ", "CUSTOMER_UPDATE",
                                "CUSTOMER_DELETE", "CUSTOMER_SEARCH", "CUSTOMER_UPDATE_NATIONAL_ID",

                                "PRODUCT_CREATE", "PRODUCT_READ", "PRODUCT_UPDATE",
                                "PRODUCT_UPDATE_INVENTORY", "PRODUCT_DELETE",

                                "ORDER_CREATE", "ORDER_READ", "ORDER_UPDATE_STATUS", "ORDER_CANCEL",

                                "ORDER_ITEM_READ", "ORDER_ITEM_ADD",
                                "ORDER_ITEM_UPDATE", "ORDER_ITEM_DELETE",

                                "ORDER_STATUS_READ");

                Set<Authority> allAuthorities = new LinkedHashSet<>();
                for (String name : authorityNames) {
                        allAuthorities.add(getOrCreateAuthority(name));
                }

                setAuthorities(admin, allAuthorities);

                setAuthorities(accountant, Set.of(
                                getOrCreateAuthority("ROLE_ACCOUNTANT"),
                                getOrCreateAuthority("VIEW_PROFILE"),
                                getOrCreateAuthority("VIEW_REPORTS"),
                                getOrCreateAuthority("CUSTOMER_READ"),
                                getOrCreateAuthority("CITY_READ"),
                                getOrCreateAuthority("PRODUCT_READ"),
                                getOrCreateAuthority("PRODUCT_UPDATE_INVENTORY"),
                                getOrCreateAuthority("ORDER_READ"),
                                getOrCreateAuthority("ORDER_UPDATE_STATUS"),
                                getOrCreateAuthority("ORDER_ITEM_READ"),
                                getOrCreateAuthority("ORDER_STATUS_READ")));

                setAuthorities(receptionist, Set.of(
                                getOrCreateAuthority("ROLE_RECEPTIONIST"),
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
                                getOrCreateAuthority("ORDER_STATUS_READ")));

                setAuthorities(technician, Set.of(
                                getOrCreateAuthority("ROLE_TECHNICIAN"),
                                getOrCreateAuthority("VIEW_PROFILE"),
                                getOrCreateAuthority("ORDER_READ"),
                                getOrCreateAuthority("ORDER_UPDATE_STATUS"),
                                getOrCreateAuthority("ORDER_ITEM_READ"),
                                getOrCreateAuthority("ORDER_STATUS_READ")));

                roles.saveAll(List.of(admin, accountant, receptionist, technician));

                User adminUser = getOrCreateUser("admin", "admin", admin);
                User accUser = getOrCreateUser("acc", "acc", accountant);
                User recUser = getOrCreateUser("reception", "reception", receptionist);
                User techUser = getOrCreateUser("tech", "tech", technician);

                createEmployeeIfMissing(adminUser, "000000001", "Admin", "System");
                createEmployeeIfMissing(accUser, "000000002", "Accountant", "User");
                createEmployeeIfMissing(recUser, "000000003", "Reception", "User");
                createEmployeeIfMissing(techUser, "000000004", "Technician", "User");
        }

        private Role getOrCreateRole(String name) {
                return roles.findByName(name).orElseGet(() -> roles.save(Role.builder().name(name).build()));
        }

        private Authority getOrCreateAuthority(String name) {
                return auths.findByNameIgnoreCase(name)
                                .orElseGet(() -> auths.save(Authority.builder().name(name).build()));
        }

        private void setAuthorities(Role role, Set<Authority> authorities) {
                if (role.getAuthorities() == null) {
                        role.setAuthorities(new HashSet<>());
                }
                role.getAuthorities().clear();
                role.getAuthorities().addAll(authorities);
        }

        private User getOrCreateUser(String username, String rawPassword, Role role) {
                return users.findByUsername(username).orElseGet(() -> users.save(User.builder()
                                .username(username)
                                .password(encoder.encode(rawPassword))
                                .enabled(true)
                                .role(role)
                                .build()));
        }

        private void createEmployeeIfMissing(
                        User user,
                        String nationalId,
                        String firstName,
                        String lastName) {
                employeeRepo.findByUserUsername(user.getUsername())
                                .orElseGet(() -> employeeRepo.save(
                                                Employee.builder()
                                                                .user(user)
                                                                .nationalId(nationalId)
                                                                .firstName(firstName)
                                                                .lastName(lastName)
                                                                .email(user.getUsername() + "@system.local")
                                                                .phoneNumber("0000000000")
                                                                .gender(Gender.MALE)
                                                                .martialStatus(MaritalStatus.SINGLE)
                                                                .build()));
        }
}
