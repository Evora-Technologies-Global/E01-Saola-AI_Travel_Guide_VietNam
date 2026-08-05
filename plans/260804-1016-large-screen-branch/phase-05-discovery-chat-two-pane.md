# Phase 05 — Discovery + Chat: hai pane, guide 352

**Liên kết:** [plan.md](plan.md) · wireframe dòng 169-274 · `LLM.md` §6 (DI), §7 (điều hướng) ·
`DiscoveryScreen.kt` (1986 dòng → **627**) · `ChatViewModel.kt` · `SharedModules.kt`

## Tổng quan

**Ưu tiên:** P1 · **Trạng thái:** ☑ Xong 04.08.2026 — đã kiểm trên Pixel Tablet API 36 ở cả hai
khổ. Còn nợ: iPad, và mở hai discovery liên tiếp trên máy thật (máy thử chỉ có một bản ghi).

Đây là giai đoạn khó nhất, và cũng là thứ thiết kế coi là lý do tồn tại của bản tablet:

> *"AI guide là cột phải thường trú thay vì một màn riêng — vừa đọc câu chuyện vừa hỏi nó.
> Đó là thứ điện thoại không làm được."*

Story cuộn bên trái, cột guide 352dp bên phải, cùng lúc.

## Nhận định then chốt

- **Đây là chỗ duy nhất trong cả plan mà tablet buộc phải sửa code dùng chung.** `ChatViewModel`
  lấy `discoveryId` từ `SavedStateHandle`, và `SavedStateHandle` đến từ back-stack entry của
  route `chat/{discoveryId}`. Ở tablet chat không có entry riêng — nó sống bên trong entry của
  discovery.
- **Cách sửa:** thêm tham số `explicitDiscoveryId: String?`, giữ `savedStateHandle` làm đường
  lấy dự phòng. Mobile không đổi hành vi; tablet gọi
  `koinViewModel(key = discoveryId) { parametersOf(discoveryId) }`. **Không** đổi
  `ChatViewModel` thành `single`.
- **Route `CHAT` vẫn còn** ở đồ thị tablet, cho deep link và cho trường hợp xoay máy khi đang ở
  màn chat của nhánh mobile. Ở tablet, route đó mở chính discovery tương ứng, hai pane.
