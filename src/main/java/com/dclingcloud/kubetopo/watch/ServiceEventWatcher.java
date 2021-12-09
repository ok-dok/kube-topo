package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.ServiceService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Async
@Slf4j
public class ServiceEventWatcher extends EventWatcher<V1Service> {
    @Resource
    private ServiceService serviceService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Service service = (V1Service) object;
        eventLog.append(", Name: ")
                .append(service.getMetadata().getName())
                .append(", Namespace: ")
                .append(service.getMetadata().getNamespace());
        switch (type.toUpperCase()) {
            case ADDED:
                try {
                    serviceService.save(service, ADDED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case DELETED:
                try {
                    serviceService.delete(service);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case MODIFIED:
                try {
                    serviceService.save(service, MODIFIED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    @Override
    protected Watch<V1Service> createWatch() throws ApiException {
        Call call = K8sApi.createIngressesCall(this.resourceVersion);
        Watch<V1Service> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Service>>() {
        }.getType());
        return watch;
    }

}
