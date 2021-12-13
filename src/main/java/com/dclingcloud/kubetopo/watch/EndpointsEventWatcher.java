package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.CoreV1EndpointPort;
import io.kubernetes.client.openapi.models.V1EndpointAddress;
import io.kubernetes.client.openapi.models.V1EndpointSubset;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Async
@Slf4j
public class EndpointsEventWatcher extends EventWatcher<V1Endpoints> {
    @Resource
    private PodPortService podPortService;
    @Resource
    private PodService podService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Endpoints endpoints = (V1Endpoints) object;
        switch (type) {
            case ADDED:
            case MODIFIED:
//                processAddedEvent(endpoints, eventLog);
                break;
            case DELETED:
                processDeleteEvent(endpoints, eventLog);
                break;
        }

    }

    private void processDeleteEvent(V1Endpoints endpoints, StringBuilder eventLog) {
        String epUid = endpoints.getMetadata().getUid();
        try {
            podPortService.deleteAllByEndpointsUid(epUid);
        } catch (K8sServiceException e) {
            // TODO 保存异常处理，重试？
            throw e;
        }
    }

    private void processAddedEvent(V1Endpoints endpoints, StringBuilder eventLog) {
        List<V1EndpointSubset> subsets = endpoints.getSubsets();
        if (subsets == null)
            return;
        List<PodPortPO> podPortPOList = new ArrayList<>();
        Map<String, PodPO> needUpdatePodMap = new HashMap<>();
        eventLog.append("Endpoints: ");
        for (V1EndpointSubset subset : subsets) {
            List<V1EndpointAddress> addresses = subset.getAddresses();
            if (CollectionUtils.isEmpty(addresses) && CollectionUtils.isNotEmpty(subset.getNotReadyAddresses())) {
                addresses = subset.getNotReadyAddresses();
            }
            for (V1EndpointAddress address : addresses) {
                PodPO podPO = null;
                if (address.getTargetRef() != null) {
                    if (!needUpdatePodMap.containsKey(address.getTargetRef().getUid())) {
                        podPO = PodPO.builder()
                                .uid(address.getTargetRef().getUid())
                                .name(address.getTargetRef().getName())
                                .namespace(address.getTargetRef().getNamespace())
                                .nodeName(address.getNodeName())
                                .hostname(address.getHostname())
                                .ip(address.getIp())
                                .build();
                        needUpdatePodMap.put(podPO.getUid(), podPO);
                    }
                }

                // Headless services with no ports.
                if (subset.getPorts().size() != 0) {
                    for (CoreV1EndpointPort port : subset.getPorts()) {
                        String id = endpoints.getMetadata().getUid() + ":" + Optional.ofNullable(address.getTargetRef()).map(t -> t.getUid()).map(Objects::toString).orElse("null") + ":" + port.getPort() + ":" + port.getProtocol();
                        String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                        //String hash = DigestUtils.md5DigestAsHex(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                        //String uid = new StringBuilder(hash).insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-').toString();
                        PodPortPO podPortPO = PodPortPO.builder()
                                .uid(uid)
                                .epUid(endpoints.getMetadata().getUid())
                                .pod(podPO)
                                .status(ADDED)
                                .name(port.getName())
                                .port(port.getPort())
                                .protocol(port.getProtocol())
                                .appProtocol(port.getAppProtocol())
                                .gmtCreate(endpoints.getMetadata().getCreationTimestamp().toLocalDateTime())
                                .build();
                        podPortPOList.add(podPortPO);
                    }
                }
            }
        }
        List<PodPO> podPOList = needUpdatePodMap.values().stream().map(pod ->
                podService.findByUid(pod.getUid())
                        .map(podPO -> podPO.setHostname(pod.getHostname())
                                .setNodeName(pod.getNodeName()))
                        .orElse(pod)
        ).collect(Collectors.toList());
        try {
            podService.saveAll(podPOList);
            podPortService.saveAll(podPortPOList);
        } catch (K8sServiceException e) {
            // TODO 保存异常处理，重试？
            throw e;
        }
        String endpointsStr = podPortPOList.stream().map(podPort -> Optional.ofNullable(podPort.getPod())
                        .map(PodPO::getIp).orElse("") + ":" + podPort.getPort())
                .collect(Collectors.joining());
        eventLog.append(endpointsStr);
    }

    @Override
    protected Watch<V1Endpoints> createWatch() throws ApiException {
        Call endpointsCall = K8sApi.createEndpointsCall(this.resourceVersion);
        Watch<V1Endpoints> watch = Watch.createWatch(apiClient, endpointsCall, new TypeToken<Watch.Response<V1Endpoints>>() {
        }.getType());
        return watch;
    }
}
