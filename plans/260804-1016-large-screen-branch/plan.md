# Màn hình lớn — nhánh `tablet` riêng trong `:shared`

**Tạo:** 04.08.2026 · **Nhánh git:** `duylt_dev` · **Đích:** `:shared`, `:app`, `iosApp`
**Nguồn thiết kế:** Claude Design *Travel app MVP với bản sắc Việt* →
`Việt Travel Lens - Tablet.dc.html`, khổ duy nhất **1194 × 834** (iPad landscape)
**Format:** nhiều file. Hai plan trước gộp một file; plan này 9 giai đoạn nên gộp lại sẽ
vượt `docs.maxLoc` 800 dòng trong `.ck.json`.

---

## Mandate

Dựng **entry point thứ hai** cho màn rộng. `:shared` tách làm hai nhánh trình bày:
`mobile/` giữ nguyên những gì đang chạy, `tablet/` sắp đặt lại theo wireframe.
`MainActivity` (và `MainViewController` bên iOS) chọn nhánh.

Ràng buộc do chủ dự án đặt ra, và mọi giai đoạn dưới đây phải tôn trọng:

> **UI component phải giống bản mobile. Chỉ cách sắp xếp là tuân theo wireframe.**

Hệ quả trực tiếp: **Contract, ViewModel, use case và design system dùng chung, không nhân
bản.** Thứ được nhân đôi chỉ là tầng sắp đặt — `XScreen.kt` và `navigation/`.

---

## Bốn quyết định — đã chốt 04.08.2026

Đánh đổi đầy đủ ở [architecture-options.md](architecture-options.md).

| # | Câu hỏi | Chốt |
|---|---|---|
| QĐ-1 | Nhân đôi tới đâu | **Phương án A** — chung ViewModel, tách Screen + navigation |
| QĐ-2 | Explore: wireframe vẽ feed, code đang là bản đồ | **Giữ bản đồ**, sắp lại: *"Around you"* + hai nút icon xếp dọc ở góc trên trái, danh sách kết quả thành cột dọc bên phải |
| QĐ-3 | Phạm vi nền tảng | **Mở hết cho cả iPhone lẫn iPad** — iPad ở phase 02, iPhone ở **phase 10, ưu tiên cuối** |
| QĐ-4 | Ngưỡng và hành vi khi đổi kích thước | Rộng **≥ 840dp** và cao **≥ 600dp**, dùng chung `NavHostController` |

> **Ngưỡng thứ hai, thêm ở phase 10 (05.08.2026).** `rememberCanStackVertically` — cao
> **≥ 500dp** — không chọn nhánh: cả hai đáp án đều là `mobile/`. Nó chọn *cách sắp đặt bên
> trong* nhánh điện thoại, và chỉ Lens đọc nó. Cố tình không dùng lại 600 của QĐ-4: 600 là
> chiều cao hai pane cần, 500 là chiều cao một cột cần.

---

## Giai đoạn

| # | Giai đoạn | Trạng thái | File |
|---|---|---|---|
| 01 | Tách `:shared` thành hai nhánh | ☑ Xong 04.08.2026 | [phase-01](phase-01-split-shared-into-two-branches.md) |
| 02 | Điểm rẽ nhánh và chuyện xoay màn | ☑ Xong 04.08.2026 · **đã thử iPad** ở phase 09 | [phase-02](phase-02-entry-point-and-orientation.md) |
| 03 | Khung tablet — rail, hai pane, token bề rộng | ☑ Xong 04.08.2026 · QĐ-4 đã chứng minh | [phase-03](phase-03-tablet-shell-rail-and-panes.md) |
| 04 | Lens — viewfinder + panel phải 310 | ☑ Xong 04.08.2026 · **đã thử iPad** ở phase 09 | [phase-04](phase-04-lens-tablet.md) |
| 05 | Discovery + Chat — hai pane, guide 352 | ☑ Xong 04.08.2026 · test thiết bị ở phase 09 | [phase-05](phase-05-discovery-chat-two-pane.md) |
| 06 | Journal master–detail + Passport 392 | ☑ Xong 04.08.2026 · test thiết bị ở phase 09 | [phase-06](phase-06-journal-passport-master-detail.md) |
| 07 | Explore — bản đồ full-bleed + hai cụm nổi | ☑ Xong 04.08.2026 · đã thử ở 1664×768dp | [phase-07](phase-07-explore-tablet.md) |
| 08 | Settings + Chủ quyền + sheet 440 | ☑ Xong 04.08.2026 · đã thử ở 1280×800dp, ja/th, và một lần resize | [phase-08](phase-08-settings-and-sovereignty-tablet.md) |
| 09 | Kiểm thử và tài liệu | ☑ Xong 04.08.2026 · [verification.md](verification.md) | [phase-09](phase-09-tests-and-docs.md) |
| 10 | iPhone landscape — mười màn mobile | ☑ Xong 05.08.2026 · **chín trên mười màn không phải sửa** | [phase-10](phase-10-iphone-landscape.md) |

