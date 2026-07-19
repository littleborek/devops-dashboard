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

### ✨ Core AI-Ops Features
*   **Hybrid AI Engine**: Intelligent routing between Direct LLM and CrewAI multi-agent depending on query complexity.
*   **Smart Endpoint Resolution**: Just enter your LM Studio IP (e.g., `100.95.111.63`) — the system automatically appends the correct port and path.
*   **Terminal-style AI Chatbot**: Streaming responses with proper whitespace rendering, bold/code markdown support.
*   **Root Cause Analysis (RCA)**: Automated packaging of server metrics to query AI and display human-readable explanations.
*   **Task Execution Queue & Command Signing**: Secure polling system where remote agents pull RSA-signed commands.
*   **WebSocket Live Log Streaming**: View remote Docker container logs in real-time via WebSocket (no disk writes).
*   **mTLS Security (Mutual TLS)**: Spring Boot backend only accepts connections from clients with valid X.509 certificates.
*   **Telegram & Discord Alerts**: Dual notification bridge for critical server states.

### 🏗️ Architecture
*   **Frontend (Angular 21)**: Streaming AI chat, notification hub, settings management.
*   **Backend (Spring Boot / Java 17)**: Orchestrates tasks, bridges to Telegram, packages AI contexts.
*   **AI Service (Python / LangGraph / CrewAI)**: Intelligent core with hybrid routing.
*   **Remote Agent (Bash/Python)**: Polls for tasks, executes commands securely, reports back.

---

### 🧠 AI Engine & Orchestration

This system uses a **Hybrid Orchestration** approach beyond simple LLM prompts:

| Query Type | Route | Engine |
|---|---|---|
| Greetings, simple info | SIMPLE | Direct LLM (fast) |
| Error analysis, diagnostics | COMPLEX | CrewAI multi-agent |

*   **LangGraph Orchestrator**: Classifies every query via LLM, falls back to keyword heuristic if unavailable.
*   **CrewAI Multi-Agent Team**:
    *   **System Data Analyst**: Analyzes server heartbeats and logs for anomalies.
    *   **DevOps Engineer**: Identifies root causes and produces actionable terminal commands.
*   **Local Inference**: Optimized for **Mistral NeMo 12B** or **Llama-3.1-8B** via **LM Studio**, ensuring 100% data privacy.

---

### 🚀 Setup & Quick Start

#### 📋 Prerequisites
*   Java 17 or higher
*   Docker & Docker Compose
*   Python 3.11+ (for AI Service)
*   Node.js 20+ (for frontend build)
*   **LM Studio** (for local AI inference)

#### 1️⃣ Start with Docker (Recommended)
```bash
# Start DB + Dashboard in one command
docker-compose up -d --build

# Start AI Service (must run on host machine, not in Docker)
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 app.py
```
Open: `http://localhost:15000`

#### 2️⃣ Start Manually (Development)
```bash
# 1. Start DB
docker-compose up -d postgres

# 2. Start AI Service
cd ai-service && python3 app.py &

# 3. Start Backend
./mvnw spring-boot:run
```

#### 3️⃣ Build Frontend (After Code Changes)
```bash
cd frontend
npm run build:deploy   # Builds Angular AND copies to Spring static resources
cd ..
./mvnw clean package -DskipTests
docker-compose up -d --build
```

#### 4️⃣ Install Remote Agent on Target Servers
To monitor remote Linux/Docker servers (e.g. CasaOS, AWS EC2, Raspberry Pi):
```bash
curl -s http://<DASHBOARD_IP>:15000/api/v1/agent/script | sudo bash
```
> ⚠️ **Note**: Make sure to pipe to `sudo bash` so the agent script has permission to inspect Docker containers via `/var/run/docker.sock`.

---

### 🤖 AI Engine Configuration (Settings Panel)

Open the **AI Assistant** button → click the ⚙️ icon to configure:

| Setting | Description |
|---|---|
| **LOCAL** | Direct LM Studio / Ollama endpoint |
| **CREW_AI** | Python AI Service with LangGraph routing |
| **CLOUD** | OpenAI / Claude (API key required) |

#### 🔌 Endpoint URL Format (LOCAL mode)
You can enter the LM Studio address in **any** of these formats — the system normalizes automatically:

| You enter | System uses |
|---|---|
| `100.95.111.63` | `http://100.95.111.63:1234/v1/chat/completions` |
| `100.95.111.63:1234` | `http://100.95.111.63:1234/v1/chat/completions` |
| `http://100.95.111.63:1234` | `http://100.95.111.63:1234/v1/chat/completions` |
| `http://100.95.111.63:1234/v1` | `http://100.95.111.63:1234/v1/chat/completions` |

