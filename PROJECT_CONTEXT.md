# SmartBank — context đã đối chiếu mã nguồn

Ngày khảo sát: 20/10/2026. Đây là bộ nhớ trong repo để các phiên sau đọc lại;
không thay thế việc kiểm tra code khi dự án thay đổi.

## Mục tiêu và phạm vi

SmartBank là backend REST mô phỏng ngân hàng mini: khách hàng, nhân viên, tài khoản,
nạp/rút/chuyển tiền, sao kê, đăng nhập và refresh token. Người dùng dùng dự án để
chuẩn bị phỏng vấn Java Backend Intern / Fresher. Trả lời bằng tiếng Việt, giải thích
cơ chế bên dưới, trade-off và ví dụ cụ thể trong dự án.

Khảo sát ban đầu đã đọc toàn bộ 61 file Java production và 6 file Java test, cùng `GEMINI.md`, README,
POM, cấu hình ứng dụng local, Docker/Compose và các tài liệu phụ. Không có frontend
trong mã nguồn ứng dụng hiện tại; thư mục static/templates hiện rỗng.
`JavaIntern.tex` là CV, `pdf-styles.css` phục vụ xuất tài liệu, không thuộc runtime.
Không cần đọc `.env` để nắm kiến trúc; không lưu bí mật vào context.

## Stack và tổ chức

- Maven, Java 21, Spring Boot **3.4.4**; group `com.smartbank`, package Java
  **`com.SmartBank`**, artifact `SmartBank:0.0.1-SNAPSHOT`.
- Spring Web, Data JPA, Security, Validation, Cache, Data Redis, AOP.
- MySQL 8.0; Redis 7.2 Alpine trong Compose.
- JJWT 0.12.6; MapStruct 1.6.3; Lombok 1.18.38; binding Lombok/MapStruct 0.2.0.
- Entry point: `src/main/java/com/SmartBank/SmartBankApplication.java`.
- `controller/v1`: 4 REST controllers. `service`: 4 interfaces nghiệp vụ và
  TokenBlacklistService; `service/impl`: triển khai nghiệp vụ và RedisTokenBlacklistService.
  `repository`: 5 Spring Data repositories.
- `dto/request`, `dto/response`, `mapper`, `entity/enums`, `exception`, `security`,
  `security/handler`, `security/ratelimit`, `config` chia trách nhiệm tương ứng.
- Constructor injection qua Lombok; mapper là bean Spring được sinh lúc compile.
  DTO hiện là class Lombok, không phải Java record.

## Mô hình dữ liệu

| Entity | Vai trò và quan hệ |
| --- | --- |
| Customer | Hồ sơ + username/password/enabled; trạng thái ACTIVE/LOCKED; 1-N Account, LAZY, cascade ALL |
| Employee | Đăng nhập nhân viên/admin; role EMPLOYEE hoặc ADMIN, enabled |
| Account | Thuộc Customer qua customer_id, LAZY; số tài khoản unique; SAVINGS/CHECKING; ACTIVE/FROZEN/CLOSED |
| Transaction | DEPOSIT/WITHDRAW/TRANSFER; sourceAccount bắt buộc, targetAccount tùy chọn, đều LAZY |
| RefreshToken | UUID token unique, username, role, expiresAt, revoked; không có FK entity tới Customer/Employee |

Tiền dùng BigDecimal, cột DECIMAL(18,2). Entity dùng IDENTITY; thời gian tạo được gán
bởi @PrePersist. Transaction không có trường status SUCCESS/FAILED.
Nạp/rút đều lưu tài khoản thao tác ở sourceAccount, targetAccount = null.

Transaction có index `(source_account, created_at)`, `(target_account, created_at)`,
`(type, created_at)` và `created_at`. Account có unique account_number và các index
status, customer_id/status, type, created_at. Customer có unique email/phone/username,
index full_name và status/created_at. Employee và RefreshToken cũng có index phụ.
`CustomerRepository.findAllCustomersWithAccounts()` dùng LEFT JOIN FETCH + DISTINCT;
không thấy @EntityGraph trong code hiện tại.

## Luồng nghiệp vụ

- AccountServiceImpl: tạo tài khoản cho customerId hoặc username đang đăng nhập,
  sinh số 10 chữ số bằng Math.random và kiểm tra exists; xem, freeze, close, nhóm theo
  khách hàng. Class readOnly transaction, method ghi override @Transactional.
