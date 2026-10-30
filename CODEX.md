# SmartBank — Context cho AI

> Cập nhật ngày 2026-10-30, đối chiếu README, toàn bộ 64 file Java trong `src/main`, 14 file test trong `src/test`, cấu hình và script build/deploy.
> Mục đích: giúp AI bắt đầu conversation mới hiểu dự án trước khi phân tích hoặc sửa code.
> Cách dùng: yêu cầu AI **“Đọc CODEX.md trước khi thực hiện yêu cầu.”** File này là tài liệu context; không giả định mọi công cụ AI tự động nạp file theo tên.

## 1. Đọc phần này trước

- SmartBank là **backend REST API mô phỏng ngân hàng mini**, một ứng dụng Spring Boot, một Maven module. Repo hiện không có frontend.
- Nghiệp vụ: đăng ký/đăng nhập, quản lý khách hàng, mở/đóng/phong tỏa tài khoản, nạp/rút/chuyển tiền nội bộ, sao kê, refresh token và logout.
- Định danh đăng nhập nằm ở **hai entity `Customer` và `Employee`**. Không có entity `User`, `IdempotencyRecord`, hoặc trạng thái thành công/thất bại trên `Transaction`.
- Role thực tế: `CUSTOMER`, `EMPLOYEE`, `ADMIN`; không có kế thừa role. ADMIN không được dùng endpoint `/api/v1/accounts/my`.
- Tiền dùng `BigDecimal`, lưu `DECIMAL(18,2)`. Giao dịch ghi tiền dùng transaction DB và pessimistic write lock.
- Khi tài liệu mâu thuẫn với implementation, đọc lại code và test liên quan. Không suy diễn tính năng từ tên dự án hoặc mô tả “production-ready” trong README.
- `GEMINI.md` là context cũ, có nhiều thông tin sai với code hiện tại; xem mục 12. Không sao chép nguyên nội dung đó làm sự thật.
- Đây là snapshot kỹ thuật, không phải backlog đã được giao. Các giới hạn ở mục 11 chưa được sửa trong lần viết tài liệu này.

## 2. Stack và bản đồ mã nguồn

| Thành phần | Phiên bản/cấu hình trong repo |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring | Web, Data JPA, Security, Validation, Cache, Data Redis, AOP; phiên bản theo Spring Boot |
| Database | MySQL 8.0 trong Compose; H2 chỉ ở test scope |
| Redis | `redis:7.2-alpine` |
| JWT | JJWT 0.12.6 |
| Mapping | MapStruct 1.6.3, `componentModel = "spring"` |
| Boilerplate | Lombok 1.18.38; lombok-mapstruct-binding 0.2.0 |
| Build | Maven Wrapper 3.3.4 tải Maven 3.9.14 |
| Test | JUnit Jupiter, Mockito, AssertJ, Spring Test/Security Test |

Nguồn phiên bản: [pom.xml](pom.xml), [wrapper properties](.mvn/wrapper/maven-wrapper.properties), [docker-compose.yml](docker-compose.yml).

```text
src/main/java/com/SmartBank/         # Chú ý chữ S/B viết hoa trong package
  SmartBankApplication.java         # @SpringBootApplication, @EnableCaching
  config/                          # SecurityConfig, RedisConfig
  controller/v1/                   # Auth, Customer, Account, Transaction controllers
  dto/request/                     # Input và Jakarta Bean Validation
  dto/response/                    # ApiResponse, PageResponse, các domain response
  entity/ + entity/enums/           # 5 entity, 5 enum
  exception/                       # ErrorCode, AppException, ErrorResponse, advice
  mapper/                          # AccountMapper, CustomerMapper, TransactionMapper
  repository/                      # 5 JpaRepository; JPQL, pagination, row lock
  security/                        # JWT, kiểm tra identity/state và ownership
    handler/                       # HTTP 401/403 của security filter chain
    ratelimit/                     # @RateLimit + AOP + Redis Lua
  service/                         # 5 interface
    impl/                          # 5 implementation
src/main/resources/application.properties
src/test/java/com/SmartBank/        # Unit, HTTP/security slice, H2 JPA tests
```

