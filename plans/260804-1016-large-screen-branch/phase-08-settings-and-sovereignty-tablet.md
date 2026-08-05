# Phase 08 — Settings, Chủ quyền, và sheet 440

**Liên kết:** [plan.md](plan.md) · wireframe dòng 478-596 · `SettingsScreen.kt` (755 → 178) ·
`SovereigntyScreen.kt` (295 → 80) + `SovereigntyMap.kt` (336, không đụng) ·
`LLM.md` §11 hàng #13 và #11

## Tổng quan

**Ưu tiên:** P2 · **Trạng thái:** ☑ Xong 04.08.2026 — đã thử trên Pixel Tablet 1280×800dp,
trên điện thoại 411×731dp, ở tiếng Nhật và tiếng Thái, và qua một lần đổi kích thước

Ba thứ nhỏ gom một giai đoạn: Settings xếp lại theo cột, overlay chủ quyền, và modal 440dp.

## Nhận định then chốt

- **Settings không được kéo giãn thành một cột dài 1114dp.** Wireframe xếp các nhóm thành hàng
  (`display:flex` ở dòng 483) — nghĩa là hai cột. Một hàng cài đặt rộng hơn ~600dp thì nhãn
  bên trái và giá trị bên phải xa nhau tới mức mắt phải quét ngang, đó là lý do wireframe
  không để nó full width.
- **`SovereigntyRoute` tự vẽ cả trang — không có `SovereigntyScreen` stateless.** Đó là
  `LLM.md` §11 hàng #13, và là feature duy nhất không tách Route/Screen. Nhánh tablet cần một
  composable stateless để đặt vào overlay, nên **việc tách đó rơi đúng vào giai đoạn này** —
  trả nợ hàng #13 luôn. Ghi chú của hàng đó cảnh báo: rút một composable ra là đổi phạm vi
  recomposition của nó, nên phải xác nhận bằng mắt sau khi tách.
- Overlay chủ quyền ở tablet là `position:absolute` phủ toàn khung (dòng 549), có nút `×`
  (dòng 553) — không phải một màn đẩy đè. Trên mobile nó vẫn là màn đẩy đè. Cùng một
  composable, hai cái vỏ.

## Ba chỗ plan gốc nói sai, và cái đúng thay vào

Plan này viết trước khi đọc code hiện tại. Ba điểm lệch, ghi lại vì chúng đổi cả việc phải làm:

1. **Không còn bộ chọn ngôn ngữ/vùng nào để chuyển.** Plan nói *"Modal 440dp là bộ chọn
   ngôn ngữ/vùng — mobile đang dùng bottom sheet"*, và yêu cầu tách `RegionPickerBody` khỏi
   vỏ sheet. Nhưng `ef5c6a9` đã xoá hẳn picker đó khi lời kể chuyển sang theo ngôn ngữ của
   máy — `SettingsRepository:35` có hẳn một dòng chú thích nói tại sao không còn `setLanguage`.
   Cả màn Settings **không có một `ModalBottomSheet` nào**. Bộ chọn duy nhất còn lại là Giao
   diện, và nó vốn đã là `AlertDialog` giữa màn.
   → Việc thay vào: giữ `AlertDialog`, chặn bề rộng ở `PaneWidth.sheet` bằng `widthIn`. Trên
   điện thoại (411dp) mức chặn 440 không chạm tới cái gì nên hình không đổi; trên cửa sổ
   1194dp nó là khác biệt giữa một hộp thoại và ba nút radio với 400dp trống bên cạnh. **Không
   dựng `tablet/navigation/CenterDialog.kt`** — một file vỏ với một người gọi mà cái đang có
   đã đúng thì là YAGNI. `PaneWidth.sheet` trước phase này **không có người đọc nào**; giờ có
   ba, và cả ba là cùng một quyết định.
2. **Có năm nhóm, không phải ba.** Plan liệt kê `INTELLIGENCE` / `EXPERIENCE` / `ABOUT` và nói
   `EXPERIENCE` chứa *"ngôn ngữ kể chuyện, giữ tên tiếng Việt, gói offline"* — không mục nào
   trong ba mục đó tồn tại. Thực tế: `INTELLIGENCE` (khoá + model), `EXPERIENCE` (đọc to),
   `APPEARANCE` (giao diện), `DATA` (xoá lịch sử), `ABOUT` (chủ quyền + phiên bản).
   → Chia hai cột theo đúng thứ tự ưu tiên của điện thoại, cắt một nhát: trái là hai thứ đổi
   **điều guide nói**, phải là đồ đạc. Chia theo nhóm, không theo chiều cao.
