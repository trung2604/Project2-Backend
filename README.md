# BookStore Backend

## Overview
Đây là source code backend cho hệ thống BookStore, xây dựng bằng Spring Boot và MongoDB. Backend cung cấp RESTful API cho các chức năng quản lý người dùng, sách, đơn hàng, phân quyền, upload ảnh (Cloudinary), v.v.

## Tech Stack

- **Framework:** Spring Boot
- **Language:** Java
- **Database:** MongoDB
- **Build Tool:** Maven
- **API Documentation:** Swagger/OpenAPI

## Tính năng chính

- Đăng ký, đăng nhập, xác thực JWT
- Phân quyền người dùng (Admin, User)
- Quản lý sách, danh mục, kho
- Quản lý người dùng, đơn hàng
- Upload và quản lý avatar (Cloudinary)
- Phân trang, tìm kiếm, lọc dữ liệu

## Cài đặt & Chạy dự án

### Yêu cầu hệ thống
- Java JDK 17 trở lên
- MongoDB 5.0 trở lên
- Maven 3.8.x trở lên

### Hướng dẫn cài đặt

1. **Clone repository:**
    ```bash
    git clone https://github.com/trung2604/Project2.git
    cd BookStore
    ```

2. **Cấu hình MongoDB & Cloudinary:**
    - Tạo database MongoDB (ví dụ: `bookstore`)
    - Tạo file `src/main/resources/application.properties` (không commit file này lên git)
    - Tham khảo file `application.properties.example` để biết các key cần cấu hình:
      ```properties
      spring.data.mongodb.uri=
      cloudinary.cloud-name=
      cloudinary.api-key=
      cloudinary.api-secret=
      jwt.secret=
      ```

3. **Chạy ứng dụng:**
    ```bash
    ./mvnw spring-boot:run
    # hoặc
    mvn spring-boot:run
    ```

4. **Truy cập API docs:**
    - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Cấu trúc Project

```
BookStore/
├── src/
│   ├── main/
│   │   ├── java/com/project2/BookStore/
│   │   └── resources/
│   └── test/
├── pom.xml
└── README.md
```

## Một số API mẫu

- **Đăng ký:** `POST /api/bookStore/user/register`
- **Đăng nhập:** `POST /api/bookStore/user/login`
- **Phân trang user:** `GET /api/bookStore/user/paged?current=1&pageSize=10`
- **Upload avatar:** `POST /api/bookStore/user/avatar/upload`


## Đóng góp
1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push lên branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## License
MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## Contact
Trung Do - [@trung2604](https://github.com/trung2604) 