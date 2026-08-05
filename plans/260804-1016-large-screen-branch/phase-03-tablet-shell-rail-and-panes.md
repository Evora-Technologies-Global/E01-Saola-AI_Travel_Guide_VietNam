# Phase 03 — Khung tablet: rail, hai pane, token bề rộng

**Liên kết:** [plan.md](plan.md) · `LLM.md` §13.2 (Dimens), §7 (điều hướng) ·
wireframe `Việt Travel Lens - Tablet.dc.html` dòng 31-61

## Tổng quan

**Ưu tiên:** P0 — chặn phase 04-08
**Trạng thái:** ☑ Xong 04.08.2026 — đã kiểm trên Pixel Tablet API 36. Tiêu chí back stack mà
[phase 02](phase-02-entry-point-and-orientation.md) chuyển sang đây **đã xanh**.

Dựng khung mà năm màn tablet sẽ nằm vào: rail điều hướng bên trái, một scaffold hai pane dùng
chung, và các bề rộng pane được đặt tên trong `Dimens.kt`.

## Nhận định then chốt

- **Bốn con số trong wireframe là vị trí đo được, không phải khoảng cách.** `LLM.md` §13.2 nói
  rõ: *"Một con số là vị trí chứ không phải khoảng cách thì không nằm trên thang này. Hãy đặt
  tên cho nó, và nói nó được đo dựa vào đâu."* Rail 104, panel Lens 310, cột guide 352, cột
  ngày Journal 392, sheet 440 — đều thuộc loại đó. Chúng đi vào một `object` riêng, không
  snap vào `Spacing`.
- Khung thiết bị trong wireframe là 1218px bề trong. Trừ rail 104 còn 1114 cho nội dung. Cột
  guide 352 chiếm 31,6% — đủ để một câu hỏi và câu trả lời đọc được mà không cắt story bên trái
  xuống dưới mức dễ đọc.
- **Rail có năm mục, không phải bốn.** Ngoài Lens/Journal/Explore/Settings còn con dấu
  `CHỦ QUYỀN` ở đáy (dòng 57). Trên mobile, chủ quyền chỉ vào được từ Settings. Đây là **lối
  vào thứ hai**, không phải lối vào thay thế — Settings ở tablet vẫn giữ mục *Our position on
  the East Sea* (dòng 538).
- `TopLevelDestination` của mobile không dùng lại được: khác số mục, khác icon selected/
  unselected, và rail cần nhãn nằm dưới icon chứ không phải trong `NavigationBarItem`.
- **Phát hiện lúc triển khai — thứ thật sự làm QĐ-4 chạy được không phải là "dùng chung
  `NavHostController`", mà là *hai đồ thị phải bằng nhau về cấu trúc*.** `setGraph` so đồ thị
  mới với đồ thị đang giữ: bằng nhau thì nó đi đường cập-nhật-tại-chỗ, tráo composable của
  từng đích và **không đụng `backQueue`**. Khác nhau — dư một route, hay một `navArgument`
  khai mặc định lệch — thì nó xoá sạch back stack. Nghĩa là chép nguyên vẹn chín route sang
  `TabletNavGraph.kt` không phải là lười; đó là điều kiện. Phép đo ở cuối file.

## Yêu cầu

**Chức năng:** rail đứng yên ở mọi màn top-level, ẩn đi ở màn chi tiết đúng như bottom bar
đang làm; bấm con dấu mở overlay chủ quyền.
**Phi chức năng:** không một số đo nào viết thẳng tại chỗ gọi — `DesignTokenTest` sẽ soi
`tablet/feature/` từ phase 01.

## Kiến trúc

```kotlin
// core/designsystem/theme/Dimens.kt — thêm vào
/**
 * Bề rộng các pane ở bố cục màn rộng.
 *
 * Đây là vị trí đo được từ wireframe tablet (1194 × 834, khung trong 1218px), không phải
 * khoảng cách — nên chúng không nằm trên thang `Spacing`. §13.2.
 */
object PaneWidth {
    val rail = 104.dp          // cột điều hướng trái
    val lensPanel = 310.dp     // panel phải của viewfinder
    val guide = 352.dp         // cột AI guide cạnh story
    val journalList = 392.dp   // cột ngày, nửa master của Journal
    val sheet = 440.dp         // modal giữa màn
}
```

```
tablet/navigation/
├── VietLensTabletApp.kt      Row { NavigationRail · NavHost }
├── RailDestinations.kt       enum RailDestination — 4 tab + con dấu chủ quyền
└── TwoPaneScaffold.kt        Row { pane chính co giãn · pane phụ cố định }
```

`TwoPaneScaffold` là chỗ duy nhất biết đến `PaneWidth`, nên khi cần đổi tỉ lệ chỉ có một file
phải sửa. Ba màn dùng nó với ba bề rộng khác nhau (Lens 310, Discovery 352, Journal 392 — chú
ý Journal đặt pane cố định ở **bên trái**, nên scaffold phải nhận tham số vị trí).

