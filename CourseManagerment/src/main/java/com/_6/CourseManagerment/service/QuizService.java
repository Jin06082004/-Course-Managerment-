package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.CreateQuizRequest;
import com._6.CourseManagerment.dto.LessonQuizDto;
import com._6.CourseManagerment.dto.QuizDTO;
import com._6.CourseManagerment.dto.QuizQuestionDto;
import com._6.CourseManagerment.dto.QuizSubmitRequest;
import com._6.CourseManagerment.dto.QuizSubmitResultDto;
import com._6.CourseManagerment.entity.*;
import com._6.CourseManagerment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CourseRepository courseRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    /**
     * Create a new quiz with questions
     */
    public QuizDTO createQuiz(Long courseId, Long instructorId, CreateQuizRequest request) {
        log.info("Creating quiz for course: {}, instructor: {}", courseId, instructorId);

        // Verify course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // Verify instructor exists
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        // Create quiz entity
        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .course(course)
                .instructor(instructor)
                .passingScore(request.getPassingScore())
                .isDraft(request.getSaveAsDraft() != null ? request.getSaveAsDraft() : true)
                .isPublished(request.getSaveAsDraft() != null ? !request.getSaveAsDraft() : false)
                .publishedAt(request.getSaveAsDraft() != null && !request.getSaveAsDraft() ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Add lesson if provided
        if (request.getLessonId() != null) {
            Resource lesson = resourceRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
            quiz.setLesson(lesson);
        }

        quiz = quizRepository.save(quiz);

        // Add questions
        List<QuizQuestion> questions = new ArrayList<>();
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuizQuestionDto questionDTO = request.getQuestions().get(i);
                QuizQuestion question = QuizQuestion.builder()
                        .quiz(quiz)
                        .content(questionDTO.getContent())
                        .optionA(questionDTO.getOptionA())
                        .optionB(questionDTO.getOptionB())
                        .optionC(questionDTO.getOptionC())
                        .optionD(questionDTO.getOptionD())
                        .correctAnswer(QuizQuestion.CorrectAnswer.valueOf(questionDTO.getCorrectAnswer()))
                        .explanation(questionDTO.getExplanation())
                        .questionOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                questions.add(question);
            }
            questions = quizQuestionRepository.saveAll(questions);
        }

        quiz.setQuestions(questions);
        quiz.updateQuestionCount();
        quiz = quizRepository.save(quiz);

        log.info("Quiz created successfully with ID: {}", quiz.getId());
        return convertToDTO(quiz);
    }

    /**
     * Update existing quiz
     */
    public QuizDTO updateQuiz(Long quizId, Long instructorId, CreateQuizRequest request) {
        log.info("Updating quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to update this quiz");
        }

        // Update basic fields
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setPassingScore(request.getPassingScore());
        quiz.setUpdatedAt(LocalDateTime.now());

        // Update lesson if provided
        if (request.getLessonId() != null) {
            Resource lesson = resourceRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
            quiz.setLesson(lesson);
        }

        // Remove old questions
        quizQuestionRepository.deleteByQuizId(quizId);
        quiz.getQuestions().clear();

        // Add new questions
        List<QuizQuestion> questions = new ArrayList<>();
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                QuizQuestionDto questionDTO = request.getQuestions().get(i);
                QuizQuestion question = QuizQuestion.builder()
                        .quiz(quiz)
                        .content(questionDTO.getContent())
                        .optionA(questionDTO.getOptionA())
                        .optionB(questionDTO.getOptionB())
                        .optionC(questionDTO.getOptionC())
                        .optionD(questionDTO.getOptionD())
                        .correctAnswer(QuizQuestion.CorrectAnswer.valueOf(questionDTO.getCorrectAnswer()))
                        .explanation(questionDTO.getExplanation())
                        .questionOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                questions.add(question);
            }
            questions = quizQuestionRepository.saveAll(questions);
            quiz.setQuestions(questions);
        }

        quiz.updateQuestionCount();
        quiz = quizRepository.save(quiz);

        log.info("Quiz updated successfully: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Delete quiz (only draft quizzes can be deleted)
     */
    public void deleteQuiz(Long quizId, Long instructorId) {
        log.info("Deleting quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to delete this quiz");
        }

        // Only allow deletion of draft quizzes
        if (quiz.getIsPublished()) {
            throw new IllegalStateException("Cannot delete published quiz");
        }

        // Delete questions first (cascade will handle this, but explicit for clarity)
        quizQuestionRepository.deleteByQuizId(quizId);
        quizRepository.delete(quiz);

        log.info("Quiz deleted successfully: {}", quizId);
    }

    /**
     * Add a question to quiz
     */
    public QuizDTO addQuestion(Long quizId, Long instructorId, QuizQuestionDto questionDTO) {
        log.info("Adding question to quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to modify this quiz");
        }

        // Get next order number
        int nextOrder = quiz.getQuestions().size() + 1;

        QuizQuestion question = QuizQuestion.builder()
                .quiz(quiz)
                .content(questionDTO.getContent())
                .optionA(questionDTO.getOptionA())
                .optionB(questionDTO.getOptionB())
                .optionC(questionDTO.getOptionC())
                .optionD(questionDTO.getOptionD())
                .correctAnswer(QuizQuestion.CorrectAnswer.valueOf(questionDTO.getCorrectAnswer()))
                .explanation(questionDTO.getExplanation())
                .questionOrder(nextOrder)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        question = quizQuestionRepository.save(question);
        quiz.getQuestions().add(question);
        quiz.updateQuestionCount();
        quiz = quizRepository.save(quiz);

        log.info("Question added to quiz: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Remove a question from quiz
     */
    public QuizDTO removeQuestion(Long quizId, Long questionId, Long instructorId) {
        log.info("Removing question {} from quiz {}", questionId, quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to modify this quiz");
        }

        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        if (!question.getQuiz().getId().equals(quizId)) {
            throw new IllegalArgumentException("Question does not belong to this quiz");
        }

        quiz.getQuestions().remove(question);
        quizQuestionRepository.delete(question);

        // Reorder remaining questions
        List<QuizQuestion> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            questions.get(i).setQuestionOrder(i + 1);
        }
        quizQuestionRepository.saveAll(questions);

        quiz.updateQuestionCount();
        quiz = quizRepository.save(quiz);

        log.info("Question removed from quiz: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Publish a quiz
     */
    public QuizDTO publishQuiz(Long quizId, Long instructorId) {
        log.info("Publishing quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to publish this quiz");
        }

        // Validate quiz has at least one question
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalStateException("Cannot publish quiz without questions");
        }

        quiz.publish();
        quiz = quizRepository.save(quiz);

        log.info("Quiz published successfully: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Save quiz as draft
     */
    public QuizDTO saveDraft(Long quizId, Long instructorId) {
        log.info("Saving quiz as draft: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to modify this quiz");
        }

        quiz.saveDraft();
        quiz = quizRepository.save(quiz);

        log.info("Quiz saved as draft: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Get all quizzes for an instructor
     */
    @Transactional(readOnly = true)
    public List<QuizDTO> getQuizzesByInstructor(Long instructorId) {
        log.info("Fetching quizzes for instructor: {}", instructorId);
        List<Quiz> quizzes = quizRepository.findByInstructorId(instructorId);
        return quizzes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get quizzes for specific course by instructor
     */
    @Transactional(readOnly = true)
    public List<QuizDTO> getQuizzesByCourseAndInstructor(Long courseId, Long instructorId) {
        log.info("Fetching quizzes for course {} and instructor {}", courseId, instructorId);
        List<Quiz> quizzes = quizRepository.findByCourseAndInstructor(courseId, instructorId);
        return quizzes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get draft quizzes for instructor
     */
    @Transactional(readOnly = true)
    public List<QuizDTO> getDraftsByInstructor(Long instructorId) {
        log.info("Fetching draft quizzes for instructor: {}", instructorId);
        List<Quiz> quizzes = quizRepository.findDraftsByInstructorId(instructorId);
        return quizzes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get single quiz by ID (for instructor viewing)
     */
    @Transactional(readOnly = true)
    public QuizDTO getQuizById(Long quizId, Long instructorId) {
        log.info("Fetching quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to view this quiz");
        }

        return convertToDTO(quiz);
    }

    /**
     * Get published quiz for student (no authorization check, used for taking quiz)
     */
    @Transactional(readOnly = true)
    public QuizDTO getPublishedQuiz(Long quizId) {
        log.info("Fetching published quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        if (!quiz.getIsPublished()) {
            throw new IllegalArgumentException("Quiz is not published");
        }

        return convertToDTO(quiz);
    }

    /**
     * Reorder questions in a quiz
     */
    public QuizDTO reorderQuestions(Long quizId, Long instructorId, List<Long> questionIds) {
        log.info("Reordering questions for quiz: {}", quizId);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        // Verify authorization
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to modify this quiz");
        }

        for (int i = 0; i < questionIds.size(); i++) {
            QuizQuestion question = quizQuestionRepository.findById(questionIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            question.setQuestionOrder(i + 1);
            quizQuestionRepository.save(question);
        }

        quiz = quizRepository.findById(quizId).orElseThrow();
        log.info("Questions reordered for quiz: {}", quizId);
        return convertToDTO(quiz);
    }

    /**
     * Get published quiz for a given lesson (student view – no correct answers exposed).
     */
    @Transactional(readOnly = true)
    public LessonQuizDto getPublishedQuizForLesson(Long lessonId) {
        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("No quiz found for this lesson"));

        if (!Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new IllegalArgumentException("Quiz is not published yet");
        }

        List<QuizQuestionDto> questions = new ArrayList<>();
        if (quiz.getQuestions() != null) {
            questions = quiz.getQuestions().stream()
                    .map(q -> QuizQuestionDto.builder()
                            .id(q.getId())
                            .content(q.getContent())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            // correctAnswer intentionally omitted for students
                            .explanation(null)
                            .questionOrder(q.getQuestionOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        LessonQuizDto dto = new LessonQuizDto();
        dto.setQuizId(quiz.getId());
        dto.setLessonId(lessonId);
        dto.setTitle(quiz.getTitle());
        dto.setQuestions(questions);
        return dto;
    }

    /**
     * Grade a student's quiz submission and return score + per-question feedback.
     */
    @Transactional
    public QuizSubmitResultDto submitQuiz(QuizSubmitRequest request, Long userId) {
        Quiz quiz = quizRepository.findByLessonId(request.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("No quiz found for this lesson"));

        if (!Boolean.TRUE.equals(quiz.getIsPublished())) {
            throw new IllegalArgumentException("Quiz is not available");
        }

        List<QuizQuestion> questions = quiz.getQuestions();
        int correctCount = 0;

        List<QuizSubmitResultDto.AnswerResult> answerResults = new ArrayList<>();
        for (QuizSubmitRequest.AnswerItem answer : request.getAnswers()) {
            QuizQuestion question = questions.stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .orElse(null);

            if (question == null) continue;

            boolean correct = question.getCorrectAnswer().toString()
                    .equalsIgnoreCase(answer.getSelectedAnswer());
            if (correct) correctCount++;

            QuizSubmitResultDto.AnswerResult ar = new QuizSubmitResultDto.AnswerResult();
            ar.setQuestionId(question.getId());
            ar.setSelectedAnswer(answer.getSelectedAnswer());
            ar.setCorrectAnswer(question.getCorrectAnswer().toString());
            ar.setCorrect(correct);
            answerResults.add(ar);
        }

        int total = questions.size();
        int score = total > 0 ? (int) Math.round((correctCount * 100.0) / total) : 0;

        QuizSubmitResultDto result = new QuizSubmitResultDto();
        result.setTotalQuestions(total);
        result.setCorrectCount(correctCount);
        result.setScore(score);
        result.setAnswerResults(answerResults);
        return result;
    }

    /**
     * Convert Quiz entity to DTO
     */
    private QuizDTO convertToDTO(Quiz quiz) {
        List<QuizQuestionDto> questionDTOs = new ArrayList<>();
        if (quiz.getQuestions() != null) {
            questionDTOs = quiz.getQuestions().stream()
                    .map(q -> QuizQuestionDto.builder()
                            .id(q.getId())
                            .content(q.getContent())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .correctAnswer(q.getCorrectAnswer().toString())
                            .explanation(q.getExplanation())
                            .questionOrder(q.getQuestionOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return QuizDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .courseId(quiz.getCourse().getId())
                .lessonId(quiz.getLesson() != null ? quiz.getLesson().getId() : null)
                .instructorId(quiz.getInstructor().getId())
                .questions(questionDTOs)
                .passingScore(quiz.getPassingScore())
                .isPublished(quiz.getIsPublished())
                .isDraft(quiz.getIsDraft())
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .publishedAt(quiz.getPublishedAt())
                .questionCount(quiz.getQuestionCount())
                .build();
    }
}
