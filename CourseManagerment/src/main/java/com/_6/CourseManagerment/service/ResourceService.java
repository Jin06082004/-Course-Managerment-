package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.CreateResourceRequest;
import com._6.CourseManagerment.dto.ResourceDto;
import com._6.CourseManagerment.entity.Course;
import com._6.CourseManagerment.entity.Resource;
import com._6.CourseManagerment.repository.CourseRepository;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.ResourceRepository;
import com._6.CourseManagerment.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    public List<ResourceDto> getCourseResourcesForUser(Long courseId, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean hasPurchasedAccess = enrollmentRepository.existsPurchasedAccess(userId, courseId);
        if (!hasPurchasedAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this course to access resources");
        }

        return resourceRepository.findByCourse_IdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(ResourceDto::new)
                .toList();
    }

    public ResourceDto addResource(CreateResourceRequest request, Long instructorId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (course.getInstructor() == null || !course.getInstructor().getId().equals(instructorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add resources to your own courses");
        }

        Resource resource = new Resource(request.getTitle(), request.getUrl(), course);
        Resource saved = resourceRepository.save(resource);

        log.info("Instructor {} added resource {} to course {}", instructorId, saved.getId(), course.getId());
        return new ResourceDto(saved);
    }

    public void deleteResource(Long resourceId, boolean isAdmin, Long currentUserId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        Long ownerInstructorId = resource.getCourse() != null && resource.getCourse().getInstructor() != null
                ? resource.getCourse().getInstructor().getId()
                : null;

        if (!isAdmin && (currentUserId == null || !currentUserId.equals(ownerInstructorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this resource");
        }

        resourceRepository.delete(resource);
        log.info("Resource {} deleted by user {}", resourceId, currentUserId);
    }

    public boolean isPurchasedByCurrentUser(Long courseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId != null && enrollmentRepository.existsPurchasedAccess(userId, courseId);
    }
}
