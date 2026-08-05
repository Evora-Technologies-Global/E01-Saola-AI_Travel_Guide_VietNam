# Phase 02 — Điểm rẽ nhánh và chuyện xoay màn

**Liên kết:** [plan.md](plan.md) · [architecture-options.md](architecture-options.md) QĐ-3, QĐ-4 ·
`LLM.md` §2, §7 · `app/src/main/java/…/MainActivity.kt:46-51` ·
`shared/src/iosMain/…/MainViewController.kt`

## Tổng quan

**Ưu tiên:** P0
**Trạng thái:** ☑ Xong 04.08.2026 — đã kiểm trên Android; iPad còn chờ Xcode. Một tiêu chí
(giữ back stack khi đổi shell) chuyển sang phase 03, lý do ghi ở cuối file.
**Quyết định áp dụng:** QĐ-3 (mở hết cả hai — nhưng **iPad ở đây, iPhone ở
[phase 10](phase-10-iphone-landscape.md)**), QĐ-4 (≥840dp × ≥600dp, chung `NavHostController`)

Dựng cơ chế chọn nhánh, mở khoá xoay màn trên iOS, và đưa một shell tablet rỗng lên được màn
hình. Sau giai đoạn này, xoay iPad sang ngang phải thấy một trang trắng có chữ "tablet" —
đúng là chưa dùng được, nhưng chứng minh toàn bộ đường ống đã thông.

## Nhận định then chốt

- **Android không hề khoá orientation.** Không có `screenOrientation` trong manifest, không có
  `requestedOrientation` trong `MainActivity`. Câu chú thích ở `Info.plist:56` — *"Portrait
  only, like the Android build"* — sai từ lúc nào không rõ. Sửa luôn câu đó khi mở khoá iPad.
- **`MainActivity` ẩn thanh hệ thống cho toàn app** (`hideSystemBars()`, dòng 65-70) với lý do
  *"màn hình vừa là viewfinder vừa là trang, và bốn tab của app đã nằm đúng chỗ thanh hệ thống
  sẽ nằm"*. Trên tablet, rail nằm bên trái chứ không nằm dưới, nên lý lẽ đó không còn đúng ở
  cạnh dưới. Cần quyết định lại — đề xuất giữ nguyên, vì Lens vẫn full-bleed.
- **Không dùng `material3-window-size-class`.** Thư viện đó gắn với Android. `BoxWithConstraints`
  ở gốc composition là đa nền, không thêm phụ thuộc, và đo đúng cửa sổ thật — kể cả khi app
  đang ở split-screen.
- **Chiều cao cũng là điều kiện.** Điện thoại nằm ngang rộng ~891dp nhưng chỉ cao ~411dp. Nếu
  chỉ xét bề rộng, nó rơi vào bố cục master–detail và vỡ ngay.
- **Phát hiện lúc triển khai: `Info.plist` không phải chỗ duy nhất khoá iPad.** Dự án Xcode
  đặt `TARGETED_DEVICE_FAMILY = 1` — chỉ iPhone. Với giá trị đó app chạy trên iPad ở chế độ
  tương thích iPhone: khung cửa sổ là khung điện thoại, và `UISupportedInterfaceOrientations~ipad`
  bị bỏ qua hoàn toàn. Nghĩa là mở khoá xoay thôi thì nhánh tablet vẫn không bao giờ hiện ra.
  Đã đổi thành `"1,2"` ở cả Debug lẫn Release. `SUPPORTS_MACCATALYST` và
  `SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD` giữ nguyên `NO` — Mac nằm ngoài phạm vi.

## Yêu cầu

**Chức năng:** cửa sổ rộng ≥ 840dp **và** cao ≥ 600dp thì hiện shell tablet; ngoài ra hiện
shell mobile. Đổi kích thước khi đang chạy thì đổi shell mà giữ nguyên chỗ đang đứng.
**Phi chức năng:** không thêm phụ thuộc mới; không có nhánh `if (Platform.isAndroid)` trong
`commonMain` (`LLM.md` §3).

## Kiến trúc

```kotlin
// core/window/WindowClass.kt — MỚI
enum class WindowClass { COMPACT, EXPANDED }

/**
 * Đo cửa sổ thật, không đoán theo loại thiết bị.
 *
 * Cả hai chiều đều được xét: một chiếc điện thoại nằm ngang rộng bằng máy tính bảng
 * nhưng chỉ cao 411dp, và bố cục master–detail 392dp + detail không sống nổi ở đó.
 */
@Composable
fun rememberWindowClass(maxWidth: Dp, maxHeight: Dp): WindowClass =
    if (maxWidth >= 840.dp && maxHeight >= 600.dp) EXPANDED else COMPACT
```

