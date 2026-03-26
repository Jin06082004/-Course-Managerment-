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
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                roleRepository.save(adminRole);
                log.info("ADMIN role created");
            }
            
            // Create INSTRUCTOR role
            if (!roleRepository.existsByName("INSTRUCTOR")) {
                Role instructorRole = new Role();
                instructorRole.setName("INSTRUCTOR");
                roleRepository.save(instructorRole);
                log.info("INSTRUCTOR role created");
            }
            
            // Create STUDENT role
            if (!roleRepository.existsByName("STUDENT")) {
                Role studentRole = new Role();
                studentRole.setName("STUDENT");
                roleRepository.save(studentRole);
                log.info("STUDENT role created");
            }
            
            log.info("Role initialization completed");
        };
    }
}
