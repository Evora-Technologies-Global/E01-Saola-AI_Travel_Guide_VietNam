# Phase 09 — Kiểm thử và tài liệu

**Liên kết:** [plan.md](plan.md) · `LLM.md` §8, §9, §11 · MVI doc §7, §9 ·
`ComposeStabilityReportTest.kt:322` · `DesignTokenTest.kt`

## Tổng quan

**Ưu tiên:** P1 — không phải việc dọn dẹp cuối, mà là thứ giữ cho cả plan này khỏi bị tháo ra
sau hai tuần
**Trạng thái:** ☑ Xong 04.08.2026 — kết quả đầy đủ ở [verification.md](verification.md)

## Nhận định then chốt

- **Không có ViewModel mới thì cũng không có test ViewModel mới.** Đó là hệ quả cố ý của
  phương án A: `commonTest` hiện có phủ luôn nhánh tablet. Cái *chưa* được phủ là tầng sắp đặt.
- **`UNSTABLE_CLASS_CEILING = 20`** (`ComposeStabilityReportTest.kt:322`). Nhánh tablet thêm
  composable chứ không thêm lớp giữ trạng thái, nên con số này *nên* đứng yên. Nếu nó tăng thì
  có lớp không đáng bị lôi vào composition — điều tra, đừng nâng trần. KDoc của hằng đó nói rõ
  nó là số đo tại thời điểm chốt, không phải chỉ tiêu.
- **`androidDeviceTest` không chạy được trên Android 15+** (`LLM.md` §11 hàng #18,
  `NoSuchMethodException: InputManager.getInstance`). Máy này chỉ có emulator API 37. Nghĩa là
  test thiết bị cho hai pane **không chạy được ở đây** — phải nói thẳng điều đó chứ không im
  lặng bỏ qua. Hoặc dựng AVD API 34, hoặc nâng `ui-test-junit4`.
- **`commonTest` không biên dịch cho Kotlin/Native** (§11 hàng #14, `SecurityException` là lớp
  JVM ở `LensViewModelCrashTest.kt:187`). Đây là chỗ hợp lý để sửa — một dòng — vì plan này
  vốn đã bắt cả hai nền phải xanh.

## Yêu cầu

**Chức năng:** không thêm gì.
**Phi chức năng:** hai gate ở `androidHostTest` phải soi được nhánh tablet; tài liệu khớp với
cây thư mục thật.

## Các bước

### Kiểm thử

1. `DesignTokenTest` — xác nhận nó đang thật sự đọc `tablet/feature/`. In số file ra và so với
   số file có thật; assert chống tập rỗng từ phase 01 phải còn nguyên.
2. `HEADER_OWNERS` — bổ sung các màn tablet. `LensTabletScreen` và `SovereigntyOverlay` là
   ngoại lệ, cùng lý do như bản mobile, và phải ghi lý do vào KDoc của danh sách.
3. `ComposeStabilityReportTest` — chạy, so với trần 20. Tăng thì điều tra.
4. Sửa §11 hàng #14 để `commonTest` biên dịch được cho iOS, rồi chạy `:shared:allTests`.
5. Test thiết bị cho hai pane: giữ story cuộn khi hỏi guide (phase 05), giữ vị trí cuộn danh
   sách ngày khi đổi pane phải (phase 06). Viết vào `androidDeviceTest`, và **ghi rõ trong plan
   rằng chúng chưa chạy được trên máy này** nếu AVD API 34 chưa dựng.
6. Kiểm thử tay theo ma trận thiết bị dưới đây.

### Tài liệu

7. `LLM.md` §2 — nói rõ `:shared` giờ có hai tầng trình bày dùng chung một tầng ViewModel.
8. `LLM.md` §3 — vẽ lại cây thư mục.
9. `LLM.md` §5 — giải phẫu feature package đổi rồi: Contract + ViewModel + `component/` ở gốc,
   Screen nằm ở nhánh.
10. `LLM.md` §7 — quy ước `NavHost` lồng của pane (phase 06), và việc hai shell dùng chung
    `Routes` cùng một `NavHostController`.
11. `LLM.md` §10 — thêm hàng "tôi đang thêm một màn tablet" vào bảng "file mới đi đâu".
12. `LLM.md` §11 — chuyển hàng #11 (phần đã rút), #13, #14 sang bảng Fixed; thêm hàng mới cho
    bất cứ thứ gì bị hoãn.
13. `LLM.md` §13 — ghi `PaneWidth` vào chuẩn UI, kèm lý do vì sao nó không nằm trên `Spacing`.
14. MVI doc §9 — thêm mục vào checklist trước PR: *"màn mới có phiên bản tablet chưa, hay đã
    ghi lý do vì sao không?"*
