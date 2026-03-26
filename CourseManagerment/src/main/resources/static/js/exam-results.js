let resultData = null;
let examData = null;

// Get result ID from URL
const resultId = getResultIdFromUrl();

document.addEventListener('DOMContentLoaded', async () => {
    try {
        if (!resultId) {
            showErrorToast('Result ID not found');
            setTimeout(() => window.location.href = '/courses', 2000);
            return;
        }

        // Load result
        await loadExamResult();

        // Display result
        if (resultData) {
            displayResult();
        }
    } catch (error) {
        console.error('Error loading result:', error);
        showErrorToast('Failed to load exam results');
    }
});

/**
 * Load exam result from API
 */
async function loadExamResult() {
    try {
        const response = await fetch(`/api/exams/results/${resultId}`, {
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
        resultData = result.result || result.data;

        console.log('Result loaded:', resultData);
    } catch (error) {
        console.error('Error loading result:', error);
        throw error;
    }
}

/**
 * Display result on page
 */
function displayResult() {
    if (!resultData) return;

    // Set exam title
    document.getElementById('examTitle').textContent = resultData.exam?.title || 'N/A';

    // Set score display
    const scoreValue = resultData.score || 0;
    const totalMarks = resultData.totalMarks || 100;
    const percentage = resultData.percentage || 0;
    const passed = resultData.passed || false;

    document.getElementById('scoreValue').textContent = scoreValue;
    document.getElementById('scoreTotal').textContent = `/ ${totalMarks}`;
    document.getElementById('percentage').textContent = `${percentage}%`;
    document.getElementById('passingMarks').textContent = resultData.exam?.passingMarks || 0;

    // Update score circle color
    const scoreCircle = document.getElementById('scoreCircle');
    if (passed) {
        scoreCircle.classList.add('passed');
    } else {
        scoreCircle.classList.add('failed');
    }

    // Update status badge
    const statusBadge = document.getElementById('statusBadge');
    const statusText = document.getElementById('statusText');
    if (passed) {
        statusBadge.className = 'status-badge passed';
        statusText.textContent = 'PASSED ✓';
    } else {
        statusBadge.className = 'status-badge failed';
        statusText.textContent = 'FAILED ✗';
    }

    // Set exam details
    const questions = resultData.exam?.questions || [];
    document.getElementById('totalQuestions').textContent = questions.length;

    // Parse answers and calculate statistics
    let correctCount = 0;
    try {
        const answers = JSON.parse(resultData.answersJson || '{}');
        const submissionDetails = resultData.submissionDetails || {};

        questions.forEach((question) => {
            if (submissionDetails[question.id]?.isCorrect) {
                correctCount++;
            }
        });
    } catch (e) {
        console.warn('Could not parse answers', e);
    }

    const wrongCount = questions.length - correctCount;
    document.getElementById('correctAnswers').textContent = correctCount;
    document.getElementById('wrongAnswers').textContent = wrongCount;

    // Format submission date
    const submittedAt = new Date(resultData.submittedAt);
    document.getElementById('submittedAt').textContent = submittedAt.toLocaleString();
}

/**
 * Review answers with full details
 */
async function reviewAnswers() {
    if (!resultData) return;

    // Toggle visibility
    const section = document.getElementById('answersReviewSection');
    if (section.style.display !== 'none') {
        section.style.display = 'none';
        return;
    }

    // Load answers if not already loaded
    const container = document.getElementById('answersContainer');
    if (container.children.length === 0) {
        renderAnswerReview();
    }

    section.style.display = 'block';
    section.scrollIntoView({ behavior: 'smooth' });
}

/**
 * Render answer review
 */
function renderAnswerReview() {
    const container = document.getElementById('answersContainer');
    container.innerHTML = '';

    const questions = resultData.exam?.questions || [];
    
    try {
        const answers = JSON.parse(resultData.answersJson || '{}');
        const submissionDetails = resultData.submissionDetails || {};

        questions.forEach((question, index) => {
            const detail = submissionDetails[question.id] || {};
            const userAnswer = detail.userAnswer || answers[question.id] || '-';
            const correctAnswer = detail.correctAnswer || question.correctAnswer;
            const isCorrect = detail.isCorrect || false;
            const marksObtained = detail.marksObtained || 0;
            const marks = detail.marks || question.marks || 0;

            const card = createAnswerCard(question, index, userAnswer, correctAnswer, isCorrect, marksObtained, marks);
            container.appendChild(card);
        });
    } catch (error) {
        console.error('Error rendering answers:', error);
        container.innerHTML = '<p class="alert alert-warning">Unable to display full answer details</p>';
    }
}

/**
 * Create answer card element
 */
function createAnswerCard(question, index, userAnswer, correctAnswer, isCorrect, marksObtained, marks) {
    const card = document.createElement('div');
    card.className = `answer-card ${isCorrect ? 'correct' : 'incorrect'}`;

    const header = document.createElement('div');
    header.className = 'answer-header';

    const numberBadge = document.createElement('div');
    numberBadge.className = 'question-number-badge';
    numberBadge.textContent = index + 1;

    const badge = document.createElement('div');
    badge.className = `answer-badge ${isCorrect ? 'correct' : 'incorrect'}`;
    badge.innerHTML = `
        <i class="fas fa-${isCorrect ? 'check-circle' : 'times-circle'}"></i>
        ${isCorrect ? 'Correct' : 'Incorrect'}
    `;

    header.appendChild(numberBadge);
    header.appendChild(badge);

    const questionText = document.createElement('div');
    questionText.className = 'question-text';
    questionText.textContent = question.content;

    const comparison = document.createElement('div');
    comparison.className = 'answer-comparison';

    // Your answer
    const yourAnswer = document.createElement('div');
    yourAnswer.className = 'answer-item your-answer';
    yourAnswer.innerHTML = `
        <div class="answer-item-label">Your Answer</div>
        <div class="answer-item-content">
            ${userAnswer}: ${getOptionText(question, userAnswer)}
        </div>
    `;
    comparison.appendChild(yourAnswer);

    // Correct answer (if wrong)
    if (!isCorrect) {
        const correctAnswerDiv = document.createElement('div');
        correctAnswerDiv.className = 'answer-item correct-answer';
        correctAnswerDiv.innerHTML = `
            <div class="answer-item-label">Correct Answer</div>
            <div class="answer-item-content">
                ${correctAnswer}: ${getOptionText(question, correctAnswer)}
            </div>
        `;
        comparison.appendChild(correctAnswerDiv);
    }

    // Marks
    const marksDisplay = document.createElement('div');
    marksDisplay.className = 'marks-display';
    marksDisplay.innerHTML = `
        <i class="fas fa-star"></i>
        <span>${marksObtained} / ${marks} marks</span>
    `;
    comparison.appendChild(marksDisplay);

    const content = document.createElement('div');
    content.appendChild(header);
    content.appendChild(questionText);
    content.appendChild(comparison);

    // Explanation if exists
    if (question.explanation) {
        const explanation = document.createElement('div');
        explanation.className = 'explanation';
        explanation.innerHTML = `
            <div class="explanation-label">
                <i class="fas fa-lightbulb"></i> Explanation
            </div>
            <div class="explanation-text">${question.explanation}</div>
        `;
        content.appendChild(explanation);
    }

    card.appendChild(content);
    return card;
}

/**
 * Get option text from question
 */
function getOptionText(question, option) {
    const optionMap = {
        'A': question.optionA,
        'B': question.optionB,
        'C': question.optionC,
        'D': question.optionD
    };
    return optionMap[option] || 'Not attempted';
}

/**
 * Download results
 */
function downloadResults() {
    const modal = new bootstrap.Modal(document.getElementById('downloadModal'));
    modal.show();
}

/**
 * Download as PDF
 */
function downloadPDF() {
    // Using a simple approach - in production, you might use jsPDF or print-to-PDF
    const printWindow = window.open('', '', 'height=600,width=800');
    printWindow.document.write('<pre>' + generateResultsText() + '</pre>');
    printWindow.document.close();
    printWindow.print();
}

/**
 * Download as JSON
 */
function downloadJSON() {
    const dataStr = JSON.stringify(resultData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `exam-result-${resultId}.json`;
    link.click();
    URL.revokeObjectURL(url);
}

/**
 * Generate results text
 */
function generateResultsText() {
    return `
EXAM RESULTS
============

Exam: ${resultData.exam?.title}
Date: ${new Date(resultData.submittedAt).toLocaleString()}

SCORE
-----
Score: ${resultData.score} / ${resultData.totalMarks}
Percentage: ${resultData.percentage}%
Status: ${resultData.passed ? 'PASSED' : 'FAILED'}

DETAILS
-------
Total Questions: ${resultData.exam?.questions?.length}
Passing Marks: ${resultData.exam?.passingMarks}
    `;
}

/**
 * Get result ID from URL
 */
function getResultIdFromUrl() {
    const url = new URL(window.location.href);
    const pathParts = url.pathname.split('/');
    return pathParts[pathParts.length - 1];
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
