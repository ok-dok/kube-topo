package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.entity.PodPO;
import com.dclingcloud.kubetopo.entity.PodPortPO;
import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.service.PodPortService;
import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.service.ServicePortService;
import com.dclingcloud.kubetopo.service.ServiceService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
public class ServiceEventWatcher extends EventWatcher<V1Service> {
    @Resource
    private ServiceService serviceService;
    @Resource
    private ServicePortService servicePortService;
    @Resource
    private PodService podService;
    @Resource
    private PodPortService podPortService;

    @Override
    protected void processEventObject(String type, V1Service object, StringBuilder eventLog) {
        V1Service service = (V1Service) object;
        eventLog.append(", Name: ")
                .append(service.getMetadata().getName())
                .append(", Namespace: ")
                .append(service.getMetadata().getNamespace());
        switch (type.toUpperCase()) {
            case MODIFIED:
                processModifiedEvent(service, eventLog);
                break;
            case ADDED:
                processAddedEvent(service, eventLog);
                break;
            case DELETED:
                try {
                    serviceService.delete(service);
                    servicePortService.deleteAllByServiceUid(service.getMetadata().getUid());
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    private void processAddedEvent(V1Service service, StringBuilder eventLog) {
        processSaveEvent(service, eventLog, ADDED);
    }

    private void processModifiedEvent(V1Service service, StringBuilder eventLog) {
        processSaveEvent(service, eventLog, MODIFIED);
    }

    private void processDeleteEvent(V1Service service, StringBuilder eventLog) {

    }

    private void processSaveEvent(V1Service service, StringBuilder eventLog, String status) {
        try {
            serviceService.saveOrUpdate(service, status);
            V1ServiceSpec spec = service.getSpec();
            if (CollectionUtils.isEmpty(spec.getPorts())) {
                return;
            }
            Map<String, List<PodPortPO>> endpointsMapping = loadEndpointsMapping(service.getMetadata().getName(), service.getMetadata().getNamespace());
            for (V1ServicePort sp : spec.getPorts()) {
                String id = service.getMetadata().getUid() + ":" + sp.getPort() + ":" + sp.getProtocol();
                String uid = new String(Base64Utils.encode(id.getBytes(StandardCharsets.UTF_8)));
                ServicePortPO servicePortPO = ServicePortPO.builder()
                        .uid(uid)
                        .service(ServicePO.builder().uid(service.getMetadata().getUid()).build())
                        .name(sp.getName())
                        .protocol(sp.getProtocol())
                        .appProtocol(sp.getAppProtocol())
                        .port(sp.getPort())
                        .nodePort(sp.getNodePort())
                        .status(ADDED)
                        .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                        .build();
                // 获取pod port映射列表
                List<PodPortPO> podPortPOList = Optional.ofNullable(endpointsMapping.get(sp.getName()))
                        .orElse(endpointsMapping.get(sp.getTargetPort().toString()));
                if (sp.getTargetPort().isInteger()) {
                    servicePortPO.setTargetPort(sp.getTargetPort().getIntValue());
                }
                if (servicePortPO.getTargetPort() == null) {
                    if (CollectionUtils.isNotEmpty(podPortPOList)) {
                        servicePortPO.setTargetPort(podPortPOList.get(0).getPort());
                    }
                }
                servicePortService.saveOrUpdate(servicePortPO);
                // 设置pod port关联到service port
                if (CollectionUtils.isNotEmpty(podPortPOList)) {
                    podPortPOList.forEach(p -> p.setServicePort(servicePortPO));
                    podPortService.saveAll(podPortPOList);
                }
            }
        } catch (K8sServiceException e) {
            // TODO 保存异常处理，重试？
            throw e;
        }
    }

    protected Map<String, List<PodPortPO>> loadEndpointsMapping(String svcName, String namespace) {
        V1Endpoints endpoints = null;
        try {
            endpoints = K8sApi.listEndpoints(namespace, svcName);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // 没有该资源，直接返回空
                return Collections.emptyMap();
            } else {
                log.error("Error: could not list endpoints with service name \"{}\" in namespace \"{}\"", svcName, namespace, e);
                return Collections.emptyMap();
            }
        }
        Optional<List<V1EndpointSubset>> subsetsOptional = Optional.ofNullable(endpoints).map(V1Endpoints::getSubsets);
        if (!subsetsOptional.isPresent())
            return Collections.emptyMap();

        Map<String, List<PodPortPO>> podPortMap = new HashMap<>();
        Map<String, PodPO> needUpdatePodMap = new HashMap<>();
        List<PodPortPO> podPortPOList = new ArrayList<>();
        for (V1EndpointSubset subset : subsetsOptional.get()) {
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
                    needUpdatePodMap.put(podPO.getUid(), podPO);
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
                                .status(ADDED)
                                .gmtCreate(endpoints.getMetadata().getCreationTimestamp().toLocalDateTime())
                                .build();
                        podPortPOList.add(podPortPO);
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
        return podPortMap;
    }

    @Override
    protected Watch<V1Service> createWatch() throws ApiException {
        Call call = K8sApi.createServicesCall(resourceVersionHolder.toString());
        Watch<V1Service> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Service>>() {
        }.getType());
        return watch;
    }

}