15. `docs/large-screen-layout.md` — tài liệu mới: ngưỡng, quy tắc chọn nhánh, bề rộng pane, và
    quy tắc *"tablet chỉ được sắp lại, không được nghĩ khác"*, kèm ngoại lệ đã duyệt
    (`RecentScanList` ở phase 04).

## Ma trận kiểm thử tay

| Thiết bị | Kiểm |
|---|---|
| iPad ngang | cả năm màn, xoay giữa chừng ở mỗi màn |
| iPad dọc | phải ra nhánh mobile — hoặc tablet, tuỳ QĐ-4 |
| Tablet Android ngang | như iPad |
| Tablet Android split-screen hẹp | phải rơi về nhánh mobile |
| Fold — gập rồi mở | đổi shell mà giữ nguyên chỗ đang đứng |
| Điện thoại | **không đổi gì cả** — đây là điều kiện nghiệm thu quan trọng nhất |
| iPhone nằm ngang | vẫn khoá portrait ở giai đoạn này — mở khoá là [phase 10](phase-10-iphone-landscape.md) |
| Tám ngôn ngữ trên iPad | nhãn rail, hai cột settings, tiêu đề pane, thẻ *"Around you"* |

## Todo

- [x] `DesignTokenTest` soi được `tablet/`, có in số file — **135 file: 116 / 10 / 9**, đếm tay
      trên đĩa khớp từng con
- [x] `HEADER_OWNERS` bổ sung màn tablet + ghi ngoại lệ — 14 mục, đã có từ phase 04–08
- [x] `ComposeStabilityReportTest` so trần — **21, không phải 20**; phase 04 đã nâng và ghi lý
      do, plan này viết theo số cũ. Không tăng thêm trong phase 09
- [x] Sửa §11 hàng #14, chạy `:shared:allTests` — **hai** lỗi chứ không phải một; xanh cả JVM
      (41) lẫn iOS simulator (30)
- [x] Test thiết bị hai pane — viết **và chạy được**: 12/12 xanh trên Pixel Tablet API 35.
      Hàng #18 của `LLM.md` sai phạm vi, đã sửa
- [x] Ma trận kiểm thử tay — 11/12 hàng; fold thật là hàng duy nhất chưa chạy, ghi rõ ở
      `verification.md`
- [x] `LLM.md` §2 §3 §5 §7 §9 §10 §11 §13 — §3 §7 §10 §13 đã đúng từ phase 01–08; phase này
      viết §2, §5 (ngoại lệ `internal`), §9 (hai quy tắc Kotlin/Native + cây `androidDeviceTest`),
      §11 (#14 → Fixed, #18 sửa phạm vi)
- [x] MVI doc §9 — thêm nhóm **Arrangement**, bốn mục
- [x] `docs/large-screen-layout.md`

## Tiêu chí hoàn thành

- `:shared:allTests` xanh trên cả JVM và iOS simulator.
- Hai gate `androidHostTest` xanh **và** chứng minh được là đang đọc nhánh tablet.
- Đi hết ma trận kiểm thử tay, ghi lại kết quả vào `verification.md` của thư mục plan này —
  cùng cách hai plan trước đã làm.
- `LLM.md` khớp cây thư mục thật. Lệch tài liệu là một lỗi, và sửa trong chính commit gây ra
  nó — quy tắc ở `.claude/CLAUDE.md`.

## Rủi ro

| Rủi ro | Giảm thiểu |
|---|---|
| Gate token xanh vì đọc tập rỗng | Bước 1 in số file; đừng tin màu xanh |
| Test thiết bị bị lặng lẽ bỏ qua vì máy không chạy được | Ghi thẳng vào `verification.md` là chưa chạy, chứ đừng đánh dấu hoàn thành |
| Tài liệu bị hoãn sang "commit sau" | Không có commit sau. Quy tắc cập nhật của dự án nói vậy |

## Bảo mật

Trước khi đóng: `git diff` toàn bộ plan, tìm khoá API, endpoint hay đường dẫn bị viết cứng lọt
vào nhánh tablet. Nhánh này không được có nguồn cấu hình riêng.

## Tiếp theo

Nhánh tablet đóng ở đây. Còn lại [phase 10](phase-10-iphone-landscape.md) — mở khoá iPhone
landscape — chạy sau cùng và hoãn được sang đợt khác.

Feed Explore trong wireframe không thuộc plan này; nếu sau này dựng thì đó là plan mới.
