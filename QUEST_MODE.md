# Nhiệm vụ săn ảnh (Quest Mode) — đề xuất hoãn lại

> **Trạng thái:** chưa triển khai. Ghi lại để đánh giá sau.
> **Ngày ghi:** 2026-08-01
> **Bối cảnh:** đề xuất thay thế cho tab Explore. Bộ sưu tập văn hoá đã làm xong và
> nằm ở màn hình Collection riêng, mở từ Nhật ký cạnh Hộ chiếu — xem mục "Culture
> collection" trong README. **Tab Explore hiện để trống.**
>
> **Lưu ý về hướng đi:** phương án *Khám phá theo tỉnh* (bản đồ 34 tỉnh) đã bị loại.
> Thứ điền vào tab Explore phải là cái Hộ chiếu và Bộ sưu tập không làm được: nói đi
> đâu / tìm gì **tiếp theo**, chứ không phải thêm một bản ghi việc đã rồi.

---

## Một câu

App chuyển từ *"chụp cái này là cái gì"* sang *"đi tìm cho tôi cái này"* — biến công cụ
tra cứu bị động thành trò chơi có mục tiêu.

## Vì sao đáng làm

Toàn bộ app hiện tại chỉ phản ứng: người dùng phải tự nghĩ ra thứ để chụp. Quest mode
là thứ duy nhất trong các phương án đã cân nhắc **tự đưa ra mục tiêu** cho người dùng,
và là thứ duy nhất mời được người khác tham gia — trong buổi demo có thể đưa máy cho
ban giám khảo và để họ tự hoàn thành một nhiệm vụ.

## Vì sao hoãn

Tốn công nhất trong bốn phương án đã cân nhắc, và là phương án duy nhất phải sửa vào
luồng camera — thứ đang chạy ổn định nhất trong app. Một Explore hoàn chỉnh đáng giá
hơn một Explore tham vọng còn dở.

---

## Thiết kế

### Màn hình chính

Ba phần:

1. **Nhiệm vụ hôm nay** — 3 nhiệm vụ, làm mới theo ngày
2. **Đang làm** — những nhiệm vụ đã mở nhưng chưa xong
3. **Bộ sưu tập huy hiệu** — đã hoàn thành, nhóm theo tỉnh

### Nhiệm vụ trông như thế nào

Không phải "đi thăm Văn Miếu". Nhiệm vụ dạy **cách nhìn**:

| Tên | Độ khó | Nội dung |
|---|---|---|
| Mái đao cong | Vừa | Tìm một mái đình hoặc chùa có góc mái cong vút lên như mũi thuyền. Đó là đầu đao — dấu hiệu nhận biết kiến trúc đình chùa Bắc Bộ. |
| Nước dùng trong | Dễ | Chụp một tô bún hoặc phở có nước dùng trong nhìn thấy đáy. Nước trong là dấu hiệu ninh xương đúng cách. |
| Chữ trên tường | Khó | Tìm một tấm hoành phi, câu đối hay bia đá còn chữ Hán Nôm. |
| Gốm men lam | Vừa | Chụp một món gốm hoa lam trắng xanh — dòng gốm Chu Đậu / Bát Tràng cổ. |
| Vật bằng tre | Dễ | Chụp một vật dụng làm từ tre còn đang được dùng, không phải đồ trang trí. |

Độ khó quyết định điểm. Bộ đầy đủ cần khoảng **30–40 nhiệm vụ**, viết tay, hai ngôn ngữ.

### Luồng chấm

```
Chọn nhiệm vụ
   → nút Chụp mở Lens ở chế độ quest (mang theo questId)
   → chụp
   → Gemini vision chấm, schema ràng buộc
   → màn kết quả
   → đạt: huy hiệu, gắn vào tỉnh nơi chụp
     chưa đạt: giải thích + gợi ý, mời thử lại
```

Gemini trả về bốn thứ:

- **đạt / chưa đạt** + độ tin cậy
- **nó nhìn thấy gì** trong ảnh
- **vì sao điều đó có ý nghĩa** — phần kiến thức
- **gợi ý tìm ở đâu**, khi chưa đạt

### Nhánh trượt mới là chỗ hay nhất

Chụp nhầm rồi nhận lại:

> "Đây là mái chồng diêm hai tầng, không phải đầu đao. Đầu đao là góc mái cong hất lên
> ở bốn góc — thử tìm ở đình làng thay vì chùa."

Người dùng vừa học được một thứ vừa muốn thử lại. App tra cứu thuần không làm được điều
này vì nó chỉ trả lời khi được hỏi.

### Chấm nới tay có chủ ý

- Đạt khi độ tin cậy **≥ 0.6**, không phải ngưỡng chặt
- Luôn có nút **"tôi vẫn nghĩ là đúng"** để người dùng tự ghi nhận

Vision model chấm sai là chuyện chắc chắn xảy ra. Một nhiệm vụ chấm sai giữa lúc demo
mà không có đường thoát thì rất khó xử.

### Nguồn nhiệm vụ — hai nguồn, cả hai đều cần

