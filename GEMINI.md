# SMARTBANK PROJECT CONTEXT & AGENT MEMORY (GEMINI.md)

> File này đóng vai trò là **bộ nhớ dài hạn (Long-term Memory)** của AI Agent (Antigravity). Mỗi khi bắt đầu một cuộc trò chuyện mới, AI sẽ tự động đọc file này để nắm trọn bối cảnh dự án mà người dùng không cần giải thích lại.

---

## 🏦 1. THÔNG TIN DỰ ÁN (PROJECT OVERVIEW)
- **Tên dự án:** SmartBank (Hệ thống ngân hàng số mini phục vụ giao dịch trực tuyến)
- **Công nghệ cốt lõi:**
  - Java 21 LTS (sử dụng các tính năng mới như Record, Pattern Matching, Stream API tối ưu)
  - Spring Boot 3.3.4 (Spring Data JPA, Spring Security 6, Spring Validation, Spring Caching)
  - Database: MySQL 8.0 (InnoDB engine, B-Tree index)
  - Caching & Blacklist: Redis
  - Mapping & Boilerplate: MapStruct 1.6.3 + Lombok
  - Testing: JUnit 5, Mockito (37/37 unit/integration tests passing)
  - Containerization: Docker, Docker Compose

---

## 🏗️ 2. KIẾN TRÚC & THIẾT KẾ CƠ SỞ DỮ LIỆU
### Các thực thể chính (Entities):
1. **User (Users):** Quản lý tài khoản đăng nhập, thông tin định danh, quan hệ 1-N với Account, phân quyền (`ROLE_USER`, `ROLE_ADMIN`).
2. **Account (Accounts):** Tài khoản ngân hàng, số tài khoản (unique index `idx_account_number`), số dư (`balance` kiểu `BigDecimal`), trạng thái (`ACTIVE`, `LOCKED`).
3. **Transaction (Transactions):** Lịch sử biến động số dư, ghi nhận `source_account_id`, `target_account_id`, `amount`, `type` (`TRANSFER`, `DEPOSIT`, `WITHDRAW`), `status` (`SUCCESS`, `FAILED`), index kép `(account_id, created_at)` phục vụ truy vấn sao kê có phân trang.
4. **IdempotencyRecord:** Đảm bảo Idempotency (chống trùng lặp giao dịch khi client bấm nhiều lần hoặc retry mạng).

### Cấu trúc phân tầng & Design Patterns (Layered Architecture):
- **Controller Layer (`controller.v1`):** Áp dụng API Versioning rõ ràng, nhận DTO gắn `@Valid` và trả về `ApiResponse<T>`.
- **Service Layer (`service` & `service.impl`):** Tuân thủ chặt chẽ nguyên lý **Dependency Inversion (chữ D trong SOLID)**. Tầng Controller chỉ phụ thuộc vào các Interface trừu tượng (`AccountService`, `TransactionService`...), tầng `service.impl` chứa các class triển khai cụ thể (`AccountServiceImpl`, `TransactionServiceImpl`...).

---

## ⚡ 3. CÁC ĐIỂM SÁNG KỸ THUẬT (HIGHLIGHTS DÙNG ĐỂ PHỎNG VẤN)
1. **Concurrency Control & Deadlock Prevention (Xử lý đồng thời & Chống Deadlock):**
   - Sử dụng **Pessimistic Write Lock (`PESSIMISTIC_WRITE`)** với câu lệnh `SELECT ... FOR UPDATE` qua method `findByAccountNumberWithLock`.
   - **Kỹ thuật Lock Ordering (Sắp xếp thứ tự khóa):** Khi chuyển tiền giữa 2 tài khoản A và B, hệ thống luôn sắp xếp khóa tài khoản có ID nhỏ hơn trước (`min(id)` rồi mới tới `max(id)`). Kỹ thuật này phá vỡ điều kiện *Circular Wait* (chờ đợi vòng tròn của Coffman), triệt tiêu hoàn toàn nguy cơ Deadlock khi 2 người chuyển tiền chéo nhau cùng tích tắc.
2. **Xử lý số thực với tiền tệ:** Dùng tuyệt đối `BigDecimal` (không dùng `double`/`float`) để tránh lỗi mất mát độ chính xác nhị phân.
3. **Hiệu năng & Tối ưu Database:**
   - Tránh N+1 Query bằng cách thiết lập FetchType mặc định là `LAZY` kết hợp `@EntityGraph` / `JOIN FETCH` khi cần lấy dữ liệu liên quan.
   - Đánh Index trên các cột tìm kiếm và lọc thường xuyên (`account_number`, `created_at`, `status`).
4. **Bảo mật:**
   - JWT Authentication stateless, băm mật khẩu với `BCryptPasswordEncoder`.
   - Thu hồi Token (Logout) thông qua Redis Token Blacklist với cơ chế tự hủy TTL theo thời gian hết hạn của JWT.
