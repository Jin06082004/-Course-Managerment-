// Quiz Builder JavaScript - Dynamic Question Management

let currentQuiz = null;
let currentQuestionIndex = null;
let questions = [];
let courses = [];
let myLessons = [];

// Initialize when page loads
document.addEventListener('DOMContentLoaded', () => {
    initializeQuizBuilder();
});

/**
 * Initialize quiz builder
 */
function initializeQuizBuilder() {
    const urlParams = new URLSearchParams(window.location.search);
    const quizId = urlParams.get('quizId');
    const courseId = urlParams.get('courseId');

    if (quizId) {
        // Edit mode: load courses first, then load the quiz data
        loadInstructorCourses().then(() => loadQuizForEditing(quizId));
    } else if (courseId) {
        // Create mode with pre-selected course: load courses, pre-select, then load lessons
        loadInstructorCourses().then(() => {
            const select = document.getElementById('courseSelect');
            select.value = courseId;
            loadLessonsForCourse();
        });
    } else {
        // Create mode: just load courses
        loadInstructorCourses();
    }
}

/**
 * Load instructor's courses — returns a Promise so callers can chain .then()
 */
function loadInstructorCourses() {
    return fetch('/api/courses/my-courses', {
        headers: { 'Authorization': `Bearer ${getToken()}` }
    })
        .then(response => response.json())
        .then(data => {
            courses = Array.isArray(data) ? data : (data.content || []);
            const courseSelect = document.getElementById('courseSelect');
            courseSelect.innerHTML = '<option value="">-- Select a course --</option>';
            courses.forEach(course => {
                const option = document.createElement('option');
                option.value = course.id;
                option.textContent = course.title;
                courseSelect.appendChild(option);
            });
            if (courses.length === 0) {
                courseSelect.innerHTML = '<option value="">No courses found</option>';
            }
        })
        .catch(error => {
            console.error('Error loading courses:', error);
            showToast('Failed to load courses. Are you logged in?', 'error');
        });
}

/**
 * Load lessons for selected course — returns a Promise so callers can chain .then()
 */
function loadLessonsForCourse() {
    const courseId = document.getElementById('courseSelect').value;
    const lessonSelect = document.getElementById('lessonSelect');
    lessonSelect.innerHTML = '<option value="">None - Quiz for entire course</option>';

    if (!courseId) {
        return Promise.resolve();
    }

    return fetch(`/api/courses/${courseId}/lessons`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
    })
        .then(response => response.json())
        .then(data => {
            myLessons = Array.isArray(data) ? data : (data.content || []);
            myLessons.forEach(lesson => {
                const option = document.createElement('option');
                option.value = lesson.id;
                option.textContent = lesson.title;
                lessonSelect.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error loading lessons:', error);
        });
}

/**
 * Load quiz for editing
 */
function loadQuizForEditing(quizId) {
    return fetch(`/api/instructor/quizzes/${quizId}`, {
        headers: { 'Authorization': `Bearer ${getToken()}` }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentQuiz = data.quiz;
                populateQuizForm(data.quiz);
                renderQuestions(data.quiz.questions || []);
                document.getElementById('pageTitle').textContent = 'Edit Quiz';
                document.getElementById('deleteBtn').style.display = 'block';
            } else {
                showToast(data.error || 'Failed to load quiz', 'error');
            }
        })
        .catch(error => {
            console.error('Error loading quiz:', error);
            showToast('Failed to load quiz', 'error');
        });
}

/**
 * Populate form with quiz data
 * Called after loadInstructorCourses() has already populated courseSelect
 */
function populateQuizForm(quiz) {
    document.getElementById('quizTitle').value = quiz.title || '';
    document.getElementById('quizDescription').value = quiz.description || '';
    document.getElementById('passingScore').value = quiz.passingScore || 70;

    // Pre-select course (options are already loaded at this point)
    const courseSelect = document.getElementById('courseSelect');
    courseSelect.value = quiz.courseId;

    updateQuizStatus(quiz);

    // Load lessons for this course, then pre-select the lesson if any
    loadLessonsForCourse().then(() => {
        if (quiz.lessonId) {
            document.getElementById('lessonSelect').value = quiz.lessonId;
        }
    });
}

