# 🛍️ Shop Vibe API

Backend RESTful API cho hệ thống thương mại điện tử **Shop Vibe**, tập trung vào các nghiệp vụ cốt lõi: xác thực người dùng, quản lý sản phẩm/danh mục, giỏ hàng, đơn hàng, đánh giá và thống kê.

## 🚀 Tổng quan

- **Tên dự án:** Shop Vibe
- **Mục tiêu:** Cung cấp API backend cho ứng dụng bán hàng online với phân quyền người dùng và quản trị viên.
- **Ngữ cảnh bài toán:** Chuẩn hóa luồng mua sắm từ duyệt sản phẩm -> thêm vào giỏ -> đặt hàng -> theo dõi đơn -> đánh giá sản phẩm.

## 🧰 Công nghệ sử dụng

- **Java 21**
- **Spring Boot 3.5.5** (Web, Validation, Data JPA, Security, OAuth2 Resource Server)
- **PostgreSQL 15**
- **Redis** (cache)
- **JWT (Nimbus JOSE JWT)**
- **MapStruct + Lombok**
- **Cloudinary** (quản lý ảnh sản phẩm)
- **OpenAPI/Swagger UI** (`springdoc-openapi`)
- **Docker & Docker Compose**
- **Maven**

## ✨ Tính năng chính

Dựa trên các controller hiện có trong mã nguồn:

- 🔐 **Authentication & Authorization**
  - Đăng ký, đăng nhập, introspect token, refresh token, đổi mật khẩu, đăng xuất.
- 👤 **User Management**
  - Quản lý hồ sơ cá nhân (`/users/me`), admin xem danh sách user, khóa/mở khóa tài khoản.
- 📦 **Product & Category Management**
  - CRUD danh mục, CRUD sản phẩm, tìm kiếm/sắp xếp/phân trang sản phẩm.
  - Upload/xóa ảnh sản phẩm.
- 🛒 **Cart Management**
  - Xem giỏ hàng, thêm/sửa/xóa item, xóa toàn bộ giỏ.
- 📑 **Order Management**
  - Tạo đơn, xem danh sách đơn theo bộ lọc trạng thái/thời gian, hủy đơn.
  - Admin xem toàn bộ đơn và cập nhật trạng thái đơn.
- ⭐ **Review System**
  - Xem/ghi/sửa/xóa đánh giá theo sản phẩm.
- 📊 **Admin Statistics**
  - Doanh thu theo khoảng thời gian, top sản phẩm, top khách hàng, tổng quan hệ thống.
- 📚 **API Documentation**
  - Swagger UI tích hợp sẵn.

## 🗂️ Cấu trúc thư mục chính

```text
shop-vibe/
├── src/
│   ├── main/
│   │   ├── java/org/ngphthinh/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```



## ⚙️ Cài đặt (Prerequisites & Installation)

### 1) Yêu cầu môi trường

- **JDK 21**
- **Maven 3.9+**
- **Docker Desktop** (khuyến nghị để chạy đầy đủ PostgreSQL + Redis + Backend)

### 2) Clone dự án

```powershell
git clone https://github.com/ngphthinh/shop-vibe-be.git
Set-Location shop-vibe
```

### 3) Tạo file `.env`

Dự án dùng biến môi trường cho DB, Redis, JWT và Cloudinary.

> Gợi ý (giá trị này chỉ dành cho môi trường phát triển local):

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shop_vibe
DB_USER=sa
DB_PASS=sapassword
DB_ROOT_PASS=sapassword

JWT_SECRET=<generate_a_secure_base64_secret>
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=86400

SPRING_PROFILES_ACTIVE=dev

REDIS_PASSWORD=sapassword

DEFAULT_PASSWORD=Test@12345

