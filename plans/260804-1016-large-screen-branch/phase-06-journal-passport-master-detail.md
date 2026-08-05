# Phase 06 — Journal master–detail + Passport 392

**Liên kết:** [plan.md](plan.md) · wireframe dòng 275-363 · `LLM.md` §5 (pane vs Route), §7
(điều hướng) · `JournalScreen.kt` (840 → **248**) · `PassportScreen.kt` (1059 → **258**) ·
`CollectionScreen.kt` (497 → **147**) · `VietnamMapCanvas.kt` (611, giữ nguyên)

## Tổng quan

**Ưu tiên:** P1 · **Trạng thái:** ☑ Xong 04.08.2026 — đã kiểm trên Pixel Tablet API 36 (1280 ×
800dp) và Galaxy A16 thật (411 × 890dp). Còn nợ: iPad.

Ghi chú thiết kế: *"Journal thành master–detail: danh sách ngày ở nguyên đó trong khi bản đồ
passport lấp phần còn lại."* Đây là màn duy nhất có pane cố định nằm **bên trái**.

## Nhận định then chốt

- **Hai màn con của Journal, wireframe chỉ vẽ một.** Passport có mặt ở pane phải (dòng 333-338).
  Collection không được vẽ. `Destinations.kt` nói rõ vì sao hai màn này đi cùng nhau: *"cả hai
  đều là cách nhìn lại một chuyến đi, nên đặt cạnh nhau là thứ giữ chúng khỏi đọc thành hai
  tính năng đối thủ."* Cùng lý lẽ đó dẫn tới: **cả hai cùng vào pane phải**.
