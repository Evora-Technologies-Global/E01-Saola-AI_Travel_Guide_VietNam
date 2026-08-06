# Kiểm định kiến trúc — MVI + SOLID + Clean Architecture

**Ngày:** 05.08.2026 · **Nhánh:** `duylt_dev` @ `92a4970` · **Phạm vi:** toàn repo (352 file `.kt`: `:domain` 45, `:data` 81, `:shared` 222, `:app` 4)

**Chuẩn đối chiếu:** `LLM.md` (913 dòng), `docs/android-mvi-best-practices.md` (1060 dòng), `docs/large-screen-layout.md`.

---

## 1. Kết luận

**Bộ khung đúng chuẩn. Cái sai nằm ở chỗ một quy tắc đã được viết ra nhưng chỉ áp một nửa.**

Mười ViewModel đều kế thừa `MviViewModel`, `onIntent` là public method duy nhất trên cả mười một, mười Contract nằm đúng file riêng, `effects.collect` xuất hiện đúng **một** chỗ trong toàn bộ production (`core/mvi/CollectEffects.kt:45`), `:domain` sạch tuyệt đối (quét từng dòng `import` của 45 file), hai shell khai báo **cùng mười route với cùng ba `navArgument` default**, và toàn bộ 11 ViewModel bind bằng `viewModel { }` không có cái nào `single { }`. Đây không phải codebase cần tái cấu trúc.

Vấn đề thật là **§11 row #26 chỉ được vá trên một màn hình**. Row đó ghi: "`DiscoveryViewModel` vứt `AppResult` của cả bốn lệnh ghi, nên một failure đã-được-xử-lý rơi thẳng vào nhánh success" — và fix ngày 05.08.2026 chỉ chạm `DiscoveryViewModel`. Còn **bốn site nữa cùng y hệt hình dạng đó** đang sống trong repo, một trong số đó nói dối người dùng về một hành động phá hủy dữ liệu.

Cùng với một lỗi rò bộ nhớ riêng của iOS, đó là hai thứ đáng sửa ngay.

---

## 2. Phương pháp và độ tin cậy

8 trục kiểm tra độc lập → mỗi trục qua một agent phản biện được lệnh **bác bỏ** phát hiện (mặc định REFUTED khi không chắc, vì một phát hiện sai làm người ta đi sửa code vốn đúng).

| | Số lượng |
|---|---|
| Phát hiện nêu ra | 62 |
| CONFIRMED | 23 |
| RESCOPED (thật nhưng bị thổi phồng) | 32 |
| **REFUTED** (bác bỏ) | **7** |
| Verifier tự tìm thêm | 15 |
| Khẳng định kiểm tra **sạch** | 121 |

Sáu trên tám trục có **hai** lượt phản biện độc lập (lần chạy đầu đứt kết nối ở `solid`/`testing`, chạy lại toàn bộ). Chỗ nào hai lượt đồng ý → đã ghi chú. Chỗ nào bất đồng → mục 8.

Ba phát hiện nặng nhất (§3.1, §3.2, §3.3) tôi **tự đọc file xác nhận**, không dựa vào agent.

---

## 3. Nhóm A — lỗi code, sửa được ngay

### 3.1 `MainViewModel` bị dựng lại mỗi lần recompose trên iOS — **cao**

`shared/src/iosMain/.../MainViewController.kt:72`

```kotlin
private val composeController = ComposeUIViewController {
    val viewModel: MainViewModel = KoinPlatform.getKoin().get()      // ← trong content lambda
    val settings: AppSettings by viewModel.settings.collectAsState() // ← đọc state của chính nó
```

Koin `viewModel { }` **chính là** `factory { }` (koin-core-viewmodel `ModuleExt.kt:33-38`). Nên `get()` dựng một `MainViewModel` **mới mỗi lần lambda chạy lại** — mà dòng 75 đọc state của chính instance đó, nên mỗi lần settings phát là thêm một vòng. Mỗi instance mang `settings = observeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, …)` đang thu thập, và vì nó nằm ngoài mọi `ViewModelStore` nên `onCleared()` **không bao giờ** chạy.

Ngay bên trên, dòng 67-69 làm đúng cho TTS:

```kotlin
private val textToSpeech: TextToSpeechManager by lazy { KoinPlatform.getKoin().get() }
```