Điều hướng: `VietLensTabletApp` nhận `navController` từ `VietLensRoot` (phase 02) và đăng ký
**cùng bộ `Routes`** như `VietLensApp`. Khác biệt duy nhất là composable nào được gắn vào mỗi
route, và ba route — `CHAT`, `PASSPORT`, `COLLECTION` — ở tablet không đẩy màn mới mà đổ vào
pane phụ (phase 05, 06).

## File liên quan

**Tạo:** `tablet/navigation/VietLensTabletApp.kt` · `tablet/navigation/RailDestinations.kt` ·
`tablet/navigation/TwoPaneScaffold.kt` · `tablet/navigation/TabletNavGraph.kt` *(ngoài plan)* ·
`navigation/TopLevelNavigation.kt` *(ngoài plan)*
**Sửa:** `core/designsystem/theme/Dimens.kt` (thêm `PaneWidth`) ·
`core/designsystem/theme/Insets.kt` (tách `ScreenInsets` ra khỏi modifier) ·
`navigation/Routes.kt` (thêm `TOP_LEVEL`) · `mobile/navigation/BottomDestinations.kt` ·
`mobile/navigation/VietLensApp.kt` · `androidHostTest/…/ComposeStabilityReportTest.kt`
**Không cần sửa:** `strings.xml` — con dấu dùng lại `sovereignty_seal` và `sovereignty_open`,
cả hai đã có đủ tám ngôn ngữ.

### Ba chỗ lệch khỏi plan, và lý do

| Lệch | Vì sao |
|---|---|
| Thêm `TabletNavGraph.kt` — bốn file thay vì ba | Gộp vào shell thì file 280 dòng, quá 200 của `development-rules.md`. Tách ra còn 177 + 104. Lợi thêm: phase 04–08 chạy song song, mỗi phase sửa một `composable` block trong file này chứ không tranh nhau sửa shell |
| Thêm `navigation/TopLevelNavigation.kt` thay vì nhét `isTopLevel()` vào `Routes.kt` | Bước 5 nói "cân nhắc". `Routes.kt` là **bảng route thuần** — không import gì của navigation. Ba hàm này cần `NavHostController`, nên chúng có nhà riêng. Và không chỉ `isTopLevel()`: `navigateToTopLevel` với `restartAtLens` đang `private` trong `VietLensApp.kt`, chép sang tablet là chép **hành vi**, đúng thứ `LLM.md` §3 cấm |
| Rail **không** vẽ dấu logo tròn ở đỉnh như wireframe | Đó là một component mobile không có. Ràng buộc của dự án là *"UI component phải giống bản mobile, chỉ cách sắp xếp theo wireframe"* — vẽ nó là tự chế component mới. Con dấu chủ quyền thì ngược lại: `SovereigntySeal` đã có sẵn trong `designsystem/component/`, nên nó được dùng lại nguyên vẹn, đặt trên đĩa `Vermilion` đúng cặp màu `SovereigntyBanner` đang dùng |

## Các bước

1. Thêm `object PaneWidth` vào `Dimens.kt` kèm KDoc nêu rõ nguồn đo.
2. `RailDestinations.kt` — enum bốn tab dùng lại đúng `StringResource` và `ImageVector` của
   `TopLevelDestination`, cộng một mục con dấu tách riêng (nó mở overlay, không phải là tab).
3. `TwoPaneScaffold.kt` — nhận `fixedPaneWidth: Dp`, `fixedPaneAtStart: Boolean`, hai slot
   composable. Không tự vẽ gì ngoài `Row` và đường phân cách.
4. `VietLensTabletApp.kt` — `Row { rail; NavHost }`, đăng ký toàn bộ `Routes`, tạm thời mọi
   route trỏ vào màn mobile tương ứng để app dùng được ngay từ giai đoạn này.
5. Ẩn rail ở màn chi tiết: dùng lại `isTopLevel()` — cân nhắc chuyển hàm đó từ
   `mobile/navigation/BottomDestinations.kt` lên `navigation/Routes.kt` vì giờ hai nhánh cùng cần.
6. Con dấu chủ quyền → `navController.navigate(Routes.SOVEREIGNTY)`.

## Todo

- [x] `PaneWidth` trong `Dimens.kt`
- [x] `RailDestinations.kt`
- [x] `TwoPaneScaffold.kt` — đúng bốn tham số, cộng `modifier` theo giao ước Compose
- [x] `VietLensTabletApp.kt` với NavHost đầy đủ route (NavHost nằm ở `TabletNavGraph.kt`)
- [x] Chuyển `isTopLevel()` lên chỗ dùng chung — kèm `navigateToTopLevel` và `restartAtLens`
- [x] Chuỗi ngôn ngữ cho con dấu — **không phát sinh**, dùng lại `sovereignty_seal` /
      `sovereignty_open`, cả hai đã đủ tám file
- [x] `ComposeStabilityReportTest` — allowlist `VietLensTabletNavHost`
- [x] Biên dịch ba đích: `:shared:compileAndroidMain`, `:app:compileDebugKotlin`,
      `:shared:compileKotlinIosSimulatorArm64` — cả ba xanh
