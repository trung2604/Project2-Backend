# BookStore API

API hệ thống quản lý sách trực tuyến, cung cấp các chức năng quản lý sách, danh mục, đơn hàng và người dùng.

## Công nghệ sử dụng

- Java 17
- Spring Boot 3.4.5
- Spring Data JPA
- PostgreSQL
- Spring Security + JWT
- Cloudinary (lưu trữ hình ảnh)
- Maven
- Lombok
- Swagger/OpenAPI

## Cài đặt và chạy

### Yêu cầu hệ thống
- JDK 17 trở lên
- Maven 3.6 trở lên
- PostgreSQL 12 trở lên
- IDE (khuyến nghị IntelliJ IDEA)

### Cấu hình database
1. Tạo database PostgreSQL:
```sql
CREATE DATABASE bookstore;
```

2. Cấu hình kết nối trong `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bookstore
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Cấu hình Cloudinary
1. Đăng ký tài khoản tại [Cloudinary](https://cloudinary.com)
2. Cập nhật thông tin trong `application.properties`:
```properties
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret
```

### Chạy ứng dụng
1. Clone repository:
```bash
git clone https://github.com/your-username/bookstore.git
cd bookstore
```

2. Build project:
```bash
mvn clean install
```

3. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại `http://localhost:8080`

## API Documentation

### Authentication

#### Đăng ký
```http
POST /api/bookStore/user/register
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123",
    "fullName": "Nguyễn Văn A",
    "phone": "0123456789"
}
```

#### Đăng nhập
```http
POST /api/bookSotre/user/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123"
}
```

### Quản lý sách

#### Lấy danh sách sách mới nhất
```http
GET /api//bookStore/books/latest?limit=5
```
Response:
```json
{
  "success": true,
  "message": "Lấy danh sách sách mới nhất thành công",
  "data": [
    {
      "id": "book-123",
      "mainText": "Đắc Nhân Tâm",
      "author": "Dale Carnegie",
      "price": 89000,
      "quantity": 50,
      "sold": 100,
      "categoryId": "cat-1",
      "categoryName": "Kỹ năng sống",
      "image": {
        "thumbnail": "https://res.cloudinary.com/.../thumbnail.jpg",
        "medium": "https://res.cloudinary.com/.../medium.jpg",
        "original": "https://res.cloudinary.com/.../original.jpg"
      },
      "createdAt": "2024-06-10T08:00:00",
      "updatedAt": "2024-06-10T08:00:00"
    }
  ]
}
```

#### Lấy danh sách sách bán chạy
```http
GET /api/bookStore/books/top-selling?limit=5
```
Response:
```json
{
  "success": true,
  "message": "Lấy danh sách sách bán chạy thành công",
  "data": [
    {
      "id": "book-456",
      "mainText": "Nhà Giả Kim",
      "author": "Paulo Coelho",
      "price": 79000,
      "quantity": 30,
      "sold": 500,
      "categoryId": "cat-2",
      "categoryName": "Tiểu thuyết",
      "image": {
        "thumbnail": "https://res.cloudinary.com/.../thumbnail.jpg",
        "medium": "https://res.cloudinary.com/.../medium.jpg",
        "original": "https://res.cloudinary.com/.../original.jpg"
      },
      "createdAt": "2024-05-01T08:00:00",
      "updatedAt": "2024-06-01T08:00:00"
    }
  ]
}
```

#### Tìm kiếm sách
```http
GET /api/bookStore/books/search?keyword=nhà&categoryId=cat-1&minPrice=50000&maxPrice=100000&inStock=true&page=0&size=10&sortBy=price&sortDirection=asc
```

#### Thêm sách mới (Admin)
```http
POST /api/bookStore/books
Content-Type: multipart/form-data
Authorization: Bearer {token}

{
    "mainText": "Tên sách",
    "author": "Tác giả",
    "price": 89000,
    "quantity": 100,
    "categoryName": "Tên danh mục",
    "image": [file]
}
```

#### Cập nhật sách (Admin)
```http
PUT /api/bookStore/books/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
    "mainText": "Tên sách mới",
    "author": "Tác giả mới",
    "price": 99000,
    "quantity": 50,
    "categoryName": "Danh mục mới"
}
```

#### Xóa sách (Admin)
```http
DELETE /api/bookStore/books/{id}
Authorization: Bearer {token}
```

### Quản lý danh mục

#### Lấy danh sách danh mục
```http
GET /api/bookStore/categories?page=0&size=10
```

#### Thêm danh mục mới (Admin)
```http
POST /api/bookStore/categories
Content-Type: application/json
Authorization: Bearer {token}

{
    "name": "Tên danh mục mới"
}
```

#### Cập nhật danh mục (Admin)
```http
PUT /api/bookStore/categories/{id}
Content-Type: application/json
Authorization: Bearer {token}

{
    "name": "Tên danh mục mới"
}
```

#### Xóa danh mục (Admin)
```http
DELETE /api/bookStore/categories/{id}
Authorization: Bearer {token}
```

### Quản lý giỏ hàng

#### Thêm sách vào giỏ hàng
```http
POST /api/bookStore/cart
Content-Type: application/json
Authorization: Bearer {token}

{
    "bookId": "book-123",
    "quantity": 2
}
```

#### Cập nhật số lượng
```http
PUT /api/bookStore/cart/{bookId}
Content-Type: application/json
Authorization: Bearer {token}

{
    "quantity": 3
}
```

#### Xóa sách khỏi giỏ hàng
```http
DELETE /api/bookStore/cart/{bookId}
Authorization: Bearer {token}
```

#### Lấy giỏ hàng
```http
GET /api/bookStore/cart
Authorization: Bearer {token}
```

### Quản lý đơn hàng

#### Tạo đơn hàng mới
```http
POST /api/bookStore/orders
Content-Type: application/json
Authorization: Bearer {token}

{
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "phone": "0123456789",
    "address": "123 Đường ABC, Quận XYZ, TP.HCM"
}
```

#### Lấy danh sách đơn hàng
```http
GET /api/bookStore/orders?page=0&size=10
Authorization: Bearer {token}
```

#### Cập nhật trạng thái đơn hàng (Admin)
```http
PUT /api/bookStore/orders/{id}/status
Content-Type: application/json
Authorization: Bearer {token}

{
    "status": "SHIPPING"
}
```

## Cấu trúc thư mục

```
src/main/java/com/project2/BookStore/
├── config/          # Cấu hình ứng dụng
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA entities
├── exception/      # Custom exceptions
├── repository/     # JPA repositories
├── service/        # Business logic
│   └── impl/      # Service implementations
├── util/           # Utility classes
└── BookStoreApplication.java
```

## Bảo mật

- Sử dụng JWT cho xác thực
- Mã hóa mật khẩu với BCrypt
- Phân quyền Admin/User
- Validate input
- Xử lý CORS
- Rate limiting

## Xử lý lỗi

API trả về các mã lỗi HTTP chuẩn:
- 200: Thành công
- 400: Bad Request (dữ liệu không hợp lệ)
- 401: Unauthorized (chưa đăng nhập)
- 403: Forbidden (không có quyền)
- 404: Not Found
- 500: Internal Server Error

Format response lỗi:
```json
{
    "success": false,
    "message": "Mô tả lỗi"
}
```

## Contributing

1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push lên branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## License

[MIT License](LICENSE)

## Liên hệ

- Email: dodinhtrungthptyv@gmail.com
- GitHub: [trung2604](https://github.com/trung2604) 