# Bản ghi kiểm chứng — nhánh màn hình lớn

**Ngày:** 04.08.2026 · **Nhánh git:** `duylt_dev` · Ghi theo yêu cầu của
[phase-09](phase-09-tests-and-docs.md). Ảnh chụp nằm ở [`screenshots/`](screenshots/).

Máy dùng để kiểm:

| Máy | API / OS | Cửa sổ |
|---|---|---|
| Pixel Tablet (AVD) | 35 | 1280 × 800 dp (2560 × 1600 @ 320 dpi) |
| Pixel 7 Pro (AVD) | 37 | mặc định 411 × 891 dp; ép 1280 × 800 dp bằng `wm size` + `wm density` |
| Galaxy A16 (máy thật) | 36 | 384 × 832 dp |
| iPad Pro 11″ (M4) simulator | iOS 18.6 | 834 × 1210 dọc, 1210 × 834 ngang |
| iPhone 16 Pro simulator | iOS 18.6 | 402 × 874 dp |

---

## 1. Bộ test tự động

| Hạng mục | Kết quả |
|---|---|
| `:shared:allTests` | **BUILD SUCCESSFUL** — 41 test JVM + 30 test iOS simulator, 0 lỗi |
| `:shared:testAndroidHostTest` | 41/41 xanh |
| `:shared:iosSimulatorArm64Test` | 30/30 xanh |
| `:shared:connectedAndroidDeviceTest` (Pixel Tablet, API 35) | **12/12 xanh** |
| `:shared:compileAndroidDeviceTest` | xanh |
| `xcodebuild -scheme iosApp` (iPad simulator) | **BUILD SUCCEEDED** |

**`:shared:allTests` lần đầu tiên xanh trên cả hai nền.** Trước đó chỉ chân Android chạy —
xem mục 4.

### Hai cổng ở `androidHostTest`

| Cổng | Kết quả | Bằng chứng nó không đọc tập rỗng |
|---|---|---|
| `DesignTokenTest` | 6 quy tắc xanh | tự in ra: **`scans 135 feature file(s) — feature: 116, mobile/feature: 10, tablet/feature: 9`**. Đếm tay trên đĩa: 116 / 10 / 9. Khớp. |
| `ComposeStabilityReportTest` | xanh | 21 lớp `unstable` trong `shared-classes.txt`, trần `UNSTABLE_CLASS_CEILING = 21` |

Hai điểm cần nói rõ, vì cả hai lệch với chữ trong plan:

- **Trần là 21, không phải 20** như phase-09 viết. Phase 04 đã nâng 20 → 21 khi `ZoomDriver`
  rời `LensScreen.kt` — nó vốn `private` nên vô hình với báo cáo Compose, giờ mới được đếm.
  KDoc của hằng số ghi rõ lý do. Con số **không tăng thêm** trong phase này: nhánh tablet chỉ
  thêm composable, không thêm lớp giữ trạng thái, đúng như phase-09 dự đoán.
- **Đổi `DiscoveryTabletScreen` và `JournalTabletScreen` sang `internal` không làm trần nhúc
  nhích.** Đã chạy lại `:shared:allTests` **sau** khi đổi: vẫn 21, vẫn 41 + 30.

### Test thiết bị cho hai pane — đã viết **và đã chạy**

Plan dự liệu rằng chúng sẽ không chạy được trên máy này. Điều đó hoá ra sai — xem mục 4.

| File | Khẳng định |
|---|---|
| `androidDeviceTest/tablet/TwoPaneScroll.kt` | máy dùng chung: cửa sổ ép 1280 × 800 dp, matcher tìm pane ở mép trái, cách đọc offset cuộn |
| `…/tablet/feature/journal/JournalPaneSwitchTest.kt` | đổi pane phải → cột ngày đứng nguyên chỗ |
| `…/tablet/feature/discovery/DiscoveryStoryScrollTest.kt` | hỏi guide → story đứng nguyên; **và** một trang chiếm trọn cửa sổ đóng lại → story vẫn đứng nguyên |

Test thứ hai của Discovery là cái thực sự có răng: `DiscoveryTabletScreen` bọc hai pane trong
`AnimatedContent`, nên `storyScroll` mà tụt xuống trong `StoryPane` là bị huỷ theo trang. Test
thứ nhất — cái plan yêu cầu — hôm nay đúng bằng nhiều cách, nên nó là lưới an toàn chứ không
phải phát hiện. Cả hai đều nói rõ điều đó trong KDoc.

**Ba lỗi của chính bộ test, bắt được bằng cách chạy thật:**

1. Matcher đầu tiên đòi `top == 0`. Sai: `screenInsetsPadding()` đẩy mỗi pane xuống một khoảng
   *khác nhau* — story ở `top = 192 px`, cột ngày ở `48 px`.