Android không dính vì `MainActivity` đi qua `ViewModelStoreOwner`. Đây là điểm duy nhất trong toàn bộ audit mà hai nền tảng thực sự lệch nhau về hành vi.

**Sửa:** `private val mainViewModel: MainViewModel by lazy { KoinPlatform.getKoin().get() }` trên `SaolaRootViewController`, đọc trong content lambda. Hoặc dùng `koinViewModel<MainViewModel>()` để nó vào đúng `ViewModelStore` của composition.

---

### 3.2 Xoá lịch sử thất bại vẫn báo "đã xoá xong" — **cao**

`shared/src/commonMain/.../feature/settings/SettingsViewModel.kt:72-76`

```kotlin
SettingsIntent.ConfirmClearHistory -> launchSafely {
    setState { copy(showClearConfirm = false) }
    clearHistory()                              // AppResult<Unit> bị vứt
    sendEffect(SettingsEffect.HistoryCleared)   // gửi vô điều kiện
}
```

`DiscoveryRepositoryImpl.deleteAll` bọc trong `runCatchingStorage` (`StorageGuards.kt:37`), tức là lỗi ổ đĩa trả về `AppResult.Failure` **chứ không ném** — nên `launchSafely.onError` không bao giờ chạy. `SettingsHost.kt:58-60` biến `HistoryCleared` thành `showMessage(Res.string.settings_cleared)` = "đã xoá toàn bộ khám phá". Người dùng đọc câu đó trong khi journal vẫn liệt kê nguyên vẹn mọi thứ.

Trớ trêu: arm `SaveApiKey` ngay trên (dòng 44-54) làm **đúng**, kèm comment tự giải thích tại sao:

> *"Unconditionally they were a lie with teeth: a failed write discarded the key the traveller had just pasted, told them it was saved…"*

Cùng file, cùng bài học, chỉ áp cho một nửa. `SettingsEffect.ShowMessage` đã tồn tại (`SettingsContract.kt:47`) và đã được collect (`SettingsHost.kt:64`) — thiếu đúng một phép kiểm tra.

**Sửa:**
```kotlin
when (val result = clearHistory()) {
    is AppResult.Failure -> sendEffect(SettingsEffect.ShowMessage(result.error))
    is AppResult.Success -> sendEffect(SettingsEffect.HistoryCleared)
}
```

---

### 3.3 Ba site còn lại cùng lớp lỗi — **trung bình / thấp**

| Site | Kết quả bị vứt | Hậu quả |
|---|---|---|
| `feature/journal/JournalViewModel.kt:74` | `toggleFavorite(id)` | Chạm tim, tim không sáng, không một chữ giải thích. `onError` có nối `JournalEffect.ShowMessage` nhưng chỉ bắt đường ném |
| `feature/chat/ChatViewModel.kt:81-84` | `clearChat(discoveryId)` | Hai đường lỗi trả lời khác nhau: ném → ghi `error`, banner hiện; `AppResult.Failure` → im lặng, tin nhắn còn nguyên, nút như chết |
| `feature/discovery/DiscoveryViewModel.kt:132` | `captureStore.flattenOrientation(path)` | Xoay ảnh thất bại → dải ảnh note vẽ ảnh nằm ngang, không báo gì. Comment ngay trên đó đã nói chính xác hậu quả này |

Cả ba đều có sẵn kênh báo lỗi (`report(AppError)` ở `DiscoveryViewModel.kt:199`, `JournalEffect.ShowMessage`, `ChatState.error`) — chỉ thiếu `.onFailure { }`.

**Đây là một PR duy nhất**, cùng với §3.2: bốn call site, một hình dạng, cùng lý do.

---

### 3.4 `SettingsIntent.ClearApiKey` im lặng — **thấp**

`SettingsViewModel.kt:56-59` vứt `AppResult` của `setApiKey(null)`.

Cả hai lượt phản biện đều **hạ mức** phát hiện gốc: hại thật nhỏ hơn báo cáo ban đầu, vì `ApiKeyCard.kt:85/91/151` đều vẽ từ luồng `settings` — pill vẫn xanh, nút Clear vẫn còn. Màn hình **không nói dối**, nó không nói gì. Chi phí là một cái nút bấm như không có tác dụng.

---

### 3.5 Câu hỏi gợi ý mất `prefill` trên điện thoại — **trung bình, có tranh cãi**

