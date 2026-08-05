# Phase 01 — Tách `:shared` thành hai nhánh

**Liên kết:** [plan.md](plan.md) · [architecture-options.md](architecture-options.md) QĐ-1 ·
`LLM.md` §3, §5, §10 · `docs/android-mvi-best-practices.md` §2

## Tổng quan

**Ưu tiên:** P0 — chặn mọi giai đoạn sau
**Trạng thái:** ☑ Xong — 04.08.2026
**Chờ:** QĐ-1

Di chuyển tầng trình bày hiện có xuống `mobile/`, mở chỗ trống cho `tablet/`, và giữ nguyên
`feature/<name>/` làm nơi ở của Contract + ViewModel dùng chung. **Không đổi một dòng logic
nào.** Kết thúc giai đoạn này app phải chạy y hệt lúc bắt đầu.

## Nhận định then chốt

- Đây là commit thuần di chuyển. Trộn một sửa đổi hành vi vào đây là làm mất khả năng đọc
  `git log --follow` của sáu file screen lớn nhất dự án.
- **Hai gate sẽ gãy, và một trong hai gãy im lặng.** `DesignTokenTest.featureFiles()` giải
  đường dẫn `com/duylt/trave/vietlensai/feature` rồi `walkTopDown()`. Khi screen chuyển sang
  `mobile/feature/`, bốn quy tắc — corner, gap, weight, type size — vẫn *pass*, vì chúng chạy
  trên danh sách rỗng. Chỉ quy tắc `HEADER_OWNERS` là gãy to (`"Screen file(s) named in
  HEADER_OWNERS no longer exist"`). Phải sửa cả hai trong cùng commit.
- `Destinations.kt` đang chứa hai thứ khác bản chất: `Routes` (dùng chung) và
  `TopLevelDestination` (bốn tab — thuộc riêng nhánh mobile). Tách đôi ngay bây giờ, vì tablet
  dùng rail chứ không dùng bottom bar.

## Yêu cầu

**Chức năng:** app Android và iOS chạy y hệt trước khi tách — cùng bốn tab, cùng điều hướng,
cùng giao diện.
**Phi chức năng:** `:shared:compileKotlinAndroid`, `:shared:compileKotlinIosSimulatorArm64`,
`:shared:testDebugUnitTest` và cả hai gate ở `androidHostTest` đều xanh.

## Kiến trúc

```
com.duylt.trave.vietlensai/
├── MainViewModel.kt                     giữ nguyên
├── core/                                giữ nguyên
├── di/SharedModules.kt                  giữ nguyên
├── platform/ · voice/                   giữ nguyên
├── navigation/Routes.kt                 MỚI ← nửa `Routes` của Destinations.kt
├── feature/<name>/
│   ├── XContract.kt                     giữ nguyên
│   └── XViewModel.kt                    giữ nguyên
├── mobile/
│   ├── navigation/VietLensApp.kt        ← navigation/VietLensApp.kt
│   ├── navigation/BottomDestinations.kt ← nửa `TopLevelDestination` của Destinations.kt
│   └── feature/<name>/                  ← mọi XScreen.kt và file phụ trợ chỉ vẽ
└── tablet/                              trống, chỉ có .gitkeep ở giai đoạn này
```

**Xếp file vào `mobile/` hay để lại `feature/`** — quy tắc: file nào `import androidx.compose`
thì xuống `mobile/`, trừ khi nó là composable đã dùng chung (thuộc `core/designsystem`).

| File | Đích | Vì sao |
|---|---|---|
| `LensScreen.kt`, `DiscoveryScreen.kt`, … (10 screen) | `mobile/feature/<name>/` | thuần vẽ |
| `ExploreComponents.kt`, `PlaceDetailSheet.kt`, `VietnamMapCanvas.kt`, `SovereigntyMap.kt` | ở lại `feature/<name>/` | tablet sẽ dùng lại — phase 06/07/08 rút tiếp vào `component/` |
| `CameraController.kt`, `CameraOptions.kt` | ở lại `feature/camera/` | không phải composable |
| `PlaceMap.kt` + hai `actual` | ở lại `feature/explore/` | có `actual` ở androidMain/iosMain, đổi package là đổi cả hai |

## File liên quan

**Sửa:** `shared/src/androidHostTest/…/designsystem/DesignTokenTest.kt` ·
`app/src/main/java/…/MainActivity.kt` (import) · `shared/src/iosMain/…/MainViewController.kt`
(import) · `LLM.md` §3, §5, §10
**Tạo:** `navigation/Routes.kt` · `mobile/navigation/BottomDestinations.kt`
**Xoá:** `navigation/Destinations.kt`, `navigation/VietLensApp.kt` (sau khi tách/di chuyển)

## Các bước

