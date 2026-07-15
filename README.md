<div align="center">

<img src="app/src/main/res/drawable/logo.png" alt="StudyMate AI logo" width="120"/>

# StudyMate AI

**Scan your notes. Let AI build your study material.**

Turn photos of textbooks, handwritten notes and PDFs into summaries, flashcards and practice quizzes — powered by Google Gemini.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)

</div>

---

## ✨ Features

- 📸 **Scan anything** — capture pages with the camera or import images, PDFs, `.docx` and `.txt` files; on-device ML Kit OCR extracts the text
- ✍️ **Edit before generating** — review and clean up the extracted text in a built-in editor
- 🧠 **AI summaries** — concise, well-structured chapter summaries in seconds
- 🃏 **Flashcards** — auto-generated from your material for quick active recall
- 📝 **Practice quizzes** — multiple-choice quizzes with instant scoring and per-question feedback
- 📊 **Quiz history** — every attempt is saved so you can track progress over time
- 📚 **Personal library** — all scanned chapters organised in one place, synced to your account
- 🔑 **Bring your own key** — optionally add a personal Gemini API key for unlimited generations

## 📱 Screenshots

| Home | Scan | Chapter |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/18246d8d-9519-4a0c-bfa6-7f3234b32ba3" alt="Home" width="250"/> | <img src="https://github.com/user-attachments/assets/804f6f3d-9e91-4820-9f83-0000e8baeb23" alt="Scan" width="250"/> | <img src="https://github.com/user-attachments/assets/1f31b94e-1ec2-4288-827a-6ba6fb6b975a" alt="Chapter" width="250"/> |

| Quiz | Library | Edit |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/b540d98c-a37d-4cdb-a64c-74ba1b5fcf27" alt="Quiz" width="250"/> | <img src="https://github.com/user-attachments/assets/6f793d1c-387e-4db2-9407-6f5705392f1d" alt="Library" width="250"/> | <img src="https://github.com/user-attachments/assets/8e170e4b-f880-4047-b91d-ccde9c3c9a9e" alt="Edit" width="250"/> |

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository pattern |
| Dependency injection | Hilt |
| AI | Google Gemini (`generativeai` SDK) |
| OCR | ML Kit Text Recognition (on-device) |
| Camera | CameraX |
| Backend | Firebase Auth + Cloud Firestore |
| Monitoring | Firebase Crashlytics + Analytics |
| Storage | EncryptedSharedPreferences (AES-256) |

## 🚀 Building the project

1. Clone the repo:
   ```bash
   git clone https://github.com/divyang5/StudyMate-AI.git
   ```
2. Add your Firebase config: place your own `google-services.json` in `app/`.
3. Create `local.properties` entries:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   ```
4. Build and run the `debug` variant from Android Studio (installs as a separate `.debug` app).

Release builds additionally read AdMob unit IDs and keystore credentials from `local.properties` / environment variables — see `app/build.gradle.kts`.

## 📄 License

This project is for portfolio and educational purposes. All rights reserved.