2. Mệnh đề "cao hơn rộng" cũng sai: đúng với cột ngày 392 dp, sai với story pane 927 dp.
3. `performScrollToIndex(6)` cuộn hàng *Culture collection* ra khỏi khung, và cú bấm sau đó
   không tìm thấy node. Đúng thao tác thật: cuộn lệch một chút rồi mới bấm — chính là cách
   phase 06 kiểm tay.

Cả ba chỉ lộ ra khi chạy trên máy. Nếu tin lời "không chạy được ở đây" thì đã commit ba lỗi này.

---

## 2. Ma trận kiểm thử tay

| Thiết bị | Yêu cầu | Kết quả | Ảnh |
|---|---|---|---|
| Tablet Android ngang | cả năm màn, xoay giữa chừng | ☑ Lens (rail 104 + panel 310 + RECENT SCANS), Journal hai pane, Discovery story + guide, Explore bản đồ tràn viền, Settings hai cột, Chủ quyền | [01](screenshots/01-tablet-lens.png) [02](screenshots/02-tablet-journal-passport.png) [06](screenshots/06-tablet-discovery-guide.png) [07](screenshots/07-tablet-explore-map.png) [08](screenshots/08-tablet-settings-two-columns.png) [09](screenshots/09-tablet-sovereignty.png) |
| Đổi pane phải | cột ngày đứng yên | ☑ passport → collection, cột ngày y nguyên | [02](screenshots/02-tablet-journal-passport.png) → [03](screenshots/03-tablet-journal-collection.png) |
| Xoay giữa chừng (1280×800 ↔ 800×1280) | đổi shell, giữ chỗ đang đứng | ☑ đang ở Journal/pane Collection → xoay dọc ra **nhánh mobile**, vẫn ở Nhật ký → xoay lại ngang, **pane Collection còn nguyên** (`rememberSaveable` sống qua config change) | [04](screenshots/04-tablet-portrait-falls-to-mobile.png) [05](screenshots/05-tablet-back-to-landscape.png) |
| Tablet split-screen hẹp | phải rơi về mobile | ☑ 437 × 500 dp → thanh dưới, một cột | [11](screenshots/11-853x533-and-437x500-fall-to-mobile.png) |
| Điện thoại nằm ngang (853 × 533 dp) | phải rơi về mobile | ☑ đủ rộng nhưng thiếu cao — **mệnh đề chiều cao của QĐ-4 làm đúng việc** | [11](screenshots/11-853x533-and-437x500-fall-to-mobile.png) |
| Điện thoại | **không đổi gì cả** | ☑ Galaxy A16 thật, tiếng Việt, giao diện sáng: Lens và Nhật ký y như trước, thanh dưới bốn tab | [12](screenshots/12-phone-unchanged.png) |
| iPad ngang | nhánh tablet | ☑ 1210 × 834 → rail + panel 310, con dấu chủ quyền dưới chân rail | [14](screenshots/14-ipad-landscape-tablet.png) |
| iPad dọc | mobile hay tablet, tuỳ QĐ-4 | ☑ 834 × 1210 → **mobile**, vì 834 < 840 | [13](screenshots/13-ipad-portrait-mobile.png) |
| iPad xoay qua lại | đổi shell khi đang chạy | ☑ dọc → ngang → dọc, shell đổi cả hai chiều | [13](screenshots/13-ipad-portrait-mobile.png) → [14](screenshots/14-ipad-landscape-tablet.png) → [15](screenshots/15-ipad-back-to-portrait.png) |
| iPhone nằm ngang | vẫn khoá dọc ở giai đoạn này | ☑ bấm xoay, màn hình không đổi | [16](screenshots/16-iphone-stays-portrait.png) |
| Bảy ngôn ngữ trên khổ tablet | nhãn rail, hai cột settings | ☑ vi · ja · ko · zh · th · fr · es đều vừa khung, không cắt chữ, dấu chồng tiếng Thái (`ตั้งค่า`, `ปัญญาประดิษฐ์`) không bị xén | [10](screenshots/10-seven-locales.png) |

Tiếng Anh là ảnh gốc ở mục trên, nên đủ **tám** ngôn ngữ.

### Fold gập/mở — **chưa kiểm bằng máy fold thật**

Không có máy fold nào ở đây, và không có AVD fold. Thứ thay thế là ba lần đổi kích thước cửa
sổ đang chạy ở trên (xoay tablet, xoay iPad, đổi `wm size`), tức là **đúng cơ chế** mà một cái
fold kích hoạt — `BoxWithConstraints` đo lại, `rememberWindowClass` đổi lớp, `VietLensRoot`
đổi shell trên cùng một `NavHostController`. Cái chưa được kiểm là bản lề vật lý và
`WindowLayoutInfo`, thứ app này không đọc. Ghi là **chưa chạy**, không đánh dấu hoàn thành.

### Hai điều quan sát được, không phải lỗi của plan này

