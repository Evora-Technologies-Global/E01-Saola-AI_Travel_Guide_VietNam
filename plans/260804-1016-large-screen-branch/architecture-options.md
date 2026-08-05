# Các phương án — và cái giá của từng phương án

Tài liệu quyết định cho [plan.md](plan.md). Bốn câu hỏi, mỗi câu một đề xuất kèm lý do.

## Đã chốt — 04.08.2026

| # | Quyết định | Ghi chú |
|---|---|---|
| QĐ-1 | **Phương án A** — chung ViewModel, tách Screen và navigation | theo đề xuất |
| QĐ-2 | **Giữ bản đồ**, chỉ sắp lại | kèm bố cục cụ thể do chủ dự án chỉ định, ghi ở cuối mục QĐ-2 |
| QĐ-3 | **Mở hết cho cả iPhone lẫn iPad** | *khác đề xuất*, và được xếp **ưu tiên cuối** — tách thành [phase-10](phase-10-iphone-landscape.md) |
| QĐ-4 | **Dùng chung `NavHostController`**, ngưỡng rộng ≥840dp và cao ≥600dp | theo đề xuất |

---

## QĐ-1 · Nhân đôi tới đâu

Yêu cầu là *"tách `:shared` ra hai nhánh mobile và tablet, gần như hai app riêng"*. Câu hỏi
thật nằm ở chỗ **ranh giới cắt đi qua đâu** — vì cắt quá sâu thì phá chính ràng buộc
"component phải giống mobile".

### Phương án A — chung ViewModel, tách Screen và navigation ✅ đề xuất

```
com.duylt.trave.vietlensai/
├── MainViewModel.kt                    chung
├── core/                               chung — mvi, designsystem, util
│   └── window/WindowClass.kt           MỚI · phân loại bề rộng cửa sổ
├── di/SharedModules.kt                 chung
├── platform/ · voice/                  chung
├── navigation/Routes.kt                chung · tách ra từ Destinations.kt
├── feature/<name>/
│   ├── XContract.kt                    chung
│   ├── XViewModel.kt                   chung
│   └── component/                      MỚI · composable rút ra từ XScreen, hai nhánh cùng gọi
├── mobile/
│   ├── navigation/VietLensApp.kt       chuyển từ navigation/
│   └── feature/<name>/XScreen.kt       chuyển từ feature/<name>/
└── tablet/
    ├── navigation/VietLensTabletApp.kt MỚI
    └── feature/<name>/XTabletScreen.kt MỚI
```

Nhánh tablet là **tầng sắp đặt thuần tuý**: nhận `state`, phát `onIntent`, xếp các component
đã có vào rail và hai pane. Không có logic nghiệp vụ nào ở đó.

- **Được:** ràng buộc "component giống mobile" được cấu trúc bảo đảm chứ không dựa vào kỷ
  luật. Một sửa lỗi nghiệp vụ sửa một lần. Bộ test ViewModel hiện có phủ luôn cả tablet.
- **Mất:** hai nhánh vẫn dùng chung `feature/<name>/` nên chưa phải "hai app rời hẳn". Ai đọc
  cây thư mục sẽ thấy ba tầng chứ không phải hai.
- **Khối lượng:** ~3.500 dòng mới ở `tablet/`, ~2.000 dòng di chuyển, 0 dòng logic nhân bản.

### Phương án B — nhân đôi trọn vẹn cả Contract và ViewModel

`mobile/feature/<name>/{Contract,ViewModel,Screen}` và `tablet/feature/<name>/{…}`.

- **Được:** đúng nghĩa đen "hai app riêng biệt". Sửa tablet không thể làm hỏng mobile.
- **Mất:** mười ViewModel thành hai mươi. Mỗi lỗi nghiệp vụ phải sửa hai chỗ, và cái thứ hai
  là cái sẽ bị quên — đây là kiểu nợ mà `LLM.md` §11 sinh ra để ghi lại. `SharedModules.kt`
  thành hai đồ thị Koin. Bộ `commonTest` nhân đôi. Vi phạm thẳng nguyên tắc DRY trong
  `development-rules.md`.
