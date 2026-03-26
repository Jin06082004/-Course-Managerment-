package com._6.CourseManagerment.service;

import com._6.CourseManagerment.dto.LessonQuizDto;
import com._6.CourseManagerment.dto.QuizQuestionDto;
import com._6.CourseManagerment.dto.QuizSubmitRequest;
import com._6.CourseManagerment.dto.QuizSubmitResultDto;
import com._6.CourseManagerment.entity.Question;
import com._6.CourseManagerment.entity.Quiz;
import com._6.CourseManagerment.entity.UserAnswer;
import com._6.CourseManagerment.repository.EnrollmentRepository;
import com._6.CourseManagerment.repository.QuestionRepository;
import com._6.CourseManagerment.repository.QuizRepository;
import com._6.CourseManagerment.repository.UserAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    public LessonQuizDto getLessonQuiz(Long lessonId, Long userId) {
        validatePurchasedAccess(lessonId, userId);

        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found for this lesson"));

        List<Question> questions = questionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        List<QuizQuestionDto> questionDtos = questions.stream().map(question -> {
            QuizQuestionDto dto = new QuizQuestionDto();
            dto.setId(question.getId());
            dto.setContent(question.getContent());
            dto.setOptions(question.getOptions());
            return dto;
        }).toList();

        LessonQuizDto response = new LessonQuizDto();
        response.setQuizId(quiz.getId());
        response.setLessonId(quiz.getLessonId());
        response.setTitle(quiz.getTitle());
        response.setQuestions(questionDtos);
        return response;
    }

    public QuizSubmitResultDto submitQuiz(QuizSubmitRequest request, Long userId) {
        validatePurchasedAccess(request.getLessonId(), userId);

        Quiz quiz = quizRepository.findByLessonId(request.getLessonId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found for this lesson"));

        List<Question> questions = questionRepository.findByQuiz_IdOrderByIdAsc(quiz.getId());
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz has no questions");
        }

        Map<Long, String> selectedByQuestion = new HashMap<>();
        if (request.getAnswers() != null) {
            for (QuizSubmitRequest.AnswerItem answer : request.getAnswers()) {
                if (answer.getQuestionId() != null) {
                    selectedByQuestion.put(answer.getQuestionId(), answer.getSelectedAnswer());
                }
            }
        }

        List<UserAnswer> persistedAnswers = new ArrayList<>();
        List<QuizSubmitResultDto.AnswerResult> answerResults = new ArrayList<>();
        int correctCount = 0;

        for (Question question : questions) {
            String selected = selectedByQuestion.get(question.getId());
            boolean isCorrect = selected != null && selected.equals(question.getCorrectAnswer());
            if (isCorrect) {
                correctCount++;
            }

            if (selected != null && !selected.isBlank()) {
                UserAnswer userAnswer = new UserAnswer();
                userAnswer.setUserId(userId);
                userAnswer.setQuestionId(question.getId());
                userAnswer.setSelectedAnswer(selected);
                persistedAnswers.add(userAnswer);
            }

            QuizSubmitResultDto.AnswerResult result = new QuizSubmitResultDto.AnswerResult();
            result.setQuestionId(question.getId());
            result.setSelectedAnswer(selected);
            result.setCorrectAnswer(question.getCorrectAnswer());
            result.setCorrect(isCorrect);
            answerResults.add(result);
        }

        if (!persistedAnswers.isEmpty()) {
            userAnswerRepository.saveAll(persistedAnswers);
        }

        int total = questions.size();
        int score = (int) Math.round((correctCount * 100.0) / total);

        QuizSubmitResultDto response = new QuizSubmitResultDto();
        response.setScore(score);
        response.setTotalQuestions(total);
        response.setCorrectCount(correctCount);
        response.setAnswerResults(answerResults);
        return response;
    }

    private void validatePurchasedAccess(Long lessonId, Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean hasPurchased = enrollmentRepository.existsPurchasedAccess(userId, lessonId);
        if (!hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must purchase this lesson to access quiz");
        }
    }
}
