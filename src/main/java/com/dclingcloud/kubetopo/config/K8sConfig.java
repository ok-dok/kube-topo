package com.dclingcloud.kubetopo.config;

import com.dclingcloud.kubetopo.util.ResourceVersionHolder;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.DiscoveryV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootConfiguration
@ConfigurationProperties("k8s")
@Data
@EnableJpaAuditing
@ComponentScan(basePackages = "com.dclingcloud.kubetopo.**")
@Slf4j
public class K8sConfig {
    private String url;
    private String token;

    @Bean
    public ResourceVersionHolder resourceVersonHolder() {
        return new ResourceVersionHolder();
    }

    @Bean
    public ApiClient createApiClient() {
        ApiClient apiClient = Config.fromToken(url, token, false);
        apiClient.setReadTimeout(30000);
        Configuration.setDefaultApiClient(apiClient);
        log.info("K8s apiserver endpoint {}, timeout={}", url, 30000);
        return apiClient;
    }

    @Bean
    public CoreV1Api createCoreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public NetworkingV1Api createNetworkingV1Api(ApiClient apiClient) {
        return new NetworkingV1Api(apiClient);
    }

    @Bean
    public DiscoveryV1Api createDiscoveryV1Api(ApiClient apiClient) {
        return new DiscoveryV1Api(apiClient);
    }
}