/**
 * Add question form
 */
function addQuestionForm() {
    currentQuestionIndex = null;
    document.getElementById('questionModalTitle').textContent = 'Add Question';
    document.getElementById('questionForm').reset();
    document.getElementById('correctAnswer').value = '';

    const modal = new bootstrap.Modal(document.getElementById('questionModal'));
    modal.show();
}

/**
 * Edit question
 */
function editQuestion(index) {
    currentQuestionIndex = index;
    const question = questions[index];

    document.getElementById('questionModalTitle').textContent = `Edit Question ${index + 1}`;
    document.getElementById('questionContent').value = question.content;
    document.getElementById('optionA').value = question.optionA;
    document.getElementById('optionB').value = question.optionB;
    document.getElementById('optionC').value = question.optionC;
    document.getElementById('optionD').value = question.optionD;
    document.getElementById('correctAnswer').value = question.correctAnswer;
    document.getElementById('explanation').value = question.explanation || '';

    const modal = new bootstrap.Modal(document.getElementById('questionModal'));
    modal.show();
}

/**
 * Save question (add or edit)
 */
function saveQuestion() {
    const form = document.getElementById('questionForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const question = {
        content: document.getElementById('questionContent').value,
        optionA: document.getElementById('optionA').value,
        optionB: document.getElementById('optionB').value,
        optionC: document.getElementById('optionC').value,
        optionD: document.getElementById('optionD').value,
        correctAnswer: document.getElementById('correctAnswer').value,
        explanation: document.getElementById('explanation').value
    };

    if (currentQuestionIndex !== null) {
        // Edit existing question
        questions[currentQuestionIndex] = { ...questions[currentQuestionIndex], ...question };
    } else {
        // Add new question
        questions.push(question);
    }

    renderQuestions(questions);
    updateQuestionCount();

    // Close modal
    bootstrap.Modal.getInstance(document.getElementById('questionModal')).hide();
    showToast('Question saved successfully', 'success');
}

/**
 * Render questions in list
 */
function renderQuestions(questionList) {
    const container = document.getElementById('questionsContainer');

    if (!questionList || questionList.length === 0) {
        container.innerHTML = `
            <div class="alert alert-info">
                <i class="fas fa-info-circle"></i> No questions added yet. Click "Add Question" to get started.
            </div>
        `;
        return;
    }

    container.innerHTML = questionList.map((q, index) => `
        <div class="question-card" draggable="true" ondragstart="handleDragStart(event, ${index})" ondrop="handleDrop(event, ${index})" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)">
            <div class="question-header">
                <div class="d-flex align-items-start gap-3" style="flex: 1;">
                    <div class="drag-handle" title="Drag to reorder">
                        <i class="fas fa-grip-vertical"></i>
                    </div>
                    <div class="question-number">${index + 1}</div>
                    <div class="question-title">${escapeHtml(q.content)}</div>
                </div>
                <div class="question-actions">
                    <button type="button" class="btn btn-sm btn-primary" onclick="editQuestion(${index})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button type="button" class="btn btn-sm btn-danger" onclick="deleteQuestion(${index})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </div>

            <div class="question-content">
                ${escapeHtml(q.content)}
            </div>

            <div class="options-grid">
                <div class="option-badge ${q.correctAnswer === 'A' ? 'correct' : ''}">
                    <strong>A:</strong> ${escapeHtml(q.optionA)}
                </div>
                <div class="option-badge ${q.correctAnswer === 'B' ? 'correct' : ''}">
                    <strong>B:</strong> ${escapeHtml(q.optionB)}
                </div>
                <div class="option-badge ${q.correctAnswer === 'C' ? 'correct' : ''}">
                    <strong>C:</strong> ${escapeHtml(q.optionC)}
                </div>
                <div class="option-badge ${q.correctAnswer === 'D' ? 'correct' : ''}">
                    <strong>D:</strong> ${escapeHtml(q.optionD)}
                </div>
            </div>

            <div class="correct-indicator">
                <i class="fas fa-check-circle"></i> Correct Answer: <strong>${q.correctAnswer}</strong>
            </div>

            ${q.explanation ? `<div class="explanation-text"><strong>Explanation:</strong> ${escapeHtml(q.explanation)}</div>` : ''}
        </div>
    `).join('');
}

