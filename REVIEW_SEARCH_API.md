# API Tìm Kiếm Đánh Giá - BookStore

## Chức Năng Tìm Kiếm và Lọc Đánh Giá (Admin)

### Endpoint: `GET /api/bookStore/reviews/admin`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Chức năng:**
- ✅ **Tìm kiếm tổng hợp**: Tự động tìm trong nội dung comment, tên người đánh giá và tên sách
- ✅ **Lọc**: Theo số sao đánh giá (1-5)
- ✅ **Phân trang**: Hỗ trợ pagination
- ✅ **Sắp xếp**: Theo thời gian tạo (DESC)

**Query Parameters:**
```bash
# Tìm kiếm tổng hợp (tự động tìm trong comment, tên người đánh giá, tên sách)
search=nguyen|java|hay|tốt

# Lọc theo số sao đánh giá
rating=1|2|3|4|5

# Phân trang
page=0&size=10
```

**Ví dụ sử dụng:**

```bash
# Lấy tất cả đánh giá
GET /api/bookStore/reviews/admin?page=0&size=10

# Tìm kiếm tổng hợp - sẽ tìm trong comment, tên người đánh giá và tên sách
GET /api/bookStore/reviews/admin?search=nguyen&page=0&size=10

# Tìm kiếm tổng hợp - sẽ tìm trong comment, tên người đánh giá và tên sách
GET /api/bookStore/reviews/admin?search=java&page=0&size=10

# Tìm kiếm tổng hợp - sẽ tìm trong comment, tên người đánh giá và tên sách
GET /api/bookStore/reviews/admin?search=hay&page=0&size=10

# Lọc theo số sao
GET /api/bookStore/reviews/admin?rating=5&page=0&size=10

# Kết hợp tìm kiếm tổng hợp và lọc theo số sao
GET /api/bookStore/reviews/admin?search=nguyen&rating=5&page=0&size=10

# Kết hợp tìm kiếm tổng hợp và lọc theo số sao
GET /api/bookStore/reviews/admin?search=java&rating=4&page=0&size=10
```

**Cách hoạt động tìm kiếm tổng hợp:**

Khi bạn gõ từ khóa `search=nguyen`, hệ thống sẽ tự động tìm kiếm trong:
1. **Nội dung comment**: Tìm các comment có chứa từ "nguyen"
2. **Tên người đánh giá**: Tìm các người dùng có tên chứa "nguyen" (không phân biệt hoa thường)
3. **Tên sách**: Tìm các sách có tên chứa "nguyen" (không phân biệt hoa thường)

**Response Success:**
```json
{
  "success": true,
  "message": "Lấy danh sách đánh giá thành công",
  "data": {
    "meta": {
      "current": 0,
      "pageSize": 10,
      "pages": 3,
      "total": 25
    },
    "result": [
      {
        "id": "review_123",
        "bookId": "book_456",
        "bookName": "Sách Lập Trình Java",
        "userId": "user_789",
        "userName": "Nguyễn Văn A",
        "userEmail": "nguyenvana@email.com",
        "orderId": "order_101",
        "rating": 5,
        "comment": "Sách rất hay và bổ ích!",
        "isVerifiedPurchase": true,
        "status": "ACTIVE",
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ]
  }
}
```

**Response Error:**
```json
{
  "success": false,
  "message": "Không có quyền truy cập",
  "data": null
}
```

## Các API Đánh Giá Khác

### 1. Lấy đánh giá của sách
**Endpoint:** `GET /api/bookStore/reviews/book/{bookId}`
```bash
GET /api/bookStore/reviews/book/book_123?page=0&size=10
```

### 2. Lấy đánh giá của user hiện tại
**Endpoint:** `GET /api/bookStore/reviews/user`
```bash
GET /api/bookStore/reviews/user?page=0&size=10
```

### 3. Lấy rating trung bình của sách
**Endpoint:** `GET /api/bookStore/reviews/book/{bookId}/rating`
```bash
GET /api/bookStore/reviews/book/book_123/rating
```

### 4. Lấy tóm tắt đánh giá của sách
**Endpoint:** `GET /api/bookStore/reviews/book/{bookId}/summary`
```bash
GET /api/bookStore/reviews/book/book_123/summary
```

### 5. Debug endpoint (để kiểm tra dữ liệu)
**Endpoint:** `GET /api/bookStore/reviews/debug`
```bash
# Kiểm tra tổng số review và lấy mẫu dữ liệu
GET /api/bookStore/reviews/debug

# Debug tìm kiếm
GET /api/bookStore/reviews/debug?search=nguyen
```

**Response Debug:**
```json
{
  "success": true,
  "message": "Debug info retrieved",
  "data": {
    "totalReviews": 25,
    "sampleReviews": [
      {
        "id": "review_123",
        "bookId": "book_456",
        "bookName": "Sách Lập Trình Java",
        "userId": "user_789",
        "userName": "Nguyễn Văn A",
        "userEmail": "nguyenvana@email.com",
        "rating": 5,
        "comment": "Sách rất hay và bổ ích!",
        "isVerifiedPurchase": true,
        "status": "ACTIVE",
        "createdAt": "2024-01-15T10:30:00",
        "updatedAt": "2024-01-15T10:30:00"
      }
    ],
    "searchResults": {
      "commentResults": [],
      "userResults": [
        {
          "id": "review_123",
          "userName": "Nguyễn Văn A",
          "comment": "Sách rất hay và bổ ích!"
        }
      ],
      "bookResults": []
    }
  }
}
```

## Tóm Tắt Chức Năng

### Tìm kiếm tổng hợp:
- **Tự động tìm kiếm** trong 3 trường:
  - Nội dung comment
  - Tên người đánh giá (không phân biệt hoa thường)
  - Tên sách (không phân biệt hoa thường)
- **Chỉ cần 1 tham số** `search` duy nhất
- **Kết quả OR**: Trả về kết quả nếu từ khóa xuất hiện trong bất kỳ trường nào

### Lọc:
- **Số sao**: Lọc theo số sao đánh giá (1, 2, 3, 4, 5)

### Kết hợp:
- Có thể kết hợp tìm kiếm tổng hợp với lọc theo số sao
- Hỗ trợ phân trang linh hoạt
- Sắp xếp theo thời gian tạo mới nhất

### Lưu ý:
- Chỉ admin mới có quyền truy cập API này
- Tìm kiếm không phân biệt hoa thường
- Kết quả được sắp xếp theo thời gian tạo giảm dần
- **Đơn giản hóa**: Chỉ cần 2 tham số thay vì 4 tham số như trước 