package com._6.CourseManagerment.security;

import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementation
 * Loads user details from database
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> {
                log.warn("User not found: {}", usernameOrEmail);
                return new UsernameNotFoundException("User not found: " + usernameOrEmail);
            });
        
        // Convert roles to authorities
        var authorities = user.getRoles().stream()
            .map(role -> {
                // Ensure role starts with "ROLE_" for Spring Security
                String roleName = role.getName();
                if (!roleName.startsWith("ROLE_")) {
                    roleName = "ROLE_" + roleName;
                }
                return new SimpleGrantedAuthority(roleName);
            })
            .collect(Collectors.toList());
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.getStatus().equals("ACTIVE"))
            .build();
    }
}