- `VietnamMapCanvas.kt` 611 dòng dùng lại được nguyên vẹn — nó vẽ trên `Canvas`, tự co giãn.
  Nó cũng là một trong ba file được `DesignTokenTest` cho phép dùng `.sp` (§11 hàng #17); giữ
  nguyên vị trí file để danh sách cho phép đó không phải sửa. ☑ đã giữ.
- Thẻ chủ quyền ở đáy pane phải chính là `SovereigntyBanner.kt` đã có trong
  `core/designsystem/component/`. Không dựng lại. ☑

## Chốt lại QĐ về cơ chế pane — 04.08.2026

Plan gốc đề nghị **`NavHost` lồng** cho pane phải, viện `LLM.md` §7 *"điều hướng là Effect,
không bao giờ là cờ trong state"*. Chủ dự án chốt lại: **state cục bộ + `BackHandler` tường
minh.** Ba lý do, kiểm chứng được:

1. **§7 không bị vi phạm.** Quy tắc đó nói về **ViewModel** giữ cờ điều hướng. `JournalEffect`
   chỉ có đúng một case `ShowMessage` — không hề có case điều hướng nào; Journal xưa nay điều
   hướng qua lambda mà route trao cho. Bước 4 của plan gốc ("nối `JournalEffect` vào
   `navController` của pane") dựa trên một effect không tồn tại.
2. **`NavHost` lồng sẽ giành mất nút back.** `NavHost` tự gọi
   `navController.setOnBackPressedDispatcher(...)`, callback chạy LIFO, và controller lồng
   đăng ký **sau** controller gốc — nên chính nó nuốt cử chỉ back của shell. Đó là điều dòng
   "giảm thiểu rủi ro" của plan gốc cấm, và chặn nó cần shim composition local, mong manh trên
   CMP.
3. **Đã có tiền lệ.** `DiscoveryTabletScreen` (phase 05) đổi giữa năm trang bằng đúng một enum
   cục bộ. Pane này là cùng loại quyết định.

`BackHandler(enabled = pane != PASSPORT)` cho lại đúng hành vi mong muốn, bật/tắt tường minh.

## Kiến trúc — thực tế đã làm

**24 component**, mỗi file một cái, `internal` (plan liệt kê 8):

| Gói | File |
|---|---|
| `journal/component/` (9) | `PlacesCounter` · `ProgressRow` · `FilterChips` · `DayHeader` (+ `SeamRule`) · `StoryCard` (+ `StoryList`) · `EndOfDayCard` · `DiscoveryCard` · `EmptyJournal` (+ `EmptyFavorites`, `LocationPermissionPrompt`) · `JournalDays` (`LazyListScope.journalDays`) |
| `passport/component/` (11) | `PassportInk` (`PassportHairline`) · `LensButton` · `PassportProgress` · `PassportMap` · `PassportEmptyHint` · `ProvinceStamp` (+ `VietnamFlag`, `drawStar`) · `ProvinceHead` · `VisitRow` (+ `VisitCell`) · `DiscoveryStrip` (+ `DiscoveryTile`) · `ProvinceFootnotes` · `ProvinceSheet` (+ `ProvinceSheetData`, `rememberLastSelection`, `SheetEdge`, hình học sheet) |
| `collection/component/` (4) | `CollectionProgress` · `CollectionTile` (+ `LockedTile`, `CollectedBadge`) · `CollectionBoard` (`LazyListScope.collectionBoard`) · `EntrySheet` |

Thêm: `feature/discovery/component/DashedInk.kt` → **`core/designsystem/component/DashedInk.kt`**,
`public`. Bản `DashedRule` riêng của Passport giống hệt tới từng byte (dash 6f/10f, cap Round,
1.dp) — hai feature cùng vẽ, tức là đúng chỗ `LLM.md` §10 chỉ định.

```
tablet/feature/journal/JournalTabletScreen.kt          378 dòng
└── TwoPaneScaffold(fixedPaneWidth = PaneWidth.journalList, fixedPaneAtStart = true)
    ├── pane 392     : PageHeader(kicker "YOUR JOURNEY", "Journal", PlacesCounter)
    │                  · ProgressRow ×2 (selected = pane đang mở) · FilterChips
    │                  · journalDays(…)
    └── pane co giãn : when (pane) {
                         PASSPORT   -> PassportPane   (tablet/feature/passport/, 208)
                         COLLECTION -> CollectionPane (tablet/feature/collection/, 109)
                       }
```

**Hai `ProgressRow` chính là công tắc pane.** Trên điện thoại chúng đẩy sang màn khác nên không
có trạng thái "đang chọn"; ở đây chúng đứng cạnh pane nên thêm `selected: Boolean = false`.
Đây là ràng buộc *"component phải giống mobile"* chạy đúng ý đồ: tablet sắp lại component của
điện thoại chứ không vẽ một cụm tab mới.

**`JournalTabletRoute` giữ ba ViewModel** — journal, passport, collection — theo đúng tiền lệ
`DiscoveryTabletRoute` giữ hai. Pane là *stateless* (`state` + `onIntent`), nên §5 vẫn đúng:
chỉ Route chạm ViewModel. Cả ba cùng một back-stack entry, nên đổi pane qua lại không phải
giải mã lại 34 ảnh bìa tỉnh.

## File liên quan

**Tạo (27):** `feature/journal/component/*.kt` (9) · `feature/passport/component/*.kt` (11) ·
`feature/collection/component/*.kt` (4) · `core/designsystem/component/DashedInk.kt` (chuyển
lên) · `tablet/feature/journal/JournalTabletScreen.kt` · `tablet/feature/passport/PassportPane.kt` ·
`tablet/feature/collection/CollectionPane.kt`
**Sửa:** `mobile/feature/{journal,passport,collection}/*Screen.kt` ·
`tablet/navigation/{TabletNavGraph,RailDestinations,VietLensTabletApp}.kt` ·
4 file `feature/discovery/component/` (đổi import) · `androidHostTest/DesignTokenTest.kt` ·
`androidHostTest/ComposeStabilityReportTest.kt` · `LLM.md` §3 §5 §7 §9 §10 §11 §12
**Xoá:** `feature/discovery/component/DashedInk.kt`

## Todo

- [x] Rút 24 component journal + passport + collection
- [x] `JournalTabletScreen.kt` với `TwoPaneScaffold(fixedPaneAtStart = true)`
- [x] Đổi pane bằng state + `BackHandler` (thay `NavHost` lồng — xem mục QĐ ở trên)
- [x] Đồ thị tablet: `JOURNAL` / `PASSPORT` / `COLLECTION` cùng mở một arrangement
- [x] Giữ trạng thái cuộn khi đổi pane — `LazyListState` hoist lên trên chỗ rẽ
- [x] `railDestination()` quyết định rail, không phải `isTopLevel()` (lỗi bắt được, xem dưới)
- [x] Ba Route mobile được bổ sung tách `Route`/`Screen` còn thiếu
- [x] `LLM.md` §3 §5 §7 §9 §10 §11 (hàng 11, 11c, 13) §12
- [x] Biên dịch ba đích: `:shared:compileAndroidMain`, `:app:compileDebugKotlin`,
      `:shared:compileKotlinIosSimulatorArm64` — cả ba xanh
- [x] `:shared:testAndroidHostTest` — **41/41 xanh**; `DesignTokenTest` quét 85 → **111** file
- [ ] Thử trên iPad thật / simulator — nợ chung từ phase 02

## Tiêu chí hoàn thành

Đã kiểm trên **Pixel Tablet API 36**, bản debug `com.duylt.trave.vietlensai.dev`:

| Tiêu chí | Kết quả |
|---|---|
| Mở Journal trên khổ 1280×800dp → danh sách ngày trái, bản đồ passport phải, không phải bấm gì | ☑ pane 392 (YOUR JOURNEY · Journey log · 1 DISCOVERY · hai ProgressRow · All/Favourites · Today · thẻ find · End of day) ‖ pane phải (Travel passport · 0 of 34 · 0% EXPLORED · bản đồ + Hoàng Sa/Trường Sa · LOCATION IS OFF · thẻ chủ quyền) |
| Bấm collection → pane phải đổi, danh sách ngày đứng yên đúng chỗ đang cuộn | ☑ đã cuộn lệch trước khi bấm; sau khi đổi pane, offset y hệt. Lưới 5 cột, viền chọn chuyển sang hàng Culture collection |
| Bản đồ tỉnh: bấm một tỉnh → panel peek trong **pane**, không phải trong cửa sổ | ☑ "NOT YET / Tỉnh Phú Thọ / No stamp here yet", tem nghiêng, viền đỏ trên bản đồ |
| Back | ☑ ba tầng đúng thứ tự: đóng panel tỉnh → về pane passport → rời Nhật ký về Ống kính |
| Deep link / đổi kích thước vào `Routes.COLLECTION` → Journal với pane phải là collection | ☑ và rail vẫn hiện, tab Journal sáng — **sau khi sửa lỗi ở mục dưới** |
| Đổi kích thước (QĐ-4) | ☑ 1280×800dp ↔ 281×500dp: qua lại giữ nguyên chỗ đang đứng và back stack |
| `JournalScreen.kt` và `PassportScreen.kt` đều dưới 600 dòng | ☑ **248** và **258** (và `CollectionScreen.kt` 147) |
| Crash | ☑ buffer `crash` rỗng, 0 `FATAL EXCEPTION` trên cả hai máy |

Và ở **khổ mobile** (Galaxy A16 thật, tiếng Việt, giao diện sáng) — nhánh mobile sau khi rút:

| Tiêu chí | Kết quả |
|---|---|
| Nhật ký | ☑ header + đếm, hai ProgressRow (viền thường, không có trạng thái chọn — đúng), chips, ngày, thẻ find, End of day |
| Lời nhắc quyền vị trí | ☑ "Đóng dấu hộ chiếu" + nút "Cho phép" — `LocationPermissionPrompt` sau khi đổi sang `isBlocked`/`onGrant` |
| Hộ chiếu | ☑ PageHeader có back chip, progress, bản đồ đóng khung, thẻ LOCATION IS OFF, banner chủ quyền |
| Bộ sưu tập | ☑ 3 cột, banner chủ quyền, tiêu đề nhóm "FOOD 0/16" |

## Lỗi bắt được khi chạy trên máy

**Rail biến mất khi vào thẳng `Routes.PASSPORT` / `Routes.COLLECTION` ở khổ tablet.**

Hai route đó không nằm trong `Routes.TOP_LEVEL` — đúng với điện thoại, nơi chúng là màn chi
tiết đẩy đè và thanh dưới phải ẩn. Trên tablet chúng **là** màn Nhật ký. Shell vẫn hỏi
`isTopLevel()`, nên rail ẩn, và người dùng nhận một màn top-level không có lối đi nào ngoài
cử chỉ back của hệ thống. Tái hiện được bằng đúng thao tác của người dùng thật: đang ở Bộ sưu
tập khổ điện thoại rồi xoay/phóng cửa sổ to ra.

**Sửa:** `railDestination()` — chứ không phải `isTopLevel()` — quyết định rail có hiện hay
không, và nó ánh xạ `PASSPORT`/`COLLECTION` → `RailDestination.JOURNAL`. **Không** thêm hai
route vào `Routes.TOP_LEVEL`: membership ở đó là một sự thật của app, thêm vào là đẩy chúng
lên thanh dưới của điện thoại. Ghi ở `LLM.md` §7.

## Lệch khỏi plan, và lý do

| Lệch | Vì sao |
|---|---|
| State + `BackHandler` thay `NavHost` lồng | Xem mục "Chốt lại QĐ" ở trên. Chủ dự án chốt 04.08.2026 |
| 24 component thay vì 8 mà plan liệt kê | Cùng bài học phase 04–05: đếm theo *màn*, không theo *panel* |
| **Rút cả `CollectionScreen`**, plan không nhắc | Plan ghi *"CollectionScreen sắp lại theo lưới"* nhưng không liệt kê component nào. Bỏ lại thì `CollectionTile`, `LockedTile`, `EntrySheet`… đều là `private` và tablet phải chép — hỏng đúng ràng buộc của cả plan |
| `DashedInk.kt` chuyển lên `core/designsystem/component/` | Passport giữ một bản `DashedRule` giống hệt tới từng byte. Hai feature cùng vẽ → `LLM.md` §10. Journal **không** dùng nó: seam của nó là dash 4/5 cap thẳng, một mark khác |
| Ba Route mobile được tách `Route`/`Screen` | `LLM.md` §11 hàng #13 nói sovereignty là feature **duy nhất** thiếu tách — sai: journal, passport, collection cũng thiếu. Đang viết lại thân ba file này thì tách là gần như miễn phí, và tablet cần bản stateless của passport/collection dù sao |
| `ProgressRow` có thêm `selected` | Không có nó thì trên tablet không cách nào biết pane nào đang mở. Đây là câu hỏi rail trả lời cho bốn nơi, hỏi lại cho hai |
| `journalDays` / `collectionBoard` là `LazyListScope` extension | Hai nhánh đặt cùng bộ item vào hai container cuộn khác nhau; composable sẽ phải lồng cuộn trong cuộn |
| Pane phải chỉ có **hai** trạng thái, không phải ba | "Chi tiết một ngày" trong plan không có màn tương ứng trong app — một ngày là header + danh sách find, đã nằm ngay trong cột trái. Bấm một find vẫn mở `Routes.DISCOVERY` toàn cửa sổ, đúng như phase 05 |
| `PassportPane`/`CollectionPane` không có `koinViewModel()` | §5 chỉ cho Route chạm ViewModel. `JournalTabletRoute` giữ cả ba, theo đúng tiền lệ `DiscoveryTabletRoute` |

## Rủi ro — kết quả

| Rủi ro | Kết quả |
|---|---|
| `NavHost` lồng làm rối nút back | Không phát sinh — đã bỏ `NavHost` lồng. Back ba tầng đã thử trên máy, đúng thứ tự |
| Bản đồ tỉnh vẽ lại mỗi lần danh sách ngày đổi | Không thấy giật ở 1280×800dp. `VietnamMapCanvas` nằm ở nhánh composition khác hẳn cột ngày |
| Passport hết là màn riêng, mất lối vào trên tablet | Vẫn là một `Routes`, chỉ đổi chỗ hiển thị — **và đây chính là chỗ lỗi rail nằm**, xem mục trên |
| Đổi kích thước làm mất back stack | Không — đồ thị hai shell vẫn khớp cấu trúc; đã thử qua lại hai lần |

## Bảo mật

Không có thay đổi.

## Tiếp theo

[Phase 07](phase-07-explore-tablet.md) — Explore. Lưu ý: `ExploreScreen` 406 dòng, và §12 của
`LLM.md` liệt kê bốn chi tiết khởi động bản đồ **không được xoá**.
