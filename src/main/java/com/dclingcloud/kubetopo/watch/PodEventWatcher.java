package com.dclingcloud.kubetopo.watch;

import com.dclingcloud.kubetopo.service.PodService;
import com.dclingcloud.kubetopo.util.K8sApi;
import com.dclingcloud.kubetopo.util.K8sServiceException;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Component
@Async
@Slf4j
public class PodEventWatcher extends EventWatcher<V1Pod> {
    @Resource
    private PodService podService;

    @Override
    protected void processEventObject(String type, Object object, StringBuilder eventLog) {
        V1Pod pod = (V1Pod) object;
        String status = pod.getStatus().getPhase();
        if ("Running".equalsIgnoreCase(status)) {
            List<V1ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
            if (CollectionUtils.isNotEmpty(containerStatuses)) {
                long readyCount = containerStatuses.stream().filter(cs -> cs.getReady()).count();
                if (readyCount == containerStatuses.size()) {
                    status = "Ready";
                } else {
                    status = "NotReady";
                }
            } else {
                status = "NotReady";
            }
        }
        eventLog.append(", Status: ")
                .append(status)
                .append(", ContainerID: ")
                .append(Optional.ofNullable(pod.getStatus().getContainerStatuses())
                        .map(l -> l.get(0)).map(s -> s.getContainerID()).orElse(null));
        switch (type) {
            case ADDED:
                try {
                    podService.save(pod, ADDED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case DELETED:
                try {
                    podService.delete(pod);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
            case MODIFIED:
                try {
                    podService.save(pod, MODIFIED);
                } catch (K8sServiceException e) {
                    // TODO 保存异常处理，重试？
                    throw e;
                }
                break;
        }
    }

    @Override
    protected Watch<V1Pod> createWatch() throws ApiException {
        Call call = K8sApi.createPodsCall(this.resourceVersion);
        Watch<V1Pod> watch = Watch.createWatch(apiClient, call, new TypeToken<Watch.Response<V1Pod>>() {
        }.getType());
        return watch;
    }

}