3. **Plan đếm 7 component, thực tế 8 + một host.** Đúng như phase 04 và 06 đã cảnh báo: đếm
   theo *màn*, không theo danh sách trong plan. `SettingsFooter` (chỗ duy nhất render số
   phiên bản) và `ClearHistoryDialog` không nằm trong plan; `RegionPickerBody` trong plan thì
   không tồn tại.

## Kiến trúc thực tế

`feature/settings/component/` — 8 file:

| Component | Ghi chú |
|---|---|
| `ApiKeyCard` + `StatusPill` | `StatusPill` là `private` trong cùng file — không nhánh nào vẽ nó riêng |
| `ModelPicker` + `ModelOption` | kèm hai extension `labelRes` / `summaryRes` của `GeminiModel` |
| `SettingsCard` | tự lấy `ScreenGutter`, nên cột tablet và list điện thoại không nhánh nào gõ mép |
| `ValueRow` · `SwitchRow` · `DestructiveRow` | một file `SettingsRow.kt` — một hàng nhìn ba lần |
| `ThemeRow` + `ChoiceDialog` (private) | **hàng và hộp thoại là một component**, xem dưới |
| `SovereigntyCard` | mục trong `ABOUT` |
| `ClearHistoryDialog` | chặn ở `PaneWidth.sheet` |
| `SettingsFooter` | chỗ duy nhất render `SharedBuildConfig.VERSION_NAME` |

`feature/sovereignty/component/` — 5 file: `SovereigntyDocument` (`SovereigntyHeading` +
`SovereigntyStatement` + `SovereigntyUnderstoodButton`), `CompassMark`, `SovereigntyPanel`
(`SovereigntyMapPanel` + `SovereigntyNote` + `frameAspect`), `SovereigntyCloseButton`,
`SovereigntyInk` (`SeaWash`, `LacquerChip`, hai alpha).

`feature/settings/SettingsHost.kt` — **host thứ ba của app**, sau `LensHost` và `ExploreHost`.
Lý do đúng như `LLM.md` §5 nói cho hai cái trước: ba nhánh effect kèm một quy tắc về cách hiện
chúng là *hành vi*, không phải sắp đặt. `content` nhận ba tham số: `state`, `onIntent`,
`SnackbarHostState`.

```
tablet/feature/settings/SettingsTabletScreen.kt   (216)
└── một scroll cho cả trang, Row hai cột chặn ở ContentWidth = 1114dp, canh giữa
    ├── cột trái  : INTELLIGENCE — ApiKeyCard · ModelPicker
    └── cột phải  : EXPERIENCE · APPEARANCE · DATA · ABOUT · SettingsFooter

tablet/feature/sovereignty/SovereigntyTabletScreen.kt   (137)
└── Row đỏ phủ toàn khung, hai pane
    ├── trái : cột 440dp — × ngoài vùng cuộn, rồi con dấu · tiêu đề · tuyên bố · Đã hiểu
    └── phải : SovereigntyMapPanel weight(1f) cao hết pane, bản đồ vẽ chạm bốn mép
```

**Sửa lại bố cục 04.08.2026 (sau khi phase đóng).** Bản đầu là một cột 440dp canh giữa với
`×` ghim TopEnd — đúng wireframe cũ, nhưng design chốt sau đó tách hai pane: chữ bên trái,
bản đồ bên phải to gần vuông, `×` về đầu cột trái. Kéo theo ba việc:

- `SovereigntyDocument` **tan thành ba khối** `internal` dùng chung. Nó từng là cả cột, với lý
  lẽ "tuyên bố giống hệt nhau trên mọi máy" — vẫn đúng với *chữ*, hết đúng với *cột*, vì cột là
  cách sắp đặt và §3 giao cách sắp đặt cho nhánh vẽ nó. Không nhánh nào giữ bản sao của một chữ
  hay một màu.
