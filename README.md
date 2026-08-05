# Saola 🇻🇳

> Hướng máy ảnh vào một mái đình, một tô bún hay một tấm biển — ứng dụng nhận ra nó, kể
> câu chuyện đằng sau, và ghi lại vào hành trình của bạn.

🇬🇧 [English version](README-en.md) · 🛠 [Chi tiết kỹ thuật](TechStack-temp.md)

---

## 1. Ứng dụng này là gì

Saola là ứng dụng du lịch dùng **Google Gemini** để biến chiếc máy ảnh trên điện thoại
thành một hướng dẫn viên bản địa.

Thay vì phải tra Google, đọc Wikipedia rồi mở thêm app dịch, người dùng chỉ cần giơ máy lên.
AI nhận diện di tích, món ăn hay hiện vật trong khung hình, giải thích lịch sử và ý nghĩa văn
hoá, dịch thực đơn và biển báo, rồi trả lời tiếp những câu hỏi phát sinh — tất cả trong một
màn hình.

Điểm khác biệt so với các app du lịch thông thường: **mọi thứ bạn chụp đều được giữ lại**.
Ảnh của bạn tô dần tấm bản đồ 34 tỉnh thành, mở dần bộ sưu tập 61 nét văn hoá Việt, và mỗi
ngày được AI viết lại thành một trang nhật ký.

Ứng dụng chạy trên **Android và iOS** từ một mã nguồn chung, hỗ trợ **8 ngôn ngữ**, và toàn
bộ dữ liệu nằm trên máy người dùng — không tài khoản, không máy chủ.

---

## 2. Tính năng

### Cốt lõi — nhận diện và trò chuyện

| Tính năng | Mô tả |
|---|---|
| **5 chế độ ống kính** | Tự động · Di tích · Món ăn · Bảo tàng · Dịch |
| **Nhận diện có cấu trúc** | Trả về tên, tên địa phương, tóm tắt, các mục nội dung, tags, độ khớp — không phải một đoạn văn thô |
| **Trò chuyện theo ngữ cảnh** | Hỏi tiếp về đúng nơi vừa chụp; lịch sử hội thoại được lưu lại |
| **Đọc to** | Đọc câu trả lời và bản dịch bằng giọng theo ngôn ngữ máy |
| **Dịch thực đơn & biển báo** | OCR trên máy + dịch, phủ bản dịch đúng vị trí từng dòng chữ |

### Điểm đặc biệt — thứ giữ chân người dùng

**🗺 Hộ chiếu du lịch.** Bản đồ Việt Nam vẽ theo **34 tỉnh thành sau sáp nhập 2025**, mỗi
tỉnh được tô bằng chính bức ảnh bạn chụp ở đó, cắt theo đúng đường biên thật. **Không có nút
check-in** — ảnh đã mang sẵn toạ độ, nên chụp một ngôi chùa *chính là* check-in. Có đầy đủ
**Hoàng Sa và Trường Sa** trong khung phụ, theo đúng quy ước bản đồ Việt Nam.

**🎴 Bộ sưu tập văn hoá.** 61 thứ đáng tìm ở Việt Nam — phở, bánh chưng, đầu đao mái đình,
thuyền thúng, bia đá đội rùa, cồng chiêng. Mỗi ô là một ô gạch trống cho tới khi bạn chụp
được thứ đó, rồi nó hiện lên bằng **ảnh của chính bạn**. Bảng này đầy đủ ngay từ lần mở đầu
tiên, và tính ngược lại cả những ảnh đã chụp từ trước.

**🧭 Khám phá.** Bản đồ trực tiếp quanh vị trí hiện tại với những nơi đáng đi bộ tới trong
**5 km**. Dữ liệu lấy từ **OpenStreetMap + Wikipedia + Wikimedia Commons** — không cần API
key, không tính phí. Xếp hạng theo lượt đọc Wikipedia 60 ngày gần nhất và mức độ chi tiết của
dữ liệu bản đồ. **Không có sao đánh giá**, vì không nguồn mở nào có — thà thiếu còn hơn bịa.

**📖 Nhật ký hành trình.** Gom theo ngày, mỗi ngày có một đoạn tổng kết do AI viết cùng gợi ý
cho hôm sau.

**🌏 8 ngôn ngữ.** Việt · Anh · Nhật · Hàn · Trung · Pháp · Tây Ban Nha · Thái. Giao diện,
câu trả lời của AI và giọng đọc đều **đi theo ngôn ngữ của máy** — điện thoại tiếng Nhật thì
nhận được màn hình, câu trả lời và giọng đọc tiếng Nhật.

