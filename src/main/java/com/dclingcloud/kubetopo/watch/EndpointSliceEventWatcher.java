package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.beanmapper.BackendEndpointRelationPOMapper;
import com.dclingcloud.kubetopo.constants.K8sResources;
import com.dclingcloud.kubetopo.entity.BackendEndpointRelationPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.service.BackendEndpointRelationService;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.service.ServicePortService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Async
@Slf4j
public class EndpointSliceEventWatcher extends EventWatcher<V1EndpointSlice> {
    @Resource
    private ServicePortService servicePortService;
    @Resource
    private BackendEndpointRelationService backendEndpointRelationService;
    @Resource
    private PodPortService podPortService;
    @Resource
    private BackendEndpointRelationPOMapper backendEndpointRelationPOMapper;

    @Override
    protected void processEventObject(String type, V1EndpointSlice object, StringBuilder eventLog) throws ApiException {
        V1EndpointSlice endpointSlice = object;
        switch (type) {
            case MODIFIED:
                processModifiedEvent(endpointSlice, eventLog);
            case ADDED:
                processAddedEvent(endpointSlice, eventLog);
                break;
            case DELETED:
                processDeleteEvent(endpointSlice, eventLog);
                break;
        }
    }

    private void processDeleteEvent(V1EndpointSlice endpointSlice, StringBuilder eventLog) throws ApiException {
        V1ObjectMeta metadata = endpointSlice.getMetadata();
        LocalDateTime gmtModified = Optional.ofNullable(metadata.getAnnotations())
                .map(map -> map.get("endpoints.kubernetes.io/last-change-trigger-time"))
                .map(timestamp -> LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .orElse(null);
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
        if(serviceUid != null){
            backendEndpointRelationService.deleteByServiceUidBeforeModifiedDateTime(serviceUid, gmtModified);
        }
    }

    private void processAddedEvent(V1EndpointSlice endpointSlice, StringBuilder eventLog) throws ApiException {
        processSaveEvent(endpointSlice, ADDED);
    }

    private void processModifiedEvent(V1EndpointSlice endpointSlice, StringBuilder eventLog) throws ApiException {
        processSaveEvent(endpointSlice, MODIFIED);
    }

    private void processSaveEvent(V1EndpointSlice endpointSlice, String status) throws ApiException {
        resourceVersionHolder.syncUpdateLatest(endpointSlice.getMetadata().getResourceVersion());
        V1ObjectMeta metadata = endpointSlice.getMetadata();
        LocalDateTime gmtCreate = metadata.getCreationTimestamp().toLocalDateTime();
        LocalDateTime gmtModified = Optional.ofNullable(metadata.getAnnotations())
                .map(map -> map.get("endpoints.kubernetes.io/last-change-trigger-time"))
                .map(timestamp -> LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .orElse(null);
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
            HashSet<BackendEndpointRelationPO> backendEndpointRelationSet = new HashSet<>();
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
                                }
                            } else {
                                backendEndpointRelationSet.add(builder.status(EventType.ADDED).build());
                            }
                        });
            }
            backendEndpointRelationService.saveAll(backendEndpointRelationSet);
        }
    }

    @Override
    protected Watch<V1EndpointSlice> createWatch() throws ApiException {
        Call endpointSliceCall = K8sApi.createEndpointSliceCall(this.resourceVersionHolder.toString());
        Watch<V1EndpointSlice> watch = Watch.createWatch(apiClient, endpointSliceCall, new TypeToken<Watch.Response<V1EndpointSlice>>() {
        }.getType());
        return watch;
    }
}