`mobile/feature/discovery/DiscoveryScreen.kt:130` chỉ đọc `effect.discoveryId`, bỏ `effect.prefill`. Bản tablet (`DiscoveryTabletScreen.kt:165-167`) dùng cả hai.

Một verifier **REFUTED** (route `Routes.CHAT` không có tham số nào chở câu hỏi qua được, nên phone không có lựa chọn khác; và `DiscoveryViewModelTest.kt:105` vẫn pin payload). Một verifier coi là medium. Thực chất: **effect chở dữ liệu mà một nửa hệ thống không dùng được** — hoặc thêm query param `question` vào `Routes.CHAT` (khai báo **giống hệt ở cả hai graph**, nếu không `setGraph` xoá back stack), hoặc ghi vào Contract rằng `prefill` chỉ dành cho arrangement nào vẽ guide cạnh story.

---

## 4. Nhóm B — nợ kiến trúc

### 4.1 `PassportViewModel` chứa nguyên một pipeline ảnh Compose — **trung bình**

`docs/android-mvi-best-practices.md:404-406` viết rõ:

> *"Never import anything from `androidx.compose.*` … The one exception in this codebase is `ImageBitmap` on `PassportState`."*

Thực tế `PassportViewModel.kt:5-13` import **bảy** ký hiệu Compose và dòng 209-228 mở `Canvas`, dựng `Paint`, đặt `FilterQuality.Medium`, gọi `drawImageRect`.

Hậu quả cụ thể, không phải chuyện thẩm mỹ: `scaledToCover` là **hàng rào chống OOM** mà KDoc của nó nói là bắt buộc — và nó **không chạy được trong bất kỳ test nào trên bất kỳ target nào**. Trên JVM (`androidHostTest`) `ImageBitmap(w,h)` là `android.graphics.Bitmap.createBitmap` = stub, ném `RuntimeException("Stub!")`, bị `runCatching` ở dòng 185 nuốt. Trên iOS `FakeCaptureStore.read` trả `ByteArray(8)` nên decode hỏng. Nghĩa là **mọi cover đều null trong mọi test**.

**Sửa:** đưa decode+resample ra sau một port (`CaptureStore.readThumbnail` hoặc `CoverDecoder` được inject), đúng khuôn §11 row #27 đã kê cho sovereignty. Hoặc — nếu quyết định giữ — **sửa rule 6 để nó mô tả đúng code**, vì hiện tại nó mô tả một codebase không tồn tại.

### 4.2 `SovereigntyViewModel` — §11 row #27, vẫn mở

`SovereigntyViewModel.kt:54` gọi thẳng `Res.readBytes`. ViewModel duy nhất không có collaborator nào để fake → đường success không có test trên bất kỳ nền tảng nào.

**Kèm theo:** row #27 trỏ sai dòng — ghi `:31`, nhưng dòng 31 là `launchSafely(onError = …)`, tức một pattern được §12 khen. Ai mở đúng dòng đó sẽ kết luận row này đã fix rồi. Đây là **row Open duy nhất trong §11 mà con trỏ đã lệch** (đã kiểm lại #11, #17, #19, #22 — còn đúng).

### 4.3 Trùng lặp giữa `mobile/` và `tablet/`

Quy tắc §5: *"a copy diverges on the first fix that only one side gets."* Đã tìm thấy sáu chỗ:

| Chỗ | Hai bản | Rủi ro cụ thể |
|---|---|---|
| `CameraFrame` | `LensScreen.kt:449` ↔ `LensTabletScreen.kt:271` | §13.3: outline vẽ tay phải khớp `clip` **đến từng pixel**. Đổi bán kính/hairline/alpha là sửa hai file |
| Sheet tỉnh của passport (~100 dòng) | `PassportScreen.kt:122-277` ↔ `PassportPane.kt:83-207` | Fix của §11 row #29 đã phải suy luận cho **cả hai bản** rồi |
| Cầu nối quyền camera cho note | `DiscoveryScreen.kt:192,345-358` ↔ `DiscoveryTabletScreen.kt:329-342` | Chính là loại behaviour mà §5 dựng `ExploreHost` để chứa |
| Đầu danh sách journal | `JournalScreen.kt:175-240` ↔ `JournalTabletScreen.kt:303-367` | Gồm cả lambda `expandedStories` — là xử lý state, không phải bố cục |
| Thân trang collection | `CollectionScreen.kt:87-137` ↔ `CollectionPane.kt:52-97` | Giống nhau trọn vẹn trừ `COLUMNS` và back chip |
| Thang trạng thái map Explore | `ExploreScreen.kt:118-130,208-221` ↔ `ExploreTabletScreen.kt:131-143,219-232` | Quyết định *lỗi nào được báo trước, lỗi nào được nút thử lại* — không phải chuyện đặt ở đâu |

