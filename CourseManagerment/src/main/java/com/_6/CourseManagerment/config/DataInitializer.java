package com._6.CourseManagerment.config;

import com._6.CourseManagerment.entity.Role;
import com._6.CourseManagerment.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Data Initialization - Creates default roles on application startup
 */
@Configuration
@Slf4j
public class DataInitializer {
    
    @Autowired
    private RoleRepository roleRepository;
    
    /**
     * Initialize default roles on application startup
     */
    @Bean
    public CommandLineRunner initializeRoles() {
        return args -> {
            log.info("Initializing default roles...");
            
            // Create ADMIN role
            if (!roleRepository.existsByName("ADMIN")) {
                roleRepository.save(new Role(null, "ADMIN"));
                log.info("ADMIN role created");
            }
            
            // Create INSTRUCTOR role
            if (!roleRepository.existsByName("INSTRUCTOR")) {
                roleRepository.save(new Role(null, "INSTRUCTOR"));
                log.info("INSTRUCTOR role created");
            }
            
            // Create STUDENT role
            if (!roleRepository.existsByName("STUDENT")) {
                roleRepository.save(new Role(null, "STUDENT"));
                log.info("STUDENT role created");
            }
            
            log.info("Role initialization completed");
        };
    }
}
