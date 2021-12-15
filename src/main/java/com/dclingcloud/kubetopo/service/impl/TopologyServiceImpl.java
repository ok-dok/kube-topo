package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.entity.*;
import com.dclingcloud.kubetopo.repository.*;
import com.dclingcloud.kubetopo.service.TopologyService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.util.ResourceVersionHolder;
import com.dclingcloud.kubetopo.vo.*;
import com.dclingcloud.kubetopo.watch.EventType;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopologyServiceImpl implements TopologyService {
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
    @Resource
    private ResourceVersionHolder resourceVersionHolder;

    @Override
    public TopologyVO getTopology() {
        List<ServicePO> svcList = serviceRepository.findAll();
        List<NodePO> nodeList = nodeRepository.findAll();
        List<IngressPO> ingressList = ingressRepository.findAll();
        List<PodPO> podList = podRepository.findAll();
        List<ServiceVO> svcs = svcList.stream().map(svc -> {
            List<BackendVO> backends = Optional.ofNullable(servicePortRepository.findAllByService(svc)).map(l -> l.stream().map(p -> {
                List<String> endpointsUids = podPortRepository.findAllUidsByServicePort(p);
                List<String> servicePortUids = pathRuleRepository.findAllUidsByServicePort(p);
                return BackendVO.builder()
                        .uid(p.getUid())
                        .serviceUid(svc.getUid())
                        .name(p.getName())
                        .port(p.getPort())
                        .targetPort(p.getTargetPort())
                        .nodePort(p.getNodePort())
                        .protocol(p.getProtocol())
                        .appProtocol(p.getAppProtocol())
                        .ingressPathRuleUids(servicePortUids)
                        .endpointUids(endpointsUids)
                        .build();
            }).collect(Collectors.toList())).orElse(null);
            ServiceVO svcVO = ServiceVO.builder()
                    .uid(svc.getUid())
                    .name(svc.getName())
                    .namespace(svc.getNamespace())
                    .type(svc.getType())
                    .clusterIP(svc.getClusterIP())
                    .externalIPs(svc.getExternalIPs())
                    .externalName(svc.getExternalName())
                    .loadBalancerIP(svc.getLoadBalancerIP())
                    .backends(backends).build();
            return svcVO;
        }).collect(Collectors.toList());
        List<IngressVO> ingresses = ingressList.stream().map(igrs -> {
            return IngressVO.builder()
                    .uid(igrs.getUid())
                    .name(igrs.getName())
                    .namespace(igrs.getNamespace())
                    .className(igrs.getClassName())
                    .loadBalancerHosts(igrs.getLoadBalancerHosts())
                    .pathRules(Optional.ofNullable(pathRuleRepository.findAllByIngress(igrs))
                                    .map(l -> l.stream().map(path -> {
//                                String backendUid = servicePortRepository.getUidByIngressPathRule(path);
                                        return IngressPathRuleVO.builder()
                                                .uid(path.getUid())
                                                .ingressUid(igrs.getUid())
                                                .host(path.getHost())
                                                .path(path.getPath())
                                                .pathType(path.getPathType())
                                                .targetBackendUid(Optional.ofNullable(path.getBackend())
                                                        .map(ServicePortPO::getUid).orElse(null))
                                                .build();
                                    }).collect(Collectors.toList()))
                                    .orElse(null)
                    ).build();
        }).collect(Collectors.toList());
        List<PodVO> pods = podList.stream().map(pod -> {
            return PodVO.builder()
                    .uid(pod.getUid())
                    .name(pod.getName())
                    .namespace(pod.getNamespace())
                    .hostname(pod.getHostname())
                    .ip(pod.getIp())
                    .nodeName(pod.getNodeName())
                    .endpoints(Optional.ofNullable(podPortRepository.findAllByPod(pod)).map(l -> l.stream().map(p -> {
                                return EndpointVO.builder()
                                        .uid(p.getUid())
                                        .name(p.getName())
                                        .backendUid(Optional.ofNullable(p.getServicePort()).map(ServicePortPO::getUid).orElse(null))
                                        .podUid(pod.getUid())
                                        .protocol(p.getProtocol())
                                        .port(p.getPort())
                                        .appProtocol(p.getAppProtocol())
                                        .build();
                            }).collect(Collectors.toList())
                    ).orElse(null))
                    .build();
        }).collect(Collectors.toList());
        List<NodeVO> nodes = nodeList.stream().map(n -> {
            return NodeVO.builder()
                    .uid(n.getUid())
                    .name(n.getName())
                    .internalIP(n.getInternalIP())
                    .hostname(n.getHostname())
                    .podCIDR(n.getPodCIDR())
                    .build();
        }).collect(Collectors.toList());
        return TopologyVO.builder().ingresses(ingresses).services(svcs).pods(pods).nodes(nodes).build();
    }

    @Override
    @Transactional
    public void loadResourcesTopology() throws ApiException {
        // load nodes
        V1NodeList nodeList = K8sApi.listNodes();
        List<NodePO> nodePOList = nodeList.getItems().stream().map(n -> {
            NodePO nodePO = NodePO.builder()
                    .name(n.getMetadata().getName())
                    .uid(n.getMetadata().getUid())
                    .podCIDR(n.getSpec().getPodCIDR())
                    .status(EventType.ADDED)
                    .gmtCreate(n.getMetadata().getCreationTimestamp().toLocalDateTime())
                    .build();
            n.getStatus().getAddresses().forEach(addr -> {
                if ("Hostname".equals(addr.getType())) {
                    nodePO.setHostname(addr.getAddress());
                } else if ("InternalIP".equals(addr.getType())) {
                    nodePO.setInternalIP(addr.getAddress());
                }
            });
            resourceVersionHolder.syncUpdateLatest(n.getMetadata().getResourceVersion());
            return nodePO;
        }).collect(Collectors.toList());
        nodeRepository.saveAllAndFlush(nodePOList);

        V1ServiceList serviceList = K8sApi.listAllServices();
        Map<String, List<PathRulePO>> ingressPathMapping = loadIngressPathMapping();
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
                    .status(EventType.ADDED)
                    .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                    .build();
            resourceVersionHolder.syncUpdateLatest(service.getMetadata().getResourceVersion());
            serviceRepository.saveAndFlush(svcPO);

            for (V1ServicePort sp : spec.getPorts()) {
                // 获取ingress path rule映射
                List<PathRulePO> pathRulePOList = ingressPathMapping.get(service.getMetadata().getNamespace() + ":" + service.getMetadata().getName() + ":" + sp.getPort());
                String id = svcPO.getUid() + ":" + sp.getPort() + ":" + sp.getProtocol();
                String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                ServicePortPO servicePortPO = ServicePortPO.builder()
                        .uid(uid)
                        .service(svcPO)
                        .name(sp.getName())
                        .protocol(sp.getProtocol())
                        .appProtocol(sp.getAppProtocol())
                        .port(sp.getPort())
                        .nodePort(sp.getNodePort())
                        .ingressPathRules(pathRulePOList)
                        .status(EventType.ADDED)
                        .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                        .build();
                // 获取pod port映射列表
                List<PodPortPO> podPortPOList = podPortMapping.get(sp.getTargetPort().toString());
                if (sp.getTargetPort().isInteger()) {
                    servicePortPO.setTargetPort(sp.getTargetPort().getIntValue());
                } else if (CollectionUtils.isNotEmpty(podPortPOList)) {
                    servicePortPO.setTargetPort(podPortPOList.get(0).getPort());
                }
                servicePortRepository.saveAndFlush(servicePortPO);
                // 设置pod port关联到service port
                if (CollectionUtils.isNotEmpty(podPortPOList)) {
                    podPortPOList.forEach(p -> p.setServicePort(servicePortPO));
                    podPortRepository.saveAllAndFlush(podPortPOList);
                }
                if (CollectionUtils.isNotEmpty(pathRulePOList)) {
                    pathRulePOList.forEach(p -> p.setBackend(servicePortPO));
                    pathRuleRepository.saveAllAndFlush(pathRulePOList);
                }
            }
        }
    }

    @Override
    @Transactional
    public void updateAllResourcesWithDeletedStatus() throws K8sServiceException {
        try {
            serviceRepository.updateAllWithDeletedStatus();
            servicePortRepository.updateAllWithDeletedStatus();
            nodeRepository.updateAllWithDeletedStatus();
            ingressRepository.updateAllWithDeletedStatus();
            pathRuleRepository.updateAllWithDeletedStatus();
            podRepository.updateAllWithDeletedStatus();
            podPortRepository.updateAllWithDeletedStatus();
        } catch (PersistenceException e) {
            log.error("Error: update all resources' status to 'DELETED' failed", e);
            throw new K8sServiceException("failed to tag old resources to deleted status", e);
        }
    }

    @Transactional
    public Map<String, List<PathRulePO>> loadIngressPathMapping() throws ApiException {
        V1IngressList ingressList = K8sApi.listIngresses();
        List<V1Ingress> ingresses = ingressList.getItems();
        HashMap<String, List<PathRulePO>> pathsMap = new HashMap<>();
        for (int i = 0; i < ingresses.size(); i++) {
            V1Ingress ingress = ingresses.get(i);
            IngressPO ingressPO = IngressPO.builder()
                    .uid(ingress.getMetadata().getUid())
                    .name(ingress.getMetadata().getName())
                    .namespace(ingress.getMetadata().getNamespace())
                    .className(ingress.getSpec().getIngressClassName())
                    .status(EventType.ADDED)
                    .gmtCreate(ingress.getMetadata().getCreationTimestamp().toLocalDateTime())
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
            resourceVersionHolder.syncUpdateLatest(ingress.getMetadata().getResourceVersion());
            ingressRepository.saveAndFlush(ingressPO);

            List<V1IngressRule> rules = ingress.getSpec().getRules();
            for (V1IngressRule rule : rules) {
                // 七层路由
                List<V1HTTPIngressPath> paths = rule.getHttp().getPaths();
                for (V1HTTPIngressPath path : paths) {
                    String id = ingressPO.getUid() + ":" + rule.getHost() + ":" + path.getPath() + ":" + path.getPathType();
                    String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                    PathRulePO pathRulePO = PathRulePO.builder()
                            .uid(uid)
                            .host(rule.getHost())
                            .path(path.getPath())
                            .pathType(path.getPathType())
                            .ingress(ingressPO)
                            .status(EventType.ADDED)
                            .gmtCreate(ingress.getMetadata().getCreationTimestamp().toLocalDateTime())
                            .build();
                    pathRuleRepository.saveAndFlush(pathRulePO);
                    String svcPortKey = ingress.getMetadata().getNamespace() + ":" + path.getBackend().getService().getName() + ":" + path.getBackend().getService().getPort();
                    if (!pathsMap.containsKey(svcPortKey)) {
                        pathsMap.put(svcPortKey, new ArrayList<PathRulePO>());
                    }
                    pathsMap.get(svcPortKey).add(pathRulePO);
                }
            }
        }
        return pathsMap;
    }

    @Transactional
    protected Map<String, List<PodPortPO>> loadEndpointsMapping(String svcName, String namespace) throws ApiException {
        V1Endpoints endpoints = K8sApi.listEndpoints(namespace, svcName);
        resourceVersionHolder.syncUpdateLatest(endpoints.getMetadata().getResourceVersion());
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        if (subsets == null)
            return Collections.emptyMap();
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
                            .status(EventType.ADDED)
                            .gmtCreate(endpoints.getMetadata().getCreationTimestamp().toLocalDateTime())
                            .build();
                    podRepository.save(podPO);
                }

                // Headless services with no ports.
                if (subset.getPorts().size() != 0) {
                    for (CoreV1EndpointPort port : subset.getPorts()) {
                        String id = endpoints.getMetadata().getUid() + ":" + Optional.ofNullable(address.getTargetRef()).map(t -> t.getUid()).map(Objects::toString).orElse("null") + ":" + port.getPort() + ":" + port.getProtocol();
                        String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                        PodPortPO podPortPO = PodPortPO.builder()
                                .uid(uid)
                                .epUid(endpoints.getMetadata().getUid())
                                .pod(podPO)
                                .name(port.getName())
                                .port(port.getPort())
                                .protocol(port.getProtocol())
                                .appProtocol(port.getAppProtocol())
                                .status(EventType.ADDED)
                                .gmtCreate(endpoints.getMetadata().getCreationTimestamp().toLocalDateTime())
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
