package com.dclingcloud.kubetopo.service.impl;

import com.dclingcloud.kubetopo.beanmapper.BackendEndpointRelationPOMapper;
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
    private BackendEndpointRelationService backendEndpointRelationService;
    @Resource
    private BackendEndpointRelationRepository backendEndpointRelationRepository;
    @Resource
    private ServicePortService servicePortService;
    @Resource
    private PodPortService podPortService;
    @Resource
    private BackendEndpointRelationPOMapper backendEndpointRelationPOMapper;

    @Override
    public TopologyVO getTopology() {
        List<ServicePO> svcList = serviceRepository.findAll();
        List<NodePO> nodeList = nodeRepository.findAll();
        List<IngressPO> ingressList = ingressRepository.findAll();
        List<PodPO> podList = podRepository.findAll();
        List<ServiceVO> svcs = svcList.stream().map(svc -> {
            List<BackendVO> backends = Optional.ofNullable(servicePortRepository.findAllByService(svc)).map(l -> l.stream().map(p -> {
                List<String> endpointsUids = Optional.ofNullable(p.getBackendEndpointRelations()).map(Collection::stream)
                        .map(stream -> stream.map(b -> b.getPodPort()).filter(Objects::nonNull).map(PodPortPO::getUid)
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
            backendEndpointRelationRepository.updateAllWithDeletedStatus();
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
                // ??????ingress path rule??????
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
            // ??????????????????IP????????????
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
                // ????????????
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
                    .orElse(gmtCreate);
            String namespace = metadata.getNamespace();
            Optional<List<V1OwnerReference>> ownerReferencesOpt = Optional.ofNullable(metadata.getOwnerReferences());
            // ???????????????ownerReferences??????????????????????????????????????????????????????????????????????????????????????????????????????
            // TODO ??????????????????ownerReference?????????
            // ????????????????????????endpoint???????????????service?????????????????????kubernetes????????????endpoint
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
                // ???????????????endpoints???????????????pod????????????
                Map<String, V1Endpoint> podStates = endpointSlice.getEndpoints().stream()
                        .filter(ep -> ep.getTargetRef() != null)
                        .map(ep -> {
                            return new AbstractMap.SimpleEntry<String, V1Endpoint>(ep.getTargetRef().getUid(), ep);
                        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

                for (DiscoveryV1EndpointPort port : endpointSlice.getPorts()) {
                    IntOrString targetPort = new IntOrString(port.getPort());
                    // ??????AtomicReference?????????????????????????????????????????????????????????????????????????????????
                    AtomicReference<Optional<ServicePortPO>> servicePortOptRef = new AtomicReference(servicePortService.findByServiceUidAndTargetPortAndProtocol(serviceUid, targetPort, port.getProtocol()));
                    // endpoint??????targetRef????????????????????????pod??????endpoint??????
                    //???????????? ServicePort ??? PodPort???????????????
                    String finalServiceUid = serviceUid;
                    podStates.forEach((podUid, ep) -> {
                        Optional<PodPortPO> podPortOpt = podPortService.find(podUid, targetPort, port.getProtocol());
                        // ???????????????podPort?????????????????????????????????????????????????????????pod????????????????????????????????????????????????????????????????????????PodPort?????????????????????????????????
                        if (podPortOpt.isPresent()) {
                            // ??????Integer?????????targetPort?????????servicePort??????????????????????????????
                            servicePortOptRef.updateAndGet(prev -> {
                                // ??????????????????????????????????????????
                                if (!prev.isPresent()) {
                                    return servicePortService.findByServiceUidAndTargetPortAndProtocol(finalServiceUid, new IntOrString(podPortOpt.get().getName()), port.getProtocol());
                                } else {
                                    return prev;
                                }
                            });
                            Optional<BackendEndpointRelationPO> backendEndpointRelationOpt = backendEndpointRelationService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), podPortOpt.get());
                            final String state = BooleanUtils.isNotFalse(ep.getConditions().getReady()) ? "Ready" :
                                    BooleanUtils.isTrue(ep.getConditions().getTerminating()) ? "Terminating" : "NotReady";
                            BackendEndpointRelationPO.BackendEndpointRelationPOBuilder<?, ?> builder = BackendEndpointRelationPO.builder()
                                    .state(state)
                                    .servicePort(servicePortOptRef.get().orElse(null))
                                    .podPort(podPortOpt.get())
                                    .addresses(StringUtils.join(ep.getAddresses(), ","))
                                    .port(port.getPort())
                                    .gmtCreate(gmtCreate)
                                    .gmtModified(gmtModified);
                            if (backendEndpointRelationOpt.isPresent()) {
                                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                // ?????????????????????????????????????????????https://kubernetes.io/zh/docs/concepts/services-networking/endpoint-slices/#distribution-of-endpointslices
                                BackendEndpointRelationPO persistPO = backendEndpointRelationOpt.get();
                                if (gmtModified != null && persistPO.getGmtModified().isBefore(gmtModified)) {
                                    backendEndpointRelationPOMapper.updatePropertiesIgnoresNull(persistPO,
                                            builder.status(EventType.MODIFIED).build());
                                    backendEndpointRelationSet.add(persistPO);
                                } else if (EventType.DELETED.equalsIgnoreCase(persistPO.getStatus())) {
                                    persistPO.setStatus(EventType.ADDED);
                                    backendEndpointRelationSet.add(persistPO);
                                }
                            } else {
                                backendEndpointRelationSet.add(builder.status(EventType.ADDED).build());
                            }
                        }
                    });
                    // ?????????pod??????endpoint???????????????
                    endpointSlice.getEndpoints().stream()
                            .filter(ep -> ep.getTargetRef() == null)
                            .forEach(ep -> {
                                Optional<BackendEndpointRelationPO> backendEndpointRelationOpt = backendEndpointRelationService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), null);
                                final String state = BooleanUtils.isNotFalse(ep.getConditions().getReady()) ? "Ready" :
                                        BooleanUtils.isTrue(ep.getConditions().getTerminating()) ? "Terminating" : "NotReady";
                                BackendEndpointRelationPO.BackendEndpointRelationPOBuilder<?, ?> builder = BackendEndpointRelationPO.builder()
                                        .state(state)
                                        .servicePort(servicePortOptRef.get().orElse(null))
                                        .addresses(StringUtils.join(ep.getAddresses(), ","))
                                        .port(port.getPort())
                                        .gmtCreate(gmtCreate)
                                        .gmtModified(gmtModified);
                                if (backendEndpointRelationOpt.isPresent()) {
                                    // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                                    // ?????????????????????????????????????????????https://kubernetes.io/zh/docs/concepts/services-networking/endpoint-slices/#distribution-of-endpointslices
                                    BackendEndpointRelationPO persistPO = backendEndpointRelationOpt.get();
                                    if (gmtModified != null && persistPO.getGmtModified().isBefore(gmtModified)) {
                                        backendEndpointRelationPOMapper.updatePropertiesIgnoresNull(persistPO,
                                                builder.status(EventType.MODIFIED).build());
                                        backendEndpointRelationSet.add(persistPO);
                                    } else if (EventType.DELETED.equalsIgnoreCase(persistPO.getStatus())) {
                                        persistPO.setStatus(EventType.ADDED);
                                        backendEndpointRelationSet.add(persistPO);
                                    }
                                } else {
                                    backendEndpointRelationSet.add(builder.status(EventType.ADDED).build());
                                }
                            });
                }
            }
        }
        backendEndpointRelationService.saveAll(backendEndpointRelationSet);
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
            StringBuilder containerIds = new StringBuilder();
            String state = "NotReady";
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
