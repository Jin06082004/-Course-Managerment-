# Course Management System (LMS) - Authentication Module

## 📋 Implementation Summary

This is the initial phase of the Course Management System focusing on **User Authentication, JWT-based security, and Role-based Authorization**.

### ✅ Completed Components

#### 1. **Entity Models**
- **User Entity** (`User.java`)
  - Fields: id, username, email, password, fullName, avatar, provider, status, createdAt, updatedAt
  - Relationships: ManyToMany with Role
  - DateTime tracking with @PrePersist/@PreUpdate

- **Role Entity** (`Role.java`)
  - Fields: id, name
  - Default roles: ADMIN, INSTRUCTOR, STUDENT

#### 2. **DTOs (Data Transfer Objects)**
- **RegisterRequest** - User registration form data with validation
- **LoginRequest** - User login credentials
- **AuthResponse** - Response after successful login/register (includes JWT token)
- **AuthResponse.UserDto** - Safe user information (no sensitive data)

#### 3. **Security Configuration**
- **JwtTokenProvider** (`JwtTokenProvider.java`)
  - Generate JWT tokens from UserDetails
  - Validate tokens
  - Extract claims (username, roles)
  - Token expiration handling

- **JwtAuthenticationFilter** (`JwtAuthenticationFilter.java`)
  - Intercepts requests
  - Extracts JWT from Authorization header
  - Sets authentication in SecurityContext

- **CustomUserDetailsService** (`CustomUserDetailsService.java`)
  - Loads user from database
  - Converts roles to GrantedAuthority
  - Enables database-driven authentication

- **SecurityConfig** (`SecurityConfig.java`)
  - Password encoding with BCrypt
  - Stateless session management
  - CSRF disabled (JWT doesn't need it)
  - Public endpoints: /api/auth/register, /api/auth/login
  - Protected endpoints: require JWT token

#### 4. **Services**
- **AuthService** (`AuthService.java`)
  - User registration logic with validation
  - Login authentication
  - Password encoding
  - Email notification on registration
  - JWT token generation

- **EmailService** (`EmailService.java`)
  - Send registration success email
  - Send payment confirmation email
  - Send password reset OTP email
  - HTML email templates

#### 5. **Controllers**
- **AuthController** (`AuthController.java`)
  - POST `/api/auth/register` - User registration
  - POST `/api/auth/login` - User login
  - GET `/api/auth/profile` - Protected endpoint example

- **HomeController** (`HomeController.java`)
  - GET `/` - Home page (Thymeleaf)

#### 6. **Database**
- **User Repository** - Custom queries for finding users
- **Role Repository** - Role management
- **DataInitializer** - Auto-creates default roles on startup

#### 7. **UI Template**
- **index.html** - Beautiful Thymeleaf home page with registration/login links

#### 8. **Configuration**
- **WebConfig** - CORS configuration
- **GlobalExceptionHandler** - Centralized error handling

---

## 🚀 Testing the APIs

### 1. **Register a New User**

**Endpoint:** `POST http://localhost:8080/api/auth/register`

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "role": "STUDENT"
}
```

**Success Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "avatar": null,
    "status": "ACTIVE",
    "createdAt": "2024-03-22T10:30:00",
    "roles": ["STUDENT"]
  }
}
```

---

### 2. **Login User**

**Endpoint:** `POST http://localhost:8080/api/auth/login`

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Success Response (200 OK):**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "avatar": null,
    "status": "ACTIVE",
    "createdAt": "2024-03-22T10:30:00",
    "roles": ["STUDENT"]
  }
}
```

---

### 3. **Access Protected Endpoint (using JWT)**

**Endpoint:** `GET http://localhost:8080/api/auth/profile`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
{
  "message": "Access granted",
  "username": "john_doe"
}
```

---

## 🔐 Security Features

✅ **JWT Token-based Authentication**
- Stateless authentication
- Token expiration: 1 day (configurable)
- Bearer token in Authorization header

✅ **Role-Based Access Control (RBAC)**
- Three roles: ADMIN, INSTRUCTOR, STUDENT
- Method-level authorization ready (@PreAuthorize)
- Role claims included in JWT

✅ **Password Security**
- BCrypt password encoding
- Passwords never stored in plain text
- Validation: minimum 6 characters

✅ **CORS Support**
- Configured for all /api/** endpoints
- Allows front-end interaction

✅ **Global Exception Handling**
- Validation error messages
- Consistent error response format

---

## 📧 Email Configuration

Configure email settings in `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**For Gmail:**
1. Enable 2-factor authentication
2. Generate an App Password
3. Use the App Password in `spring.mail.password`

---

## 🗂️ Project Structure

```
CourseManagerment/
├── entity/
│   ├── User.java
│   └── Role.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   └── AuthResponse.java
├── controller/
│   ├── AuthController.java
│   └── HomeController.java
├── service/
│   ├── AuthService.java
│   └── EmailService.java
├── repository/
│   ├── UserRepository.java
│   └── RoleRepository.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   ├── DataInitializer.java
│   └── ...
├── exception/
│   └── GlobalExceptionHandler.java
└── resources/
    ├── templates/
    │   └── index.html
    └── application.properties
```

---

## 🔄 Next Steps

The following features will be implemented next:

1. **Course Management**
   - Course entity with categories
   - Instructor course creation
   - Course CRUD operations

2. **Enrollment System**
   - Student enrollment
   - Enrollment tracking

3. **Learning Progress**
   - Progress tracking per lesson
   - Resume functionality

4. **Payment Integration**
   - Payment history
   - Payment status tracking

5. **Advanced Features**
   - Login history for security
   - Dynamic pricing rules
   - Smart learning path recommendations
   - WebSocket for real-time learning

6. **Admin Dashboard**
   - User management
   - Course moderation
   - Statistics

---

## 📝 Notes

- **Database**: MySQL (auto-created tables with JPA)
- **Java Version**: 17+
- **Spring Boot Version**: 4.0.4
- **Default Roles**: Created automatically on startup
- **JWT Secret**: Configure in `application.properties` (jwt.secret)
- **Token Expiration**: 1 day (86400000 ms)

---

## 🧪 Testing Checklist

- [ ] Test user registration with valid data
- [ ] Test registration with duplicate username/email
- [ ] Test registration with invalid data (validation)
- [ ] Test user login with correct credentials
- [ ] Test login with incorrect password
- [ ] Test login with non-existent user
- [ ] Test accessing protected endpoint without token
- [ ] Test accessing protected endpoint with valid token
- [ ] Test accessing protected endpoint with invalid token
- [ ] Verify JWT token contains user roles
- [ ] Verify password is encoded (BCrypt)
- [ ] Verify registration email is sent
- [ ] Check database for created users and roles

---

**Last Updated**: March 22, 2026