CLOUDINARY_CLOUD_NAME=<your_cloudinary_name>
CLOUDINARY_API_KEY=<your_cloudinary_key>
CLOUDINARY_API_SECRET=<your_cloudinary_secret>
```

## ▶️ Chạy dự án (Usage)

### Cách 1 - Khuyến nghị: Docker Compose (chạy toàn bộ stack: Postgres + Redis + backend)

```powershell
docker compose up --build -d
```

Dừng toàn bộ dịch vụ:

```powershell
docker compose down
```

> Lưu ý: `docker-compose.yml` hiện sử dụng các biến từ file `.env`. Hãy chắc chắn bạn đã tạo `.env` trước khi gọi `docker compose up`.

### Cách 2 - Chạy bằng Maven (khi đã có DB/Redis tương thích và cấu hình `.env`/môi trường)

```powershell
mvn clean spring-boot:run
```

### Chạy unit/integration tests

```powershell
mvn test
```

## 🔑 Tài khoản test (Test accounts)

- Admin: `admin@shopvibe.vn` / `Test@12345`
- User: `user1@shopvibe.vn` / `Test@12345`

> Các tài khoản này là gợi ý — nếu project có `InitData` hoặc seed dữ liệu, kiểm tra xem password mặc định có đúng như trên không (file `.env` chứa `DEFAULT_PASSWORD=Test@12345` trong workspace hiện tại).

## 🌐 Truy cập nhanh (Endpoints & Docs)

- **Base URL:** `http://localhost:8080/api`
- **Swagger UI (UI mới):** `http://localhost:8080/api/swagger-ui/index.html`
- **Swagger UI (cổng cũ / fallback):** `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v3/api-docs`

## 📌 Một số endpoint tiêu biểu

- `POST /api/v1/auth/register` — đăng ký
- `POST /api/v1/auth/login` — đăng nhập (trả access token + refresh token)
- `POST /api/v1/auth/refresh-token` — refresh token
- `GET /api/v1/products` — lấy danh sách sản phẩm (phân trang)
- `POST /api/v1/cart/items` — thêm item vào giỏ
- `POST /api/v1/orders` — tạo đơn hàng
- `GET /api/v1/admin/statistics/overview` — báo cáo tổng quan (admin)

## 🏗️ Kiến trúc tổng quan

- Ứng dụng là backend RESTful theo chuẩn layered architecture:
  - `controller` — lớp entrypoint HTTP (REST endpoints)
  - `service` — logic nghiệp vụ
  - `repository` — truy vấn dữ liệu (Spring Data JPA)
  - `entity` / `dto` — model dữ liệu & data transfer objects
  - `security` — cấu hình bảo mật JWT và xử lý OAuth2 Resource Server
  - `config` — cấu hình chung (caching, cloudinary, jpa, async...)

- Docker Compose dựng 3 service:
  - `db` (Postgres)
  - `cache` (Redis)
  - `backend` (ứng dụng Spring Boot)

- Flow chính (ví dụ đặt hàng):
  1. Khách gửi request tạo đơn tới controller `OrderController`.
  2. Controller gọi `OrderService` để kiểm tra tồn kho, tính toán giá/chiết khấu, lưu `Order` và `OrderItem` vào DB.
  3. Nếu thanh toán online tích hợp, service sẽ gọi payment gateway (tùy cài đặt).
  4. Thông tin đơn được lưu và có thể truy vấn qua `OrderController` hoặc `StatisticsController` cho báo cáo.

## 🧭 Assumptions / Quyết định kỹ thuật

- Mã sử dụng JWT để bảo vệ API và Spring Security OAuth2 Resource Server để validate token.
- Dữ liệu file cấu hình (DB/Redis/JWT/Cloudinary) lấy từ file `.env` (được nạp bởi Docker Compose và `application.properties`).
- Cấu hình `spring.jpa.hibernate.ddl-auto=update` để thuận tiện phát triển (chú ý đổi cho môi trường production).
- Redis dùng làm cache & session-like storage cho cart (tuỳ implement), với mật khẩu được cấu hình.
- Cloudinary dùng để lưu ảnh sản phẩm (key/secret lấy từ `.env`).
- Swagger/OpenAPI được bật cho mục đích phát triển và tài liệu API.

## 🔒 Bảo mật & Lưu ý

- Không commit file `.env` chứa secret vào git.
- Thay `JWT_SECRET`, `CLOUDINARY_API_SECRET`, `DB_PASS`... bằng giá trị an toàn cho môi trường production.
- Kiểm tra lại `spring.profiles.active` và các cấu hình logging/cors cho production.

