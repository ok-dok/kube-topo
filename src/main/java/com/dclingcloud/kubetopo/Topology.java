package com.dclingcloud.kubetopo;

import com.dclingcloud.kubetopo.model.IngressInfo;
import com.dclingcloud.kubetopo.model.PodEndpointMeta;
import com.dclingcloud.kubetopo.model.ServiceEndpointMeta;
import com.dclingcloud.kubetopo.model.ServiceInfo;
import com.dclingcloud.kubetopo.util.K8sApi;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingApi;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class Topology {
    @Resource
    private CoreV1Api coreV1Api;
    @Resource
    private NetworkingApi networkingApi;

    public List<ServiceInfo> getServices() throws ApiException {
        V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null);
        List<ServiceInfo> svcList = new ArrayList<>(serviceList.getItems().size());
        Map<String, IngressInfo> ingressMapping = getIngressMapping();
        List<V1Service> services = serviceList.getItems();
        for (V1Service service : services) {
            V1Endpoints endpoints = K8sApi.listEndpoints(service.getMetadata().getNamespace(), service.getMetadata().getName());
            List<PodEndpointMeta> podEndpointMetaList = parseEndpoints(endpoints);
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
                ServiceEndpointMeta sep = ServiceEndpointMeta.builder()
                        .name(sp.getName())
                        .protocol(sp.getProtocol())
                        .port(sp.getPort())
                        .nodePort(sp.getNodePort())
                        .ingress(ingressInfo)
                        .endpoints(podEndpointMetaList).build();
                svc.getEndpoints().add(sep);
            }
            svcList.add(svc);
        }
        return svcList;
    }

    private List<PodEndpointMeta> parseEndpoints(V1Endpoints endpoints) {
        if (endpoints == null)
            return null;
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        ArrayList<PodEndpointMeta> list = new ArrayList<>();
        for (V1EndpointSubset subset : subsets) {
            if (subset.getPorts().size() == 0) {
                // It's possible to have headless services with no ports.
                for (V1EndpointAddress address : subset.getAddresses()) {
                    PodEndpointMeta pep = PodEndpointMeta.builder()
                            .hostname(address.getHostname())
                            .ip(address.getIp())
                            .name(Optional.ofNullable(address.getTargetRef()).map(V1ObjectReference::getName).orElse(null))
                            .nodeName(address.getNodeName()).build();
                    list.add(pep);
                    //	TODO mapping 需要加入外部ip的地址映射
                }
            } else {
                for (V1EndpointPort port : subset.getPorts()) {
                    for (V1EndpointAddress address : subset.getAddresses()) {
                        PodEndpointMeta pep = PodEndpointMeta.builder()
                                .hostname(address.getHostname())
                                .ip(address.getIp())
                                .port(port.getPort())
                                .name(Optional.ofNullable(address.getTargetRef()).map(V1ObjectReference::getName).orElse(null))
                                .nodeName(address.getNodeName()).build();
                        list.add(pep);
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