#### 🤖 Endpoint URL Format (CREW_AI mode)
In CREW_AI mode, the endpoint in Settings is **your LM Studio IP** (same as LOCAL). The system **automatically routes** CrewAI requests to the Python service at `host.docker.internal:8000`.

> 💡 LM Studio note: LM Studio only accepts requests at `/v1/chat/completions`. The root endpoint (`/`) returns `"Unexpected endpoint POST /"` — this is normal and not an error.

---

### 🔐 mTLS Security (CRITICAL)

This application uses **Mutual TLS (mTLS)** for enterprise-grade security.

**A. Establish Server Trust (Root CA):**
*   Locate `certs/rootCA.crt` in the project root.
*   **Mac**: Double-click → Keychain Access → Find "AI-Ops-Root-CA" → Get Info → Trust → **"Always Trust"**.
*   **Windows**: Install Certificate → Local Machine → **"Trusted Root Certification Authorities"**.

**B. Install Your Identity (Client Certificate):**
*   Locate `certs/client.p12`.
*   Double-click to install. **Password**: `ai-ops-password`.

**C. Accessing the Dashboard:**
*   URL: `https://localhost:15000`
*   **Browser Prompt**: Select the `ai-ops-agent` certificate.

**Login credentials:**
*   **Username**: `admin`
*   **Password**: `admin`

---

### 🔐 Security Features
*   **mTLS Authentication**: X.509 Mutual TLS prevents unauthorized scanners from hitting endpoints.
*   **RSA Command Signing**: Every task is cryptographically signed. The agent verifies before execution.
*   **Audit Trail**: All remote commands logged in `audit_logs` with digital signatures.

---

<a name="türkçe"></a>
## 🇹🇷 Türkçe

### 🎯 Nedir?
**"AI-Ops Dashboard"**, yüksek performanslı bir altyapı izleme ve teşhis ekosistemidir. Temel amacı, sunucu yönetimindeki "bağlam boşluğunu" çözmektir. Geleneksel araçlar sadece bir servisin kapalı olduğunu bildirirken, bu platform yapay zekayı kullanarak bunun **neden** olduğunu açıklar.

### 👤 Kimin İçin?
*   **DevOps Mühendisleri & SRE'ler**: Hızlı Kök Neden Analizi (RCA) ve uzak komut çalıştırma ihtiyacı olanlar.
*   **Home-lab Meraklıları**: Yerel clusterlarını (Docker/K8s) akıllı bildirim katmanıyla yönetmek isteyenler.
*   **Sistem Yöneticileri**: Yüksek riskli portları açmadan kısıtlı güvenlik duvarları arkasındaki sunucuları izlemek isteyenler.

### ✨ Özellikler
*   **Hibrit YZ Motoru**: Sorgu karmaşıklığına göre Doğrudan LLM veya CrewAI çoklu-ajan arasında akıllı yönlendirme.
*   **Akıllı Endpoint Çözümleyici**: LM Studio IP'sini yazmanız yeterli — sistem portu ve yolu otomatik tamamlar.
*   **Terminal Tasarımlı Chatbot**: Boşlukları koruyan akış (streaming) yanıtları, markdown desteği.
*   **Kök Neden Analizi (RCA)**: Sunucu metrikleri otomatik paketlenerek YZ'ye sorulur.
*   **RSA Komut İmzalama**: Ajan, dijital imzayı doğrulamadan hiçbir betiği çalıştırmaz.
*   **Canlı Log Akışı**: Uzak Docker konteyner loglarını WebSocket üzerinden RAM-to-RAM canlı izleme.
*   **mTLS Güvenliği**: Yalnızca geçerli X.509 dijital sertifikasına sahip istemciler bağlanabilir.
*   **Telegram & Discord Bildirimleri**: Kritik durumlar için çift bildirim kanalı.

---

### 🧠 Yapay Zeka Motoru (Detaylı Bakış)

| Sorgu Tipi | Rota | Motor |
|---|---|---|
| Selamlama, genel bilgi | SIMPLE | Doğrudan LLM (hızlı) |
| Hata analizi, teşhis | COMPLEX | CrewAI çoklu-ajan |

*   **LangGraph Orkestratörü**: Her sorguyu YZ ile sınıflandırır, YZ başarısız olursa anahtar kelime buluştiği devreye girer.
*   **CrewAI Çoklu-Ajan Ekibi**: Sistem Veri Analisti + DevOps Mühendisi.
*   **Yerel Çıkarım**: **LM Studio** üzerinden **Mistral NeMo 12B** veya **Llama-3.1-8B** ile %100 veri gizliliği.

---

### 🏗️ Kurulum ve Hızlı Başlangıç