- **Đánh giá:** chỉ đáng nếu hai nền thật sự khác *hành vi*, không chỉ khác *bố cục*. Wireframe
  cho thấy dữ liệu, luồng và trạng thái y hệt — chỉ khác chỗ đặt. Không đáng.

### Phương án C — tách thành module Gradle `:shared:mobile` / `:shared:tablet`

- **Được:** Gradle chặn được chuyện nhánh này lỡ gọi vào nhánh kia.
- **Mất:** `LLM.md` §2 phải viết lại; framework iOS `VietLensShared` phải gộp nhiều module;
  `compose-stability.conf` và hai gate ở `androidHostTest` đều gắn với một module `:shared`.
  Chi phí hạ tầng lớn, đổi lại một ràng buộc mà quy ước package đã đủ giữ.
- **Đánh giá:** để dành. Nếu sau này nhánh tablet phình ra thật thì nâng A lên C là việc cơ học.

---

## QĐ-2 · Explore — wireframe vẽ một màn, code là một màn khác

Đây không phải chuyện sắp lại bố cục. Là hai màn khác hẳn nhau.

| | Wireframe (**cả mobile lẫn tablet**) | Code đang chạy |
|---|---|---|
| Nội dung | Feed gợi ý: *Where next · Happening right now · Continue the thread · Trail of the week · Việt Nam 101* | Bản đồ Google Maps / MapKit + địa điểm gần |
| Dữ liệu | Chưa có model, use case hay repository nào | `NearbyPlace`, `PlaceDetails`, `ExploreViewModel` 262 dòng |
| Bản đồ | Không có | Là toàn bộ màn hình |

Wireframe mobile cũng vẽ feed này — nghĩa là **thiết kế đi trước code**, chứ không phải
tablet đòi khác mobile.

- **Giữ bản đồ, chỉ sắp lại** ✅ *đề xuất* — bản đồ co giãn, danh sách địa điểm thành cột phải
  thay cho bottom sheet. Đúng phạm vi "mở rộng màn lớn", đúng ràng buộc component.
- **Dựng feed theo wireframe** — feature mới trọn vẹn: domain model, use case, repository,
  nguồn dữ liệu cho suggestions/trails/articles. Và phải làm cho cả mobile, nếu không mobile
  với tablet lại lệch nhau. Nằm ngoài plan này.
- **Hai pane: map trái + feed phải** — vẫn phải dựng toàn bộ feed như trên.

Ghi chú: bản ghi nhớ của dự án có dòng *"Explore phải độc lập với Passport — không bản đồ
tỉnh"*. Cả ba phương án đều tôn trọng điều đó; bản đồ ở Explore là bản đồ địa điểm, không
phải bản đồ tỉnh của Passport.

### Bố cục đã chốt cho Explore tablet

Wireframe không vẽ màn này, nên bố cục do chủ dự án chỉ định trực tiếp:

- **Góc trên bên trái:** container *"Around you"* và **hai nút icon xếp theo chiều dọc** —
  tức là thẻ tiêu đề nằm trên, hai nút xếp chồng bên dưới nó. Trên mobile hai nút này đang
  nằm trong `trailing` của `OverlayHeader`, nghĩa là tablet **lấy chúng ra khỏi header** rồi
  xếp dọc. Hai nút đó là `explore_recenter` ("Centre on me") và `explore_refresh`
  ("Search again").
- **Bên phải:** danh sách kết quả đề xuất, dạng cột dọc — thay cho `ModalBottomSheet` của
  mobile.

