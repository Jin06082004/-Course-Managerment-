package com._6.CourseManagerment.config;

import com._6.CourseManagerment.entity.Category;
import com._6.CourseManagerment.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CategoryInitializer implements CommandLineRunner {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeCategories();
    }
    
    private void initializeCategories() {
        // Check and create default categories
        String[] categoryNames = {
                "Web Development",
                "Mobile Development",
                "Backend Development",
                "Data Science",
                "Cloud Computing",
                "DevOps",
                "UI/UX Design",
                "Game Development"
        };
        
        String[] categoryDescriptions = {
                "Learn frontend and full-stack web development",
                "Master iOS, Android, and cross-platform development",
                "Build robust backend systems and APIs",
                "Explore data analysis, machine learning, and AI",
                "Deploy and manage applications on cloud platforms",
                "Master containerization, orchestration, and CI/CD",
                "Design beautiful and intuitive user interfaces",
                "Create interactive and engaging games"
        };
        
        String[] categoryIcons = {
                "layout",
                "smartphone",
                "server",
                "bar-chart-2",
                "cloud",
                "tool",
                "palette",
                "zap"
        };
        
        String[] categoryColors = {
                "#FF6B35",
                "#7C3AED",
                "#00D4FF",
                "#FFD700",
                "#FF1493",
                "#00FF00",
                "#FF69B4",
                "#FF4500"
        };
        
        for (int i = 0; i < categoryNames.length; i++) {
            if (!categoryRepository.existsByName(categoryNames[i])) {
                Category category = new Category(
                        categoryNames[i],
                        categoryDescriptions[i],
                        categoryIcons[i],
                        categoryColors[i]
                );
                categoryRepository.save(category);
                System.out.println("✓ Category created: " + categoryNames[i]);
            }
        }
    }
}
