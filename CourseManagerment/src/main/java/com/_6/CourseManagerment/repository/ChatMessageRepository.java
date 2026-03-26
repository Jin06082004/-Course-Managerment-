package com._6.CourseManagerment.repository;

import com._6.CourseManagerment.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT c FROM ChatMessage c WHERE c.lesson.id = :lessonId AND c.user.id = :userId ORDER BY c.createdAt ASC")
    List<ChatMessage> findChatHistoryByLessonAndUser(@Param("lessonId") Long lessonId, @Param("userId") Long userId);

    @Query("SELECT c FROM ChatMessage c WHERE c.lesson.id = :lessonId ORDER BY c.createdAt DESC LIMIT 1")
    ChatMessage findLatestChatByLesson(@Param("lessonId") Long lessonId);

    List<ChatMessage> findByLesson_IdOrderByCreatedAtDesc(Long lessonId);
}