/**
 * Delete question
 */
function deleteQuestion(index) {
    if (confirm('Are you sure you want to delete this question?')) {
        questions.splice(index, 1);
        renderQuestions(questions);
        updateQuestionCount();
        showToast('Question deleted', 'success');
    }
}

/**
 * Update question count badge
 */
function updateQuestionCount() {
    document.getElementById('questionCount').textContent = questions.length;
    document.getElementById('statQuestionCount').textContent = questions.length;
}

/**
 * Drag and drop handlers
 */
let draggedIndex = null;

function handleDragStart(event, index) {
    draggedIndex = index;
    event.dataTransfer.effectAllowed = 'move';
    event.target.closest('.question-card').style.opacity = '0.5';
}

function handleDragOver(event) {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
    event.target.closest('.question-card').classList.add('drag-over');
}

function handleDragLeave(event) {
    event.target.closest('.question-card').classList.remove('drag-over');
}

function handleDrop(event, index) {
    event.preventDefault();
    event.target.closest('.question-card').classList.remove('drag-over');

    if (draggedIndex !== null && draggedIndex !== index) {
        // Reorder questions
        const draggedQuestion = questions[draggedIndex];
        questions.splice(draggedIndex, 1);
        questions.splice(index, 0, draggedQuestion);
        renderQuestions(questions);
        showToast('Question reordered', 'success');
    }

    draggedIndex = null;
}

/**
 * Publish quiz
 */
function publishQuiz() {
    // Validate form
    const settingsForm = document.getElementById('quizSettingsForm');
    if (!settingsForm.checkValidity()) {
        settingsForm.reportValidity();
        return;
    }

    // Validate questions
    if (questions.length === 0) {
        showToast('Quiz must have at least one question', 'error');
        return;
    }

    const request = buildQuizRequest(false);

    const endpoint = currentQuiz ? `/api/instructor/quizzes/${currentQuiz.id}` : `/api/instructor/quizzes?courseId=${request.courseId}`;
    const method = currentQuiz ? 'PUT' : 'POST';

    fetch(endpoint, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
        },
        body: JSON.stringify(request)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showToast('Quiz published successfully!', 'success');
                setTimeout(() => window.location.href = '/instructor/dashboard', 1500);
            } else {
                showToast(data.error || 'Failed to publish quiz', 'error');
            }
        })
        .catch(error => {
            console.error('Error publishing quiz:', error);
            showToast('Error publishing quiz', 'error');
        });
}

/**
 * Save quiz as draft
 */
function saveDraft() {
    // Validate form
    const settingsForm = document.getElementById('quizSettingsForm');
    if (!settingsForm.checkValidity()) {
        settingsForm.reportValidity();
        return;
    }

    const request = buildQuizRequest(true);

    const endpoint = currentQuiz ? `/api/instructor/quizzes/${currentQuiz.id}` : `/api/instructor/quizzes?courseId=${request.courseId}`;
    const method = currentQuiz ? 'PUT' : 'POST';

    fetch(endpoint, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
        },
        body: JSON.stringify(request)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentQuiz = data.quiz;
                updateQuizStatus(data.quiz);
                showToast('Quiz saved as draft', 'success');
            } else {
                showToast(data.error || 'Failed to save quiz', 'error');
            }
        })
        .catch(error => {
            console.error('Error saving draft:', error);
            showToast('Error saving quiz', 'error');
        });
}