Phase 04–08 độc lập với nhau, có thể chạy song song sau khi 03 xong. 01 → 02 → 03 là chuỗi.
Phase 10 rời hẳn phần còn lại — nó không đụng nhánh tablet, và hoãn được sang đợt sau.

---

## Phụ thuộc then chốt

- **Phase 01 phá hai gate cùng lúc.** `DesignTokenTest.featureFiles()` duyệt đúng thư mục
  `…/feature`, và `HEADER_OWNERS` khớp theo *tên file*. Đổi đường dẫn mà không sửa test thì
  bốn quy tắc token lặng lẽ chạy trên tập rỗng — hỏng nguy hiểm hơn là gãy build.
- **Phase 05 chạm chữ ký `ChatViewModel`.** ☑ 04.08.2026 — thêm `explicitDiscoveryId: String?`,
  `SavedStateHandle` thành đường dự phòng. Một chi tiết plan gốc chưa biết: entry của
  `discovery/{discoveryId}` **cũng** mang đúng tên tham số ấy, nên đường dự phòng vẫn trả lời
  đúng ở hiện tại. Đó là hai route tình cờ đặt tên giống nhau, không phải hợp đồng — và một
  cột guide dựa vào nó chỉ cách một lần đổi tên route là nói về nhầm nơi mà build log không hé
  răng. `ChatViewModelTest` chốt bằng một ca có hai id **lệch nhau**.
- **Phase 02 là điều kiện để nhìn thấy bất cứ thứ gì trên iPad** — `Info.plist` khoá portrait,
  **và** dự án Xcode để `TARGETED_DEVICE_FAMILY = 1` (chỉ iPhone), thứ hai này không có trong
  plan gốc và phát hiện lúc triển khai. Cả hai đã sửa.
- **Phase 03 đã chứng minh QĐ-4.** ☑ 04.08.2026 — Hộ chiếu sống qua 1280×800dp →
  337×731dp → về lại, và back một lần nữa vẫn về Nhật ký. Nhưng điều kiện hoá ra chặt hơn
  "shell tablet đăng ký đủ bộ `Routes`": **hai đồ thị phải bằng nhau về cấu trúc**, vì đó là
  phép so mà `setGraph` dùng để quyết định giữa cập-nhật-tại-chỗ và xoá sạch back stack. Một
  route lệch, hay một `navArgument` mặc định lệch, là đủ. Cách tái hiện nằm cuối
  [phase-03](phase-03-tablet-shell-rail-and-panes.md); ràng buộc đã viết vào `LLM.md` §7.
- **Phase 04–08 mỗi phase sửa một `composable` block trong `tablet/navigation/TabletNavGraph.kt`.**
  Đồ thị tách khỏi shell chính vì năm phase này chạy song song. Sửa route nào thì **chỉ**
  sửa route đó, và giữ nguyên chuỗi route lẫn `navArgument` — xem gạch đầu dòng trên. ☑ Xong
  cả năm; `Routes.TRANSLATION` là route duy nhất còn trỏ vào màn của điện thoại.
- **Phase 08 phát hiện plan gốc mô tả một màn Settings không còn tồn tại.** ☑ 04.08.2026 —
  plan viết theo wireframe và theo code của *trước* `ef5c6a9`: nó yêu cầu tách
  `RegionPickerBody` khỏi một `ModalBottomSheet` chọn ngôn ngữ, nhưng picker ấy đã bị xoá khi
  lời kể chuyển sang theo ngôn ngữ của máy, và cả màn Settings không còn một sheet nào. Ba
  nhóm plan liệt kê cũng không khớp: thực tế có năm, và không nhóm nào chứa ba mục plan gọi
  tên. Bài học không phải "plan sai" — mà là **đọc file trước khi tin dòng mô tả của nó trong
  plan**, đúng như quy tắc đọc `LLM.md` §11 trước khi chép một pattern. Cái thay vào chỗ modal
  440 là chặn bề rộng cho hai `AlertDialog` đang có, và `PaneWidth.sheet` — trước đó là một
  token không ai đọc — giờ có ba người đọc.
- **Phase 09 trả ba món nợ mà plan tưởng là không trả được, và cả ba đều vì cùng một lý do:
  một câu khẳng định trong tài liệu, không ai chạy lại.** ☑ 04.08.2026. (a) `LLM.md` §11 hàng
  #18 viết *"androidDeviceTest không chạy được trên Android 15 trở lên"*, nên suốt hai plan
  không ai chạy chân thiết bị; đo lại thì **API 35 và 36 đều xanh, chỉ API 37 hỏng** — và ba
  lỗi trong chính hai test mới chỉ lộ ra khi chạy thật. (b) Hàng #14 hoá ra là **hai** lỗi
  chồng nhau: sửa `SecurityException` xong thì Kotlin/Native chặn tiếp một *tên hàm có dấu
  phẩy*, thứ JVM chấp nhận — nên `:shared:allTests` chưa từng xanh, và giờ là 41 + 30. (c) Nợ
  *"chờ thử iPad"* của phase 02 và 04 đã trả: iPad Pro 11″ ngang ra nhánh tablet, dọc ra nhánh
  mobile vì 834 < 840 — **sáu** điểm, không phải bốn như chú thích `Info.plist` viết. Chi tiết
  và ảnh ở [verification.md](verification.md).
