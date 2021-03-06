package com.dclingcloud.kubetopo.util;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.DiscoveryV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import okhttp3.Call;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class K8sApi {
    private static CoreV1Api coreV1Api;
    private static NetworkingV1Api networkingV1Api;
    private static DiscoveryV1Api discoveryV1Api;

    public static V1ServiceList listAllServices() throws ApiException {
        return listAllServices(false);
    }

    public static V1ServiceList listAllServices(boolean watch) throws ApiException {
        return coreV1Api.listServiceForAllNamespaces(watch, null, null, null, null, null, null, null, null, watch);
    }

    public static Call watchServicesAsync() throws ApiException {
        return coreV1Api.listServiceForAllNamespacesAsync(true, null, null, null, null, null, null, null, null, true, null);
    }


    public static Call createServicesCall(String resourceVersion) throws ApiException {
        return coreV1Api.listServiceForAllNamespacesCall(true, null, null, null, null, null, resourceVersion, null, null, true, null);
    }

    public static V1IngressList listIngresses() throws ApiException {
        return listIngresses(false);
    }

    public static V1IngressList listIngresses(boolean watch) throws ApiException {
        return networkingV1Api.listIngressForAllNamespaces(watch, null, null, null, null, null, null, null, null, watch);
    }

    public static Call watchIngressesAsync() throws ApiException {
        return networkingV1Api.listIngressForAllNamespacesAsync(true, null, null, null, null, null, null, null, null, true, null);
    }


    public static Call createIngressesCall(String resourceVersion) throws ApiException {
        return networkingV1Api.listIngressForAllNamespacesCall(true, null, null, null, null, null, resourceVersion, null, null, true, null);
    }


    public static V1PodList listAllPods() throws ApiException {
        return listAllPods(false);
    }

    public static V1PodList listAllPods(boolean watch) throws ApiException {
        return coreV1Api.listPodForAllNamespaces(watch, null, null, null, null, null, null, null, null, watch);
    }

    public static Call watchPodsAsync() throws ApiException {
        return coreV1Api.listPodForAllNamespacesAsync(true, null, null, null, null, null, null, null, null, true, null);
    }

    public static Call createPodsCall(String resourceVersion) throws ApiException {
        Call localVarCall = coreV1Api.listPodForAllNamespacesCall(true, null, null, null, null, null, resourceVersion, null, null, true, null);
        return localVarCall;
    }

    public static V1Endpoints listEndpoints(String namespace, String serviceName) throws ApiException {
        return coreV1Api.readNamespacedEndpoints(serviceName, namespace, null);
    }

    public static V1NodeList listNodes() throws ApiException {
        return listNodes(false);
    }

    public static V1NodeList listNodes(boolean watch) throws ApiException {
        return coreV1Api.listNode(null, watch, null, null, null, null, null, null, null, watch);
    }

    public static Call watchNodesAsync() throws ApiException {
        return coreV1Api.listNodeAsync(null, true, null, null, null, null, null, null, null, true, null);
    }

    public static Call createNodesCall(String resourceVersion) throws ApiException {
        return coreV1Api.listNodeCall(null, true, null, null, null, null, resourceVersion, null, null, true, null);
    }

    public static Call createEndpointsCall(String resourceVersion) throws ApiException {
        return coreV1Api.listEndpointsForAllNamespacesCall(true, null, null, null, null, null, resourceVersion, null, null, true, null);
    }

    public static Call createEndpointSliceCall(String resourceVersion) throws ApiException {
        return discoveryV1Api.listEndpointSliceForAllNamespacesCall(true, null, null, null, null, null, resourceVersion, null, null, true, null);
    }

    public static V1EndpointSliceList listEndpointSlices() throws ApiException {
        return discoveryV1Api.listEndpointSliceForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
    }

    public static V1Service getNamespacedService(String namespace, String svcName) throws ApiException {
        return coreV1Api.readNamespacedService(svcName, namespace, null);
    }

    @Resource
    public void setCoreV1Api(CoreV1Api coreV1Api) {
        K8sApi.coreV1Api = coreV1Api;
    }

    @Resource
    public void setNetworkingV1beta1Api(NetworkingV1Api networkingV1Api) {
        K8sApi.networkingV1Api = networkingV1Api;
    }

    @Resource
    public void setDiscoveryV1Api(DiscoveryV1Api discoveryV1Api) {
        K8sApi.discoveryV1Api = discoveryV1Api;
    }
}
