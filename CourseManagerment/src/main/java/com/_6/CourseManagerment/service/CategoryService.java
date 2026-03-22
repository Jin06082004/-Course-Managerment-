package com._6.CourseManagerment.service;

import com._6.CourseManagerment.entity.Category;
import com._6.CourseManagerment.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * Create a new category
     */
    public Category createCategory(String name, String description, String icon, String color) throws Exception {
        if (categoryRepository.existsByName(name)) {
            throw new Exception("Category already exists");
        }
        
        Category category = new Category(name, description, icon, color);
        return categoryRepository.save(category);
    }
    
    /**
     * Get all categories
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    /**
     * Get category by ID
     */
    public Category getCategoryById(Long id) throws Exception {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new Exception("Category not found"));
    }
    
    /**
     * Get category by name
     */
    public Category getCategoryByName(String name) throws Exception {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new Exception("Category not found"));
    }
    
    /**
     * Update category
     */
    public Category updateCategory(Long id, String name, String description, String icon, String color) throws Exception {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new Exception("Category not found"));
        
        // Check name uniqueness if changed
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new Exception("Category name already exists");
        }
        
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        category.setColor(color);
        
        return categoryRepository.save(category);
    }
    
    /**
     * Delete category
     */
    public void deleteCategory(Long id) throws Exception {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new Exception("Category not found"));
        categoryRepository.delete(category);
    }
}
