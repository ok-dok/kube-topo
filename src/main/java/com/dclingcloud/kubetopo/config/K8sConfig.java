package com.dclingcloud.kubetopo.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.util.Config;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
@ConfigurationProperties("k8s")
@Data
@EnableJpaAuditing
@ComponentScan(basePackages = "com.dclingcloud.kubetopo.**")
public class K8sConfig {
    private String url;
    private String token;

    @Bean
    public ApiClient createApiClient() {
        ApiClient apiClient = Config.fromToken(url, token, false);
        Configuration.setDefaultApiClient(apiClient);
        // infinite timeout
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(20, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);
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
    public NetworkingV1beta1Api createNetworkingV1beta1Api(ApiClient apiClient) {
        return new NetworkingV1beta1Api(apiClient);
    }
}