**Điểm còn diễn giải:** cả hai được mô tả bằng vị trí *trên màn hình* ("góc trái trên màn
hình", "bên tay phải màn hình"), nên plan hiểu là **bản đồ trải full-bleed, hai cụm này nổi
lên trên nó** — giống cách Google Maps và Apple Maps làm ở khổ tablet. Nếu ý là bản đồ bị co
lại nhường chỗ cho một pane đặc bên phải thì nói lại; đó là khác biệt giữa một `Box` có lớp
nổi và một `Row` hai pane, và nó đổi cả cách xử lý cử chỉ kéo.

---

## QĐ-3 · Phạm vi nền tảng

`iosApp/iosApp/Info.plist:54` khoá portrait, lý do ghi ngay trong file: *"Portrait only, like
the Android build: the viewfinder is framed for it."* Android thì **không** khoá — không có
`screenOrientation` trong manifest, không có `requestedOrientation` trong `MainActivity`. Câu
chú thích đó đã lỗi thời.

- **iPad mở hết, iPhone giữ portrait** — *đề xuất ban đầu*, thêm `UISupportedInterfaceOrientations~ipad`
  riêng. iPad có landscape và hai pane; iPhone không đổi hành vi nên không có rủi ro hồi quy.
- **Mở hết cho cả iPhone lẫn iPad** ✅ **đã chốt, ưu tiên cuối** — phải rà lại toàn bộ Lens ở
  khổ ngang trên điện thoại: khung camera, vị trí shutter, mọi `screenInsetsPadding()` đều
  đang giả định khổ dọc.
- **Chỉ Android đợt này** — chạy được ngay trên tablet và fold. Nhưng KMP mà chỉ giao một nền
  thì mất một nửa giá trị, và nhánh `tablet/` nằm ở `commonMain` nên iOS vẫn phải biên dịch nó.

### Vì sao chốt này tách thành hai giai đoạn

iPhone nằm ngang rộng ~891dp nhưng chỉ cao ~411dp, nên theo QĐ-4 nó **ở lại nhánh mobile**.
Mở khoá iPhone landscape vì thế không phải là việc của nhánh tablet chút nào — nó là việc bắt
mười màn **mobile** hiện có sống được ở khổ ngang. Đúng như chủ dự án xếp: ưu tiên cuối.

- [Phase 02](phase-02-entry-point-and-orientation.md) mở khoá **iPad**, vì không có nó thì
  không nhìn thấy nhánh tablet.
- [Phase 10](phase-10-iphone-landscape.md) mở khoá **iPhone** và rà mười màn mobile. Chạy sau
  cùng, và có thể hoãn sang đợt sau mà không ảnh hưởng gì tới phần còn lại của plan.

---

## QĐ-4 · Ngưỡng, và chuyện gì xảy ra khi đổi kích thước giữa chừng

Người dùng xoay iPad, gấp/mở fold, kéo split-screen — cửa sổ đổi bề rộng khi app đang chạy.

- **≥ 840dp, dùng chung `NavHostController`** ✅ *đề xuất* — `NavHostController` được nâng lên
  trên chỗ rẽ, hai shell đăng ký cùng bộ `Routes`. Đổi kích thước thì đổi shell nhưng giữ
  nguyên chỗ đang đứng và cả lịch sử back. Dưới 840dp — kể cả fold mở ~673dp và điện thoại
  nằm ngang — dùng nhánh mobile, đúng vì wireframe không vẽ khổ nào khác ngoài 1194.
- **Hai `NavHost` độc lập** — tách triệt để hơn, nhưng xoay máy là về màn đầu và mất chỗ đang
  đọc; mỗi deep link phải khai báo hai lần rồi lệch nhau về sau.
- **Rẽ một lần lúc khởi động theo loại thiết bị** — đơn giản nhất, không có vấn đề back-stack.
  Đổi lại: split-screen hẹp trên iPad vẫn nhận layout rộng, và fold gấp lại thì rail 104dp ăn
  một phần tư bề ngang.

Chiều cao cũng phải tính, không chỉ bề rộng: điện thoại nằm ngang rộng ~891dp nhưng chỉ cao
~411dp. Đề xuất dùng **cả hai** điều kiện — rộng ≥ 840dp **và** cao ≥ 600dp — để một chiếc
điện thoại nằm ngang không rơi vào bố cục master–detail 392dp + detail.
