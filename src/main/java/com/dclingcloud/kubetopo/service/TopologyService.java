package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.*;
import com.dclingcloud.kubetopo.model.*;
import com.dclingcloud.kubetopo.repository.*;
import com.dclingcloud.kubetopo.util.K8sApi;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopologyService {
    @Resource
    private CoreV1Api coreV1Api;
    @Resource
    private NetworkingV1beta1Api networkingV1beta1Api;
    @Resource
    private IngressRepository ingressRepository;
    @Resource
    private PathRuleRepository pathRuleRepository;
    @Resource
    private ServiceRepository serviceRepository;
    @Resource
    private PodRepository podRepository;
    @Resource
    private PodPortRepository podPortRepository;
    @Resource
    private ServicePortRepository servicePortRepository;
    @Resource
    private NodeRepository nodeRepository;

    @Transactional
    public void loadResources() throws ApiException {
        // load nodes
        V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null);
        List<NodePO> nodePOList = nodeList.getItems().stream().map(n -> {
            NodePO nodePO = NodePO.builder()
                    .name(n.getMetadata().getName())
                    .uid(n.getMetadata().getUid())
                    .podCIDR(n.getSpec().getPodCIDR())
                    .build();
            n.getStatus().getAddresses().forEach(addr -> {
                if ("Hostname".equals(addr.getType())) {
                    nodePO.setHostname(addr.getAddress());
                } else if ("InternalIP".equals(addr.getType())) {
                    nodePO.setInternalIP(addr.getAddress());
                }
            });
            return nodePO;
        }).collect(Collectors.toList());
        nodeRepository.saveAllAndFlush(nodePOList);

        V1ServiceList serviceList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null);
        Map<String, PathRulePO> ingressPathMapping = loadIngressPathMapping();
        List<V1Service> services = serviceList.getItems();
        for (V1Service service : services) {
            Map<String, List<PodPortPO>> podPortMapping = loadEndpointsMapping(service.getMetadata().getName(), service.getMetadata().getNamespace());
            V1ServiceSpec spec = service.getSpec();
            ServicePO svcPO = ServicePO.builder()
                    .uid(service.getMetadata().getUid())
                    .name(service.getMetadata().getName())
                    .namespace(service.getMetadata().getNamespace())
                    .type(spec.getType())
                    .clusterIP(spec.getClusterIP())
                    .externalName(spec.getExternalName())
                    .externalIPs(StringUtils.joinWith(",", spec.getExternalIPs()))
                    .loadBalancerIP(spec.getLoadBalancerIP())
                    .build();
            serviceRepository.saveAndFlush(svcPO);

            for (V1ServicePort sp : spec.getPorts()) {
                // 获取ingress path rule映射
                PathRulePO pathRulePO = ingressPathMapping.get(sp.getName() + ":" + sp.getPort());
                ServicePortPO servicePortPO = ServicePortPO.builder()
                        .service(svcPO)
                        .name(sp.getName())
                        .protocol(sp.getProtocol())
                        .appProtocol(sp.getAppProtocol())
                        .port(sp.getPort())
                        .nodePort(sp.getNodePort())
                        .ingressPathRule(pathRulePO)
                        .build();
                // 获取pod port映射列表
                List<PodPortPO> podPortPOList = podPortMapping.get(sp.getTargetPort().toString());
                if (sp.getTargetPort().isInteger()) {
                    servicePortPO.setTargetPort(sp.getTargetPort().getIntValue());
                } else {
                    servicePortPO.setTargetPort(podPortPOList.get(0).getPort());
                }
                // 设置pod port关联到service port
                try {
                    podPortPOList.forEach(p -> p.setServicePort(servicePortPO));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                servicePortRepository.saveAndFlush(servicePortPO);
                podPortRepository.saveAllAndFlush(podPortPOList);
            }
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
//                        .targetPort(sp.getTargetPort().isInteger() ? sp.getTargetPort().getIntValue() : Integer.parseInt(sp.getTargetPort().getStrValue()))
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

    @Transactional
    public Map<String, PathRulePO> loadIngressPathMapping() throws ApiException {
        NetworkingV1beta1IngressList ingressList = K8sApi.listIngresses();
        List<NetworkingV1beta1Ingress> ingresses = ingressList.getItems();
        HashMap<String, PathRulePO> pathsMap = new HashMap<>();
        for (int i = 0; i < ingresses.size(); i++) {
            NetworkingV1beta1Ingress ingress = ingresses.get(i);
            IngressPO ingressPO = IngressPO.builder()
                    .uid(ingress.getMetadata().getUid())
                    .name(ingress.getMetadata().getName())
                    .namespace(ingress.getMetadata().getNamespace())
                    .className(ingress.getSpec().getIngressClassName())
                    .build();
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
            ingressPO.setLoadBalancerHosts(StringUtils.join(ips, ","));
            ingressRepository.saveAndFlush(ingressPO);

            List<NetworkingV1beta1IngressRule> rules = ingress.getSpec().getRules();
            for (NetworkingV1beta1IngressRule rule : rules) {
                // 七层路由
                List<NetworkingV1beta1HTTPIngressPath> paths = rule.getHttp().getPaths();
                for (NetworkingV1beta1HTTPIngressPath path : paths) {
                    PathRulePO pathRulePO = PathRulePO.builder()
                            .host(rule.getHost())
                            .path(path.getPath())
                            .pathType(path.getPathType())
                            .ingress(ingressPO)
                            .build();
                    pathRuleRepository.saveAndFlush(pathRulePO);
                    String svcPortKey = path.getBackend().getServiceName() + ":" + path.getBackend().getServicePort();
                    if (!pathsMap.containsKey(svcPortKey)) {
                        pathsMap.put(svcPortKey, pathRulePO);
                    }
                }
            }
        }
        return pathsMap;
    }

    @Transactional
    private Map<String, List<PodPortPO>> loadEndpointsMapping(String svcName, String namespace) throws ApiException {
        V1Endpoints endpoints = K8sApi.listEndpoints(namespace, svcName);
        if (endpoints == null)
            return Collections.emptyMap();
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        Map<String, List<PodPortPO>> podPortMap = new HashMap<>();
        for (V1EndpointSubset subset : subsets) {
            List<V1EndpointAddress> addresses = subset.getAddresses();
            if (CollectionUtils.isEmpty(addresses) && CollectionUtils.isNotEmpty(subset.getNotReadyAddresses())) {
                addresses = subset.getNotReadyAddresses();
            }
            for (V1EndpointAddress address : addresses) {
                PodPO podPO = null;
                if (address.getTargetRef() != null) {
                    podPO = PodPO.builder()
                            .uid(address.getTargetRef().getUid())
                            .name(address.getTargetRef().getName())
                            .namespace(address.getTargetRef().getNamespace())
                            .nodeName(address.getNodeName())
                            .hostname(address.getHostname())
                            .ip(address.getIp())
                            .build();
                    podRepository.save(podPO);
                }

                // Headless services with no ports.
                if (subset.getPorts().size() != 0) {
                    for (V1EndpointPort port : subset.getPorts()) {
                        PodPortPO podPortPO = PodPortPO.builder()
                                .pod(podPO)
                                .name(port.getName())
                                .port(port.getPort())
                                .protocol(port.getProtocol())
                                .appProtocol(port.getAppProtocol())
                                .build();
                        if (!podPortMap.containsKey(podPortPO.getPort()) && podPortPO.getPort() != null) {
                            podPortMap.put(podPortPO.getPort().toString(), new ArrayList<PodPortPO>());
                        }
                        if (!podPortMap.containsKey(podPortPO.getName()) && StringUtils.isNotBlank(podPortPO.getName())) {
                            podPortMap.put(podPortPO.getName(), new ArrayList<PodPortPO>());
                        }
                        if (podPortPO.getPort() != null) {
                            podPortMap.get(podPortPO.getPort().toString()).add(podPortPO);
                        }
                        if (StringUtils.isNotBlank(podPortPO.getName())) {
                            podPortMap.get(podPortPO.getName()).add(podPortPO);
                        }
                    }
                }
            }
        }
        return podPortMap;
    }
}
