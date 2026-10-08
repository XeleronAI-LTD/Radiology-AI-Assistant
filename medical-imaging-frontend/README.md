# 🩻 Medical Imaging Frontend

> Web client of the **AI-Powered Medical Imaging Analysis & Automated Radiology Reporting Platform** — built during my AI Engineering internship at **XeleronAI Ltd** (London, remote).

![Angular](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?logo=tailwindcss&logoColor=white)
![RxJS](https://img.shields.io/badge/RxJS-B7178C?logo=reactivex&logoColor=white)

Backend API: [medical-imaging-backend](https://github.com/ahmedaminefaiz/medical-imaging-backend)

---

## ✨ Features

- 🔐 **Login with JWT** — `AuthService`, HTTP interceptor, auth & role guards
- 📋 **Exam list** — paginated, with patient **MRN search**
- 📤 **Upload** — standard images or **DICOM**, including a **whole DICOM folder** at once
- 🔎 **Exam detail** — patient / study info and image grid, images loaded as authenticated blobs (with proper object-URL cleanup)
- 🖼️ **Full-screen viewer** with zoom & pan — tested on a real 150-slice CT series
- 🤖 **AI analysis** — *Lancer l'analyse IA* button (radiologist / technician), then asynchronous status polling until `TERMINEE` / `ECHOUEE`
- 🎯 **AI results in the viewer** — bounding boxes **and** segmentation masks overlaid on the image, aligned at any zoom / pan level
- 🔴 **Slices with findings** — a badge on each thumbnail of the image grid that carries at least one AI result
- ✅ **Clinician validation** — *Accepter* / *Rejeter* on each result (radiologists only), with status-based colors and the validator's email

## 🤖 AI workflow (Sprint 2)

All of it lives in the exam detail page (`ExamenDetail`) and the full-screen viewer (`ExamenImageViewer`), backed by `DetectionService` (REST calls) and `DetectionMasqueStore` (mask blobs).

**1. Launching & polling** — the *Lancer l'analyse IA* button is shown to `RADIOLOGUE` and `TECHNICIEN`, and disabled while an analysis is running or once it has finished. After `POST …/detections/analyser`, the page polls `GET …/detections/statut` every 3 s (RxJS `interval` + `switchMap` + `takeWhile`) until the status leaves `EN_COURS`. On `TERMINEE` the detections are reloaded; on `ECHOUEE` the backend's message is displayed. Polling also resumes if the page is opened while an analysis is still running. 409 / 422 responses are shown as readable errors.

**2. Thumbnail badge** — `ExamenImageApercu` shows a red dot on each slice that has at least one detection, so slices with findings stand out in a 150-slice series.

**3. Overlays in the viewer** — the image and its overlays are stacked in the same CSS grid cell, inside the container that carries the zoom/pan `transform`, so they stay aligned at any zoom level:

| Result type | Rendering |
|---|---|
| `BOX` | an SVG whose `viewBox` is the image's natural size (`preserveAspectRatio="none"`), so the boxes use the backend's pixel coordinates directly; label = anomaly + confidence % |
| `MASQUE` | the mask PNG is fetched as an authenticated blob (object URL revoked on slice change / page exit) and used as a CSS `mask-image` (`mask-size: 100% 100%`, luminance mode) on a colored layer at 40 % opacity |

**4. Validation** — each result on the current slice is listed in a side panel. For radiologists (`peutValider`, role `RADIOLOGUE`), *Accepter* / *Rejeter* buttons call `PATCH …/detections/{id}/statut`, and the updated detection immediately replaces the old one in the list. Visual feedback:

- boxes: amber = `EN_ATTENTE`, green = `ACCEPTEE`, red = `REJETEE`
- masks: green when `ACCEPTEE`, red otherwise
- rejected results are struck through; validated ones show *validé par &lt;email&gt;*

## 🗂️ Project structure

```
src/app/
├── features/
│   ├── auth/       # login page, guards, interceptor, AuthService
│   └── examens/    # list, upload, detail + viewer pages, exam/detection services & stores, models
├── layout/         # shared app shell
└── shared/         # shared models
```

Feature-based architecture with standalone components.

## ▶️ Run locally

**Prerequisites:** Node.js 20+, Angular CLI, the [backend](https://github.com/ahmedaminefaiz/medical-imaging-backend) running

```bash
npm install
ng serve          # http://localhost:4200
ng test           # unit tests
ng build          # production build → dist/
```

Set the API URL in `src/environments/`.

---

👤 **Ahmed Amine Faiz** — AI Engineering student @ ENIAD Berkane · [GitHub](https://github.com/ahmedaminefaiz)
