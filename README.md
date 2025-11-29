# Fi Thnity – Monorepo

This repository includes both your **Node.js backend** and your **modern Android app** for the Fi Thnity (On My Way) project.

---

## 📁 Structure

```
Fi thnity/
├── backend/    # Node.js Express + MongoDB backend API
├── fithnity/   # Android app (Jetpack Compose + Firebase Auth)
```

---

## 🚦 Quick Start

### 1. Clone the repository
```bash
git clone <your-repo-url>
cd Fi\ thnity
```

### 2. Initial Setup
- **backend/**: See [`backend/README.md`](backend/README.md) for all API and admin setup details.
- **fithnity/**: See [`fithnity/README.md`](fithnity/README.md) for Android Studio setup (Kotlin, Compose) and Firebase integration.

### 3. Install Prerequisites
- Node.js v18+, npm/yarn
- MongoDB (Atlas or local)
- JDK 11+, Android Studio Hedgehog+
- (Optional: MapTiler account for enhanced maps on Android)

---

## 🔑 Sensitive Files & Credentials

Sensitive or local config **will NOT be committed** (see .gitignore):
- backend/firebase-service-account.json (get your own from Firebase console)
- fithnity/app/google-services.json (get this from your Firebase project, package name: `tn.esprit.fithnity`)
- backend/.env (copy `.env.example` and fill it using values from README + Firebase/Atlas)

**Team members:** Ask your tech lead for latest `.env` and credentials files when onboarding the project!

---

## 📦 Git Ignore Summary
- Only the `backend/` and `fithnity/` folders are tracked by default
- All Markdown files except `README.md` in those folders are ignored during pushes (to keep documentation local if sensitive)
- All sensitive Firebase configs, Node, Android Studio, and build outputs are ignored

---

## 🚀 Development Workflow
- PRs should update respective READMEs or reference them for clarity
- Make sure your local .env and credential files are up to date before running back or front end
- Start backend first (API/mongo must be up), then the Android app

---

## 🤝 Contributions & Issues
- This is a student project at ESPRIT Engineers School
- For bugs or questions, open issues or reach out on your team channel

---

**Built with ❤️ for Tunisia 🇹🇳 – Save Time, Save Tunisia!**








