# KẾ HOẠCH DỰ ÁN — Nền tảng đấu giá (BE + FE)

> Bản nháp của orchestrator. Phần nghiệp vụ cần chủ dự án xác nhận trước khi chia task chi tiết.

## Hiện trạng (23/11/2026)
- BE: JHipster 9.4.0 vừa generate, mới có User/Authority/JWT, chưa có entity nghiệp vụ, 1 commit.
- FE: đã khởi tạo (23/11) theo bulletproof-react — React 19 + Vite + JavaScript + Axios + React Query; có sẵn đăng nhập/đăng ký/hồ sơ, khung trang phiên đấu giá.

## Lộ trình đề xuất
| Giai đoạn | BE | FE |
|---|---|---|
| 0. Hạ tầng | Xác nhận build/test chạy được, docker services | Chọn stack, scaffold, layout, API client + auth |
| 1. Domain | Entity: Category, Auction (Product), Bid, Watchlist…; Liquibase; CRUD | Danh sách / chi tiết phiên đấu giá |
| 2. Đấu giá lõi | Đặt giá (kiểm tra bước giá, đồng thời), tự đóng phiên (scheduler), xác định người thắng | Form đặt giá, đếm ngược |
| 3. Realtime | WebSocket/STOMP đẩy giá mới, thông báo | Cập nhật giá trực tiếp |
| 4. Người bán / quản trị | Duyệt phiên, quản lý user, báo cáo | Dashboard người bán & admin |
| 5. Hoàn thiện | Thanh toán/đặt cọc (tùy phạm vi), bảo mật, test tải | Responsive, i18n, test E2E |

## Câu hỏi mở cho chủ dự án
1. ~~Framework FE~~ → đã chốt: React + JavaScript (không TS) + Axios.
2. Loại đấu giá: kiểu Anh (giá tăng dần) / đấu giá ngược / giá kín? Có "mua ngay", gia hạn khi có bid phút cuối?
3. Có thanh toán / đặt cọc thật không, hay chỉ mô phỏng?
4. Vai trò người dùng: người mua, người bán, admin — người bán có cần duyệt?
5. Hạn nộp đồ án và các mốc báo cáo?
