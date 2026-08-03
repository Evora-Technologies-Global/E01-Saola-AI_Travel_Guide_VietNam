# AI Travel Companion - Project Plan

## 1. Project Overview

**AI Travel Companion** là một ứng dụng Android sử dụng Google Gemini để mang đến trải nghiệm du lịch thông minh và tương tác theo thời gian thực. Thay vì phải tìm kiếm thông tin trên nhiều ứng dụng khác nhau, người dùng chỉ cần mở camera và hướng vào một địa điểm, món ăn, công trình kiến trúc hoặc hiện vật để AI nhận diện, giải thích và trò chuyện như một hướng dẫn viên du lịch cá nhân.

Ứng dụng hướng đến việc giúp khách du lịch trong và ngoài nước khám phá văn hóa, lịch sử và ẩm thực Việt Nam theo cách trực quan, tự nhiên và cá nhân hóa.

---

# 2. Objectives

## Primary Goal

Xây dựng một AI Travel Assistant có khả năng:

* Nhận diện địa điểm, món ăn và công trình thông qua camera.
* Giải thích lịch sử và ý nghĩa văn hóa bằng ngôn ngữ tự nhiên.
* Trả lời câu hỏi theo ngữ cảnh của cuộc hội thoại.
* Dịch biển báo, thực đơn hoặc nội dung hiển thị trong thời gian thực.
* Đề xuất các địa điểm liên quan dựa trên vị trí và lịch sử khám phá.

## Secondary Goals

* Tạo trải nghiệm hướng dẫn viên du lịch AI hoàn toàn trên thiết bị di động.
* Xây dựng kiến trúc Android hiện đại, dễ mở rộng và bảo trì.
* Tận dụng tối đa khả năng của Google Gemini Vision và Gemini Live API.

---

# 3. Target Users

## Primary Users

* Khách du lịch quốc tế.
* Khách du lịch trong nước.
* Người yêu thích khám phá văn hóa và lịch sử.

## Secondary Users

* Học sinh, sinh viên.
* Bảo tàng.
* Trung tâm văn hóa.
* Công ty du lịch.
* Hướng dẫn viên.

---

# 4. Core Features

## AI Landmark Recognition

* Nhận diện địa danh nổi tiếng.
* Giới thiệu lịch sử.
* Ý nghĩa văn hóa.
* Kiến trúc.
* Các sự kiện liên quan.

---

## AI Food Explorer

* Nhận diện món ăn.
* Giới thiệu nguồn gốc.
* Thành phần.
* Cách thưởng thức.
* Đặc trưng vùng miền.

---

## AI Cultural Storytelling

Gemini tạo các câu chuyện sinh động về:

* Nhân vật lịch sử.
* Truyền thuyết.
* Văn hóa.
* Phong tục.

Thay vì chỉ hiển thị thông tin khô khan.

---

## AI Conversation

Người dùng có thể hỏi tiếp:

* Why is this place famous?
* Who built this?
* Tell me more.
* Is there another place nearby?

Gemini duy trì ngữ cảnh xuyên suốt cuộc hội thoại.

---

## Smart Recommendation

Đề xuất:

* Địa điểm gần đó.
* Nhà hàng.
* Bảo tàng.
* Quán cà phê.
* Lịch trình tiếp theo.

Dựa trên:

* Lịch sử khám phá.
* Sở thích.
* Vị trí hiện tại.

---

## AI Translation

Dịch:

* Menu.
* Biển báo.
* Tờ rơi.
* Bảng hướng dẫn.

---

## Travel Journal

Lưu lại:

* Các địa điểm đã ghé.
* Hình ảnh.
* Ghi chú.
* AI Summary sau mỗi chuyến đi.

---

# 5. Technical Architecture

## Architecture Pattern

* Clean Architecture
* MVI (Model - View - Intent)
* Repository Pattern
* Single Source of Truth (Room)
* Offline First Architecture

---

## Module Structure

```
app
│
├── core
│   ├── ui
│   ├── designsystem
│   ├── common
│   ├── network
│   ├── database
│   ├── datastore
│   ├── model
│   └── ai
│
├── domain
│   ├── repository
│   ├── usecase
│   └── model
│
├── data
│   ├── remote
│   ├── local
│   ├── mapper
│   └── repository
│
├── feature
│   ├── camera
│   ├── landmark
│   ├── chat
│   ├── translation
│   ├── recommendation
│   ├── journal
│   └── settings
│
└── app
```

