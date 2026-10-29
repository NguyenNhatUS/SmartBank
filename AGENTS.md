# SmartBank — context cho agent

Khi bắt đầu làm việc trong repo, đọc `PROJECT_CONTEXT.md` và `GEMINI.md`.
`PROJECT_CONTEXT.md` ghi nhận hiện trạng đã đối chiếu với mã nguồn; `GEMINI.md`
chứa mục tiêu học tập của người dùng nhưng có một số mô tả kỹ thuật đã cũ.
Nếu tài liệu khác code, kiểm tra code liên quan trước khi trả lời hoặc sửa đổi.

- Trao đổi bằng tiếng Việt, giải thích thẳng vào bản chất và dùng ví dụ SmartBank.
- Người dùng đang chuẩn bị phỏng vấn Java Backend Intern / Fresher; khi giải thích
  kỹ thuật, nêu cơ chế, ưu/nhược điểm và phân biệt tính năng đã có với hướng cải tiến.
- Giữ kiến trúc Controller → Service interface → Service implementation → Repository,
  DTO + MapStruct, BigDecimal cho tiền và transaction/lock ordering khi sửa giao dịch.
- Không khẳng định test đang pass chỉ từ báo cáo cũ. Ghi rõ lệnh và kết quả nếu chạy lại.
- Không chép giá trị mật khẩu, JWT secret hay nội dung `.env` vào tài liệu context.
- Cập nhật `PROJECT_CONTEXT.md` khi thay đổi kiến trúc hoặc hành vi quan trọng.
