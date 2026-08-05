# Phase 04 — Lens: viewfinder co giãn + panel phải 310

**Liên kết:** [plan.md](plan.md) · [phase-03](phase-03-tablet-shell-rail-and-panes.md) ·
wireframe dòng 61-166 · `LLM.md` §11 hàng #11 · `LensScreen.kt` (2183 dòng → **388**)

## Tổng quan

**Ưu tiên:** P1 · **Trạng thái:** ☑ Xong 04.08.2026 — đã kiểm cả hai nhánh: Pixel Tablet
API 36 (tablet) và Pixel 7 API 36 (mobile). Còn nợ: iPad, và một lần chụp bằng camera vật lý.

Wireframe: viewfinder chiếm phần co giãn bên trái, shutter và những gì nó sinh ra nằm trong
panel cố định 310dp bên phải. Ghi chú của thiết kế nói thẳng lý do: *"Camera giữ panel riêng
bên phải để shutter nằm dưới ngón cái khi cầm tablet hai tay."*

## Nhận định then chốt

- **`LensScreen.kt` 2183 dòng là file lớn nhất dự án.** Không rút component ra thì tablet phải
  chép lại — và ràng buộc "component giống mobile" tan ngay. Bước 1 của giai đoạn này là công
  việc trả nợ `LLM.md` §11 hàng #11, làm cho cả hai nhánh cùng hưởng.
- **`SHUTTER_INSET` không còn tồn tại** — nó đã thành `MESSAGES_BOTTOM_INSET` từ đợt trước.
  Nguyên tắc thì vẫn nguyên: đây là *vị trí đo được*, và con số của mobile (160, đo theo hàng
  chip + hàng shutter) không mang sang được. Tablet có con số riêng, 144, đo theo vòng zoom
  và thanh ngôn ngữ nằm ở đáy khung.
- `state.recentDiscoveries` đã có sẵn trong `LensContract.kt:58`. Mobile vẽ nó bằng
  `RecentCaptureStack` — chồng bài, cố ý không phải dải cuộn, vì dải cuộn ăn nguyên một hàng
  của viewfinder. **Trên tablet lý lẽ đó không còn**: panel 310 có sẵn chiều dọc. Đây là chỗ
  duy nhất tablet được phép trình bày khác, và lý do đã ghi vào KDoc của cả hai bên.
- `VolumeShutterBus` và `CameraController` không đổi. Camera vẫn là `expect/actual`.

## Ba quyết định chốt lúc triển khai

| Câu hỏi | Chốt | Vì sao |
|---|---|---|
| Hàng chip mode ở đâu | **Pane trái, cùng hàng với 4 nút camera** — theo wireframe dòng 64-85 | Sơ đồ trong `plan.md` đặt nó ở panel phải; đó là diễn giải của người lập plan, còn mandate nói *cách sắp xếp tuân theo wireframe*. Panel 310 cũng không đủ rộng cho 5 chip ở tiếng Việt/Pháp |
| Phần không-layout của `LensRoute` | **Tách ra `feature/camera/LensHost.kt` dùng chung** | ~90 dòng gồm coroutine chụp với khối `finally` giải phóng shutter và observer vòng đời cho phím âm lượng. Đó là *hành vi*, mà `LLM.md` §3 cấm nhánh trình bày sở hữu hành vi. Chép sang là một lần sửa bug chỉ vá được một nửa |
| Nút thư viện ảnh | **Giữ, đặt bên trái shutter trong panel** | Wireframe không vẽ. Bỏ đi nghĩa là xoay ngang iPad thì mất một tính năng — hồi quy, không phải rút gọn |

## Kiến trúc

`feature/camera/component/` — **14 file, mỗi file một component, `internal`**:

| File | Nội dung |
|---|---|
| `ShutterButton.kt` | hoạ tiết Đông Sơn, giữ nguyên 78dp — xem ghi chú dưới |
| `ModeChipRow.kt` | 5 chip mode |
| `CameraToolRow.kt` | flash · lưới · đổi ống kính · hẹn giờ, + `ToolButton` |
| `Viewfinder.kt` | preview + lưới + chạm lấy nét + chụm để zoom |
| `FocusReticle.kt` | vòng lấy nét + thanh phơi sáng |
| `ZoomDial.kt` | vòng zoom + `ZoomDriver` |
| `TranslateLanguageBar.kt` | capsule nguồn ⇄ đích + `LanguageChip` |
| `LanguageSheet.kt` | sheet chọn ngôn ngữ |
| `AnalysingOverlay.kt` | *Reading the scene… · MATCHING · HERITAGE ARCHIVE* |
| `CountdownOverlay.kt` | đếm ngược hẹn giờ |
| `CaptureHintBubble.kt` | mẹo ngắm, một lần mỗi phiên |
| `GalleryButton.kt` | mở thư viện ảnh |
| `RecentCaptureCard.kt` | thẻ ảnh đơn, dùng cho cả chồng bài lẫn danh sách |
| `CameraPrompts.kt` · `LensSnackbars.kt` · `LensModeText.kt` · `CameraFormat.kt` | lời nhắc quyền, hai snackbar, `labelRes`/`hintRes`, định dạng số |