Cộng thêm hai cái verifier tự tìm: thang phân giải trang của discovery (`DiscoveryTabletScreen.kt:249`, hai enum private khác kiểu nhau) và hằng `OVERLAY_SCALE` nằm hai bên — đúng cái §12 kể là suýt làm guide thành hai sắc kem khác nhau.

**Điều quan trọng:** §3 vẫn giữ được. Không file nào dưới `mobile/`/`tablet/` khai báo ViewModel, không file nào import repository/use case/`:data`. Đây là **trùng lặp bố cục**, không phải rò rỉ logic nghiệp vụ.

### 4.4 SOLID — mức thấp, nhưng có thật

- **ISP:** `TranslationRepository` — 3/4 method không có consumer nào (`observeTranslations`, `observeTranslation`, `delete`). `DiscoveryRepository.observeFavorites` và `ProvinceRepository.provinces` — 0 consumer production. Mỗi cái vẫn buộc mọi implementer và mọi fake phải viết, và **cả hai fake của `observeFavorites` đều viết sai** (`Fakes.kt:146` trả về toàn bộ discovery).
- **`CaptureStore` 8 method:** `listCaptures` có KDoc nói *"no screen should be listing storage"* nhưng cùng interface đó được `NoteCameraOverlay.kt:69` `koinInject()` vào thẳng một composable để dùng đúng một method. Lưu ý: tách port này **đi ngược** đoạn §12 về "một port với hai nghĩa là cách lỗi này quay lại" — cân nhắc kỹ.
- **DIP:** `Dispatchers.Default` hardcode trong `PassportViewModel:184` và `SovereigntyViewModel:52` — hai chỗ duy nhất trong app, trong khi `:data` inject `ioDispatcher` ở cả 13 nơi. Hệ quả: `PassportViewModelTest` không lái được thời gian ảo qua đó, và §9 lại khẳng định Sovereignty là *"the only one"*.
- **Reducer không thuần:** `ExploreViewModel` gọi `nextCamera()` (`requestId = ++cameraRequests`) **bên trong** `setState` ở ba chỗ (133, 214, 268). `MutableStateFlow.update` được phép chạy lambda nhiều lần. Hôm nay vô hại vì `viewModelScope` là `Main.immediate`; là quả bom hẹn giờ.
- **DRY:** `PlaceMap.kt:106-114` `markerColor` trùng byte-for-byte `Formatters.kt:71-80` `accentColor`.
- **Code chết:** `platform/Platform.kt` — `expect val IS_APPLE_PLATFORM` + hai `actual`, **0 call site** toàn repo.

---

## 5. Nhóm C — lỗ hổng test

`Fakes.kt:169-171` tự viết ra quy tắc:

> *"`failOn…` is the ordinary handled failure a repository returns, `throwOn…` the unwrapped one it promises never to produce. **Four of this app's silent failures lived on the first of those**, which no `throwOn…` hook can reach."*

Rồi chính file đó không giữ nổi quy tắc của mình:

| Fake | Thiếu | Che mất gì |
|---|---|---|
| `FakeChatRepository.clearThread` (`:374`) | **cả hai** hook | Che đúng lỗi §3.3 — không test nào chạm được |
| `FakeDiscoveryRepository.deleteAll` (`:189`) | **cả hai** hook | Che đúng lỗi §3.2 |
| `FakeCaptureStore.read` (`:275`) | `failOnRead` | `PassportViewModel` nhánh `AppResult.Failure` — chính là nhánh mà `AndroidCaptureStore` thực sự sinh ra cho ảnh bị xoá |
| `FakeDiscoveryRepository.delete` (`:182`) | `throwOnDelete` | Nhánh ném của `ConfirmDelete` |
| `FakeNoteRepository.throwOnDelete` (`:517`) | có hook, **không test nào set** | — |