Luồng chính: HTTP → JWT filter → URL authorization → controller/service method authorization → service → repository/mapper → MySQL. Rate limiting là aspect trên các controller method có annotation; cache là Spring Cache dùng Redis.

Controllers phụ thuộc service interface; logic nghiệp vụ ở `service/impl`. Dependency injection chủ yếu qua constructor với Lombok `@RequiredArgsConstructor`. Mapper implementation được sinh lúc compile trong `target/generated-sources/annotations`; sửa mapper interface, không sửa file sinh tự động.

## 3. Mô hình dữ liệu

| Entity | Dữ liệu và quan hệ cần nhớ |
| --- | --- |
| `Customer` | Hồ sơ (`fullName`, `email`, `phone`, `address`, `dateOfBirth`), `username`, mật khẩu BCrypt, `enabled`, `status`, `createdAt`. Email/phone/username có unique riêng. Một customer có nhiều accounts, collection LAZY và `cascade = ALL`. |
| `Employee` | Username unique, mật khẩu BCrypt, role và enabled; dùng cho EMPLOYEE/ADMIN. |
| `Account` | Số tài khoản unique, không update, sinh ngẫu nhiên 10 chữ số; type, balance, status, createdAt. `ManyToOne` LAZY đến Customer, FK `customer_id`. |
| `Transaction` | Code unique, type, amount, description, createdAt. Source account bắt buộc, target account nullable; cả hai LAZY. Nạp/rút cũng ghi tài khoản thao tác ở source; target null. |
| `RefreshToken` | Token UUID dạng chuỗi lưu trực tiếp trong DB, username, role, expiresAt, revoked. Không phải JWT và không có JPA relationship/FK đến Customer/Employee. |

Enum chính xác:

- `CustomerStatus`: `ACTIVE`, `LOCKED`.
- `AccountStatus`: `ACTIVE`, `FROZEN`, `CLOSED`.
- `AccountType`: `SAVINGS`, `CHECKING`.
- `TransactionType`: `DEPOSIT`, `WITHDRAW`, `TRANSFER`.
- `Role`: `EMPLOYEE`, `ADMIN`, `CUSTOMER`.

`@PrePersist` gán timestamp và mặc định: Customer ACTIVE; Account ACTIVE, balance zero nếu chưa có. `enabled` mặc định true qua `@Builder.Default`. Thời gian chủ yếu là `LocalDateTime.now()`; không mặc định hiểu mọi timestamp JSON là UTC.

Index phục vụ sao kê nằm trên `(source_account, created_at)` và `(target_account, created_at)`. Các entity còn khai báo index cho status/type/createdAt và một số tổ hợp; đọc annotation khi đổi schema. Không có Flyway/Liquibase; cấu hình local mặc định `ddl-auto=update`.

## 4. API và phân quyền thực tế

Nguồn đối chiếu: [SecurityConfig](src/main/java/com/SmartBank/config/SecurityConfig.java) và [controllers](src/main/java/com/SmartBank/controller/v1).

Trong bảng, “staff” nghĩa là EMPLOYEE hoặc ADMIN; “owner” phải là CUSTOMER đã xác thực và username trùng chủ tài khoản.

