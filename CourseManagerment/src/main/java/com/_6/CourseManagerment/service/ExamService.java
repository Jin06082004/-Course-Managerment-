package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.*;
import com._6.CourseManagerment.entity.*;
import com._6.CourseManagerment.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamResultRepository examResultRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new exam
     */
    public ExamDTO createExam(Long courseId, Long instructorId, CreateExamRequest request) {
        log.info("Creating exam for course: {}, instructor: {}", courseId, instructorId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new IllegalArgumentException("Instructor not found"));

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .course(course)
                .instructor(instructor)
                .duration(request.getDuration())
                .totalMarks(request.getTotalMarks())
                .passingMarks(request.getPassingMarks())
                .randomizeQuestions(request.getRandomizeQuestions() != null ? request.getRandomizeQuestions() : false)
                .showResults(request.getShowResults() != null ? request.getShowResults() : true)
                .isDraft(request.getSaveAsDraft() != null ? request.getSaveAsDraft() : true)
                .isPublished(request.getSaveAsDraft() != null ? !request.getSaveAsDraft() : false)
                .publishedAt(request.getSaveAsDraft() != null && !request.getSaveAsDraft() ? LocalDateTime.now() : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        exam = examRepository.save(exam);

        // Add questions
        List<ExamQuestion> questions = new ArrayList<>();
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                ExamQuestionDTO questionDTO = request.getQuestions().get(i);
                ExamQuestion question = ExamQuestion.builder()
                        .exam(exam)
                        .content(questionDTO.getContent())
                        .optionA(questionDTO.getOptionA())
                        .optionB(questionDTO.getOptionB())
                        .optionC(questionDTO.getOptionC())
                        .optionD(questionDTO.getOptionD())
                        .correctAnswer(ExamQuestion.CorrectAnswer.valueOf(questionDTO.getCorrectAnswer()))
                        .explanation(questionDTO.getExplanation())
                        .marks(questionDTO.getMarks() != null ? questionDTO.getMarks() : 1)
                        .questionOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                questions.add(question);
            }
            questions = examQuestionRepository.saveAll(questions);
        }

        exam.setQuestions(questions);
        exam.updateQuestionCount();
        exam = examRepository.save(exam);

        log.info("Exam created successfully with ID: {}", exam.getId());
        return convertToDTO(exam);
    }

    /**
     * Update existing exam
     */
    public ExamDTO updateExam(Long examId, Long instructorId, CreateExamRequest request) {
        log.info("Updating exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to update this exam");
        }

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setDuration(request.getDuration());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setPassingMarks(request.getPassingMarks());
        exam.setRandomizeQuestions(request.getRandomizeQuestions() != null ? request.getRandomizeQuestions() : false);
        exam.setShowResults(request.getShowResults() != null ? request.getShowResults() : true);
        exam.setUpdatedAt(LocalDateTime.now());

        examQuestionRepository.deleteByExamId(examId);
        exam.getQuestions().clear();

        List<ExamQuestion> questions = new ArrayList<>();
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (int i = 0; i < request.getQuestions().size(); i++) {
                ExamQuestionDTO questionDTO = request.getQuestions().get(i);
                ExamQuestion question = ExamQuestion.builder()
                        .exam(exam)
                        .content(questionDTO.getContent())
                        .optionA(questionDTO.getOptionA())
                        .optionB(questionDTO.getOptionB())
                        .optionC(questionDTO.getOptionC())
                        .optionD(questionDTO.getOptionD())
                        .correctAnswer(ExamQuestion.CorrectAnswer.valueOf(questionDTO.getCorrectAnswer()))
                        .explanation(questionDTO.getExplanation())
                        .marks(questionDTO.getMarks() != null ? questionDTO.getMarks() : 1)
                        .questionOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                questions.add(question);
            }
            questions = examQuestionRepository.saveAll(questions);
            exam.setQuestions(questions);
        }

        exam.updateQuestionCount();
        exam = examRepository.save(exam);

        log.info("Exam updated successfully: {}", examId);
        return convertToDTO(exam);
    }

    /**
     * Delete exam (draft only)
     */
    public void deleteExam(Long examId, Long instructorId) {
        log.info("Deleting exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to delete this exam");
        }

        if (exam.getIsPublished()) {
            throw new IllegalStateException("Cannot delete published exam");
        }

        examQuestionRepository.deleteByExamId(examId);
        examRepository.delete(exam);

        log.info("Exam deleted successfully: {}", examId);
    }

    /**
     * Get exam for student taking exam
     */
    @Transactional(readOnly = true)
    public ExamDTO getExamForTaking(Long examId) {
        log.info("Fetching exam for student: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getIsPublished()) {
            throw new IllegalArgumentException("Exam is not published");
        }

        ExamDTO dto = convertToDTO(exam);

        // Randomize questions if enabled
        if (exam.getRandomizeQuestions() && dto.getQuestions() != null) {
            Collections.shuffle(dto.getQuestions());
        }

        return dto;
    }

    /**
     * Get exam by ID for instructor
     */
    @Transactional(readOnly = true)
    public ExamDTO getExamById(Long examId, Long instructorId) {
        log.info("Fetching exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to view this exam");
        }

        return convertToDTO(exam);
    }

    /**
     * Get all exams for instructor
     */
    @Transactional(readOnly = true)
    public List<ExamDTO> getExamsByInstructor(Long instructorId) {
        log.info("Fetching exams for instructor: {}", instructorId);
        List<Exam> exams = examRepository.findByInstructorId(instructorId);
        return exams.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Get exams by course and instructor
     */
    @Transactional(readOnly = true)
    public List<ExamDTO> getExamsByCourseAndInstructor(Long courseId, Long instructorId) {
        List<Exam> exams = examRepository.findByCourseAndInstructor(courseId, instructorId);
        return exams.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Get published exams for course
     */
    @Transactional(readOnly = true)
    public List<ExamDTO> getPublishedExamsByCourse(Long courseId) {
        List<Exam> exams = examRepository.findPublishedByCourseId(courseId);
        return exams.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Publish exam
     */
    public ExamDTO publishExam(Long examId, Long instructorId) {
        log.info("Publishing exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to publish this exam");
        }

        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
            throw new IllegalStateException("Cannot publish exam without questions");
        }

        exam.publish();
        exam = examRepository.save(exam);

        log.info("Exam published successfully: {}", examId);
        return convertToDTO(exam);
    }

    /**
     * Submit exam answers and calculate score
     */
    public ExamResultDTO submitExam(Long examId, Long userId, SubmitExamRequest request) {
        log.info("Submitting exam: {} by user: {}", examId, userId);

        // Check if already submitted
        Optional<ExamResult> existingResult = examResultRepository.findByUserIdAndExamId(userId, examId);
        if (existingResult.isPresent()) {
            throw new IllegalStateException("You have already submitted this exam");
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getIsPublished()) {
            throw new IllegalArgumentException("Exam is not published");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ExamQuestion> questions = examQuestionRepository.findByExamIdOrderByQuestionOrder(examId);

        // Calculate score
        int totalScore = 0;
        Map<Long, SubmissionDetail> submissionDetails = new HashMap<>();

        for (ExamQuestion question : questions) {
            String userAnswer = request.getAnswers() != null ? request.getAnswers().get(question.getId()) : null;
            String correctAnswer = question.getCorrectAnswer().toString();
            boolean isCorrect = userAnswer != null && userAnswer.equals(correctAnswer);

            if (isCorrect) {
                totalScore += question.getMarks();
            }

            submissionDetails.put(question.getId(), SubmissionDetail.builder()
                    .userAnswer(userAnswer)
                    .correctAnswer(correctAnswer)
                    .isCorrect(isCorrect)
                    .marks(question.getMarks())
                    .marksObtained(isCorrect ? question.getMarks() : 0)
                    .build());
        }

        // Calculate percentage
        int percentage = exam.getTotalMarks() > 0 ? Math.round((totalScore * 100.0f) / exam.getTotalMarks()) : 0;
        boolean passed = percentage >= exam.getPassingMarks();

        // Save result
        ExamResult result = ExamResult.builder()
                .user(user)
                .exam(exam)
                .score(totalScore)
                .totalMarks(exam.getTotalMarks())
                .percentage(percentage)
                .passed(passed)
                .answersJson(convertToJson(submissionDetails))
                .submittedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        result = examResultRepository.save(result);

        log.info("Exam submitted successfully. Score: {}/{}, Percentage: {}%", totalScore, exam.getTotalMarks(), percentage);
        return convertResultToDTO(result, exam);
    }

    /**
     * Get exam result for student
     */
    @Transactional(readOnly = true)
    public ExamResultDTO getExamResult(Long examId, Long userId) {
        ExamResult result = examResultRepository.findByUserIdAndExamId(userId, examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam result not found"));

        Exam exam = examRepository.findById(examId).orElseThrow();
        return convertResultToDTO(result, exam);
    }

    /**
     * Get all results for an exam (instructor view)
     */
    @Transactional(readOnly = true)
    public List<ExamResultDTO> getExamResults(Long examId, Long instructorId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        if (!exam.getInstructor().getId().equals(instructorId)) {
            throw new SecurityException("Not authorized to view exam results");
        }

        List<ExamResult> results = examResultRepository.findByExamIdOrderBySubmittedAtDesc(examId);
        return results.stream().map(r -> convertResultToDTO(r, exam)).collect(Collectors.toList());
    }

    /**
     * Get student's exam results
     */
    @Transactional(readOnly = true)
    public List<ExamResultDTO> getStudentExamResults(Long userId) {
        List<ExamResult> results = examResultRepository.findByUserId(userId);
        return results.stream().map(r -> convertResultToDTO(r, r.getExam())).collect(Collectors.toList());
    }

    /**
     * Get exam result by result ID (with authorization check)
     */
    @Transactional(readOnly = true)
    public ExamResultDTO getExamResultById(Long resultId, Long userId) {
        ExamResult result = examResultRepository.findById(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Exam result not found"));

        // Verify user owns this result
        if (!result.getUser().getId().equals(userId)) {
            throw new SecurityException("Not authorized to view this exam result");
        }

        Exam exam = result.getExam();
        return convertResultToDTO(result, exam);
    }

    /**
     * Convert Exam to ExamDTO
     */
    private ExamDTO convertToDTO(Exam exam) {
        List<ExamQuestionDTO> questionDTOs = new ArrayList<>();
        if (exam.getQuestions() != null) {
            questionDTOs = exam.getQuestions().stream()
                    .map(q -> ExamQuestionDTO.builder()
                            .id(q.getId())
                            .content(q.getContent())
                            .optionA(q.getOptionA())
                            .optionB(q.getOptionB())
                            .optionC(q.getOptionC())
                            .optionD(q.getOptionD())
                            .correctAnswer(q.getCorrectAnswer().toString())
                            .explanation(q.getExplanation())
                            .marks(q.getMarks())
                            .questionOrder(q.getQuestionOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return ExamDTO.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .courseId(exam.getCourse().getId())
                .instructorId(exam.getInstructor().getId())
                .duration(exam.getDuration())
                .totalMarks(exam.getTotalMarks())
                .passingMarks(exam.getPassingMarks())
                .isPublished(exam.getIsPublished())
                .isDraft(exam.getIsDraft())
                .randomizeQuestions(exam.getRandomizeQuestions())
                .showResults(exam.getShowResults())
                .questionCount(exam.getQuestionCount())
                .questions(questionDTOs)
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .publishedAt(exam.getPublishedAt())
                .build();
    }

    /**
     * Convert ExamResult to ExamResultDTO
     */
    private ExamResultDTO convertResultToDTO(ExamResult result, Exam exam) {
        ExamResultDTO dto = ExamResultDTO.builder()
                .id(result.getId())
                .examId(result.getExam().getId())
                .userId(result.getUser().getId())
                .score(result.getScore())
                .totalMarks(result.getTotalMarks())
                .percentage(result.getPercentage())
                .passed(result.getPassed())
                .answersJson(result.getAnswersJson())  // Include raw JSON
                .submittedAt(result.getSubmittedAt())
                .exam(convertToDTO(exam))
                .build();
        
        // Parse submission details for UI rendering
        dto.parseSubmissionDetails();
        
        return dto;
    }

    /**
     * Convert submission details to JSON
     */
    private String convertToJson(Map<Long, SubmissionDetail> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return "{}";
        }
    }

    /**
     * Inner class for submission details
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubmissionDetail {
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private Integer marks;
        private Integer marksObtained;
    }
}
