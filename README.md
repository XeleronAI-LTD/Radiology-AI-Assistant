# 🩻 Radiology AI Assistant

> AI-assisted medical imaging analysis platform — built during an AI Engineering internship at **XeleronAI Ltd** (London, remote).

Radiology AI Assistant is a web platform for analyzing medical imaging exams (CT, DICOM series) with AI. It covers two kinds of analysis:

- **Detection** — lung nodules on chest CT, returned as **bounding boxes** with a confidence score
- **Segmentation** — spleen on abdominal CT, returned as per-slice **masks**

The platform follows one guiding principle: **AI proposes, the clinician validates.** Every AI result is created as *pending* (`EN_ATTENTE`), and only a radiologist can accept or reject it. Nothing is auto-accepted.

---

## 🏗️ Architecture

The platform is made of three independent services:

| Service | Role |
|---|---|
| **`medical-imaging-backend`** | Spring Boot REST API: JWT authentication and roles, PostgreSQL persistence, MinIO object storage, DICOM metadata extraction, asynchronous orchestration of AI analyses, audit log. **It is the only component that accesses MinIO and the database.** |
| **`medical-imaging-ai-service`** | FastAPI / MONAI inference microservice. **Stateless**: it receives the raw DICOM bytes of a series from the backend and returns either bounding boxes (`BOX`) or masks (`MASQUE`). |
| **`medical-imaging-frontend`** | Angular client: exam list and upload, full-screen viewer with AI overlays (boxes and masks), analysis trigger + status polling, clinician validation. |

### Request flow

```
┌────────────┐   REST + JWT    ┌──────────────────┐   JPA / S3 API   ┌──────────────────────┐
│  Frontend  │ ──────────────▶ │     Backend      │ ───────────────▶ │ PostgreSQL  │  MinIO │
│  (Angular) │ ◀────────────── │  (Spring Boot)   │ ◀─────────────── │ (metadata)  │ (files)│
└────────────┘  202 + polling  └──────────────────┘                  └──────────────────────┘
                                   │          ▲
               async HTTP          │          │  JSON response
               POST /predict       ▼          │  (BOX | MASQUE)
               (raw DICOM)   ┌──────────────────┐
                             │    AI service    │
                             │ (FastAPI + MONAI)│
                             └──────────────────┘
```

1. The frontend asks the backend to analyze an exam → the backend answers **202 Accepted** right away.
2. In the background, the backend reads the DICOM series from MinIO and sends it to the AI service (`POST /predict`).
3. The AI service returns boxes or masks; the backend stores them (masks go to MinIO, metadata to PostgreSQL) as `EN_ATTENTE`.
4. The frontend polls the analysis status, displays the results as overlays, and a radiologist accepts or rejects each one.

---

## 📁 Repository structure

```
Radiology-AI-Assistant/
├── medical-imaging-backend/      # Spring Boot API — auth, storage, DICOM, async AI orchestration, validation, audit
├── medical-imaging-ai-service/   # FastAPI + MONAI inference service — lung nodule detection & spleen segmentation
└── medical-imaging-frontend/     # Angular web client — viewer, AI overlays, analysis trigger, clinician validation
```

- [`medical-imaging-backend/`](./medical-imaging-backend/) — REST API and single point of access to data and storage. See its [README](./medical-imaging-backend/README.md).
- [`medical-imaging-ai-service/`](./medical-imaging-ai-service/) — stateless inference microservice built on MONAI Model Zoo bundles. See its [README](./medical-imaging-ai-service/README.md).
- [`medical-imaging-frontend/`](./medical-imaging-frontend/) — web client used by radiologists and technicians. See its [README](./medical-imaging-frontend/README.md).

---

## 🚀 Getting started

Start the services in this order. Each step is detailed in the linked README — this section only gives the big picture.

| # | Step | Default address | Details |
|---|---|---|---|
| 1 | Start **PostgreSQL** and **MinIO** with Docker | `localhost:5432`, `localhost:9000` (console `:9001`) | [backend README](./medical-imaging-backend/README.md) |
| 2 | Configure and start the **backend** (`./mvnw spring-boot:run`) | `http://localhost:8080` | [backend README](./medical-imaging-backend/README.md) |
| 3 | Install the **AI service**, download the MONAI bundles (`lung_nodule_ct_detection`, `spleen_ct_segmentation`), then start it (`uvicorn`) | `http://localhost:8000` | [AI service README](./medical-imaging-ai-service/README.md) |
| 4 | Install and start the **frontend** (`npm install && ng serve`) | `http://localhost:4200` | [frontend README](./medical-imaging-frontend/README.md) |

**Prerequisites:** Docker, Java 21, Python 3.11+, Node.js 20+ and the Angular CLI. A GPU is strongly recommended for the AI service (see [Status & limitations](#-status--limitations)).

Wiring between services (defaults):

- Backend → AI service: `AI_DETECTION_BASE_URL` (default `http://localhost:8000`)
- Backend → MinIO: `MINIO_ENDPOINT` (default `http://localhost:9000`)
- Frontend → backend: `apiUrl` in `medical-imaging-frontend/src/environments/` (default `http://localhost:8080/api`)
- Backend CORS: `APP_CORS_ALLOWED_ORIGINS` (default `http://localhost:4200`)

---

## 🧰 Tech stack

- **Backend:** Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA, Flyway, dcm4che
- **Frontend:** Angular 21, TypeScript, RxJS, Tailwind CSS
- **AI service:** Python, FastAPI, MONAI, PyTorch
- **Data & storage:** PostgreSQL, MinIO
- **Tooling:** Docker

---

## 📌 Status & limitations

**Sprint 2 delivered:**

- [x] Asynchronous AI pipeline — lung nodule **detection** and spleen **segmentation** (pre-trained MONAI models)
- [x] Overlays in the viewer (bounding boxes and masks)
- [x] Clinician validation module (accept / reject, with traceability) and consolidated audit log
- [ ] Automated structured radiology report generation (next step)

**Known limitations:**

- **Inference latency depends heavily on the hardware.** On CPU, detection on a ~150-slice series can take around 10 minutes; a GPU is needed for realistic use.
- **Single-series only.** An analysis works on one series at a time; multi-series / multi-modality studies (e.g. PET-CT) are not supported yet.

---

## ⚠️ Medical disclaimer

This is a **research and educational project**. It is **not a certified medical device** and must not be used for diagnosis or clinical decision-making. Every AI result requires validation by a qualified clinician.

---

👤 **Ahmed Amine Faiz** — AI Engineering student @ ENIAD Berkane · [GitHub](https://github.com/ahmedaminefaiz)
