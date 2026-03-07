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
- **Task Execution Queue:** A new secure polling system where remote agents pull commands (`eval` via bash script) and post results back to the dashboard, bypassing strict inbound firewalls.
- **IP Whitelisting & Secure Agent Sync:** Validation of agent IPs via headers or direct extraction during task fetching to prevent unauthorized remote command execution.
- **Telegram Alert Bridge:** Integrated Telegram API along with the existing Discord alerts for critical server states.
- **Modernized UI/UX:** Complete overhaul of the Server Detail page and Chatbot using TailwindCSS, featuring backdrop-blur, dynamic micro-animations, and modern prose styling.

### 🏗️ Architecture
1. **Frontend (Angular):** Hosts the Notification Hub, Chat Interface, and stores User API Keys securely.
2. **Backend (Spring Boot):** Orchestrates tasks, bridges to Telegram, and packages contexts for AI models.
3. **Remote Agent (Bash Script):** Polls for tasks, executes commands securely, and reports back.
4. **AI Integration Layer:** Routes queries locally or to cloud services based on config.

### 🛠️ How to Use?
1. **Run the App:** Start the project using Maven (`./mvnw spring-boot:run`).
2. **Access:** The app runs on port **15000** by default.
3. **Login:** Use the default credentials:
   - **Username:** `admin`
   - **Password:** `admin`
4. **Add Agent:** Click "Agent Setup" on the dashboard, copy the command, and run it on your remote server (using `sudo` for full access).
5. **Grafana:** Connect your Prometheus to `http://<master-ip>:15000/actuator/prometheus` and enjoy professional dashboards.

### 🔐 Security Updates
- The **DELETE `/api/v1/servers/{id}`** endpoint now requires authentication. Only logged‑in users (default admin) can delete a server.
- Spring Security has been tightened:
  - CSRF protection disabled for API calls.
  - HTTP Basic authentication enabled (`admin:admin`).
  - Method‑level security (`@PreAuthorize`) added to protect delete operations.
- When calling the delete endpoint from a script or curl, include the credentials, e.g:
  ```bash
  curl -u admin:admin -X DELETE http://localhost:15000/api/v1/servers/9
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

### ✨ Eklenen Yeni AI-Ops Özellikleri
- **Yapay Zeka (AI) Teşhis Katmanı:** Kendi API Anahtarınızı Getirin (BYOK) mimarisi ile Yerel YZ (LM Studio/Ollama) ve Bulut YZ (OpenAI/Claude) entegrasyonu sağlandı.
- **Kök Neden Analizi (RCA):** Sunucu metriklerinin (CPU, RAM, Durum) otomatik olarak paketlenip YZ'ye sorulması ve sonuçların şık, bulanık arka planlı (glassmorphic) bir ekranda gösterilmesi (Analyze with AI butonu).
- **Terminal Tasarımlı Chatbot:** Cihazların güncel durumunu doğal dilde sorgulayabilmeniz için sağ alt köşeye eklenen, daktilo animasyonlu macOS terminal hissi veren modern akıllı asistan.
- **Görev Yönetimi ve Güvenli Komut Yürütme (Task Queue):** Güvenlik duvarlarını aşan pasif yapı; sunuculara kurulan ajanlar (bash script) her 15 saniyede bir dashboard'a bağlanıp yürütülecek komut var mı kontrol eder, çalıştırır ve `.json` formatında sonucu sisteme geri iletir.
- **IP Doğrulama (Whitelisting):** Görev kuyruğu erişimlerinde uzak ajanın IP adresi kayıtlı veritabanındaki IP ile kontrol edilerek yetkisiz erişimler engellenmiştir.
- **Telegram Bildirim Köprüsü:** Mevcut Discord yapısına ek olarak, sistem kritik duruma geçtiği an Telegram botunuz üzerinden anlık cep telefonu bildirimleri gönderir.
- **Modern Arayüz (UI/UX) Yenilikleri:** Arayüz bileşenleri baştan tasarlandı; TailwindCSS kullanılarak degrade renkler, mikro asılı kalma (hover) animasyonları, Modal pencereler ve tam duyarlı (responsive) tasarım entegre edildi.

### ⚙️ Mevcut Klasik Özellikler
- **Anlık İzleme:** SSE (Server-Sent Events) ile sayfa yenilemeden sunucu durumlarını görürsünüz, **15 saniyelik** periyotlarla data çeker.
- **Genişletilmiş Metrikler:** CPU, RAM, Disk Doluluğu ve Yük Ortalaması (Load Avg) takibi.
- **Grafana & Prometheus Entegrasyonu:** `/actuator/prometheus` üzerinden veri gönderimi sağlar.
- **Tek Satır Ajan Yönetimi:** Sunucu tarafındaki Linux ajanını tek komutla kurma imkanı.
- **Docker & K8s Desteği:** Konteyner ve pod'larınızı otomatik olarak tanır, canlı log ve port bilgisi sunar.
- **Geçerli Olmayan SSL Atlatma:** Self-signed sertifikaya sahip sunucuları izleme esnekliği.

### 🛠️ Nasıl Kullanılır?
1. **Çalıştır:** Projeyi Maven ile başlatın (`./mvnw spring-boot:run`).
2. **Erişim:** Uygulama varsayılan olarak **15000** portunda çalışır.
3. **Giriş Yap:** Varsayılan giriş bilgileri:
   - **Kullanıcı Adı:** `admin`
   - **Şifre:** `admin`
4. **Ajan Kurulumu:** Paneldeki "Ajan Kurulumu" butonuna basın, kopyalayın ve uzak sunucunuzda çalıştırın.
5. **Grafana:** Prometheus veri kaynağı olarak `http://<sunucu-ip>:15000/actuator/prometheus` adresini ekleyin.

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






