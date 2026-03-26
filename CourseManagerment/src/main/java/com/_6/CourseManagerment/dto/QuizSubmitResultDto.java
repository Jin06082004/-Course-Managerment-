package com._6.CourseManagerment.dto;

import java.util.List;

public class QuizSubmitResultDto {

    private int score;
    private int totalQuestions;
    private int correctCount;
    private List<AnswerResult> answerResults;

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public List<AnswerResult> getAnswerResults() {
        return answerResults;
    }

    public void setAnswerResults(List<AnswerResult> answerResults) {
        this.answerResults = answerResults;
    }

    public static class AnswerResult {
        private Long questionId;
        private String selectedAnswer;
        private String correctAnswer;
        private boolean correct;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public String getSelectedAnswer() {
            return selectedAnswer;
        }

        public void setSelectedAnswer(String selectedAnswer) {
            this.selectedAnswer = selectedAnswer;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }
    }
}
