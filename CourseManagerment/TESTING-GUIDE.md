# Course Management System - Testing Guide

## Phase Completion Status

### ✅ Phase 1: Bug Fixes (100% Complete)
- Fixed 252 compilation errors
- Corrected package names (com._6.CourseManagerment)
- Updated Jakarta EE imports
- Modernized Spring Security 6 configuration
- Maven build: **SUCCESS** (69.79 MB JAR)

### ✅ Phase 2: My Courses Feature (100% Complete)

#### Backend Implementation
1. **Enrollment Entity** (`com._6.CourseManagerment.entity.Enrollment`)
   - ManyToOne relationships to User and Course
   - Progress tracking (0-100%)
   - Status management (ENROLLED, IN_PROGRESS, COMPLETED)
   - Automatic status updates at 100% progress
   - Unique constraint on (user_id, course_id)

2. **EnrollmentRepository** 
   - 11 custom JPA queries for enrollment lookups
   - Pagination support on all queries
   - Methods: findByUser_Id, findByStatus, findActiveEnrollments, findCompletedEnrollments, etc.

3. **EnrollmentService** 
   - 9 business logic methods
   - enrollUserInCourse() - Creates enrollment, updates course student count
   - getMyEnrollments() - Returns paginated user enrollments
   - updateProgress() - Updates progress percentage with auto-completion
   - unenrollUserFromCourse() - Removes enrollment
   - Error handling with descriptive messages

4. **EnrollmentController**
   - 9 REST endpoints
   - Base path: `/api/enrollments`
   - All endpoints require authentication (@PreAuthorize("isAuthenticated()"))
   - HTTP status codes: 201 CREATED, 400 BAD_REQUEST, 404 NOT_FOUND, 500 INTERNAL_SERVER_ERROR

5. **EnrollmentDto**
   - Safe data transfer object
   - Includes course details, instructor name, progress percentage
   - Auto-converts from Enrollment entity

#### Frontend Implementation
1. **my-courses.html Template**
   - Route: `/my-courses`
   - Modern dark-themed design with gradient header
   - Responsive course card grid (300px min width)
   - Tab navigation: All Courses / In Progress / Completed
   - Dynamic API integration with Bearer token authentication
   - Progress bars with percentage display
   - Status badges (completed/in-progress/enrolled)
   - last accessed date display with relative time

2. **JavaScript Functionality**
   - `loadEnrollments()` - Fetches enrollments from API
   - `filterCourses()` - Tab switching with API refetch
   - `renderCourses()` - Dynamic course card rendering
   - `formatDate()` - Relative time formatting (e.g., "5m ago")
   - `continueCourse()` - Navigate to learning page

---

## API Testing Guide