---

# 6. Tech Stack

## UI

* Jetpack Compose
* Material Design 3
* Navigation Compose
* Adaptive Layout
* Dynamic Color
* Coil (Image Loading)

---

## Architecture

* Clean Architecture
* MVI
* Repository Pattern
* UseCase Pattern
* StateFlow
* SharedFlow
* Kotlin Coroutines
* Flow

---

## Dependency Injection

* Hilt

---

## Local Storage

* Room Database
* DataStore Preferences
* DataStore Proto (optional)

---

## Networking

* Ktor Client *(hoặc Retrofit + OkHttp nếu muốn đơn giản hơn)*
* Kotlin Serialization

---

## Camera

* CameraX

Bao gồm:

* Preview
* Image Capture
* Image Analysis

---

## Google AI

### Google AI Studio

* Gemini API

### Gemini Vision

* Landmark Recognition
* Food Recognition
* OCR
* Scene Understanding

### Gemini Live API

* Voice Conversation
* Real-time Interaction

### Structured Output

* JSON Response
* Function Calling

Ví dụ:

```
Gemini

↓

Return JSON

↓

Landmark

↓

Description

↓

Nearby Places
```

Giúp client xử lý dữ liệu ổn định thay vì parse text.

---

## Maps

* Google Maps SDK
* Google Places API *(nếu cần mở rộng)*

---

## Voice

* Android TextToSpeech

> Dictation (`Android SpeechRecognizer`) đã viết xong cả hai nền tảng nhưng không có nút nào
> gọi tới, nên **đã xoá ngày 02.08.2026** cùng quyền micro và speech recognition. Chỉ còn
> phần đọc — câu trả lời và bản dịch được đọc lên; câu hỏi thì gõ.

*(Có thể thay thế bằng Gemini Live nếu muốn trải nghiệm hội thoại tự nhiên hơn.)*

---

## Image

* Coil
* CameraX ImageAnalysis

---

## Logging

* Timber

---

## Testing

* JUnit5
* MockK
* Turbine
* Compose UI Test

---

# 7. Data Flow

```
Camera

↓

CameraX

↓

Capture Image

↓

Gemini Vision API

↓

Structured JSON

↓

Repository

↓

Room Database

↓

UseCase

↓

ViewModel (MVI)

↓

Compose UI
```

---

# 8. AI Workflow

## Landmark Recognition

```
Capture

↓

Gemini Vision

↓

Recognize Landmark

↓

Generate Summary

↓

Store History

↓

Show UI
```

---

## AI Conversation

```
Question

↓

Gemini Live

↓

Context Memory

↓

AI Response

↓

Voice + Text
```

---

## Recommendation

```
Current Location

+

Visited Places

+

Gemini

↓

Recommended Places
```

---

# 9. Local Database

## Tables

### TravelHistory

* id
* locationName
* imageUri
* latitude
* longitude
* summary
* visitedAt

### Conversation

* id
* landmarkId
* role
* message
* createdAt

### FavoritePlace

* id
* placeName
* note
* createdAt

---

# 10. MVP Scope

## Phase 1

* Camera Preview
* Landmark Recognition
* AI Description
* Chat with AI
* Room History

## Phase 2

* Voice Conversation
* Translation
* Recommendation
* Google Maps

## Phase 3

* Travel Journal
* Offline Cache
* AI Daily Summary
* Personalized Suggestions

---

# 11. Expected Outcome

Sau khi hoàn thành MVP, người dùng chỉ cần mở ứng dụng, hướng camera vào bất kỳ địa điểm, món ăn hoặc công trình nào để AI nhận diện và đóng vai trò như một hướng dẫn viên du lịch cá nhân. AI không chỉ cung cấp thông tin mà còn duy trì hội thoại theo ngữ cảnh, hỗ trợ dịch thuật, gợi ý địa điểm tiếp theo và lưu lại toàn bộ hành trình khám phá.

Mục tiêu cuối cùng là xây dựng một trải nghiệm du lịch thông minh, cá nhân hóa và dễ tiếp cận, giúp việc khám phá văn hóa Việt Nam trở nên hấp dẫn hơn đối với cả khách du lịch trong nước và quốc tế.
