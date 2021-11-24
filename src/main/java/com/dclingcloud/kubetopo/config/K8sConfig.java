package com.dclingcloud.kubetopo.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.util.Config;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
public class K8sConfig {
    @Value("k8s.url")
    private static String url;
    @Value("k8s.value")
    private static String token;

    @Bean
    public ApiClient getApiClient() {
        ApiClient apiClient = Config.fromToken(url, token);
        Configuration.setDefaultApiClient(apiClient);
        // infinite timeout
        OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        apiClient.setHttpClient(httpClient);
        return apiClient;
    }

    @Bean
    public CoreV1Api getCoreV1Api() {
        return new CoreV1Api();
    }

    @Bean
    public NetworkingV1Api getNetworkingV1Api() {
        return new NetworkingV1Api();
    }

    @Bean
    public NetworkingV1beta1Api getNetworkingV1beta1Api(){
        return new NetworkingV1beta1Api();
    }
}
