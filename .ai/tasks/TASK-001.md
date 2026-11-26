# TASK-001: Xác nhận backend build & test chạy được

- Repo: be
- Phụ thuộc: không
- Ước lượng: S

## Bối cảnh
Task khởi động để kiểm tra pipeline executor → verifier và đảm bảo nền JHipster sạch trước khi thêm nghiệp vụ.

## Yêu cầu
1. Chạy compile và toàn bộ test của project.
2. Nếu có lỗi do môi trường (thiếu Docker/Postgres cho integration test…) thì KHÔNG sửa code, chỉ ghi lại nguyên nhân và cách khắc phục vào report.
3. Nếu có lỗi do code generate (hiếm) thì sửa tối thiểu và giải thích.

## Phạm vi
- Được sửa: chỉ khi bắt buộc để build pass.
- KHÔNG được sửa: cấu hình bảo mật, JWT secret, `.yo-rc.json`.

## Tiêu chí chấp nhận
- [ ] `./mvnw -ntp compile` thành công.
- [ ] Đã chạy `./mvnw -ntp verify` và ghi rõ kết quả (số test pass/fail/skip) hoặc nguyên nhân không chạy được.
- [ ] Report liệt kê phiên bản Java, Maven wrapper, Node đang dùng.
- [ ] `git status` sạch, trừ các file trong `.ai/` (không thay đổi code ngoài ý muốn).

## Kiểm tra
```
./mvnw -ntp compile
./mvnw -ntp verify
java -version
node -v
git status
```
