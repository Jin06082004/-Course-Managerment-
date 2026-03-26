// Summary Feature Functionality

let summaryState = {
    currentSummary: null,
    isLoading: false,
    lessonId: null,
    token: null
};

// Initialize Summary Feature
function initSummary() {
    summaryState.token = localStorage.getItem('token');
    summaryState.lessonId = getLessonIdFromUrl();
}

/** Get lesson ID from URL */
function getLessonIdFromUrlForSummary() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('lessonId') || window.activeLessonId;
}

/** Generate and show summary */
function generateSummary() {
    if (!summaryState.lessonId || summaryState.isLoading) {
        return;
    }

    const modal = document.getElementById('summaryModal');
    const loading = document.getElementById('summaryLoading');
    const error = document.getElementById('summaryError');
    const content = document.getElementById('summaryContent');

    // Reset states
    loading.classList.remove('hidden');
    error.classList.add('hidden');
    content.classList.add('hidden');

    // Show modal
    modal.classList.remove('hidden');
    summaryState.isLoading = true;

    // Fetch summary from API
    fetch(`/api/ai/summary/${summaryState.lessonId}`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${summaryState.token}`,
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.summary) {
            summaryState.currentSummary = data.summary;
            displaySummary(data.summary, data.message);
        } else {
            showSummaryError(data.error || "Failed to generate summary");
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showSummaryError('Error: Unable to connect to summary service');
    })
    .finally(() => {
        summaryState.isLoading = false;
    });
}

/** Display summary in modal */
function displaySummary(summary, message) {
    const loading = document.getElementById('summaryLoading');
    const error = document.getElementById('summaryError');
    const content = document.getElementById('summaryContent');

    // Hide loading and error
    loading.classList.add('hidden');
    error.classList.add('hidden');

    // Display summary text
    const summaryText = document.getElementById('summaryText');
    summaryText.textContent = summary.content || 'No summary available';

    // Display key points
    const keypointsList = document.getElementById('summaryKeypoints');
    keypointsList.innerHTML = '';

    if (summary.keyPoints && summary.keyPoints.length > 0) {
        summary.keyPoints.forEach(point => {
            const li = document.createElement('li');
            li.textContent = point;
            keypointsList.appendChild(li);
        });
    } else {
        const li = document.createElement('li');
        li.textContent = 'No key points available';
        keypointsList.appendChild(li);
    }

    // Display metadata
    const cacheStatus = summary.isFromCache ? '(from cache)' : '(newly generated)';
    const generatedTime = new Date(summary.createdAt).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    
    document.getElementById('summaryGeneratedTime').textContent = `Generated: ${generatedTime} ${cacheStatus}`;
    document.getElementById('summaryViewCount').textContent = `Views: ${summary.viewCount || 0}`;

    // Show content
    content.classList.remove('hidden');
}

/** Show error message */
function showSummaryError(errorMessage) {
    const loading = document.getElementById('summaryLoading');
    const error = document.getElementById('summaryError');
    const errorText = document.getElementById('summaryErrorText');

    loading.classList.add('hidden');
    errorText.textContent = errorMessage;
    error.classList.remove('hidden');
}

/** Close summary modal */
function closeSummaryModal() {
    const modal = document.getElementById('summaryModal');
    modal.classList.add('hidden');
}

/** Copy summary to clipboard */
function copySummaryToClipboard() {
    if (!summaryState.currentSummary) return;

    const summary = summaryState.currentSummary;
    
    let text = `Lesson Summary\n`;
    text += `${'='.repeat(50)}\n\n`;
    text += `${summary.content}\n\n`;
    text += `Key Points:\n`;
    text += `-${'='.repeat(48)}\n`;
    
    if (summary.keyPoints && summary.keyPoints.length > 0) {
        summary.keyPoints.forEach((point, index) => {
            text += `${index + 1}. ${point}\n`;
        });
    }

    // Copy to clipboard
    navigator.clipboard.writeText(text).then(() => {
        showToast('Summary copied to clipboard');
    }).catch(err => {
        console.error('Failed to copy:', err);
        showToast('Failed to copy summary', 'error');
    });
}

/** Download summary as text file */
function downloadSummary() {
    if (!summaryState.currentSummary) return;

    const summary = summaryState.currentSummary;
    
    let text = `Lesson Summary\n`;
    text += `${'='.repeat(50)}\n\n`;
    text += `${summary.content}\n\n`;
    text += `Key Points:\n`;
    text += `-${'='.repeat(48)}\n`;
    
    if (summary.keyPoints && summary.keyPoints.length > 0) {
        summary.keyPoints.forEach((point, index) => {
            text += `${index + 1}. ${point}\n`;
        });
    }

    // Create download link
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', `lesson-summary-${summaryState.lessonId}.txt`);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);

    showToast('Summary downloaded');
}

/** Initialize when DOM is ready */
document.addEventListener('DOMContentLoaded', function() {
    initSummary();
});

// Export functions
window.Summary = {
    generate: generateSummary,
    close: closeSummaryModal,
    copy: copySummaryToClipboard,
    download: downloadSummary
};