| Method và route | Quyền | Hành vi chính |
| --- | --- | --- |
| POST `/auth/register` | Public | Tạo Customer từ username/email/password; HTTP 201 |
| POST `/auth/login` | Public | Trả access token, refresh token, username, role; HTTP 200 |
| POST `/auth/refresh` | Public, xác minh refresh token | Rotation và trả cặp token mới; HTTP 200 |
| POST `/api/auth/logout` | Đã xác thực | Bearer header, không body; blacklist access token hiện tại, xóa refresh tokens theo username |
| POST `/admin/employees` | ADMIN | Tạo EMPLOYEE hoặc ADMIN; cấm role CUSTOMER; HTTP 201 có body |
| POST `/api/v1/customers` | ADMIN | Tạo hồ sơ và credentials; HTTP 201 |
| GET `/api/v1/customers` | ADMIN | Danh sách customer có phân trang |
| GET `/api/v1/customers/{id}` | ADMIN | Chi tiết customer |
| PUT `/api/v1/customers/{id}` | ADMIN | Cập nhật thông tin hồ sơ |
| DELETE `/api/v1/customers/{id}` | ADMIN | Đổi status thành LOCKED, không xóa row; HTTP 200 có body |
| GET `/api/v1/customers/{id}/accounts` | ADMIN | Danh sách account của customer, không phân trang |
| POST `/api/v1/accounts` | Staff | Tạo account theo `customerId` và `type`; HTTP 201 |
| GET `/api/v1/accounts` | Staff | Phân trang nhóm account theo customer |
| GET `/api/v1/accounts/{id}` | Staff hoặc owner | Chi tiết account; ownership ở controller `@PreAuthorize` |
| PATCH `/api/v1/accounts/{id}/freeze` | Staff | Đổi status thành FROZEN |
| PATCH `/api/v1/accounts/{id}/close` | Staff | Đổi status thành CLOSED |
| POST `/api/v1/accounts/my` | Chỉ CUSTOMER | Chỉ nhận `type`; lấy owner từ Authentication; HTTP 201 |
| GET `/api/v1/accounts/my` | Chỉ CUSTOMER | Danh sách account của người đăng nhập, không phân trang |
| POST `/api/v1/transactions/deposit` | Staff hoặc owner | Nạp tiền; HTTP 201 |
| POST `/api/v1/transactions/withdraw` | Staff hoặc owner | Rút tiền; HTTP 201 |
| POST `/api/v1/transactions/transfer` | Staff hoặc owner của source | Chuyển nội bộ; không cần sở hữu target; HTTP 201 |
| GET `/api/v1/transactions/account/{accountNumber}` | Staff hoặc owner | Sao kê source OR target, phân trang |

Các method ghi tiền kiểm tra quyền tại **service** bằng `@PreAuthorize`, trước khi thay đổi số dư. `AccountSecurity` kiểm tra cả role CUSTOMER và username, không chỉ so tên. URL rules và method rules phải cùng cho phép. Giữ matcher `/accounts/my` trước matcher `/{id}` và wildcard.

### Response contract

- Success luôn được controller bọc bằng `ApiResponse<T>`: `code` (200/201), `message`, `data`, `timestamp`. `data` null được bỏ khỏi JSON. Token nằm trong `data.accessToken`/`data.refreshToken`.
- `PageResponse<T>`: `content`, `pageNo`, `pageSize`, `totalElements`, `totalPages`, `last`; danh sách phân trang nằm ở `data.content`.
- Customer/account/transaction response không trả entity trực tiếp; `CustomerResponse` không chứa credentials. `AccountResponse` có customerId/customerName; `TransactionResponse` chứa số tài khoản source/target.
- `AppException` → `ErrorResponse`: `code`, `message`, `path`, `timestamp`, HTTP status lấy từ `ErrorCode`.
- Bean validation trả HTTP 400 với `code=400`; query parameter sai kiểu trả HTTP 400/code 1001; lỗi tổng quát trả HTTP 500/code 500.
- Lỗi method authorization qua advice trả HTTP 403/code 1003. Hai security handler ở filter chain có format khác: `status`, `message`, `path`, `timestamp`, không có `code`. Chưa có một schema lỗi thống nhất hoàn toàn.

### Validation và pagination