- `SovereigntyMapPanel` **nhận kích thước từ modifier**, và `RegionProjection` **giãn hai trục
  độc lập** để lấp đúng canvas được giao. Điện thoại đưa `frameAspect` nên `scaleX == scaleY`
  tuyệt đối (đã tính tay: tỉ lệ méo = 1.000000) — không đổi một pixel, panel vẫn đúng hình bản
  đồ nên trang không nhảy lúc asset về. Tablet đưa cả pane 776×768dp, khung 1.56 giãn vào ô
  1.01 → **bề ngang còn 65%**: Việt Nam vẽ hẹp hơn thật.

  Chốt như vậy sau khi chủ dự án bác cả hai đường còn lại, và đây là cái giá đã cân:
  *fit* (bản đầu) để bản đồ nằm giữa với một phần ba panel là nước trống trên dưới; *cover*
  lấp kín nhưng ở đúng pane này xén mất **0.84° Tây Bắc** (Điện Biên, Lai Châu) khỏi khung —
  không chấp nhận được trên màn tuyên bố chủ quyền; cắt lại khung JSON cho cao hơn thì phải
  dựng lại asset mà điện thoại cũng đọc. Giãn là cái duy nhất **không mất gì và không dời gì**:
  Hoàng Sa vẫn bắc Trường Sa, cả hai vẫn đông bờ biển và tây Philippines — đúng điều hình này
  cần nói. Lý lẽ nằm trong KDoc của `RegionProjection` để lần sau không ai "sửa" nó về `minOf`.
- **Tablet không vẽ `SovereigntyNote`.** Nó nói tuyên bố hiện ở đâu — một chú thích chân trang,
  mà bố cục này không có chân trang, chỉ có hai cột kết thúc ở hai độ cao khác nhau.

**Ba điểm đáng chép lại:**

- **`ThemeRow` gộp hàng và hộp thoại làm một.** `pickingTheme` là chuyện của *cái điều khiển*,
  không phải của nhánh nào — tách ra hai nhánh thì cờ đó được khai báo hai lần và lần thứ hai
  là lần sẽ quên. Đây là khuôn cho bất kỳ hàng-mở-picker nào sau này.
- **Đặt tên file theo quy ước `LLM.md` §10, không theo plan.** Plan gọi
  `SovereigntyOverlay.kt`; quy ước là `XTabletScreen.kt` cho một đích đến và `XPane.kt` cho
  một pane. File là `SovereigntyTabletScreen.kt`, composable bên trong tên `SovereigntyOverlay`
  — giữ cả hai. Tương tự, component đi vào `feature/<name>/component/` chứ không phải
  `feature/<name>/` như plan viết.
- **`ContentWidth` là mức *chặn*, không phải bề rộng.** Ở 1194dp nó không làm gì (cột ra 557).
  Ở cửa sổ 1664dp mà phase 07 đã chạy thật thì không có nó cột sẽ là 780 — quay lại đúng khoảng
  mà nhãn và giá trị thôi đọc được thành một dòng. Chặn rồi canh giữa: cửa sổ rộng hơn cho
  thêm lề, không cho thêm dòng dài.

## File liên quan

**Tạo (15):** `feature/settings/component/*.kt` (8) · `feature/settings/SettingsHost.kt` ·
`feature/sovereignty/component/*.kt` (5) · `tablet/feature/settings/SettingsTabletScreen.kt` ·
`tablet/feature/sovereignty/SovereigntyTabletScreen.kt`
**Sửa:** `mobile/feature/settings/SettingsScreen.kt` (755 → 178) ·
`mobile/feature/sovereignty/SovereigntyScreen.kt` (295 → 80) ·
`tablet/navigation/TabletNavGraph.kt` · `theme/Dimens.kt` (KDoc `PaneWidth.sheet`) ·
`DesignTokenTest.kt` (+`SettingsTabletScreen.kt` vào `HEADER_OWNERS`) ·
`ComposeStabilityReportTest.kt` (allowlist) · `LLM.md` §3 §5 §9 §11 §12 §13.5

## Todo

- [x] Tách `SovereigntyDocument`, thử lại trên điện thoại
- [x] Rút component settings (8, không phải 7)
- [x] ~~Tách `RegionPickerBody`~~ — không tồn tại; thay bằng chặn 440dp cho hộp thoại đang có
- [x] `SettingsTabletScreen.kt`
- [x] `SovereigntyTabletScreen.kt` (`SovereigntyOverlay`); ~~`CenterDialog.kt`~~ — không cần
- [x] Nối route và con dấu rail
- [x] `LLM.md` §11 — hàng #13 sang Fixed, và hàng #11 mất `SettingsScreen` (thành 11d)

