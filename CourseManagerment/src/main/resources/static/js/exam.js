let examData = null;
let currentQuestionIndex = 0;
let timeRemaining = 0;
let timerInterval = null;
let userAnswers = {};
let totalTimeTaken = 0;
let examStartTime = null;
let isAutoSubmitting = false;

// Get exam ID from URL
const examId = getExamIdFromUrl();

document.addEventListener('DOMContentLoaded', async () => {
    try {
        if (!examId) {
            showErrorToast('Exam ID not found');
            setTimeout(() => window.location.href = '/courses', 2000);
            return;
        }

        // Load exam data
        await loadExam();
        
        // Initialize exam
        if (examData) {
            initializeExam();
            renderQuestions();
            startTimer();
            
            // Save to localStorage for recovery
            saveExamProgress();
            
            // Auto-save progress every 10 seconds
            setInterval(saveExamProgress, 10000);
        }
    } catch (error) {
        console.error('Error initializing exam:', error);
        showErrorToast('Failed to load exam');
    }
});

/**
 * Load exam from API
 */
async function loadExam() {
    try {
        const response = await fetch(`/api/exams/${examId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        examData = result.data;
        
        console.log('Exam loaded:', examData);
    } catch (error) {
        console.error('Error loading exam:', error);
        throw error;
    }
}

/**
 * Initialize exam UI
 */
function initializeExam() {
    if (!examData) return;

    // Set header information
    document.getElementById('examTitle').textContent = examData.title;
    document.getElementById('totalQuestions').textContent = examData.questions?.length || 0;
    document.getElementById('totalMarks').textContent = examData.totalMarks || 0;
    document.getElementById('passingMarks').textContent = examData.passingMarks || 0;
    document.getElementById('duration').textContent = examData.duration || 0;

    // Show random note if applicable
    if (examData.randomizeQuestions) {
        document.getElementById('randomNote').style.display = 'block';
    }

    // Initialize timer
    timeRemaining = (examData.duration || 60) * 60; // Convert minutes to seconds
    examStartTime = Date.now();

    // Initialize user answers from localStorage if resuming
    const savedAnswers = localStorage.getItem(`exam_answers_${examId}`);
    if (savedAnswers) {
        userAnswers = JSON.parse(savedAnswers);
    }
}

/**
 * Render all questions
 */
function renderQuestions() {
    if (!examData || !examData.questions) return;

    const questionsPanel = document.getElementById('questionsPanel');
    questionsPanel.innerHTML = '';

    examData.questions.forEach((question, index) => {
        const questionDiv = createQuestionElement(question, index);
        questionsPanel.appendChild(questionDiv);
    });

    // Render question navigator grid
    renderQuestionNavigator();
}

/**
 * Create question element
 */
function createQuestionElement(question, index) {
    const div = document.createElement('div');
    div.className = 'question-card';
    div.id = `question-${index}`;

    const questionNumber = document.createElement('div');
    questionNumber.className = 'question-number';
    questionNumber.textContent = index + 1;

    const questionText = document.createElement('div');
    questionText.className = 'question-text';
    questionText.textContent = question.content;

    const optionsDiv = document.createElement('div');
    optionsDiv.className = 'options';

    ['A', 'B', 'C', 'D'].forEach(option => {
        const optionValue = question[`option${option}`];
        
        const label = document.createElement('label');
        label.className = 'option-label';

        const input = document.createElement('input');
        input.type = 'radio';
        input.name = `question_${question.id}`;
        input.value = option;
        input.id = `q${question.id}_${option}`;
        
        // Check if this option was previously selected
        if (userAnswers[question.id] === option) {
            input.checked = true;
        }

        input.addEventListener('change', () => {
            userAnswers[question.id] = option;
            updateQuestionNavigator();
            saveExamProgress();
        });

        const span = document.createElement('span');
        span.className = 'option-text';
        span.textContent = `${option}. ${optionValue}`;

        label.appendChild(input);
        label.appendChild(span);
        optionsDiv.appendChild(label);
    });

    div.appendChild(questionNumber);
    div.appendChild(questionText);
    div.appendChild(optionsDiv);

    return div;
}

/**
 * Render question navigator grid
 */
function renderQuestionNavigator() {
    const grid = document.getElementById('questionsGrid');
    grid.innerHTML = '';

    if (!examData || !examData.questions) return;

    examData.questions.forEach((question, index) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'question-btn';
        btn.textContent = index + 1;
        btn.id = `nav_q${index}`;

        // Update status
        updateQuestionButtonStatus(btn, question.id, index);

        btn.addEventListener('click', () => {
            currentQuestionIndex = index;
            scrollToQuestion(index);
            updateAllQuestionButtons();
        });

        grid.appendChild(btn);
    });

    // Highlight first question
    document.getElementById('nav_q0').classList.add('current');
}

/**
 * Update question button status
 */
function updateQuestionButtonStatus(btn, questionId, index) {
    btn.classList.remove('answered', 'unanswered', 'current');

    if (index === currentQuestionIndex) {
        btn.classList.add('current');
    } else if (userAnswers[questionId]) {
        btn.classList.add('answered');
    } else {
        btn.classList.add('unanswered');
    }
}

/**
 * Update all question buttons
 */
function updateAllQuestionButtons() {
    if (!examData || !examData.questions) return;

    examData.questions.forEach((question, index) => {
        const btn = document.getElementById(`nav_q${index}`);
        if (btn) {
            updateQuestionButtonStatus(btn, question.id, index);
        }
    });
}

/**
 * Update question navigator
 */
function updateQuestionNavigator() {
    const answeredCount = Object.keys(userAnswers).length;
    document.getElementById('answeredCount').textContent = answeredCount;
    document.getElementById('confirmAnsweredQ').textContent = answeredCount;

    updateAllQuestionButtons();
}

/**
 * Scroll to question
 */
function scrollToQuestion(index) {
    const element = document.getElementById(`question-${index}`);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

/**
 * Start timer countdown
 */
function startTimer() {
    updateTimerDisplay();

    timerInterval = setInterval(() => {
        timeRemaining--;

        updateTimerDisplay();

        // Warning at 5 minutes
        if (timeRemaining === 300) {
            showInfoToast('5 minutes remaining!', 5000);
        }

        // Warning at 1 minute
        if (timeRemaining === 60) {
            showWarningToast('Only 1 minute remaining!', 5000);
        }

        // Warning at 30 seconds
        if (timeRemaining === 30) {
            showWarningToast('30 seconds remaining!', 5000);
        }

        // Time's up - auto-submit
        if (timeRemaining <= 0) {
            clearInterval(timerInterval);
            autoSubmitExam();
        }
    }, 1000);
}

/**
 * Update timer display
 */
function updateTimerDisplay() {
    const minutes = Math.floor(timeRemaining / 60);
    const seconds = timeRemaining % 60;
    const timeText = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

    document.getElementById('timerText').textContent = timeText;

    const timerDisplay = document.getElementById('timerDisplay');

    // Change color based on remaining time
    if (timeRemaining <= 60) {
        timerDisplay.classList.add('danger');
    } else if (timeRemaining <= 300) {
        timerDisplay.classList.remove('danger');
        timerDisplay.style.color = '#f59e0b'; // Warning yellow
    } else {
        timerDisplay.classList.remove('danger');
        timerDisplay.style.color = 'white';
    }
}

/**
 * Confirm submission
 */
function confirmSubmit() {
    const modal = new bootstrap.Modal(document.getElementById('submitConfirmModal'));

    // Update confirmation details
    document.getElementById('confirmTotalQ').textContent = examData.questions?.length || 0;
    document.getElementById('confirmAnsweredQ').textContent = Object.keys(userAnswers).length;

    const timeTaken = Math.floor((Date.now() - examStartTime) / 1000);
    const minutes = Math.floor(timeTaken / 60);
    const seconds = timeTaken % 60;
    document.getElementById('confirmTimeTaken').textContent = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

    modal.show();
}

/**
 * Submit exam
 */
async function submitExam() {
    if (isAutoSubmitting) return; // Prevent double submission
    isAutoSubmitting = true;

    try {
        clearInterval(timerInterval);

        // Prepare submission
        const payload = {
            examId: parseInt(examId),
            answers: userAnswers,
            submittedInSeconds: Math.floor((Date.now() - examStartTime) / 1000)
        };

        console.log('Submitting exam:', payload);

        const response = await fetch('/api/exams/submit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Failed to submit exam');
        }

        const result = await response.json();
        const resultId = result.data.id;

        // Clear localStorage
        localStorage.removeItem(`exam_answers_${examId}`);
        
        // Close modal if open
        const modal = bootstrap.Modal.getInstance(document.getElementById('submitConfirmModal'));
        if (modal) modal.hide();

        // Show success and redirect
        showSuccessToast('Exam submitted successfully!', 3000);
        setTimeout(() => {
            window.location.href = `/exam/results/${resultId}`;
        }, 1500);

    } catch (error) {
        console.error('Error submitting exam:', error);
        showErrorToast(error.message || 'Failed to submit exam');
        isAutoSubmitting = false;
    }
}

/**
 * Auto-submit when time ends
 */
async function autoSubmitExam() {
    isAutoSubmitting = true;
    
    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('autoSubmitModal'), {
        backdrop: 'static',
        keyboard: false
    });
    modal.show();

    // Some delay to ensure user sees the message
    setTimeout(() => {
        submitExam();
    }, 1000);
}

/**
 * Save exam progress to localStorage
 */
function saveExamProgress() {
    localStorage.setItem(`exam_answers_${examId}`, JSON.stringify(userAnswers));
    localStorage.setItem(`exam_time_${examId}`, Date.now().toString());
}

/**
 * Get exam ID from URL
 */
function getExamIdFromUrl() {
    const url = new URL(window.location.href);
    return url.searchParams.get('examId');
}

/**
 * Get JWT token
 */
function getToken() {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
}

/**
 * Toast notifications
 */
function showSuccessToast(message, duration = 3000) {
    const toast = document.createElement('div');
    toast.className = 'alert alert-success alert-dismissible fade show';
    toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    toast.innerHTML = `
        <i class="fas fa-check-circle"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), duration);
}

function showErrorToast(message, duration = 3000) {
    const toast = document.createElement('div');
    toast.className = 'alert alert-danger alert-dismissible fade show';
    toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    toast.innerHTML = `
        <i class="fas fa-exclamation-circle"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), duration);
}

function showWarningToast(message, duration = 3000) {
    const toast = document.createElement('div');
    toast.className = 'alert alert-warning alert-dismissible fade show';
    toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    toast.innerHTML = `
        <i class="fas fa-warning"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), duration);
}

function showInfoToast(message, duration = 3000) {
    const toast = document.createElement('div');
    toast.className = 'alert alert-info alert-dismissible fade show';
    toast.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    toast.innerHTML = `
        <i class="fas fa-info-circle"></i> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), duration);
}

// Prevent accidental page exit
window.addEventListener('beforeunload', (event) => {
    if (Object.keys(userAnswers).length > 0 && !isAutoSubmitting) {
        event.preventDefault();
        event.returnValue = '';
    }
});
