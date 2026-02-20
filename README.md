# 🚀 DevOps Dashboard

[English](#english) | [Türkçe](#türkçe)

---

## English

### 🎯 What is it?
DevOps Dashboard is a simple and powerful tool to monitor your servers, Docker containers, and Kubernetes pods from a single screen. It helps you track uptime, system resources (CPU/RAM/Disk), and alerts you via Discord when something goes wrong.

### 👤 Who is it for?
- Developers and System Admins who manage multiple servers.
- Home lab enthusiasts (monitoring local PCs or IoT devices).
- Teams who want real-time notifications on server health.

### ✨ Key Features
- **Real-time Monitoring:** Sees server status and resource usage instantly via SSE (Server-Sent Events).
- **Comprehensive Metrics:** CPU, RAM, **Disk Usage**, and **Load Average** tracking.
- **15s High-Frequency Polling:** Near real-time data updates for critical systems.
- **Grafana & Prometheus Ready:** Built-in metrics exporter at `/actuator/prometheus` for professional visualization.
- **One-Liner Agent Setup:** Install or **Uninstall** the lightweight Linux agent with a single command.
- **Docker & K8s support:** Automatically discover and monitor your containers and pods.
- **Discord Alerts:** Sends automatic notifications to your Discord channel on status changes or high resource usage.
- **Maintenance Mode:** Stop alerts temporarily when you are working on a server.
- **SSL Bypass:** Easily monitor servers with self-signed certificates.

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

### ✨ Önemli Özellikler
- **Anlık İzleme:** SSE (Server-Sent Events) ile sayfa yenilemeden sunucu durumlarını görürsünüz.
- **Genişletilmiş Metrikler:** CPU, RAM, **Disk Doluluğu** ve **Yük Ortalaması (Load Avg)** takibi.
- **15 Saniyelik Hızlı Polling:** Kritik sistemler için saniyeler içinde güncellenen veri akışı.
- **Grafana & Prometheus Entegrasyonu:** `/actuator/prometheus` üzerinden profesyonel görselleştirme desteği.
- **Tek Satır Ajan Yönetimi:** Linux ajanını tek komutla kurun veya **kaldırın (Uninstall)**.
- **Docker & K8s Desteği:** Konteyner ve pod'larınızı otomatik olarak tanır ve izler.
- **Discord Bildirimleri:** Durum değişikliklerinde veya yüksek kaynak kullanımında Discord'a anlık mesaj gönderir.
- **Bakım Modu:** Sunucu üzerinde çalışma yaparken bildirimleri geçici olarak durdurun.
- **SSL Atlatma:** Kendi sertifikası (self-signed) olan sunucuları kolayca izleyin.

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



<img width="1469" height="839" alt="1" src="https://github.com/user-attachments/assets/8cbb4719-7f56-43f9-ac53-8b6415a3393c" />

<img width="2940" height="4248" alt="2" src="https://github.com/user-attachments/assets/2ac5d4d2-4192-4f60-947f-4402bd115d3d" />

<img width="1467" height="803" alt="3" src="https://github.com/user-attachments/assets/0a1b658d-c3f0-45d4-841d-9c99cdf732ba" />


<img width="673" height="648" alt="4" src="https://github.com/user-attachments/assets/8fcee8e2-0148-4ff9-9a80-51885389fbe7" />

<img width="513" height="577" alt="5" src="https://github.com/user-attachments/assets/5233c015-8315-41c7-9117-e1ed426aeef9" />