- CustomerServiceImpl: tạo hồ sơ có kiểm tra trùng email/phone/username, mã hóa password;
  đọc/cập nhật; xóa mềm bằng status LOCKED. Update chỉ sửa thông tin hồ sơ, không sửa
  username/password dù dùng chung CustomerRequest có các trường này bắt buộc.
- TransactionServiceImpl: nạp/rút/chuyển trong @Transactional; tải tài khoản qua
  `AccountRepository.findByAccountNumberWithLock()` với PESSIMISTIC_WRITE; kiểm tra
  ACTIVE, kiểm tra đủ tiền cho rút/chuyển; cập nhật số dư và lưu Transaction.
- Chuyển tiền chặn cùng tài khoản; khóa theo **thứ tự từ điển của accountNumber**
  bằng String.compareTo, sau đó ánh xạ lại source/target. Không sắp xếp theo ID.
  Lock ordering giảm rủi ro deadlock của luồng chuyển chéo; không kết luận hệ thống
  không thể có deadlock trong mọi tình huống.
- Mã giao dịch hiện là `TXN` + System.currentTimeMillis(), cột unique.
- DTO nạp/rút yêu cầu 1.000–500.000.000; chuyển tiền tối thiểu 1.000, chưa có mức tối đa.
  Validation được kích hoạt ở controller qua @Valid.
- Sao kê dùng truy vấn source OR target account number, PageRequest, createdAt DESC;
  mặc định page=0, size=10. PageResponse có content/pageNo/pageSize/totalElements/
  totalPages/last. Chưa giới hạn size tối đa.

## API và phân quyền thực tế

| Nhóm | Routes và hành vi |
| --- | --- |
| Auth public | POST /auth/register, /auth/login, /auth/refresh |
| Logout | POST /api/auth/logout, cần authentication |
| Nhân viên | POST /admin/employees, ADMIN |
| Customer | /api/v1/customers: POST, GET list, GET /{id}, PUT /{id}, DELETE /{id}, GET /{id}/accounts; ADMIN |
| Account cá nhân | GET/POST /api/v1/accounts/my; CUSTOMER, tạo chỉ nhận type |
| Account quản lý | /api/v1/accounts: POST, GET grouped, GET /{id}, PATCH /{id}/freeze, PATCH /{id}/close; filter yêu cầu EMPLOYEE/ADMIN |
| Transaction | POST /api/v1/transactions/deposit, /withdraw, /transfer; CUSTOMER/EMPLOYEE/ADMIN |
| Sao kê | GET /api/v1/transactions/account/{accountNumber}; ADMIN/EMPLOYEE hoặc owner qua @PreAuthorize |

Đọc cả SecurityConfig lẫn @PreAuthorize để xác định quyền cuối cùng. GET account
/{id} có bất nhất: filter chặn CUSTOMER, trong khi method chỉ cho ADMIN hoặc owner;
EMPLOYEE thông thường cũng không đáp ứng method check.

Success responses bọc ApiResponse<T>: code/message/data/timestamp, bỏ field null.
AppException + ErrorCode được GlobalExceptionHandler chuyển sang ErrorResponse
code/message/path/timestamp. Validation trả 400; catch-all trả 500. Hai security
handlers trả payload có `status`, nên chưa thống nhất hoàn toàn với ErrorResponse.

## Authentication, Redis, caching

- Stateless, CSRF disabled, BCrypt. JwtAuthenticationFilter chạy trước
  UsernamePasswordAuthenticationFilter, kiểm tra chữ ký/hạn JWT và Redis blacklist rồi lấy username/role
  để tạo Authentication; không truy vấn trạng thái user mỗi request.
- Login tự tìm Employee trước, rồi Customer, kiểm tra PasswordEncoder.matches,
  sau đó kiểm tra enabled; Customer còn bị chặn khi status LOCKED. User không hoạt
  động và đúng password bị ném AppException(USER_NOT_ACTIVE), code 1104/HTTP 403,
  trước khi phát JWT hoặc lưu refresh token. Sai password hoặc không tìm thấy username
  đều trả INVALID_USERNAME_OR_PASSWORD, code 1103/HTTP 401. Luồng này vẫn tự xác thực,
  không gọi DaoAuthenticationProvider. Kiểm tra password trước giúp không tiết lộ
  trạng thái disabled cho người chưa biết password; chưa có dummy BCrypt cho username
  không tồn tại nên không khẳng định đã loại bỏ timing enumeration.