- [x] `:shared:testAndroidHostTest` — 36/36 xanh. `DesignTokenTest` báo
      `feature: 27, mobile/feature: 10, tablet/feature: 0`, y như phase 02: giai đoạn này
      không thêm file nào vào `tablet/feature`, nên `POPULATED_BRANCHES` vẫn chưa phải sửa —
      đó là việc của phase 04
- [x] Thử trên Pixel Tablet API 36 — bảng dưới
- [ ] Thử trên iPad thật / simulator — vẫn cần máy Mac chạy `iosApp`, chưa làm (nợ từ phase 02)

## Tiêu chí hoàn thành

Đã kiểm trên **Pixel Tablet API 36 (2560×1600 @320dpi = 1280 × 800dp)**, bản debug
`com.duylt.trave.vietlensai.dev`:

| Tiêu chí | Kết quả |
|---|---|
| Rail bên trái, bấm được cả bốn tab | ☑ Lens / Nhật ký / Khám phá / Cài đặt, mỗi tab hiện màn mobile tương ứng |
| Mở Hộ chiếu (màn chi tiết) → rail ẩn | ☑ ẩn hẳn, trang trải full-bleed |
| Bấm con dấu → mở màn chủ quyền | ☑ |
| **Xoay khi đang mở Hộ chiếu thì vẫn ở Hộ chiếu** (nợ từ phase 02) | ☑ 1280×800dp → 337×731dp → về lại: vẫn ở Hộ chiếu. Back một lần nữa → về Nhật ký, tức là **cả lịch sử** còn chứ không chỉ trang đang mở |
| `DesignTokenTest` xanh và có soi `tablet/feature/` | ☑ 36/36; `tablet/feature: 0` như dự kiến |
| Nhãn rail ở tám ngôn ngữ | ☑ thử tiếng Việt ("Ống kính", "Khám phá") và tiếng Pháp ("Objectif", "Réglages") — dài nhất trong tám — đều gọn một dòng trong 104dp, dấu chồng của Ố không bị cắt |

**Ghi chú lúc kiểm:** trong khoảng một giây đầu sau khi khởi động lại app, thanh trạng thái
còn hiện và mục Lens có thể nằm dưới nó — `MainActivity.hideSystemBars()` chạy sau khi
composition đầu tiên đã đo. Là hành vi có sẵn của app, không phải do rail, và nó tự ổn định.

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Rail 104dp ở khổ 840dp chiếm 12,4% bề ngang | Ngưỡng 840dp là sàn; nếu thấy chật khi thử thật thì nâng ngưỡng, đừng thu rail — rail hẹp lại thì nhãn bị cắt. **Chưa đo ở đúng 840dp** — mới đo ở 1280dp |
| Nhãn rail dài ở tiếng Việt/Nhật tràn 104dp | ☑ Đã thử vi và fr, gọn một dòng. `Text` để mặc định nên nhãn tràn sẽ tự xuống hai dòng; không đặt `TextOverflow.Ellipsis` ở đâu cả |
| `TwoPaneScaffold` bị nhét thêm tham số dần dần | Giữ đúng bốn tham số. Cần thứ năm là dấu hiệu màn đó nên tự dựng `Row` của nó — đã viết thẳng vào KDoc của nó |
| **Hai danh sách tab lệch nhau** — `TopLevelDestination` và `RailDestination` cùng bốn mục, không có gì bắt chúng khớp | Tạm thời: `Routes.TOP_LEVEL` là nơi duy nhất khai *thành viên*, `isTopLevel()` đọc nó, và KDoc của cả hai enum trỏ về nó. Nhưng **chưa có test**. Đề nghị phase 09 thêm một test `commonTest` khẳng định `TopLevelDestination.entries.map { it.route } == Routes.TOP_LEVEL` và tương tự cho `RailDestination` — mười lăm dòng, chặn đúng cái lỗi mà `LLM.md` §7 nói là không ai enforce |

## Bảo mật

Không có.

## Tiếp theo

[Phase 04](phase-04-lens-tablet.md) trở đi — năm màn, chạy song song được. Mỗi phase sửa
một `composable` block trong `TabletNavGraph.kt`, và phase nào **đầu tiên** thêm file vào
`tablet/feature/` phải thêm luôn nhánh đó vào `POPULATED_BRANCHES` của `DesignTokenTest`.

## Phép đo back stack — cách tái hiện

Đây là bằng chứng cho QĐ-4 mà [phase 02](phase-02-entry-point-and-orientation.md) chưa lấy
được. Chạy lại đúng các bước này nếu có ai đổi một trong hai đồ thị:

```bash
adb shell wm size 2560x1600            # 1280 × 800dp → shell tablet
# Nhật ký → Hộ chiếu
adb shell wm size 1080x2340            # 337 × 731dp  → shell mobile
adb shell wm size 2560x1600            # về lại tablet
```

Kỳ vọng: sau bước cuối vẫn đang ở **Hộ chiếu**, và một lần back nữa về **Nhật ký**.
Nếu văng về Ống kính thì thủ phạm gần như chắc chắn là hai đồ thị đã lệch nhau về cấu trúc —
so `TabletNavGraph.kt` với `VietLensApp.kt` từng route một, kể cả ba `navArgument` mặc định
của `Routes.TRANSLATION`.
