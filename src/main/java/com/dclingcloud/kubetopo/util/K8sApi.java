package com.dclingcloud.kubetopo.util;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class K8sApi {
    @Resource
    private static CoreV1Api coreV1Api;
    @Resource
    private static NetworkingV1beta1Api networkingV1beta1Api;

    public static V1ServiceList listAllServices() throws ApiException {
        return coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null);
    }

    public static NetworkingV1beta1IngressList listIngresses() throws ApiException {
        return networkingV1beta1Api.listIngressForAllNamespaces(null, null, null, null, null, null, null, null, null);
    }

    public static V1PodList listAllPods() throws ApiException {
        return coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
    }

    public static V1Endpoints listEndpoints(String namespace, String serviceName) throws ApiException {
        return coreV1Api.readNamespacedEndpoints(serviceName, namespace, null, null, null);
    }
}