/**
 * Build quiz request payload
 */
function buildQuizRequest(saveAsDraft) {
    return {
        title: document.getElementById('quizTitle').value,
        description: document.getElementById('quizDescription').value,
        courseId: parseInt(document.getElementById('courseSelect').value),
        lessonId: document.getElementById('lessonSelect').value ? parseInt(document.getElementById('lessonSelect').value) : null,
        passingScore: parseInt(document.getElementById('passingScore').value),
        saveAsDraft: saveAsDraft,
        questions: questions
    };
}

/**
 * Update quiz status display
 */
function updateQuizStatus(quiz) {
    const statusBadge = document.getElementById('quizStatus');
    document.getElementById('statPassingScore').textContent = quiz.passingScore + '%';
    document.getElementById('lastModified').textContent = formatDate(quiz.updatedAt);

    if (quiz.isPublished) {
        statusBadge.textContent = 'Published';
        statusBadge.className = 'badge bg-success';
        document.getElementById('publishBtn').textContent = 'Update & Publish';
    } else {
        statusBadge.textContent = 'Draft';
        statusBadge.className = 'badge bg-warning';
    }
}

/**
 * Delete quiz
 */
function confirmDeleteQuiz() {
    if (!currentQuiz) return;

    if (confirm('Are you sure you want to delete this quiz? This action cannot be undone.')) {
        fetch(`/api/instructor/quizzes/${currentQuiz.id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('Quiz deleted successfully', 'success');
                    setTimeout(() => window.location.href = '/instructor/dashboard', 1500);
                } else {
                    showToast(data.error || 'Failed to delete quiz', 'error');
                }
            })
            .catch(error => {
                console.error('Error deleting quiz:', error);
                showToast('Error deleting quiz', 'error');
            });
    }
}

/**
 * Back to dashboard
 */
function goBackToDashboard() {
    window.location.href = '/instructor/dashboard';
}

/**
 * Utility: Escape HTML
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Utility: Format date
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) return 'Just now';
    if (diff < 3600000) return Math.floor(diff / 60000) + 'm ago';
    if (diff < 86400000) return Math.floor(diff / 3600000) + 'h ago';
    if (diff < 604800000) return Math.floor(diff / 86400000) + 'd ago';

    return date.toLocaleDateString();
}

/**
 * Utility: Show toast notification
 */
function showToast(message, type = 'info') {
    // Create toast element
    const toastId = 'toast_' + Date.now();
    const toastHTML = `
        <div id="${toastId}" class="toast align-items-center text-white border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas fa-${type === 'error' ? 'exclamation-circle' : type === 'success' ? 'check-circle' : 'info-circle'}"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    // Add to page if container doesn't exist
    let toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);

        // Add CSS for toast container
        const style = document.createElement('style');
        style.textContent = `
            .toast {
                background: #333 !important;
                padding: 12px 16px;
            }
            .toast.error {
                background: #ef4444 !important;
            }
            .toast.success {
                background: #10b981 !important;
            }
            .toast.info {
                background: #3b82f6 !important;
            }
        `;
        document.head.appendChild(style);
    }

    toastContainer.innerHTML += toastHTML;
    const toastElement = document.getElementById(toastId);
    toastElement.classList.add(type);

    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    // Remove toast after it's hidden
    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

/**
 * ====== AI QUIZ GENERATOR FUNCTIONS ======
 */

/**
 * Show AI Generator Modal
 */
function showAIGeneratorModal() {
    // Check if lesson is selected
    const lessonId = document.getElementById('lessonSelect').value;
    if (!lessonId) {
        showToast('Please select a lesson first', 'warning');
        return;
    }

    // Get lesson name
    const lessonSelect = document.getElementById('lessonSelect');
    const selectedOption = lessonSelect.options[lessonSelect.selectedIndex];
    const lessonName = selectedOption.text;

    // Update lesson reference in modal
    document.getElementById('referenceLessonName').textContent = lessonName;
    document.getElementById('lessonReference').style.display = 'block';

    // Reset form
    document.getElementById('aiNumberOfQuestions').value = 5;
    document.getElementById('aiErrorMessage').style.display = 'none';
    document.getElementById('aiLoadingIndicator').style.display = 'none';

    // Show modal
    const modal = new bootstrap.Modal(document.getElementById('aiGeneratorModal'));
    modal.show();
}

/**
 * Increase number of questions
 */
function increaseQuestions() {
    const input = document.getElementById('aiNumberOfQuestions');
    let value = parseInt(input.value) || 5;
    if (value < 10) {
        input.value = value + 1;
    }
}

/**
 * Decrease number of questions
 */
function decreaseQuestions() {
    const input = document.getElementById('aiNumberOfQuestions');
    let value = parseInt(input.value) || 5;
    if (value > 3) {
        input.value = value - 1;
    }
}

/**
 * Generate quiz with AI
 */
async function generateWithAI() {
    try {
        const lessonId = document.getElementById('lessonSelect').value;
        const numberOfQuestions = parseInt(document.getElementById('aiNumberOfQuestions').value);

        if (!lessonId) {
            showErrorMessage('Please select a lesson');
            return;
        }

        if (!numberOfQuestions || numberOfQuestions < 3 || numberOfQuestions > 10) {
            showErrorMessage('Number of questions must be between 3 and 10');
            return;
        }

        // Show loading
        document.getElementById('aiLoadingIndicator').style.display = 'block';
        document.getElementById('aiErrorMessage').style.display = 'none';
        document.getElementById('aiGenerateBtn').disabled = true;

        // Call API
        const response = await fetch('/api/ai/generate-quiz', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${getToken()}`
            },
            body: JSON.stringify({
                lessonId: parseInt(lessonId),
                numberOfQuestions: numberOfQuestions
            })
        });

        const data = await response.json();

        // Hide loading
        document.getElementById('aiLoadingIndicator').style.display = 'none';
        document.getElementById('aiGenerateBtn').disabled = false;

        if (!data.success) {
            showErrorMessage(data.error || 'Failed to generate questions');
            return;
        }

        // Add generated questions to quiz
        if (data.questions && data.questions.length > 0) {
            addGeneratedQuestions(data.questions);
            
            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('aiGeneratorModal'));
            modal.hide();

            showToast(`Successfully generated ${data.questions.length} questions!`, 'success');
        } else {
            showErrorMessage('No questions were generated');
        }

    } catch (error) {
        console.error('Error generating quiz with AI:', error);
        showErrorMessage('Error: ' + error.message);
        document.getElementById('aiLoadingIndicator').style.display = 'none';
        document.getElementById('aiGenerateBtn').disabled = false;
    }
}

/**
 * Add generated questions to quiz
 */
function addGeneratedQuestions(generatedQuestions) {
    generatedQuestions.forEach((genQuestion, index) => {
        // Create question object
        const question = {
            content: genQuestion.question,
            optionA: genQuestion.options[0] || '',
            optionB: genQuestion.options[1] || '',
            optionC: genQuestion.options[2] || '',
            optionD: genQuestion.options[3] || '',
            correctAnswer: genQuestion.correctAnswer,
            explanation: genQuestion.explanation,
            questionOrder: questions.length + index + 1
        };

        questions.push(question);
    });

    // Update UI
    renderQuestions(questions);
    updateQuestionCount();
    showToast(`Added ${generatedQuestions.length} questions`, 'success');
}

/**
 * Show error message in modal
 */
function showErrorMessage(message) {
    const errorDiv = document.getElementById('aiErrorMessage');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
}

/**
 * Get JWT token from storage — uses the same key as AuthManager ('authToken')
 */
function getToken() {
    return (window.AuthManager && typeof window.AuthManager.getToken === 'function')
        ? window.AuthManager.getToken()
        : (localStorage.getItem('authToken') || '');
}