5. **Clean Code & DTO Mapping:** Sử dụng MapStruct 1.6.3 để sinh code mapper tại compile-time, nhanh gấp nhiều lần Reflection và sạch sẽ hơn `BeanUtils`.
6. **Chuẩn Hóa Phản Hồi & Phân Trang Hiệu Năng Cao (ApiResponse & Pagination):**
   - Đóng gói toàn bộ kết quả API qua `ApiResponse<T>` thống nhất format (`code`, `message`, `data`, `timestamp`).
   - Phân trang sao kê giao dịch với `PageResponse<T>` và Spring Data JPA `Pageable` kết hợp Index kép `(account_id, created_at)` sắp xếp thời gian mới nhất, triệt tiêu nguy cơ OutOfMemoryError (OOM) khi số lượng giao dịch ngân hàng tăng lớn.
7. **Kiến Trúc Tầng Service Chuẩn Enterprise (Dependency Inversion & Loose Coupling):**
   - Tách biệt Interface (`AccountService`, `TransactionService`...) và Class triển khai (`AccountServiceImpl`...), giúp dễ dàng mở rộng logic nghiệp vụ (ví dụ thêm phiên bản VIP, OTP verification) mà không gây ảnh hưởng đến Controller, tối ưu hóa quá trình viết Unit Test / Mocking với Mockito.

---

## 📚 4. HỆ THỐNG TÀI LIỆU ÔN TẬP PHỎNG VẤN TRONG WORKSPACE
Trong dự án đã xây dựng sẵn các bộ cẩm nang ôn tập phỏng vấn đầy đủ cho Intern / Fresher:
1. **Spring Boot & Dự án SmartBank:**
   - Markdown: `INTERVIEW_PREP_GUIDE.md`
   - Bản in PDF: `INTERVIEW_PREP_GUIDE.html` (Mở trình duyệt, nhấn `Ctrl + P` để in/lưu PDF)
   - Nội dung: 8 chủ đề Spring Boot + Elevator Pitch 2 phút giới thiệu dự án + Câu hỏi thực chiến từ `luyenphongvan.online`.
2. **Java Core & Design Patterns:**
   - Markdown: `JAVA_CORE_INTERVIEW_GUIDE.md`
   - Bản in PDF: `JAVA_CORE_INTERVIEW_GUIDE.html`
   - Nội dung: 8 chủ đề Java Core (OOP, Memory Stack vs Heap, Collections, Exception, Java 8 Stream/Lambda/Optional, Generics, Concurrency & Java 21 Virtual Threads, Top Design Patterns: Singleton, Factory, Builder, Proxy, Strategy, Observer).
3. **Cơ Chế Xử Lý Lỗi (Exception Handling Master Guide):**
   - Markdown: `EXCEPTION_HANDLING_GUIDE.md`
   - Nội dung: Cây phân cấp ngoại lệ Java, Checked vs Unchecked, Try-catch-finally, Try-with-resources, Global Exception Handling (`@RestControllerAdvice`, `@ExceptionHandler`), ErrorCode Enum, AppException, và bộ câu hỏi phỏng vấn song ngữ Anh - Việt thực chiến.
4. **Bộ Cẩm Nang 8 Topic Ôn Tập Phỏng Vấn Thực Chiến (Lộ trình Theory ➡️ Project Connection):**
   - Master Index & Checklist: `InterviewPrep.md`
   - **Topic 1:** `1_JavaCore&OOP.md` (4 tính chất OOP, Stack vs Heap, Pass-by-value, BigDecimal vs Double, Java 21 Record).
   - **Topic 2:** `2_SpringCore&Architecture.md` (IoC, DI, Bean Scope/Lifecycle, SOLID & DIP, Layered Architecture).
   - **Topic 3:** `3_RestApi&Controller.md` (RESTful, HTTP Codes, DTO vs Entity, MapStruct, ApiResponse<T>, PageResponse<T> chống OOM).
   - **Topic 4:** `4_ExceptionHandling&Validation.md` (Exception Hierarchy, @RestControllerAdvice, ErrorCode Enum, Jakarta Bean Validation).
   - **Topic 5:** `5_SpringDataJPA&Database.md` (Persistence Context, Dirty Checking, triệt tiêu N+1 Query, Index B-Tree kép).
   - **Topic 6:** `6_Transaction&Concurrency.md` (ACID, @Transactional Proxy, Pessimistic Lock, kỹ thuật Lock Ordering chống Deadlock, Idempotency).
   - **Topic 7:** `7_SpringSecurity&Redis.md` (Security Filter Chain, JWT Stateless, BCrypt Salt, Redis Token Blacklist thu hồi Token với TTL).
   - **Topic 8:** `8_Testing&Deployment.md` (Testing Pyramid, JUnit 5, Mockito Unit Test 37/37 pass, Slice Testing, Docker & Docker Compose).

---

## 🎯 5. MỤC TIÊU & HƯỚNG DẪN DÀNH CHO AI AGENT TRONG PHIÊN LÀM VIỆC TIẾP THEO
- **Mục tiêu chính của người dùng:** Chuẩn bị phỏng vấn vị trí Java Backend Intern / Fresher, tự tin trả lời lưu loát về toàn bộ kiến thức Java Core, Spring Boot và bảo vệ các kỹ thuật triển khai trong dự án SmartBank.
- **Phong cách trả lời:** Đi thẳng vào trọng tâm, giải thích bản chất (under the hood), so sánh ưu/nhược điểm và luôn gắn liền với ví dụ thực tế trong dự án SmartBank.
