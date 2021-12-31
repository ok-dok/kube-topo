package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.entity.ServicePO;
import com.dclingcloud.kubetopo.entity.ServicePortPO;
import com.dclingcloud.kubetopo.service.*;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

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
    @Resource
    private BackendEndpointRelationService backendEndpointRelationService;

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
                processDeleteEvent(service, eventLog);
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
        try {
            serviceService.delete(service);
            servicePortService.deleteAllByServiceUid(service.getMetadata().getUid());
        } catch (K8sServiceException e) {
            // TODO 保存异常处理，重试？
            throw e;
        }
    }

    private void processSaveEvent(V1Service service, StringBuilder eventLog, String status) {
        try {
            serviceService.saveOrUpdate(service, status);
            V1ServiceSpec spec = service.getSpec();
            if (CollectionUtils.isEmpty(spec.getPorts())) {
                return;
            }
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
                        .targetPort(sp.getTargetPort())
                        .nodePort(sp.getNodePort())
                        .status(status)
                        .gmtCreate(service.getMetadata().getCreationTimestamp().toLocalDateTime())
                        .build();
                servicePortService.saveOrUpdate(servicePortPO);
            }
        } catch (K8sServiceException e) {
            // TODO 保存异常处理，重试？
            throw e;
        }
    }

    @Override
    protected Watch<V1Service> createWatch() throws ApiException {
        Call call = K8sApi.createServicesCall(resourceVersionHolder.toString());
        Watch<V1Service> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Service>>() {
        }.getType());
        return watch;
    }

}
