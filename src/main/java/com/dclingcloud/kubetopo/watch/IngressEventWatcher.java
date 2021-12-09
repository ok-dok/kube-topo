package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.IngressService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
@Async
public class IngressEventWatcher extends EventWatcher<V1Ingress> {
    @Resource
    private IngressService ingressService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Ingress ingress = (V1Ingress) object;
        switch (type.toUpperCase()) {
            case ADDED:
                try {
                    ingressService.save(ingress, ADDED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case DELETED:
                try {
                    ingressService.delete(ingress);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case MODIFIED:
                try {
                    ingressService.save(ingress, MODIFIED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    @Override
    protected Watch<V1Ingress> createWatch() throws ApiException {
        Call call = K8sApi.createIngressesCall(this.resourceVersion);
        Watch<V1Ingress> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Ingress>>() {
        }.getType());
        return watch;
    }

}
