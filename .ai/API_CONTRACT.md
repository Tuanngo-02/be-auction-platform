# Hợp đồng API (Nguồn chuẩn cho Backend & Frontend)

- Base URL dev: `http://localhost:8080` (FE gọi qua `/api/*` và được proxy sang backend)
- Định dạng phản hồi chuẩn (Response Envelope):
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": { ... }
}
```
- Khi xảy ra lỗi: Trả về HTTP status code tương ứng (400, 401, 403, 404, 500) kèm format ProblemDetail hoặc Error Envelope:
```json
{
  "statusCode": 400,
  "message": "Mô tả lỗi cụ thể",
  "data": null
}
```

---

## I. Module Authentication & Authorization

### 1. Đăng ký tài khoản người dùng mới (Bidder/Seller)
- **Method:** `POST`
- **Path:** `/api/auth/register`
- **Auth:** Public
- **Request Body:**
```json
{
  "fullName": "Lê Long Thiên",
  "email": "thien@example.com",
  "phone": "0905123456",
  "password": "Password123!",
  "birthDate": "2000-01-01"
}
```
- **Response (201 Created):**
```json
{
  "statusCode": 201,
  "message": "Created",
  "data": {
    "userId": 101,
    "email": "thien@example.com",
    "emailVerified": false
  }
}
```

### 2. Xác thực OTP đăng ký tài khoản qua email
- **Method:** `POST`
- **Path:** `/api/auth/verify-email-otp`
- **Auth:** Public
- **Request Body:**
```json
{
  "email": "thien@example.com",
  "otpCode": "123456"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "emailVerified": true
  }
}
```

### 3. Đăng nhập người dùng & cấp Token
- **Method:** `POST`
- **Path:** `/api/auth/login`
- **Auth:** Public
- **Request Body:**
```json
{
  "emailOrPhone": "thien@example.com",
  "password": "Password123!"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 4. Làm mới Access Token (Refresh Token)
- **Method:** `POST`
- **Path:** `/api/auth/refresh-token`
- **Auth:** Public
- **Request Body:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGci...",
    "expiresIn": 3600
  }
}
```

### 5. Đăng xuất & vô hiệu hóa token hiện tại
- **Method:** `POST`
- **Path:** `/api/auth/logout`
- **Auth:** Tất cả role đã xác thực
- **Request Body:**
```json
{
  "refreshToken": "eyJhbGci..."
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": null
}
```

### 6. Quên mật khẩu: Gửi mã OTP khôi phục qua email
- **Method:** `POST`
- **Path:** `/api/auth/forgot-password/send-otp`
- **Auth:** Public
- **Request Body:**
```json
{
  "email": "thien@example.com"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "email": "thien@example.com",
    "otpExpiresIn": 300
  }
}
```

### 7. Quên mật khẩu: Xác minh OTP khôi phục mật khẩu
- **Method:** `POST`
- **Path:** `/api/auth/forgot-password/verify-otp`
- **Auth:** Public
- **Request Body:**
```json
{
  "email": "thien@example.com",
  "otpCode": "654321"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "resetToken": "rst_xyz123"
  }
}
```

### 8. Đặt mật khẩu mới sau khi xác minh OTP
- **Method:** `POST`
- **Path:** `/api/auth/reset-password`
- **Auth:** Public
- **Request Body:**
```json
{
  "resetToken": "rst_xyz123",
  "newPassword": "NewPassword123!"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": null
}
```

---

## II. Module User & Seller

### 9. Xem thông tin hồ sơ cá nhân
- **Method:** `GET`
- **Path:** `/api/users/me`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "userId": 101,
    "fullName": "Lê Long Thiên",
    "email": "thien@example.com",
    "phone": "0905123456",
    "birthDate": "2000-01-01",
    "reputation": "4.9",
    "sellerStatus": "VERIFIED",
    "roles": ["ROLE_USER", "ROLE_SELLER"]
  }
}
```

### 10. Cập nhật thông tin hồ sơ cá nhân
- **Method:** `PUT`
- **Path:** `/api/users/me`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Request Body:**
```json
{
  "fullName": "Lê Long Thiên Updated",
  "birthDate": "2000-01-01"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "userId": 101,
    "fullName": "Lê Long Thiên Updated",
    "birthDate": "2000-01-01"
  }
}
```

### 11. Đổi mật khẩu tài khoản (khi đang đăng nhập)
- **Method:** `PATCH`
- **Path:** `/api/users/me/password`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": null
}
```

### 12. Lấy danh sách các phiên đăng nhập đang hoạt động
- **Method:** `GET`
- **Path:** `/api/users/me/sessions`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": [
    {
      "sessionId": 501,
      "deviceInfo": "Chrome / Windows",
      "ipAddress": "14.241.x.x",
      "isCurrent": true,
      "createdAt": "2026-11-25T10:00:00Z"
    }
  ]
}
```

### 13. Thu hồi một phiên đăng nhập cụ thể
- **Method:** `DELETE`
- **Path:** `/api/users/me/sessions/{sessionId}`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": null
}
```

### 14. Lấy danh sách địa chỉ trong sổ địa chỉ
- **Method:** `GET`
- **Path:** `/api/users/me/addresses`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": [
    {
      "addressId": 12,
      "addressText": "123 Nguyễn Lương Bằng, Đà Nẵng",
      "isDefault": true
    }
  ]
}
```

### 15. Thêm địa chỉ mới vào sổ địa chỉ
- **Method:** `POST`
- **Path:** `/api/users/me/addresses`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Request Body:**
```json
{
  "addressText": "456 Tôn Đức Thắng, Đà Nẵng",
  "isDefault": false
}
```
- **Response (201 Created):**
```json
{
  "statusCode": 201,
  "message": "Created",
  "data": {
    "addressId": 13,
    "addressText": "456 Tôn Đức Thắng, Đà Nẵng",
    "isDefault": false
  }
}
```

### 16. Xóa địa chỉ khỏi sổ địa chỉ
- **Method:** `DELETE`
- **Path:** `/api/users/me/addresses/{addressId}`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": null
}
```

### 17. Gửi yêu cầu xác minh tài khoản người bán
- **Method:** `POST`
- **Path:** `/api/users/me/seller-verification`
- **Auth:** ROLE_USER, ROLE_BIDDER
- **Request Body:**
```json
{
  "idCardNumber": "048000123456",
  "idCardFrontUrl": "http://...",
  "idCardBackUrl": "http://..."
}
```
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "status": "PENDING"
  }
}
```

### 18. Tra cứu lịch sử hoạt động cá nhân (Audit Log)
- **Method:** `GET`
- **Path:** `/api/users/me/activities`
- **Auth:** ROLE_USER, ROLE_BIDDER, ROLE_SELLER
- **Query Params:** `page` (int, default 0), `size` (int, default 10)
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "items": [
      {
        "activityId": 88,
        "action": "LOGIN",
        "timestamp": "2026-11-25T13:00:00Z"
      }
    ],
    "total": 1
  }
}
```

### 21. Xem thông tin công khai và uy tín người bán
- **Method:** `GET`
- **Path:** `/api/sellers/{sellerId}/profile`
- **Auth:** Public
- **Response (200 OK):**
```json
{
  "statusCode": 200,
  "message": "Success",
  "data": {
    "sellerId": 10,
    "fullName": "Nguyễn Văn A",
    "reputation": "4.8",
    "totalAuctions": 15,
    "verified": true
  }
}
```

---

## III. Các Module tiếp theo (Đấu giá, Đặt cọc, Thanh toán)
Sẽ được bổ sung tiếp theo lộ trình khi triển khai các task sau.