- Deposit/withdraw: `accountNumber` không blank; amount từ 1.000 đến 500.000.000, description tối đa 255 ký tự.
- Transfer: source/target không blank, amount tối thiểu 1.000; **chưa có maximum amount** trong DTO.
- `CustomerRequest`: fullName bắt buộc/tối đa 100; email bắt buộc/đúng định dạng; phone dài 10–11; dateOfBirth bắt buộc/trong quá khứ; username/password bắt buộc. Phone hiện kiểm tra độ dài, không kiểm tra toàn chữ số.
- `RegisterRequest` chỉ `@NotBlank` cho username/email/password, chưa có `@Email` hoặc giới hạn độ dài mật khẩu.
- List customers và list account groups: page mặc định 0, size 10, page >= 0, size 1–100; sai phạm vi trả 400/code 1001. Trang quá cuối trả content rỗng và giữ tổng thực tế.
- List customers: sort `id DESC`, lấy một page customer rồi aggregate count account cho các ID trong page. Không load collection để đếm `totalAccounts`, không cache list.
- List account groups: **paginate customer, không paginate account**; customer `id DESC`, account mỗi nhóm `id ASC`. Bao gồm customer LOCKED và customer không có account (`accounts: []`). Lấy accounts cho cả page bằng một query JOIN FETCH customer, không paginate collection fetch join.
- Sao kê: source OR target, sort `createdAt DESC`, mặc định page 0/size 10. Hiện chưa có kiểm tra phạm vi/giới hạn size như hai list trên; không áp dụng nhầm contract size <= 100 cho sao kê.

## 5. Authentication, token và trạng thái người dùng

Nguồn: [AuthServiceImpl](src/main/java/com/SmartBank/service/impl/AuthServiceImpl.java), [JwtAuthenticationFilter](src/main/java/com/SmartBank/security/JwtAuthenticationFilter.java), [UserAccessValidator](src/main/java/com/SmartBank/security/UserAccessValidator.java).

1. Login tìm username ở cả hai bảng. Nếu trùng cả hai hoặc không tồn tại → 401/code 1103. So BCrypt trước khi tiết lộ trạng thái; đúng mật khẩu nhưng inactive → 403/code 1104.
2. JWT access token ký HMAC, có `sub=username`, `role`, `iat`, `exp`, `jti` UUID. Expiration cấu hình bằng milliseconds, mặc định 15 phút.
3. Refresh token là UUID lưu DB, mặc định 7 ngày. Refresh kiểm tra tồn tại/revoked/hết hạn, xác minh identity hiện tại rồi revoke token cũ và tạo cặp mới. Token hết hạn bị xóa khi được dùng để refresh.
4. Bearer filter kiểm tra chữ ký/hạn → Redis blacklist → `UserAccessValidator` đọc hai bảng → tạo Authentication có authority `ROLE_<role>`.
5. UserAccessValidator từ chối customer LOCKED hoặc disabled và employee disabled (403/1104); identity không tồn tại, role không hỗ trợ, staff đổi role, hoặc username trùng hai bảng (401/1105).
6. Logout ghi `token_blacklist:<SHA-256(accessToken)>` vào Redis, giá trị `1`, TTL bằng thời gian còn lại; không lưu bearer token thô. Sau đó xóa **tất cả refresh token theo username**. Access token khác của cùng user vẫn dùng được nếu hợp lệ.

Redis lỗi trong blacklist, hoặc DB lỗi trong bước UserAccessValidator, làm request bị từ chối với 503/code 1110. Không suy rộng rằng mọi lỗi DB/Redis ở mọi endpoint đều được map sang 503.

JWT là sessionless nhưng mỗi bearer request vẫn đọc DB và Redis. `/auth/**` public không có nghĩa là bỏ qua bearer filter: nếu gửi kèm bearer token lỗi, filter vẫn có thể từ chối request.

Khóa customer chặn token khi trạng thái không active; không revoke vĩnh viễn mọi token. Re-enable có thể làm token còn hạn dùng lại được. Request đã vượt kiểm tra không bị hủy ngược khi user bị khóa sau đó.

