# Phase 07 — Explore: bản đồ full-bleed + hai cụm nổi

**Liên kết:** [plan.md](plan.md) · [architecture-options.md](architecture-options.md) **QĐ-2 đã chốt** ·
`ExploreScreen.kt` (397) · `PlaceDetailSheet.kt` (354) · `PlaceMap.kt` + hai `actual` ·
`LLM.md` §12 (bốn mảnh khởi động của bản đồ), §13.5 (`OverlayIconButton`)

## Tổng quan

**Ưu tiên:** P2 · **Trạng thái:** ☑ Xong 04.08.2026

QĐ-2 chốt **giữ bản đồ**. Feed trong wireframe — *Where next · Happening right now · Continue
the thread · Trail of the week · Việt Nam 101* — là thiết kế đi trước code, và là một plan
khác. Ở đây chỉ sắp lại màn bản đồ đang chạy cho khổ rộng.

Bố cục do chủ dự án chỉ định (wireframe không vẽ màn này):

- **Góc trên bên trái:** container *"Around you"*, và **hai nút icon xếp theo chiều dọc** bên
  dưới nó.
- **Bên phải:** danh sách kết quả đề xuất, cột dọc.

## Nhận định then chốt

- **Hai nút icon phải ra khỏi header.** Trên mobile chúng nằm trong `trailing` của
  `OverlayHeader(style = Card)` — `ExploreScreen.kt:242-262`, `explore_recenter` ("Centre on
  me") và `explore_refresh` ("Search again"). Tablet gọi cùng `OverlayHeader` nhưng
  `trailing = null`, rồi xếp hai nút thành `Column` ngay dưới thẻ. Không dựng nút mới.
  *(Plan viết `OverlayIconButton` ở đây và **sai** — mobile dùng `MapControl`; xem "Đã làm
  khác plan" bên dưới.)*
- **Bản đồ trải full-bleed, hai cụm nổi lên trên** — đây là cách plan hiểu chỉ định "góc trái
  trên màn hình" / "bên tay phải màn hình", cả hai đều nói vị trí *trên màn hình* chứ không
  nói chia pane. Hệ quả kỹ thuật: một `Box`, không phải `Row`; bản đồ không đổi kích thước khi
  danh sách xuất hiện hay biến mất, nên camera không bị đẩy lệch.
- **Mọi lớp nổi phải nuốt cử chỉ kéo.** `Modifier.mapCover()` mang một `pointerInput` rỗng
  đúng vì lý do này (`LLM.md` §12): thiếu nó thì kéo trên lớp phủ sẽ pan bản đồ bên dưới.
  Cột danh sách bên phải là một mặt cuộn nằm trên bản đồ — nó **phải** có cùng bảo vệ, nếu
  không cuộn danh sách sẽ kéo luôn bản đồ.
- **Bốn mảnh khởi động là ngân sách khung hình đo được, không phải nghi thức.** Mang cả bốn
  sang, đặc biệt (b) — dựng bản đồ ngay khi *quyền* được trả lời chứ không đợi định vị đầu
  tiên — và (d) — yêu cầu camera **đầu tiên** không animate, nếu không mỗi lần mở tab là bay
  từ Hà Nội về.
- `val onIntent = remember(viewModel) { viewModel::onIntent }` chỉ tồn tại ở `ExploreRoute` và
  có lý do (`LLM.md` §12, MVI doc §8). `ExploreTabletRoute` cũng cần — cùng lý do, cùng subtree đắt.
- Bản ghi nhớ dự án: *"Explore phải độc lập với Passport — không bản đồ tỉnh."* Giữ nguyên;
  bản đồ ở đây là bản đồ địa điểm.

## Yêu cầu

**Chức năng:** thấy bản đồ và địa điểm gần, chọn một địa điểm để xem chi tiết, recenter, tìm
lại, và xử lý đủ ba trạng thái hỏng — chưa cấp quyền, đang tải, thất bại.
**Phi chức năng:** không chậm hơn bản mobile ở khung hình đầu tiên khi mở tab; cuộn danh sách
không làm bản đồ nhúc nhích.

## Kiến trúc

```
tablet/feature/explore/ExploreTabletScreen.kt
└── Box(fillMaxSize)
    ├── PlaceMap + mapCover()                        full-bleed, dưới cùng
    ├── Column · align(TopStart)                     cụm trái, nổi
    │   ├── OverlayHeader(title = explore_title,
    │   │                 subtitle = explore_nearby_count,
    │   │                 style = Card, trailing = null)
    │   └── Column                                   hai nút xếp dọc
    │       ├── OverlayIconButton(explore_recenter)
    │       └── OverlayIconButton(explore_refresh)
    ├── Column · align(TopEnd) · PaneWidth.journalList
    │   └── danh sách kết quả — PlaceRow, cuộn, nuốt cử chỉ kéo
    │       chọn một cái → cột đổi thành PlaceDetailBody, không phải sheet trượt
    └── ba lớp phủ trạng thái                        trên cùng, đục
```

Bề rộng cột phải dùng lại `PaneWidth.journalList` (392) chứ không thêm hằng thứ sáu — cùng vai
trò *"cột danh sách cạnh một mặt phẳng lớn"*, và một token dùng hai nơi vẫn là một token.

~~Bề rộng thẻ *"Around you"* là một **số đo, không phải khoảng cách**, nên nó là một
`private val` trong file này.~~ **Bỏ khi triển khai:** `OverlayHeader` để thẻ tự co theo nội
dung, nên không ràng buộc gì cả là câu trả lời đúng trong cả tám ngôn ngữ *theo cấu trúc*,
thay vì một con số đo trong một ngôn ngữ. Không có hằng nào được thêm.

Rút sang `feature/explore/component/`: `PlaceRow`, `PlaceDetailBody` (thân của
`PlaceDetailSheet`, tách khỏi vỏ sheet), `RecenterButton`, `RefreshButton`, ba lớp phủ trạng thái.

## File liên quan

**Tạo:** `feature/explore/component/*.kt` · `tablet/feature/explore/ExploreTabletScreen.kt`
**Sửa:** `mobile/feature/explore/ExploreScreen.kt` · `feature/explore/PlaceDetailSheet.kt`
(tách thân khỏi vỏ) · `tablet/navigation/VietLensTabletApp.kt`
**Không đụng:** `PlaceMap.kt` và hai `actual` — đổi package là đổi cả hai nền

## Các bước

1. Tách `PlaceDetailSheet` thành `PlaceDetailBody` (dùng chung) + vỏ sheet (chỉ mobile).
   Thử trên điện thoại trước khi đi tiếp.
2. Rút `PlaceRow`, hai nút, ba lớp phủ trạng thái.
3. `ExploreTabletScreen.kt` — `Box` full-bleed, cụm trái xếp dọc, cột phải.
4. Cho cột phải cùng lớp bảo vệ cử chỉ như `mapCover()`; kiểm bằng tay: cuộn danh sách, bản
   đồ phải đứng im.
5. Mang đủ bốn mảnh khởi động và `remember(viewModel) { viewModel::onIntent }`.
6. Đo khung hình đầu tiên khi mở tab trên tablet, so với mobile.

## Todo

- [x] Tách `PlaceDetailBody`, thử sheet mobile còn nguyên
- [x] Rút component explore
- [x] `ExploreTabletScreen.kt` — `Box` full-bleed
- [x] Cụm trái: header không `trailing` + hai nút xếp dọc
- [x] Cột phải 392, cuộn, nuốt cử chỉ kéo
- [x] Bốn mảnh khởi động + `onIntent` memo hoá
- [ ] Đo khung hình đầu tiên — **chưa làm**, xem "Còn nợ" bên dưới
- [x] Nối route

## Đã làm khác plan — và vì sao

- **Không dựng `RecenterButton`/`RefreshButton` bằng `OverlayIconButton`.** Plan nói vậy dựa
  trên `LLM.md` §13.5, nhưng mobile **không** dùng nó ở màn này: nó dùng `MapControl` — đĩa
  màu `surface` — và §13.5 nói rõ `OverlayIconButton` là kính đen cho nút *trên ảnh chụp*.
  Cùng lập luận `OverlayHeaderStyle.Card` đưa ra: bản đồ nhạt, phủ đen lên là một vết bầm.
  Đổi riêng bên tablet là phá đúng ràng buộc "component giống mobile". Nên `MapControl` được
  giữ nguyên và gói vào hai nút có tên, mỗi nút mang sẵn intent của mình, ở
  `feature/explore/component/MapControls.kt` — hai nhánh không thể nối nhầm nút nào ra intent
  nào.
- **Không thêm hằng bề rộng cho thẻ *"Around you"*.** Plan đòi một `private val` đo theo
  ngôn ngữ dài nhất trong tám. Nhưng `OverlayHeader` để thẻ tự co theo nội dung, nên **không
  ràng buộc gì cả** là câu trả lời đúng trong cả tám ngôn ngữ theo cấu trúc, thay vì một con
  số đo trong một ngôn ngữ rồi hy vọng. Rủi ro "tràn chữ ở tiếng Nhật/Thái" biến mất chứ
  không phải được giảm thiểu.
- **Rút thêm `ExploreHost.kt`.** Không có trong plan, nhưng Route của Explore giữ ba thứ
  không phải layout — cầu nối quyền, chỗ thu effect, và `remember(viewModel) { … }`. Đây đúng
  bài học phase 04 đã ghi. Xem `LLM.md` §5.
- **`PlaceRow` thành `PlaceCard` dùng chung.** Plan gọi tên `PlaceRow` như một component mới;
  thực tế thẻ mà strip của điện thoại đang vẽ *là* thứ cột dọc cần, chỉ khác `modifier` định
  bề rộng. Một component, hai bề rộng — thay vì hai component sẽ lệch nhau sau lần sửa đầu.
- **Bốn lớp mới, không phải sáu component.** `feature/explore/` nay có `component/` (6 file),
  `PlaceLabels.kt`, `MapTheme.kt` và `ExploreHost.kt`. `ExploreComponents.kt` và
  `PlaceDetailSheet.kt` bị xoá; nội dung đi hết vào `component/`.

## Lỗi tìm thấy khi chạy thật

**Cột kết quả vẽ một thẻ kem rỗng suốt lượt tìm đầu tiên.** Nhánh `else` của `when` — thứ
mobile dùng để vẽ `PlaceStrip` — trên điện thoại vô hại vì một `LazyRow` rỗng không có chiều
cao lẫn màu; trên tablet nó là một `Surface`, nên một panel rỗng rộng gần một phần ba cửa sổ
đứng im suốt một vòng gọi Overpass (45 giây khi mirror timeout). Sửa bằng cách đổi nhánh
`else` thành điều kiện `state.places.isNotEmpty()` — spinner trên thẻ *"Around you"* mới là
thứ nói "đang tìm". Thấy ở cửa sổ 1664 × 768 dp, 04.08.2026.

## Đã thử đến đâu

Chạy trên emulator Android 16 ép về **1664 × 768 dp** (`wm size 3120x1440` + `wm density 300`,
đã trả lại nguyên trạng), fix GPS thật ở Hà Nội, 12 địa điểm thật từ Overpass:

- Bản đồ trải kín, thẻ *"Around you · 12 PLACES WITHIN 5 KM"* góc trên trái, hai nút tròn xếp
  dọc ngay dưới nó, cột kết quả bên phải. Không sheet nào trượt lên. ✓
- Cuộn danh sách bên phải → **bản đồ đứng im tuyệt đối**, so hai ảnh chụp liên tiếp. ✓
- Chọn "Pho 10" → cột đổi thành `PlaceDetailBody` (ảnh Wikimedia, chip 418 m + Vietnamese,
  nút Start đỏ, giờ mở cửa, web, điện thoại, dòng ghi nguồn), bản đồ bay tới địa điểm và
  marker nằm giữa khung — cách mép cột hơn 400 dp, không bị che. ✓
- `BackChip` và **cả cử chỉ back của hệ thống** đều trả cột về danh sách chứ không rời tab. ✓
- Recenter → bay về khổ neighbourhood với đủ 12 marker. ✓
- Trạng thái hỏng: trên Pixel Tablet không có fix GPS, `MapFailureCover` hiện đúng với nút
  thử lại. ✓
- Mobile không đổi: cùng bản build, màn Explore điện thoại vẫn là bản đồ + thẻ + hai nút
  trong header. ✓
- 41 test `androidHostTest` xanh; `DesignTokenTest` quét **119 file** (feature 102, mobile 10,
  tablet 7); biên dịch sạch cả `compileAndroidMain` lẫn `compileKotlinIosSimulatorArm64`.

## Còn nợ

- **Chưa đo khung hình đầu tiên** (bước 6). Cần Perfetto hoặc `FrameMetrics` trên một máy
  tablet thật; emulator không cho một con số đáng tin. Bốn mảnh khởi động đã mang sang đủ và
  có KDoc, nên rủi ro là hồi quy chậm chứ không phải thiếu cơ chế.
- **Chưa thử trên iPad.** Cùng tình trạng với phase 04–06.
- **Chưa rà tám ngôn ngữ bằng mắt.** Thẻ tự co theo nội dung nên tràn chữ không còn khả dĩ về
  mặt cấu trúc, nhưng cột 392 dp giữ `PlaceCard` thì chưa xem ở tiếng Nhật/Thái.

## Tiêu chí hoàn thành

- Mở Explore trên iPad → bản đồ trải kín, thẻ *"Around you"* với hai nút xếp dọc ở góc trên
  trái, danh sách kết quả thành cột bên phải. Không sheet nào trượt lên.
- Cuộn danh sách bên phải → bản đồ **không** di chuyển.
- Chọn một địa điểm → cột phải đổi nội dung, camera bản đồ không nhảy.
- Bấm recenter và tìm lại → hoạt động đúng như mobile.
- Chưa cấp quyền / mất mạng → đúng ba trạng thái như mobile.
- Mở tab lần đầu không chậm hơn mobile một cách thấy được.

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Cuộn danh sách kéo theo bản đồ | ✓ Đã chặn: cột mang `pointerInput` rỗng như `mapCover()`. Thử tay: cuộn hết danh sách, bản đồ đứng im tuyệt đối |
| Bốn mảnh khởi động bị bỏ vì "trông thừa" | ✓ Chép nguyên KDoc sang; `LLM.md` §12 nay nói rõ mảnh (c) áp cho **mọi** lớp nổi, không riêng ba lớp phủ |
| Thẻ *"Around you"* tràn chữ ở tiếng Nhật/Thái | ✓ Không còn khả dĩ: thẻ tự co theo nội dung, không bị ràng buộc bề rộng nào |
| Tách sheet làm hỏng sheet mobile | ✓ Cùng bản build, sheet điện thoại còn nguyên |
| Cột phải che mất địa điểm đang chọn trên bản đồ | ✓ Không cần đẩy camera: ViewModel đã đưa địa điểm vào **giữa** bản đồ, mà giữa cửa sổ cách mép cột hơn 400 dp. Đo trên máy |

## Bảo mật

Khoá Maps đọc từ `MAPS_API_KEY` trong manifest, không đổi. **Không** thêm khoá nào vào nhánh
tablet.

## Tiếp theo

[Phase 08](phase-08-settings-and-sovereignty-tablet.md).
