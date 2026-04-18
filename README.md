# 🚀 AI-Ops Dashboard

[English](#english) | [Türkçe](#türkçe)

---

<a name="english"></a>
## 🇬🇧 English

### 🎯 What is it?
The **"AI-Ops Dashboard"** is a high-performance infrastructure monitoring and diagnostic ecosystem. Its primary objective is to solve the "context gap" in server management. While traditional tools notify you that a service is down, this platform leverages Artificial Intelligence to explain **why** it happened. By centralizing telemetry from remote servers (Oracle Cloud, AWS, or local nodes), the dashboard provides a unified view of system health and automated incident response.

### 👤 Target Users
*   **DevOps Engineers & SREs**: Who require rapid Root Cause Analysis (RCA) and remote command execution.
*   **Home-lab Enthusiasts**: Users managing complex local clusters (Docker/K8s) who need an intelligent notification layer.
*   **System Administrators**: Who need to monitor servers behind restrictive firewalls without exposing high-risk ports.

### ✨ Core AI-Ops Features Added
*   **AI Diagnostic Layer**: Integration with Local LLMs (LM Studio/Ollama) and Cloud LLMs (OpenAI/Claude) using a Bring-Your-Own-Key (BYOK) architecture.
*   **Root Cause Analysis (RCA)**: Automated packaging of server metrics (CPU, RAM, Status) to query the AI and display human-readable explanations in a polished modal interface.
*   **Terminal-style AI Chatbot**: A macOS-inspired glassmorphic chat widget at the bottom right, processing natural language queries about system state.
*   **Task Execution Queue & Command Signing**: A new secure polling system where remote agents pull commands. Every command is cryptographically signed via **RSA**. The agent verifies the digital signature before executing any script to prevent malicious takeovers.
*   **WebSocket Live Log Streaming**: View remote Docker container logs streaming in real-time straight to your dashboard. This operates memory-to-memory via a secure WebSocket connection—no logs are ever saved to the disk or database.
*   **mTLS Security (Mutual TLS)**: Strict authentication layer. The Spring Boot backend only accepts connections from clients and agents presenting valid X.509 digital certificates.
*   **Telegram Alert Bridge**: Integrated Telegram API along with the existing Discord alerts for critical server states.
*   **Modernized UI/UX**: Complete overhaul of the Server Detail page and Chatbot using TailwindCSS, featuring backdrop-blur, dynamic micro-animations, and modern prose styling.

### 🏗️ Architecture
*   **Frontend (Angular)**: Hosts the Notification Hub, Chat Interface, and stores User API Keys securely.
*   **Backend (Spring Boot)**: Orchestrates tasks, bridges to Telegram, and packages contexts for AI models.
*   **AI Service (Python/LangGraph/CrewAI)**: The intelligent core using **LangGraph** for orchestration and **CrewAI** for autonomous diagnostics.
*   **Remote Agent (Bash/Python)**: Polls for tasks, executes commands securely, and reports back.


---

### 🧠 AI Engine & Orchestration (Deep Dive)
Beyond simple LLM prompts, this system utilizes a **Hybrid Orchestration** approach:
*   **LangGraph Orchestrator**: Manages the state machine and intelligent routing. Every query is classified as **SIMPLE** (Direct LLM) or **COMPLEX** (CrewAI) to optimize speed and resource usage.
*   **CrewAI Multi-Agent Team**:
    *   **System Data Analyst**: Analyzes server heartbeats and logs for anomalies.
    *   **DevOps Engineer**: Identifies root causes and provides executable terminal commands.
*   **Local Inference**: Optimized for **Mistral NeMo 12B** or **Llama-3.1-8B** via **LM Studio**, ensuring 100% data privacy.


---

### 🚀 Setup & Quick Start

#### 📋 Prerequisites
*   Java 17 or higher
*   Docker & Docker Compose
*   Python 3.11+ (for AI Service)
*   **LM Studio** (for local AI inference)

#### 1️⃣ Step 1: Start the Database
The application requires PostgreSQL. Use the provided docker-compose file to start just the DB:
```bash
docker-compose up -d postgres
```

#### 2️⃣ Step 2: Run the AI Service (Optional but Recommended)
```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt # or install crewai fastapi uvicorn langchain_openai
python3 app.py
```

#### 3️⃣ Step 3: Run the Application
Start the Spring Boot backend (which also serves the integrated frontend on port 15000):
```bash
./mvnw spring-boot:run
```
Once started, open your browser and go to: `http://localhost:15000`

#### 4️⃣ Step 4: Configure Security & mTLS (CRITICAL)
This application uses **Mutual TLS (mTLS)** for enterprise-grade security.

**A. Establish Server Trust (Root CA):**
*   Locate `certs/rootCA.crt` in the project root.
*   **Mac**: Double-click -> Keychain Access -> Find "AI-Ops-Root-CA" -> Get Info -> Trust -> Set to **"Always Trust"**.
*   **Windows**: Install Certificate -> Local Machine -> Place in **"Trusted Root Certification Authorities"**.

**B. Install Your Identity (Client Certificate):**
*   Locate `certs/client.p12`.
*   Double-click to install. **Password**: `ai-ops-password`.

**C. Accessing the Dashboard:**
*   URL: Use `https://localhost:15000`.
*   **Browser Prompt**: Select the `ai-ops-agent` certificate.

#### 5️⃣ Step 5: Login
*   **Username**: `admin`
*   **Password**: `admin` (Auto-generated on first run)

---

### 🔐 Security Updates
*   **mTLS Authentication**: X.509 Mutual TLS ensures unauthorized scanners cannot hit endpoints.
*   **RSA Command Signing**: Every task is cryptographically signed. The agent verifies this before execution.
*   **Audit Trail**: All remote commands are logged in `audit_logs` with digital signatures.

---

<a name="türkçe"></a>
## 🇹🇷 Türkçe

### 🎯 Nedir?
**"AI-Ops Dashboard"**, yüksek performanslı bir altyapı izleme ve teşhis ekosistemidir. Temel amacı, sunucu yönetimindeki "bağlam boşluğunu" çözmektir. Geleneksel araçlar sadece bir servisin kapalı olduğunu bildirirken, bu platform yapay zekayı kullanarak bunun **neden** olduğunu açıklar.

### 👤 Kimin İçin?
*   **DevOps Mühendisleri & SRE'ler**: Hızlı Kök Neden Analizi (RCA) ve uzak komut çalıştırma ihtiyacı olanlar.
*   **Home-lab Meraklıları**: Yerel clusterlarını (Docker/K8s) akıllı bir bildirim katmanıyla yönetmek isteyenler.
*   **Sistem Yöneticileri**: Yüksek riskli portları açmadan, kısıtlı güvenlik duvarları arkasındaki sunucuları izlemek isteyenler.

### ✨ Yeni AI-Ops Özellikleri ve Güvenlik Altyapısı
*   **YZ Teşhis Katmanı**: Yerel YZ (LM Studio/Ollama) ve Bulut YZ (OpenAI/Claude) entegrasyonu (BYOK mimarisi).
*   **Kök Neden Analizi (RCA)**: Sunucu metriklerinin otomatik paketlenip YZ'ye sorulması ve sonuçların glassmorphic modal arayüzde gösterilmesi.
*   **Terminal Tasarımlı Chatbot**: Sistem durumu hakkında doğal dilde sorgulama yapılabilen macOS esintili sohbet aracı.
*   **RSA Komut İmzalama**: Arka planda **RSA** ile imzalanan komutlar. Ajan, dijital imzayı doğrulamadan hiçbir betiği çalıştırmaz.
*   **Canlı Log Akışı**: Uzak Docker konteyner loglarını WebSocket üzerinden RAM-to-RAM (diske yazmadan) canlı izleme.
*   **mTLS Güvenliği**: Yalnızca geçerli X.509 dijital sertifikasına sahip istemciler ve ajanlar sisteme bağlanabilir.
*   **Telegram Bildirim Köprüsü**: Kritik durumlar için Discord'a ek olarak Telegram API entegrasyonu.
*   **Modern UI/UX**: TailwindCSS ile baştan aşağı yenilenen, bulanık arka planlı ve mikro animasyonlu arayüz.

---

### 🧠 Yapay Zeka Motoru ve Orkestrasyon (Detaylı Bakış)
Bu sistem sadece basit bir YZ isteminden ibaret değildir; DevOps süreçleri için **LangGraph** ve **CrewAI** tabanlı hibrit bir yaklaşım kullanır:
*   **LangGraph Orkestratörü**: Akış diyagramını ve zeki yönlendirmeyi yönetir. Her sorgu **SIMPLE** (Hızlı Yanıt) veya **COMPLEX** (Derin Analiz) olarak sınıflandırılarak hız-kaynak dengesi sağlanır.
*   **CrewAI Çoklu-Ajan Ekibi**:
    *   **Sistem Veri Analisti**: Sunucu metriklerini ve logları tarayarak anormallikleri bulur.
    *   **DevOps Mühendisi**: Kök nedenleri belirler ve çalıştırılabilir terminal komutları üretir.
*   **Yerel Çıkarım (Local Inference)**: **Mistral NeMo 12B** veya **Llama-3.1-8B** modelleriyle **LM Studio** üzerinden %100 veri gizliliği ile çalışır.


---

### 🏗️ Kurulum ve Hızlı Başlangıç

#### 📋 Gereksinimler
*   Java 17 veya üzeri
*   Docker & Docker Compose
*   Python 3.11+ (AI Servisi için)
*   **LM Studio** (Yerel YZ için)

#### 1️⃣ 1. Adım: Veritabanını Başlatın
```bash
docker-compose up -d postgres
```

#### 2️⃣ 2. Adım: AI Servisini Başlatın (Opsiyonel)
```bash
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install crewai fastapi uvicorn langchain_openai
python3 app.py
```

#### 3️⃣ 3. Adım: Uygulamayı Çalıştırın
Spring Boot backend'i başlatın (Frontend 15000 portunda entegre sunulur):
```bash
./mvnw spring-boot:run
```
Tarayıcınızdan şu adrese gidin: `http://localhost:15000`

#### 4️⃣ 4. Adım: mTLS Yapılandırması (KRİTİK)
**A. Sunucu Güvenini Sağlayın (Root CA):**
*   `certs/rootCA.crt` dosyasını bulun ve çift tıklayın.
*   **Mac**: Keychain Access -> "AI-Ops-Root-CA" -> Bilgi Ver -> Güven -> **"Her Zaman Güven"**.
*   **Windows**: Sertifika Yükle -> Yerel Makine -> **"Güvenilen Kök Sertifika Yetkilileri"**.

**B. İstemci Sertifikasını Yükleyin:**
*   `certs/client.p12` dosyasını yükleyin. **Parola**: `ai-ops-password`.

**C. Panele Giriş:**
*   Adres: `https://localhost:15000`.
*   **Sertifika Seçimi**: Tarayıcı uyarısında `ai-ops-agent` sertifikasını seçin.

#### 5️⃣ 5. Adım: Giriş Yapın
*   **Kullanıcı Adı**: `admin`
*   **Şifre**: `admin`

---

### 🔐 Güvenlik ve Denetim
*   **Denetim İzi (Audit Trail)**: Tüm uzak terminal görevleri, komut imzalarıyla birlikte PostgreSQL veritabanında saklanır.
*   **Silme Koruması**: Kritik operasyonlar (sunucu silme vb.) `@PreAuthorize` ile korunur ve oturum gerektirir.
*   **mTLS Zorunluluğu**: Geçerli sertifikası olmayan hiçbir dış tarayıcı API uç noktalarına ulaşamaz.

🗺️ **Roadmap / Gelecek Planları**: Grafana Exporter Desteği, Mobil Uyumlu UI, Kubernetes ResourceQuota Entegrasyonu.

---