`CustomUserDetailsService`/`DaoAuthenticationProvider` vẫn được đăng ký, nhưng endpoint login hiện gọi logic riêng của `AuthServiceImpl`, không gọi `AuthenticationManager`. CustomUserDetailsService còn ưu tiên Employee và chỉ check enabled; không nhầm nó với luồng kiểm tra identity/state đang dùng cho JWT/login.

## 6. Các quy tắc nghiệp vụ khi sửa code

Nguồn: [TransactionServiceImpl](src/main/java/com/SmartBank/service/impl/TransactionServiceImpl.java), [AccountServiceImpl](src/main/java/com/SmartBank/service/impl/AccountServiceImpl.java), [CustomerServiceImpl](src/main/java/com/SmartBank/service/impl/CustomerServiceImpl.java).

- Deposit/withdraw khóa một account bằng `AccountRepository.findByAccountNumberWithLock`, có `@Lock(PESSIMISTIC_WRITE)`.
- Transfer cấm source = target; khóa hai account theo **thứ tự từ điển của chuỗi accountNumber**, không theo ID. Sau đó xác định lại source/target.
- Account tham gia giao dịch phải ACTIVE. Withdraw/transfer phải đủ số dư. Update số dư và lưu transaction nằm trong cùng `@Transactional`; lỗi runtime làm rollback.
- Deposit cộng vào source; withdraw trừ source; transfer trừ source/cộng target và ghi **một** Transaction có cả source/target. Không tự biến luồng này thành hai transaction record.
- Không dùng float/double cho tiền. Phân biệt Bean Validation ở HTTP boundary với kiểm tra trong service; gọi object service trực tiếp trong unit test không kích hoạt Spring proxy/validation/transaction.
- Account mới có balance zero và ACTIVE. Staff tạo theo customerId, customer tự tạo theo username từ Authentication.
- Freeze/close hiện chỉ đổi status, chưa kiểm tra state transition hoặc số dư bằng zero. Không có endpoint unfreeze/reopen.
- Xóa customer thực chất đổi `status=LOCKED`; không xóa account/transaction liên quan và không đổi `enabled`.
- Update customer chỉ đổi fullName/email/phone/address/dateOfBirth. Request vẫn bắt buộc username/password nhưng implementation bỏ qua hai trường này; đây không phải API đổi credentials.
- Ba đường tạo identity (`register`, `createEmployee`, `CustomerService.create`) kiểm tra username ở cả hai bảng. Global uniqueness chỉ được kiểm tra bằng application code, chưa có constraint dùng chung giữa bảng.

## 7. Redis cache và rate limiting

Nguồn: [RedisConfig](src/main/java/com/SmartBank/config/RedisConfig.java), [RateLimitAspect](src/main/java/com/SmartBank/security/ratelimit/RateLimitAspect.java), annotations trong service/controller.

| Cache | Key và nơi dùng | TTL cấu hình trong Java |
| --- | --- | --- |
| `accounts` | Account detail: id; getAccountsByUsername: username (default single-argument key) | 5 phút |
| `customers` | Customer detail: id; update dùng CachePut, soft delete dùng CacheEvict | 30 phút |
| `accounts_customers` | Danh sách account theo customerId | 10 phút |

CacheManager mặc định 10 phút, không cache null, serialize value bằng `GenericJackson2JsonRedisSerializer`. Bean CacheManager tự tạo hardcode TTL; `CACHE_TTL`/`spring.cache.redis.time-to-live` hiện không điều khiển các TTL đó.

Mỗi deposit/withdraw/transfer thành công dùng `@CacheEvict(allEntries=true)` cho cả ba cache. Create account, freeze và close **chưa invalidation cache**; đọc ngay sau mutation có thể thấy dữ liệu cũ. Khi sửa mutation, xem cả detail, list của owner, list theo customer và `totalAccounts`.