**Ba lỗi ở §3 nằm chính xác trong vùng mù mà các fake tạo ra.** Đó không phải trùng hợp — đó là quan hệ nhân quả.

Thêm:
- `Fakes.kt:74` viết *"Every fake carries a `throwOnNext…` switch"* — **không có field nào tên như vậy**, và bốn fake không có switch nào cả.
- `ChatViewModelTest.kt:118` dùng dạng yếu mà §9 bác bỏ (không gửi câu hỏi thứ hai, không đếm).
- `TranslationOverlayGestureTest.kt:108` — assert **không thể fail**: nếu overlay biến mất hẳn thì `centreX == 0f`, thoả `panned.centreX < zoomed.centreX - 20f`, test báo pan thành công cho một bản dịch không còn trên màn hình. Test pinch anh em không có lỗ này.
- `SovereigntyViewModelTest.kt:53` — `assertFalse(settled.isLoading)` sau `first { !it.isLoading }`: tautology. (Lực thật của suite nằm ở `withTimeout` dòng 49 — verifier đúng khi hạ mức.)
- `JournalPaneSwitchTest.kt:49` và `DiscoveryStoryScrollTest.kt:49` vẫn ghi *"It will not run on API 37"* dù #18 đã đóng ngày 05.08.2026 và fix (`espressoCore = "3.7.0"`) đã nằm trong build. Đúng cơ chế đã khiến device leg **không được chạy suốt hai plan** lần trước.

**Đã kiểm và sạch:** không file `commonTest` nào import `java.*`; **186 tên test backtick** đã máy-kiểm từng cái cho `, . ; : [ ] ( ) < >` — sạch (đây là hai cái bẫy Kotlin/Native từng làm `:shared:allTests` xanh giả). `Dispatchers.setMain`/`resetMain` đủ ở cả 11 suite. Không `Thread.sleep`, không `advanceUntilIdle`, không `assertTrue(true)`, không test nào chỉ có setup.

---

## 6. Nhóm D — `LLM.md` lệch so với code

Theo `.claude/CLAUDE.md`, doc drift **là một defect**. Danh sách dưới đây đều đã được đọc-đối-chiếu.

### Số đếm sai

| Chỗ | Ghi | Thật |
|---|---|---|
| §9:506 + §11 row #25 | "All **twelve** ViewModels" / "eight of the twelve" | **11** ViewModel (file thứ 12 là base class `MviViewModel.kt`). §3 nói đúng ở hai chỗ khác → tài liệu **tự mâu thuẫn**. Ba trục độc lập cùng phát hiện |
| §9:553 | `:shared` 110 JVM / 99 iOS | **112 / 101** |
| §9:555 | `:data` 110 / 76 · tổng 413 | **120 / 86** · tổng **443**. Chính câu đó cũng không tự cộng đúng (110+99+110+76+10+10+4 = 419 ≠ 413) |
| §11 row #11 | "**One** screen file far over the 200-line rule" | **12** file screen > 200 dòng. Và §10:641 lại nói "**six**" — hai câu, hai con số, cùng một tài liệu |
| Row 11a / 11b / 11c | LensScreen 388 · DiscoveryScreen 627 · PassportScreen 258 | **594** · **653** · **278**. LensScreen là 594 ở *mọi commit* trong lịch sử hiện tại — không phải trôi sau khi viết |
| §9:523 | "`:app` has exactly one suite" | **2** (`AppGraphTest` 4 test + `RecognitionEndToEndTest` 4 test) |
| `AppGraphTest.kt:86` KDoc | "All **eight** modules" | **9** — và chính file đó assert `6+1+1+1` ở dòng 140 |

### Bản đồ sai