1. Tạo `navigation/Routes.kt`, chuyển `object Routes` và `urlEncoded()` sang, giữ nguyên KDoc.
2. Tạo `mobile/navigation/BottomDestinations.kt`, chuyển `TopLevelDestination`,
   `isTopLevel()`, `topLevelDestination()` sang. Xoá `Destinations.kt`.
3. `git mv` mười `XScreen.kt` sang `mobile/feature/<name>/`, sửa dòng `package`.
4. `git mv` `VietLensApp.kt` sang `mobile/navigation/`, sửa `package` và import.
5. Sửa import ở `MainActivity.kt` và `MainViewController.kt`.
6. Sửa `DesignTokenTest`:
   - `featureFiles()` duyệt `mobile/feature` **và** `tablet/feature` **và** `feature`, hợp
     nhất kết quả. Ghi rõ trong KDoc rằng cả ba nhánh đều bị soi.
   - Thêm một assert mới: nếu `featureFiles()` trả về **rỗng** thì `fail` — chính là hàng rào
     ngăn kiểu hỏng im lặng vừa mô tả.
   - `HEADER_OWNERS` giữ nguyên tên file, vì so khớp theo `it.name` chứ không theo đường dẫn.
7. `mkdir tablet/` + `.gitkeep`.
8. Biên dịch cả hai nền, chạy `:shared:testDebugUnitTest` và hai gate.
9. Cập nhật `LLM.md` §3 (cây thư mục), §5 (giải phẫu feature package đổi rồi), §10 (bảng
   "file mới đi đâu"), và thêm một hàng vào §11 nếu có gì phải hoãn.

## Todo

- [x] `navigation/Routes.kt`
- [x] `mobile/navigation/BottomDestinations.kt`, xoá `Destinations.kt`
- [x] `git mv` 10 screen + `VietLensApp.kt`
- [x] Sửa import ở hai entry point
- [x] `DesignTokenTest`: gộp ba gốc + assert chống tập rỗng
- [x] `tablet/.gitkeep`
- [x] Biên dịch Android + iOS, chạy toàn bộ test
- [x] `LLM.md` §3 §5 §7 §9 §10 §11 + MVI doc §4

## Ghi chú khi làm

- **Tên task Gradle trong plan sai.** Không có `:shared:compileKotlinAndroid` hay
  `:shared:testDebugUnitTest`. Tên thật: `:shared:compileAndroidMain` và
  `:shared:testAndroidHostTest`. Các phase sau dùng tên này.
- **Mỗi screen phải thêm import cho chính package cũ của nó.** Trước khi tách,
  `LensScreen.kt` gọi `LensState`, `LensViewModel`, `CameraController` mà không cần import
  vì cùng package. Sau khi tách là 44 dòng import mới trải trên mười file — đó là toàn bộ
  phần "sửa" của commit này ngoài dòng `package`.
- **`androidDeviceTest` cũng phải sửa.** `TranslationOverlayGestureTest` import thẳng
  `TranslationScreen`; plan không nhắc tới file này. Nó vẫn không chạy được (§11 hàng #18)
  nhưng vẫn phải biên dịch.
- **Hàng rào chống tập rỗng đã được thử cho gãy thật**, không chỉ viết ra: đổi
  `mobile/feature` thành một tên không tồn tại → `every presentation branch is actually
  scanned` fail đúng như thiết kế, và bốn quy tắc token vẫn *pass* trên tập rỗng — tức là
  đúng cái kiểu hỏng im lặng mà bước 6 sinh ra để chặn.
- **Bốn hàng ở `LLM.md` §11 trỏ vào file/dòng đã đổi** (#11, #13, #16, #17, #19) — đã cập
  nhật vị trí. Số dòng cũ trỏ vào code vô can là drift, không phải chuyện nhỏ.

## Tiêu chí hoàn thành

- `git diff --stat` chỉ có đổi package/import, không có đổi thân hàm.
- Cài app lên máy thật, đi hết bốn tab và mở một discovery — không khác gì trước.
- `DesignTokenTest` chạy trên đúng 10 file screen như trước khi tách (in ra số lượng để xác
  nhận, đừng tin màu xanh).

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Gate token pass trên tập rỗng | Bước 6 thêm assert chống rỗng — bắt buộc, không phải tuỳ chọn |
| Đổi package làm gãy `expect/actual` | `PlaceMap.kt`, `Permissions.kt`, `SystemBars.kt` ở lại đúng chỗ cũ; không đụng tới |
| Xung đột merge với việc đang làm dở | Đây là commit lớn nhất về số file — làm sớm, merge sớm |

## Bảo mật

Không có. Giai đoạn này không chạm tới khoá, quyền hay lưu trữ.

## Tiếp theo

[Phase 02](phase-02-entry-point-and-orientation.md) — dựng chỗ rẽ nhánh.
