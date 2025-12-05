package com.berk.devopsdashboard.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class K8sConfig {
    @Bean
    public ApiClient k8sApiClient() throws IOException {
        try {
            ApiClient client = Config.defaultClient();
            client.setConnectTimeout(10_000); 
            client.setReadTimeout(10_000);
            io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
            
            return client;
        } catch (IOException e) {
            System.err.println("WARN: Kubernetes yapılandırması yüklenemedi. (kubeconfig bulunamadı mı?)");
            return null; 
        }
    }
    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        if (apiClient == null) {
            return null;
        }
        return new CoreV1Api(apiClient);
    }
}