```kotlin
// navigation/VietLensRoot.kt — MỚI, là thứ hai entry point gọi tới
@Composable
fun VietLensRoot(navController: NavHostController = rememberNavController()) {
    BoxWithConstraints {
        when (rememberWindowClass(maxWidth, maxHeight)) {
            COMPACT  -> VietLensApp(navController)        // mobile/navigation/
            EXPANDED -> VietLensTabletApp(navController)  // tablet/navigation/
        }
    }
}
```

`navController` được nâng lên **trên** chỗ rẽ và truyền vào cả hai shell. Đây là điểm mấu chốt
của QĐ-4: hai shell đăng ký cùng bộ `Routes`, nên xoay máy giữa lúc đang đọc một discovery thì
sau khi đổi shell vẫn đang ở discovery đó, và nút back vẫn còn nguyên lịch sử.

`MainActivity` giữ đúng hình dạng yêu cầu — một chỗ rẽ, đọc được bằng mắt:

```kotlin
setContent {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    VietLensTheme(themePreference = settings.darkTheme) {
        VietLensRoot()
    }
}
```

`MainViewController.kt` đổi y hệt. Hai nền cùng gọi một hàm, nên không nền nào có thể lệch
khỏi nền kia — cùng lý lẽ đã viết trong `LLM.md` §6 về `appModules(isDebug)`.

## File liên quan

**Tạo:** `core/window/WindowClass.kt` · `navigation/VietLensRoot.kt` ·
`tablet/navigation/VietLensTabletApp.kt` (bản rỗng)
**Sửa:** `MainActivity.kt:46-51` · `MainViewController.kt` · `iosApp/iosApp/Info.plist:54-58` ·
`mobile/navigation/VietLensApp.kt` (bỏ default của `navController`) ·
`iosApp/iosApp.xcodeproj/project.pbxproj` (`TARGETED_DEVICE_FAMILY`) ·
`androidHostTest/…/ComposeStabilityReportTest.kt` (allowlist)
**Xoá:** `tablet/.gitkeep` — thư mục đã có file thật

## Các bước

1. `core/window/WindowClass.kt`.
2. `navigation/VietLensRoot.kt` với `BoxWithConstraints` và `navController` nâng lên trên.
3. `tablet/navigation/VietLensTabletApp.kt` — tạm thời chỉ là một `Box` có chữ, đủ để nhìn thấy.
4. `VietLensApp` nhận `navController` từ tham số thay vì tự `rememberNavController()`.
5. `MainActivity` và `MainViewController` gọi `VietLensRoot()`.
6. `Info.plist`: thêm `UISupportedInterfaceOrientations~ipad` với đủ bốn hướng; **tạm** giữ
   khoá portrait cho iPhone — mở nó ra là việc của phase 10, và mở sớm ở đây chỉ tạo ra một
   khoảng thời gian dài mà iPhone nằm ngang thì vỡ. Sửa lại câu chú thích sai về Android, ghi
   rõ khoá iPhone là tạm và trỏ sang phase 10.
7. Xác nhận trên máy thật: xoay iPad, gấp/mở fold, kéo split-screen Android.

## Todo

- [x] `WindowClass.kt`
- [x] `VietLensRoot.kt`
- [x] `VietLensTabletApp.kt` bản rỗng
- [x] `VietLensApp` nhận `navController` qua tham số — **bỏ hẳn default**, để không ai lỡ
      tạo controller thứ hai bằng cách quên truyền
- [x] Hai entry point gọi `VietLensRoot()`
- [x] `Info.plist` — `~ipad`, iPhone tạm khoá, sửa chú thích sai về Android
- [x] `TARGETED_DEVICE_FAMILY = "1,2"` — không có nó thì `~ipad` vô nghĩa (xem Nhận định)
- [x] `ComposeStabilityReportTest` — allowlist `VietLensRoot` và `VietLensTabletApp`
- [x] Biên dịch ba đích: `:shared:compileAndroidMain`, `:app:compileDebugKotlin`,
      `:shared:compileKotlinIosSimulatorArm64`
- [x] `:shared:testAndroidHostTest` — 36 test xanh. `DesignTokenTest` báo
      `feature: 27, mobile/feature: 10, tablet/feature: 0`, đúng như trước: giai đoạn này
      không thêm file nào vào `tablet/feature`, nên `POPULATED_BRANCHES` chưa phải sửa —
      đó là việc của phase 04
- [x] Thử xoay trên Pixel Tablet API 36 + điện thoại thật — bảng kết quả ở mục
      "Tiêu chí hoàn thành"
