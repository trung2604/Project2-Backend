# BookStore Project

## Overview
BookStore là một ứng dụng web toàn diện cho phép quản lý và mua sắm sách trực tuyến. Dự án được xây dựng với kiến trúc microservices, sử dụng Spring Boot cho backend, React cho frontend và MongoDB làm cơ sở dữ liệu.

## Tech Stack

### Backend
- **Framework**: Spring Boot
- **Language**: Java
- **Database**: MongoDB
- **Build Tool**: Maven
- **API Documentation**: Swagger/OpenAPI

### Frontend
- **Framework**: React.js
- **State Management**: Redux/Context API
- **UI Library**: Material-UI/Ant Design
- **Build Tool**: npm/yarn

### Database
- **Database**: MongoDB
- **Database Management**: MongoDB Compass

## Tính năng chính

### Quản lý sách
- Thêm, sửa, xóa thông tin sách
- Tìm kiếm và lọc sách theo nhiều tiêu chí
- Quản lý danh mục sách
- Quản lý kho sách

### Quản lý người dùng
- Đăng ký và đăng nhập
- Phân quyền người dùng (Admin, Customer)
- Quản lý thông tin cá nhân
- Quản lý đơn hàng

### Giỏ hàng và thanh toán
- Thêm/xóa sách vào giỏ hàng
- Quản lý giỏ hàng
- Xử lý đơn hàng
- Tích hợp thanh toán

## Cài đặt và Chạy dự án

### Yêu cầu hệ thống
- Java JDK 17 trở lên
- Node.js 16.x trở lên
- MongoDB 5.0 trở lên
- Maven 3.8.x trở lên
- npm hoặc yarn

### Backend Setup
1. Clone repository:
```bash
git clone https://github.com/trung2604/Project2.git
cd BookStore
```

2. Cấu hình MongoDB:
- Cài đặt MongoDB
- Tạo database "bookstore"
- Cập nhật thông tin kết nối trong `application.properties`

3. Chạy ứng dụng Spring Boot:
```bash
./mvnw spring-boot:run
```

### Frontend Setup
1. Di chuyển vào thư mục frontend:
```bash
cd frontend
```

2. Cài đặt dependencies:
```bash
npm install
# hoặc
yarn install
```

3. Chạy ứng dụng React:
```bash
npm start
# hoặc
yarn start
```

## API Documentation
API documentation có sẵn tại: `http://localhost:8080/swagger-ui.html` sau khi chạy backend

## Cấu trúc Project

```
BookStore/
├── src/                    # Backend source code
│   ├── main/
│   │   ├── java/
│   │   │   └── com/project2/BookStore/
│   │   │       ├── controllers/
│   │   │       ├── models/
│   │   │       ├── repositories/
│   │   │       ├── services/
│   │   │       └── BookStoreApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── frontend/              # Frontend source code
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── App.js
│   ├── package.json
│   └── README.md
├── pom.xml
└── README.md
```

## Contributing
1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'Add some AmazingFeature'`)
4. Push lên branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## License
Dự án này được cấp phép theo MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## Contact
Trung Nguyen - [@trung2604](https://github.com/trung2604)

Project Link: [https://github.com/trung2604/Project2](https://github.com/trung2604/Project2) 