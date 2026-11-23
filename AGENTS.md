# AGENTS.md — be-auction-platform

File này được cả **Antigravity CLI (executor)** và **Codex CLI (verifier)** đọc.
Vai trò cụ thể: executor → `.ai/roles/executor.md`; verifier → `.ai/roles/verifier.md`. Quy trình: `.ai/README.md`.

## Dự án
Backend nền tảng đấu giá trực tuyến (đồ án tốt nghiệp).
- JHipster 9.4.0, Spring Boot, monolith, **không có client** (FE là repo riêng `fe-auction-platform`).
- Java package gốc: `com.auction.system`
- Auth: JWT (`/api/authenticate`), DB: PostgreSQL (dev & prod), cache: Redis, migration: Liquibase.
- i18n: tiếng Việt (`vi`).

## Lệnh
- Build/compile: `./mvnw -ntp compile` (Windows: `mvnw.cmd -ntp compile`)
- Test: `./mvnw -ntp verify` hoặc 1 class: `./mvnw -ntp test -Dtest=TenClassTest`
- Chạy dev: `./mvnw` (cần Postgres + Redis: `docker compose -f src/main/docker/services.yml up -d`)
- Format: `npm run prettier:format`

## Quy ước code
- Phân lớp: `domain` (entity) → `repository` → `service` (+ `service/dto`, `service/mapper` MapStruct) → `web/rest` (Resource).
- REST không trả entity trực tiếp, luôn qua DTO. Lỗi dùng `BadRequestAlertException` / `ExceptionTranslator` (ProblemDetail).
- Mọi thay đổi schema phải có changelog Liquibase mới trong `src/main/resources/config/liquibase/changelog/` và được include trong `master.xml`. Không sửa changelog đã tồn tại.
- Phân quyền bằng `@PreAuthorize` / `AuthoritiesConstants`; kiểm tra quyền sở hữu tài nguyên ở service.
- Tiền tệ dùng `BigDecimal`, thời gian dùng `Instant` (UTC).
- Logic đấu giá (đặt giá, đóng phiên) phải an toàn khi đồng thời (khóa lạc quan `@Version` hoặc khóa bi quan) và có test.
- Mỗi tính năng mới có test (unit cho service, integration cho Resource).

## Hợp đồng API
Nguồn chuẩn: `.ai/API_CONTRACT.md`. Thay đổi API phải cập nhật file này trong cùng task (FE dựa vào đó).

## Cấm
- Commit/push, sửa ngoài phạm vi task, đưa secret thật vào repo, tắt test để cho pass.
