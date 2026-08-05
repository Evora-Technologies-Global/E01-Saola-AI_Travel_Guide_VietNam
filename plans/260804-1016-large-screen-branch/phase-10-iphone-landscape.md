# Phase 10 — iPhone landscape: mười màn mobile ở khổ ngang

**Liên kết:** [plan.md](plan.md) · [architecture-options.md](architecture-options.md) QĐ-3 ·
`iosApp/iosApp/Info.plist:54-58` · `core/designsystem/theme/Insets.kt` ·
`LLM.md` §13.4 (PageHeader không tự áp inset trên), §13.5 (OverlayHeader thì có)

## Tổng quan

**Ưu tiên:** P3 — **ưu tiên cuối**, đúng như chủ dự án xếp
**Trạng thái:** ☐ Chưa bắt đầu
**Quyết định áp dụng:** QĐ-3 — mở hết orientation cho cả iPhone lẫn iPad

## Vì sao đây là giai đoạn riêng, không phải một bước của phase 02

iPhone nằm ngang rộng ~891dp nhưng chỉ cao ~411dp. Theo ngưỡng đã chốt ở QĐ-4 — rộng ≥840dp
**và** cao ≥600dp — nó **không** đạt, nên nó ở lại **nhánh mobile**.

Nghĩa là giai đoạn này không đụng một dòng nào của `tablet/`. Nó là việc bắt mười màn mobile
hiện có sống được ở khổ ngang. Hoãn nó sang đợt sau cũng không ảnh hưởng gì tới phần còn lại
của plan — và ngược lại, làm nó sớm cũng không giúp nhánh tablet chạy nhanh hơn một ngày nào.

Phase 02 vì thế chỉ mở `~ipad` và **để nguyên khoá portrait của iPhone**, có ghi chú trỏ sang
đây. Mở sớm chỉ tạo ra một quãng dài mà iPhone nằm ngang thì vỡ.

## Nhận định then chốt

- **Lens là màn khó nhất, và là lý do câu khoá được viết ra.** `Info.plist:56` nói *"the
  viewfinder is framed for it"*. Ở khổ ngang: khung camera đổi tỉ lệ, `SHUTTER_INSET` là vị
  trí đo được cho khổ dọc (`LLM.md` §12 — nó là vùng chạm mà không có gì che), và hàng công cụ
  chạy ngang màn sẽ ăn hết chiều cao 411dp.
- **Notch chuyển sang cạnh bên khi xoay.** `Insets.kt` đã lường trước chuyện này —
  §13.4 nói rõ vì sao `PageHeader` **không** tự áp inset trên: *"khi nằm ngang, phần khuyết
  chuyển sang một bên và cả trang phải dịch theo, không chỉ mỗi cái tiêu đề."* Cái lý lẽ đó
  viết ra cho đúng ngày hôm nay. Kiểm tra nó có thật sự đúng.
- **`OverlayHeader` thì tự áp `screenInsetsPadding()`** (§13.5) vì nó nổi trên nội dung. Năm
  màn dùng nó — discovery, translation, explore, và hai overlay trong `DiscoveryScreen` — là
  năm chỗ phải soi kỹ nhất ở khổ ngang.
- **Bàn phím ở khổ ngang ăn gần hết màn.** Chat và ô dán khoá API trong Settings là hai chỗ có
  ô nhập. `android:windowSoftInputMode="adjustResize"` lo phần Android; iOS thì Compose
  Multiplatform xử lý qua inset IME — phải kiểm tay.
- **`COMPOSER_CLEARANCE` và `SheetPeekHeight` là vị trí đo được cho khổ dọc** (§12). Ở khổ
  ngang chúng có thể sai, và cách sửa **không** phải là snap chúng vào `Spacing` — mà là đo
  lại và nói chúng đo theo cái gì.

## Yêu cầu

**Chức năng:** cả mười màn dùng được ở khổ ngang trên iPhone — không nội dung nào bị notch cắt,
không điều khiển nào ra ngoài màn, không ô nhập nào bị bàn phím che.
**Phi chức năng:** khổ dọc **không đổi một pixel nào**. Đây là điều kiện nghiệm thu chính.

## File liên quan

**Sửa:** `iosApp/iosApp/Info.plist` · `mobile/feature/camera/LensScreen.kt` (nhiều nhất) ·
`core/designsystem/theme/Insets.kt` nếu inset cạnh bên chưa đúng · các màn mobile khác tuỳ kết
quả rà soát
**Không đụng:** toàn bộ `tablet/`

## Các bước

1. Bỏ khoá portrait trong `Info.plist`, xoá luôn câu chú thích đã lỗi thời.
2. Rà **Lens** trước — đây là màn quyết định giai đoạn này có khả thi không. Xoay ngang, chụp
   thử, kiểm shutter, hàng công cụ, chips ngôn ngữ, zoom.
3. Rà năm màn dùng `OverlayHeader` ở khổ ngang: notch cạnh bên.
4. Rà năm màn dùng `PageHeader`: inset trên nằm ở container ngoài cùng, xác nhận nó dịch cả
   trang chứ không chỉ tiêu đề.
5. Rà hai màn có ô nhập với bàn phím bật.
6. Đo lại `COMPOSER_CLEARANCE`, `SheetPeekHeight`, `SHUTTER_INSET` ở khổ ngang; nếu phải đổi
   thì đặt tên mới kèm cơ sở đo, đừng nhét vào thang `Spacing`.
7. Chụp ảnh khổ dọc trước và sau, so từng màn.

## Todo

- [ ] Bỏ khoá `Info.plist`, xoá chú thích cũ
- [ ] Lens ở khổ ngang — chụp thử được
- [ ] Năm màn `OverlayHeader` — notch cạnh bên
- [ ] Năm màn `PageHeader` — inset trên
- [ ] Chat và ô khoá API với bàn phím bật
- [ ] Đo lại ba hằng vị trí
- [ ] So ảnh khổ dọc trước/sau

## Tiêu chí hoàn thành

- Mười màn dùng được ở khổ ngang trên iPhone thật, không phải trên simulator.
- Khổ dọc không đổi — chứng minh bằng ảnh so sánh, không bằng lời.
- `DesignTokenTest` vẫn xanh (không có số đo nào bị viết cứng tại chỗ gọi khi sửa).

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Lens không cứu được ở khổ ngang trên iPhone | Rà Lens ở bước 2, **trước** mọi thứ khác. Nếu nó không ổn thì dừng, báo lại, và cân nhắc khoá riêng mình Lens ở portrait — iOS cho phép mỗi view controller tự khai báo |
| Sửa cho ngang làm hỏng dọc | Bước 7 so ảnh từng màn; đây là điều kiện nghiệm thu chính chứ không phải bước cuối cho có |
| Ba hằng vị trí bị snap vào `Spacing` cho gọn | `LLM.md` §12 và §13.2 cấm. Review phải bác |

## Bảo mật

Không có.

## Tiếp theo

Đóng plan. Nếu QĐ-2 sau này đổi sang dựng feed Explore thì đó là plan mới.