- [ ] Thử trên iPad thật / simulator — cần máy Mac có Xcode chạy `iosApp`; chưa làm
- [ ] Fold và split-screen — chưa có thiết bị

## Tiêu chí hoàn thành

- ☑ Xoay sang ngang → đổi shell. Xoay về dọc → về lại mobile.
- ☐ **Xoay khi đang mở một discovery thì vẫn ở discovery đó**, không văng về màn đầu.
  **Không thể đạt ở giai đoạn này** — xem mục dưới. Chuyển thành tiêu chí nghiệm thu của
  [phase 03](phase-03-tablet-shell-rail-and-panes.md).
- ☑ Cửa sổ hẹp trên tablet Android → về shell mobile.
- ☑ iPhone không đổi hành vi gì so với trước (`Info.plist` vẫn khoá portrait cho iPhone).

### Đã kiểm trên máy — Pixel Tablet API 36 (2560×1600 @320dpi)

| Việc | Kết quả |
|---|---|
| Ngang, 1280 × 800dp | shell **tablet** — chữ "tablet" giữa màn |
| Dọc, 800 × 1280dp | shell **mobile** — dưới 840dp nên đúng ngưỡng QĐ-4 |
| Điện thoại thật (SM-A165F, 393 × 851dp) | shell mobile, không đổi gì |
| Biên dịch | `:shared:compileAndroidMain`, `:app:compileDebugKotlin`, `:shared:compileKotlinIosSimulatorArm64` — cả ba xanh |
| `:shared:testAndroidHostTest` | 36/36 xanh |

**Bẫy mất giờ, ghi lại để lần sau khỏi vấp:** applicationId của bản debug là
`com.duylt.trave.vietlensai.dev`, không phải `com.duylt.trave.vietlensai`. Gọi `monkey` hay
`dumpsys package` với tên không có `.dev` là đang mở **bản release cũ** — nó chạy, nó không
crash, và nó hiện code của hôm trước. Mất gần một giờ mới nhận ra.

### Vì sao tiêu chí giữ back stack chưa đạt được

Đo thật, không suy đoán: mở Nhật ký → Hộ chiếu, rồi xoay sang ngang và xoay về dọc → app về
thẳng màn Ống kính. Nhưng cùng thao tác đó với một **config change không đổi shell** (đổi
`wm size` 1080×2340 → 1080×2000, cả hai đều COMPACT) thì màn Hộ chiếu **còn nguyên**. Hai
phép đo cạnh nhau chỉ đúng một thủ phạm:

`VietLensTabletApp` bản rỗng **không có `NavHost`**, nên không ai gọi `setGraph`. Xoay máy
huỷ và dựng lại Activity; controller mới khôi phục được bundle back stack nhưng không có
graph để áp nó vào, và tới config change kế tiếp thì `saveState()` ghi đè bằng một back stack
rỗng. Lịch sử bị mất ở **lần đi qua shell không có graph**, không phải ở việc xoay.

Không sửa ở đây, và đó là quyết định chứ không phải bỏ sót: cách sửa duy nhất là cho shell
tablet đăng ký đúng bộ `Routes` — tức là dựng `NavHost` thật, đúng nội dung của phase 03. Làm
sớm ở đây thì phải lôi `VietLensNavHost` (đang `private`) ra khỏi nhánh mobile theo một hình
dạng mà phase 03 và phase 05 sẽ sắp lại lần nữa.

**Việc của phase 03:** dựng xong rail và `NavHost` thì chạy lại đúng phép đo trên. Nó cũng là
bằng chứng trực tiếp cho QĐ-4 — dùng chung `NavHostController` để không mất chỗ đang đứng —
nên nếu nó vẫn đỏ sau phase 03 thì QĐ-4 sai chứ không phải cài đặt sai.

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Đổi shell làm ViewModel bị tạo lại | ViewModel gắn với back-stack entry, không gắn với composable — nhưng phải xác nhận bằng tay: mở Lens, xoay, kiểm tra ảnh vừa chụp còn nguyên |
| `BoxWithConstraints` ở gốc gây thêm một lớp đo | Một lớp, ở gốc, không nằm trong vòng cuộn nào. Không đáng lo, nhưng nếu Explore giật thì đây là chỗ nhìn đầu tiên |
| Ẩn thanh hệ thống cư xử khác trên tablet | Kiểm tra trên máy thật; nếu rail bị notch che thì `screenInsetsPadding()` trong `Insets.kt` là chỗ sửa |

## Bảo mật

Không có.

## Tiếp theo

[Phase 03](phase-03-tablet-shell-rail-and-panes.md) — dựng rail thật và khung hai pane.
