# Phase 10 — iPhone landscape: mười màn mobile ở khổ ngang

**Liên kết:** [plan.md](plan.md) · [architecture-options.md](architecture-options.md) QĐ-3 ·
`iosApp/iosApp/Info.plist:54-58` · `core/designsystem/theme/Insets.kt` ·
`LLM.md` §13.4 (PageHeader không tự áp inset trên), §13.5 (OverlayHeader thì có)

## Tổng quan

**Ưu tiên:** P3 — **ưu tiên cuối**, đúng như chủ dự án xếp
**Trạng thái:** ☑ Xong 05.08.2026 — xem [Kết quả](#kết-quả) cuối file
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

- [x] Bỏ khoá `Info.plist`, xoá chú thích cũ
- [x] Lens ở khổ ngang — chụp thử được
- [x] Năm màn `OverlayHeader` — notch cạnh bên
- [x] Năm màn `PageHeader` — inset trên
- [x] Chat và ô khoá API với bàn phím bật
- [x] Đo lại ba hằng vị trí
- [x] So ảnh khổ dọc trước/sau

---

## Kết quả

**Xong 05.08.2026.** Đo trên **Galaxy A16 thật** (SM-A165F, 1080 × 2340 @ density 450 →
384 × 832 dp dọc, **832 × 384 dp ngang**), không phải trên máy ảo. Android vốn không khai
`screenOrientation` ở đâu cả nên nó đã xoay sẵn từ trước — nghĩa là giai đoạn này khảo sát
được hiện trạng **trước khi viết một dòng code nào**, đúng thứ tự bước 2 của plan.

### Điều plan đoán sai, và sai theo hướng có lợi

Plan viết *"mười màn mobile hiện có sống được ở khổ ngang"*, ngầm hiểu cả mười đều cần sửa.
Khảo sát cho thấy **chín màn không cần sửa một dòng nào**:

| Màn | Vì sao sống được |
|---|---|
| Nhật ký, Bộ sưu tập, Cài đặt, Chủ quyền, Khám phá (story) | đều cuộn — `LazyColumn` / `verticalScroll` không quan tâm cửa sổ cao bao nhiêu |
| Explore | bản đồ full-bleed, chrome nổi lên trên; bản đồ lấp đầy phần được giao |
| Hộ chiếu | `VietnamMapCanvas` fit theo `min(width/worldWidth, height/worldHeight)` — cửa sổ thấp thì vẽ Việt Nam nhỏ lại chứ không cắt |
| Dịch | ảnh co giãn; lớp phủ định vị theo ảnh chứ không theo cửa sổ |
| Chat | composer đã có `imePadding()` dưới `adjustResize` |

Ảnh chụp Nhật ký / Explore / Cài đặt ở khổ ngang **trước khi sửa** đã dùng được ngay.

### Màn thứ mười, và nó vỡ đúng như plan cảnh báo

Lens ở 832 × 384dp: khung ngắm còn **một dải 55dp** với vòng zoom lơ lửng trong đó
([ảnh 17](screenshots/17-phone-landscape-lens-before.png)). Cột của nó cần **214dp chrome**
trước khi ảnh được một pixel — hàng công cụ 44dp trong `Spacing.sm` (60), hàng chip trong
`Spacing.md` (68), shutter 78dp với `Spacing.sm` dưới (86).

Sửa bằng cách cho `LensScreen.kt` **hai cách sắp đặt** dùng chung mọi mảnh, kể cả khung ngắm:
`StackedLens` (như cũ) và `SideBySideLens` (ảnh chiếm bề rộng, shutter về cạnh phải). Rẽ ở
`rememberCanStackVertically(maxHeight)` — ngưỡng **500dp**, tức 214 + 280 cho một khung còn
ra dáng khung ngắm, làm tròn. Kết quả: [ảnh 18](screenshots/18-phone-landscape-lens-after.png).

Nó trùng kết luận của tablet nhưng **xuất phát từ tiền đề khác**: tablet dời shutter sang cột
phải vì tablet cầm hai mép, còn điện thoại vì không còn chiều cao. Và **cột điều khiển của
điện thoại không đòi bề rộng riêng** — nó tự đo bằng shutter 78dp cộng gutter hai bên, nên
không sinh ra bản sao khổ ngang của `PaneWidth.lensPanel` phải giữ đồng bộ với ai.

### Ba hằng vị trí: không hằng nào phải đổi

Bước 6 yêu cầu đo lại `COMPOSER_CLEARANCE`, `SheetPeekHeight`, `SHUTTER_INSET`. Cả ba đều
thuộc màn không đổi cách sắp đặt (chat, explore, note camera overlay), nên không hằng nào bị
chạm. Thứ *thêm mới* là `StackableMinHeight = 500.dp`, và nó được đặt tên kèm cơ sở đo đúng
theo `LLM.md` §13.2 chứ không nhét vào thang `Spacing`.

**Nó cố tình không dùng lại `ExpandedMinHeight = 600`.** Hai con số trả lời hai câu hỏi khác
nhau — 600 là chiều cao hai pane cần, 500 là chiều cao một cột cần — và nối chúng lại là để
một thay đổi ở cổng tablet lặng lẽ sắp xếp lại màn điện thoại.

### Một lỗi thật, do gate bắt chứ không do mắt

`ComposeStabilityReportTest` đánh trượt lần chạy đầu: năm tham số composable mới thành
unstable. `ZoomDriver` truyền thẳng vào ba composable con là sai — tablet đã giải bài này từ
trước bằng cách truyền **ba lambda** (`onPinch` / `onZoomProgress` / `onZoomGlideTo`) thay vì
cả đối tượng. Sửa theo đúng tiền lệ đó; chỉ `controller` vào allowlist, cùng lý lẽ đã dùng
cho `ViewfinderPane`.

### Nghiệm thu

- **Khổ dọc không đổi một pixel** — chứng minh bằng pixel-diff hai ảnh chụp cùng máy, không
  bằng lời: hàng công cụ, hàng chip + shutter và thanh tab **giống hệt từng bit**. Vùng duy
  nhất khác là preview camera đang chạy, chênh tối đa 12/255 — nhiễu cảm biến giữa hai lần
  chụp cùng một cảnh tĩnh. [ảnh 19](screenshots/19-phone-portrait-unchanged.png).
- **Chụp thử được ở khổ ngang** — shutter nổ, overlay phân tích chạy rồi tan, shutter sống
  lại, không crash. [ảnh 20](screenshots/20-phone-landscape-capture.png).
- **Xoay qua lại bốn lần** khi app đang chạy: dọc → ngang → dọc → ngang, preview vẫn sống,
  `logcat` không một dòng FATAL.
- **Test:** `:shared:allTests` **104 JVM / 93 iOS**, xanh cả hai. `WindowClassTest` mới đi
  qua cả hai biên ngưỡng, gồm cả sáu điểm mà iPad khổ dọc trượt cổng bề rộng.
- `DesignTokenTest` vẫn xanh — không có số đo nào bị viết cứng tại chỗ gọi.

### Hai thứ cố tình **không** làm

- **Không đổi thanh tab dưới thành `NavigationRail` ở khổ ngang.** Nó ăn 80 trên 384dp và
  Material khuyến nghị rail ở khung này, nhưng đổi shell là đụng cả mười màn và đe doạ đúng
  tiêu chí nghiệm thu chính — *"khổ dọc không đổi một pixel"*. Phạm vi phase này là mười màn,
  không phải shell. Nếu sau này muốn làm thì đó là plan riêng, và nó lãi 80dp cho mọi màn.
- **Không mở `UIInterfaceOrientationPortraitUpsideDown` cho iPhone.** Điện thoại có một chiều
  đúng và loa nghe nằm ở trên. iPad thì có, vì lý do ngược lại.

### Nợ còn lại

**Chưa chạy trên iPhone thật.** `Info.plist` đã mở khoá và logic rẽ nhánh là `commonMain`
dùng chung, đo bằng `BoxWithConstraints` chứ không hỏi nền tảng — nên cửa sổ 832 × 384 của
A16 và cửa sổ ngang của iPhone đi qua đúng một nhánh code. Nhưng tiêu chí *"trên iPhone thật,
không phải simulator"* của chính phase này vẫn chưa có ai ký. Việc còn lại đúng bằng: cắm một
iPhone, xoay ngang, chụp một tấm.

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