- **§3:123** — `voice/ expect: SpeechRecognizer, TextToSpeech`. **Cả hai vế đều sai**: `SpeechRecognizer` không tồn tại (dictation đã bị xoá), và `TextToSpeechManager` là `interface`, không phải `expect` — KDoc của nó nói thẳng điều đó.
- **§9:501-508** — cây `commonTest` thiếu `core/window/WindowClassTest.kt` (**9 test**, ~9% tổng), trong khi §7:474 lại viện dẫn nó đích danh.
- **§10:633** — bảo tạo `XComponents.kt` khi file quá ~150 dòng. §5:221-223 **cấm đúng điều đó**, và `find` không ra file `*Components.kt` nào. §10 là bảng bắt buộc đọc trước khi thêm file.
- **§3:114-116** — liệt kê `tablet/feature/` là `XTabletScreen.kt` + `XPane.kt`, không nhắc `RecentScanList.kt` (file thứ 9).
- **`AppGraphTest.kt:45`** — KDoc nói passport đọc route argument. `Routes.PASSPORT = "passport"` không có tham số nào; `PassportViewModel` không nhận `SavedStateHandle`. Đúng ba VM đọc route arg: Chat, Discovery, Translation.
- **§11 row #17** — mô tả `MEASURED_TYPE` như allowlist cho `.sp`. `DesignTokenTest.kt:85` truyền cùng list đó cho rule **fontWeight** — một miễn trừ toàn-file thứ hai, không ghi ở đâu, và đang gánh 3 call site thật.
- **§11 row #23** — vẫn ghi cần device API ≤ 36, dù #18 đã đóng.
- **`values-vi/strings.xml`** thiếu key `app_name` — key duy nhất phá bất biến "cả tám, luôn luôn". Không hại runtime (không code nào đọc nó từ composeResources), nhưng nó là **thuộc tính máy-kiểm-được duy nhất** của tám file đó.

---

## 7. Đã kiểm tra và SẠCH

121 khẳng định. Những cái đáng nêu, vì chúng là thứ dễ hỏng nhất:

**MVI**
- 10/10 ViewModel kế thừa `MviViewModel<S,I,E>`; `MainViewModel` là ngoại lệ duy nhất, đúng như §3.
- `onIntent` là public member duy nhất trên **cả 11** — quét từng khai báo cấp lớp.
- 10/10 Contract nằm file riêng, kể cả hai effect set rỗng.
- `launchSafely` rethrow `CancellationException` (`MviViewModel.kt:94-95`). Không `GlobalScope`, không `runBlocking` production.
- **Audit từng cờ busy, cả ba nhánh (success / Failure / onError): sạch toàn bộ.** Bốn lỗi cờ kẹt của row #25 đã fix thật.
- Sở hữu job có cấu trúc: stage ticker là **con** của analysis job (`LensViewModel.kt:200-207` + `finally { ticker.cancel() }`).
- Không reducer nào gọi repository/use case/`sendEffect` (trừ vụ `++cameraRequests` ở §4.4).

**Effect & điều hướng**
- `effects.collect` đúng **một** chỗ. `collectLatest` = 0.
- `repeatOnLifecycle(STARTED)` + `rememberUpdatedState` có thật trong `CollectEffects.kt:38-47`.
- Effect qua `Channel(BUFFERED).receiveAsFlow()`, không SharedFlow.
- **Không state nào chứa cờ điều hướng** — dump toàn bộ `val` của cả 10 `XState`.
- **Hai shell khớp cấu trúc**: cùng 10 `composable()`, cùng thứ tự, cùng 3 `navArgument … defaultValue = ""` cho `Routes.TRANSLATION`. Đây là thứ mà lệch một chút là `setGraph` xoá back stack mỗi lần đổi kích thước cửa sổ.
- `Routes.TOP_LEVEL` ↔ `TopLevelDestination` ↔ `RailDestination`: khớp từng phần tử, đúng thứ tự.
- Mọi route arg đọc qua hằng `Routes.ARG_*`, không literal.
- Một `NavHostController` duy nhất, tạo trên fork, không shell nào default tham số.

**Clean Architecture**
- `:domain` **sạch tuyệt đối** — lọc từng dòng `import` của cả 45 file. Không project dependency.
- 14/14 repository interface có impl trong `:data`.
- `:shared` → `:data` đúng bằng đường Koin mà §2 hứa.
- **Không Entity/DAO/DTO nào ra khỏi `:data`.**
- **Không quy tắc nghiệp vụ nào nằm trong ViewModel** — đọc hết cả 12.
- `AppResult`/`AppError` giữ nguyên ở biên `:data`: `throw` chỉ còn re-throw `CancellationException`.
- Không `if (BuildConfig.DEBUG)` trong implementation.

