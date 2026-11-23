# Hợp đồng API (nguồn chuẩn cho FE)

Base URL dev: `http://localhost:8080`. Lỗi trả về dạng ProblemDetail (`application/problem+json`).

## Có sẵn từ JHipster
| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| POST | /api/authenticate | public | body `{username,password,rememberMe}` → `{id_token}` |
| POST | /api/register | public | đăng ký `{login,email,password,langKey}` |
| GET | /api/activate?key= | public | kích hoạt tài khoản |
| GET | /api/account | user | thông tin người dùng hiện tại |
| POST | /api/account | user | cập nhật thông tin |
| POST | /api/account/change-password | user | `{currentPassword,newPassword}` |
| POST | /api/account/reset-password/init | public | gửi email reset |
| POST | /api/account/reset-password/finish | public | `{key,newPassword}` |
| GET | /api/admin/users | admin | quản lý user |

## Nghiệp vụ đấu giá
(Claude bổ sung khi chốt domain model.)