- **Bộ cứng đóng gói trong assets** (~30–40 cái, viết tay, phân theo vùng và độ khó).
  Đi theo đúng đường `BundledAssets` mà `provinces.json` đang dùng. Có nó thì demo
  không phụ thuộc mạng, và tránh được cảnh Gemini sinh ra "chụp rồng đá thời Lý" cho
  người đang đứng ở Cà Mau.
- **Gemini sinh thêm** theo tỉnh hiện tại và lịch sử chụp, để bộ nhiệm vụ không cạn.

### Gắn với Passport

Huy hiệu gắn vào **tỉnh nơi chụp** — `provinceId` đã có sẵn trong mỗi discovery, không
cần thêm gì để biết chỗ. Passport có thêm một tầng ý nghĩa: không chỉ "tôi đã đến 7
tỉnh" mà "tôi săn được 3 huy hiệu ở Huế".

Ảnh chụp qua nhiệm vụ vẫn vào journal và passport như ảnh thường — **không tạo kho dữ
liệu riêng**. Đây là ràng buộc thiết kế, không phải chi tiết triển khai: một silo dữ
liệu song song sẽ làm hỏng cả journal lẫn passport.

---

## Phải làm những gì

### Domain

- `Quest` — id, title, description, difficulty, category, provinceScope (nullable),
  source (bundled | generated)
- `QuestAttempt` — questId, discoveryId, passed, verdict, at
- `QuestVerdict` — passed, confidence, whatISee, whyItMatters, hint
- Use case: quan sát danh sách nhiệm vụ, chấm một lần chụp, sinh nhiệm vụ mới

### Data

- Entity + DAO cho `quest` và `quest_attempt`, migration Room
- Asset nhiệm vụ đóng gói + loader (theo mẫu `ProvinceAssetSource`)
- Prompt + schema **chấm ảnh** trong `GeminiPrompts.kt` / `GeminiSchemas.kt`
- Prompt + schema **sinh nhiệm vụ**
- `QuestRepositoryImpl`

### UI

- Màn danh sách nhiệm vụ (thay hoặc gắn vào Explore)
- Màn kết quả chấm
- Màn bộ sưu tập huy hiệu
- **Sửa `LensScreen.kt`** — nhận `questId`, sau khi chụp rẽ sang màn chấm thay vì màn
  Discovery. **Đây là chỗ tốn công nhất và dễ làm vỡ thứ đang chạy ổn nhất.**
- Route mới trong `Destinations.kt`
- Chuỗi string tiếng Việt + tiếng Anh

### Nội dung

30–40 nhiệm vụ viết tay, hai ngôn ngữ. Không khó nhưng ngốn thời gian thật, và **chất
lượng nội dung quyết định phần lớn ấn tượng** — một nhiệm vụ viết hay dạy được người ta
một điều họ chưa biết; một nhiệm vụ viết dở chỉ là việc vặt.

---

## Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Sửa luồng camera làm vỡ thứ đang chạy ổn | Cao | Tách nhánh quest ra khỏi nhánh thường ngay từ điểm sau capture, không đan xen |
| Gemini chấm sai — trượt cái đúng | Vừa | Ngưỡng 0.6 + nút tự ghi nhận |
| Gemini chấm sai — cho qua cái sai | Thấp | Chấp nhận được, thà rộng còn hơn chặn người dùng |
| Nhiệm vụ không làm được ở nơi demo | Vừa | Luôn có vài nhiệm vụ độ khó Dễ làm được ở bất cứ đâu |
| Gemini sinh nhiệm vụ bất khả thi | Vừa | Bộ cứng làm nền, phần sinh chỉ là thêm |
| Nội dung viết tay không kịp | Vừa | Bắt đầu bằng 10 nhiệm vụ chất lượng cao còn hơn 40 cái nhạt |

---

## Khi nào nên quay lại

Xét lại phương án này khi bộ sưu tập đã ổn định và còn đủ thời gian cho cả phần sửa
luồng camera lẫn phần viết nội dung.

Nếu quay lại thì **gắn nhiệm vụ vào chính bảng sưu tập**, đừng dựng màn hình rời: mỗi
nhiệm vụ trỏ tới một mục trong `catalog.json`, hoàn thành thì mục đó mở khoá. Bảng sưu
tập đã có sẵn danh mục, dấu hiệu nhận biết và tiến độ — quest chỉ thêm phần thử thách
và chấm ảnh lên trên, không phải dựng lại từ đầu. Không dùng bản đồ tỉnh.

Còn nếu định dùng quest để **lấp tab Explore**, thì phải là phần "hôm nay đi tìm gì" —
tức là nhiệm vụ được giao, chứ không phải bảng huy hiệu đã đạt. Bảng huy hiệu thuộc về
bộ sưu tập trong Nhật ký; tab Explore chỉ giữ phần nhìn về phía trước.

Nếu thời gian chỉ đủ một nửa, phần đáng làm nhất là **luồng chấm ảnh** với bộ nhiệm vụ
cứng, bỏ phần Gemini sinh nhiệm vụ. Phần sinh là tiện lợi; phần chấm mới là thứ gây ấn
tượng.