**DI**
- 11/11 ViewModel là `viewModel { }`, 0 cái `single { }`.
- 30/30 use case có đúng một dòng `factory { }` — đối chiếu **hai chiều**.
- Hai entry point Android/iOS truyền đúng `appModules(isDebug)`.
- Cả ba cảnh báo của §9 về `AppGraphTest` còn đúng, kể cả case chứng minh test không rỗng nghĩa.
- Không service-locator trong class nào.

**Nhánh mobile/tablet**
- **Không ViewModel nào dưới hai nhánh** (grep 25 file).
- **Không file nhánh nào import repository / use case / `:data`.**
- Route/Screen split đủ **cả mười** feature — §11 row #13 nói thật.
- Panes stateless đúng chuẩn.
- Chỉ ba composable `internal` — đúng ba file §5 kê tên.

**Design token & i18n**
- §11 row #19 (lỗ hổng tiềm ẩn ở `core/designsystem/`) — **kiểm tay lại, vẫn sạch**: 0 corner literal, 0 fontWeight call-site, 0 `.sp` ngoài `Type.kt`.
- **277 key chung cả tám locale, multiset placeholder giống hệt nhau ở cả tám** — không lệch số, không lệch chỉ số.
- §11 row #20 vẫn fix thật: 0 `%%`, 0 `\'`.
- `DesignTokenTest` reach 135 file (116/10/9) — đúng chính xác.
- 12/12 `expect` có `actual` ở **cả hai** nền tảng. 0 chỗ `if (Platform.isAndroid)`.

---

## 8. Đã bác bỏ — đừng đụng vào

| Bị tố | Vì sao không phải lỗi |
|---|---|
| `DiscoveryViewModel.kt:243` dùng `ApplicationScope.launch` thay `launchSafely` | `launchSafely` **là** `viewModelScope.launch`, không dùng được cho việc phải sống lâu hơn ViewModel — mà đó chính là mục đích của `discardUnsaved`, đã ghi ở dòng 231 |
| `LensHost.kt:62` có arm effect rỗng | Chính là **ví dụ mẫu** trong `docs/android-mvi-best-practices.md:534-539` |
| Use case một dòng là "lớp thừa" | Không quy tắc nào yêu cầu use case phải có logic; và fat use-case layer cũng không ngăn được lỗi §3.2, vốn nằm ở arm của ViewModel |
| `core/designsystem/layout/` thiếu trong §3 | Thư mục rỗng — git không biểu diễn được, không bản clone nào có |
| Fuzz test thiếu ở 8 ViewModel | "Concurrent jobs" trong §9 nghĩa là job cầm tay, không phải flow observer. Đọc đúng nghĩa thì cả ba VM đủ điều kiện **đều đã có** fuzz suite |

**Một điểm hai lượt phản biện bất đồng:** §2:57 "eighteen arrangement files". Lượt A nói phải là 19 (`tablet/feature/` có 9 file, thiếu `RecentScanList.kt`); lượt B bác, cho rằng `RecentScanList.kt` là component chứ không phải arrangement file nên 8 là đúng. **Không kết luận** — nhưng §3:114-116 liệt kê thiếu nó thì đúng ở cả hai cách hiểu.

---

## 9. Thứ tự đề xuất

1. **PR #1 — `AppResult` bị vứt** (§3.2 + §3.3 + §3.4). Năm call site, một hình dạng. Kèm hook `failOn…`/`throwOn…` còn thiếu trong `Fakes.kt` và test cho từng cái — vì chính vùng mù của fake đã che các lỗi này. Đóng nốt §11 row #26 cho toàn app thay vì một màn hình.
2. **PR #2 — rò `MainViewModel` trên iOS** (§3.1). Một dòng, nhưng là chỗ duy nhất hai nền tảng lệch hành vi.
3. **PR #3 — `LLM.md`** (§6). Rẻ, và nó là tài liệu mà `.claude/CLAUDE.md` bắt đọc trước mọi thay đổi code. Đang có chỗ tự mâu thuẫn ("one" vs "six" file quá dài; "twelve" vs "ten" ViewModel) — người đọc không biết câu nào mới.
4. **PR #4 — hai KDoc device test còn nói API 37 không chạy được** (§5). Một đoạn văn, nhưng chính nó từng khiến device leg bị bỏ suốt hai plan.
5. **Sau đó** — `PassportViewModel` ra sau port (§4.1) rồi §11 #27, và lần lượt nâng sáu chỗ trùng lặp nhánh (§4.3) mỗi khi chạm vào file đó.
