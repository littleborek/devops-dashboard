package com.berk.devopsdashboard.service;

import com.berk.devopsdashboard.entity.Server;
import com.berk.devopsdashboard.entity.enums.ServerStatus;
import com.berk.devopsdashboard.repository.ServerRepository;
import com.berk.devopsdashboard.repository.DockerContainerRepository;
import com.berk.devopsdashboard.repository.KubernetesPodRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricsService implements MeterBinder {

        private final ServerRepository serverRepository;
        private final DockerContainerRepository containerRepository;
        private final KubernetesPodRepository k8sRepository;
        private final Map<String, Boolean> registeredServers = new ConcurrentHashMap<>();
        private MeterRegistry registry;

        @Override
        public void bindTo(MeterRegistry registry) {
                this.registry = registry;
                log.info(">>> Prometheus MeterBinder bağlanıyor...");

                Gauge.builder("devops_dashboard_server_count", serverRepository, repo -> (double) repo.count())
                                .description("Toplam kayıtlı sunucu sayısı")
                                .register(registry);

                Gauge.builder("devops_dashboard_online_count", serverRepository,
                                repo -> (double) repo.findAll().stream()
                                                .filter(s -> s.getStatus() == ServerStatus.ONLINE).count())
                                .description("Çevrimiçi sunucu sayısı")
                                .register(registry);
        }

        public void registerServerMetrics(Server server) {
                if (registry == null)
                        return;

                String serverIp = server.getIpAddress();
                if (registeredServers.containsKey(serverIp))
                        return;

                String serverName = server.getName();
                log.info("Yeni sunucu metrikleri Grafana/Prometheus için kaydediliyor: {} ({})", serverName, serverIp);

                Gauge.builder("devops_server_cpu_usage", serverRepository,
                                repo -> repo.findByIpAddress(serverIp).map(Server::getCpuUsage).orElse(0.0))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                Gauge.builder("devops_server_ram_usage", serverRepository,
                                repo -> repo.findByIpAddress(serverIp).map(Server::getRamUsage).orElse(0.0))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                Gauge.builder("devops_server_disk_usage", serverRepository,
                                repo -> repo.findByIpAddress(serverIp).map(Server::getDiskUsage).orElse(0.0))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                Gauge.builder("devops_server_container_count", containerRepository,
                                repo -> (double) repo.countByServer_IpAddress(serverIp))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                Gauge.builder("devops_server_pod_count", k8sRepository,
                                repo -> (double) repo.countByServer_IpAddress(serverIp))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                Gauge.builder("devops_server_status", serverRepository,
                                repo -> repo.findByIpAddress(serverIp)
                                                .map(s -> s.getStatus() == ServerStatus.ONLINE ? 1.0 : 0.0)
                                                .orElse(0.0))
                                .tag("ip", serverIp)
                                .tag("name", serverName)
                                .register(registry);

                registeredServers.put(serverIp, true);
        }
}