Còn lại ở mỗi nhánh: `RecentCaptureStack` (chỉ mobile), `RecentScanList` (chỉ tablet).

```
tablet/feature/camera/LensTabletScreen.kt
└── TwoPaneScaffold(fixedPaneWidth = PaneWidth.lensPanel, fixedPaneAtStart = false)
    ├── pane co giãn : Row { ModeChipRow · CameraToolRow }
    │                  CameraFrame { Viewfinder · CaptureHintBubble
    │                                TranslateLanguageBar · ZoomDial
    │                                CountdownOverlay · AnalysingOverlay · Snackbar }
    └── pane 310     : Kicker("READY TO SCAN") · modeTitle · modeHint
                       GalleryButton · ShutterButton
                       Kicker("RECENT SCANS") · RecentScanList
```

**Shutter giữ nguyên 78dp** dù wireframe vẽ 104/80. Kích thước không phải tham số, ở cả hai
nhánh: nhận số đo của wireframe nghĩa là biến cỡ shutter thành lựa chọn của nơi gọi, và từ đó
hai nhánh sở hữu hai cái nút sẽ trôi xa nhau ở lần sửa sau. Ràng buộc của dự án là tablet
*sắp xếp lại* component của mobile, không phải *vẽ lại*.

**Overlay nằm trong khung, không phủ cả cửa sổ.** Mobile phủ toàn màn vì màn hình chỉ có một
cột. Ở tablet, một tấm scrim kéo qua panel sẽ che mất chính cái shutter dùng để huỷ việc mà
scrim đang báo.

## File liên quan

**Tạo:** `feature/camera/component/*.kt` (14) · `feature/camera/LensHost.kt` ·
`tablet/feature/camera/LensTabletScreen.kt` · `tablet/feature/camera/RecentScanList.kt`
**Sửa:** `mobile/feature/camera/LensScreen.kt` (2183 → 388) ·
`tablet/navigation/TabletNavGraph.kt` (route `LENS` → `LensTabletRoute`) ·
`androidHostTest/…/DesignTokenTest.kt` (`POPULATED_BRANCHES`) ·
`androidHostTest/…/ComposeStabilityReportTest.kt` (allowlist + trần 20 → 21) ·
`composeResources/values*/strings.xml` (2 chuỗi × 8 file)

## Todo

- [x] Rút 14 component (plan dự tính 6 — xem "Lệch khỏi plan")
- [x] `LensHost.kt` — phần Route dùng chung
- [x] `LensScreen.kt` gọi component — **388 dòng**, đã thử lại trên khổ điện thoại
- [x] `RecentScanList` + KDoc giải thích vì sao không dùng chồng bài
- [x] `LensTabletScreen.kt` + `LensTabletRoute`
- [x] Hai chuỗi mới `camera_ready_to_scan` / `camera_recent_scans` — đủ tám ngôn ngữ
- [x] Nối route trong `TabletNavGraph.kt`
- [x] `POPULATED_BRANCHES` thêm `tablet/feature` — `DesignTokenTest` báo `tablet/feature: 2`
- [x] Biên dịch ba đích: `:shared:compileAndroidMain`, `:app:compileDebugKotlin`,
      `:shared:compileKotlinIosSimulatorArm64` — cả ba xanh
- [x] `:shared:testAndroidHostTest` — **36/36 xanh**
- [x] Thử trên Pixel Tablet API 36 — bảng dưới
- [x] Chụp thử ở khổ điện thoại trên **Pixel 7 API 36** (1440×3120 @560dpi = 411 × 891dp),
      cài mới nên đi qua cả luồng xin quyền — bảng dưới
- [x] `LLM.md` §11 — `LensScreen` chuyển sang bảng Fixed (hàng 11a)
- [ ] Thử trên iPad thật / simulator — nợ chung từ phase 02
- [ ] Chụp bằng camera **vật lý** — cả hai máy đều là emulator. Máy Samsung thật đang khoá
      màn hình. Đường ống CameraX và vòng gọi Gemini đã chạy thật, chỉ có ảnh vào là ảnh giả

## Tiêu chí hoàn thành

Đã kiểm trên **Pixel Tablet API 36 (2560×1600 @320dpi = 1280 × 800dp)**, bản debug
`com.duylt.trave.vietlensai.dev`:

