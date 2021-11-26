package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.model.*;
import com.dclingcloud.kubetopo.util.K8sApi;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class TopologyService {
    @Resource
    private CoreV1Api coreV1Api;
    @Resource
    private NetworkingV1beta1Api networkingV1beta1Api;

    public void loadResources() throws ApiException {
        V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null);
        List<ServicePO> svcList = new ArrayList<>(serviceList.getItems().size());
        Map<String, IngressInfo> ingressMapping = getIngressMapping();
        List<V1Service> services = serviceList.getItems();
        for (V1Service service : services) {
            V1Endpoints endpoints = K8sApi.listEndpoints(service.getMetadata().getNamespace(), service.getMetadata().getName());
            List<PodEndpoint> podEndpointList = parseEndpoints(endpoints);
            V1ServiceSpec spec = service.getSpec();
            ServicePO svc = ServicePO.builder()
                    .uid(service.getMetadata().getUid())
                    .name(service.getMetadata().getName())
                    .namespace(service.getMetadata().getNamespace())
                    .type(spec.getType())
                    .clusterIP(spec.getClusterIP())
                    .externalName(spec.getExternalName())
                    .externalIPs(StringUtils.joinWith(",", spec.getExternalIPs()))
                    .loadBalancerIP(spec.getLoadBalancerIP())
                    .build();
            for (V1ServicePort sp : spec.getPorts()) {
                IngressInfo ingressInfo = ingressMapping.get(sp.getName() + ":" + sp.getPort());
                List<PodEndpoint> podEndpoints = new ArrayList<>();
                if (sp.getTargetPort().isInteger()) {
                    for (PodEndpoint podEndpoint : podEndpointList) {
                        for (PodPort port : podEndpoint.getPorts()) {
                            if (port.getPort() == sp.getTargetPort().getIntValue()) {
                                PodEndpoint endpoint = ObjectUtils.clone(podEndpoint);
                                podEndpoints.add(endpoint);
                            }
                        }
                    }
                }
                ServiceEndpointMeta sep = ServiceEndpointMeta.builder()
                        .name(sp.getName())
                        .protocol(StringUtils.defaultString(sp.getAppProtocol(), sp.getProtocol()))
                        .port(sp.getPort())
                        .targetPort(sp.getTargetPort().isInteger() ? sp.getTargetPort().getIntValue() : Integer.parseInt(sp.getTargetPort().getStrValue()))
                        .nodePort(sp.getNodePort())
                        .ingress(ingressInfo)
                        .endpoints(podEndpointList).build();
//                svc.getEndpoints().add(sep);
            }
            svcList.add(svc);
        }
    }

    public List<ServiceInfo> getServices() throws ApiException {
        V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null);
        List<ServiceInfo> svcList = new ArrayList<>(serviceList.getItems().size());
        Map<String, IngressInfo> ingressMapping = getIngressMapping();
        List<V1Service> services = serviceList.getItems();
        for (V1Service service : services) {
            V1Endpoints endpoints = K8sApi.listEndpoints(service.getMetadata().getNamespace(), service.getMetadata().getName());
            List<PodEndpoint> podEndpointList = parseEndpoints(endpoints);
            V1ServiceSpec spec = service.getSpec();
            ServiceInfo svc = ServiceInfo.builder()
                    .name(service.getMetadata().getName())
                    .namespace(service.getMetadata().getNamespace())
                    .type(spec.getType())
                    .clusterIP(spec.getClusterIP())
                    .externalName(spec.getExternalName())
                    .externalIPs(spec.getExternalIPs())
                    .loadbalancerIP(spec.getLoadBalancerIP())
                    .endpoints(new ArrayList<>())
                    .build();
            for (V1ServicePort sp : spec.getPorts()) {
                IngressInfo ingressInfo = ingressMapping.get(sp.getName() + ":" + sp.getPort());
                List<PodEndpoint> podEndpoints = new ArrayList<>();
                if (sp.getTargetPort().isInteger()) {
                    for (PodEndpoint podEndpoint : podEndpointList) {
                        for (PodPort port : podEndpoint.getPorts()) {
                            if (port.getPort() == sp.getTargetPort().getIntValue()) {
                                PodEndpoint endpoint = ObjectUtils.clone(podEndpoint);
                                podEndpoints.add(endpoint);
                            }
                        }
                    }
                }
                ServiceEndpointMeta sep = ServiceEndpointMeta.builder()
                        .name(sp.getName())
                        .protocol(StringUtils.defaultString(sp.getAppProtocol(), sp.getProtocol()))
                        .port(sp.getPort())
                        .targetPort(sp.getTargetPort().isInteger() ? sp.getTargetPort().getIntValue() : Integer.parseInt(sp.getTargetPort().getStrValue()))
                        .nodePort(sp.getNodePort())
                        .ingress(ingressInfo)
                        .endpoints(podEndpointList).build();
                svc.getEndpoints().add(sep);
            }
            svcList.add(svc);
        }
        return svcList;
    }

    private List<PodEndpoint> parseEndpoints(V1Endpoints endpoints) {
        if (endpoints == null)
            return null;
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        ArrayList<PodEndpoint> list = new ArrayList<>();
        for (V1EndpointSubset subset : subsets) {
            for (V1EndpointAddress address : subset.getAddresses()) {
                // Headless services with no ports.
                PodEndpoint pep = PodEndpoint.builder()
                        .hostname(address.getHostname())
                        .ip(address.getIp())
                        .name(Optional.ofNullable(address.getTargetRef()).map(V1ObjectReference::getName).orElse(null))
                        .nodeName(address.getNodeName()).build();
                list.add(pep);
                if (subset.getPorts().size() != 0) {
                    pep.setPorts(new ArrayList<>(subset.getPorts().size()));
                    for (V1EndpointPort port : subset.getPorts()) {
                        PodPort podPort = PodPort.builder()
                                .name(port.getName())
                                .port(port.getPort())
                                .protocol(StringUtils.defaultString(port.getAppProtocol(), port.getProtocol()))
                                .podUID(address.getTargetRef().getUid())
                                .build();
                        pep.getPorts().add(podPort);
                    }
                }
            }
        }
        return list;
    }

    public Map<String, IngressInfo> getIngressMapping() throws ApiException {
        NetworkingV1beta1IngressList ingressList = K8sApi.listIngresses();
        List<NetworkingV1beta1Ingress> ingresses = ingressList.getItems();
        HashMap<String, IngressInfo> igrsMap = new HashMap<>();
        for (int i = 0; i < ingresses.size(); i++) {
            NetworkingV1beta1Ingress ingress = ingresses.get(i);
            // 获取负载均衡IP地址列表
            List<V1LoadBalancerIngress> lbIngresses = ingress.getStatus().getLoadBalancer().getIngress();
            List<String> ips = new ArrayList<>(lbIngresses.size());
            for (V1LoadBalancerIngress lbIngress : lbIngresses) {
                if (StringUtils.isNotBlank(lbIngress.getIp())) {
                    ips.add(lbIngress.getIp());
                } else {
                    ips.add(lbIngress.getHostname());
                }
            }
            List<NetworkingV1beta1IngressRule> rules = ingress.getSpec().getRules();
            for (NetworkingV1beta1IngressRule rule : rules) {
                // 七层路由
                List<NetworkingV1beta1HTTPIngressPath> paths = rule.getHttp().getPaths();
                for (NetworkingV1beta1HTTPIngressPath path : paths) {
                    String svcEndpointKey = path.getBackend().getServiceName() + ":" + path.getBackend().getServicePort();
                    if (!igrsMap.containsKey(svcEndpointKey)) {
                        igrsMap.put(svcEndpointKey, IngressInfo.builder().hostname(rule.getHost()).ips(ips).path(path.getPath()).build());
                    }
                }
            }
        }
        return igrsMap;
    }
}
