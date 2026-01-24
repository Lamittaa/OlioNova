package com.project.auth_service.service;

import com.project.auth_service.model.User;
import com.project.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    @Transactional(readOnly = true) // ✅ حل مشكلة LazyInitializationException
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = u.getRole().getAuthorities().stream()
                .map(a -> new SimpleGrantedAuthority(a.getName()))
                .collect(Collectors.toSet());
         authorities.add(new SimpleGrantedAuthority("ROLE_" + u.getRole().getName()));
                    

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPassword(),
                u.isEnabled(),
                true, true, true,
                authorities
        );
    }
}
