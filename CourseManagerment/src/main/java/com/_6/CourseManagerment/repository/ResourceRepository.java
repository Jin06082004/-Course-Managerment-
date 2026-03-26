package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByCourse_IdOrderByCreatedAtDesc(Long courseId);
}