- **Phase 06 phải sửa cả `RailDestinations.kt`, thứ plan gốc không lường.** ☑ 04.08.2026 —
  trên tablet `PASSPORT` và `COLLECTION` mở chính màn Nhật ký, nhưng chúng không nằm trong
  `Routes.TOP_LEVEL`, nên `isTopLevel()` trả false và **rail biến mất**. Người dùng deep-link
  vào hộ chiếu, hoặc đang ở Bộ sưu tập khổ điện thoại rồi phóng cửa sổ to ra, sẽ nhận một màn
  top-level không có lối đi nào ngoài cử chỉ back. Đã tái hiện trên Pixel Tablet. Sửa bằng
  cách để `railDestination()` — chứ không phải `isTopLevel()` — quyết định rail có hiện hay
  không; **không** đụng `Routes.TOP_LEVEL`, vì thêm hai route đó vào là đẩy chúng lên thanh
  dưới của điện thoại. Ghi ở `LLM.md` §7.

## Rủi ro lớn nhất

Sáu file screen đang quá khổ (`LensScreen` 2183, `DiscoveryScreen` 1986, `PassportScreen`
1055, `JournalScreen` 837, `SettingsScreen` 751, `TranslationScreen` 679 — `LLM.md` §11 hàng
#11). Component dùng chung đang nằm `private` bên trong chúng. Nếu không rút ra trước khi
sắp đặt lại, nhánh tablet sẽ chép lại chứ không dùng lại — và ràng buộc *"component phải
giống mobile"* mất hiệu lực ngay ở commit đầu tiên. Mỗi phase 04–08 vì thế bắt đầu bằng một
bước rút component, và bước đó trả nợ luôn hàng #11.

**Năm file đã đi qua nó:** `LensScreen` 2183 → 388 (phase 04), `DiscoveryScreen` 1986 → 627
(phase 05), `PassportScreen` 1059 → 258 và `JournalScreen` 840 → 248 (phase 06, kèm
`CollectionScreen` 497 → 147 dù nó chưa từng nằm trong hàng #11). Phase 07 thêm
`ExploreScreen` 407 → 231, cũng chưa từng nằm trong hàng #11 nhưng giữ ba lớp phủ và cả cụm
điều khiển ở dạng `private`. Phase 08 đóng `SettingsScreen` 755 → 178, và kèm theo đó là
`SovereigntyScreen` 295 → 80 — file duy nhất trong app chưa hề tách Route/Screen, tức là
hàng #13 luôn. **Còn lại đúng `TranslationScreen` 683**, và nó còn lại vì đúng một lý do:
đó là màn duy nhất không có bản tablet, nên chưa có gì ép nó phải rút. Nó là việc của
phase 10.

**Phase 04 đã đi qua nó một lần và đặt ra khuôn.** `LensScreen` 2183 → 388, mười bốn
composable sang `feature/camera/component/`, mỗi file một cái, `internal`. Ba điều học được,
phase 07–08 dùng lại được ngay:

- **Đếm component theo *màn*, không theo *panel*.** Plan phase 04 liệt kê sáu; thực tế cần
  mười bốn, vì bên cạnh cụm điều khiển thì viewfinder, đếm ngược, lời nhắc quyền và snackbar
  cũng là thứ hai nhánh cùng vẽ. Phase 06 lặp lại y hệt: plan liệt kê tám, thực tế 24 — và
  còn thiếu cả một *màn* (Collection) mà plan không nhắc tới.
- **Phần không-layout của Route cũng phải rút.** Nếu `XRoute` giữ nhiều hơn một cú gọi — một
  coroutine, một observer vòng đời — thì nó là *hành vi*, và `LLM.md` §3 cấm nhánh sở hữu
  hành vi. Phase 04 tách thành `feature/camera/LensHost.kt`; ngoại lệ và ràng buộc của nó
  nằm ở `LLM.md` §5. **Phase 07 tách cái thứ hai** — `feature/explore/ExploreHost.kt`, giữ
  cầu nối quyền vị trí, chỗ thu effect và dòng `remember(viewModel) { viewModel::onIntent }`.
  Hai cái là đủ để nói đây là khuôn, không phải ngoại lệ: cứ màn nào có Route dài hơn một cú
  gọi thì rút, đừng chép.
- **Danh sách thì rút thành `LazyListScope` extension, không phải composable.** Hai nhánh đặt
  cùng bộ item vào hai container cuộn khác nhau; một composable sẽ phải lồng cuộn trong cuộn.
  `journalDays(…)` và `collectionBoard(…)` là hai cái đầu tiên (phase 06).
