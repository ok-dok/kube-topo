package com.dclingcloud.kubetopo.watch;

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
            Set<BackendEndpointRelationPO> backendEndpointRelationSet = new HashSet<>();
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
                        Optional<BackendEndpointRelationPO> backendEndpointRelation = backendEndpointRelationService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), podPortOpt.get());
                        final String state = BooleanUtils.isNotFalse(ep.getConditions().getReady()) ? "Ready" :
                                BooleanUtils.isTrue(ep.getConditions().getTerminating()) ? "Terminating" : null;
                        if (backendEndpointRelation.isPresent()) {
                            // 记录已存在的情况下，需要比对修改时间，只有时间在后的修改才可以提交数据库
                            // 这个操作必须存在，原因请参考：https://kubernetes.io/zh/docs/concepts/services-networking/endpoint-slices/#distribution-of-endpointslices
                            if (gmtModified != null && backendEndpointRelation.get().getGmtModified().isBefore(gmtModified)) {
                                BackendEndpointRelationPO backendEndpointRelationPO = backendEndpointRelation.map(r -> {
                                    r.setGmtModified(gmtModified);
                                    r.setState(state);
                                    r.setStatus(status);
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
                                    .status(status)
                                    .build();
                            backendEndpointRelationSet.add(backendEndpointRelationPO);
                        }
                    }
                });
                // 不存在pod提供endpoint服务的情况
                endpointSlice.getEndpoints().stream()
                        .filter(ep -> ep.getTargetRef() == null)
                        .forEach(ep -> {
                            Optional<BackendEndpointRelationPO> backendEndpointRelation = backendEndpointRelationService.findByServicePortUidAndPodPortUid(servicePortOptRef.get().orElse(null), null);
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
