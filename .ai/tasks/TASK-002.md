# TASK-002: Triển khai Module User & Seller (Backend)

- Repo: be
- Phụ thuộc: TASK-001 (Đã hoàn thành)
- Ước lượng: L

## Bối cảnh
Triển khai toàn bộ tầng dữ liệu, nghiệp vụ và REST API cho Module Authentication & Authorization và Module User & Seller theo đặc tả trong `.ai/API_CONTRACT.md` và sơ đồ lớp nhóm A trong `docs/GIAI_THICH_SO_DO_LOP_HE_THONG_DAU_GIA.md`.

## Yêu cầu
1. **Liquibase CSDL:**
   - Tạo changelog XML mới mở rộng bảng `jhi_user` (thêm các cột: `phone`, `birth_date`, `account_status`, `seller_verification_status`, `reputation`, `id_card_number`, `id_card_front_url`, `id_card_back_url`).
   - Thêm các quyền mới `ROLE_SELLER`, `ROLE_BIDDER` vào bảng `jhi_authority`.
   - Tạo bảng `address` (`id`, `user_id`, `address_text`, `is_default`, created_date...).
   - Tạo bảng `login_session` (`id`, `user_id`, `device_info`, `ip_address`, `refresh_token`, `expires_at`, `created_at`).
   - Khai báo changelog mới vào `master.xml`.
2. **Domain & Repository:**
   - Cập nhật entity `User.java` map với các cột mới.
   - Tạo entity `Address.java` và `AddressRepository.java`.
   - Tạo entity `LoginSession.java` và `LoginSessionRepository.java`.
3. **DTO & Envelope:**
   - Tạo cấu trúc Response Envelope dùng chung: `ApiResponse<T>` (`statusCode`, `message`, `data`).
   - Tạo các DTO cho Auth: `RegisterDTO`, `VerifyOtpDTO`, `LoginDTO`, `AuthTokenDTO`, `ForgotPasswordDTO`, `ResetPasswordDTO`.
   - Tạo các DTO cho User: `UserProfileDTO`, `UpdateProfileDTO`, `ChangePasswordDTO`, `AddressDTO`, `SellerVerificationDTO`, `SellerPublicProfileDTO`, `SessionDTO`.
4. **Service:**
   - Xử lý đăng ký kèm OTP, xác thực email, đăng nhập cấp JWT, refresh token, thu hồi session khi logout.
   - Xử lý xem/cập nhật hồ sơ cá nhân, đổi mật khẩu.
   - Quản lý sổ địa chỉ (lấy danh sách, thêm địa chỉ, xóa địa chỉ).
   - Tiếp nhận hồ sơ xác minh người bán (chuyển status thành `PENDING`).
   - Tra cứu thông tin công khai & uy tín người bán.
5. **REST Controller:**
   - Viết các Controller theo đúng chuẩn trong `.ai/API_CONTRACT.md`:
     - `/api/auth/*`
     - `/api/users/me/*`
     - `/api/sellers/*`
   - Đảm bảo phân quyền `@PreAuthorize` và kiểm tra quyền sở hữu tài nguyên.
   - Trả về đúng định dạng envelope `{"statusCode": 200, "message": "Success", "data": ...}`.

## Phạm vi
- Được sửa/thêm: `domain`, `repository`, `service`, `web/rest`, `config/liquibase`.
- KHÔNG được sửa: Xóa các entity gốc của JHipster hoặc làm gãy các test cơ bản.

## Tiêu chí chấp nhận
- [ ] `./mvnw compile` thành công không lỗi cú pháp.
- [ ] Toàn bộ các API từ mục 1 đến 18 và mục 21 trong `.ai/API_CONTRACT.md` đều có Controller và Service tương ứng.
- [ ] Liquibase migration chạy trơn tru khi khởi động ứng dụng.
- [ ] Có Unit test hoặc Integration test cho UserProfile & Auth.

## Kiểm tra
```powershell
./mvnw -ntp compile
./mvnw -ntp test -Dtest=AuthenticateControllerIT "-Duser.timezone=UTC"
```
