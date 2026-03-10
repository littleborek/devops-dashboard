# 🚀 AI-Ops Dashboard

[English](#english) | [Türkçe](#türkçe)

---

## English

### 🎯 What is it?
The "AI-Ops Dashboard" is a high-performance infrastructure monitoring and diagnostic ecosystem. Its primary objective is to solve the "context gap" in server management. While traditional tools notify you that a service is down, this platform leverages Artificial Intelligence to explain why it happened. By centralizing telemetry from remote servers (Oracle Cloud, AWS, or local nodes), the dashboard provides a unified view of system health and automated incident response.

### 👤 Target Users
- **DevOps Engineers & SREs:** Who require rapid Root Cause Analysis (RCA) and remote command execution.
- **Home-lab Enthusiasts:** Users managing complex local clusters (Docker/K8s) who need an intelligent notification layer.
- **System Administrators:** Who need to monitor servers behind restrictive firewalls without exposing high-risk ports.

### ✨ Core AI-Ops Features Added
- **AI Diagnostic Layer:** Integration with Local LLMs (LM Studio/Ollama) and Cloud LLMs (OpenAI/Claude) using a Bring-Your-Own-Key (BYOK) architecture.
- **Root Cause Analysis (RCA):** Automated packaging of server metrics (CPU, RAM, Status) to query the AI and display human-readable explanations in a polished modal interface.
- **Terminal-style AI Chatbot:** A macOS-inspired glassmorphic chat widget at the bottom right, processing natural language queries about system state.
- **Task Execution Queue & Command Signing:** A new secure polling system where remote agents pull commands. Every command is cryptographically signed via **RSA**. The agent verifies the digital signature before executing any script to prevent malicious takeovers.
- **WebSocket Live Log Streaming:** View remote Docker container logs streaming in real-time straight to your dashboard. This operates memory-to-memory via a secure WebSocket connection—no logs are ever saved to the disk or database. 
- **mTLS Security (Mutual TLS):** Strict authentication layer. The Spring Boot backend only accepts connections from clients and agents presenting valid X.509 digital certificates.
- **Telegram Alert Bridge:** Integrated Telegram API along with the existing Discord alerts for critical server states.
- **Modernized UI/UX:** Complete overhaul of the Server Detail page and Chatbot using TailwindCSS, featuring backdrop-blur, dynamic micro-animations, and modern prose styling.

### 🏗️ Architecture
1. **Frontend (Angular):** Hosts the Notification Hub, Chat Interface, and stores User API Keys securely.
2. **Backend (Spring Boot):** Orchestrates tasks, bridges to Telegram, and packages contexts for AI models.
3. **Remote Agent (Bash Script):** Polls for tasks, executes commands securely, and reports back.
4. **AI Integration Layer:** Routes queries locally or to cloud services based on config.

### � Setup & Quick Start

#### 📋 Prerequisites
- **Java 17** or higher
- **Docker & Docker Compose**
- **Maven** (Optional, `./mvnw` wrapper is included)

#### 1️⃣ Step 1: Start the Database
The application requires PostgreSQL. Use the provided docker-compose file to start just the DB:
```bash
docker-compose up -d postgres
```

#### 2️⃣ Step 2: Run the Application
Start the Spring Boot backend (which also serves the integrated frontend on port 15000):
```bash
./mvnw spring-boot:run
```
Once started, open your browser and go to: **`http://localhost:15000`**

#### 3️⃣ Step 3: Configure Security & mTLS (CRITICAL)
This application uses **Mutual TLS (mTLS)** for enterprise-grade security. This means the server verifies your identity via a certificate, and your browser verifies the server via a Root CA. **Without these two steps, you will face SSL connection errors.**

**A. Establish Server Trust (Root CA):**
Your computer needs to trust the "Authority" that signed our certificates.
1. Locate `certs/rootCA.crt` in the project root.
2. **Mac:** Double-click the file -> Keychain Access opens. Find "AI-Ops-Root-CA" -> Right-click "Get Info" -> Expand **Trust** -> Set **"When using this certificate"** to **"Always Trust"**.
3. **Windows:** Double-click the file -> "Install Certificate" -> "Local Machine" -> Place in "Trusted Root Certification Authorities".

**B. Install Your Identity (Client Certificate):**
1. Locate `certs/client.p12`.
2. Double-click to install. **Password:** `ai-ops-password`.
3. Mac users: Ensure it's added to the "login" keychain.

**C. Accessing the Dashboard:**
1. **URL:** Use **`https://localhost:15000`**. (Do not use port 4200 for mTLS testing as it lacks SSL support).
2. **Browser Prompt:** A popup will appear asking you to "Select a Certificate". Choose the **ai-ops-agent** certificate and click OK.
3. If using Firefox: You MUST manually import the `.p12` file via `Settings -> Privacy & Security -> View Certificates -> Your Certificates -> Import`.

#### 4️⃣ Step 4: Login
Once the certificate handshake is complete, use:
- **Username:** `admin`
- **Password:** `admin` (Auto-generated on first run)

#### 🔓 Optional: Grafana & Prometheus
Connect your Prometheus/Grafana stack to the dashboard metrics endpoint:
- **Metrics URL:** `https://<server-ip>:15000/actuator/prometheus` (Note: Requires valid mTLS certificate)
- Use this endpoint as a data source to visualize system metrics and application performance.

### 🔐 Security Updates
- **mTLS Authentication:** X.509 Mutual TLS ensures that external scanners cannot even hit the HTTP endpoints without a valid client certificate.
- **RSA Command Signing:** To prevent lateral movement, any task pushed to an agent is cryptographically signed by the backend. The Python agent verifies this signature with a public key before execution.
- **Complete Audit Trail:** All remote commands executed by agents are historically logged to the `audit_logs` database table along with their digital signatures and targets.
- The **DELETE `/api/v1/servers/{id}`** endpoint now requires authentication. Only logged‑in users (default admin) can delete a server.
- Spring Security has been tightened:
  - CSRF protection disabled for API calls.
  - X.509 Authentication enabled.
  - Method‑level security (`@PreAuthorize`) added to protect delete operations.
- When calling endpoints from a script or curl, include the certificate, e.g:
  ```bash
  curl -k -E certs/client.crt --key certs/client.key https://localhost:15000/api/v1/servers
  ```
- Related data (Docker containers, Kubernetes pods, deployments, server history) is now removed safely using explicit JPQL delete queries to avoid foreign‑key violations.

---

## Türkçe

### 🎯 Nedir?
DevOps Dashboard, sunucularınızı, Docker konteynerlerinizi ve Kubernetes pod'larınızı tek bir ekrandan izlemenizi sağlayan basit ve güçlü bir araçtır. Sunucu sağlığını, kaynak kullanımını (CPU/RAM/Disk) takip eder ve bir sorun olduğunda Discord üzerinden sizi uyarır.

### 👤 Kimin İçin?
- Birden fazla sunucuyu yöneten geliştiriciler ve sistem yöneticileri.
- Kendi ev ağını yönetenler (PC'ler veya IoT cihazlarını izlemek için).
- Sunucu durumları hakkında anlık bildirim almak isteyen ekipler.

### ✨ Yeni AI-Ops Özellikleri ve Güvenlik Altyapısı
- **Yapay Zeka (AI) Teşhis Katmanı:** Kendi API Anahtarınızı Getirin (BYOK) mimarisi ile Yerel YZ (LM Studio/Ollama) ve Bulut YZ (OpenAI/Claude) entegrasyonu sağlandı.
- **Kök Neden Analizi (RCA):** Sunucu metriklerinin otomatik olarak paketlenip YZ'ye sorulması ve sonuçların şık, bulanık arka planlı (glassmorphic) bir ekranda gösterilmesi.
- **Terminal Tasarımlı Chatbot:** Cihazların güncel durumunu doğal dilde sorgulayabilmeniz için sisteme entegre modern akıllı asistan.
- **mTLS Güvenliği (X.509):** Sunucu tarafındaki (Spring Boot) tüm API'ler giriş kapısını mTLS ile kapatmıştır. Yalnızca elinde özel oluşturulmuş dijital sertifikası (Client Certificate) bulunan Ajanlar ve Yöneticiler sisteme bağlanabilir.
- **Görev Yönetimi ve RSA Komut İmzalama (Command Signing):** Sadece güvenli komutlar! Ajanlar dashboard'dan yürütülecek komutları çektiğinde, kodlar arka planda **RSA Private Key** ile imzalanır. Ajan bu komutu çalıştırmadan önce elindeki Public Key ile dijital imzayı doğrular (Signature Verification). Böylece araya giren biri komutu değiştirse bile ajan komutu reddeder.
- **WebSocket Canlı Log Akışı:** Uzak sunuculardaki Docker konteyner loglarını tarayıcı arayüzünden canlı ve anlık olarak akıtır. Bu sistem RAM-to-RAM çalışır ve veritabanını şişirmemek için disk üzerine veri yazmaz.
- **Audit Log (Denetim İzi):** Sistem üzerinden başarılı / başarısız atılan tüm uzak terminal görevleri, komut imzalarıyla birlikte PostgreSQL veritabanında denetim tablosunda tutulur.
- **Telegram Bildirim Köprüsü:** Sistemin kritik duruma geçmesi anında Telegram botunuz üzerinden cep telefonu bildirimleri gönderir.
- **Modern Arayüz (UI/UX) Yenilikleri:** Arayüz bileşenleri baştan tasarlandı; TailwindCSS kullanılarak asılı kalma (hover) animasyonları, Canlı Log Terminal ekranı eklendi.

### ⚙️ Mevcut Klasik Özellikler
- **Anlık İzleme:** SSE (Server-Sent Events) ile sayfa yenilemeden sunucu durumlarını görürsünüz, **15 saniyelik** periyotlarla data çeker.
- **Genişletilmiş Metrikler:** CPU, RAM, Disk Doluluğu ve Yük Ortalaması (Load Avg) takibi.
- **Grafana & Prometheus Entegrasyonu:** `/actuator/prometheus` üzerinden veri gönderimi sağlar.
- **Tek Satır Ajan Yönetimi:** Sunucu tarafındaki Linux ajanını tek komutla kurma imkanı.
- **Docker & K8s Desteği:** Konteyner ve pod'larınızı otomatik olarak tanır, canlı log ve port bilgisi sunar.
- **Geçerli Olmayan SSL Atlatma:** Self-signed sertifikaya sahip sunucuları izleme esnekliği.

### � Kurulum ve Hızlı Başlangıç

#### 📋 Gereksinimler
- **Java 17** veya üzeri
- **Docker & Docker Compose**
- **Maven** (Opsiyonel, `./mvnw` scripti proje içinde mevcuttur)

#### 1️⃣ 1. Adım: Veritabanını Başlatın
Uygulama PostgreSQL veritabanına ihtiyaç duyar. Docker kullanarak tek komutla başlatabilirsiniz:
```bash
docker-compose up -d postgres
```

#### 2️⃣ 2. Adım: Uygulamayı Çalıştırın
Spring Boot backend sunucusunu başlatın (Frontend 15000 portunda entegre olarak sunulacaktır):
```bash
./mvnw spring-boot:run
```
Uygulama hazır olduğunda tarayıcınızdan şu adrese gidin: **`http://localhost:15000`**

#### 3️⃣ 3. Adım: Güvenlik ve mTLS Yapılandırması (KRİTİK)
Bu uygulama, kurumsal düzeyde **Mutual TLS (mTLS)** güvenliği kullanmaktadır. Yani sadece kullanıcı adı/şifre yeterli değildir; tarayıcınızın sunucuya, sunucunuzun da tarayıcıya güvenmesi gerekir. **Bu adımları atlamanız durumunda SSL bağlantı hatası alırsınız.**

**A. Sunucu Güvenini Sağlayın (Root CA):**
Bilgisayarınızın, sertifikalarımızı imzalayan "Makamı" tanıması gerekir.
1. Proje kök dizinindeki `certs/rootCA.crt` dosyasını bulun ve çift tıklayın.
2. **MacOS:** Anahtar Zinciri (Keychain Access) açılacaktır. "AI-Ops-Root-CA" sertifikasını bulun -> Sağ Tık "Bilgi Ver" -> **Güven (Trust)** sekmesini açın -> **"Bu sertifikayı kullanırken:"** ayarını **"Her Zaman Güven (Always Trust)"** yapın.
3. **Windows:** Dosyaya çift tıklayın -> "Sertifika Yükle" -> "Yerel Makine" -> Sertifika deposu olarak "Güvenilen Kök Sertifika Yetkilileri"ni seçin.

**B. Kendi Kimliğinizi Tanıtın (Client Certificate):**
1. Proje kök dizinindeki `certs/client.p12` dosyasını bulun.
2. Çift tıklayarak yükleyin. **Parola:** `ai-ops-password`.
3. MacOS kullanıcıları "login" veya "giriş" zincirine eklendiğinden emin olmalıdır.

**C. Panele Giriş Yapın:**
1. **Adres:** Mutlaka **`https://localhost:15000`** adresini kullanın. (Port 4200 üzerinden SSL testi yapılamaz).
2. **Sertifika Seçimi:** Adrese girdiğinizde tarayıcınız "Kimliğinizi doğrulamak için bir sertifika seçin" penceresini çıkaracaktır. Listeden **ai-ops-agent** sertifikasını seçin ve Tamam deyin.
3. **Önemli Not (Firefox):** Firefox sistem sertifikalarına bakmaz. `Ayarlar -> Gizlilik ve Güvenlik -> Sertifikaları Göster -> Sertifikalarınız -> İçe Aktar` yolunu izleyerek `.p12` dosyasını elle yüklemelisiniz.

#### 4️⃣ 4. Adım: Giriş Yapın
Sertifika el sıkışması başarıyla tamamlandığında giriş ekranına ulaşacaksınız:
- **Kullanıcı Adı:** `admin`
- **Şifre:** `admin` (İlk çalıştırmada veritabanında otomatik oluşturulur)

#### 🔓 Opsiyonel: Grafana & Prometheus Entegrasyonu
Grafana/Prometheus kurulumunuzu dashboard metrik ucuna bağlayabilirsiniz:
- **Metrik Adresi:** `https://<sunucu-ip>:15000/actuator/prometheus` (Uyarı: Prometheus tarafına da client.crt ve key eklenmelidir)
- Bu adresi Prometheus veri kaynağı (data source) olarak ekleyip profesyonel dashboard'lar oluşturabilirsiniz.

### 🔐 Güvenlik Güncellemeleri
- **DELETE `/api/v1/servers/{id}`** endpointi artık kimlik doğrulama gerektiriyor. Sadece oturum açmış kullanıcılar (varsayılan admin) sunucu silebilir.
- Spring Security ayarları güncellendi:
  - API istekleri için CSRF koruması devre dışı bırakıldı.
  - HTTP Basic authentication etkinleştirildi (`admin:admin`).
  - Metod‑seviyesi güvenlik (`@PreAuthorize`) eklenerek silme işlemleri korundu.
- Silme isteği bir betikten ya da curl ile yapılırken kimlik bilgileri eklenmelidir, örnek:
  ```bash
  curl -u admin:admin -X DELETE http://localhost:15000/api/v1/servers/9
  ```
- Sunucu silinirken ilişkili Docker container, Kubernetes pod, deployment ve server‑history kayıtları JPQL delete sorguları ile güvenli bir şekilde temizleniyor.

---

## 🗺️ Roadmap / Gelecek Planları
- [x] **Grafana Exporter Support:** Industry standard visualization.
- [ ] **HTTP Endpoint Monitoring:** Monitor serverless functions (Lambda, Vercel) via HTTP status checks.
- [ ] **Log Tail Viewer:** View remote server logs directly from the dashboard.
- [ ] **Public Status Page:** A read‑only dashboard for sharing system status.
- [ ] **Mobile Responsive UI:** Improved experience for tablets and phones.
- [ ] Kubernetes pod‑resource limits / requests	K8s API’dan resourceQuota ve limitRange bilgilerini çekme.	K8s‑orchestrated ortamların tam görünümü.

<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 50 22" src="https://github.com/user-attachments/assets/f5d91933-f219-4dde-80e7-b36e5886ac06" />

<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 50 36" src="https://github.com/user-attachments/assets/7164352f-ea40-4b65-bda0-3b38adbab1ea" />


<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 49 30" src="https://github.com/user-attachments/assets/c44f636e-3555-470c-bea9-31482e8630cf" />

<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 57 21" src="https://github.com/user-attachments/assets/8d3efddb-34c2-46e2-b3dc-ffa41f822923" />

<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 57 23" src="https://github.com/user-attachments/assets/c1df9876-0028-444a-8cca-6be60dfa46b1" />

<img width="1470" height="956" alt="Screenshot 2026-03-07 at 14 49 23" src="https://github.com/user-attachments/assets/c82b0bc8-4255-427d-83f7-e6e0be6d2187" />


<img width="673" height="648" alt="4" src="https://github.com/user-attachments/assets/8fcee8e2-0148-4ff9-9a80-51885389fbe7" />

<img width="513" height="577" alt="5" src="https://github.com/user-attachments/assets/5233c015-8315-41c7-9117-e1ed426aeef9" />

<img width="1467" height="803" alt="3" src="https://github.com/user-attachments/assets/0a1b658d-c3f0-45d4-841d-9c99cdf732ba" />