- `ChatScreen` cố ý bỏ qua chế độ tối (`LLM.md` §11 hàng #16). **Không** giải quyết ở đây; cột
  guide dùng đúng bảng màu chat như hiện tại.

## Yêu cầu

**Chức năng:** đọc story, nghe đọc, lưu vào journal, đánh dấu yêu thích, xem ảnh; đồng thời hỏi
guide và thấy câu trả lời — không rời màn.
**Phi chức năng:** cuộn story không làm cột guide cuộn theo và ngược lại.

## Kiến trúc

`feature/discovery/component/` — **21 file**, `feature/chat/component/` — **6 file**, mỗi file
một composable, `internal`:

| Gói | File |
|---|---|
| `discovery/component/` | `DiscoveryPhoto` · `MatchBadge` · `DiscoveryTitleBlock` · `DashedInk` (rule + `Modifier.dashedBorder`) · `ListenCard` (+ `Waveform`, `narrationMinutes`) · `StoryBody` (+ `LowConfidenceNote`) · `ThingsToNotice` · `Ordinal` · `ContextChips` · `SaveRow` · `NoteBlock` · `NoteCards` (blank + written) · `NoteComposer` · `NotePhotoStrip` (+ `PhotoActionTile`) · `NotePhotoViewer` (+ `PhotoAlbum`, `ZoomablePhoto`) · `NoteCameraOverlay` · `NoteDialogs` (lời nhắc quyền + hộp thoại xoá) · `NearbyBlock` · `SuggestedQuestions` · `DiscoveryFooter` · `RememberLastPresent` |
| `chat/component/` | `ChatThread` · `ChatBubble` · `ChatComposer` · `SuggestionPill` · `ThinkingCard` · `ChatEmptyState` |

```
tablet/feature/discovery/DiscoveryTabletScreen.kt
└── TwoPaneScaffold(fixedPaneWidth = PaneWidth.guide, fixedPaneAtStart = false)
    ├── pane co giãn : OverlayHeader { đóng · MatchBadge ‖ SaveRow(300) · xoá }
    │                  cuộn — Row { cột 296: ảnh · ListenCard · chips
    │                              cột co giãn: tiêu đề · story · THREE THINGS }
    │                        NoteBlock · NearbyBlock · Footer
    └── pane 352     : PageHeader("Ask your guide" / "About {title}")
                       ChatThread · ChatComposer
```

Còn lại ở mỗi nhánh: `PhotoHeader` + `AskPill` (chỉ mobile), `StoryPane`/`GuidePane` (chỉ tablet).

## File liên quan

**Tạo:** `feature/discovery/component/*.kt` (21) · `feature/chat/component/*.kt` (6) ·
`tablet/feature/discovery/DiscoveryTabletScreen.kt` · `commonTest/…/ChatViewModelTest.kt`
**Sửa:** `ChatViewModel.kt` (chữ ký + một lỗi, xem dưới) · `di/SharedModules.kt` ·
`core/designsystem/theme/Color.kt` (`GuidePalette`) · `mobile/feature/discovery/DiscoveryScreen.kt` ·
`mobile/feature/chat/ChatScreen.kt` · `tablet/navigation/TabletNavGraph.kt` ·
`androidHostTest/…/DesignTokenTest.kt` · `androidHostTest/…/ComposeStabilityReportTest.kt` ·
`commonTest/…/Fakes.kt` · `composeResources/values*/strings.xml` (1 chuỗi × 8 file) · `LLM.md`

## Todo

- [x] `ChatViewModel` nhận `explicitDiscoveryId` tường minh
- [x] `SharedModules.kt` — đăng ký có tham số (`params.getOrNull<String>()`)
- [x] Chat trên mobile vẫn chạy nguyên — kiểm trên máy, xem bảng dưới
- [x] Rút 27 component discovery + chat
- [x] `DiscoveryTabletScreen.kt` + `DiscoveryTabletRoute`
- [x] Nối `DISCOVERY` và `CHAT` ở đồ thị tablet, cả hai qua `NavBackStackEntry.discoveryId()`
- [x] Test `explicitDiscoveryId` — `ChatViewModelTest`, 5 ca
- [x] Chuỗi mới `chat_about` — đủ tám ngôn ngữ
- [x] `LLM.md` §3, §5, §6, §7, §9, §11 (hàng 11, 11b, 16, 24), §12
- [x] Biên dịch ba đích: `:shared:compileAndroidMain`, `:app:compileDebugKotlin`,
      `:shared:compileKotlinIosSimulatorArm64` — cả ba xanh
- [x] `:shared:testAndroidHostTest` — **41/41 xanh** (trước phase này là 36)
- [ ] Thử trên iPad thật / simulator — nợ chung từ phase 02
- [ ] Mở hai discovery liên tiếp trên máy thật — máy thử chỉ có một bản ghi; cơ chế `key` đã
      được `ChatViewModelTest` phủ ở mức ViewModel

## Tiêu chí hoàn thành

Đã kiểm trên **Pixel Tablet API 36**, bản debug `com.duylt.trave.vietlensai.dev`:

| Tiêu chí | Kết quả |
|---|---|
| Mở discovery trên khổ 1280 × 800dp → thấy story trái, guide phải, không phải bấm gì thêm | ☑ toolbar (đóng · LANDMARK 100% MATCH ‖ Save to favourites · chia sẻ · xoá), cột 296 ảnh + Listen + chips, cột chữ, cột guide 352 với "Ask your guide / About Ho Chi Minh Mausoleum" |
| Hỏi guide trong lúc story đang cuộn dở → story không nhảy về đầu | ☑ hỏi *"Who designed it"*, Gemini trả lời trong cột phải; story vẫn đứng ở "VISITING ETIQUETTE" |
| Cột guide đầy đủ | ☑ trạng thái rỗng "Ask me anything" khi chưa hỏi; sau khi hỏi hiện bong bóng, nút "Listen", và nút xoá luồng xuất hiện ở header |
| `SuggestedQuestions` không vẽ hai lần | ☑ story pane không có khối này; câu hỏi nằm ở "TRY ASKING" trong cột guide |
| Đổi kích thước cửa sổ (QĐ-4) | ☑ 1280×800dp → 337×731dp: chuyển sang màn discovery **mobile**, đúng discovery đó; về lại 1280×800dp thì hai pane trở lại kèm nguyên luồng chat; back một lần về Nhật ký |
| `DiscoveryScreen.kt` xuống dưới 1000 dòng | ☑ **627** |

Và ở **khổ mobile trên cùng máy** (337 × 731dp) — nhánh mobile sau khi rút component:

| Tiêu chí | Kết quả |
|---|---|
| Trang discovery | ☑ ảnh full-bleed với mép giấy lượn, badge, tiêu đề, ListenCard, story, THINGS TO NOTICE, chips, SaveRow, khối ghi chú, chân trang gạch đứt, AskPill nổi |
| Màn chat | ☑ `PageHeader` nền kem có nút back + nút xoá luồng, bong bóng guide, composer — đúng như trước khi rút |
| Khối ghi chú | ☑ "Write a note" mở composer: hai ô gạch đứt Take/Add photo, "0 / 6 PHOTOS", ô chữ, Save/Cancel; AskPill lùi đi đúng lúc |
| Lời nhắc quyền camera | ☑ `NoteCameraPermissionSheet` hiện đúng câu và hai nút |
| Crash | ☑ không có gì trong buffer `crash`, không `FATAL EXCEPTION` |

## Lệch khỏi plan, và lý do

| Lệch | Vì sao |
|---|---|
| 27 component thay vì 11 mà plan liệt kê | Cùng bài học phase 04: đếm theo *màn*, không theo *panel*. Ngoài các khối story còn có khối ghi chú (5 file), trình xem ảnh, overlay camera, hai modal, và `rememberLastPresent` — bỏ lại cái nào thì tablet phải chép cái đó |
| Bảng màu chat chuyển sang `Color.kt` thành `GuidePalette` | Hai nhánh cùng vẽ một cuộc hội thoại thì không thể mỗi bên giữ một bản màu. Đây đúng là việc `LLM.md` §11 hàng #16 đề nghị; **không** đụng tới quyết định chế độ tối |
| Không có `CollectEffects` cho `chatViewModel` | Plan nói "hai ViewModel thì hai lần `CollectEffects`". `ChatEffect` là sealed interface rỗng, và MVI doc §4 nói thẳng bốn feature có tập effect rỗng thì **đúng** là không có collector. Một `when` không nhánh nào |
| `SuggestedQuestions` vắng mặt ở story pane tablet | Cột guide đã mời đúng ba câu đó dưới "TRY ASKING". `DiscoveryEffect.OpenChat` vẫn được thu và được rót vào cột guide, nên bấm hỏi vẫn chạy |
| Sửa thêm một lỗi ngoài phạm vi | Xem dưới |
| `chat_about` — chuỗi mới, ngoài plan | Wireframe ghi "About {title}" ở đầu cột guide. Không có nó thì phụ đề chỉ còn trơ tên địa danh |

## Lỗi bắt được khi viết test

`ChatViewModel.send` nâng `isSending` rồi hạ nó ở cả hai nhánh `AppResult`, nhưng
`launchSafely(onError = …)` chỉ ghi `error`. Một cú ném **không được bọc** — hàng Room hỏng,
mapper gặp cột do bản cũ ghi — để lại thẻ *"đang nghĩ"* trên màn vĩnh viễn, và composer bị khoá
sau nó vì `canSend` đọc cùng cờ đó. Đường ra duy nhất của người dùng là rời cuộc hội thoại.

Đúng loại "stuck state" mà bảng §7 của MVI doc liệt kê, và là loại duy nhất mà bộ test chưa phủ
cho feature này — chat chưa từng có test. Sửa một dòng, ghi vào `LLM.md` §11 bảng Fixed hàng #24.

## Rủi ro — kết quả

| Rủi ro | Kết quả |
|---|---|
| Đổi chữ ký `ChatViewModel` làm hỏng chat mobile | ☑ Không. Đã mở màn chat ở khổ mobile trên máy: header, luồng cũ, composer nguyên vẹn |
| Thiếu `key` → guide nói nhầm discovery | ☑ Có `key = discoveryId` và tham số tường minh; `ChatViewModelTest` có ca *"an explicit id wins over the route argument"* với hai id **lệch nhau** — nếu đường dự phòng thắng thì ca đó đỏ |
| Bảng màu chat chỏi với trang theo scheme | Cột kem đứng cạnh pane theo scheme là chỗ đầu tiên hai hệ màu gặp nhau trên một màn. Đã ghi vào `LLM.md` §11 hàng #16 chứ không xử lý ở đây |
| Hai `CollectEffects` bị gộp tay thành một | Không phát sinh — chỉ có một, và lý do vì sao chỉ một đã ghi ngay tại chỗ |

## Bảo mật

Không có thay đổi. Prompt và khoá API vẫn đi qua đúng use case cũ.

## Tiếp theo

[Phase 06](phase-06-journal-passport-master-detail.md). Bước rút component ở đó lặp lại đúng
khuôn này — và lưu ý `PassportScreen` 1059 dòng là file lớn nhất còn lại.