- **Explore trên AVD Pixel Tablet không có GPS**, nên phải ép cửa sổ lớn trên emulator điện
  thoại rồi `adb emu geo fix` — đúng cách phase 07 đã làm. Ở đó bản đồ vẽ đúng: tràn viền,
  cụm *"Around you"* + hai nút icon xếp dọc ở góc trên trái. Cột kết quả bên phải **không
  hiện** vì `0 PLACES WITHIN 5 KM` — dữ liệu rỗng, không phải bố cục sai. Cách bày cột đã được
  phase 07 kiểm ở 1664 × 768 dp.
- **Lens của điện thoại ở khổ ngang chật chội** (Galaxy A16, 832 × 384 dp): khung ngắm dẹt,
  cụm điều khiển dồn lại. Android chưa bao giờ khoá hướng nên đây là chuyện có từ trước plan
  này, và nó chính là việc của [phase 10](phase-10-iphone-landscape.md).

---

## 3. Bảo mật

Ba lượt quét, tất cả đều trả về rỗng:

| Quét | Phạm vi | Kết quả |
|---|---|---|
| `AIza` · `sk-…` · `api_key =` · `Bearer ` · `secret` · `password` · `token =` | `tablet/` (cả `commonMain` lẫn `androidDeviceTest`) | 0 |
| `https?://` | như trên | 0 |
| `/Users/` · `/home/` · `C:\` | như trên | 0 |
| các mẫu trên + `-----BEGIN` | `git diff HEAD` toàn bộ, chỉ tính dòng thêm | 0 |

`git ls-files` không có `.env`, `.jks` hay keystore nào — chỉ bốn file `.env.example` mẫu, vốn
đã nằm trong repo từ trước.

Kết luận: **`tablet/` không có nguồn cấu hình riêng.** Nó nhận `state`, phát `onIntent`, và đặt
composable — đúng ràng buộc đặt ra ở đầu plan.

---

## 4. Ba điều plan viết sai, và cái giá của chúng

Ghi lại vì cả ba đều cùng một dạng: **một câu khẳng định trong tài liệu, không ai chạy lại.**

### a. `LLM.md` §11 hàng #18 — "không chạy được trên Android 15 trở lên"

Sai phạm vi, và sai theo hướng đắt nhất: vì tin nó, không ai chạy chân thiết bị suốt hai plan.
Đo ngày 04.08.2026:

| Máy | API | Kết quả |
|---|---|---|
| Pixel Tablet AVD | 35 | **12/12 xanh** |
| Galaxy A16 máy thật | 36 | **7/7 xanh** (`RecompositionTest`) |
| Pixel 7 Pro AVD | 37 | 12/12 đỏ, `NoSuchMethodException: InputManager.getInstance` |

Chỉ **API 37** hỏng. Hàng #18 đã sửa lại phạm vi, kèm bài học: *"máy này không chạy được"* là
một khẳng định có hạn dùng, và chạy lại tốn một phút.

### b. `LLM.md` §11 hàng #14 — một lỗi, hoá ra là hai

Sửa `SecurityException` xong, trình biên dịch đi tiếp và gặp lỗi thứ hai đứng ngay sau:
`JournalViewModelTest:120` có tên hàm backtick chứa dấu phẩy — *"…is reported, not swallowed"*
— và **Kotlin/Native từ chối `,` trong định danh** trong khi JVM chấp nhận. Cả hai đã sửa; cả
hai đã viết thành quy tắc ở `LLM.md` §9 để test sau không tái phạm.

### c. `ComposeStabilityReportTest` — trần 21, plan viết 20

Phase 04 đã nâng và ghi lý do; phase-09 viết theo con số cũ. Không gây hại, nhưng nếu tin plan
mà "sửa" trần về 20 thì build đỏ vì một thay đổi đã được duyệt từ trước.

Và một chỗ nhỏ trong `Info.plist`: chú thích viết iPad dọc *"four points short"* — thực tế là
**sáu** (834 so với 840). Đã sửa, kèm số đo thật của iPad Pro 11″ M4.

---

## 5. Việc còn lại sau phase này

| Việc | Trạng thái |
|---|---|
| Fold gập/mở trên máy thật | ☐ không có thiết bị |
| Bốn màn tablet còn lại trên iPad simulator (Journal, Discovery, Explore, Settings) | ☐ không bấm được vào Simulator từ phiên này (thiếu quyền Accessibility cho tiến trình). Fork, xoay và màn Lens đã kiểm; bốn màn kia dùng đúng code trong `tablet/` đã kiểm trên tablet Android |
| Cột kết quả của Explore với dữ liệu thật | ☐ `0 PLACES WITHIN 5 KM` trên AVD |
| `androidDeviceTest` trên API 37 | ☐ chờ nâng `ui-test-junit4` — `LLM.md` §11 hàng #18 |
| `TranslationScreen` 683 dòng | ☐ hàng #11, là việc của phase 10 |