#### 📋 Gereksinimler
*   Java 17 veya üzeri
*   Docker & Docker Compose
*   Python 3.11+ (AI Servisi için)
*   Node.js 20+ (Frontend build için)
*   **LM Studio** (Yerel YZ için)

#### 1️⃣ Docker ile Başlatın (Önerilen)
```bash
# Veritabanı + Dashboard tek komutla
docker-compose up -d --build

# AI Servisi (Host makinede çalışmalı, Docker içinde değil)
cd ai-service
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python3 app.py
```
Tarayıcıdan: `http://localhost:15000`

#### 2️⃣ Manuel Başlatma (Geliştirme)
```bash
docker-compose up -d postgres
cd ai-service && python3 app.py &
./mvnw spring-boot:run
```

#### 3️⃣ Frontend Derlemesi (Kod Değişikliğinden Sonra)
```bash
cd frontend
npm run build:deploy   # Angular'ı derler VE Spring static klasörüne kopyalar
cd ..
./mvnw clean package -DskipTests
docker-compose up -d --build
```

#### 4️⃣ Uzak Sunuculara Ajan Kurulumu
Uzak Linux/Docker sunucularını (CasaOS, AWS EC2, Raspberry Pi vb.) izlemek için hedef sunucu terminalinde:
```bash
curl -s http://<DASHBOARD_IP>:15000/api/v1/agent/script | sudo bash
```
> ⚠️ **Not**: Komutun sonundaki `sudo bash` kullanımı önemlidir; böylece ajan `/var/run/docker.sock` üzerindeki tüm Docker konteynerlerini (OpenWebUI vb.) okuma yetkisine sahip olur.

---

### 🤖 YZ Motor Yapılandırması (Ayarlar Paneli)

**AI Assistant** butonuna tıklayın → ⚙️ simgesine tıklayın:

| Ayar | Açıklama |
|---|---|
| **LOCAL** | Doğrudan LM Studio / Ollama endpoint |
| **CREW_AI** | LangGraph yönlendirmeli Python AI Servisi |
| **CLOUD** | OpenAI / Claude (API anahtarı gerekli) |

#### 🔌 Endpoint URL Formatları (LOCAL modu)
LM Studio adresini **istediğiniz formatta** girebilirsiniz — sistem otomatik düzenler:

| Girilen | Sistem Kullanır |
|---|---|
| `100.95.111.63` | `http://100.95.111.63:1234/v1/chat/completions` |
| `100.95.111.63:1234` | `http://100.95.111.63:1234/v1/chat/completions` |
| `http://100.95.111.63:1234` | `http://100.95.111.63:1234/v1/chat/completions` |
| `http://100.95.111.63:1234/v1` | `http://100.95.111.63:1234/v1/chat/completions` |

#### 🤖 Endpoint URL Formatları (CREW_AI modu)
CREW_AI modunda Ayarlar'daki endpoint **LM Studio IP'nizdir** (LOCAL ile aynı). Sistem, CrewAI isteklerini `host.docker.internal:8000` üzerindeki Python servisine **otomatik yönlendirir**.

> 💡 LM Studio notu: LM Studio yalnızca `/v1/chat/completions` adresine gelen istekleri işler. Kök adrese (`/`) yapılan istekler `"Unexpected endpoint POST /"` yanıtı döner — bu normaldir, hata değildir.

---

### 🔐 mTLS Yapılandırması (KRİTİK)

**A. Sunucu Güvenini Sağlayın (Root CA):**
*   `certs/rootCA.crt` dosyasını çift tıklayın.
*   **Mac**: Keychain Access → "AI-Ops-Root-CA" → Bilgi Ver → Güven → **"Her Zaman Güven"**.
*   **Windows**: Sertifika Yükle → Yerel Makine → **"Güvenilen Kök Sertifika Yetkilileri"**.

**B. İstemci Sertifikasını Yükleyin:**
*   `certs/client.p12` dosyasını yükleyin. **Parola**: `ai-ops-password`.

**C. Panele Giriş:**
*   Adres: `https://localhost:15000`
*   Tarayıcı sertifika sorduğunda `ai-ops-agent`'ı seçin.

**Giriş bilgileri:**
*   **Kullanıcı Adı**: `admin`
*   **Şifre**: `admin`

---

### 🔐 Güvenlik ve Denetim
*   **Denetim İzi**: Tüm uzak terminal görevleri komut imzalarıyla PostgreSQL'de saklanır.
*   **mTLS Zorunluluğu**: Geçerli sertifikası olmayan hiçbir dış bağlantı API uç noktalarına ulaşamaz.
*   **RSA Komut İmzalama**: Ajan imzasız hiçbir komutu çalıştırmaz.

🗺️ **Roadmap**: Grafana Exporter Desteği, Mobil Uyumlu UI, Kubernetes ResourceQuota Entegrasyonu.

---