- Refresh: kiểm tra tồn tại/revoked/hết hạn; revoke token cũ, tạo cặp mới.
  Chưa có transaction/lock bao trùm rotation hay cơ chế thu hồi cả token family khi reuse.
- Logout nhận access token từ Authorization: Bearer, lưu blacklist qua
  TokenBlacklistService/RedisTokenBlacklistService, sau đó delete refresh tokens theo username.
  Redis key `token_blacklist:<SHA-256 của token>`, value `1`, SET kèm TTL bằng thời gian
  còn lại đến exp. Token hết hạn không cần ghi Redis. Không lưu bearer token thô.
- JWT filter từ chối token trong blacklist bằng 401/INVALID_TOKEN; lỗi truy cập Redis
  trả 503/AUTH_SERVICE_UNAVAILABLE (1110), không bỏ qua blacklist. Logout ghi Redis lỗi
  cũng trả 503. JWT mới có jti UUID để phân biệt các lần cấp token sát nhau.
- Chỉ access token gửi khi logout bị blacklist; access token khác của cùng username
  vẫn còn hiệu lực tới expiry. Tất cả refresh token của username bị xóa như trước.
  Redis và MySQL không có transaction phân tán: nếu xóa refresh token thất bại sau
  ghi Redis, access token vẫn bị thu hồi nhưng logout trả lỗi.
- RedisConfig: cache accounts 5 phút, customers 30 phút, accounts_customers 10 phút,
  default 10 phút; JSON serialization. TTL này được đặt trong custom cache manager.
- Các giao dịch xóa toàn bộ 3 cache qua @CacheEvict(allEntries=true).
  Customer update dùng CachePut theo ID; delete dùng CacheEvict theo ID.
- RateLimitAspect: @Before, key `rate_limit:<remoteAddr>:<URI>`, Lua INCR + EXPIRE
  nguyên tử trong Redis, cửa sổ tính từ request đầu. Register 3/60s, login 5/60s,
  mỗi endpoint ghi giao dịch 10/60s; vượt ngưỡng trả 429. Lỗi Redis được ném tiếp.

## Chạy và kiểm thử

- Windows: `./mvnw.cmd test`, `./mvnw.cmd spring-boot:run`.
- Compose: `docker compose up --build` hoặc chỉ `docker compose up -d mysql redis`.
  Host ports: app 8080, MySQL 3307→3306, Redis 6379. Container app dùng service names
  mysql/redis; JVM local dùng localhost và port host tương ứng.
- Docker build nhiều stage JDK21→JRE21, package -DskipTests, runtime non-root.
- `.env` và `src/main/resources/application.properties` đang bị gitignore.
  Compose nạp `.env`; code/config hiện không khai báo cơ chế tự nạp `.env` khi chạy JVM
  trực tiếp, nên phải truyền environment phù hợp qua shell/IDE.
- Local properties có open-in-view=false; JWT mặc định 15 phút và refresh 7 ngày.
  **DDL mặc định là create**: trước khi chạy app/context test trên DB có dữ liệu,
  phải kiểm tra cấu hình đích và DDL để tránh tái tạo schema ngoài ý muốn.
- Không có Flyway/Liquibase hoặc test profile riêng trong source đã khảo sát.
- Sau triển khai blacklist: 43 unit tests và 1 contextLoads (@SpringBootTest).
  TokenBlacklistTest có 7 test, dùng JWT thật và Redis mock: logout/replay 401, token
  khác còn dùng được, hash/TTL, token hết hạn, lỗi Redis đọc/ghi, JWT sai và request
  không có bearer. Các service/JWT unit test cũ vẫn giữ nguyên.
- Ngày 20/10/2026 đã chạy thành công **43 tests, 0 failures/errors** bằng:
  `mvn -o "-Dmaven.repo.local=C:/Users/ADMIN/.m2/repository" "-Dtest=*ServiceTest,JwtTokenProviderTest,TokenBlacklistTest" test`.
  Wrapper gặp lỗi khởi động nên dùng Maven cài sẵn. Không chạy contextLoads vì local
  DDL mặc định create; chưa kiểm thử tích hợp Redis/MySQL thật trong lần thay đổi này.
  Test Mockito không chứng minh DB locking, rollback hay TTL thực tế trên Redis.