### Authentication
All endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer {jwt_token}
```

The JWT token is stored in localStorage as `auth_token` after login.

### Enrollment Endpoints

#### 1. Get My Enrollments
```
GET /api/enrollments/my-courses?page=0&size=10&sortBy=enrollmentDate&direction=DESC
```
**Response (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "userId": 1,
      "courseId": 1,
      "courseTitle": "Advanced JavaScript",
      "courseThumbnail": "url_to_thumbnail",
      "courseLevel": "Advanced",
      "progressPercentage": 45.5,
      "status": "IN_PROGRESS",
      "instructorName": "John Doe",
      "enrollmentDate": "2024-01-15T10:30:00",
      "lastAccessedDate": "2024-01-20T14:20:00",
      "completionDate": null,
      "totalLessons": 20
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

#### 2. Get Active Enrollments
```
GET /api/enrollments/active?page=0&size=10
```
Returns only ENROLLED or IN_PROGRESS courses, sorted by lastAccessedDate DESC.

#### 3. Get Completed Enrollments
```
GET /api/enrollments/completed?page=0&size=10
```
Returns only COMPLETED courses, sorted by completionDate DESC.

#### 4. Enroll in Course
```
POST /api/enrollments/enroll/{courseId}
```
**Response (201 CREATED):**
```json
{
  "id": 5,
  "userId": 1,
  "courseId": 3,
  "courseTitle": "React Mastery",
  "courseThumbnail": "url",
  "courseLevel": "Intermediate",
  "progressPercentage": 0.0,
  "status": "ENROLLED",
  "instructorName": "Jane Smith",
  "enrollmentDate": "2024-01-20T15:00:00",
  "lastAccessedDate": "2024-01-20T15:00:00",
  "completionDate": null,
  "totalLessons": 25
}
```

**Error Response (400 BAD_REQUEST):**
```json
{
  "error": "User already enrolled in this course"
}
```

#### 5. Check if Enrolled
```
GET /api/enrollments/check/{courseId}
```
**Response (200 OK):**
```json
{
  "enrolled": true,
  "courseId": 1
}
```

#### 6. Update Progress
```
PUT /api/enrollments/{enrollmentId}/progress?progressPercentage=75.0
```
**Response (200 OK):**
```json
{
  "id": 1,
  "progressPercentage": 75.0,
  "status": "IN_PROGRESS",
  ...
}
```
Note: When progressPercentage >= 100, status automatically becomes COMPLETED.

#### 7. Complete Enrollment
```
PUT /api/enrollments/{enrollmentId}/complete
```
Sets progress to 100% and status to COMPLETED.

#### 8. Unenroll from Course
```
DELETE /api/enrollments/{courseId}/unenroll
```
**Response (200 OK):**
```json
{
  "message": "Successfully unenrolled from course"
}
```

#### 9. Get Course Enrollments (Admin/Instructor)
```
GET /api/enrollments/course/{courseId}?page=0&size=20
```
Returns all enrollments for a specific course. Requires ADMIN or INSTRUCTOR role.

---

## Testing Workflow

### 1. User Registration & Login
1. Navigate to `/register`
2. Create a test account
3. Login with credentials
4. JWT token stored in localStorage

### 2. Browse Courses
1. Navigate to `/courses`
2. View available courses
3. Click "View Course Details"

### 3. Enroll in Course
1. On course detail page, click "Enroll Now"
2. Backend creates enrollment record
3. Redirects to `/my-courses` after 1.5 seconds
4. Course appears in "All Courses" tab

### 4. View My Courses
1. Navigate to `/my-courses`
2. See all enrolled courses in card grid
3. Switch between tabs:
   - **All Courses**: Shows all enrollments
   - **In Progress**: Shows active courses
   - **Completed**: Shows finished courses

### 5. Track Progress
1. Backend updates progress via API: `PUT /api/enrollments/{id}/progress`
2. Progress bar updates dynamically
3. Status badges change based on progress:
   - 0-99%: "In Progress" (orange)
   - 100%: "Completed" (green)
   - else: "Enrolled" (gray)

### 6. Complete Course
1. Click "View Course Details" on completed course
2. Enrollment status shows "Completed"
3. Completion date is set
4. Course moves to "Completed" tab

---

## Database Schema

### Enrollment Table
```sql
CREATE TABLE enrollments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE KEY(user_id, course_id),
  course_id BIGINT NOT NULL,
  progress_percentage FLOAT DEFAULT 0.0,
  status VARCHAR(50) DEFAULT 'ENROLLED',
  enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completion_date TIMESTAMP NULL,
  last_accessed_date TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (course_id) REFERENCES courses(id)
);
```

---

## Security

- **Authentication**: JWT tokens required for all endpoints
- **Authorization**: 
  - Student endpoints: `@PreAuthorize("isAuthenticated()")`
  - Admin endpoints: `@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")`
- **CORS**: Enabled for frontend requests
- **CSRF**: Disabled for API endpoints

---

## Common Error Scenarios

### "User not found"
- Check if you're logged in
- Verify JWT token in localStorage
- Check user record exists in database

### "Enrollment not found"
- Verify enrollmentId is correct
- Check if enrollment record exists

### "User already enrolled in this course"
- User is already taking this course
- Call `GET /api/enrollments/check/{courseId}` to verify
- Unenroll first if needed

### "Course not found"
- Verify courseId parameter is correct
- Check if course exists in database

---

## Performance Considerations

1. **Pagination**: All list endpoints support pagination (default: page=0, size=10)
2. **Sorting**: Supports sort by enrollmentDate, lastAccessedDate, completionDate
3. **Caching**: Consider caching user enrollments client-side
4. **API Rate Limiting**: Not implemented (add if needed)

---

## Next Features to Implement

1. **Course Learning Page** (/learn/{courseId})
   - Video player integration
   - Lesson list sidebar
   - Progress save on lesson completion
   - Estimated: 15-20 file changes

2. **Profile Page** (/profile)
   - User info display
   - Avatar upload
   - Edit profile form
   - Estimated: 8-10 file changes

3. **Payment System** (/payment/{courseId})
   - Payment integration
   - Receipt generation
   - Estimated: 10-15 file changes

4. **Admin Dashboard** (/admin)
   - User management
   - Course management
   - Category management
   - Estimated: 20-25 file changes

5. **Notification System**
   - Real-time notifications
   - Email notifications
   - Estimated: 12-15 file changes

---

## Debug Tips

### Check Enrollments in Database
```sql
SELECT * FROM enrollments WHERE user_id = 1;
SELECT * FROM enrollments WHERE course_id = 1;
SELECT COUNT(*) FROM enrollments WHERE status = 'COMPLETED';
```

### Monitor API Calls
1. Open browser DevTools (F12)
2. Go to Network tab
3. Filter by XHR requests
4. Check headers and payload
5. Verify response status codes

### Check JWT Token
```javascript
// In browser console:
localStorage.getItem('auth_token')
```

### Enable Debug Logging
Add to `application.properties`:
```properties
logging.level.com.g6.CourseManagerment=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## Build & Run

### Build with Maven
```bash
.\mvnw.cmd clean package -DskipTests
```

### Run Spring Boot Application
```bash
.\mvnw.cmd spring-boot:run
```

### Access Application
- **Frontend**: http://localhost:8080
- **API Base**: http://localhost:8080/api
- **Database**: MySQL on localhost:3306

---

**Last Updated**: 2024
**Status**: Ready for production testing
