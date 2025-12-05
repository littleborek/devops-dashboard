package com.berk.devopsdashboard.service.impl;

import com.berk.devopsdashboard.dto.request.ServerRequest;
import com.berk.devopsdashboard.dto.response.ServerResponse;
import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.service.ServerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.berk.devopsdashboard.repository.ServerHistoryRepository; 
import com.berk.devopsdashboard.repository.DeploymentRepository; 
import com.berk.devopsdashboard.repository.DockerContainerRepository; 
import com.berk.devopsdashboard.repository.KubernetesPodRepository; 

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final ObjectMapper objectMapper;
    private final ServerHistoryRepository serverHistoryRepository; 
    private final DeploymentRepository deploymentRepository; 
    private final DockerContainerRepository dockerContainerRepository; 
    private final KubernetesPodRepository kubernetesPodRepository; 

    @Override
    public ServerResponse createServer(ServerRequest request) {
        Server server = Server.builder()
                .name(request.getName())
                .ipAddress(request.getIpAddress())
                .operatingSystem(request.getOperatingSystem())
                .location(request.getLocation())
                .category(request.getCategory() == null || request.getCategory().isEmpty() ? "Genel" : request.getCategory())
                .customCertificate(request.getCustomCertificate())
                .status(ServerStatus.UNKNOWN)
                .build();

        return mapToResponse(serverRepository.save(server));
    }


    @Override
    public ServerResponse updateServer(Long id, ServerRequest request) {
        Server server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sunucu bulunamadı ID: " + id));

        server.setName(request.getName());
        server.setIpAddress(request.getIpAddress());
        server.setOperatingSystem(request.getOperatingSystem());
        server.setLocation(request.getLocation());
        server.setCategory(request.getCategory() == null || request.getCategory().isEmpty() ? "Genel" : request.getCategory());
        server.setCustomCertificate(request.getCustomCertificate());
        server.setMaintenanceMode(request.isMaintenanceMode());

        return mapToResponse(serverRepository.save(server));
    }

    @Override
    public List<ServerResponse> getAllServers() {
        return serverRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteServer(Long id) {
        if (!serverRepository.existsById(id)) throw new RuntimeException("Sunucu yok");

        kubernetesPodRepository.deleteByServerId(id); 
        dockerContainerRepository.deleteByServerId(id); 
        deploymentRepository.deleteByServerId(id); 
        serverHistoryRepository.deleteByServerId(id); 
        
        serverRepository.deleteById(id);
    }

    @Override
    public ServerResponse getServerById(Long id) {
        return mapToResponse(serverRepository.findById(id).orElseThrow());
    }
    
    private ServerResponse mapToResponse(Server server) {
        return ServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .ipAddress(server.getIpAddress())
                .operatingSystem(server.getOperatingSystem())
                .location(server.getLocation())
                .category(server.getCategory()) 
                .status(server.getStatus()) 
                .customCertificate(server.getCustomCertificate()) 
                .lastResponseTime(server.getLastResponseTime())
                .maintenanceMode(server.isMaintenanceMode())
                .cpuUsage(server.getCpuUsage())
                .ramUsage(server.getRamUsage())
                .totalRam(server.getTotalRam())
                
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
    
    @Override
    public ServerStatus checkServerStatus(Server server) {
        if (server.getUpdatedAt() != null && 
            server.getUpdatedAt().isAfter(LocalDateTime.now().minusSeconds(45))) {
            return ServerStatus.ONLINE;
        }
        long startTime = System.currentTimeMillis();

        String rawAddress = server.getIpAddress();
        if (rawAddress == null) return ServerStatus.OFFLINE;
        String address = rawAddress.trim().replace("\n", "").replace("\r", "");
        

        if (address.toLowerCase().startsWith("udp://")) {
            ServerStatus status = checkUdpStatus(address);
            server.setLastResponseTime(-1);
            return status;
        }

        String customCert = server.getCustomCertificate();
        String urlString;


        if (address.toLowerCase().startsWith("http")) {
            urlString = address;
        } else if (address.matches("^[0-9.]+$")) {
            urlString = "http://" + address; 
        } else {
            urlString = "https://" + address; 
        }

        try {
            URL url = new URL(urlString);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();


            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                

                if (customCert != null && !customCert.isBlank()) {
                    try {
                        SSLContext sslContext = createSSLContextFromPEM(customCert);
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
 
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                        
                    } catch (Exception e) {
                        System.out.println("Custom Sertifika Hatası: " + e.getMessage());
                        return ServerStatus.OFFLINE;
                    }
                } 
  
                else {
                    if (isLocalNetwork(url.getHost())) {
   
                        trustAllCertificates(httpsConnection);

                    } else {

                    }
                }
            }

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; DevOpsDashboard/1.0)");
            connection.setInstanceFollowRedirects(false); 
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int code = connection.getResponseCode();
            
            long endTime = System.currentTimeMillis();
            int duration = (int) (endTime - startTime);
            
            server.setLastResponseTime(duration);

            System.out.println("Kontrol: " + urlString + " | Kod: " + code + " | Süre: " + duration + "ms");
            
            if (code >= 200 && code < 500) {
                String category = server.getCategory();
                if (category != null && (category.equalsIgnoreCase("WLED") || category.equalsIgnoreCase("IoT"))) {
                    System.out.println(">>> WLED/IoT Cihazı Tespit Edildi: " + server.getName() + ". Metrikler çekiliyor...");
                    fetchWledMetrics(server, urlString);
                }
                return ServerStatus.ONLINE;
            }

        } catch (IOException e) {
            System.out.println("HTTP Hatası (" + urlString + "): " + e.getMessage());

            try {
                long pingStart = System.currentTimeMillis();

                if (InetAddress.getByName(address).isReachable(3000)) {
                    
                    long pingEnd = System.currentTimeMillis();
                    server.setLastResponseTime((int) (pingEnd - pingStart));
                    
                    System.out.println("Ping Başarılı: " + address);
                    return ServerStatus.ONLINE;
                }
            } catch (Exception ex) { }
        }

        server.setLastResponseTime(0);
        return ServerStatus.OFFLINE;
    }

    private void fetchWledMetrics(Server server, String baseUrl) {
        try {
            String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String apiUrl = cleanBaseUrl + "/json/info";
            
            System.out.println(">>> WLED API İsteği: " + apiUrl);

            URL url = new URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            System.out.println(">>> WLED API Yanıt Kodu: " + responseCode);

            if (responseCode == 200) {
                JsonNode root = objectMapper.readTree(conn.getInputStream());
                
                if (root.has("freeheap")) {
                    long freeHeap = root.get("freeheap").asLong();
                    double estimatedTotalHeap = 360 * 1024.0;
                    double usedRam = estimatedTotalHeap - freeHeap;
                    double ramPercent = (usedRam / estimatedTotalHeap) * 100.0;
                    
                    if (ramPercent < 0) ramPercent = 0;
                    if (ramPercent > 100) ramPercent = 100;

                    server.setRamUsage(Math.round(ramPercent * 100.0) / 100.0);
                    server.setTotalRam((freeHeap / 1024) + "KB Free");
                    System.out.println(">>> WLED RAM Güncellendi: %" + ramPercent);
                } else {
                    System.out.println(">>> WLED JSON içinde 'freeheap' alanı bulunamadı.");
                }

                if (root.has("wifi") && root.get("wifi").has("signal")) {
                    double signalStrength = root.get("wifi").get("signal").asDouble();
                    server.setCpuUsage(signalStrength); 
                    System.out.println(">>> WLED WiFi Sinyali (CPU yerine): %" + signalStrength);
                }
            } else {
                System.out.println(">>> WLED API Hatası: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println(">>> WLED Metrik Hatası (" + server.getName() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isLocalNetwork(String host) {

        if (host.equals("localhost") || host.equals("127.0.0.1")) return true;
        
  
        if (host.startsWith("192.168.")) return true;
        

        if (host.startsWith("10.")) return true;
        

        if (host.startsWith("172.") && host.length() > 4) {
          
             return true; 
        }
        
        return false;
    }

    private SSLContext createSSLContextFromPEM(String certString) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
        X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("custom-server-cert", cert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);


        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        return sslContext;
    }

    @Override
    public ServerStatus getServerStatus(String rawAddress) {
        if (rawAddress == null) return ServerStatus.OFFLINE;
        String address = rawAddress.trim().replace("\n", "").replace("\r", "");
        
        String urlString;

        if (address.toLowerCase().startsWith("http")) {
            urlString = address;
        } else if (address.matches("^[0-9.]+$")) {

            urlString = "http://" + address;
        } else {

            urlString = "https://" + address;
        }


        System.out.println("Ham Veri: '" + rawAddress + "' -> Karar Verilen URL: " + urlString);


        try {
            URL url = new URL(urlString);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            trustAllCertificates(connection);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            System.out.println("Sonuç (" + urlString + "): " + responseCode);

            if (responseCode >= 200 && responseCode < 500) {
                return ServerStatus.ONLINE;
            }
        } catch (IOException e) {
            System.out.println("HATA (" + urlString + "): " + e.getMessage());
        }

        return ServerStatus.OFFLINE;
    }
    private static void trustAllCertificates(java.net.HttpURLConnection connection) {
        if (!(connection instanceof javax.net.ssl.HttpsURLConnection)) {
            return;
        }

        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            httpsConnection.setHostnameVerifier(allHostsValid);

        } catch (Exception e) {
            System.err.println("SSL Bypass Hatası: " + e.getMessage());
        }
    }
    private ServerStatus checkUdpStatus(String address) {

        try {
            String cleanAddress = address.replace("udp://", "");
            
            String[] parts = cleanAddress.split(":");
            if (parts.length != 2) return ServerStatus.OFFLINE; 
            
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);


            try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                socket.setSoTimeout(3000); 
                socket.connect(java.net.InetAddress.getByName(host), port);
                

                byte[] data = "Ping".getBytes();
                java.net.DatagramPacket packet = new java.net.DatagramPacket(data, data.length);
                socket.send(packet);
  
                System.out.println("UDP Başarılı: " + host + ":" + port);
                return ServerStatus.ONLINE;
            }
        } catch (java.net.PortUnreachableException e) {
            System.out.println("UDP Port Kapalı (Servis Yok): " + address);
            return ServerStatus.OFFLINE;
        } catch (Exception e) {
            System.out.println("UDP Genel Hata: " + e.getMessage());
            return ServerStatus.OFFLINE;
        }
    }
}