## Các điểm cần nhớ khi giải thích hoặc sửa về sau

Các điểm sau quan sát từ code, chưa được sửa hay kiểm chứng bằng kiểm thử runtime
trong phiên này:

- Chưa có User entity, IdempotencyRecord hoặc xử lý idempotency. GEMINI.md mô tả
  các thành phần này nhưng không khớp implementation. Blacklist đã được bổ sung
  theo yêu cầu người dùng ngày 20/10/2026.
- Endpoint nạp/rút/chuyển mới kiểm tra role; chưa kiểm tra CUSTOMER có sở hữu tài khoản
  bị trừ tiền hay không. Owner check hiện có ở đọc account/sao kê.
- Login đã kiểm tra enabled/status sau bản sửa ngày 29/10/2026. Refresh và JWT filter
  vẫn chưa tải lại trạng thái user: thay đổi này chưa thu hồi token đã cấp hoặc chặn
  refresh của user bị vô hiệu hóa.
- Tạo employee chỉ kiểm tra username trong bảng Employee; tạo customer qua admin chỉ
  kiểm tra bảng Customer. Đăng ký public mới kiểm tra cả hai. Có khả năng trùng danh
  tính giữa hai bảng, trong khi login ưu tiên Employee.
- Rotation chưa atomic trước hai request đồng thời; không tuyên bố đã chống mọi replay.
- Cache invalidation thiếu ở create/freeze/close account và một số cache danh sách
  customer; không tuyên bố đảm bảo nhất quán cache hoàn toàn.
- Transaction code theo millisecond có thể trùng; kiểm tra exists trước tạo số account
  không thay thế unique constraint và xử lý race.
- Customer và Employee đã có @Builder.Default cho enabled=true; truyền enabled(false)
  tường minh vẫn được giữ. Điều này sửa đường tạo Customer qua MapStruct vốn bỏ qua
  enabled. Không tự cập nhật các record enabled=false đã tồn tại trong DB.
  Initializer accountList của Customer vẫn chưa có @Builder.Default.
- Chưa có benchmark chứng minh giảm query 40–60%, hay kiểm thử concurrency chứng minh
  các khẳng định hiệu năng/deadlock trong CV. Dùng chúng như mục cần xác minh.
- Pagination không tự đảm bảo không OOM khi size do client quyết định; LAZY không tự
  giải quyết mọi N+1. Kiểm tra query và mapping cụ thể khi tối ưu.

Giữ nguyên GEMINI.md trong phiên khảo sát vì file đã có chỉnh sửa của người dùng.
Context được cập nhật sau triển khai Redis blacklist theo yêu cầu người dùng.

## Xác minh bản sửa login ngày 29/10/2026

- Đã chạy thành công: `mvn -o -e "-Dmaven.repo.local=C:/Users/ADMIN/.m2/repository" "-Dtest=*ServiceTest,JwtTokenProviderTest,TokenBlacklistTest,AuthLoginResponseTest" test`.
- Kết quả: **58 tests, 0 failures, 0 errors, 0 skipped**. AuthServiceTest có 18 case
  thực thi (bao gồm parameterized tests); CustomerServiceTest có 8; AuthLoginResponseTest
  có 4. Các test còn lại: Account 5, Transaction 8, JWT 8, blacklist 7.
- Bao phủ Employee/ADMIN disabled, Customer disabled/LOCKED, đúng/sai password,
  không tồn tại username, không phát/lưu token khi bị chặn, user hoạt động vẫn login
  được và Customer tạo qua generated MapStruct mapper có enabled=true.
- AuthLoginResponseTest dùng MockMvc standalone với controller/service/advice thật,
  repository/encoder/provider mock; xác nhận HTTP 401/403 và ErrorResponse. Không chạy
  security filter chain/rate-limit aspect hay SQL/Redis thật. Không chạy contextLoads.
- Lần chạy đầu gặp lỗi testCompile liên quan JAR antlr4-runtime trong Maven cache;
  chạy lại cùng bộ test với `-e` thành công. Không thay dependency để xử lý lỗi này.
