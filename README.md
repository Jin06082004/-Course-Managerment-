# 🎓 Course Management System (LMS)

> Hệ thống Quản lý Khóa học trực tuyến — Learning Management System  
> **Stack:** Spring Boot 4.0.4 · Java 21 · MySQL · Thymeleaf · JWT · MoMo Payment

---

## Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Chức năng theo vai trò](#chức-năng-theo-vai-trò)
- [Chi tiết API Endpoints](#chi-tiết-api-endpoints)
- [Luồng vận hành chính](#luồng-vận-hành-chính)
- [Bảo mật](#bảo-mật)
- [Database Schema](#database-schema)
- [Cấu hình & Triển khai](#cấu-hình--triển-khai)

---

## Tổng quan

Hệ thống LMS hỗ trợ **3 vai trò** (Student, Instructor, Admin) với các tính năng:

| # | Nhóm chức năng | Mô tả |
|---|----------------|-------|
| 1 | **Xác thực & Phân quyền** | Đăng ký, đăng nhập JWT, phân quyền theo vai trò |
| 2 | **Quản lý Khóa học** | CRUD khóa học, phân loại, tìm kiếm, lọc |
| 3 | **Đăng ký Khóa học** | Ghi danh miễn phí / trả phí, theo dõi tiến độ |
| 4 | **Thanh toán MoMo** | Tích hợp cổng thanh toán MoMo (Sandbox) |
| 5 | **Wishlist** | Lưu khóa học yêu thích |
| 6 | **Email thông báo** | Gửi email xác nhận đăng ký, thanh toán |
| 7 | **Bảng điều khiển Instructor** | Quản lý khóa học, phân tích hiệu suất |
| 8 | **Bảng điều khiển Admin** | Quản lý người dùng, vai trò, danh mục, thống kê |
| 9 | **Giao diện Thymeleaf** | 20+ trang HTML responsive với Bootstrap |

---

## Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (Browser)                       │
│   Thymeleaf Templates + CSS + Vanilla JavaScript            │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/HTTPS
┌───────────────────────▼─────────────────────────────────────┐
│                   SPRING SECURITY FILTER                     │
│   JwtAuthenticationFilter → SecurityContext                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                     CONTROLLER LAYER                         │
│   PageController (views)  │  REST Controllers (API /api/*) │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                     SERVICE LAYER (7 services)               │
│   AuthService · CourseService · EnrollmentService           │
│   CategoryService · WishlistService · MoMoPaymentService    │
│   EmailService                                               │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                    REPOSITORY LAYER (JPA)                     │
│   UserRepo · RoleRepo · CourseRepo · CategoryRepo           │
│   EnrollmentRepo · WishlistRepo                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                     MySQL Database                            │
│   users · roles · courses · categories                       │
│   enrollments · wishlists                                    │
└─────────────────────────────────────────────────────────────┘
```

**Luồng request:**
1. Request → `JwtAuthenticationFilter` xác thực token
2. → Controller nhận request, validate input
3. → Service xử lý business logic
4. → Repository truy vấn database (JPA/Hibernate)
5. → Response trả về JSON (API) hoặc render HTML (Thymeleaf)

---

## Công nghệ sử dụng

| Thành phần | Công nghệ | Phiên bản |
|------------|-----------|-----------|
| Backend Framework | Spring Boot | 4.0.4 |
| Ngôn ngữ | Java | 21 |
| ORM | Hibernate / Spring Data JPA | — |
| Database | MySQL | — |
| Template Engine | Thymeleaf | — |
| Bảo mật | Spring Security 6 + JWT (HS512) | — |
| Thanh toán | MoMo API v2 (captureWallet) | Sandbox |
| Email | Spring Mail (Gmail SMTP) | — |
| Build Tool | Maven | — |
| API Docs | Swagger / OpenAPI 3 | — |
| Utility | Lombok | — |
| Dev Tools | Spring Boot DevTools (hot reload) | — |

---

## Cấu trúc dự án

```
CourseManagerment/src/main/java/com/_6/CourseManagerment/
├── config/                  # Cấu hình ứng dụng
│   ├── SecurityConfig.java          # Spring Security, CORS, JWT filter
│   ├── WebConfig.java               # CORS REST config
│   ├── DataInitializer.java         # Tạo roles mặc định (ADMIN, INSTRUCTOR, STUDENT)
│   └── CategoryInitializer.java     # Tạo categories mặc định
│
├── controller/              # Xử lý request
│   ├── AuthController.java          # /api/auth/* — Đăng ký, đăng nhập
│   ├── CourseController.java        # /api/courses/* — CRUD khóa học
│   ├── EnrollmentController.java    # /api/enrollments/* — Ghi danh
│   ├── PaymentController.java       # /api/payment/* — Thanh toán MoMo
│   ├── WishlistController.java      # /api/wishlist/* — Danh sách yêu thích
│   ├── CategoryController.java      # /api/categories/* — Danh mục
│   ├── InstructorController.java    # /api/instructor/* — API instructor
│   ├── AdminController.java         # /api/admin/* — API admin
│   ├── PageController.java          # / — Trang HTML public
│   ├── InstructorPageController.java # /instructor/* — Trang instructor
│   └── AdminPageController.java     # /admin/* — Trang admin
│
├── model/                   # Entity (database mapping)
│   ├── User.java                    # Bảng users
│   ├── Role.java                    # Bảng roles
│   ├── Course.java                  # Bảng courses
│   ├── Category.java                # Bảng categories
│   ├── Enrollment.java              # Bảng enrollments
│   └── Wishlist.java                # Bảng wishlists
│
├── dto/                     # Data Transfer Objects
│   ├── RegisterRequest.java         # Dữ liệu đăng ký
│   ├── LoginRequest.java            # Dữ liệu đăng nhập
│   ├── AuthResponse.java           # Response chứa JWT token
│   ├── UserDto.java                 # Thông tin user (an toàn)
│   ├── CourseDto.java               # Thông tin khóa học
│   ├── CreateCourseRequest.java     # Dữ liệu tạo khóa học
│   ├── EnrollmentDto.java           # Thông tin ghi danh
│   ├── WishlistDto.java             # Thông tin wishlist
│   └── PageResponse.java           # Response phân trang generic
│
├── repository/              # Data Access Layer (JPA)
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── CourseRepository.java
│   ├── CategoryRepository.java
│   ├── EnrollmentRepository.java
│   └── WishlistRepository.java
│
├── service/                 # Business Logic
│   ├── AuthService.java             # Xác thực, tạo token
│   ├── CourseService.java           # Logic khóa học
│   ├── EnrollmentService.java       # Logic ghi danh + thanh toán
│   ├── CategoryService.java         # Logic danh mục
│   ├── WishlistService.java         # Logic wishlist
│   ├── MoMoPaymentService.java      # Tích hợp cổng MoMo
│   └── EmailService.java            # Gửi email thông báo
│
├── security/                # Bảo mật
│   ├── JwtTokenProvider.java        # Tạo & xác thực JWT
│   ├── JwtAuthenticationFilter.java # Filter xác thực mỗi request
│   ├── CustomUserDetailsService.java# Load user từ DB
│   ├── SecurityUtils.java           # Helper lấy user hiện tại
│   └── MoMoSecurityUtil.java        # HMAC-SHA256 cho MoMo
│
└── CourseManagermentApplication.java # Main entry point
```

---

## Chức năng theo vai trò

### 👤 Khách (Guest — Chưa đăng nhập)

| Chức năng | Endpoint | Ghi chú |
|-----------|----------|---------|
| Xem trang chủ | `GET /` | Hiển thị khóa học nổi bật |
| Xem danh sách khóa học | `GET /courses` | Phân trang, lọc theo danh mục/level |
| Xem chi tiết khóa học | `GET /courses/{id}` | Thông tin đầy đủ + instructor |
| Tìm kiếm khóa học | `GET /api/courses/search` | Theo title, category, level |
| Đăng ký tài khoản | `POST /api/auth/register` | Tự động gán role STUDENT |
| Đăng nhập | `POST /api/auth/login` | Trả về JWT token |

### 🎓 Student (Học viên)

| Chức năng | Endpoint | Ghi chú |
|-----------|----------|---------|
| Ghi danh khóa miễn phí | `POST /api/enrollments/enroll/{courseId}` | Trạng thái → ENROLLED, FREE |
| Mua khóa trả phí | `POST /api/payment/create` | Chuyển hướng sang MoMo |
| Xem khóa học đã đăng ký | `GET /api/enrollments/my-courses` | Phân trang |
| Xem khóa đang học | `GET /api/enrollments/active` | Status = ENROLLED/IN_PROGRESS + PAID/FREE |
| Xem khóa đã hoàn thành | `GET /api/enrollments/completed` | Status = COMPLETED |
| Cập nhật tiến độ | `PUT /api/enrollments/{id}/progress` | 0–100% |
| Hoàn thành khóa học | `PUT /api/enrollments/{id}/complete` | Đánh dấu COMPLETED |
| Hủy ghi danh | `DELETE /api/enrollments/{courseId}/unenroll` | Xóa enrollment |
| Thêm/xóa wishlist | `POST /api/wishlist/toggle/{courseId}` | Toggle on/off |
| Xem wishlist | `GET /api/wishlist` | Phân trang |
| Xem/sửa profile | `GET/PUT /api/auth/profile` | Cập nhật thông tin cá nhân |
| Đổi mật khẩu | `POST /api/auth/change-password` | Yêu cầu mật khẩu cũ |

### 👨‍🏫 Instructor (Giảng viên)

| Chức năng | Endpoint | Ghi chú |
|-----------|----------|---------|
| Tạo khóa học mới | `POST /api/instructor/courses` | Trạng thái → DRAFT |
| Sửa khóa học | `PUT /api/instructor/courses/{id}` | Chỉ khóa của mình |
| Xóa khóa học | `DELETE /api/instructor/courses/{id}` | Chỉ khóa của mình |
| Xem danh sách khóa mình dạy | `GET /api/instructor/courses` | Phân trang |
| Xem chi tiết khóa | `GET /api/instructor/courses/{id}` | Kèm thống kê |
| Dashboard phân tích | `GET /api/instructor/analytics/dashboard` | Tổng quan hiệu suất |
| Phân tích theo khóa | `GET /api/instructor/analytics/course/{id}` | Chi tiết từng khóa |
| **Trang giao diện** | `/instructor/*` | dashboard, courses, create, edit, content, students, earnings, statistics, settings |

### 🔧 Admin (Quản trị viên)

| Chức năng | Endpoint | Ghi chú |
|-----------|----------|---------|
| Quản lý người dùng | `GET/PUT/DELETE /api/admin/users/*` | Lọc theo role, tìm kiếm |
| Gán vai trò | `POST /api/admin/users/{id}/roles` | Thay đổi role của user |
| Quản lý danh mục | `POST/PUT/DELETE /api/categories/*` | CRUD categories |
| Quản lý khóa học | `PUT/DELETE /api/courses/{id}` | Duyệt, xóa khóa học |
| Thống kê hệ thống | `/api/admin/statistics` | Dashboard tổng quan |
| **Trang giao diện** | `/admin/*` | dashboard, users, courses, categories, roles, statistics, settings, edit-user |

---

## Chi tiết API Endpoints

### 🔐 Authentication (`/api/auth`)

```
POST /api/auth/register          — Đăng ký (username, email, password, fullName)
POST /api/auth/login             — Đăng nhập → JWT token
POST /api/auth/logout            — Đăng xuất
GET  /api/auth/profile           — Xem profile [🔒 Auth]
PUT  /api/auth/profile           — Sửa profile [🔒 Auth]
POST /api/auth/change-password   — Đổi mật khẩu [🔒 Auth]
```

### 📚 Courses (`/api/courses`)

```
GET  /api/courses                — Danh sách khóa (phân trang, sắp xếp)
GET  /api/courses/{id}           — Chi tiết khóa
GET  /api/courses/search         — Tìm kiếm (title, categoryId, level)
GET  /api/courses/featured       — Khóa nổi bật
GET  /api/courses/category/{id}  — Theo danh mục
GET  /api/courses/instructor/{id}— Theo giảng viên
POST /api/courses                — Tạo khóa [🔒 INSTRUCTOR/ADMIN]
PUT  /api/courses/{id}           — Sửa khóa [🔒 INSTRUCTOR/ADMIN]
DELETE /api/courses/{id}         — Xóa khóa [🔒 ADMIN]
```

### 📝 Enrollments (`/api/enrollments`)

```
POST /api/enrollments/enroll/{courseId}     — Ghi danh [🔒 Auth]
GET  /api/enrollments/check/{courseId}      — Kiểm tra trạng thái [🔒 Auth]
GET  /api/enrollments/my-courses            — DS khóa đã đăng ký [🔒 Auth]
GET  /api/enrollments/active                — Khóa đang học [🔒 Auth]
GET  /api/enrollments/completed             — Khóa hoàn thành [🔒 Auth]
GET  /api/enrollments/{enrollmentId}        — Chi tiết enrollment [🔒 Auth]
PUT  /api/enrollments/{id}/progress         — Cập nhật tiến độ [🔒 Auth]
PUT  /api/enrollments/{id}/complete         — Hoàn thành khóa [🔒 Auth]
DELETE /api/enrollments/{courseId}/unenroll  — Hủy ghi danh [🔒 Auth]
```

### 💳 Payment (`/api/payment`)

```
POST /api/payment/create         — Tạo thanh toán MoMo [🔒 Auth]
GET  /api/payment/momo-return    — Callback sau thanh toán (redirect)
POST /api/payment/momo-notify    — Webhook IPN từ MoMo (server-to-server)
```

### ❤️ Wishlist (`/api/wishlist`)

```
GET  /api/wishlist                    — DS wishlist [🔒 Auth]
POST /api/wishlist/toggle/{courseId}  — Toggle thêm/xóa [🔒 Auth]
POST /api/wishlist/{courseId}         — Thêm vào wishlist [🔒 Auth]
DELETE /api/wishlist/{courseId}       — Xóa khỏi wishlist [🔒 Auth]
GET  /api/wishlist/check/{courseId}   — Kiểm tra [🔒 Auth]
```

### 🏷️ Categories (`/api/categories`)

```
GET  /api/categories             — Tất cả danh mục
GET  /api/categories/{id}        — Chi tiết danh mục
POST /api/categories             — Tạo danh mục [🔒 ADMIN]
PUT  /api/categories/{id}        — Sửa danh mục [🔒 ADMIN]
DELETE /api/categories/{id}      — Xóa danh mục [🔒 ADMIN]
```

### 👨‍🏫 Instructor (`/api/instructor`)

```
GET  /api/instructor/categories             — DS danh mục (dropdown)
GET  /api/instructor/courses                — DS khóa của mình
GET  /api/instructor/courses/{id}           — Chi tiết khóa
POST /api/instructor/courses                — Tạo khóa mới
PUT  /api/instructor/courses/{id}           — Sửa khóa
DELETE /api/instructor/courses/{id}         — Xóa khóa
GET  /api/instructor/analytics/dashboard    — Tổng quan
GET  /api/instructor/analytics/course/{id}  — Phân tích từng khóa
```

### 🔧 Admin (`/api/admin`)

```
GET  /api/admin/users                — DS người dùng (lọc, tìm kiếm)
GET  /api/admin/users/{id}           — Chi tiết user
PUT  /api/admin/users/{id}           — Sửa user
DELETE /api/admin/users/{id}         — Xóa user
POST /api/admin/users/{id}/roles     — Gán role
```

---

## Luồng vận hành chính

### 1. Luồng Đăng ký & Đăng nhập

```
[User] → POST /api/auth/register (username, email, password, fullName)
         ↓
[AuthService] kiểm tra trùng username/email → Mã hóa BCrypt → Lưu DB (role=STUDENT)
         ↓
[EmailService] gửi email xác nhận đăng ký → [User]
         ↓
[User] → POST /api/auth/login (username, password)
         ↓
[AuthService] xác thực → [JwtTokenProvider] tạo JWT token (HS512, TTL 24h)
         ↓
Response: { token: "eyJ...", user: { id, name, role } }
         ↓
[Browser] lưu token vào localStorage → Gắn header "Authorization: Bearer {token}" mỗi request
```

### 2. Luồng Ghi danh Khóa học Miễn phí

```
[Student] → POST /api/enrollments/enroll/{courseId}
             ↓
[EnrollmentService] kiểm tra:
  - Đã ghi danh chưa? → Nếu rồi, trả lỗi
  - Khóa có tồn tại? → Nếu không, trả lỗi
  - Price ≤ 0? → Khóa miễn phí
             ↓
Tạo Enrollment: status=ENROLLED, paymentStatus=FREE
             ↓
Cập nhật course.studentCount += 1
             ↓
Response: EnrollmentDto (với progress=0%, status=ENROLLED)
```

### 3. Luồng Thanh toán Khóa học Trả phí (MoMo)

```
[Student] → POST /api/payment/create { courseId: 123 }
             ↓
[PaymentController]:
  - Lấy thông tin course (title, price)
  - Tạo orderId = "COURSE_{courseId}_{userId}_{uuid}"
  - Tạo Enrollment: status=PENDING_PAYMENT, paymentStatus=PENDING, orderId
             ↓
[MoMoPaymentService].createPayment():
  - Tạo rawSignature (alphabetical params)
  - Ký HMAC-SHA256 với secretKey
  - POST đến MoMo API → Nhận payUrl
             ↓
Response: { payUrl: "https://test-payment.momo.vn/...", orderId: "..." }
             ↓
[Browser] redirect đến payUrl → [MoMo] hiển thị trang thanh toán
             ↓
[User] thanh toán trên MoMo (hoặc hủy)
             ↓
┌─────────────────────────────────────────────────────────┐
│ MoMo redirect → GET /api/payment/momo-return            │
│   resultCode = 0 (thành công):                          │
│     → activateEnrollmentByOrderId(orderId)              │
│     → paymentStatus = PAID, status = ENROLLED           │
│     → Gửi email xác nhận thanh toán                     │
│     → Redirect đến trang khóa học                       │
│   resultCode ≠ 0 (thất bại):                           │
│     → Enrollment giữ PENDING_PAYMENT                    │
│     → Redirect đến trang lỗi                            │
├─────────────────────────────────────────────────────────┤
│ MoMo IPN → POST /api/payment/momo-notify               │
│   Server-to-server callback (đảm bảo chính xác):       │
│   → Xác thực chữ ký HMAC-SHA256                        │
│   → Nếu hợp lệ: activateEnrollment                    │
│   → Trả { status: "ok" }                               │
└─────────────────────────────────────────────────────────┘
```

### 4. Luồng Theo dõi Tiến độ Học tập

```
[Student] xem khóa học → PUT /api/enrollments/{id}/progress { percentage: 45 }
             ↓
[EnrollmentService]:
  - Cập nhật progressPercentage = 45%
  - Nếu percentage = 100 → status = COMPLETED, completionDate = now
  - Cập nhật lastAccessedDate = now
             ↓
[Student Dashboard] /my-courses hiển thị:
  - Tab "Đang học": Courses có status IN_PROGRESS/ENROLLED
  - Tab "Hoàn thành": Courses có status COMPLETED
  - Progress bar cho mỗi khóa
```

### 5. Luồng Quản lý của Instructor

```
[Instructor] → POST /api/instructor/courses
  { title, description, code, categoryId, level, price, duration, thumbnailUrl }
             ↓
[CourseService]:
  - Validate: code phải unique
  - Tạo Course: status=DRAFT, studentCount=0
             ↓
[Instructor] → (Chỉnh sửa nội dung) → PUT /api/instructor/courses/{id}
             ↓
[Instructor] → Xem dashboard: GET /api/instructor/analytics/dashboard
  - Tổng số khóa, tổng học viên, rating trung bình
```

### 6. Luồng Quản trị Admin

```
[Admin] → /admin/dashboard — Tổng quan hệ thống
             ↓
[Admin] → GET /api/admin/users?role=STUDENT&search=john
  - Lọc theo role, tìm kiếm theo tên/email
             ↓
[Admin] → POST /api/admin/users/{id}/roles { role: "INSTRUCTOR" }
  - Thay đổi vai trò user (VD: nâng Student lên Instructor)
             ↓
[Admin] → CRUD categories, duyệt/xóa khóa học
```

---

## Bảo mật

### Xác thực JWT (JSON Web Token)

| Thuộc tính | Giá trị |
|------------|---------|
| Algorithm | HS512 (HMAC-SHA512) |
| Secret Key | 512+ bit (cấu hình trong `application.properties`) |
| Time-to-live | 24 giờ (86400000 ms) |
| Claims | sub (username), userId, roles, iss, exp |

**Workflow:**
- Mỗi request → `JwtAuthenticationFilter` trích `Authorization: Bearer {token}`
- Validate signature + expiration → Set `SecurityContext`
- Controller nhận user đã xác thực từ context

### Mã hóa mật khẩu
- **BCrypt** (strength mặc định) — mật khẩu không bao giờ lưu plaintext

### Phân quyền theo vai trò (RBAC)

| Vai trò | Quyền truy cập |
|---------|----------------|
| `ROLE_STUDENT` | Ghi danh, học, wishlist, profile |
| `ROLE_INSTRUCTOR` | Tạo/sửa/xóa khóa học, xem analytics |
| `ROLE_ADMIN` | Toàn quyền: quản lý user, role, category, khóa học |

### CORS Configuration
- Origins: `localhost:8080`, `localhost:3000`, `localhost:4200`
- Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- Credentials: Enabled

### Bảo mật Thanh toán MoMo
- **HMAC-SHA256** ký mỗi request đến MoMo
- **Xác thực IPN** — verify chữ ký webhook chống giả mạo
- **Unique OrderId** — `COURSE_{courseId}_{userId}_{uuid}` chống replay attack

### Endpoints công khai (không cần token)
```
/api/auth/**  ·  /api/courses/**  ·  /api/categories/**
/api/payment/momo-return  ·  /api/payment/momo-notify
/  ·  /home  ·  /login  ·  /register  ·  /courses/**  ·  /error/**
/css/**  ·  /js/**  ·  /images/**  ·  /swagger-ui/**
```

---

## Database Schema

### Bảng `users`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã người dùng |
| username | VARCHAR(100) | UNIQUE, NOT NULL | Tên đăng nhập |
| email | VARCHAR(100) | UNIQUE, NOT NULL | Email |
| password | VARCHAR(255) | NOT NULL | Mật khẩu (BCrypt) |
| full_name | VARCHAR(200) | | Họ tên |
| avatar | VARCHAR(500) | | URL ảnh đại diện |
| provider | VARCHAR(50) | | LOCAL / GOOGLE / FACEBOOK |
| status | VARCHAR(20) | | ACTIVE / LOCKED |
| role_id | BIGINT | FK → roles(id) | Vai trò |
| created_at | DATETIME | | Ngày tạo |
| updated_at | DATETIME | | Ngày cập nhật |

### Bảng `roles`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã vai trò |
| name | VARCHAR(50) | UNIQUE | ADMIN / INSTRUCTOR / STUDENT |

### Bảng `courses`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã khóa học |
| title | VARCHAR(255) | NOT NULL | Tên khóa học |
| description | TEXT | | Mô tả chi tiết |
| code | VARCHAR(50) | UNIQUE | Mã khóa (VD: CS101) |
| category_id | BIGINT | FK → categories(id) | Danh mục |
| instructor_id | BIGINT | FK → users(id) | Giảng viên |
| level | VARCHAR(20) | | BEGINNER / INTERMEDIATE / ADVANCED |
| price | DECIMAL | | Giá (VND), 0 = miễn phí |
| duration | INT | | Thời lượng (giờ) |
| rating | FLOAT | | Đánh giá trung bình (0–5) |
| student_count | INT | | Số học viên |
| thumbnail_url | TEXT | | Ảnh bìa |
| video_url | TEXT | | Video giới thiệu |
| status | VARCHAR(20) | | DRAFT / PUBLISHED / ARCHIVED |
| created_at | DATETIME | | Ngày tạo |
| updated_at | DATETIME | | Ngày cập nhật |

### Bảng `categories`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã danh mục |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Tên danh mục |
| description | TEXT | | Mô tả |
| icon | VARCHAR(100) | | Icon class / URL |
| color | VARCHAR(20) | | Mã màu (hex) |
| created_at | DATETIME | | Ngày tạo |

### Bảng `enrollments`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã ghi danh |
| user_id | BIGINT | FK → users(id) | Học viên |
| course_id | BIGINT | FK → courses(id) | Khóa học |
| enrollment_date | DATETIME | | Ngày ghi danh |
| completion_date | DATETIME | | Ngày hoàn thành |
| progress_percentage | FLOAT | | Tiến độ 0–100% |
| status | VARCHAR(30) | | PENDING_PAYMENT / ENROLLED / IN_PROGRESS / COMPLETED |
| payment_status | VARCHAR(20) | | PENDING / PAID / FREE |
| order_id | VARCHAR(255) | UNIQUE | Mã đơn MoMo |
| last_accessed_date | DATETIME | | Truy cập cuối |
| **UNIQUE** | | (user_id, course_id) | Không trùng ghi danh |

### Bảng `wishlists`
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK, AUTO | Mã wishlist |
| user_id | BIGINT | FK → users(id) | Người dùng |
| course_id | BIGINT | FK → courses(id) | Khóa học |
| created_at | DATETIME | | Ngày thêm |
| **UNIQUE** | | (user_id, course_id) | Không trùng |

---

## Cấu hình & Triển khai

### Yêu cầu hệ thống
- **Java** 21+
- **Maven** 3.9+
- **MySQL** 8.0+
- **Port** 8080 (mặc định)

### Các bước chạy

```bash
# 1. Clone repo
git clone <repo-url>
cd CourseManagerment

# 2. Tạo database MySQL
mysql -u root -p
CREATE DATABASE course_managerment_;

# 3. Cấu hình application.properties
# Sửa username/password MySQL, JWT secret, email, MoMo keys

# 4. Build & chạy
./mvnw spring-boot:run
# hoặc
mvn clean package
java -jar target/CourseManagerment-0.0.1-SNAPSHOT.jar

# 5. Truy cập
# Web: http://localhost:8080
# API: http://localhost:8080/api/*
# Swagger: http://localhost:8080/swagger-ui/index.html
```

### Biến cấu hình quan trọng (`application.properties`)

| Key | Mô tả | Mặc định |
|-----|--------|----------|
| `spring.datasource.url` | JDBC URL MySQL | `jdbc:mysql://localhost:3306/course_managerment_` |
| `spring.datasource.username` | User MySQL | `root` |
| `spring.datasource.password` | Password MySQL | `123456` |
| `spring.jpa.hibernate.ddl-auto` | Tự động tạo/cập nhật bảng | `update` |
| `jwt.secret` | JWT signing key (512+ bit) | — |
| `jwt.expiration` | JWT TTL (ms) | `86400000` (24h) |
| `spring.mail.username` | Gmail gửi email | — |
| `spring.mail.password` | Gmail App Password | — |
| `momo.partner-code` | MoMo Partner Code | (Sandbox) |
| `momo.access-key` | MoMo Access Key | (Sandbox) |
| `momo.secret-key` | MoMo Secret Key | (Sandbox) |
| `momo.return-url` | URL callback sau thanh toán | `http://localhost:8080/api/payment/momo-return` |

### Dữ liệu khởi tạo tự động
Khi ứng dụng khởi động lần đầu, `DataInitializer` tự tạo:
- **Roles:** ADMIN, INSTRUCTOR, STUDENT
- **Categories:** Các danh mục mặc định (nếu có `CategoryInitializer`)

---

## Thống kê dự án

| Chỉ số | Giá trị |
|--------|---------|
| Tổng Entity | 6 (User, Role, Course, Category, Enrollment, Wishlist) |
| Tổng Service | 7 |
| Tổng Controller | 11 (3 page + 8 REST) |
| Tổng API endpoint | 50+ |
| Tổng Templates | 20+ trang HTML |
| Database tables | 6 |
| Phiên bản Java | 21 |
| Framework | Spring Boot 4.0.4 |

---

> **Ghi chú:** Dự án đang chạy MoMo ở chế độ **Sandbox** (test). Trước khi deploy production, cần thay đổi MoMo credentials và `return-url`/`notify-url` sang domain thực.