| Tiêu chí | Kết quả |
|---|---|
| Bố cục đúng wireframe | ☑ rail · chip mode + 4 nút camera một hàng · viewfinder · panel 310 với kicker/tên mode/gợi ý/shutter/RECENT SCANS |
| Chụp được ảnh | ☑ shutter mờ đi, overlay *"Looking at your photo… / Gathering the history behind it"* phủ **đúng khung**, panel vẫn đọc được; xong thì shutter sáng lại |
| Đổi mode | ☑ bấm *Translate* → tiêu đề panel đổi sang "Translate", gợi ý đổi sang "Hold steady over the text" |
| Cặp ngôn ngữ nguồn/đích | ☑ capsule 🌐 Auto ⇄ 🇬🇧 English hiện ở đáy khung khi vào mode dịch |
| Danh sách quét gần đây | ☑ ba hàng, mỗi hàng ảnh + tên + giờ ("Phở bò · 8:53 AM") |
| Nút flash vắng mặt | ☑ đúng — emulator không có đèn, `capabilities.hasFlash` false |
| Báo lỗi nhận diện | ☑ Gemini trả 200 và nói ảnh không phải thứ nhận diện được (khung cảnh giả của emulator là bàn cờ thử camera). Snackbar đỏ hiện **trong khung**, cách đáy đủ để không đè vòng zoom, và không tràn sang panel — `MESSAGES_BOTTOM_INSET` 144 đúng |
| `LensScreen.kt` xuống dưới 1000 dòng | ☑ **388** |

Và trên **Pixel 7 API 36 (1440 × 3120 @560dpi = 411 × 891dp)** — nhánh mobile, cài mới:

| Tiêu chí | Kết quả |
|---|---|
| Luồng xin quyền camera | ☑ `CameraPermissionPrompt` lấp đầy khung, shutter mờ, **nút thư viện vẫn bật** — đúng chủ ý: máy ảnh bị từ chối không được kéo theo bộ chọn ảnh |
| Bố cục sau khi cấp quyền | ☑ hàng tool trên · khung · chip mode · hàng shutter thư viện / trống đồng. Giống hệt trước khi rút component |
| Chụp | ☑ shutter mờ, overlay **phủ toàn màn** (đúng hành vi mobile, khác tablet phủ trong khung), rồi sáng lại |
| Đường ống nhận diện | ☑ ảnh lên `generativelanguage.googleapis.com`, `gemini-3.5-flash` bị `RateLimited` → tự rơi xuống `gemini-3.1-flash-lite`, trả 200 |
| Báo lỗi | ☑ snackbar đỏ với câu gợi ý của model, nằm trên hàng chip đúng `MESSAGES_BOTTOM_INSET` 160 của mobile |

**Một chỗ phải sửa sau khi nhìn máy thật:** `TranslateLanguageBar` gọi `fillMaxWidth()` và
chia đôi bề rộng nhận được — ở khung 900dp, hai chip bị đẩy ra hai đầu ảnh với nút đảo chiều
lạc lõng ở giữa. Sửa bằng cách **chặn bề rộng tại nơi gọi** (`TRANSLATE_BAR_MAX_WIDTH` 380dp,
bằng bề rộng nó có trên điện thoại), không đụng vào component. Đây đúng ranh giới của mandate:
component tự quyết định nó tự dàn thế nào, cách sắp xếp chỉ quyết định nó được bao nhiêu chỗ.

## Lệch khỏi plan, và lý do

| Lệch | Vì sao |
|---|---|
| 14 component thay vì 6 | Sáu cái plan liệt kê đủ cho *panel*, không đủ cho *màn*. Tablet còn cần viewfinder, hàng tool, đếm ngược, mẹo ngắm, hai lời nhắc quyền và hai snackbar. Để lại bất cứ cái nào là tablet phải chép — đúng thứ bước rút này sinh ra để chặn |
| Thêm `LensHost.kt`, ngoài plan | Xem bảng "Ba quyết định". Kèm theo là một ngoại lệ mới trong `LLM.md` §5, đã viết rõ ràng buộc để nó không lan |
| `feature/camera/component/` là **thư mục**, không phải `LensComponents.kt` | 14 component là ~1 500 dòng. Một file là vi phạm quy tắc 200 dòng ngay từ commit đầu. `LLM.md` §5 và §10 đã sửa theo |
| Trần `UNSTABLE_CLASS_CEILING` 20 → 21 | `ZoomDriver` từ `private` (báo cáo Compose không thấy) thành `internal` (bị đếm). Bản thân lớp không đổi — nó giữ `glide: Job?` nên *unstable* là câu trả lời đúng, và không có dòng nào trong `compose-stability.conf` nói khác đi cho trung thực được. Nó không bao giờ là tham số composable |

## Rủi ro

| Rủi ro | Kết quả |
|---|---|
| Rút component làm hỏng mobile | ☑ Không. Đã chạy trọn luồng trên Pixel 7: xin quyền → chụp → overlay → Gemini → snackbar. Ảnh vào vẫn là camera giả của emulator |
| Tỉ lệ khung camera ở khổ ngang | ☑ Trên Android khung không méo. **iOS chưa kiểm** — `IosCameraDevice` là cài đặt khác |
| Rút xong lại lộ ra hàng #11 mới | Không phát sinh. Năm file screen còn lại vẫn nguyên trong hàng #11 |

## Bảo mật

Không có thay đổi. Quyền camera vẫn đi qua `PermissionSheet` và `Permissions.kt` như cũ.

## Tiếp theo

[Phase 05](phase-05-discovery-chat-two-pane.md). Bước rút component ở đó lặp lại đúng khuôn
này: `feature/discovery/component/`, `internal`, mỗi file một composable.