Rate limit dùng Redis Lua `INCR`, chỉ `EXPIRE` khi count = 1, tức cửa sổ tính từ request đầu tiên. Key `rate_limit:<request.getRemoteAddr()>:<requestURI>`, không theo user/token và không đọc X-Forwarded-For.

- Register: 3 request/60 giây.
- Login: 5 request/60 giây.
- Deposit, withdraw, transfer: mỗi URI 10 request/60 giây.
- Refresh/logout: không có `@RateLimit`.
- Vượt ngưỡng: HTTP 429/code 1504. Redis lỗi ở aspect được ném tiếp; generic advice có thể trả 500, khác cơ chế 503 ở blacklist.

## 8. Cấu hình và chạy local

Chạy từ thư mục gốc repo. [application.properties](src/main/resources/application.properties) import `optional:file:.env[.properties]`. `.env` và file properties này đang bị `.gitignore` loại khỏi Git và không được track ở snapshot này; checkout mới có thể thiếu cấu hình. Không ghi secret thật vào context, test output hoặc tài liệu.

| Biến | Ý nghĩa/default |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC và credentials; properties không đặt default |
| `REDIS_HOST`, `REDIS_PORT` | Mặc định localhost, 6379 |
| `REDIS_PASSWORD` | Không có default; Compose dùng để bật requirepass |
| `REDIS_TIMEOUT` | 2000ms |
| `JPA_DDL_AUTO`, `JPA_SHOW_SQL` | update, false |
| `CACHE_TTL` | 10m trong properties; xem lưu ý CacheManager ở mục 7 |
| `JWT_SECRET` | Secret ký HMAC, không có default; dùng giá trị riêng đủ mạnh cho JJWT |
| `JWT_EXPIRATION` | 900000 ms |
| `JWT_REFRESH_EXPIRATION` | 604800000 ms |

Compose chạy MySQL `3307:3306`, Redis `6379:6379`, app `8080:8080`; volume `mysql_data` và `redis_data`. App đợi hai healthcheck healthy. Dockerfile build bằng Temurin 21 JDK, chạy bằng 21 JRE với user `smartbank`; bước package trong Docker **skip tests**.

Chạy toàn bộ trong Docker, cấu hình JDBC host `mysql:3306`, Redis host `redis`:

```powershell
docker compose up --build
```

Chạy app trên máy và hạ tầng trong Docker:

```powershell
docker compose up -d mysql redis
# .env: DB_URL dùng localhost:3307, REDIS_HOST=localhost
.\mvnw.cmd spring-boot:run
```

JDBC mẫu không chứa mật khẩu: `jdbc:mysql://localhost:3307/SmartBank?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true`. Trong container đổi host/port thành `mysql:3306`. Đặt comment `.env` trên dòng riêng, nhất là với biến số expiration.

Build jar: `.\mvnw.cmd package`; chạy jar: `java -jar target/SmartBank-0.0.1-SNAPSHOT.jar`. Linux/macOS dùng `./mvnw`. Wrapper có thể cần mạng lần đầu để tải Maven/dependencies.

Không có seed/bootstrap ADMIN trong source. Register chỉ tạo CUSTOMER; không giả định có tài khoản admin mặc định. Khi cần bootstrap phải xử lý riêng và lưu password bằng BCrypt.

## 9. Test và cách xác minh