## Đã kiểm chứng

Pixel Tablet API 35 ở 1280×800dp, và emulator điện thoại 411×694dp:

| Tiêu chí | Kết quả |
|---|---|
| Settings hai cột, không hàng nào quá ~600dp | ✅ đo trên ảnh: 523dp mỗi cột |
| Dán khoá riêng → lưu, hiện `ACTIVE`, xoá được | ✅ "Your own key is in use" + nút Clear + snackbar *API key saved*; xoá xong về "Using the key built into this app" |
| Bộ chọn giao diện là hộp thoại giữa màn | ✅ 438dp đo trên ảnh (`PaneWidth.sheet` = 440) |
| Chủ quyền mở được từ cả hai lối | ✅ con dấu ở rail **và** mục trong `ABOUT` |
| Màn chủ quyền trên điện thoại không đổi | ✅ nút × vẫn ở đầu vùng cuộn, thứ tự y nguyên |
| Settings trên điện thoại không đổi | ✅ một cột, đúng thứ tự cũ |
| Hai cột ở tiếng Nhật và tiếng Thái | ✅ không cắt chữ, không nhóm nào bị chia lệch (`cmd locale set-app-locales`) |
| QĐ-4 sống qua đổi kích thước | ✅ đang đọc chủ quyền ở 1280×800dp → 411×731dp: vẫn ở chủ quyền, đổi sang vỏ điện thoại; back về Settings |

Kiểm lại sau khi sửa bố cục hai pane, cùng emulator ở 1280×800dp và 411×731dp:

| Tiêu chí | Kết quả |
|---|---|
| Hai pane: chữ 440dp trái, bản đồ chiếm phần còn lại | ✅ đo trên ảnh: pane bản đồ 490→1264dp |
| Bản đồ chạm bốn mép panel, không còn nước trống | ✅ đất vẽ tới sát mép trên và mép dưới |
| Không mất mảnh nào của khung | ✅ vẫn đủ Việt Nam, hai quần đảo, Philippines, Borneo, Hải Nam |
| Điện thoại không méo, không đổi | ✅ tỉ lệ méo = 1.000000, ảnh trùng khít bản trước |
| Cả hai lối thoát | ✅ `×` ở đầu cột trái, và **Đã hiểu** — cùng về Ống kính |
| Tiếng Việt: tiêu đề dài hơn, nút hẹp hơn | ✅ tiêu đề 2 dòng không cắt, "Đã hiểu" ôm sát chữ |
| Điện thoại giữ nguyên thứ tự cũ | ✅ ×, con dấu, tiêu đề, bản đồ đúng tỉ lệ khung, tuyên bố, note, nút full width |

Gate: `:shared:testAndroidHostTest` **41/41 xanh**. `DesignTokenTest` giờ quét **135 file**
(`feature: 116`, `mobile/feature: 10`, `tablet/feature: 9`). `UNSTABLE_CLASS_CEILING` giữ
nguyên 21 — không có class mới. Không composable mới nào mất `skippable`.

## Rủi ro — kết quả

| Rủi ro | Đã xảy ra chưa |
|---|---|
| Tách `SovereigntyDocument` đổi phạm vi recomposition | Không. Đã thử trên điện thoại ở bước 1 trước khi làm gì khác, và lại lần nữa lúc xong |
| Hai cột settings làm nhóm bị chia lệch ở tiếng Nhật/Thái | Không. Chia theo nhóm nên chiều cao lệch không đụng tới nội dung nhóm |
| Khoá API lộ trong log ở nhánh mới | Không. Nhánh tablet không có một dòng log nào; `ApiKeyCard` chỉ phát Intent |

## Bảo mật

Khoá API do người dùng dán vẫn đi qua `SaveApiKeyUseCase` và kho hiện có — `ApiKeyCard` nằm
ở `feature/settings/component/`, chỉ nhận `state` và phát `SettingsIntent`, không tự lưu,
không tự đọc, và cả hai nhánh gọi đúng một bản. Ô nhập vẫn `PasswordVisualTransformation`
mặc định và khoá đã lưu không bao giờ được vẽ lại — chỉ có một dòng trạng thái nói đang dùng
nguồn nào.

## Tiếp theo

[Phase 09](phase-09-tests-and-docs.md).
