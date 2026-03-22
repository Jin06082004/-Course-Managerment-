package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.CourseDto;
import com._6.CourseManagerment.dto.CreateCourseRequest;
import com._6.CourseManagerment.entity.Category;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.User;
import com._6.CourseManagerment.repository.CategoryRepository;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseService {
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new course
     */
    public CourseDto createCourse(CreateCourseRequest request, Long instructorId) throws Exception {
        // Validate course code uniqueness
        if (courseRepository.existsByCode(request.getCode())) {
            throw new Exception("Course code already exists");
        }
        
        // Get instructor
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new Exception("Instructor not found"));
        
        // Get category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new Exception("Category not found"));
        
        // Create course
        Course course = new Course(
                request.getTitle(),
                request.getDescription(),
                request.getCode(),
                category,
                instructor,
                request.getLevel(),
                request.getPrice(),
                request.getDuration()
        );
        
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setVideoUrl(request.getVideoUrl());
        course.setStatus("DRAFT");
        
        Course savedCourse = courseRepository.save(course);
        return new CourseDto(savedCourse);
    }
    
    /**
     * Get course by ID
     */
    public CourseDto getCourseById(Long id) throws Exception {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new Exception("Course not found"));
        return new CourseDto(course);
    }
    
    /**
     * Get all courses with pagination
     */
    public Page<CourseDto> getAllCourses(Pageable pageable) {
        return courseRepository.findByStatus("PUBLISHED", pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Search courses by title
     */
    public Page<CourseDto> searchCourses(String title, Pageable pageable) {
        return courseRepository.findByTitleContainingIgnoreCase(title, pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Get courses by category
     */
    public Page<CourseDto> getCoursesByCategory(Long categoryId, Pageable pageable) {
        return courseRepository.findByCategory_Id(categoryId, pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Get courses by level
     */
    public Page<CourseDto> getCoursesByLevel(String level, Pageable pageable) {
        return courseRepository.findByLevel(level, pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Get courses by instructor
     */
    public Page<CourseDto> getCoursesByInstructor(Long instructorId, Pageable pageable) {
        return courseRepository.findByInstructor_Id(instructorId, pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Get featured courses
     */
    public List<CourseDto> getFeaturedCourses(Pageable pageable) {
        return courseRepository.findFeaturedCourses(pageable)
                .stream()
                .map(CourseDto::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Search courses with multiple filters
     */
    public Page<CourseDto> searchCourses(String title, Long categoryId, String level, Pageable pageable) {
        return courseRepository.searchCourses(title, categoryId, level, pageable)
                .map(CourseDto::new);
    }
    
    /**
     * Update course
     */
    public CourseDto updateCourse(Long id, CreateCourseRequest request) throws Exception {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new Exception("Course not found"));
        
        // Check code uniqueness if changed
        if (!course.getCode().equals(request.getCode()) && 
            courseRepository.existsByCode(request.getCode())) {
            throw new Exception("Course code already exists");
        }
        
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCode(request.getCode());
        course.setLevel(request.getLevel());
        course.setPrice(request.getPrice());
        course.setDuration(request.getDuration());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setVideoUrl(request.getVideoUrl());
        
        // Update category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new Exception("Category not found"));
            course.setCategory(category);
        }
        
        Course updatedCourse = courseRepository.save(course);
        return new CourseDto(updatedCourse);
    }
    
    /**
     * Delete course
     */
    public void deleteCourse(Long id) throws Exception {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new Exception("Course not found"));
        courseRepository.delete(course);
    }
    
    /**
     * Publish course
     */
    public CourseDto publishCourse(Long id) throws Exception {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new Exception("Course not found"));
        course.setStatus("PUBLISHED");
        Course published = courseRepository.save(course);
        return new CourseDto(published);
    }
    
    /**
     * Archive course
     */
    public CourseDto archiveCourse(Long id) throws Exception {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new Exception("Course not found"));
        course.setStatus("ARCHIVED");
        Course archived = courseRepository.save(course);
        return new CourseDto(archived);
    }
    
    /**
     * Update course rating
     */
    public void updateCourseRating(Long courseId, Float newRating) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course not found"));
        course.setRating(newRating);
        courseRepository.save(course);
    }
    
    /**
     * Increment student count
     */
    public void incrementStudentCount(Long courseId) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new Exception("Course not found"));
        course.setStudentCount(course.getStudentCount() + 1);
        courseRepository.save(course);
    }
}