| Nhóm/file test | Kiểm tra gì và giới hạn |
| --- | --- |
| `AccountServiceTest`, `CustomerServiceTest`, `TransactionServiceTest` | Nghiệp vụ với mock repository/mapper; có test dùng CustomerMapper thật. Không chứng minh row locking hoặc rollback thật. |
| `AuthServiceTest` | Login/disabled/locked/duplicate identity, tạo user, refresh; có parameterized tests. |
| `AuthLoginResponseTest` | Controller + service + advice thật, dependencies mock; không chạy security chain/rate limit. |
| `CustomerPaginationTest`, `AccountGroupPaginationTest` | HTTP contract, defaults, bounds, page rỗng; MockMvc standalone không có security chain. |
| `CustomerPaginationJpaTest`, `AccountGroupPaginationJpaTest` | H2, repository/service/mapper thật, query count, không fetch collection khi paginate; cache in-memory. Không cần MySQL/Redis thật cho hai slice này. |
| `EndpointAuthorizationTest` | WebMvcTest với JWT/filter chain/method security/ownership/state check thật; auth/transaction services thật, repositories/blacklist mock. |
| `AccountSecurityTest`, `JwtTokenProviderTest` | Ownership/role và tạo/parse/validate JWT. |
| `TokenBlacklistTest` | Hash key, TTL truyền vào Redis, logout, fail-closed; Redis được mock, không kiểm chứng expiration trên Redis thật. |
| `SmartBankApplicationTests` | `@SpringBootTest contextLoads`; không có profile test riêng, phụ thuộc cấu hình runtime và DB khả dụng. |

```powershell
# Toàn bộ suite, cần chuẩn bị cấu hình/hạ tầng cho contextLoads
.\mvnw.cmd test

# Các test còn lại, bỏ full application context
.\mvnw.cmd "-Dtest=!SmartBankApplicationTests" test

# Ví dụ khi sửa security
.\mvnw.cmd "-Dtest=EndpointAuthorizationTest,AccountSecurityTest,TokenBlacklistTest,AuthServiceTest,JwtTokenProviderTest,AuthLoginResponseTest" test

# Ví dụ khi sửa pagination
.\mvnw.cmd "-Dtest=CustomerPaginationTest,CustomerPaginationJpaTest,AccountGroupPaginationTest,AccountGroupPaginationJpaTest" test
```

Surefire reports nằm trong `target/surefire-reports/`. Không lấy số test/pass từ `GEMINI.md` hoặc report cũ làm kết quả hiện tại. Lần tạo CODEX.md này chỉ đọc/đối chiếu source, **không chạy Maven tests hay khởi động dịch vụ**, nên chưa xác nhận trạng thái build/runtime hiện tại.

## 10. Chỉ dẫn làm việc cho AI ở phiên tiếp theo

1. Đọc context này, kiểm tra `git status`, rồi đọc source/test đúng phạm vi yêu cầu; giữ nguyên thay đổi sẵn có của người dùng.
2. Nếu đổi API: đối chiếu controller, DTO, service, URL rules, method security và test contract. Không dựa riêng vào payload ví dụ của README.
3. Nếu đổi tiền/giao dịch: giữ `BigDecimal`, transaction boundary, lock ordering, kiểm tra owner/status/balance và cache invalidation; cần test phù hợp với rủi ro thay đổi.
4. Nếu đổi auth: xem cả login/access/refresh/logout, trùng username, disabled/LOCKED, role thay đổi và lỗi DB/Redis. Không bỏ kiểm tra blacklist/state chỉ để request chạy được.
5. Nếu đổi query phân trang: giữ nghĩa của `totalElements`, sort và giới hạn size; không đưa collection fetch join vào query có pagination. Xem hai JPA tests trước khi tối ưu.
6. Giữ kiến trúc controller → service interface → implementation → repository/mapper; dùng `AppException(ErrorCode)` cho lỗi nghiệp vụ phù hợp, không trả entity chứa credentials.
7. Chạy kiểm tra liên quan và báo chính xác lệnh/kết quả, phân biệt lỗi môi trường với lỗi code. Không khẳng định đã pass nếu chưa chạy.
8. Cập nhật CODEX.md/README khi thay đổi contract, kiến trúc, cấu hình hoặc khắc phục giới hạn đã ghi nhận. Không lưu token/password hoặc log tạm làm context lâu dài.

## 11. Giới hạn hiện có cần nhớ

Các điểm sau được nhận diện từ source, không phải kết luận kiểm thử tải hay kiểm toán triển khai:

- Chưa có idempotency cho giao dịch; retry cùng request có thể thực hiện lại nghiệp vụ.
- Transaction code là `"TXN" + System.currentTimeMillis()`: có unique DB constraint nhưng có thể va chạm khi cùng millisecond; chưa có retry sinh code.
- Account number sinh bằng `Math.random()` và check tồn tại trước save; vẫn có race giữa check và insert, chưa retry unique violation.
- Refresh rotation không có transaction bao toàn luồng hoặc row lock/atomic compare-and-set; chưa bảo đảm chỉ một refresh đồng thời thành công. Không có token-family revocation hay job dọn token cũ.
- Username uniqueness xuyên hai bảng chưa atomic. Khi trùng xảy ra, login và token validation từ chối identity mơ hồ.
- Logout ghi Redis rồi xóa DB, không phải distributed transaction; có thể thành công một phần.
- Cache invalidation thiếu ở mở/freeze/close account; update profile cũng chưa làm mới customerName trong account responses đã cache. Cache `accounts` dùng chung cho detail theo id và list theo username, cần xem thiết kế key khi thay đổi.
- Freeze/close dùng `findById` thông thường, chưa cùng cơ chế row lock như giao dịch tiền; không mặc định rằng lock ordering đã giải quyết mọi tình huống concurrency.
- Chưa có kiểm tra giới hạn số chữ số thập phân (`@Digits`) trong amount DTO, chưa có maximum transfer amount hoặc balance overflow guard riêng.
- List account groups giới hạn số customer nhưng vẫn trả mọi account của mỗi customer; `/my` và customer accounts cũng chưa phân trang. Sao kê chưa giới hạn size, chưa thêm id làm tie-break khi cùng createdAt.
- Register chưa kiểm tra email trùng ở service; update profile chưa kiểm tra email/phone trùng trước save. DB unique violation có thể rơi vào generic 500. DTO/update credentials chưa khớp như mô tả mục 6.
- Chưa có migration có version, bootstrap admin, frontend, Swagger/OpenAPI config, CI pipeline trong cây repo hiện tại. Không tự mô tả những phần đó như đã được triển khai.
- Test hiện có không chứng minh concurrent money transfers trên MySQL thật, Redis Lua/TTL thật hoặc toàn bộ ứng dụng end-to-end.

## 12. Những điểm README/GEMINI dễ gây nhầm

| Tài liệu cũ/ví dụ | Thực tế theo code |
| --- | --- |
| GEMINI: Spring Boot 3.3.4 | pom.xml dùng 3.4.4 |
| GEMINI: User, ROLE_USER, IdempotencyRecord, transaction status | Không tồn tại trong source hiện tại |
| GEMINI: khóa account theo ID tăng dần | So chuỗi accountNumber bằng compareTo |
| GEMINI: account có LOCKED | Account có FROZEN/CLOSED; LOCKED thuộc Customer |
| GEMINI: 37/37 tests passing, các cẩm nang phỏng vấn | Số liệu chưa xác minh lại; những file cẩm nang được liệt kê không có trong cây repo hiện tại |
| README: nhiều response trả trực tiếp domain object/token | Controller thực tế bọc ApiResponse; đọc data |
| README: tạo employee không body, delete customer 204 | Cả hai có ApiResponse; delete là 200 |
| README: CustomerRequest không username/password | DTO yêu cầu cả hai, kể cả PUT; service update không thay credentials |
| README: `/accounts/my` nhận customerId | CustomerAccountCreateRequest chỉ nhận type; owner từ authentication |
| README ERD: refresh token là JWT, role CUSTOMER/STAFF | Token là UUID, role lưu CUSTOMER/EMPLOYEE/ADMIN |

README vẫn hữu ích để đọc tổng quan và ví dụ, nhưng các absolute `file:///...` links và credential mẫu không nên sao chép sang tài liệu mới. Bối cảnh ôn phỏng vấn trong GEMINI là thông tin lịch sử, không mặc định là mục tiêu của mọi yêu cầu tiếp theo.
