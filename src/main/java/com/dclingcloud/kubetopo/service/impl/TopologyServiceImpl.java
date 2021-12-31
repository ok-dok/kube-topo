package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.constants.K8sResources;
import com.dclingcloud.kubetopo.entity.*;
import com.dclingcloud.kubetopo.repository.*;
import com.dclingcloud.kubetopo.service.*;
import com.dclingcloud.kubetopo.util.CustomUidGenerateUtil;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.dclingcloud.kubetopo.util.ResourceVersionHolder;
import com.dclingcloud.kubetopo.vo.*;
import com.dclingcloud.kubetopo.watch.EventType;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private PodService podService;
    @Resource
    private PodPortRepository podPortRepository;
    @Resource
    private ServicePortRepository servicePortRepository;
    @Resource
    private NodeRepository nodeRepository;
    @Resource
    private ResourceVersionHolder resourceVersionHolder;
    @Resource
    private EndpointsService endpointsService;
    @Resource
    private ServicePortService servicePortService;
    @Resource
    private PodPortService podPortService;

    @Override
    public TopologyVO getTopology() {
        List<ServicePO> svcList = serviceRepository.findAll();
        List<NodePO> nodeList = nodeRepository.findAll();
        List<IngressPO> ingressList = ingressRepository.findAll();
        List<PodPO> podList = podRepository.findAll();
        List<ServiceVO> svcs = svcList.stream().map(svc -> {
            List<BackendVO> backends = Optional.ofNullable(servicePortRepository.findAllByService(svc)).map(l -> l.stream().map(p -> {
                List<String> endpointsUids = Optional.ofNullable(p.getBackendEndpointRelations()).map(Collection::stream)
                        .map(stream -> stream.map(b -> b.getPodPort().getUid())
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
                List<String> servicePortUids = pathRuleRepository.findAllUidsByServicePort(p);
                return BackendVO.builder()
                        .uid(p.getUid())
                        .serviceUid(svc.getUid())
                        .name(p.getName())
                        .port(p.getPort())
                        .targetPort(p.getTargetPort().toString())
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
                    .nodeName(Optional.ofNullable(pod.getNode()).map(NodePO::getName).orElse(""))
                    .endpoints(Optional.ofNullable(podPortRepository.findAllByPod(pod)).map(l -> l.stream().map(p -> {
                                return EndpointVO.builder()
                                        .uid(p.getUid())
                                        .name(p.getName())
                                        .backendUids(Optional.ofNullable(p.getBackendEndpointRelations())
                                                .map(Collection::stream)
                                                .map(stream -> stream
                                                        .map(BackendEndpointRelationPO::getServicePort)
                                                        .map(ServicePortPO::getUid)
                                                        .collect(Collectors.toSet()))
                                                .orElse(null))
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

    @Override
    public void loadResourcesTopology() throws ApiException {
        Map<String, NodePO> nodeMapping = loadNodeMapping();
        loadPods(nodeMapping);
        Map<String, Collection<PathRulePO>> ingressPathMapping = loadIngressPathMapping();
        loadServices(ingressPathMapping);
        loadEndpointSlices();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void loadServices(Map<String, Collection<PathRulePO>> ingressPathMapping) throws ApiException {
        V1ServiceList serviceList = K8sApi.listAllServices();
        List<V1Service> services = serviceList.getItems();
        for (V1Service service : services) {
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
                Collection<PathRulePO> pathRulePOList = ingressPathMapping.get(service.getMetadata().getNamespace() + ":" + service.getMetadata().getName() + ":" + sp.getPort().intValue());
                String uid = CustomUidGenerateUtil.getServicePortUid(svcPO.getUid(), sp.getPort(), sp.getProtocol());
                ServicePortPO servicePortPO = ServicePortPO.builder()
                        .uid(uid)
                        .service(svcPO)
                        .name(sp.getName())
                        .protocol(sp.getProtocol())
                        .appProtocol(sp.getAppProtocol())
                        .port(sp.getPort())
                        .nodePort(sp.getNodePort())
                        .targetPort(sp.getTargetPort())
                        .ingressPathRules(pathRulePOList)
                        .status(EventType.ADDED)
                        .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                        .build();
                servicePortRepository.saveAndFlush(servicePortPO);

                if (CollectionUtils.isNotEmpty(pathRulePOList)) {
                    pathRulePOList.forEach(p -> p.setBackend(servicePortPO));
                    pathRuleRepository.saveAllAndFlush(pathRulePOList);
                }
            }
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Map<String, Collection<PathRulePO>> loadIngressPathMapping() throws ApiException {
        V1IngressList ingressList = K8sApi.listIngresses();
        List<V1Ingress> ingresses = ingressList.getItems();
        HashMap<String, Collection<PathRulePO>> pathsMap = new HashMap<>();
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
                    String svcPortKey = ingress.getMetadata().getNamespace() + ":" + path.getBackend().getService().getName() + ":" + path.getBackend().getService().getPort().getNumber();
                    if (!pathsMap.containsKey(svcPortKey)) {
                        pathsMap.put(svcPortKey, new HashSet<PathRulePO>());
                    }
                    pathsMap.get(svcPortKey).add(pathRulePO);
                }
            }
        }
        return pathsMap;
    }


   /* @Transactional
    protected Map<String, Collection<PodPortPO>> loadEndpointsMapping(String svcName, String namespace) throws ApiException {
        V1Endpoints endpoints = K8sApi.listEndpoints(namespace, svcName);
        resourceVersionHolder.syncUpdateLatest(endpoints.getMetadata().getResourceVersion());
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        if (subsets == null)
            return Collections.emptyMap();
        Map<String, Collection<PodPortPO>> podPortMap = new HashMap<>();
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
                    podService.saveOrUpdate(podPO);
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
                        //podPortRepository.saveAndFlush(podPortPO);

                        if (!podPortMap.containsKey(podPortPO.getPort()) && podPortPO.getPort() != null) {
                            podPortMap.put(podPortPO.getPort().toString(), new ArrayList<PodPortPO>());
                        }
                        if (!podPortMap.containsKey(podPortPO.getName()) && StringUtils.isNotBlank(podPortPO.getName())) {
                            podPortMap.put(podPortPO.getName(), new HashSet<PodPortPO>());
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
    }*/

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    protected void loadEndpointSlices() throws ApiException {
        V1EndpointSliceList endpointSliceList = K8sApi.listEndpointSlices();
        List<V1EndpointSlice> sliceList = endpointSliceList.getItems();
        if (CollectionUtils.isEmpty(sliceList)) {
            return;
        }
        Set<BackendEndpointRelationPO> backendEndpointRelationSet = new HashSet<>();
        for (V1EndpointSlice endpointSlice : sliceList) {
            V1ObjectMeta metadata = endpointSlice.getMetadata();
            LocalDateTime gmtCreate = metadata.getCreationTimestamp().toLocalDateTime();
            LocalDateTime gmtModified = Optional.ofNullable(metadata.getAnnotations())
                    .map(map -> map.get("endpoints.kubernetes.io/last-change-trigger-time"))
                    .map(timestamp -> LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .orElse(null);
            String namespace = metadata.getNamespace();
            Optional<List<V1OwnerReference>> ownerReferencesOpt = Optional.ofNullable(metadata.getOwnerReferences());
            // 通常来说，ownerReferences只有一个，但是其类型是列表类型，因此极端情况下或许会出现多个的情况？
            // TODO 需要验证多个ownerReference的情况
            // 需要注意：有一些endpoint没有对应的service，比如集群服务kubernetes，它只有endpoint
            String serviceUid = ownerReferencesOpt.map(list -> list.stream()
                            .filter(owner -> K8sResources.Service.toString().equalsIgnoreCase(owner.getKind())))
                    .flatMap(Stream::findAny)
                    .map(V1OwnerReference::getUid).orElse(null);
            if (serviceUid == null) {
                Optional<String> svcNameOpt = Optional.ofNullable(metadata.getLabels())
                        .map(map -> map.get("kubernetes.io/service-name"));
                if (svcNameOpt.isPresent()) {
                    V1Service service = K8sApi.getNamespacedService(metadata.getNamespace(), svcNameOpt.get());
                    serviceUid = service.getMetadata().getUid();
                }
            }
            if (CollectionUtils.isNotEmpty(endpointSlice.getPorts())) {
                // 取出所有的endpoints状态（存在pod的情况）
                Map<String, V1Endpoint> podStates = endpointSlice.getEndpoints().stream()
                        .filter(ep -> ep.getTargetRef() != null)
                        .map(ep -> {
                            return new AbstractMap.SimpleEntry<String, V1Endpoint>(ep.getTargetRef().getUid(), ep);
                        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

                for (DiscoveryV1EndpointPort port : endpointSlice.getPorts()) {
                    IntOrString targetPort = new IntOrString(port.getPort());
                    // 使用AtomicReference进行包装的目的是使流处理能够进行引用，而且需要更改其值
                    AtomicReference<Optional<ServicePortPO>> servicePortOptRef = new AtomicReference(servicePortService.findByServiceUidAndTargetPortAndProtocol(serviceUid, targetPort, port.getProtocol()));
                    // endpoint存在targetRef即意味着存在后端pod提供endpoint服务
                    //此时需要 ServicePort 与 PodPort做笛卡尔积
                    String finalServiceUid = serviceUid;
                    podStates.forEach((podUid, ep) -> {
                        Optional<PodPortPO> podPortOpt = podPortService.find(podUid, targetPort, port.getProtocol());
                        // 能找到后端podPort，非意外情况，必存在，因为已经有对应的pod提供服务，如果出现找不到的情况，必须要排查找不到PodPort的原因，是否保存异常？
                        if (podPortOpt.isPresent()) {
                            // 通过Integer类型的targetPort找不到servicePort时，通过名称查找一下
                            servicePortOptRef.updateAndGet(prev -> {
                                // 如果能够找到就不需要再查找了
                                if (!prev.isPresent()) {
                                    return servicePortService.findByServiceUidAndTargetPortAndProtocol(finalServiceUid, new IntOrString(podPortOpt.get().getName()), port.getProtocol());
                                } else {
                                    return prev;
                                }
                            });
                            Optional<BackendEndpointRelationPO> backendEndpointRelation = endpointsService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), podPortOpt.get());
                            final String state = BooleanUtils.isNotFalse(ep.getConditions().getReady()) ? "Ready" :
                                    BooleanUtils.isTrue(ep.getConditions().getTerminating()) ? "Terminating" : null;
                            if (backendEndpointRelation.isPresent()) {
                                // 记录已存在的情况下，需要比对修改时间，只有时间在后的修改才可以提交数据库
                                // 这个操作必须存在，原因请参考：https://kubernetes.io/zh/docs/concepts/services-networking/endpoint-slices/#distribution-of-endpointslices
                                if (gmtModified != null && backendEndpointRelation.get().getGmtModified().isBefore(gmtModified)) {
                                    BackendEndpointRelationPO backendEndpointRelationPO = backendEndpointRelation.map(r -> {
                                        r.setGmtModified(gmtModified);
                                        r.setState(state);
                                        r.setStatus(EventType.ADDED);
                                        r.setAddresses(StringUtils.join(ep.getAddresses(), ","));
                                        r.setPort(port.getPort());
                                        return r;
                                    }).get();
                                    backendEndpointRelationSet.add(backendEndpointRelationPO);
                                }
                            } else {
                                BackendEndpointRelationPO backendEndpointRelationPO = BackendEndpointRelationPO.builder()
                                        .state(state)
                                        .servicePort(servicePortOptRef.get().orElse(null))
                                        .podPort(podPortOpt.get())
                                        .addresses(StringUtils.join(ep.getAddresses(), ","))
                                        .port(port.getPort())
                                        .gmtCreate(gmtCreate)
                                        .gmtModified(gmtModified)
                                        .status(EventType.ADDED)
                                        .build();
                                backendEndpointRelationSet.add(backendEndpointRelationPO);
                            }
                        }
                    });
                    // 不存在pod提供endpoint服务的情况
                    endpointSlice.getEndpoints().stream()
                            .filter(ep -> ep.getTargetRef() == null)
                            .forEach(ep -> {
                                Optional<BackendEndpointRelationPO> backendEndpointRelation = endpointsService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), null);
                                final String state = BooleanUtils.isNotFalse(ep.getConditions().getReady()) ? "Ready" :
                                        BooleanUtils.isTrue(ep.getConditions().getTerminating()) ? "Terminating" : null;
                                if (backendEndpointRelation.isPresent()) {
                                    // 记录已存在的情况下，需要比对修改时间，只有时间在后的修改才可以提交数据库
                                    // 这个操作必须存在，原因请参考：https://kubernetes.io/zh/docs/concepts/services-networking/endpoint-slices/#distribution-of-endpointslices
                                    if (gmtModified != null && backendEndpointRelation.get().getGmtModified().isBefore(gmtModified)) {
                                        BackendEndpointRelationPO backendEndpointRelationPO = backendEndpointRelation.map(r -> {
                                            r.setGmtModified(gmtModified);
                                            r.setState(state);
                                            r.setStatus(EventType.ADDED);
                                            r.setAddresses(StringUtils.join(ep.getAddresses(), ","));
                                            r.setPort(port.getPort());
                                            return r;
                                        }).get();
                                        backendEndpointRelationSet.add(backendEndpointRelationPO);
                                    }
                                } else {
                                    BackendEndpointRelationPO backendEndpointRelationPO = BackendEndpointRelationPO.builder()
                                            .state(state)
                                            .servicePort(servicePortOptRef.get().orElse(null))
                                            .addresses(StringUtils.join(ep.getAddresses(), ","))
                                            .port(port.getPort())
                                            .gmtCreate(gmtCreate)
                                            .gmtModified(gmtModified)
                                            .status(EventType.ADDED)
                                            .build();
                                    backendEndpointRelationSet.add(backendEndpointRelationPO);
                                }
                            });
                }
            }
        }
        endpointsService.saveAll(backendEndpointRelationSet);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    protected void loadPods(Map<String, NodePO> nodeMap) throws ApiException {
        V1PodList v1PodList = K8sApi.listAllPods();
        List<V1Pod> items = v1PodList.getItems();
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        List<PodPO> podList = new ArrayList<>();
        ArrayList<PodPortPO> podPortPOList = new ArrayList<>();
        for (V1Pod pod : items) {
            String status = pod.getStatus().getPhase();
            StringBuilder containerIds = new StringBuilder();
            String state = null;
            List<V1ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            if (CollectionUtils.isNotEmpty(containerStatuses)) {
                state = "Ready";
                for (V1ContainerStatus containerStatus : containerStatuses) {
                    if (!containerStatus.getReady()) {
                        state = "NotReady";
                    }
                    if (containerStatus.getState().getRunning() != null) {
                        containerIds.append(containerStatus.getContainerID()).append(",");
                    }
                }
            }
            int lastCommaIndex = containerIds.lastIndexOf(",");
            if (lastCommaIndex > 0) {
                containerIds.deleteCharAt(lastCommaIndex);
            }
            PodPO podPO = PodPO.builder()
                    .uid(pod.getMetadata().getUid())
                    .gmtCreate(pod.getMetadata().getCreationTimestamp().toLocalDateTime())
                    .name(pod.getMetadata().getName())
                    .namespace(pod.getMetadata().getNamespace())
                    .status(EventType.ADDED)
                    .state(state)
                    .ip(pod.getStatus().getHostIP())
                    .hostname(pod.getSpec().getHostname())
                    .subdomain(pod.getSpec().getSubdomain())
                    .containerIds(containerIds.toString())
                    .node(nodeMap.get(pod.getStatus().getHostIP()))
                    .build();
            podList.add(podPO);
            List<V1Container> containers = pod.getSpec().getContainers();
            if (CollectionUtils.isNotEmpty(containers)) {
                List<PodPortPO> podPorts = containers.stream()
                        .map(c -> c.getPorts())
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .distinct()
                        .map(cp -> {
                            String podPortUid = CustomUidGenerateUtil.getPodPortUid(podPO.getUid(), cp.getContainerPort(), cp.getProtocol());
                            return PodPortPO.builder()
                                    .uid(podPortUid)
                                    .gmtCreate(podPO.getGmtCreate())
                                    .status(EventType.ADDED)
                                    .name(cp.getName())
                                    .port(cp.getContainerPort())
                                    .protocol(cp.getProtocol())
                                    .pod(podPO)
                                    .build();
                        })
                        .distinct()
                        .collect(Collectors.toList());
                podPortPOList.addAll(podPorts);
            }
        }
        podRepository.saveAllAndFlush(podList);
        podPortRepository.saveAllAndFlush(podPortPOList);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    protected Map<String, NodePO> loadNodeMapping() throws ApiException {
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
        nodePOList = nodeRepository.saveAllAndFlush(nodePOList);
        return nodePOList.stream().collect(Collectors.toMap(NodePO::getInternalIP, n -> n));
    }
}
