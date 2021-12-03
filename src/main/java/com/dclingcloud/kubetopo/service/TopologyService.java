package com.dclingcloud.kubetopo.service;

import com.dclingcloud.kubetopo.entity.*;
import com.dclingcloud.kubetopo.repository.*;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.vo.*;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
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

    public TopologyVO getTopo() {
        List<ServicePO> svcList = serviceRepository.findAll();
        List<NodePO> nodeList = nodeRepository.findAll();
        List<IngressPO> ingressList = ingressRepository.findAll();
        List<PodPO> podList = podRepository.findAll();
        List<ServiceVO> svcs = svcList.stream().map(svc -> {
            List<BackendVO> backends = Optional.ofNullable(servicePortRepository.findAllByService(svc)).map(l -> l.stream().map(p -> {
                List<String> endpointsUids = podPortRepository.findAllUidsByServicePort(p);
                String ingressPathRuleUid = pathRuleRepository.getUidByBackend(p);
                return BackendVO.builder()
                        .uid(p.getUid())
                        .serviceUid(svc.getUid())
                        .name(p.getName())
                        .port(p.getPort())
                        .targetPort(p.getTargetPort())
                        .nodePort(p.getNodePort())
                        .protocol(p.getProtocol())
                        .appProtocol(p.getAppProtocol())
                        .ingressPathRuleUid(ingressPathRuleUid)
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
                                                .targetBackendUid(Optional.ofNullable(path.getBackend()).map(ServicePortPO::getUid).orElse(null)).build();
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
                String id = svcPO.getUid() + ":" + sp.getPort() + ":" + sp.getProtocol();
                String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                ServicePortPO servicePortPO = ServicePortPO.builder()
                        .uid(uid)
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
                    String id = ingressPO.getUid() + ":" + rule.getHost() + ":" + path.getPath() + ":" + path.getPathType();
                    String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                    String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                    PathRulePO pathRulePO = PathRulePO.builder()
                            .uid(uid)
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
    protected Map<String, List<PodPortPO>> loadEndpointsMapping(String svcName, String namespace) throws ApiException {
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
                        String id = endpoints.getMetadata().getUid() + ":" + Optional.ofNullable(address.getTargetRef()).map(t -> t.getUid()).map(Objects::toString).orElse("null") + ":" + port.getPort() + ":" + port.getProtocol();
                        String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                        String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                        PodPortPO podPortPO = PodPortPO.builder()
                                .uid(uid)
                                .epUid(endpoints.getMetadata().getUid())
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