**📱 Hai bố cục.** Điện thoại dùng thanh tab dưới; máy tính bảng và cửa sổ rộng dùng thanh
điều hướng dọc và bố cục hai khung — đọc bài viết bên trái, hỏi hướng dẫn viên bên phải.

---

## 3. Ảnh chụp màn hình

Chụp trên máy ảo Pixel 7 Pro, Pixel Tablet, iPhone 17 và iPad Pro 11". Dữ liệu là bộ demo 24
điểm từ Sa Pa tới Cần Thơ (xem [§10 TechStack](TechStack-temp.md#10-demoing-it-and-the-seeded-data));
ảnh trong các màn hình là ảnh thật lấy từ Wikimedia Commons.

### Android — điện thoại

| Ống kính | Nhật ký | Hộ chiếu du lịch | Bộ sưu tập văn hoá |
|---|---|---|---|
| <img src="screenshots/android-phone/01-lens.png" width="180" alt="Ống kính"> | <img src="screenshots/android-phone/02-journal.png" width="180" alt="Nhật ký"> | <img src="screenshots/android-phone/03-passport.png" width="180" alt="Hộ chiếu"> | <img src="screenshots/android-phone/04-collection.png" width="180" alt="Bộ sưu tập"> |

| Chi tiết khám phá | Trò chuyện | Khám phá quanh đây |
|---|---|---|
| <img src="screenshots/android-phone/05-discovery.png" width="180" alt="Chi tiết"> | <img src="screenshots/android-phone/06-chat.png" width="180" alt="Trò chuyện"> | <img src="screenshots/android-phone/07-explore.png" width="180" alt="Khám phá"> |

| Chi tiết địa điểm | Dịch thực đơn | Cài đặt |
|---|---|---|
| <img src="screenshots/android-phone/08-explore-detail.png" width="180" alt="Địa điểm"> | <img src="screenshots/android-phone/09-translate.png" width="180" alt="Dịch"> | <img src="screenshots/android-phone/10-settings.png" width="180" alt="Cài đặt"> |

### Android — máy tính bảng

Bố cục rộng: thanh điều hướng dọc bên trái, hai khung nội dung song song.

| Ống kính | Nhật ký + Hộ chiếu | Bộ sưu tập |
|---|---|---|
| <img src="screenshots/android-tablet/01-lens.png" width="250" alt="Ống kính"> | <img src="screenshots/android-tablet/02-journal.png" width="250" alt="Nhật ký"> | <img src="screenshots/android-tablet/03-collection.png" width="250" alt="Bộ sưu tập"> |

| Bài viết + Hỏi hướng dẫn viên | Khám phá | Cài đặt |
|---|---|---|
| <img src="screenshots/android-tablet/04-discovery.png" width="250" alt="Chi tiết"> | <img src="screenshots/android-tablet/05-explore.png" width="250" alt="Khám phá"> | <img src="screenshots/android-tablet/06-settings.png" width="250" alt="Cài đặt"> |

### iOS — iPhone

| Nhật ký | Hộ chiếu | Bộ sưu tập | Chi tiết khám phá |
|---|---|---|---|
| <img src="screenshots/ios-phone/01-journal.png" width="180" alt="Nhật ký"> | <img src="screenshots/ios-phone/02-passport.png" width="180" alt="Hộ chiếu"> | <img src="screenshots/ios-phone/03-collection.png" width="180" alt="Bộ sưu tập"> | <img src="screenshots/ios-phone/04-discovery.png" width="180" alt="Chi tiết"> |

| Trò chuyện | Khám phá (MapKit) | Cài đặt |
|---|---|---|
| <img src="screenshots/ios-phone/05-chat.png" width="180" alt="Trò chuyện"> | <img src="screenshots/ios-phone/06-explore.png" width="180" alt="Khám phá"> | <img src="screenshots/ios-phone/07-settings.png" width="180" alt="Cài đặt"> |

> Màn Ống kính không có trong bộ iOS vì máy ảo iOS không có camera — khung ngắm sẽ trắng.

### iOS — iPad

| Nhật ký + Hộ chiếu | Bộ sưu tập | Bài viết + Hỏi hướng dẫn viên |
|---|---|---|
| <img src="screenshots/ios-ipad/01-journal.png" width="250" alt="Nhật ký"> | <img src="screenshots/ios-ipad/02-collection.png" width="250" alt="Bộ sưu tập"> | <img src="screenshots/ios-ipad/03-discovery.png" width="250" alt="Chi tiết"> |

| Khám phá | Cài đặt |
|---|---|
| <img src="screenshots/ios-ipad/04-explore.png" width="250" alt="Khám phá"> | <img src="screenshots/ios-ipad/05-settings.png" width="250" alt="Cài đặt"> |

> Ảnh Khám phá trên iPad chỉ có 12 quán ăn và không có di tích: Overpass (máy chủ OpenStreetMap)
> giới hạn số truy vấn theo IP, và sau một ngày chụp ảnh liên tục thì nhánh truy vấn di tích bị
> từ chối. Đây là hành vi thật của ứng dụng khi bị giới hạn, không phải lỗi bố cục.

---

## 4. Công nghệ

Một mã nguồn Kotlin Multiplatform chạy cả hai nền tảng; **toàn bộ màn hình, ViewModel và hệ
thiết kế viết một lần** bằng Compose Multiplatform. Chỉ camera, OCR, bản đồ, định vị và giọng
đọc là viết riêng cho từng nền tảng.

| Nhóm | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ | Kotlin Multiplatform | 2.3.21 |
| Build | AGP · KSP · JDK | 9.2.1 · 2.3.10 · 17+ |
| Giao diện | Compose Multiplatform | 1.12.0-beta03 |
| Điều hướng | Navigation Compose (JetBrains) | 2.9.2 |
| Vòng đời / ViewModel | Lifecycle (JetBrains) | 2.11.0 |
| Kiến trúc | Clean Architecture + MVI (`MviViewModel<S, I, E>`) | — |
| Dependency Injection | Koin | 4.2.2 |
| Cơ sở dữ liệu | Room (multiplatform) + SQLite bundled | 2.8.4 · 2.7.0 |
| Lưu tuỳ chọn | DataStore Preferences | 1.2.1 |
| Mạng | Ktor Client (OkHttp trên Android, Darwin trên iOS) | 3.5.1 |
| Chuyển đổi JSON | kotlinx.serialization | 1.11.0 |
| Bất đồng bộ | kotlinx.coroutines | 1.11.0 |
| Ngày giờ | kotlinx-datetime | 0.8.0 |
| Ảnh | Coil 3 | 3.4.0 |
| Camera | CameraX (Android) · AVFoundation (iOS) | 1.6.1 · nền tảng |
| OCR | ML Kit — Latin, Trung, Nhật, Hàn (Android) · Vision (iOS) | 16.0.1 · nền tảng |
| Bản đồ | Maps Compose (Android) · MapKit (iOS) | 8.3.1 · nền tảng |
| Định vị | Play Services Location · CoreLocation | 21.4.0 · nền tảng |
| Giọng đọc | TextToSpeech · AVSpeechSynthesizer | nền tảng |
| Ghi log | Kermit (logcat / os_log) | 2.1.0 |
| **AI** | **Google Gemini 3** qua Google AI Studio REST API | 3.5-flash → 3.1-flash-lite → 3-pro-preview |
| Kiểm thử | JUnit4 · MockK · Turbine · Ktor MockEngine · Koin test | 4.13.2 · 1.14.11 · 1.2.1 |

**Nền tảng tối thiểu:** Android 8.0 (API 26) trở lên · iOS 16.0 trở lên.

**Cấu trúc 4 module**, phụ thuộc một chiều vào trong:

```
:app  →  :shared  →  :domain  ←  :data
 host    toàn bộ      thuần        Room, Ktor,
Android  giao diện    Kotlin       DataStore
```

**Hai quyết định kỹ thuật đáng nói với người ngoài:**

- **Gemini trả về JSON theo schema định sẵn**, không phải văn xuôi. Nhờ vậy màn hình kết quả
  luôn có đủ trường, không bao giờ phải "đoán" xem AI viết gì.
- **Chuỗi model dự phòng.** Gemini Flash hay trả `503` vào giờ cao điểm. Nếu để lỗi thì người
  dùng đang đứng trước ngôi chùa sẽ nhận được một thông báo lỗi — nên yêu cầu tự động chuyển
  sang model tiếp theo, chỉ bỏ cuộc khi cả chuỗi đều bận.

Chi tiết đầy đủ (hình học bản đồ 34 tỉnh, truy vấn Overpass, R8/AGP, ký APK) nằm ở
[TechStack-temp.md](TechStack-temp.md).

---

## 5. Dữ liệu được lưu ở đâu

**Toàn bộ dữ liệu nằm trên máy người dùng. Không tài khoản, không máy chủ, không analytics.**

| Loại dữ liệu | Lưu ở đâu | Ghi chú |
|---|---|---|
| Khám phá, hội thoại, ghi chú, bản dịch, tổng kết ngày | **Room (SQLite)** — 5 bảng | Nguồn sự thật duy nhất; màn hình đọc qua `Flow`, nên hoạt động cả khi offline |
| Ảnh chụp | **Tệp JPEG** trong thư mục riêng của app | Đã xoay đúng chiều theo EXIF, thu về cạnh dài 1024 px |
| Tuỳ chọn (khoá API, model, giao diện, đọc to, định vị) | **DataStore Preferences** | |
| Dữ liệu 34 tỉnh + 61 mục văn hoá | **Tệp đóng gói sẵn trong app** | Chỉ đọc, không cần mạng |
| Dữ liệu địa điểm quanh đây | **Bộ nhớ tạm (RAM)** — 15 phút / 300 m | Cố tình không lưu xuống đĩa: đây là dữ liệu về nơi bạn *đang* đứng, mở lại app mà thấy thành phố tuần trước là sai |

**Cơ sở dữ liệu chỉ lưu *tên tệp ảnh*, không bao giờ lưu đường dẫn.** Trên iOS, thư mục chứa
dữ liệu app bị đổi tên sau mỗi lần cài lại, cập nhật hay khôi phục — đường dẫn lưu hôm qua trỏ
vào một thư mục không còn tồn tại. Lỗi này từng làm mất **toàn bộ ảnh** của người dùng một lần.

**Những gì rời khỏi máy:** ảnh cần nhận diện (gửi tới Gemini), toạ độ hiện tại (gửi tới
OpenStreetMap / Wikipedia), và ảnh bản đồ. Không có gì khác.

**Khoá API Gemini** đọc từ `local.properties` khi build (không vào git), hoặc người dùng tự
dán khoá của mình trong **Cài đặt → Khoá API** — khoá của người dùng được ưu tiên.

---

## 6. Đối tượng người dùng

| Nhóm | Vì sao phù hợp |
|---|---|
| **Khách quốc tế đến Việt Nam** | Rào cản lớn nhất là chữ và ngữ cảnh văn hoá — app xử lý cả hai bằng chính ngôn ngữ của họ |
| **Người Việt đi du lịch trong nước** | Hộ chiếu 34 tỉnh và bộ sưu tập văn hoá biến chuyến đi thành thứ có thể "sưu tầm" |
| **Học sinh, sinh viên** | Tra cứu tại chỗ ở bảo tàng, di tích; nội dung có cấu trúc theo mục thay vì một khối văn bản |
| **Bảo tàng và khu di tích** | Hoạt động như audio guide thông minh mà không cần đầu tư thiết bị |
| **Công ty du lịch** | Bổ trợ cho hướng dẫn viên ở những đoạn khách tự do khám phá |

**Nhóm trọng tâm giai đoạn đầu:** khách quốc tế và người Việt đi du lịch tự túc — hai nhóm
dùng điện thoại làm công cụ chính và không có hướng dẫn viên đi kèm.

---

## 7. Trạng thái hiện tại

| Hạng mục | Trạng thái |
|---|---|
| Chụp ảnh / chọn từ thư viện, 5 chế độ ống kính | ✅ |
| Nhận diện di tích / món ăn / hiện vật, kết quả có cấu trúc | ✅ |
| Trò chuyện theo ngữ cảnh, lưu lịch sử | ✅ |
| Đọc to câu trả lời và bản dịch | ✅ |
| OCR + dịch thực đơn, biển báo (phủ đúng vị trí) | ✅ |
| Nhật ký theo ngày + tổng kết do AI viết | ✅ |
| Hộ chiếu du lịch 34 tỉnh | ✅ |
| Bộ sưu tập văn hoá 61 mục | ✅ |
| Khám phá quanh đây trong 5 km | ✅ |
| Giao diện + giọng đọc 8 ngôn ngữ theo máy | ✅ |
| Đọc offline (Room là nguồn sự thật) | ✅ |
| Bố cục máy tính bảng / cửa sổ rộng | ✅ |
| iOS (Compose Multiplatform + MapKit) | ✅ |
| Nhập liệu bằng giọng nói | ❌ đã gỡ — code hoàn chỉnh nhưng chưa có nút gọi tới |
| Gợi ý điểm đến do AI tự sinh | ❌ đã gỡ — địa chỉ và giá đều bịa; thay bằng **Khám phá** |

**Ba giới hạn cần biết trước khi demo:**

1. **Khả năng tiếp cận (accessibility) chưa làm.** Bản đồ hộ chiếu vẽ bằng `Canvas` trần nên
   trình đọc màn hình bỏ qua hoàn toàn.
2. **Overpass là hạ tầng cộng đồng và có giới hạn theo IP.** Dùng dồn dập sẽ bị từ chối tới
   khi hồi lại.
3. **Phần lớn địa điểm không có ảnh.** OSM không lưu ảnh; chỉ những nơi nổi tiếng mới có bài
   Wikipedia hoặc ảnh trên Commons.

---

## 8. Hướng phát triển

**Ngắn hạn — hoàn thiện thứ đang có**

- Khoảnh khắc "mở khoá" khi một ô trong bộ sưu tập được lật (hiện đang diễn ra âm thầm)
- Biến bộ sưu tập từ *bản ghi* thành *hướng dẫn*: đưa 61 gợi ý nhận biết ra ngoài thay vì giấu
  sau một cú chạm
- Accessibility cho bản đồ hộ chiếu và bản đồ khám phá — bắt buộc trước khi lên store
- Màn hình giấy phép, để ghi nhận OpenStreetMap đúng yêu cầu ODbL
- Kiểm thử cho lớp giao diện của hộ chiếu và hai bản đồ

**Trung hạn — mở rộng năng lực**

- **Gemini Live API** — hội thoại bằng giọng nói theo thời gian thực
- **Chế độ offline** — gói dữ liệu cho một tỉnh, dùng được khi không có mạng
- **Nhập liệu bằng giọng nói** (khôi phục phần đã gỡ, lần này có nút)
- Đọc `name:en` từ OSM cho ~47% địa điểm đã có sẵn tên tiếng Anh
- Gom nhóm ghim (marker clustering) trên bản đồ khám phá

**Dài hạn — sản phẩm**

- **Điều hướng AR** — chỉ đường và chú thích ngay trên khung hình
- **Chế độ gia đình / trẻ em** — nội dung ngắn hơn, giọng kể khác
- **Đóng góp từ cộng đồng** — người dùng bổ sung mục cho bộ sưu tập văn hoá
- Mở rộng bộ sưu tập cho vùng cao, Chăm và đồng bằng sông Cửu Long (hiện còn thiên về Kinh)
- Hợp tác với bảo tàng và khu di tích để bổ sung nội dung được kiểm chứng

---

## 9. Bắt đầu

```bash
# Android
./gradlew :app:installDebug

# iOS
open iosApp/iosApp.xcodeproj     # chạy scheme iosApp

# Kiểm thử (không cần mạng, không cần khoá API)
./gradlew test
```

**Bản debug tự nạp dữ liệu demo.** Mở app lần đầu là đã có sẵn 24 khám phá trải từ Sa Pa tới
Cần Thơ, một cuộc trò chuyện và ba trang nhật ký — đủ để xem ngay hộ chiếu, bộ sưu tập và nhật
ký mà không phải đi chụp. Điều kiện duy nhất là nhật ký đang trống, nên **Cài đặt → Xóa toàn bộ
khám phá** chính là nút đưa bản demo về trạng thái đẹp.

**Bản `release` và `fastRelease` không bao giờ nạp**, và không thể nạp: 3.5 MB ảnh demo chỉ
được đóng gói vào variant `debug`, nên APK phát hành không có gì để nạp từ đó. Muốn xem trạng
thái trống thật thì chạy bản release.

Cần một khoá Gemini từ [Google AI Studio](https://aistudio.google.com/apikey) đặt trong
`local.properties`. Hướng dẫn đầy đủ, kèm khoá Maps SDK cho Android và cách tạo bản release,
xem [TechStack-temp.md §9–§11](TechStack-temp.md#9-getting-started).

---

## 10. AI Riser Vietnam 2026

**Hạng mục:** Du lịch văn hoá & Thể thao · **Nền tảng AI:** Google Gemini, Google AI Studio

> Để mỗi người đi Việt Nam đều có cảm giác đang khám phá cùng một người bản địa hiểu chuyện.

**Ghi nhận nguồn dữ liệu:** ranh giới tỉnh và dữ liệu địa điểm © OpenStreetMap contributors
(ODbL) · đường bờ biển từ Natural Earth (public domain) · nội dung và lượt đọc từ Wikipedia
(CC BY-SA) · ảnh từ Wikimedia Commons theo giấy phép riêng của từng ảnh.
